package thredds.inventory.s3;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import thredds.inventory.MFile;
import ucar.unidata.io.s3.TestS3Read;

public class TestMFileS3 {

  private static final Logger logger = LoggerFactory.getLogger(TestMFileS3.class);

  private static final String parentDirName = "242";
  private static final String dirName = "00";
  private static final String G16_DIR = "ABI-L1b-RadC/2017/" + parentDirName + "/" + dirName;
  private static final String G16_NAME_1 =
      "OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";
  private static final String G16_NAME_2 =
      "OR_ABI-L1b-RadC-M3C01_G16_s20172420012168_e20172420014540_c20172420014583.nc";
  private static final String G16_OBJECT_KEY_1 = G16_DIR + "/" + G16_NAME_1;
  private static final String G16_OBJECT_KEY_2 = G16_DIR + "/" + G16_NAME_2;
  private static final int G16_OBJECT_1_SIZE = 7979480;

  private static final String DELIMITER_FRAGMENT = "#delimiter=/";
  private static final String[] DELIMITER_FRAGMENTS = new String[] {"", DELIMITER_FRAGMENT};

  // AWS constants
  private static final String TOP_LEVEL_AWS_BUCKET = "cdms3:noaa-goes16";
  private static final String AWS_G16_S3_OBJECT_1 = TOP_LEVEL_AWS_BUCKET + "?" + G16_OBJECT_KEY_1;
  private static final String AWS_G16_S3_OBJECT_2 = TOP_LEVEL_AWS_BUCKET + "?" + G16_OBJECT_KEY_2;
  private static final String AWS_G16_S3_URI_DIR = TOP_LEVEL_AWS_BUCKET + "?" + G16_DIR;

  private static final String AWS_REGION_PROP_NAME = "aws.region";
  private static final String AWS_G16_REGION = Region.US_EAST_1.toString();

  // Google Cloud Platform constants
  private static final String TOP_LEVEL_GCS_BUCKET = "cdms3://storage.googleapis.com/gcp-public-data-goes-16";
  private static final String GCS_G16_S3_OBJECT_1 = TOP_LEVEL_GCS_BUCKET + "?" + G16_OBJECT_KEY_1;
  private static final String GCS_G16_S3_OBJECT_2 = TOP_LEVEL_GCS_BUCKET + "?" + G16_OBJECT_KEY_2;
  private static final String GCS_G16_S3_URI_DIR = TOP_LEVEL_GCS_BUCKET + "?" + G16_DIR;

  // Open Science Data Cloud Platform constants
  private static final String TOP_LEVEL_OSDC_BUCKET =
      "cdms3://griffin-objstore.opensciencedatacloud.org/noaa-goes16-hurricane-archive-2017";
  private static final String OSDC_G16_DIR = "ABI-L1b-RadC/" + parentDirName + "/" + dirName;
  private static final String OSDC_G16_OBJECT_KEY_1 = G16_OBJECT_KEY_1.replaceFirst(G16_DIR, OSDC_G16_DIR);
  private static final String OSDC_G16_OBJECT_KEY_2 = G16_OBJECT_KEY_2.replaceFirst(G16_DIR, OSDC_G16_DIR);
  private static final String OSDC_G16_S3_OBJECT_1 =
      TOP_LEVEL_OSDC_BUCKET + "?" + G16_OBJECT_KEY_1.replaceFirst(G16_DIR, OSDC_G16_DIR);
  private static final String OSDC_G16_S3_OBJECT_2 =
      TOP_LEVEL_OSDC_BUCKET + "?" + G16_OBJECT_KEY_2.replaceFirst(G16_DIR, OSDC_G16_DIR);
  private static final String OSDC_G16_S3_URI_DIR = TOP_LEVEL_OSDC_BUCKET + "?" + OSDC_G16_DIR;

  @BeforeClass
  public static void setup() throws URISyntaxException, IOException {
    System.setProperty(AWS_REGION_PROP_NAME, AWS_G16_REGION);
  }

