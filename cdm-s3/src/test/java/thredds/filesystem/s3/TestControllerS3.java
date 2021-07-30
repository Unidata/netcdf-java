/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.filesystem.s3;

import static com.google.common.truth.Truth.assertThat;

import java.net.URISyntaxException;
import java.util.Iterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import thredds.inventory.CollectionConfig;
import thredds.inventory.MFile;
import ucar.unidata.io.s3.CdmS3Uri;
import ucar.unidata.io.s3.S3TestsCommon;
import ucar.unidata.io.s3.TestS3Read;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.category.NotPullRequest;

/**
 * Tests for ControllerS3 using AWS, GCS, and OSDC object stores
 */
@Category(NeedsExternalResource.class)
public class TestControllerS3 {

  private static final Logger logger = LoggerFactory.getLogger(TestS3Read.class);

  // prefix for all data from 2017 day 242
  private static final String G16_KEY_SINGLE_DAY = "ABI-L1b-RadC/2017/242/";
  // prefix for all data from 2017 day 242 hour 0
  private static final String G16_KEY_PREFIX_SINGLE_DAY_PARTIAL_FILE =
      G16_KEY_SINGLE_DAY + "00/OR_ABI-L1b-RadC-M3C03_G16_s201724200";
  // prefix that uniquely matches a single file from 2017 day 242 hour 0
  private static final String G16_KEY_PREFIX_SINGLE_MATCH =
      G16_KEY_SINGLE_DAY + "00/OR_ABI-L1b-RadC-M3C03_G16_s20172420057168";

  private static final String DELIMITER_FRAGMENT = "#delimiter=/";
  private static final String[] DELIMITER_FRAGMENTS = new String[] {"", DELIMITER_FRAGMENT};
  private static final String AWS_REGION_PROP_NAME = "aws.region";
  private static final String AWS_G16_REGION = Region.US_EAST_1.toString();

  // The maximum number of objects reported from a bucket (currently 2000) to keep tests from spinning for an
  // incredibly long time while trying to list the entire GOES-16 archive
  private static final int LIMIT_COUNT_MAX = ControllerS3.LIMIT_COUNT_MAX;

  // Controls if the countObjects method should print out the key values
  private static final boolean PRINT;

  static {
    PRINT = logger.isDebugEnabled();
  }

  @BeforeClass
  public static void setup() {
    System.setProperty(AWS_REGION_PROP_NAME, AWS_G16_REGION);

  }

