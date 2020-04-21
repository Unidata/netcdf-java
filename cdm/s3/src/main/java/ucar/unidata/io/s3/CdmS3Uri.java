package ucar.unidata.io.s3;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
public final class CdmS3Uri {

  private static final Logger logger = LoggerFactory.getLogger(CdmS3Uri.class);

  private static final String SCHEME_HTTP = "http";
  private static final String SCHEME_HTTPS = "https";
  private static final String SCHEME_CDM_S3 = "cdms3";
  private static final String SCHEME_CDM_S3_DEPRECATED = "s3";

  private final String bucket;
  private final String key;
  private final String profile;
  private final URI endpoint;

  /**
   * A {@link URI} for use by the CDM for identifying a resource on an Object Store, such as AWS S3, Google Cloud
   * Storage,
   * Azure Blob Storage, Ceph, etc.
   * <p>
   * Using the generic URI syntax from <a href="https://tools.ietf.org/html/rfc3986">RFC3986</a>, the CDM will identify
   * resources located in an object store as follows:
   * </p>
   * <div>
   * <ul>
   * <li>scheme (<b>required</b>): defined to be cdms3</li>
   * <li>
   * authority (<b>optional for AWS S3, otherwise required</b>): If present, the authority component is preceded by a
   * double slash ("//") and is
   * terminated by the next slash ("/").
   * <br>
   * As with the generic URI syntax, the authority is composed of three parts:
   * <br>
   * <b>authority = [ userinfo "@" ] host [ ":" port ]</b>
   * <ul>
   * <li>userinfo (<b>optional</b>): name of the profile to be used by the AWS SDK</li>
   * <ul>
   * <li>Note: netCDF-Java uses the AWS SDK to manage credentials, even for non-AWS object stores.
   * One method for supplying credentials is through the use of a special credentials file, in which named profiles can
   * be used to manage multiple sets of credentials.
   * The profile name referenced here corresponds to a named profile in an AWS credentials file.
   * For more information, please see the
   * <a href="https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html#setting-credentials"> AWS
   * Documentation</a>.</li>
   * </ul>
   * <li>host (<b>required</b>): host name of the object store</li>
   * <ul>
   * <li>Note: If you need to supply a profile name when accessing an AWS S3 object, you must use the generic host name
   * AWS in order to have a valid URI.</li>
   * </ul>
   * <li>port(<b>optional</b>): default: 443</li>
   * </ul>
   * </li>
   * <li>path (<b>required</b>): path associated with the bucket</li>
   * <ul>
   * <li>may not be empty.</li>
   * <li>the final path segment is interpreted to be the name of the object stores bucket.</li>
   * <ul>
   * <li>if there are no path segments (no authority component and the path is relative), the path defines the bucket
   * name.</li>
   * </ul>
   * </ul>
   * <li>query (<b>required</b>): the object's key</li>
   * </div>
   * <div>
   * <p>
   * Example 1: cdms3://my-profile@bucket.unidata.ucar.edu:443/data/gfs0p25?26/03/2020/data.nc
   * <ul>
   * <li>scheme: cdms3</li>
   * <li>authority: my-profile@bucket.unidata.ucar.edu:443</li>
   * <ul>
   * <li>AWS Profile Name: my-profile</li>
   * </ul>
   * <li>path:/data/gfs0p25</li>
   * <ul>
   * <li>bucket: gfs0p25</li>
   * </ul>
   * <li>query (object key): 26/03/2020/data.nc</li>
   * </ul>
   * </p>
   * <p>
   * Example 2: cdms3:noaa-goes16?ABI-L1b-RadC/2017/242/00/data.nc
   * <ul>
   * <li>scheme: cdms3</li>
   * <li>authority: none</li>
   * <li>path: noaa-goes16</li>
   * <ul>
   * <li>bucket: noaa-goes16</li>
   * </ul>
   * <li>query (object key): ABI-L1b-RadC/2017/242/00/data.nc</li>
   * </ul>
   * </p>
   * <p>
   * Example cdms3 URIs:
   * <ul>
   * <li>cdms3://profile_name@my.endpoint.edu/endpoint/path/bucket-name?super/long/key</li>
   * <li>cdms3://profile_name@my.endpoint.edu/bucket-name?super/long/key</li>
   * <li>cdms3://my.endpoint.edu/endpoint/path/bucket-name?super/long/key</li>
   * <li>cdms3://my.endpoint.edu/bucket-name?super/long/key</li>
   * </ul>
   *
   * Secure HTTP access is assumed by default.
   * Insecure HTTP access is attempted when of the following ports is explicitly referenced in the authority portion of
   * the cdms3 URI:
   * <ul>
   * <li>80</li>
   * <li>8008</li>
   * <li>8080</li>
   * <li>7001 (WebLogic)</li>
   * <li>9080 (WebSphere)</li>
   * <li>16080 (Mac OS X Server)</li>
   * </ul>
   *
   * Example cdms3 URIs (specific to AWS S3):
   * <ul>
   * <li>cdms3:bucket-name?super/long/key</li>
   * <li>cdms3://profile_name@aws/bucket-name?super/long/key</li>
   * </ul>
   * Note: In order to supply a profile name (one way to set the region and/or credentials) while maintaining
   * conformance to the URI specification, you may use "aws" as the host.
   * When the generic "aws" host is used, netCDF-Java will ignore the host and allow the AWS SDK to set the appropriate
   * host based on region, as described in the <a href=
   * "https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/java-dg-region-selection.html#default-region-provider-chain">AWS
   * Documentation</a>.
   * </p>
   * </div>
   * 
   * @param cdmUriString String representation of the cdms3 URI
   *
   * @throws URISyntaxException
   *         If the given string violates RFC2396, as augmented
   *         by {@link URI}, or does not define the required URI
   *         components for a cdms3 URI.
   */
  public CdmS3Uri(String cdmUriString) throws URISyntaxException {
    // if cdmUriString isn't a valid URI to start with, this
    // will throw a URISyntaxException
    URI cdmS3Uri = new URI(cdmUriString);
    String scheme = cdmS3Uri.getScheme();

    if (scheme == null) {
      throw new URISyntaxException(cdmS3Uri.toString(),
          String.format("A CdmS3Uri must have a defined scheme (i.e. start with %s).", SCHEME_CDM_S3));
    }

    if (scheme.equalsIgnoreCase(SCHEME_CDM_S3_DEPRECATED)) {
      // In 5.3, we allowed s3://bucket/key. Very AWS Specific. Now we have the more generic cdms3.
      // Deprecate scheme: s3
      logger.warn("Use of the s3 scheme is deprecated. Please switch to the cdms3 scheme.");
      String updatedUri = String.format("cdms3:%s?%s", cdmS3Uri.getHost(), cdmS3Uri.getPath().substring(1));
      logger.warn(String.format("Using updated URI: %s", updatedUri));
      cdmS3Uri = new URI(updatedUri);
      scheme = cdmS3Uri.getScheme();
    }

    if (!scheme.equalsIgnoreCase(SCHEME_CDM_S3)) {
      throw new URISyntaxException(cdmS3Uri.toString(),
          String.format("A CdmS3Uri must use the %s scheme.", SCHEME_CDM_S3));
    }

    bucket = getBucketName(cdmS3Uri);
    key = getObjectKey(cdmS3Uri);
    profile = getProfile(cdmS3Uri);
    endpoint = getEndpoint(cdmS3Uri, bucket);
  }

