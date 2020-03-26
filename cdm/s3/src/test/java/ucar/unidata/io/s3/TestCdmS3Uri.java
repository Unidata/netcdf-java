package ucar.unidata.io.s3;

import static com.google.common.truth.Truth.assertThat;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCdmS3Uri {

  private static final Logger logger = LoggerFactory.getLogger(TestCdmS3Uri.class);

  private static final String user_info = "profile_name";
  private static final String host = "my.endpoint.edu";
  private static final String endpoint_segments = "endpoint/path/";
  private static final String bucket_name = "bucket-name";
  private static final String query = "super/long/key";

  private static final String schemeHttp = "http";
  private static final String schemeHttps = "https";
  private static final String schemeCdmS3 = "cdms3";

  @Test
  public void testAwsMinimumDep() throws URISyntaxException {
    // cdms3:/bucket/key
    String cdmS3Uri = "s3://" + bucket_name + "/" + query;
    testUri(cdmS3Uri);
  }

  @Test
  public void testAwsMinimum() throws URISyntaxException {
    // cdms3:/bucket/key
    String cdmS3Uri = schemeCdmS3 + ":" + bucket_name + "?" + query;
    testUri(cdmS3Uri);
  }

  @Test
  public void testAwsWithProfile() throws URISyntaxException {
    // cdms3://profile@aws/bucket/key
    String cdmS3Uri = schemeCdmS3 + "://" + user_info + "@aws" + "/" + bucket_name + "?" + query;
    testUri(cdmS3Uri);
  }

  @Test
  public void testWithHost() throws URISyntaxException {
    // cdms3://host/bucket/key
    String cdmS3Uri = schemeCdmS3 + "://" + host + "%s/" + bucket_name + "?" + query;
    String expectedEndpoint = host + "%s/";
    gauntlet(cdmS3Uri, expectedEndpoint);
  }

  @Test
  public void testWithHostAndEndpointPath() throws URISyntaxException {
    // cdms3://host/path/bucket/key
    String cdmS3Uri = schemeCdmS3 + "://" + host + "%s/" + endpoint_segments + bucket_name + "?" + query;
    String expectedEndpoint = host + "%s/" + endpoint_segments;
    gauntlet(cdmS3Uri, expectedEndpoint);
  }

  @Test
  public void testWithHostAndProfile() throws URISyntaxException {
    // cdms3://profile@host/bucket/key
    String cdmS3Uri = schemeCdmS3 + "://" + user_info + "@" + host + "%s/" + bucket_name + "?" + query;
    String expectedEndpoint = host + "%s/";
    gauntlet(cdmS3Uri, expectedEndpoint);
  }

  @Test
  public void testFull() throws URISyntaxException {
    // cdms3://profile@host/path/bucket/key
    String cdmS3Uri =
        schemeCdmS3 + "://" + user_info + "@" + host + "%s/" + endpoint_segments + bucket_name + "?" + query;
    String expectedEndpoint = host + "%s/" + endpoint_segments;
    gauntlet(cdmS3Uri, expectedEndpoint);
  }

  //////////////////
  // Reusable Testers

  public void gauntlet(String cdmS3UriString, String expectedEndpoint) throws URISyntaxException {
    testStandardHttps(cdmS3UriString, expectedEndpoint);
    testExplicitHttps(cdmS3UriString, expectedEndpoint);
    testStandardHttp(cdmS3UriString, expectedEndpoint);
    testExplicitHttp(cdmS3UriString, expectedEndpoint);
  }

  public void testStandardHttps(String cdmS3UriF, String expectedEndpointF) throws URISyntaxException {
    String[] implicitHttpsPorts = new String[] {"", ":443"};
    for (String port : implicitHttpsPorts) {
      String cdmS3Uri = String.format(cdmS3UriF, port);
      String expectedEndpoint = schemeHttps + "://" + String.format(expectedEndpointF, "");
      testUri(cdmS3Uri, expectedEndpoint);
    }
  }

  public void testExplicitHttps(String cdmS3UriF, String expectedEndpointF) throws URISyntaxException {
    String[] implicitHttpsPorts = new String[] {":8443", ":63532"};
    for (String port : implicitHttpsPorts) {
      String cdmS3Uri = String.format(cdmS3UriF, port);
      String expectedEndpoint = schemeHttps + "://" + String.format(expectedEndpointF, port);
      testUri(cdmS3Uri, expectedEndpoint);
    }
  }

  public void testStandardHttp(String cdmS3UriF, String expectedEndpointF) throws URISyntaxException {
    String cdmS3Uri = String.format(cdmS3UriF, ":80");
    String expectedEndpoint = schemeHttp + "://" + String.format(expectedEndpointF, "");
    testUri(cdmS3Uri, expectedEndpoint);
  }

  public void testExplicitHttp(String cdmS3UriF, String expectedEndpointF) throws URISyntaxException {
    String[] explicitHttpPorts = new String[] {"8080", "7001", "9080", "16080"};
    for (String port : explicitHttpPorts) {
      String cdmS3Uri = String.format(cdmS3UriF, ":" + port);
      String expectedEndpoint = schemeHttp + "://" + String.format(expectedEndpointF, ":" + port);
      testUri(cdmS3Uri, expectedEndpoint);
    }
  }

  public void testUri(String cdmS3UriString) throws URISyntaxException {
    testUri(cdmS3UriString, null);
  }

  public void testUri(String cdmS3UriString, String expectedEndpoint) throws URISyntaxException {
    logger.debug(cdmS3UriString);
    CdmS3Uri cdmS3Uri = new CdmS3Uri(cdmS3UriString);
    testBucket(cdmS3Uri);
    testKey(cdmS3Uri);
    testProfile(cdmS3Uri);
    if (expectedEndpoint != null) {
      testEndpoint(cdmS3Uri, URI.create(expectedEndpoint));
    }
  }

  public void testBucket(CdmS3Uri cdmS3Uri) {
    assertThat(cdmS3Uri.getBucket()).isEqualTo(bucket_name);
  }

  public void testKey(CdmS3Uri cdmS3Uri) {
    assertThat(cdmS3Uri.getKey()).isEqualTo(query);
  }

  public void testProfile(CdmS3Uri cdmS3Uri) {
    Optional<String> cdmS3Profile = cdmS3Uri.getProfile();
    cdmS3Profile.ifPresent(s -> assertThat(s).isEqualTo(user_info));
  }

  public void testEndpoint(CdmS3Uri cdmS3Uri, URI expectedEndpoint) {
    Optional<URI> actualEndpoint = cdmS3Uri.getEndpoint();
    actualEndpoint.ifPresent(uri -> assertThat(uri).isEqualTo(expectedEndpoint));
  }
}
