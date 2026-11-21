/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.fmrc;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.write.Ncdump;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.category.Slow;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

public class TestFmrcOffsetsGreaterEqual {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String AWS_REGION_PROP_NAME = "aws.region";
  private static final String AWS_REGION = "us-east-1";

  @BeforeClass
  public static void setAwsRegion() {
    System.setProperty(AWS_REGION_PROP_NAME, AWS_REGION);
  }

  @AfterClass
  public static void clearAwsRegion() {
    System.clearProperty(AWS_REGION_PROP_NAME);
  }

  /*
   * <featureCollection name="espresso_2013_da_history_fmrc_with_Offset_in_filename" featureType="FMRC"
   * harvest="true" path="roms/espresso/2013_da/fmrc/his/Offset">
   * 
   * <metadata inherited="true">
   * <serviceName>all</serviceName>
   * </metadata>
   * 
   * <collection spec="C:/Users/madry/work/DavidRobertson/subdir/.*\.nc$"
   * name="espresso_2013_da_his_fmrc_offset_with_Offset_in_filename"
   * dateFormatMark="#espresso_his_offset_#yyyyMMdd_HHmm"
   * olderThan="5 min" />
   * 
   * <update startup="true" rescan="0 0/5 * * * ? *" trigger="allow" />
   * 
   * <!--protoDataset choice="Penultimate">
   * <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="nc/cldc.mean.nc">
   * <remove name="ocean_time" type="variable" />
   * </netcdf>
   * </protoDataset-->
   * 
   * <fmrcConfig regularize="false" datasetTypes="TwoD Files Runs ConstantForecasts ConstantOffsets">
   * <dataset name="Best" offsetsGreaterEqual="26" />
   * </fmrcConfig>
   * 
   * </featureCollection>
   */

  /*
   * original data:
   * 
   * 1) time 2013-05-05T00:00:00Z offsets=0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0,
   * 26.0, 28.0, 30.0, 32.0, 34.0, 36.0, 38.0, 40.0, 42.0, 44.0, 46.0, 48.0,
   * 50.0, 52.0, 54.0, 56.0, 58.0, 60.0, 62.0, 64.0, 66.0, 68.0, 70.0, 72.0,
   * 74.0, 76.0, 78.0, 80.0, 82.0, 84.0, 86.0, 88.0, 90.0, 92.0, 94.0, 96.0,
   * 98.0, 100.0, 102.0, 104.0, 106.0, 108.0, 110.0, 112.0, 114.0, 116.0, 118.0, 120.0,
   * 122.0, 124.0, 126.0, 128.0, 130.0, 132.0, 134.0, 136.0, 138.0, 140.0, 142.0, 144.0,
   * 146.0, 148.0, 150.0, 152.0, 154.0, 156.0, 158.0, 160.0, 162.0, 164.0, 166.0, 168.0,
   * 
   * 2) time 2013-05-06T00:00:00Z offsets=0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0,
   * 26.0, 28.0, 30.0, 32.0, 34.0, 36.0, 38.0, 40.0, 42.0, 44.0, 46.0, 48.0,
   * 50.0, 52.0, 54.0, 56.0, 58.0, 60.0, 62.0, 64.0, 66.0, 68.0, 70.0, 72.0,
   * 74.0, 76.0, 78.0, 80.0, 82.0, 84.0, 86.0, 88.0, 90.0, 92.0, 94.0, 96.0,
   * 98.0, 100.0, 102.0, 104.0, 106.0, 108.0, 110.0, 112.0, 114.0, 116.0, 118.0, 120.0,
   * 122.0, 124.0, 126.0, 128.0, 130.0, 132.0, 134.0, 136.0, 138.0, 140.0, 142.0, 144.0,
   * 146.0, 148.0, 150.0, 152.0, 154.0, 156.0, 158.0, 160.0, 162.0, 164.0, 166.0, 168.0,
   */


  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBestOffsetGE() throws IOException {
    checkBestOffsetGE(TestDir.cdmUnitTestDir + "ft/fmrc/espresso/.*.nc$");
  }

  @Test
  @Category({NeedsExternalResource.class, Slow.class})
  public void testBestOffsetGEOnS3() throws IOException {
    // annotated Slow because these files contain record (coordinate) variables, and each
    // value is a call to the object.
    // the same files found under TestDir.cdmUnitTestDir + "ft/fmrc/espresso/" are
    // available in the thredds-test-data bucket on AWS S3
    final String S3_SPEC = "cdms3:thredds-test-data?ft/fmrc/espresso/.*\\.nc$#delimiter=/";
    checkBestOffsetGE(S3_SPEC);
  }

  private void checkBestOffsetGE(String spec) throws IOException {
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("espresso_2013_da_his_fmrc_offset_with_Offset_in_filename", "DavidRobertson",
            FeatureCollectionType.FMRC, spec, null, "#espresso_his_#yyyyMMdd_HHmm", null, null, null);
    config.fmrcConfig.regularize = false;
    config.fmrcConfig.addBestDataset("Best", 26);

    Formatter errlog = new Formatter();
    Fmrc fmrc = Fmrc.open(config, errlog);
    assert (fmrc != null) : errlog;
    FeatureCollectionConfig.BestDataset bd = new FeatureCollectionConfig.BestDataset("Best", 26);
    try (ucar.nc2.dt.GridDataset gridDs = fmrc.getDatasetBest(bd)) {
      GridDatatype grid = gridDs.findGridByShortName("salt");
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1D timeAxis = gcs.getTimeAxis1D();
      logger.debug("timeAxis = {} {}", Ncdump.printArray(timeAxis.read()), timeAxis.getUnitsString());
      CoordinateAxis1D runAxis = gcs.getRunTimeAxis();
      logger.debug("runAxis = {} {}", Ncdump.printArray(runAxis.read()), runAxis.getUnitsString());

      CalendarDate expected = CalendarDate.parseISOformat(null, "2013-05-05T00:00:00");
      CalendarDateUnit cdu = CalendarDateUnit.of(null, timeAxis.getUnitsString());
      Assert.assertEquals(expected, cdu.getBaseCalendarDate());

      CalendarDateUnit cdu2 = CalendarDateUnit.of(null, runAxis.getUnitsString());
      Assert.assertEquals(expected, cdu2.getBaseCalendarDate());

      // offsetsGreaterEqual=26 drops the earlier offsets, so the Best time coordinate must start at 26 hours
      // (it would start at 0 if the offset filter were ignored)
      Assert.assertEquals("first best time coord", 26.0, timeAxis.getCoordValue(0), 1.0e-8);

      // both runs must contribute to the Best dataset, otherwise the run coord check below cannot see the second run
      Assert.assertTrue("run axis size = " + runAxis.getSize(), runAxis.getSize() > 12);

      for (int i = 0; i < runAxis.getSize(); i++) {
        Assert.assertEquals("run coord", i < 12 ? 0 : 24, runAxis.getCoordValue(i), 1.0e-8);
      }
    }
  }


}