  public String getBucket() {
    return bucket;
  }

  public String getKey() {
    return key;
  }

  public Optional<String> getProfile() {
    return Optional.ofNullable(profile);
  }

  public Optional<URI> getEndpoint() {
    return Optional.ofNullable(endpoint);
  }

  @Nullable
  private String getProfile(URI cdmUri) {
    String profile = null;
    if (cdmUri.getAuthority() != null) {
      profile = cdmUri.getRawUserInfo();
    }
    return profile;
  }

  @Nullable
  private URI getEndpoint(URI cdmUri, String bucketName) {
    URI s3Endpoint = null;
    // First, in order to have an endpoint, the cdms3 URI must have an authority section
    if (cdmUri.getAuthority() != null) {
      String host = cdmUri.getHost();
      // If a user is accessing an AWS bucket using a profile, e.g.
      // cdms3://profile_name@aws/bucket?key
      // the host will be a generic "AWS".
      // In this case, the AWS SDK handles the endpoint selection under the hood, so only keep
      // going if we are NOT dealing with the generic AWS endpoint
      if (!host.equalsIgnoreCase("aws")) {
        // try to figure out the scheme for the endpoint based on the specified port
        // We will assume https, so the switch will look for known http ports
        String scheme;
        boolean keepPort = true;
        int port = cdmUri.getPort();
        switch (port) {
          case -1: // default uri port is -1. Assume https, do not keep port
          case 443: // official registered https port, do not keep port
            keepPort = false;
            scheme = SCHEME_HTTPS;
            break;
          // official registered http port, do not keep the port
          case 80:
            keepPort = false;
            scheme = SCHEME_HTTP;
            break;
          // official registered alternatives to 80, keep the port
          case 8008:
          case 8080:
            // not registered, but standardized, specified or widely used, keep the port
          case 7001: // WebLogic
          case 9080: // WebSphere
          case 16080: // Mac OS X Server
            scheme = SCHEME_HTTP;
            break;
          // default: assume https enabled on the endpoint and keep the port
          default:
            scheme = SCHEME_HTTPS;
            break;
        }

        if (keepPort) {
          host = host + ":" + port;
        }

        String endpointPath = cdmUri.getPath();
        // could be /bucket-name, or /something/something2/bucket-name
        // the path for the endpoint uri would be everything up to the bucket name
        int endOfPath = endpointPath.lastIndexOf(bucketName);
        if (endOfPath > 0)
          endpointPath = endpointPath.substring(0, endOfPath);
        s3Endpoint = URI.create(scheme + "://" + host + endpointPath);
      }
    }
    return s3Endpoint;
  }

