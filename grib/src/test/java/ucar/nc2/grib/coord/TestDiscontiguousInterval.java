package ucar.nc2.grib.coord;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.ma2.Array;
import ucar.nc2.*;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

public class TestDiscontiguousInterval {

  /**
   * Ensure that when creating a time coordinate variable from a series of
   * time intervals that are discontiguous and mixed, the time coordinate values
   * increase in a strictly-monotonic manner (as required by the CF Conventions
   * for netCDF).
   *
   * @throws IOException
   */
  @Test
  public void testTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing_Dataset1() throws IOException {
    String testfile = "../grib/src/test/data/GFS_Global_onedeg_20220627_0000.TotalPrecip.Out48hrs.grib2";
    String varName = "Total_precipitation_surface_Mixed_intervals_Accumulation";

    checkTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing(testfile, varName);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing_Dataset2() throws IOException {
    String testfile = TestDir.cdmUnitTestDir + "formats/grib1/wrf-em.wmo";
    String varName = "Total_precipitation_surface_Mixed_intervals_Accumulation";

    checkTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing(testfile, varName);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing_Dataset3() throws IOException {
    String testfile = TestDir.cdmUnitTestDir + "formats/grib1/nomads/ruc2_252_20110830_2300_008.grb";
    String varName = "Convective_precipitation_surface_Mixed_intervals_Accumulation";

    checkTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing(testfile, varName);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing_Dataset4() throws IOException {
    String testfile = TestDir.cdmUnitTestDir + "formats/grib2/cosmo_de_eps_m001_2009051100.grib2";
    String varName = "Total_precipitation_rate_surface_Mixed_intervals_Accumulation_ens";

    checkTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing(testfile, varName);
  }

  private void checkTimeCoord1D_fromIntervals_isStrictlyMonotonicallyIncreasing(String testfile, String varName)
      throws IOException {
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable dataVar = nc.findVariable(varName);
      Assert.assertNotNull(dataVar);

      Dimension timeDim = null;
      for (Dimension dim : dataVar.getDimensions()) {
        if (dim.getShortName().startsWith("time")) {
          timeDim = dim;
          break;
        }
      }
      Variable timeCoordVar = nc.findVariable(timeDim.getShortName());
      Assert.assertNotNull(timeCoordVar);

      Attribute att = timeCoordVar.findAttribute("bounds");
      Assert.assertNotNull(att);
      Assert.assertEquals(timeDim.getShortName() + "_bounds", att.getStringValue());

      Array timeCoordValues = timeCoordVar.read();
      checkTimeCoordinateVariable1D_IsStrictlyMonotonicallyIncreasing(timeDim.getLength(), timeCoordValues);
    }
  }

  private void checkTimeCoordinateVariable1D_IsStrictlyMonotonicallyIncreasing(int timeDimLength,
      Array timeCoordValues) {
    double currentValue = timeCoordValues.getDouble(0);
    double prevValue = currentValue;
    StringBuilder valuesSoFar = new StringBuilder();
    valuesSoFar.append(currentValue);
    for (int i = 1; i < timeDimLength; i++) {
      currentValue = timeCoordValues.getDouble(i);
      valuesSoFar.append(", ").append(currentValue);
      Assert.assertTrue("Not increasing in a strictly-monotonic manner: [" + valuesSoFar + "]",
          currentValue > prevValue);
      prevValue = currentValue;
    }
  }

  @Test
  public void testTimeCoord2D_fromIntervals_isStrictlyMonotonicallyIncreasing_Dataset1() throws IOException {
    String testfile = "../grib/src/test/data/GFS_Global_onedeg_20220627.TotalPrecip.Out24hrs.grib2";
    String varName = "Total_precipitation_surface_Mixed_intervals_Accumulation";

    checkTimeCoord2D_fromIntervals_isStrictlyMonotonicallyIncreasing(testfile, varName);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testFeatureCollectionBest_isStrictlyMonotonicallyIncreasing() throws IOException {
    final String spec =
        TestDir.cdmUnitTestDir + "/gribCollections/nonMonotonicTime/GFS_CONUS_80km_#yyyyMMdd_HHmm#\\.grib1$";
    final FeatureCollectionConfig config = new FeatureCollectionConfig("testFeatureCollectionBest", "path",
        FeatureCollectionType.GRIB1, spec, null, null, null, "file", null);
    final boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, null);
    assertThat(changed).isTrue();
    final String topLevelIndex = GribCdmIndex.getTopIndexFileFromConfig(config).getAbsolutePath();

    final String varName = "Total_precipitation_surface_Mixed_intervals_Accumulation";
    checkTimeCoord2D_fromIntervals_isStrictlyMonotonicallyIncreasing(topLevelIndex, varName, "Best/");
  }

  private void checkTimeCoord2D_fromIntervals_isStrictlyMonotonicallyIncreasing(String testfile, String varName)
      throws IOException {
    checkTimeCoord2D_fromIntervals_isStrictlyMonotonicallyIncreasing(testfile, varName, "");
  }

  private void checkTimeCoord2D_fromIntervals_isStrictlyMonotonicallyIncreasing(String testfile, String varName,
      String groupName) throws IOException {
    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      Variable dataVar = nc.findVariable(groupName + varName);
      Assert.assertNotNull(dataVar);

      Dimension timeDim = null;
      for (Dimension dim : dataVar.getDimensions()) {
        if (dim.getShortName().startsWith("time")) {
          timeDim = dim;
          break;
        }
      }
      Variable timeCoordVar = nc.findVariable(groupName + timeDim.getShortName());
      Assert.assertNotNull(timeCoordVar);

      Attribute att = timeCoordVar.findAttribute("bounds");
      Assert.assertNotNull(att);
      Assert.assertEquals(timeDim.getShortName() + "_bounds", att.getStringValue());

      Array timeCoordValues = timeCoordVar.read();
      checkTimeCoordVariable2D_IsStrictlyMonotonicallyIncreasing(timeDim.getLength(), timeCoordValues);
    }
  }

  private void checkTimeCoordVariable2D_IsStrictlyMonotonicallyIncreasing(int timeDimLength, Array timeCoordValues) {
    double currentValue = timeCoordValues.getDouble(0);
    double prevValue = currentValue;
    StringBuilder valuesSoFar = new StringBuilder();
    valuesSoFar.append(currentValue);
    for (int i = 1; i < timeDimLength; i++) {
      currentValue = timeCoordValues.getDouble(i);
      valuesSoFar.append(", ").append(currentValue);
      Assert.assertTrue("Not increasing in a strictly-monotonic manner: [" + valuesSoFar + "]",
          currentValue > prevValue);
      prevValue = currentValue;
    }
  }
}
