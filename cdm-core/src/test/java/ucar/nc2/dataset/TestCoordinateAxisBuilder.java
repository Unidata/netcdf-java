/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.write.NcdumpArray;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Test {@link CoordinateAxis.Builder} */
public class TestCoordinateAxisBuilder {

  @Test
  public void testFromVariableDS() {
    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT)
        .setUnits("units").setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.GeoX);
    CoordinateAxis axis = builder.build(makeDummyGroup());

    assertThat(axis.getShortName()).isEqualTo("name");
    assertThat(axis.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(axis.getUnitsString()).isEqualTo("units");
    assertThat(axis.getDescription()).isEqualTo("desc");
    assertThat(axis.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(axis.findAttributeString(CDM.UNITS, "")).isEqualTo("units");
    assertThat(axis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");

    CoordinateAxis copy = axis.toBuilder().build(makeDummyGroup());
    assertThat(copy).isEqualTo(axis);
    assertThat(copy.hashCode()).isEqualTo(axis.hashCode());
  }

  /*
   * https://github.com/Unidata/netcdf-java/issues/592
   * CoordinateAxis1D lonAxis = ...
   * lonAxis.toString();
   * double Lon(XDim=360);
   * :units = "degrees_east";
   * :_CoordinateAxisType = "Lon";
   * lonAxis.isRegular() returns true
   * lonAxis.getStart() returns -179.5
   * lonAxis.getIncrement() returns 1.0
   * lonAxis.getMinEdgeValue() returns invalid value -179.5 !
   * lonAxis.getCoordEdge(0) returns valid value -180.0
   * lonAxis.getMinEdgeValue() now returns valid value -180.0 !
   */

  @Test
  public void testIssue592() throws IOException {
    Group group = Group.builder().addDimension(Dimension.builder().setName("lon").setLength(360).build()).build();
    List<Dimension> varDims = group.makeDimensionsList("lon");

    VariableDS.Builder vdsBuilder = VariableDS.builder().setName("LonCoord").setUnits("degrees_east")
        .setArrayType(ArrayType.FLOAT).addDimensions(varDims).setAutoGen(-179.5, 1);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Lon);
    CoordinateAxis axis = builder.build(group);

    assertThat(axis.getRank()).isEqualTo(1);
    assertThat(axis).isInstanceOf(CoordinateAxis1D.class);

    CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
    System.out.printf("axis1D values=%s%n", NcdumpArray.printArray(axis1D.readArray()));

    CoordinateAxis1D lonAxis = axis1D.correctLongitudeWrap();
    System.out.printf("lonAxis values=%s%n", NcdumpArray.printArray(lonAxis.readArray()));
    System.out.printf("lonAxis edges=%s%n", Arrays.toString(lonAxis.getCoordEdges()));

    assertThat(lonAxis.getSize()).isEqualTo(360);
    assertThat(lonAxis.getIncrement()).isEqualTo(1);
    assertThat(lonAxis.getCoordEdge(0)).isEqualTo(-180.);
  }


}
