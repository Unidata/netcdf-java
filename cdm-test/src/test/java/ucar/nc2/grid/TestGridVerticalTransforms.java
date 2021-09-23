/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.ArrayDouble;
import ucar.array.InvalidRangeException;
import ucar.ma2.Section;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.OceanSigma;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGridVerticalTransforms {

  @Test
  @Ignore("must know the time dimension for wrf")
  public void testWRF() throws Exception {
    testDataset(TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc");
    testDataset(TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_d01_2006-03-08_21-00-00");
  }

  private void testDataset(String filename) throws IOException, InvalidRangeException {
    System.out.printf("testWRF %s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      testWrfGrid(gds.findGrid("U").orElseThrow());
      testWrfGrid(gds.findGrid("V").orElseThrow());
      testWrfGrid(gds.findGrid("W").orElseThrow());
      testWrfGrid(gds.findGrid("T").orElseThrow());
    }
  }

  private void testWrfGrid(Grid grid) throws IOException, InvalidRangeException {
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
    assertThat(vt.getUnitString()).isNotNull();

    ArrayDouble.D3 vcoord = null;
    try {
      vcoord = vt.getCoordinateArray(0);
    } catch (ucar.ma2.InvalidRangeException e) {
      e.printStackTrace();
    }
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
  @Ignore("must know the time dimension for OceanSigma")
  public void testErie() throws IOException, InvalidRangeException, ucar.ma2.InvalidRangeException {
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
      Section s = Section.makeFromList(gcs.getNominalShape());

      GridHorizCoordinateSystem hcs = gcs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      VerticalTransform vt = gcs.getVerticalTransform();
      assertThat(vt).isNotNull();
      assertThat(vt).isInstanceOf(OceanSigma.class);
      ArrayDouble.D3 z = vt.getCoordinateArray(0);
      Section sv = new Section(z.getShape());
      System.out.printf("3dcoord = %s %n", sv);

      s = s.toBuilder().removeRange(0).build();
      assertThat(s).isEqualTo(sv);
    }
  }

}
