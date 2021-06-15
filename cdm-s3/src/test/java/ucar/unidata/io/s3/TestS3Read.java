/*
 * Copyright (c) 2019-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Formatter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.Region;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.util.IO;
import ucar.unidata.io.http.ReadFromUrl;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.category.NeedsUcarNetwork;
import ucar.unidata.util.test.category.Slow;

@Category(NeedsExternalResource.class)
public class TestS3Read {

  private static final Logger logger = LoggerFactory.getLogger(TestS3Read.class);

  private static final String COMMON_G16_KEY =
      "ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";

  // AWS specific constants
  private static final String AWS_G16_S3_URI_FULL = S3TestsCommon.TOP_LEVEL_AWS_BUCKET + "?" + COMMON_G16_KEY;
  private static final String AWS_G16_S3_URI_SIMPLE = "s3://noaa-goes16/" + COMMON_G16_KEY; // deprecated
  private static final String AWS_SHARED_CREDENTIALS_FILE_PROP =
      ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE.property();
  private static final String CUSTOM_AWS_SHARED_CREDENTIALS_FILE_GOOD_DEFAULT =
      "s3test_shared_credentials_file_good_default";
  private static final String CUSTOM_AWS_SHARED_CREDENTIALS_FILE_BAD_DEFAULT =
      "s3test_shared_credentials_file_bad_default";
  private static final String GOOD_PROFILE_NAME = "goes-profile";
  private static final String BAD_PROFILE_NAME = "no-goes-profile";

  // Google Cloud Platform constants
  private static final String GCS_G16_S3_URI = S3TestsCommon.TOP_LEVEL_GCS_BUCKET + "?" + COMMON_G16_KEY;

  // Open Science Data Cloud constants
  private static final String OSDC_G16_S3_URI =
      S3TestsCommon.TOP_LEVEL_OSDC_BUCKET + "?ABI-L1b-RadC/" + COMMON_G16_KEY.replaceFirst("ABI-L1b-RadC/2017/", "");

  // NCAR ActiveScale constants
  private static final String NCAR_PROFILE_NAME = "stratus-profile"; // generally not available
  private static final String NCAR_G16_S3_URI = "cdms3://" + NCAR_PROFILE_NAME
      + "@stratus.ucar.edu/unidata-netcdf-zarr-testing?netcdf-java/test/" + COMMON_G16_KEY;

  // Location to save local copy of file for comparison
  private static File LOCAL_G16_FILE;

  private static String credentialsFileGoodDefault = null;
  private static String credentialsFileBadDefault = null;

  @BeforeClass
  public static void setup() throws URISyntaxException, IOException {
    URL credFileGoodDefaultResource =
        TestS3Read.class.getClassLoader().getResource(CUSTOM_AWS_SHARED_CREDENTIALS_FILE_GOOD_DEFAULT);
    URL credFileBadDefaultResource =
        TestS3Read.class.getClassLoader().getResource(CUSTOM_AWS_SHARED_CREDENTIALS_FILE_BAD_DEFAULT);
    if (credFileGoodDefaultResource != null && credFileBadDefaultResource != null) {
      try {
        credentialsFileGoodDefault = Paths.get(credFileGoodDefaultResource.toURI()).toFile().toString();
        credentialsFileBadDefault = Paths.get(credFileBadDefaultResource.toURI()).toFile().toString();
      } catch (URISyntaxException e) {
        logger.error("Could not load the test S3 test AWS credential files.");
        throw e;
      }
    } else {
      String msg = "Could not locate the test S3 test AWS credential files.";
      logger.error(msg);
      throw new IOException(msg);
    }

    // Disable CdmS3Uri cache, otherwise credentials, regions, etc. will be ignored.
    // For example, awsProfileSharedCredsBadDefault will not pass.
    CdmS3Client.enableCache(false);
    String buffSize = System.getProperty("ucar.unidata.io.s3.bufferSize");
    logger.info("S3 Read Buffer Size: {} bytes", buffSize);

    // fetch local copy of the file we are testing against from AWS S3
    String directDownloadUrl = "https://noaa-goes16.s3.amazonaws.com/" + COMMON_G16_KEY;
    LOCAL_G16_FILE = File.createTempFile("ncj-", "TestS3Read.nc");
    LOCAL_G16_FILE.deleteOnExit();
    try (InputStream is = ReadFromUrl.getInputStreamFromUrl(directDownloadUrl)) {
      IO.writeToFile(is, LOCAL_G16_FILE.getCanonicalPath());
    }
  }

  /**
   * AWS S3
   *
   * https://aws.amazon.com/s3/
   *
   * @throws IOException Error accessing object store
   */
  @Test
  public void awsFullReadFile() throws IOException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfFile ncfile = NetcdfFiles.open(AWS_G16_S3_URI_FULL)) {
      testFullReadGoes16S3(ncfile);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  public void awsFullReadDataset() throws IOException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(AWS_G16_S3_URI_FULL)) {
      testFullReadGoes16S3(ncd);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  public void awsPartialReadFileDefaultRegionProp() throws IOException, InvalidRangeException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfFile ncfile = NetcdfFiles.open(AWS_G16_S3_URI_FULL)) {
      testPartialReadGoes16S3(ncfile);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  public void awsPartialReadDataset() throws IOException, InvalidRangeException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(AWS_G16_S3_URI_FULL)) {
      testPartialReadGoes16S3(ncd);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  public void awsPartialReadFileSimple() throws IOException, InvalidRangeException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfFile ncfile = NetcdfFiles.open(AWS_G16_S3_URI_SIMPLE)) {
      testPartialReadGoes16S3(ncfile);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  public void awsPartialReadDatasetSimple() throws IOException, InvalidRangeException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(AWS_G16_S3_URI_SIMPLE)) {
      testPartialReadGoes16S3(ncd);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  public void awsPartialReadAquireFile() throws IOException, InvalidRangeException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    DatasetUrl durl = DatasetUrl.findDatasetUrl(AWS_G16_S3_URI_FULL);
    try (NetcdfFile ncf = NetcdfDatasets.acquireFile(durl, null)) {
      testPartialReadGoes16S3(ncf);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  public void awsPartialReadAquireDataset() throws IOException, InvalidRangeException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    DatasetUrl durl = DatasetUrl.findDatasetUrl(AWS_G16_S3_URI_FULL);
    try (NetcdfDataset ncd = NetcdfDatasets.acquireDataset(durl, null)) {
      testPartialReadGoes16S3(ncd);
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  // tests using custom credential file location for setting region

  @Test
  public void awsProfileSharedCredsGoodDefault() throws IOException {
    System.setProperty(AWS_SHARED_CREDENTIALS_FILE_PROP, credentialsFileGoodDefault);
    try (NetcdfFile ncfile = NetcdfFiles.open(AWS_G16_S3_URI_FULL)) {
      assertThat(ncfile).isNotNull();
    } finally {
      System.clearProperty(AWS_SHARED_CREDENTIALS_FILE_PROP);
    }
  }

  @Test(expected = software.amazon.awssdk.services.s3.model.S3Exception.class)
  public void awsProfileSharedCredsBadDefault() throws IOException {
    // point to a shared credentials file with a bad default region (i.e. GOES-16 does not exist in the Asia Pacific
    // (Mumbai) region
    System.setProperty(AWS_SHARED_CREDENTIALS_FILE_PROP, credentialsFileBadDefault);
    try (NetcdfFile ncfile = NetcdfFiles.open(AWS_G16_S3_URI_FULL)) {
      assertThat(ncfile).isNotNull();
    } finally {
      System.clearProperty(AWS_SHARED_CREDENTIALS_FILE_PROP);
    }
  }

  @Test
  public void awsProfileSharedCredsBadDefaultGoodProfileName() throws IOException {
    // point to a shared credentials file with a bad default region (i.e. GOES-16 does not exist in the Asia Pacific
    // (Mumbai) region) but use a profile with a good region
    System.setProperty(AWS_SHARED_CREDENTIALS_FILE_PROP, credentialsFileBadDefault);
    String cdmS3Uri = String.format("cdms3://%s@aws/noaa-goes16?", GOOD_PROFILE_NAME) + COMMON_G16_KEY;
    try (NetcdfFile ncfile = NetcdfFiles.open(cdmS3Uri)) {
      assertThat(ncfile).isNotNull();
    } finally {
      System.clearProperty(AWS_SHARED_CREDENTIALS_FILE_PROP);
    }
  }

  @Test(expected = software.amazon.awssdk.services.s3.model.S3Exception.class)
  public void awsProfileSharedCredsGoodDefaultBadProfileName() throws IOException {
    // point to a shared credentials file with a good default region but use a profile that uses a bad region (one
    // without goes16 data)
    System.setProperty(AWS_SHARED_CREDENTIALS_FILE_PROP, credentialsFileBadDefault);
    String cdmS3Uri = String.format("cdms3://%s@aws/noaa-goes16?", BAD_PROFILE_NAME) + COMMON_G16_KEY;
    try (NetcdfFile ncfile = NetcdfFiles.open(cdmS3Uri)) {
      assertThat(ncfile).isNotNull();
    } finally {
      System.clearProperty(AWS_SHARED_CREDENTIALS_FILE_PROP);
    }
  }

  @Test
  public void awsSharedCredsPrecedence() throws IOException {
    // Test that the region set in the custom shared credentials file takes precedence
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, Region.AP_SOUTH_1.id());
    System.setProperty(AWS_SHARED_CREDENTIALS_FILE_PROP, credentialsFileBadDefault);
    String cdmS3Uri = String.format("cdms3://%s@aws/noaa-goes16?", GOOD_PROFILE_NAME) + COMMON_G16_KEY;
    try (NetcdfFile ncfile = NetcdfFiles.open(cdmS3Uri)) {
      assertThat(ncfile).isNotNull();
    } finally {
      System.clearProperty(AWS_SHARED_CREDENTIALS_FILE_PROP);
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  /**
   * Google Cloud Storage
   *
   * https://cloud.google.com/storage
   *
   * @throws IOException Error accessing object store
   */
  @Test
  public void gcsFullReadFile() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(GCS_G16_S3_URI)) {
      testFullReadGoes16S3(ncfile);
    }
  }

  @Test
  public void gcsFullReadDataset() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(GCS_G16_S3_URI)) {
      testFullReadGoes16S3(ncd);
    }
  }

  @Test
  public void gcsPartialReadFile() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(GCS_G16_S3_URI)) {
      testPartialReadGoes16S3(ncfile);
    }
  }

  @Test
  public void gcsPartialReadDataset() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(GCS_G16_S3_URI)) {
      testPartialReadGoes16S3(ncd);
    }
  }

  @Test
  public void gcsPartialReadAquireFile() throws IOException, InvalidRangeException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(GCS_G16_S3_URI);
    try (NetcdfFile ncf = NetcdfDatasets.acquireFile(durl, null)) {
      testPartialReadGoes16S3(ncf);
    }
  }

  @Test
  public void gcsPartialReadAquireDataset() throws IOException, InvalidRangeException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(GCS_G16_S3_URI);
    try (NetcdfDataset ncd = NetcdfDatasets.acquireDataset(durl, null)) {
      testPartialReadGoes16S3(ncd);
    }
  }

  /**
   * Open Science Data Cloud
   *
   * https://www.opensciencedatacloud.org/
   *
   * Managed by Open Commons Consortium (OCC)
   * I believe OSDC uses Ceph Object Gateway:
   * https://www.opensciencedatacloud.org/support/griffin.html#understanding-osdc-griffin-storage-options-and-workflow
   *
   * @throws IOException Error accessing object store
   */
  @Test
  public void osdcFullReadFile() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(OSDC_G16_S3_URI)) {
      testFullReadGoes16S3(ncfile);
    }
  }

  @Test
  public void osdcFullReadDataset() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(OSDC_G16_S3_URI)) {
      testFullReadGoes16S3(ncd);
    }
  }

  @Test
  public void osdcPartialReadFile() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(OSDC_G16_S3_URI)) {
      testPartialReadGoes16S3(ncfile);
    }
  }

  @Test
  public void osdcPartialReadDataset() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(OSDC_G16_S3_URI)) {
      testPartialReadGoes16S3(ncd);
    }
  }

  @Test
  public void osdcPartialReadAquireFile() throws IOException, InvalidRangeException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(OSDC_G16_S3_URI);
    try (NetcdfFile ncf = NetcdfDatasets.acquireFile(durl, null)) {
      testPartialReadGoes16S3(ncf);
    }
  }

  @Test
  public void osdcPartialReadAquireDataset() throws IOException, InvalidRangeException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(OSDC_G16_S3_URI);
    try (NetcdfDataset ncd = NetcdfDatasets.acquireDataset(durl, null)) {
      testPartialReadGoes16S3(ncd);
    }
  }

  @Test
  @Category(Slow.class)
  public void compareStores() throws IOException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfFile osdc = NetcdfFiles.open(OSDC_G16_S3_URI);
        NetcdfFile gcs = NetcdfFiles.open(GCS_G16_S3_URI);
        NetcdfFile aws = NetcdfFiles.open(AWS_G16_S3_URI_FULL)) {
      Formatter f = new Formatter();
      CompareNetcdf2 comparer = new CompareNetcdf2(f, false, false, true);
      Assert.assertTrue("Compare AWS S3 Object to GCS Object.", comparer.compare(aws, gcs));
      Assert.assertTrue("Compare AWS S3 Object to OSDC Object.", comparer.compare(aws, osdc));
      Assert.assertTrue("Compare OSDC Object to GCS Object.", comparer.compare(osdc, gcs));
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  @Test
  @Category(Slow.class)
  public void compareAgainstLocal() throws IOException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfFile osdc = NetcdfFiles.open(OSDC_G16_S3_URI);
        NetcdfFile gcs = NetcdfFiles.open(GCS_G16_S3_URI);
        NetcdfFile aws = NetcdfFiles.open(AWS_G16_S3_URI_FULL);
        NetcdfFile local = NetcdfFiles.open(LOCAL_G16_FILE.getPath())) {
      Formatter f = new Formatter();
      CompareNetcdf2 comparer = new CompareNetcdf2(f, false, false, true);
      // Indirectly verify some of the S3RandomAccessFile reading logic
      Assert.assertTrue("Compare local file read to AWS S3 Object store read.", comparer.compare(local, aws));
      Assert.assertTrue("Compare local file read to GCS Object store read.", comparer.compare(local, gcs));
      Assert.assertTrue("Compare local file read to OSDC Object store read.", comparer.compare(local, osdc));
    } finally {
      System.clearProperty(S3TestsCommon.AWS_REGION_PROP_NAME);
    }
  }

  /**
   * NCAR ActiveScale Object Store
   *
   * https://www.quantum.com/en/products/object-storage/
   *
   * Must be on the UCAR network to see this system and have properly configured credentials to run these tests.
   *
   * @throws IOException Error accessing object store
   */
  @Test
  @Category(NeedsUcarNetwork.class)
  public void ncarFullReadFile() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(NCAR_G16_S3_URI)) {
      testFullReadGoes16S3(ncfile);
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void ncarFullReadDataset() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(NCAR_G16_S3_URI)) {
      testFullReadGoes16S3(ncd);
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void ncarPartialReadFile() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(NCAR_G16_S3_URI)) {
      testPartialReadGoes16S3(ncfile);
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void ncarPartialReadDataset() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(NCAR_G16_S3_URI)) {
      testPartialReadGoes16S3(ncd);
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void ncarPartialReadAquireFile() throws IOException, InvalidRangeException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(NCAR_G16_S3_URI);
    try (NetcdfFile ncf = NetcdfDatasets.acquireFile(durl, null)) {
      testPartialReadGoes16S3(ncf);
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void ncarPartialReadAquireDataset() throws IOException, InvalidRangeException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(NCAR_G16_S3_URI);
    try (NetcdfDataset ncd = NetcdfDatasets.acquireDataset(durl, null)) {
      testPartialReadGoes16S3(ncd);
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void testActiveScaleWithCredsS3() throws IOException {
    String host = "stratus.ucar.edu";
    String bucket = "unidata-netcdf-zarr-testing";
    String key = "netcdf-java/test/GFS_Global_0p25deg_20200326_1200_apparent_temperature.nc4";
    String s3Uri = "cdms3://" + NCAR_PROFILE_NAME + "@" + host + "/" + bucket + "?" + key;
    try (NetcdfFile ncfile = NetcdfFiles.open(s3Uri)) {
      Attribute conv = ncfile.getRootGroup().attributes().findAttributeIgnoreCase("Conventions");
      assertThat(conv).isNotNull();
      assertThat(conv.getStringValue()).ignoringCase().isEqualTo("CF-1.6");
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void testActiveScaleS3() throws IOException {
    // do not need credentials to run this one, just access to the internal ucar network.
    String host = "stratus.ucar.edu";
    String bucket = "rda-data";
    String key = "ds262.0/CERFACS/uo_Omon_NEMO3-2_FRCCORE2_f_r1i1p1_199801-200712.nc";
    String s3Uri = "cdms3://" + host + "/" + bucket + "?" + key;
    try (NetcdfFile ncfile = NetcdfFiles.open(s3Uri)) {
      Attribute conv = ncfile.getRootGroup().attributes().findAttributeIgnoreCase("Conventions");
      assertThat(conv).isNotNull();
      assertThat(conv.getStringValue()).ignoringCase().isEqualTo("CF-1.4");
    }
  }

  @AfterClass
  public static void resetCdmS3Client() {
    // re-enable the CdmS3Cache
    CdmS3Client.enableCache(true);
  }

  //////////////
  // Test helper methods

  public <T extends NetcdfFile> void testFullReadGoes16S3(T nc) throws IOException {
    Dimension x = nc.findDimension("x");
    Dimension y = nc.findDimension("y");
    assertThat(x).isNotNull();
    assertThat(y).isNotNull();

    if (nc instanceof NetcdfDataset) {
      String partialConventionValue = "CF-1.";
      // read conventions string
      String conventions = nc.getRootGroup().attributes().findAttributeString(CF.CONVENTIONS, "");

      // check that the file was read the CF convention builder
      assertThat(conventions).startsWith(partialConventionValue);
      assertThat(((NetcdfDataset) nc).getConventionUsed()).startsWith(partialConventionValue);
    }

    testG16RadVar(nc);
  }

  public <T extends NetcdfFile> void testG16RadVar(T nc) throws IOException {
    // find variable "Rad"
    Variable radiance = nc.findVariable("Rad");
    Assert.assertNotNull(radiance);

    // read full array
    Array array = radiance.read();
    assertThat(array.getRank()).isEqualTo(2);

    // check shape of array is the same as the shape of the variable
    int[] variableShape = radiance.getShape();
    int[] arrayShape = array.getShape();
    assertThat(variableShape).isEqualTo(arrayShape);
  }

  public <T extends NetcdfFile> void testPartialReadGoes16S3(T nc) throws InvalidRangeException, IOException {
    // find variable "Rad"
    Variable radiance = nc.findVariable("Rad");
    Assert.assertNotNull(radiance);

    // read part of the array
    Section section = new Section("(100:200:2,10:20:1)");
    Array array = radiance.read(section);
    assertThat(array.getRank()).isEqualTo(2);

    // check shape of array is the same as the shape of the section
    assertThat(array.getShape()).isEqualTo(section.getShape());
  }
}
