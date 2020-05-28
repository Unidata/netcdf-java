package ucar.nc2.grib.grib1;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

public class TestSinglePointGds {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * See https://github.com/Unidata/netcdf-java/issues/297
   *
   * This tests reading a GRIB message with a GDS describing a single lat/lon point.
   *
   */
  @Test
  public void checkLatLon() throws IOException {
    final String testfile = "../grib/src/test/data/single_point_gds.grib1";
    final double expectedLon = 76.21;
    final double expectedLat = 18.95;
    final double tol = 0.001;

    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      checkVal(nc.findVariable("lon"), expectedLon, tol);
      checkVal(nc.findVariable("lat"), expectedLat, tol);
    }
  }

  private void checkVal(Variable variable, double expectedValue, double tol) throws IOException {
    Array array = variable.read();
    assertThat(array.getSize()).isEqualTo(1);
    double val = array.getDouble(0);
    assertThat(val).isWithin(tol).of(expectedValue);
  }
}