  private String getBucketName(URI cdmUri) throws URISyntaxException {
    String bucketName = null;

    if (cdmUri.getAuthority() != null) {
      String path = cdmUri.getPath();
      // bucket name is whatever comes after the last non-trailing slash of the path
      // trim trailing slash
      path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
      int bucketIndex = path.lastIndexOf('/');
      bucketName = bucketIndex >= 0 ? path.substring(bucketIndex + 1) : path;
    } else {
      // when there is no authority, the path and query make up the "Scheme Specific Part"
      // of the Java URI object. The bucket name will be everything before the first ?, which
      // separates the bucket name from the object key.
      String schemeSpecificPart = cdmUri.getSchemeSpecificPart();
      int bucketEndIndex = schemeSpecificPart.indexOf('?');
      if (bucketEndIndex >= 0) {
        bucketName = schemeSpecificPart.substring(0, bucketEndIndex);
      }
    }

    if (bucketName == null) {
      throw new URISyntaxException(cdmUri.toString(), "Cannot determine the Object Store bucket name (required).");
    }

    return bucketName;
  }

  private String getObjectKey(URI cdmUri) throws URISyntaxException {
    String key = null;
    if (cdmUri.getAuthority() != null) {
      key = cdmUri.getQuery();
    } else {
      // when there is no authority, the path and query make up the "Scheme Specific Part"
      // of the Java URI object. The key will be everything after the first ?, which
      // separates the bucket name from the object key.
      String schemeSpecificPart = cdmUri.getSchemeSpecificPart();
      int bucketEndIndex = schemeSpecificPart.indexOf('?');
      int queryStartIndex = bucketEndIndex + 1;
      if (bucketEndIndex >= 0 && queryStartIndex < schemeSpecificPart.length()) {
        key = schemeSpecificPart.substring(queryStartIndex);
      }
    }

    if (key == null) {
      throw new URISyntaxException(cdmUri.toString(), "Cannot find the Object's key (required).");
    }

    return key;
  }
}
