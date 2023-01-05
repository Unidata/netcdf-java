package ucar.nc2.grib.coord;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.nc2.*;
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
}