  //////////////////////
  // getInventoryAll() tests
  //
  @Test
  public void testGetInventoryTopBucketNoDelimiterAws() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET);
    checkInventoryTopCountExact(uri, LIMIT_COUNT_MAX);
  }

  @Test
  public void testGetInventoryTopBucketNoDelimiterGcs() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET);
    checkInventoryTopCountExact(uri, LIMIT_COUNT_MAX);
  }

  @Test
  public void testGetInventoryTopBucketGcsNoDelimiterOsdc() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_OSDC_BUCKET);
    checkInventoryTopCountExact(uri, LIMIT_COUNT_MAX);
  }

  @Test
  @Category(NotPullRequest.class)
  public void testGetInventoryTopBucketDelimiterAws() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + DELIMITER_FRAGMENT);
    // contains a single object at "/" (/index.html)
    checkInventoryTopCountAtMost(uri, 3);
  }

  @Test
  @Category(NotPullRequest.class)
  public void testGetInventoryTopBucketDelimiterGcs() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + DELIMITER_FRAGMENT);
    // does not contain anything at "/"
    checkInventoryTopCountExact(uri, 0);
  }

  @Test
  @Category(NotPullRequest.class)
  public void testGetInventoryTopBucketDelimiterOsdc() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + DELIMITER_FRAGMENT);
    // does not contain anything at "/"
    checkInventoryTopCountExact(uri, 0);
  }

  @Test
  @Category(NotPullRequest.class)
  public void testGetInventoryTopBucketAndPrefixSingleMatchAws() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_MATCH + delimiter);
      checkInventoryTopCountExact(uri, 1);
    }
  }

  @Test
  @Category(NotPullRequest.class)
  public void testGetInventoryTopBucketAndPrefixSingleMatchGcs() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_MATCH + delimiter);
      checkInventoryTopCountExact(uri, 1);
    }
  }

  @Test
  @Category(NotPullRequest.class)
  public void testGetInventoryTopBucketAndPrefixSingleMatchOsdc() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri =
          new CdmS3Uri(S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + getOsdcKey(G16_KEY_PREFIX_SINGLE_MATCH) + delimiter);
      checkInventoryTopCountExact(uri, 1);
    }
  }

  @Test
  public void testGetInventoryTopBucketAndPrefixMultiMatchAws() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri =
          new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_DAY_PARTIAL_FILE + delimiter);
      checkInventoryTopCountExact(uri, 12);
    }
  }

  @Test
  public void testGetInventoryTopBucketAndPrefixMultiMatchGcs() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri =
          new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_DAY_PARTIAL_FILE + delimiter);
      checkInventoryTopCountExact(uri, 12);
    }
  }

  @Test
  public void testGetInventoryTopBucketAndPrefixMultiMatchOsdc() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(
          S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + getOsdcKey(G16_KEY_PREFIX_SINGLE_DAY_PARTIAL_FILE) + delimiter);
      checkInventoryTopCountExact(uri, 12);
    }
  }

  //////////////////////
  // getInventoryAll() tests
  //
  @Test
  public void testGetInventoryAllBucketAws() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + delimiter);
      checkInventoryAllCount(uri, LIMIT_COUNT_MAX);
    }
  }

  @Test
  public void testGetInventoryAllBucketGcs() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + delimiter);
      checkInventoryAllCount(uri, LIMIT_COUNT_MAX);
    }
  }

  @Test
  public void testGetInventoryAllBucketOsdc() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + delimiter);
      checkInventoryAllCount(uri, LIMIT_COUNT_MAX);
    }
  }

  @Test
  public void testGetInventoryAllBucketAndPrefixSingleMatchAws() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_MATCH + delimiter);
      checkInventoryAllCount(uri, 1);
    }
  }

  @Test
  public void testGetInventoryAllBucketAndPrefixSingleMatchGcs() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_MATCH + delimiter);
      checkInventoryAllCount(uri, 1);
    }
  }

  @Test
  public void testGetInventoryAllBucketAndPrefixSingleMatchOsdc() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri =
          new CdmS3Uri(S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + getOsdcKey(G16_KEY_PREFIX_SINGLE_MATCH) + delimiter);
      checkInventoryAllCount(uri, 1);
    }
  }

  @Test
  public void testGetInventoryAllBucketAndPrefixMultiMatchAws() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri =
          new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_DAY_PARTIAL_FILE + delimiter);
      checkInventoryAllCount(uri, 12);
    }
  }

  @Test
  public void testGetInventoryAllBucketAndPrefixMultiMatchGcs() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri =
          new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_KEY_PREFIX_SINGLE_DAY_PARTIAL_FILE + delimiter);
      checkInventoryAllCount(uri, 12);
    }
  }

  @Test
  public void testGetInventoryAllBucketAndPrefixMultiMatchOsdc() throws URISyntaxException {
    for (String delimiter : DELIMITER_FRAGMENTS) {
      CdmS3Uri uri = new CdmS3Uri(
          S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + getOsdcKey(G16_KEY_PREFIX_SINGLE_DAY_PARTIAL_FILE) + delimiter);
      checkInventoryAllCount(uri, 12);
    }
  }

  //////////////////////
  // getSubdirs() tests
  //
  @Test
  public void testGetSubdirsWithDelimiterAws() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_KEY_SINGLE_DAY + DELIMITER_FRAGMENT);
    checkSubdirsCount(uri, 24);
  }

  @Test
  public void testGetSubdirsWithDelimiterGcs() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_KEY_SINGLE_DAY + DELIMITER_FRAGMENT);
    checkSubdirsCount(uri, 24);
  }

  @Test
  public void testGetSubdirsWithDelimiterOsdc() throws URISyntaxException {
    CdmS3Uri uri =
        new CdmS3Uri(S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + getOsdcKey(G16_KEY_SINGLE_DAY) + DELIMITER_FRAGMENT);
    checkSubdirsCount(uri, 24);
  }

  @Test
  public void testGetSubdirsWithoutDelimiterAws() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + G16_KEY_SINGLE_DAY);
    checkSubdirsCount(uri, 0);
  }

  @Test
  public void testGetSubdirsWithoutDelimiterGcs() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + G16_KEY_SINGLE_DAY);
    checkSubdirsCount(uri, 0);
  }

  @Test
  public void testGetSubdirsWithoutDelimiterOsdc() throws URISyntaxException {
    CdmS3Uri uri = new CdmS3Uri(S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?" + getOsdcKey(G16_KEY_SINGLE_DAY));
    checkSubdirsCount(uri, 0);
  }

  @AfterClass
  public static void teardown() {
    System.clearProperty(AWS_REGION_PROP_NAME);
  }

  private CollectionConfig getCollectionConfig(CdmS3Uri uri) {
    // for these tests, we'll always have the config include subdirectories, and the MFileFilter and auxInfo will be
    // null
    return new CollectionConfig(uri.getBucket(), uri.toString(), true, null, null);
  }

  private void checkInventoryTopCountExact(CdmS3Uri uri, int expectedCount) {
    int actualCount = topInventoryCount(uri);
    assertThat(actualCount).isEqualTo(expectedCount);
  }

  private void checkInventoryTopCountAtMost(CdmS3Uri uri, int expectedMaximumCount) {
    int actualCount = topInventoryCount(uri);
    assertThat(actualCount).isAtMost(expectedMaximumCount);
  }

  private int topInventoryCount(CdmS3Uri uri) {
    logger.debug("getInventoryTop: {}", uri);
    ControllerS3 controller = new ControllerS3();
    controller.limit = true;
    Iterator<MFile> it = controller.getInventoryTop(getCollectionConfig(uri), false);
    return countObjects(it);
  }

  private void checkInventoryAllCount(CdmS3Uri uri, int expectedCount) {
    logger.debug("getInventoryAll: {}", uri);
    ControllerS3 controller = new ControllerS3();
    controller.limit = true;
    Iterator<MFile> it = controller.getInventoryAll(getCollectionConfig(uri), false);
    assertThat(countObjects(it)).isEqualTo(expectedCount);
  }

  private void checkSubdirsCount(CdmS3Uri uri, int expectedCount) {
    logger.debug("getSubdirs: {}", uri);
    ControllerS3 controller = new ControllerS3();
    controller.limit = true;
    Iterator<MFile> it = controller.getSubdirs(getCollectionConfig(uri), false);
    assertThat(countObjects(it)).isEqualTo(expectedCount);
  }

  private int countObjects(Iterator<MFile> it) {
    int i = 0;
    while (it.hasNext()) {
      MFile mFile = it.next();
      if (PRINT) {
        System.out.print("\n The name of the MFile is " + mFile.getPath() + " " + i);
      }
      i++;
    }
    return i;
  }

  private static String getOsdcKey(String key) {
    return key.replaceFirst("ABI-L1b-RadC/2017/", "ABI-L1b-RadC/");
  }
}