  /////////////////////////////////////////
  // CdmS3Uri for just the bucket (no key)
  //
  @Test
  public void justBucketAws() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      String fullUri = TOP_LEVEL_AWS_BUCKET + DELIMITER_FRAGMENT;
      testJustBucket(fullUri);
    }
  }

  @Test
  public void justBucketGcs() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      String fullUri = TOP_LEVEL_GCS_BUCKET + DELIMITER_FRAGMENT;
      testJustBucket(fullUri);
    }
  }

  @Test
  public void justBucketOsdc() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      String fullUri = TOP_LEVEL_OSDC_BUCKET + DELIMITER_FRAGMENT;
      testJustBucket(fullUri);
    }
  }

  //////////////////////////////////////////
  // CdmS3Uri bucket and key (valid object)
  //
  @Test
  public void bucketAndKeyAws() throws IOException {
    long lastModified = 1532465845000L;
    testBucketAndKey(AWS_G16_S3_OBJECT_1, G16_OBJECT_KEY_1, null, lastModified);
    testBucketAndKey(AWS_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/", lastModified);
  }

  @Test
  public void bucketAndKeyGcs() throws IOException {
    long lastModified = 1504051532000L;
    testBucketAndKey(GCS_G16_S3_OBJECT_1, G16_OBJECT_KEY_1, null, lastModified);
    testBucketAndKey(GCS_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/", lastModified);
  }

  @Test
  public void bucketAndKeyOsdc() throws IOException {
    long lastModified = 1603308465000L;
    testBucketAndKey(OSDC_G16_S3_OBJECT_1, OSDC_G16_OBJECT_KEY_1, null, lastModified);
    testBucketAndKey(OSDC_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/", lastModified);
  }

  @Test
  public void dirCheckNoDelim() throws IOException {
    MFile mFile = new MFileS3(AWS_G16_S3_URI_DIR);
    // The path is always the full cdms3 uri.
    assertThat(mFile.getPath()).isEqualTo(AWS_G16_S3_URI_DIR);
    // Without a delimiter, the name is the key.
    assertThat(mFile.getName()).isEqualTo(G16_DIR);
    // Without a delimiter, there is no parent.
    assertThat(mFile.getParent()).isNull();
    // Without a delimiter, there is no concept of a directory.
    assertThat(mFile.isDirectory()).isFalse();
  }

  @Test
  public void dirCheckDelim() throws IOException {
    String fullCdmS3Uri = AWS_G16_S3_URI_DIR + DELIMITER_FRAGMENT;
    MFile mFile = new MFileS3(fullCdmS3Uri);
    assertThat(mFile.getPath()).isEqualTo(fullCdmS3Uri);
    // With a delimiter, the name is equal to the rightmost part of the path
    assertThat(mFile.getName()).isEqualTo(dirName);
    MFile parent = mFile.getParent();
    // Since we have a delimiter, and the object key contains the delimiter, we know this should not be null.
    assertThat(parent).isNotNull();
    assertThat(parent.getName()).isEqualTo(parentDirName);
    assertThat(parent.isDirectory()).isTrue();
  }

  @Test
  public void compareS3MFiles() throws IOException {
    MFile mFile1 = new MFileS3(AWS_G16_S3_OBJECT_1);
    MFile mFile2 = new MFileS3(AWS_G16_S3_OBJECT_1);
    MFile mFile3 = new MFileS3(AWS_G16_S3_OBJECT_2);
    assert mFile1.equals(mFile2);
    assertThat(mFile1).isEqualTo(mFile2);
    assertThat(AWS_G16_S3_OBJECT_1).ignoringCase().isNotEqualTo(AWS_G16_S3_OBJECT_2);
    assertThat(mFile1).isNotEqualTo(mFile3);
  }

  @Test
  public void s3MFilesAuxInfo() throws IOException {
    MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1);
    mFile.setAuxInfo("Hello");
    Object auxInfo = mFile.getAuxInfo();
    assertThat(auxInfo.toString()).isEqualTo("Hello");
    assertThat(auxInfo.toString()).isNotEqualTo("hello");
  }

  private String getLastPathSegment(String fullPath, String delimiter) {
    int lastDelimiter = fullPath.lastIndexOf(delimiter);
    return fullPath.substring(lastDelimiter + delimiter.length(), fullPath.length());
  }

  private void testJustBucket(String cdmS3Uri) throws IOException {
    MFile mFile = new MFileS3(cdmS3Uri);
    assertThat(mFile.getPath()).isEqualTo(cdmS3Uri);
    // Without a delimiter, the name is equal to the key. In this case, there is no key, so the name is empty
    assertThat(mFile.getName()).isEqualTo("");
    MFile parent = mFile.getParent();
    // Since we have do not have a delimiter, we should not have a parent.
    assertThat(parent).isNull();
  }

  private void testBucketAndKey(String cdmS3Uri, String expectedName, String delimiter, long expectedLastModified)
      throws IOException {
    MFile mFile = new MFileS3(cdmS3Uri);
    assertThat(mFile.getPath()).isEqualTo(cdmS3Uri);
    assertThat(mFile.getName()).isEqualTo(expectedName);

    if (delimiter != null) {
      assertThat(mFile.getParent()).isNotNull();
    } else {
      assertThat(mFile.getParent()).isNull();
    }
    assertThat(mFile.isDirectory()).isFalse();
    assertThat(mFile.getLastModified()).isEqualTo(expectedLastModified);
    assertThat(mFile.getLength()).isEqualTo(G16_OBJECT_1_SIZE);
  }

  private void dirCheck(String cdmS3Uri, String expectedName) throws IOException {
    MFile mFile = new MFileS3(cdmS3Uri);
    // The path is always the full cdms3 uri.
    assertThat(mFile.getPath()).isEqualTo(cdmS3Uri);
    // Without a delimiter, the name is the key.
    assertThat(mFile.getName()).isEqualTo(expectedName);
    // Without a delimiter, there is no parent.
    assertThat(mFile.getParent()).isNull();
    // Without a delimiter, there is no concept of a directory.
    assertThat(mFile.isDirectory()).isFalse();
  }

  private void dirCheck(String cdmS3Uri, String expectedName, String expectedParentPath, String expectedParentName)
      throws IOException {
    MFile mFile = new MFileS3(cdmS3Uri);
    assertThat(mFile.getPath()).isEqualTo(cdmS3Uri);
    // With a delimiter, the name is equal to the rightmost part of the path
    assertThat(mFile.getName()).isEqualTo(expectedName);
    MFile parent = mFile.getParent();
    // Since we have a delimiter, and the object key contains the delimiter, we know this should not be null.
    assertThat(parent).isNotNull();
    assertThat(parent.getPath()).isEqualTo(expectedParentPath);
    assertThat(parent.getName()).isEqualTo(expectedParentName);
    assertThat(parent.isDirectory()).isTrue();
  }

  @AfterClass
  public static void teardown() {
    System.clearProperty(AWS_REGION_PROP_NAME);
  }

}
