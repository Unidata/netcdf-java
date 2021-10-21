/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.grib1;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
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
    final float expectedLon = 76.21f;
    final float expectedLat = 18.95f;
    final float tol = 0.001f;

    try (NetcdfFile nc = NetcdfFiles.open(testfile)) {
      checkVal(nc.findVariable("lon"), expectedLon, tol);
      checkVal(nc.findVariable("lat"), expectedLat, tol);
    }
  }

  private void checkVal(Variable variable, float expectedValue, float tol) throws IOException {
    Array<Float> array = (Array<Float>) variable.readArray();
    assertThat(array.getSize()).isEqualTo(1);
    float val = array.get(0);
    assertThat(val).isWithin(tol).of(expectedValue);
  }
}
