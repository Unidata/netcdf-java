/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisSpacing;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link SubsetHelpers} */

public class TestSubsetHelpers {
  @Test
  public void testFindCoordElementContiguous() {
    int n = 6;
    double[] xvalues = new double[] {2, 4, 8, 15, 50, 80};
    double[] xedges = new double[] {0, 3, 5, 10, 20, 79, 100};
    GridAxisPoint.Builder<?> xbuilder =
        GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name").setUnits("km").setDescription("desc")
            .setNcoords(n).setValues(xvalues).setEdges(xedges).setSpacing(GridAxisSpacing.nominalPoint);
    GridAxisPoint xaxis = xbuilder.build();

    assertThat(SubsetHelpers.findCoordElement(xaxis, 2.999, false)).isEqualTo(0);
    assertThat(SubsetHelpers.findCoordElement(xaxis, 3.00, false)).isEqualTo(1);
    assertThat(SubsetHelpers.findCoordElement(xaxis, 4.2, false)).isEqualTo(1);
    assertThat(SubsetHelpers.findCoordElement(xaxis, 78, false)).isEqualTo(4);
    assertThat(SubsetHelpers.findCoordElement(xaxis, 80, false)).isEqualTo(5);
  }

  @Test
  public void testFindCoordElementRegular() {
    int n = 19;
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("km").setDescription("desc").setRegular(n, -810, 90);
    GridAxisPoint xaxis = xbuilder.build();

    assertThat(SubsetHelpers.findCoordElement(xaxis, -856, false)).isEqualTo(-1);
    assertThat(SubsetHelpers.findCoordElement(xaxis, -855, false)).isEqualTo(0);
    assertThat(SubsetHelpers.findCoordElement(xaxis, -810, false)).isEqualTo(0);
    assertThat(SubsetHelpers.findCoordElement(xaxis, -766, false)).isEqualTo(0);
    assertThat(SubsetHelpers.findCoordElement(xaxis, -765, false)).isEqualTo(1);
    assertThat(SubsetHelpers.findCoordElement(xaxis, -764, false)).isEqualTo(1);
    assertThat(SubsetHelpers.findCoordElement(xaxis, 854, false)).isEqualTo(18);
    assertThat(SubsetHelpers.findCoordElement(xaxis, 855, false)).isEqualTo(19);
    assertThat(SubsetHelpers.findCoordElement(xaxis, 880, false)).isEqualTo(19);
  }

}
