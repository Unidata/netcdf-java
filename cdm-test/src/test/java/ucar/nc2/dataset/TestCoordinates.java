/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.io.StringReader;
import java.util.Formatter;

/** Test _Coordinates dataset in the JUnit framework. */
public class TestCoordinates {

  @Category(NeedsCdmUnitTest.class)
  @Test
  public void testAlias() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/demeter/MM_cnrm_129_red.ncml";
    try (NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Variable v = ncd.findCoordinateAxis("number");
      assertThat(v).isNotNull();
      assertThat(v).isInstanceOf(CoordinateAxis1D.class);
      assertThat(ncd.findDimension("ensemble")).isNotNull();
      assertThat(ncd.findDimension("ensemble")).isEqualTo(v.getDimension(0));
    }
  }

  // test offset only gets applied once
  @Category(NeedsCdmUnitTest.class)
  @Test
  public void testWrapOnce() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncml/coords/testCoordScaling.ncml";
    System.out.printf("%s%n", filename);
    try (NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Variable v = ncd.findCoordinateAxis("Longitude");
      assertThat(v).isNotNull();
      assertThat(v).isInstanceOf(CoordinateAxis1D.class);

      // if offset is applied twice, the result is not in +-180 range
      Array<Number> data = (Array<Number>) v.readArray();
      for (Number val : data) {
        assertThat(val.doubleValue() < 180).isTrue();
        assertThat(val.doubleValue() > -180).isTrue();
      }
    }
  }

  // from tom kunicki 1/3/11
  @Test
  public void testTimeUnitErrorMessage() throws IOException {
    String ncml = "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='All'>\n"
        + "  <dimension name='time' length='2' />\n" //
        + "  <dimension name='lat' length='3' />\n" //
        + "  <dimension name='lon' length='4' />\n" //
        + "  <dimension name='height' length='5' />\n" //
        + "  <attribute name='title' value='testSimpleTZYXGrid Data' />\n" //
        + "  <attribute name='Conventions' value='CF-1.4' />\n" //
        + "  <variable name='rh' shape='time height lat lon' type='int'>\n" //
        + "    <attribute name='long_name' value='relative humidity' />\n" //
        + "    <attribute name='units' value='percent' />\n" //
        + "    <attribute name='coordinates' value='lat lon' />\n" //
        + "    <values start='10' increment='5' />\n" //
        + "  </variable>\n" //
        + "  <variable name='time' shape='time' type='int'>\n" //
        + "    <attribute name='units' value='hours since 1970-01-01T12:00' />\n" //
        + "    <values start='1' increment='1' />\n" //
        + "  </variable>\n" //
        + "  <variable name='lat' shape='lat' type='float'>\n" //
        + "    <attribute name='units' value='degrees_north' />\n" //
        + "    <values start='40' increment='1' />\n" //
        + "  </variable>\n" //
        + "  <variable name='lon' shape='lon' type='float'>\n" //
        + "    <attribute name='units' value='degrees_east' />\n" //
        + "    <values start='-90' increment='1' />\n" //
        + "  </variable>\n" //
        + "  <variable name='height' shape='height' type='float'>\n" //
        + "    <attribute name='units' value='meters' />\n" //
        + "    <attribute name='positive' value='up'/>\n" //
        + "    <values start='1' increment='1' />\n" //
        + "  </variable>\n" //
        + "</netcdf>";

    String location = "testTimeUnitErrorMessage";
    System.out.println("testTimeUnitErrorMessage=\n" + ncml);
    try (NetcdfDataset ncd = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), location, null)) {
      System.out.printf("%n%s%n", ncd);
      VariableDS v = (VariableDS) ncd.findVariable("time");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.INT);
      assertThat((Object) v).isInstanceOf(CoordinateAxis.class);
      CoordinateAxis axis = (CoordinateAxis) v;
      assertThat(axis.getAxisType()).isEqualTo(AxisType.Time);

      Formatter errlog = new Formatter();
      try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.wrapGridDataset(ncd, errlog).orElseThrow()) {
        assertThat(gds).isNotNull();
        Grid grid = gds.findGrid("rh").orElseThrow();
        assertThat(grid).isNotNull();
        assertThat(grid.getArrayType()).isEqualTo(ArrayType.INT);
      }
    }
  }


}
