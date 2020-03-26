/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.ReadableRemoteFile;
import ucar.unidata.io.RemoteRandomAccessFile;
import ucar.unidata.io.spi.RandomAccessFileProvider;

/**
 * @author James McClain, based on work by John Caron and Donald Denbof
 */

public final class S3RandomAccessFile extends RemoteRandomAccessFile implements ReadableRemoteFile, Closeable {

  private static final int s3BufferSize = Integer
      .parseInt(System.getProperty("ucar.unidata.io.s3.bufferSize", String.valueOf(defaultRemoteFileBufferSize)));

  private static final long s3MaxReadCacheSize = Long
      .parseLong(System.getProperty("ucar.unidata.io.s3.maxReadCacheSize", String.valueOf(defaultMaxReadCacheSize)));
  /**
   * The maximum number of connections allowed in the S3 http connection pool. Each built S3 HTTP client has it's own
   * private connection pool.
   */
  private static final int maxConnections =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.httpMaxConnections", "128"));
  /**
   * The amount of time to wait (in milliseconds) when initially establishing a connection before giving up and timing
   * out. A duration of
   * 0 means infinity, and is not recommended.
   */
  private static final int connectionTimeout = Integer
      .parseInt(System.getProperty("ucar.unidata.io.s3.connectionTimeout", String.valueOf(defaultRemoteFileTimeout)));
  /**
   * The amount of time to wait for data to be transferred over an established, open connection before the connection is
   * timed out. A duration of 0 means infinity, and is not recommended.
   */
  private static final int socketTimeout =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.socketTimeout", "100000"));
  private final CdmS3Uri uri;
  private final S3Client client;

  private HeadObjectResponse objectHeadResponse;

  private S3RandomAccessFile(String url) throws IOException {
    super(url, s3BufferSize, s3MaxReadCacheSize);

    // Region is tricky. Since we are using AWS SDK to manage connections to all object stores, we might have users
    // who use netCDF-Java and never touch AWS. If that's they case, they likely have not setup a basic credentials or
    // configuration file, and thus lack a default region. What we will do here is check to see if there is one set.
    // If, by the time we make the client, profileRegion isn't set, we will default to the AWS_GLOBAL region, which is
    // like a no-op region when it comes to S3. This will allow requests to non-AWS-S3 object stores to work, because
    // a region must be set, even if it's useless.
    Optional<Region> profileRegion = ProfileFile.defaultProfileFile().profile("default")
        .map(p -> p.properties().get(ProfileProperty.REGION)).map(Region::of);

    try {
      uri = new CdmS3Uri(url);
    } catch (URISyntaxException urie) {
      // If we are given a string that is not a valid CdmS3Uri
      // throw an IOException
      throw new IOException(urie.getCause());
    }

    Builder httpConfig = ApacheHttpClient.builder().maxConnections(maxConnections)
        .connectionTimeout(Duration.ofMillis(connectionTimeout)).socketTimeout(Duration.ofMillis(socketTimeout));

    S3ClientBuilder s3ClientBuilder = S3Client.builder().httpClientBuilder(httpConfig);

    // if we are accessing an S3 compatible service, we need to override the server endpoint
    uri.getEndpoint().ifPresent(s3ClientBuilder::endpointOverride);

    // build up a chain of credentials providers
    AwsCredentialsProviderChain.Builder cdmCredentialsProviderChainBuilder = AwsCredentialsProviderChain.builder();

    // if uri has a profile name, we need setup a credentials provider to look for potential credentials, and see if a
    // region has been set
    if (uri.getProfile().isPresent()) {
      // get the profile name
      String profileName = uri.getProfile().get();

      ProfileCredentialsProvider namedProfileCredentials =
          ProfileCredentialsProvider.builder().profileName(profileName).build();

      // add it to the chain that it is the first thing checked for credentials
      cdmCredentialsProviderChainBuilder.addCredentialsProvider(namedProfileCredentials);

      // Read the region associated with the profile, if set
      // Note: the java sdk does not do this by default
      Optional<Region> namedProfileRegion = ProfileFile.defaultProfileFile().profile(profileName)
          .map(p -> p.properties().get(ProfileProperty.REGION)).map(Region::of);
      // if the named profile has a region, update profileRegion to use it.
      if (namedProfileRegion.isPresent()) {
        profileRegion = namedProfileRegion;
      }
    }

    // Add the Default Credentials Provider Chain:
    // https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html
    cdmCredentialsProviderChainBuilder.addCredentialsProvider(DefaultCredentialsProvider.create());

    // Add the AnonymousCredentialsProvider last
    cdmCredentialsProviderChainBuilder.addCredentialsProvider(AnonymousCredentialsProvider.create());

    // build the credentials provider that we'll use
    AwsCredentialsProviderChain cdmCredentialsProviderChain = cdmCredentialsProviderChainBuilder.build();

    // Add the credentials provider to the client builder
    s3ClientBuilder.credentialsProvider(cdmCredentialsProviderChain);

    // Set the region for the client builder (default to AWS_GLOBAL)
    s3ClientBuilder.region(profileRegion.orElse(Region.AWS_GLOBAL));

    // Build the client
    client = s3ClientBuilder.build();

    // request HEAD for the object
    HeadObjectRequest headdObjectRequest =
        HeadObjectRequest.builder().bucket(uri.getBucket()).key(uri.getKey()).build();

    objectHeadResponse = client.headObject(headdObjectRequest);
  }

  public void closeRemote() {
    // close the client
    if (client != null) {
      client.close();
    }

    objectHeadResponse = null;
  }

  /**
   * Read directly from the remote service All reading goes through here or readToByteChannel;
   *
   * 1. https://docs.aws.amazon.com/AmazonS3/latest/dev/RetrievingObjectUsingJava.html
   *
   * @param pos start here in the file
   * @param buff put data into this buffer
   * @param offset buffer offset
   * @param len this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  @Override
  public int readRemote(long pos, byte[] buff, int offset, int len) throws IOException {

    String range = String.format("bytes=%d-%d", pos, pos + len);
    GetObjectRequest rangeObjectRequest =
        GetObjectRequest.builder().bucket(uri.getBucket()).key(uri.getKey()).range(range).build();

    ResponseBytes<GetObjectResponse> objectPortion = client.getObjectAsBytes(rangeObjectRequest);

    int bytes;
    int totalBytes = 0;
    // read response into buff
    try (InputStream objectData = objectPortion.asInputStream()) {
      bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
      while ((bytes > 0) && ((len - totalBytes) > 0)) {
        totalBytes += bytes;
        bytes = objectData.read(buff, offset + totalBytes, len - totalBytes);
      }
    }
    return totalBytes;
  }


  @Override
  public long length() {
    return objectHeadResponse.contentLength();
  }

  @Override
  public long getLastModified() {
    return objectHeadResponse.lastModified().toEpochMilli();
  }

  /**
   * Hook into service provider interface to RandomAccessFileProvider. Register in
   * META-INF.services.ucar.unidata.io.spi.RandomAccessFileProvider
   */
  public static class Provider implements RandomAccessFileProvider {

    @Override
    public boolean isOwnerOf(String location) {
      return location.startsWith("cdms3:") || location.startsWith("s3:");
    }

    /**
     * Open a location that this Provider is the owner of.
     */
    @Override
    public RandomAccessFile open(String location) throws IOException {
      return new S3RandomAccessFile(location);
    }
  }
}
