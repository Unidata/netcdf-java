/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
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
  private AmazonS3URI uri;
  private S3Client client;
  private String bucket;
  private String key;
  private HeadObjectResponse objectHeadResponse;

  private S3RandomAccessFile(String url) {
    super(url, s3BufferSize, s3MaxReadCacheSize);

    uri = new AmazonS3URI(url);
    bucket = uri.getBucket();
    key = uri.getKey();

    Builder httpConfig = ApacheHttpClient.builder().maxConnections(maxConnections)
        .connectionTimeout(Duration.ofMillis(connectionTimeout)).socketTimeout(Duration.ofMillis(socketTimeout));
    S3ClientBuilder s3ClientBuilder = S3Client.builder().httpClientBuilder(httpConfig);

    // use the Default Credential Provider Chain:
    // https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html
    // but, as a last resort, try the AnonymousCredentialsProvider.
    AwsCredentialsProviderChain defaultThenAnonymous = AwsCredentialsProviderChain.builder()
        .credentialsProviders(DefaultCredentialsProvider.create(), AnonymousCredentialsProvider.create()).build();

    // Uses the default Default Region Provider Chain:
    // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
    client = s3ClientBuilder.credentialsProvider(defaultThenAnonymous).build();
    // request HEAD for the object
    objectHeadResponse = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
  }

  public void closeRemote() {
    // close the client
    if (client != null) {
      client.close();
    }

    uri = null;
    client = null;
    bucket = null;
    key = null;
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
    GetObjectRequest rangeObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).range(range).build();

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
      return location.startsWith("s3:");
    }

    /**
     * Open a location that this Provider is the owner of.
     */
    @Override
    public RandomAccessFile open(String location) {
      return new S3RandomAccessFile(location);
    }
  }
}
