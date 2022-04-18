package ucar.nc2.iosp.gini;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.MAMath;
import ucar.ma2.MAMath.MinMax;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestGiniNegativeCalibrationValues {

  @Test
  public void testMinMaxValues() throws IOException {
    try (NetcdfFile ncfile =
        NetcdfFiles.open(TestDir.cdmUnitTestDir + "formats/gini/images_sat_NEXRCOMP_1km_n0r_n0r_20200907_0740")) {
      Variable variable = ncfile.findVariable("Reflectivity");
      Array array = variable.read();
      MinMax minMax = MAMath.getMinMax(array);
      // If the bug reported in https://github.com/Unidata/netcdf-java/issues/480 shows up, and we are
      // incorrectly reading the calibration coefficients from the GINI file headers "Unidata cal block"
      // (in the case of the original bug, we were decoding negative numbers as intended), the data values
      // will be on the order of -3.9 x 10^5 for the minimum, and -2.1 x 10^5 for the maximum. Those
      // values (radar reflectivity) should be -30 dBZ to 60 dBZ.
      assertThat(minMax.min).isWithin(1e-6).of(-30);
      assertThat(minMax.max).isWithin(1e-6).of(60);
    }
  }
}
