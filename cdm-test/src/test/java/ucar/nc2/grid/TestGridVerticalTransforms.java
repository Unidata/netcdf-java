/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.geoloc.vertical.VerticalTransform;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGridVerticalTransforms {

  @Test
  public void testWRF() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc";
    System.out.printf("testWRF %s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      testWrfGrid(gds.findGrid("U").orElseThrow(), ImmutableList.of(27, 60, 74));
      testWrfGrid(gds.findGrid("V").orElseThrow(), ImmutableList.of(27, 61, 73));
      testWrfGrid(gds.findGrid("W").orElseThrow(), ImmutableList.of(28, 60, 73));
      testWrfGrid(gds.findGrid("T").orElseThrow(), ImmutableList.of(27, 60, 73));
    }
  }

  private void testWrfGrid(Grid grid, List<Integer> expected) throws IOException, InvalidRangeException {
    GridCoordinateSystem gcs = grid.getCoordinateSystem();
    assertThat(gcs).isNotNull();
    GridHorizCoordinateSystem hcs = gcs.getHorizCoordinateSystem();
    assertThat(hcs).isNotNull();

    assertThat(gcs.getNominalShape()).hasSize(4);
    System.out.printf("Grid %s shape = %s%n", grid.getName(), gcs.getNominalShape());

    GridReferencedArray geoArray = grid.getReader().setTimeFirst().read();
    List<Integer> shape = geoArray.getMaterializedCoordinateSystem().getMaterializedShape();

    GridAxis<?> zaxis = gcs.getVerticalAxis();
    assertThat(shape.get(1)).isEqualTo(zaxis.getNominalSize());

    GridAxis<?> yaxis = gcs.getYHorizAxis();
    assertThat(shape.get(2)).isEqualTo(yaxis.getNominalSize());

    GridAxis<?> xaxis = gcs.getXHorizAxis();
    assertThat(shape.get(3)).isEqualTo(xaxis.getNominalSize());

    VerticalTransform vt = gcs.getVerticalTransform();
    assertThat(vt).isNotNull();
    assertThat(vt).isInstanceOf(ucar.nc2.geoloc.vertical.WrfEta.class);

    Array<Number> z3d = vt.getCoordinateArray3D(0);
    Section sv = new Section(z3d.getShape());
    System.out.printf("3dcoord = %s %n", sv);
    assertThat(Ints.asList(z3d.getShape())).isEqualTo(expected);

    Array<Number> z1D = vt.getCoordinateArray1D(0, 10, 10);
    assertThat(Ints.asList(z1D.getShape())).isEqualTo(expected.subList(0, 1));

    int timeIndex = 0;
    GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
    for (Object timeCoord : tcs.getTimeOffsetAxis(0)) {
      Array<Number> zt = vt.getCoordinateArray3D(timeIndex++);
      assertThat(Ints.asList(zt.getShape())).isEqualTo(expected);
    }

    Array<Number> vcoord = vt.getCoordinateArray3D(0);
    int[] zshape = vcoord.getShape();
    assertThat(zshape[0]).isEqualTo(zaxis.getNominalSize());
    assertThat(zshape[1]).isEqualTo(yaxis.getNominalSize());
    assertThat(zshape[2]).isEqualTo(xaxis.getNominalSize());
  }


  /*
   * The 3D coordinate array does not return correct shape and values. Just running this simple code to get z values..
   * 
   * url=http://coast-enviro.er.usgs.gov/models/share/erie_test.ncml;
   * var='temp';
   * 
   * z is of shape 20x2x87, it should be 20x87x193.
   */
  @Test
  public void testErie() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/erie_test.ncml";
    String gridName = "temp";
    System.out.printf("testErie %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(2, 20, 87, 193));
      Section s = new Section(Ints.toArray(gcs.getNominalShape()));

      GridHorizCoordinateSystem hcs = gcs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      VerticalTransform vt = gcs.getVerticalTransform();
      assertThat(vt).isNotNull();
      assertThat(vt).isInstanceOf(ucar.nc2.geoloc.vertical.OceanSigma.class);

      Array<Number> z3d = vt.getCoordinateArray3D(0);
      Section sv = new Section(z3d.getShape());
      System.out.printf("3dcoord = %s %n", sv);
      assertThat(Ints.asList(z3d.getShape())).isEqualTo(ImmutableList.of(20, 87, 193));
      assertThat(s.toBuilder().removeRange(0).build()).isEqualTo(sv);

      Array<Number> z1D = vt.getCoordinateArray1D(0, 10, 10);
      assertThat(Ints.asList(z1D.getShape())).isEqualTo(ImmutableList.of(20));

      int timeIndex = 0;
      GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
      for (Object timeCoord : tcs.getTimeOffsetAxis(0)) {
        Array<Number> zt = vt.getCoordinateArray3D(timeIndex++);
        assertThat(Ints.asList(zt.getShape())).isEqualTo(ImmutableList.of(20, 87, 193));
      }
    }
  }

}
