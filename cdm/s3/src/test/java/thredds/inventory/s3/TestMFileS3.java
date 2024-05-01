/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import thredds.filesystem.MFileOS;
import thredds.inventory.MFile;
import thredds.inventory.s3.MFileS3.Provider;
import ucar.unidata.io.s3.CdmS3Uri;
import ucar.unidata.io.s3.S3TestsCommon;
import ucar.unidata.util.test.category.NotPullRequest;

public class TestMFileS3 {

  private static final Logger logger = LoggerFactory.getLogger(TestMFileS3.class);

  private static final String parentDirName = "242";
  private static final String dirName = "00";
  private static final String topLevelDir = "ABI-L1b-RadC";
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
  private static final String AWS_G16_S3_URI_TOP_DIR = S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + topLevelDir;

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
  public void bucketAndKeyAws() throws IOException {
    checkWithBucketAndKey(AWS_G16_S3_OBJECT_1, G16_OBJECT_KEY_1, null);
    checkWithBucketAndKey(AWS_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/");
  }

  @Test
  public void bucketAndKeyGcs() throws IOException {
    checkWithBucketAndKey(GCS_G16_S3_OBJECT_1, G16_OBJECT_KEY_1, null);
    checkWithBucketAndKey(GCS_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/");
  }

  @Ignore("Failing due to expired certificate on OSDC")
  @Test
  @Category(NotPullRequest.class)
  public void bucketAndKeyOsdc() throws IOException {
    checkWithBucketAndKey(OSDC_G16_S3_OBJECT_1, OSDC_G16_OBJECT_KEY_1, null);
    checkWithBucketAndKey(OSDC_G16_S3_OBJECT_1 + DELIMITER_FRAGMENT, G16_NAME_1, "/");
  }

  @Test
  public void dirCheckAws() throws IOException {
    dirCheckNoDelim(AWS_G16_S3_URI_DIR, G16_DIR);
    dirCheckDelim(AWS_G16_S3_URI_DIR + DELIMITER_FRAGMENT);
    dirCheckDelim(AWS_G16_S3_URI_DIR + "/" + DELIMITER_FRAGMENT);
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
  public void shouldReturnTopLevelKeyName() throws IOException {
    final MFileS3 fileWithoutDelimiter = new MFileS3(AWS_G16_S3_URI_TOP_DIR);
    assertThat(fileWithoutDelimiter.getName()).isEqualTo(topLevelDir);

    final MFileS3 fileWithDelimiter = new MFileS3(AWS_G16_S3_URI_TOP_DIR + DELIMITER_FRAGMENT);
    assertThat(fileWithDelimiter.getName()).isEqualTo(topLevelDir);
  }

  @Test
  public void shouldCompareSameMFile() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1);
    assertThat(mFile.equals(mFile)).isTrue();
    assertThat(mFile.compareTo(mFile)).isEqualTo(0);
  }

  @Test
  public void shouldCompareToDifferentClass() throws IOException {
    final MFile mFile1 = new MFileS3(AWS_G16_S3_OBJECT_1);
    final MFile mFile2 = new MFileOS("test");
    assertThat(mFile1.equals(mFile2)).isFalse();
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

  @Ignore("Failing due to expired certificate on OSDC")
  @Test
  public void shouldWriteObjectsToStream() throws IOException {
    final String[] objects = {AWS_G16_S3_OBJECT_1, GCS_G16_S3_OBJECT_1, OSDC_G16_S3_OBJECT_1};

    for (String object : objects) {
      final MFile mFile = new MFileS3(object);
      final long length = mFile.getLength();

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mFile.writeToStream(outputStream);
      assertThat(outputStream.size()).isEqualTo(length);
    }
  }

  @Test
  public void shouldWritePartialObjectToStream() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1);
    final long length = mFile.getLength();

    final long[][] testCases = {{0, 0}, {10, 10}, {0, length}, {0, 100}, {42, 100}};

    for (long[] testCase : testCases) {
      final long offset = testCase[0];
      final long maxBytes = testCase[1];

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mFile.writeToStream(outputStream, offset, maxBytes);

      final long bytesWritten = Math.min(maxBytes, length - offset);
      assertThat(outputStream.size()).isEqualTo(bytesWritten);
    }
  }

