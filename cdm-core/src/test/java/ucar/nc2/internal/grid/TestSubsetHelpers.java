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
  public void testBinarySearch() {
    int n = 6;
    double[] xvalues = new double[] {2, 4, 8, 15, 50, 80};
    double[] xedges = new double[] {0, 3, 5, 10, 20, 79, 100};
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("km").setDescription("desc").setNcoords(n).setValues(xvalues).setEdges(xedges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    assertThat(SubsetHelpers.findCoordElementContiguous(xaxis, 2.999, false)).isEqualTo(0);
    assertThat(SubsetHelpers.findCoordElementContiguous(xaxis, 3.00, false)).isEqualTo(1);
    assertThat(SubsetHelpers.findCoordElementContiguous(xaxis, 4.2, false)).isEqualTo(1);
    assertThat(SubsetHelpers.findCoordElementContiguous(xaxis, 78, false)).isEqualTo(4);
    assertThat(SubsetHelpers.findCoordElementContiguous(xaxis, 80, false)).isEqualTo(5);
  }

}
