import software.amazon.awssdk.regions.Region;

public class ZarrTestsCommon {
  // AWS props
  public static final String AWS_REGION_PROP_NAME = "aws.region";
  public static final String AWS_REGION = Region.US_EAST_1.toString();
  public static final String AWS_BUCKET_NAME = "unidata-zarr-test-data";
  public static final String S3_PREFIX = "cdms3:";
  public static final String S3_FRAGMENT = "delimiter=/";

  // test file names
  public static final String ZARR_FILENAME = "test_data.zarr/";
  public static final String COMPRESSED_ZARR_FILENAME = "test_data.zip";

  // Object stores
  public static final String OBJECT_STORE_ZARR_URI =
      S3_PREFIX + AWS_BUCKET_NAME + "?" + ZARR_FILENAME + "#" + S3_FRAGMENT;

  // Local stores
  public static final String LOCAL_TEST_DATA_PATH = "src/test/data/preserveLineEndings/";
  public static final String DIRECTORY_STORE_URI = LOCAL_TEST_DATA_PATH + ZARR_FILENAME;
  public static final String ZIP_STORE_URI = LOCAL_TEST_DATA_PATH + COMPRESSED_ZARR_FILENAME;
}
