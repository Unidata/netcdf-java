/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.InvalidRangeException;
import ucar.nc2.grib.collection.Grib;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGridSubset {

  @Test
  public void testRegular() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/nuwg/03061219_ruc.nc";
    String gridName = "T";
    System.out.printf("testRegular %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(2, 19, 65, 93));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridAxis<?> zaxis = csys.getVerticalAxis();
      assertThat((Object) zaxis).isNotNull();
      assertThat(zaxis.getUnits()).isEqualTo("hectopascals");

      GridReferencedArray geoArray = grid.getReader().setHorizStride(3).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {2, 19, 22, 31});

      GridAxis<?> zaxis2 = mcs.getVerticalAxis();
      assertThat((Object) zaxis2).isNotNull();
      assertThat(zaxis2.getUnits()).isEqualTo("hectopascals");

      for (GridAxis<?> axis : mcs.getGridAxes()) {
        assertThat(axis.getAxisType()).isNotNull();
      }
    }
  }

  @Test
  public void testGrib() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/AVN.wmo";
    String gridName = "Temperature_isobaric";
    System.out.printf("testGrib %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGridByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_7-0-2-11_L100").orElseThrow(); // "Temperature_isobaric");
      assertThat(grid).isNotNull();
      assertThat(grid.getName()).isEqualTo(gridName);

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(1, 3, 9, 39, 45));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridReferencedArray geoArray = grid.getReader().setHorizStride(3).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 3, 9, 13, 15});

      for (GridAxis<?> axis : mcs.getGridAxes()) {
        assertThat(axis.getAxisType()).isNotNull();
      }
    }
  }

  @Test
  public void testWRF() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc";
    String gridName = "T";
    System.out.printf("testWRF %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(13, 27, 60, 73));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridAxis<?> zaxis = csys.getVerticalAxis();
      assertThat((Object) zaxis).isNotNull();
      assertThat(zaxis.getNominalSize()).isEqualTo(27);

      VerticalTransform vt = csys.getVerticalTransform();
      assertThat(vt).isNotNull();
      assertThat(vt.getUnitString()).isEqualTo("Pa");

      GridReferencedArray geoArray = grid.getReader().setHorizStride(3).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {13, 27, 20, 25});

      GridAxis<?> zaxis2 = mcs.getVerticalAxis();
      assertThat((Object) zaxis2).isNotNull();
      assertThat(zaxis2.getUnits()).isEqualTo(zaxis.getUnits());
    }
  }

  @Test
  public void testMSG() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    String gridName = "Pixel_scene_type";
    System.out.printf("testMSG %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(1, 1, 1237, 1237));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      // bbox = ll: 16.79S 20.5W+ ur: 14.1N 20.09E
      LatLonRect bbox = new LatLonRect(-16.79, -20.5, 14.1, 20.9);
      GridReferencedArray geoArray = grid.getReader().setLatLonBoundingBox(bbox).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 1, 363, 479});
    }
  }

  @Test
  public void test2D() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    String gridName = "salt";
    System.out.printf("test2D %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(1, 20, 64, 128));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      LatLonPoint p0 = LatLonPoint.create(29.0, -90.0);
      LatLonRect bbox = LatLonRect.builder(p0, 1.0, 2.0).build();
      GridReferencedArray geoArray = grid.getReader().setLatLonBoundingBox(bbox).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 20, 64, 55});

      p0 = LatLonPoint.create(30.0, -90.0);
      bbox = LatLonRect.builder(p0, 1.0, 2.0).build();
      geoArray = grid.getReader().setLatLonBoundingBox(bbox).read();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 20, 19, 18});
    }
  }

  // longitude subsetting (CoordAxis1D regular)
  @Test
  public void testLatLonSubset() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "conventions/problem/SUPER-NATIONAL_latlon_IR_20070222_1600.nc";
    String gridName = "micron11";
    System.out.printf("testLatLonSubset %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(800, 1300));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNull();
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      LatLonRect bbox = LatLonRect.builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
      testLatLonSubset(grid, bbox, ImmutableList.of(141, 281));

      bbox = LatLonRect.builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
      testLatLonSubset(grid, bbox, ImmutableList.of(800, 1300));
    }
  }

  private GridHorizCoordinateSystem testLatLonSubset(Grid grid, LatLonRect bbox, List<Integer> expected)
      throws InvalidRangeException, IOException {
    GridReferencedArray geoArray = grid.getReader().setLatLonBoundingBox(bbox).read();
    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();
    GridHorizCoordinateSystem hcs = mcs.getHorizCoordinateSystem();
    assertThat(hcs).isNotNull();
    assertThat(hcs.getShape()).isEqualTo(expected);
    return hcs;
  }

  @Test
  public void testVerticalAxis() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "ncml/nc/cg/CG2006158_120000h_usfc.nc";
    String gridName = "CGusfc";
    System.out.printf("testVerticalAxis %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(1, 1, 29, 26));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridAxis<?> zaxis = csys.getVerticalAxis();
      assertThat((Object) zaxis).isNotNull();
      assertThat(zaxis.getUnits()).isEqualTo("m");

      GridReferencedArray geoArray = grid.getReader().read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 1, 29, 26});

      GridAxis<?> zaxis2 = mcs.getVerticalAxis();
      assertThat((Object) zaxis2).isNotNull();
      assertThat(zaxis2.getUnits()).isEqualTo("m");

      for (GridAxis<?> axis : mcs.getGridAxes()) {
        assertThat(axis.getAxisType()).isNotNull();
      }
    }
  }

  @Test
  public void testBBSubsetVP() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    String gridName = "Pixel_scene_type";
    System.out.printf("testBBSubsetVP %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(1, 1, 1237, 1237));
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      LatLonRect bbox = LatLonRect.builder(LatLonPoint.create(0, 0), 20.0, 40.0).build();
      GridHorizCoordinateSystem hcsSubset = testLatLonSubset(grid, bbox, ImmutableList.of(235, 436));

      LatLonRect expectLBB = LatLonRect.fromSpec("-0.043318, -0.043487, 21.202380, 44.559265");
      assertThat(hcsSubset.getLatLonBoundingBox().nearlyEquals(expectLBB)).isTrue();

      ProjectionRect expectBB = ProjectionRect.fromSpec("-4.502221, -4.570379, 3925.936303, 2148.077947");
      assertThat(hcsSubset.getBoundingBox().nearlyEquals(expectBB)).isTrue();
    }
  }

  // x,y in meters
  @Test
  public void testBBSubsetUnits() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "ncml/testBBSubsetUnits.ncml";
    String gridName = "pr";
    System.out.printf("testBBSubsetUnits %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(8760, 128, 158));
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      System.out.printf("original proj bbox = %s%n", hcs.getBoundingBox());
      System.out.printf("original lat/lon bbox = %s%n", hcs.getLatLonBoundingBox());

      LatLonRect bbox = new LatLonRect(48, -100, 52, -90);
      System.out.printf("subset proj bbox = %s%n", hcs.getProjection().latLonToProjBB(bbox));
      System.out.printf("subset lat/lon bbox = %s%n", bbox);

      GridReferencedArray geoArray = grid.getReader().setTimeLatest().setLatLonBoundingBox(bbox).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getMaterializedShape()).isEqualTo(ImmutableList.of(1, 12, 18));
      GridHorizCoordinateSystem hcsSubset = mcs.getHorizCoordinateSystem();
      assertThat(hcsSubset).isNotNull();
      assertThat(hcsSubset.getShape()).isEqualTo(ImmutableList.of(12, 18));
    }
  }

  // this one has the coordinate bounds set in the file
  @Test
  public void testSubsetCoordEdges() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "ncml/subsetCoordEdges.ncml";
    String gridName = "foo";
    System.out.printf("testSubsetCoordEdges %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(4, 4, 4));
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      double[] expectTime = new double[] {15.5, 45.0, 74.5, 105.0};
      double[] expectTimeEdge = new double[] {0.0, 31.0, 59.0, 90.0, 120.0};
      GridAxis<?> fooTimeAxis = grid.getTimeCoordinateSystem().getTimeOffsetAxis(0);
      for (int i = 0; i < fooTimeAxis.getNominalSize(); i++) {
        assertThat(fooTimeAxis.getCoordDouble(i)).isEqualTo(expectTime[i]);
        assertThat(fooTimeAxis.getCoordInterval(i).start()).isEqualTo(expectTimeEdge[i]);
        assertThat(fooTimeAxis.getCoordInterval(i).end()).isEqualTo(expectTimeEdge[i + 1]);
      }

      GridAxisPoint fooLatAxis = grid.getHorizCoordinateSystem().getYHorizAxis();
      double[] expectLat = new double[] {-54.0, 9.0, 54.0, 81.0};
      double[] expectLatEdge = new double[] {-90.0, -18.0, 36.0, 72.0, 90.0};
      for (int i = 0; i < fooLatAxis.getNominalSize(); i++) {
        assertThat(fooLatAxis.getCoordDouble(i)).isEqualTo(expectLat[i]);
        assertThat(fooLatAxis.getCoordInterval(i).start()).isEqualTo(expectLatEdge[i]);
        assertThat(fooLatAxis.getCoordInterval(i).end()).isEqualTo(expectLatEdge[i + 1]);
      }

      GridAxisPoint fooLonAxis = grid.getHorizCoordinateSystem().getXHorizAxis();
      double[] expectLon = new double[] {18.0, 72.0, 162.0, 288.0};
      double[] expectLonEdge = new double[] {0.0, 36.0, 108.0, 216.0, 360.0};
      for (int i = 0; i < fooLonAxis.getNominalSize(); i++) {
        assertThat(fooLonAxis.getCoordDouble(i)).isEqualTo(expectLon[i]);
        assertThat(fooLonAxis.getCoordInterval(i).start()).isEqualTo(expectLonEdge[i]);
        assertThat(fooLonAxis.getCoordInterval(i).end()).isEqualTo(expectLonEdge[i + 1]);
      }

      // take mid range for all of the 3 coordinates
      LatLonPoint llpt = LatLonPoint.create(9.0, 72.0);
      GridReferencedArray geoArray = grid.getReader().setTimeOffsetCoord(45.0).setLatLonPoint(llpt).read();
      assertThat(geoArray.getMaterializedCoordinateSystem().getMaterializedShape())
          .isEqualTo(ImmutableList.of(1, 1, 1));
      assertThat(geoArray.data().getScalar().doubleValue()).isEqualTo(22.0);
    }
  }

  @Test
  public void testTPgribCollection() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4";
    String gridName = "Pressure_surface";
    System.out.printf("testTPgribCollection %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(2, 181, 360));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      assertThat(tcs.getType()).isEqualTo(GridTimeCoordinateSystem.Type.Observation);
      assertThat((Object) tcs.getRunTimeAxis()).isNull();

      GridAxis<?> time = tcs.getTimeOffsetAxis(0);
      assertThat((Object) time).isNotNull();
      assertThat(time.getNominalSize()).isEqualTo(2);

      double[] expectTime = new double[] {0, 6};
      double[] expectTimeEdge = new double[] {-3, 3, 9};
      GridAxis<?> fooTimeAxis = grid.getTimeCoordinateSystem().getTimeOffsetAxis(0);
      for (int i = 0; i < fooTimeAxis.getNominalSize(); i++) {
        assertThat(fooTimeAxis.getCoordDouble(i)).isEqualTo(expectTime[i]);
        assertThat(fooTimeAxis.getCoordInterval(i).start()).isEqualTo(expectTimeEdge[i]);
        assertThat(fooTimeAxis.getCoordInterval(i).end()).isEqualTo(expectTimeEdge[i + 1]);
      }
    }
  }
}