  @Test
  public void shouldNotWriteDirectoryToStream() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_URI_DIR + "/" + DELIMITER_FRAGMENT);
    assertThat(mFile.isDirectory()).isTrue();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThrows(NoSuchKeyException.class, () -> mFile.writeToStream(outputStream));
  }

  @Test
  public void shouldNotWriteNonExistingObjectToStream() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_URI_DIR + "/NotARealKey");

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThrows(NoSuchKeyException.class, () -> mFile.writeToStream(outputStream));
  }

  @Test
  public void shouldReturnTrueForExistingFile() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1);
    assertThat(mFile.exists()).isEqualTo(true);
  }

  @Test
  public void shouldReturnFalseForNonExistingFile() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_URI_DIR + "/NotARealKey");
    assertThat(mFile.exists()).isEqualTo(false);
  }

  @Test
  public void shouldCheckExistsForExistingDirectory() throws IOException {
    final MFile withFragmentWithSlash = new MFileS3(AWS_G16_S3_URI_DIR + "/" + DELIMITER_FRAGMENT);
    assertThat(withFragmentWithSlash.exists()).isEqualTo(true);

    final MFile withoutFragmentWithSlash = new MFileS3(AWS_G16_S3_URI_DIR + "/");
    assertThat(withoutFragmentWithSlash.exists()).isEqualTo(false);

    final MFile withFragmentWithoutSlash = new MFileS3(AWS_G16_S3_URI_DIR + DELIMITER_FRAGMENT);
    assertThat(withFragmentWithoutSlash.exists()).isEqualTo(false);

    final MFile withoutFragmentWithoutSlash = new MFileS3(AWS_G16_S3_URI_DIR);
    assertThat(withoutFragmentWithoutSlash.exists()).isEqualTo(false);
  }

  @Test
  public void shouldReturnFalseForNonExistingDirectory() throws IOException {
    final MFile withFragmentWithSlash = new MFileS3(AWS_G16_S3_URI_DIR + "/notADirectory/" + DELIMITER_FRAGMENT);
    assertThat(withFragmentWithSlash.exists()).isEqualTo(false);

    final MFile withoutFragmentWithSlash = new MFileS3(AWS_G16_S3_URI_DIR + "/notADirectory/");
    assertThat(withoutFragmentWithSlash.exists()).isEqualTo(false);

    final MFile withFragmentWithoutSlash = new MFileS3(AWS_G16_S3_URI_DIR + "/notADirectory" + DELIMITER_FRAGMENT);
    assertThat(withFragmentWithoutSlash.exists()).isEqualTo(false);

    final MFile withoutFragmentWithoutSlash = new MFileS3(AWS_G16_S3_URI_DIR + "/notADirectory");
    assertThat(withoutFragmentWithoutSlash.exists()).isEqualTo(false);
  }

  @Test
  public void shouldReturnFalseForKeyPrefixMatch() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1.substring(0, AWS_G16_S3_OBJECT_1.length() - 5));
    assertThat(mFile.exists()).isEqualTo(false);
  }

  @Test
  public void shouldReturnTrueForBucket() throws IOException {
    final MFile bucketWithDelimiter = new MFileS3(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + DELIMITER_FRAGMENT);
    assertThat(bucketWithDelimiter.exists()).isEqualTo(true);

    final MFile bucketWithoutDelimiter = new MFileS3(S3TestsCommon.TOP_LEVEL_AWS_BUCKET);
    assertThat(bucketWithoutDelimiter.exists()).isEqualTo(true);
  }

  @Test
  public void shouldReturnFalseForNonExistentBucket() throws IOException {
    final MFile bucketWithDelimiter = new MFileS3("cdms3:notABucket" + DELIMITER_FRAGMENT);
    assertThat(bucketWithDelimiter.exists()).isEqualTo(false);

    final MFile bucketWithoutDelimiter = new MFileS3("cdms3:notABucket");
    assertThat(bucketWithoutDelimiter.exists()).isEqualTo(false);
  }

  @Test
  public void shouldGetChildMFileFromBucket() throws IOException {
    final MFileS3 withDelimiter = new MFileS3("cdms3:bucket" + DELIMITER_FRAGMENT);
    final MFileS3 newMFileWithDelimiter = withDelimiter.getChild("newKey");
    assertThat(newMFileWithDelimiter).isNotNull();
    assertThat(newMFileWithDelimiter.getPath()).isEqualTo("cdms3:bucket?newKey" + DELIMITER_FRAGMENT);

    final MFileS3 withoutDelimiter = new MFileS3("cdms3:bucket");
    final MFileS3 newMFileWithoutDelimiter = withoutDelimiter.getChild("newKey");
    assertThat(newMFileWithoutDelimiter).isNotNull();
    assertThat(newMFileWithoutDelimiter.getPath()).isEqualTo("cdms3:bucket?newKey");
  }

  @Test
  public void shouldGetChildMFileFromBucketAndKey() throws IOException {
    final MFileS3 withDelimiterWithSlash = new MFileS3("cdms3:bucket?key/" + DELIMITER_FRAGMENT);
    final MFileS3 newMFileWithDelimiterWithSlash = withDelimiterWithSlash.getChild("newKey");
    assertThat(newMFileWithDelimiterWithSlash).isNotNull();
    assertThat(newMFileWithDelimiterWithSlash.getPath()).isEqualTo("cdms3:bucket?key/newKey" + DELIMITER_FRAGMENT);

    final MFileS3 withoutDelimiterWithSlash = new MFileS3("cdms3:bucket?key/");
    final MFileS3 newMFileWithoutDelimiterWithSlash = withoutDelimiterWithSlash.getChild("newKey");
    assertThat(newMFileWithoutDelimiterWithSlash).isNotNull();
    assertThat(newMFileWithoutDelimiterWithSlash.getPath()).isEqualTo("cdms3:bucket?key/newKey");

    final MFileS3 withDelimiterWithoutSlash = new MFileS3("cdms3:bucket?key" + DELIMITER_FRAGMENT);
    final MFileS3 newMFileWithDelimiterWithoutSlash = withDelimiterWithoutSlash.getChild("newKey");
    assertThat(newMFileWithDelimiterWithoutSlash).isNotNull();
    assertThat(newMFileWithDelimiterWithoutSlash.getPath()).isEqualTo("cdms3:bucket?key/newKey" + DELIMITER_FRAGMENT);

    final MFileS3 withoutDelimiterWithoutSlash = new MFileS3("cdms3:bucket?key");
    final MFileS3 newMFileWithoutDelimiterWithoutSlash = withoutDelimiterWithoutSlash.getChild("newKey");
    assertThat(newMFileWithoutDelimiterWithoutSlash).isNotNull();
    assertThat(newMFileWithoutDelimiterWithoutSlash.getPath()).isEqualTo("cdms3:bucket?keynewKey");
  }

  @Test
  public void shouldGetInputStream() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1);
    try (final InputStream inputStream = mFile.getInputStream()) {
      assertThat(inputStream.read()).isNotEqualTo(-1);
    }
  }


  @Test
  public void shouldGetLastModifiedForExistingFile() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1);
    assertThat(mFile.getLastModified()).isGreaterThan(0);

    final MFile mFile2 = new MFileS3(AWS_G16_S3_OBJECT_1, 0, -1);
    assertThat(mFile2.getLastModified()).isGreaterThan(0);

    final MFile mFile3 = new MFileS3(AWS_G16_S3_OBJECT_1, 0, 1);
    assertThat(mFile3.getLastModified()).isEqualTo(1);
  }

  @Test
  public void shouldThrowForGetLastModifiedOnNonExistingFile() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_URI_DIR + "/NotARealKey");
    assertThrows(NoSuchKeyException.class, mFile::getLastModified);
  }

  @Test
  public void shouldGetLengthForExistingFile() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_OBJECT_1);
    assertThat(mFile.getLength()).isGreaterThan(0);

    final MFile mFile2 = new MFileS3(AWS_G16_S3_OBJECT_1, -1, 0);
    assertThat(mFile2.getLength()).isGreaterThan(0);

    final MFile mFile3 = new MFileS3(AWS_G16_S3_OBJECT_1, 1, 0);
    assertThat(mFile3.getLength()).isEqualTo(1);
  }

  @Test
  public void shouldThrowForGetLengthOnNonExistingFile() throws IOException {
    final MFile mFile = new MFileS3(AWS_G16_S3_URI_DIR + "/NotARealKey");
    assertThrows(NoSuchKeyException.class, mFile::getLength);
  }

  @Test
  public void shouldGetProtocol() {
    assertThat(new Provider().getProtocol()).isEqualTo("cdms3");
  }

  @Test
  public void shouldCreateMFile() throws IOException {
    final MFile mFile = new Provider().create(AWS_G16_S3_OBJECT_1);
    assertThat(mFile.exists()).isTrue();
  }

  @Test
  public void shouldCreateMFileUsingCdms3Uri() throws URISyntaxException {
    final MFile mFile = new MFileS3(new CdmS3Uri(AWS_G16_S3_OBJECT_1));
    assertThat(mFile.exists()).isTrue();
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

  private void checkWithBucketAndKey(String cdmS3Uri, String expectedName, String delimiter) throws IOException {
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
    assertThat(parent.getPath())
        .isEqualTo(cdmS3Uri.replace("/" + dirName, "/").replace(parentDirName + "//", parentDirName + "/"));
    assertThat(parent.getName()).isEqualTo(parentDirName);
    assertThat(parent.isDirectory()).isTrue();
  }

  private void compareS3Mfiles(String uri1, String uri2) throws IOException {
    MFile mFile1 = new MFileS3(uri1);
    MFile mFile2 = new MFileS3(uri1);
    MFile mFile3 = new MFileS3(uri2);
    assert mFile1.equals(mFile2);
    assertThat(mFile1).isEqualTo(mFile2);
    assertThat(mFile1.compareTo(mFile2)).isEqualTo(0);
    assertThat(mFile1.hashCode()).isEqualTo(mFile2.hashCode());
    assertThat(uri1).ignoringCase().isNotEqualTo(uri2);
    assertThat(mFile1).isNotEqualTo(mFile3);
    assertThat(mFile1.compareTo(mFile3)).isNotEqualTo(0);
    assertThat(mFile1.hashCode()).isNotEqualTo(mFile3.hashCode());
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
