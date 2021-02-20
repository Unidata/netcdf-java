/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.s3;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.MFile;
import ucar.unidata.io.s3.S3TestsCommon;

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
  private static final String AWS_G16_S3_OBJECT_1 = S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_OBJECT_KEY_1;
  private static final String AWS_G16_S3_OBJECT_2 = S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_OBJECT_KEY_2;
  private static final String AWS_G16_S3_URI_DIR = S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_DIR;

  // Google Cloud Platform constants
  private static final String GCS_G16_S3_OBJECT_1 = S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_OBJECT_KEY_1;
  private static final String GCS_G16_S3_OBJECT_2 = S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_OBJECT_KEY_2;
  private static final String GCS_G16_S3_URI_DIR = S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_DIR;

  // Open Science Data Cloud Platform constants
  // The keys on OSDC are slightly different that on GCS or AWS, so they take a little more work...
  private static final String OSDC_G16_DIR = "ABI-L1b-RadC/" + parentDirName + "/" + dirName;
  private static final String OSDC_G16_OBJECT_KEY_1 = G16_OBJECT_KEY_1.replaceFirst(G16_DIR, OSDC_G16_DIR);
  private static final String OSDC_G16_S3_OBJECT_1 =
      S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + G16_OBJECT_KEY_1.replaceFirst(G16_DIR, OSDC_G16_DIR);
  private static final String OSDC_G16_S3_OBJECT_2 =
      S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + G16_OBJECT_KEY_2.replaceFirst(G16_DIR, OSDC_G16_DIR);
  private static final String OSDC_G16_S3_URI_DIR = S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + OSDC_G16_DIR;

  @BeforeClass
  public static void setup() {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
  }

  /////////////////////////////////////////
  // CdmS3Uri for just the bucket (no key)
  //
  @Test
  public void justBucketAws() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      String fullUri = S3TestsCommon.TOP_LEVEL_AWS_BUCKET + delimiter;
      checkWithBucket(fullUri);
    }
  }

  @Test
  public void justBucketGcs() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      String fullUri = S3TestsCommon.TOP_LEVEL_GCS_BUCKET + delimiter;
      checkWithBucket(fullUri);
    }
  }

  @Test
  public void justBucketOsdc() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      String fullUri = S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + delimiter;
      checkWithBucket(fullUri);
    }
  }

  //////////////////////////////////////////
  // CdmS3Uri bucket and key (valid object)
  //
  @Test
  @Ignore
  public void bucketAndKeyAws() throws IOException {
    long lastModified = 1532465845000L;
    checkWithBucketAndKey(AWS_G16_S3_OBJECT_1, G16_OBJECT_KEY_1, null, lastModified);
    checkWithBucketAndKey(AWS_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/", lastModified);
  }

  @Test
  @Ignore
  public void bucketAndKeyGcs() throws IOException {
    long lastModified = 1611593614000L;
    checkWithBucketAndKey(GCS_G16_S3_OBJECT_1, G16_OBJECT_KEY_1, null, lastModified);
    checkWithBucketAndKey(GCS_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/", lastModified);
  }

  @Test
  @Ignore
  public void bucketAndKeyOsdc() throws IOException {
    long lastModified = 1603308465000L;
    checkWithBucketAndKey(OSDC_G16_S3_OBJECT_1, OSDC_G16_OBJECT_KEY_1, null, lastModified);
    checkWithBucketAndKey(OSDC_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/", lastModified);
  }

  @Test
  public void dirCheckAws() throws IOException {
    dirCheckNoDelim(AWS_G16_S3_URI_DIR, G16_DIR);
    dirCheckDelim(AWS_G16_S3_URI_DIR + DELIMITER_FRAGMENT);
  }

  @Test
  public void dirCheckGcs() throws IOException {
    dirCheckNoDelim(GCS_G16_S3_URI_DIR, G16_DIR);
    dirCheckDelim(GCS_G16_S3_URI_DIR + DELIMITER_FRAGMENT);
  }

  @Test
  public void dirCheckOsdc() throws IOException {
    dirCheckNoDelim(OSDC_G16_S3_URI_DIR, OSDC_G16_DIR);
    dirCheckDelim(OSDC_G16_S3_URI_DIR + DELIMITER_FRAGMENT);
  }

  @Test
  public void compareMFilesAws() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      compareS3Mfiles(AWS_G16_S3_OBJECT_1 + delimiter, AWS_G16_S3_OBJECT_2 + delimiter);
    }
  }

  @Test
  public void compareMFilesGcs() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      compareS3Mfiles(GCS_G16_S3_OBJECT_1 + delimiter, GCS_G16_S3_OBJECT_2 + delimiter);
    }
  }

  @Test
  public void compareMFilesOsdc() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      compareS3Mfiles(OSDC_G16_S3_OBJECT_1 + delimiter, OSDC_G16_S3_OBJECT_2 + delimiter);
    }
  }

  @Test
  public void s3MFilesAuxInfoAws() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      checkS3MFilesAuxInfo(AWS_G16_S3_OBJECT_1 + delimiter);
    }
  }

  @Test
  public void s3MFilesAuxInfoGsc() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      checkS3MFilesAuxInfo(GCS_G16_S3_OBJECT_1 + delimiter);
    }
  }

  @Test
  public void s3MFilesAuxInfoOsdc() throws IOException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      checkS3MFilesAuxInfo(OSDC_G16_S3_OBJECT_1 + delimiter);
    }
  }

  private void checkWithBucket(String cdmS3Uri) throws IOException {
    logger.info("Checking {}", cdmS3Uri);
    MFile mFile = new MFileS3(cdmS3Uri);
    assertThat(mFile.getPath()).isEqualTo(cdmS3Uri);
    // Without a delimiter, the name is equal to the key. In this case, there is no key, so the name is empty
    assertThat(mFile.getName()).isEqualTo("");
    MFile parent = mFile.getParent();
    // Since we have do not have a delimiter, we should not have a parent.
    assertThat(parent).isNull();
  }

  private void checkWithBucketAndKey(String cdmS3Uri, String expectedName, String delimiter, long expectedLastModified)
      throws IOException {
    logger.info("Checking {}", cdmS3Uri);
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

  private void dirCheckNoDelim(String cdmS3Uri, String expectedName) throws IOException {
    MFile mFile = new MFileS3(cdmS3Uri);
    logger.info("Checking {}", cdmS3Uri);
    // The path is always the full cdms3 uri.
    assertThat(mFile.getPath()).isEqualTo(cdmS3Uri);
    // Without a delimiter, the name is the key.
    assertThat(mFile.getName()).isEqualTo(expectedName);
    // Without a delimiter, there is no parent.
    assertThat(mFile.getParent()).isNull();
    // Without a delimiter, there is no concept of a directory.
    assertThat(mFile.isDirectory()).isFalse();
  }

  private void dirCheckDelim(String cdmS3Uri) throws IOException {
    logger.info("Checking {}", cdmS3Uri);
    MFile mFile = new MFileS3(cdmS3Uri);
    assertThat(mFile.getPath()).isEqualTo(cdmS3Uri);
    // With a delimiter, the name is equal to the rightmost part of the path
    assertThat(mFile.getName()).isEqualTo(dirName);
    MFile parent = mFile.getParent();
    // Since we have a delimiter, and the object key contains the delimiter, we know this should not be null.
    assertThat(parent).isNotNull();
    assertThat(parent.getPath()).isEqualTo(cdmS3Uri.replace("/" + dirName, "/"));
    assertThat(parent.getName()).isEqualTo(parentDirName);
    assertThat(parent.isDirectory()).isTrue();
  }

  private void compareS3Mfiles(String uri1, String uri2) throws IOException {
    MFile mFile1 = new MFileS3(uri1);
    MFile mFile2 = new MFileS3(uri1);
    MFile mFile3 = new MFileS3(uri2);
    assert mFile1.equals(mFile2);
    assertThat(mFile1).isEqualTo(mFile2);
    assertThat(uri1).ignoringCase().isNotEqualTo(uri2);
    assertThat(mFile1).isNotEqualTo(mFile3);
  }

  private void checkS3MFilesAuxInfo(String uri) throws IOException {
    MFile mFile = new MFileS3(uri);
    mFile.setAuxInfo("Aux Info");
    Object auxInfo = mFile.getAuxInfo();
    assertThat(auxInfo.toString()).isEqualTo("Aux Info");
    assertThat(auxInfo.toString()).isNotEqualTo("aux info");
    assertThat(auxInfo.toString()).isNotEqualTo("Ox Info");
  }

  @AfterClass
  public static void teardown() {
    System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
  }

}
