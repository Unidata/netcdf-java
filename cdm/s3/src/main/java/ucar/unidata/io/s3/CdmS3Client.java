/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient.Builder;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * A client for accessing Object Store resources.
 *
 * This class manages Region settings (for AWS S3 resources), authentication, and http client connection properties.
 *
 */
public class CdmS3Client {

  static {
    s3ClientCache = CacheBuilder.newBuilder().removalListener(new RemovalListener<CdmS3Uri, S3Client>() {
      @Override
      public void onRemoval(RemovalNotification<CdmS3Uri, S3Client> entry) {
        // close the client after eviction from the cache
        entry.getValue().close();
      }
    }).build(new CacheLoader<CdmS3Uri, S3Client>() {
      public S3Client load(@Nonnull CdmS3Uri key) {
        return createS3Client(key);
      }
    });
  }

  private static final Logger logger = LoggerFactory.getLogger(CdmS3Client.class);

  private static final String AWS_REGION_ENV_VAR_NAME = "AWS_REGION";
  private static final String AWS_REGION_PROP_NAME = "aws.region";

  /**
   * The maximum number of connections allowed in the S3 http connection pool. Each built S3 HTTP client has it's own
   * private connection pool.
   */
  private static final int maxConnections =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.httpMaxConnections", "128"));

  /**
   * The amount of time to wait (in milliseconds) when initially establishing a connection before giving up and timing
   * out. A duration of 0 means infinity, and is not recommended.
   */
  private static final int connectionTimeout =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.connectionTimeout",
          String.valueOf(S3RandomAccessFile.getDefaultRemoteFileTimeout())));

  /**
   * The amount of time to wait for data to be transferred over an established, open connection before the connection is
   * timed out. A duration of 0 means infinity, and is not recommended.
   */
  private static final int socketTimeout =
      Integer.parseInt(System.getProperty("ucar.unidata.io.s3.socketTimeout", "100000"));

  private static final LoadingCache<CdmS3Uri, S3Client> s3ClientCache;
  private static boolean useCache = true;

  /**
   * Acquire an S3Client configured to connect to the object store associated with the supplied CdmS3Uri.
   *
   * Clients are cached based on the authority and path (bucket) of the supplied CdmS3Uri.
   *
   * @param uri string representing a {@link CdmS3Uri}
   * @return S3Client
   * @throws IOException Indicates an error creating the client, or an error creating the CdmS3Uri object.
   */
  public static S3Client acquire(String uri) throws IOException {
    CdmS3Uri cdmS3Uri;
    try {
      cdmS3Uri = new CdmS3Uri(uri);
    } catch (URISyntaxException urie) {
      // If we are given a string that is not a valid CdmS3Uri
      // throw an IOException
      throw new IOException("Error making a CdmS3Uri from:" + uri, urie);
    }
    return acquire(cdmS3Uri);
  }

  /**
   * Acquire an S3Client configured to connect to the object store associated with the supplied CdmS3Uri.
   *
   * Clients are cached based on the authority and path (bucket) of the supplied CdmS3Uri.
   *
   * @param uri {@link CdmS3Uri}
   * @return S3Client
   * @throws IOException Indicates an error creating the client, or an error creating the CdmS3Uri object.
   */
  public static S3Client acquire(CdmS3Uri uri) throws IOException {
    CdmS3Uri key = makeKey(uri);
    S3Client s3Client;
    try {
      s3Client = useCache ? s3ClientCache.get(key) : createS3Client(uri);
    } catch (ExecutionException e) {
      // If we are given a string that is not a valid CdmS3Uri
      // throw an IOException
      throw new IOException("Error obtaining S3Client from the cache.", e.getCause());
    }
    return s3Client;
  }

  /** enable/disable cache for testing */
  static void enableCache(boolean enableCache) {
    useCache = enableCache;
  }

  private static CdmS3Uri makeKey(CdmS3Uri cdmS3Uri) {
    CdmS3Uri bucketUri = null;
    String bucketUriString = cdmS3Uri.toString();
    if (cdmS3Uri.getKey().isPresent()) {
      int chop = bucketUriString.lastIndexOf("?" + cdmS3Uri.getKey().get());
      // remove everything after the ?key portion of the URI (retains authority and path (bucket) of URI)
      if (chop > 0) {
        bucketUriString = bucketUriString.substring(0, chop - 1);
      }
    }

    try {
      bucketUri = new CdmS3Uri(bucketUriString);
    } catch (URISyntaxException e) {
      bucketUri = null;
    }

    return bucketUri;
  }

  private static S3Client createS3Client(CdmS3Uri uri) {
    // Region is tricky. Since we are using AWS SDK to manage connections to all object stores, we might have users
    // who use netCDF-Java and never touch AWS. If that's they case, they likely have not setup a basic credentials or
    // configuration file, and thus lack a default region. What we will do here is check to see if there is one set.
    // If, by the time we make the client, profileRegion isn't set, we will default to the AWS_GLOBAL region, which is
    // like a no-op region when it comes to S3. This will allow requests to non-AWS-S3 object stores to work, because
    // a region must be set, even if it's useless.
    // First, look for a region in the default profile of the default config file (~/.aws/config)
    Optional<Region> profileRegion = ProfileFile.defaultProfileFile().profile("default")
        .map(p -> p.properties().get(ProfileProperty.REGION)).map(Region::of);

    // If region not found, check the aws.region system property and, if not found there, the environmental
    // variable AWS_REGION
    if (!profileRegion.isPresent()) {
      // first check system property
      logger.debug("Checking system property {} for Region.", AWS_REGION_PROP_NAME);
      String regionName = System.getProperty(AWS_REGION_PROP_NAME);
      if (regionName == null) {
        // ok, now check environmental variable
        logger.debug("Checking environmental variable {} for Region.", AWS_REGION_ENV_VAR_NAME);
        regionName = System.getenv(AWS_REGION_ENV_VAR_NAME);
      }

      if (regionName != null) {
        logger.debug("Region found: {}", regionName);
        profileRegion = Optional.ofNullable(Region.of(regionName));
      }
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

      // Read the region associated with the profile, if set, as it might be different than the
      // default value in the profile file, or the value found in the System property or Environmental
      // variable.
      // Note: the java sdk does not do this by default
      Optional<Region> namedProfileRegion = ProfileFile.defaultProfileFile().profile(profileName)
          .map(p -> p.properties().get(ProfileProperty.REGION)).map(Region::of);
      // if the named profile has a region, update profileRegion to use it.
      if (namedProfileRegion.isPresent()) {
        logger.debug("Region {} found for profile {} - will be using this region.", namedProfileRegion.get(),
            profileName);
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

    // Set the region for the client builder (or default to AWS_GLOBAL)
    s3ClientBuilder.region(profileRegion.orElse(Region.AWS_GLOBAL));

    // Build the client
    return s3ClientBuilder.build();
  }
}
