/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import org.junit.Test;
import ucar.array.Array;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.String.format;

public class TestSimpleGeom {
  private static final String cfConvention = "CF-1.X";

  @Test
  public void testLine() throws IOException {
    String failMessage, found, expected;
    boolean testCond;
    String tstFile = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(tstFile)) {
      // make sure this dataset used the cfConvention
      expected = cfConvention;
      found = ncd.getConventionUsed();
      testCond = found.equals(expected);
      failMessage =
          format("This dataset used the %s convention, but should have used the %s convention.", found, expected);
      assertWithMessage(failMessage).that(testCond).isTrue();

      // check that attributes were filled in correctly
      List<Variable> vars = ncd.getVariables();
      for (Variable v : vars) {
        if (v.findAttribute(CF.GEOMETRY) != null) {
          assertThat(v.findAttribute(CF.NODE_COORDINATES));
          assertThat(v.findAttribute(_Coordinate.Axes));
        }
      }
    }
  }

  @Test
  public void testPolygon() throws IOException {
    String failMessage, found, expected;
    boolean testCond;

    String tstFile = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/outflow_3seg_5timesteps_vlen.nc";

    // open the test file
    NetcdfDataset ncd = NetcdfDatasets.openDataset(tstFile);

    // make sure this dataset used the cfConvention
    expected = cfConvention;
    found = ncd.getConventionUsed();
    testCond = found.equals(expected);
    failMessage =
        format("This dataset used the %s convention, but should have used the %s convention.", found, expected);
    assertWithMessage(failMessage).that(testCond).isTrue();

    // check that attributes were filled in correctly
    List<Variable> vars = ncd.getVariables();
    for (Variable v : vars) {
      if (v.findAttribute(CF.GEOMETRY) != null) {
        assertThat(v.findAttribute(CF.NODE_COORDINATES)).isNotNull();
        assertThat(v.findAttribute(_Coordinate.Axes)).isNotNull();
      }
    }
    ncd.close();
  }

  @Test
  public void testCoordinateVariable() throws IOException {
    String tstFile = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/outflow_3seg_5timesteps_vlen.nc";
    // open the test file
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(tstFile)) {
      for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
        System.out.printf("Try to read %s ", axis.getFullName());
        Array<?> data = axis.readArray();
        System.out.printf(" OK (%d) %n", data.getSize());
      }
    }
  }

  @Test
  public void testVarLenDataVariable() throws IOException {
    String tstFile = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/outflow_3seg_5timesteps_vlen.nc";
    // open the test file
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(tstFile)) {
      for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
        System.out.printf("Try to read %s ", axis.getFullName());
        Array<?> data = axis.readArray();
        System.out.printf(" OK (%d) %n", data.getSize());
      }
    }
  }
}
