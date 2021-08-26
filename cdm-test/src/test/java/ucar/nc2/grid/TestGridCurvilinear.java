/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Port of Coverage tests for Grid. Subsetting curvilinear coordinate systems.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGridCurvilinear {
  private static final double TOL = 1e-6;

  @Test
  public void TestNetcdfCurvilinear() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc"; // NetCDF has 2D and 1D
    String gridName = "u";
    System.out.printf("open %s %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertWithMessage(errlog.toString()).that(gds).isNotNull();
      assertThat(gds.getGrids()).hasSize(24);
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();
      assertThat(hcs).isInstanceOf(GridHorizCurvilinear.class);
      System.out.printf("  getLatLonBoundingBox = %s%n", hcs.getLatLonBoundingBox());
      System.out.printf("  getBoundingBox = %s%n", hcs.getBoundingBox());

      // read data with no horiz subsetting
      GridReferencedArray geo = grid.readData(new GridSubset().setTimePresent().setVertCoord(-.05));
      int[] expectedShape = new int[] {1, 1, 22, 12};
      assertThat(geo.data().getShape()).isEqualTo(expectedShape);

      assertThat(geo.data().get(0, 0, 0, 0).doubleValue()).isWithin(TOL).of(0.0036624447);
      assertThat(geo.data().get(0, 0, 21, 11).doubleValue()).isWithin(TOL).of(0.20564626);

      // read data with ProjectionRect subsetting
      ProjectionRect prect = ProjectionRect.fromSpec("-73.966053, 40.076202, 0.3246, 0.242");
      GridReferencedArray geoSubsetP = grid.readData(new GridSubset().setTimePresent().setProjectionBoundingBox(prect));
      assertThat(geoSubsetP.data().getShape()).isEqualTo(new int[] {1, 1, 11, 16});

      /*
       * read data with latlon subsetting
       * LatLonRect bbox = LatLonRect.fromSpec("40.076201, -73.966053, 0.242, 0.3246");
       * GridReferencedArray geoSubset = grid.readData(new GridSubset().setTimePresent().setLatLonBoundingBox(bbox));
       * assertThat(geoSubset.data().getShape()).isEqualTo(new int[] {1, 1, 11, 6});
       */
    }
  }

  @Test
  public void TestNetcdfCurvilinear2D() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc"; // NetCDF Curvilinear 2D only
    String gridName = "hs";
    System.out.printf("open %s %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getGrids()).hasSize(10);
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      GridReferencedArray geo = grid.readData(new GridSubset().setTimePresent());
      assertThat(geo.data().getShape()).isEqualTo(new int[] {1, 151, 171});

      assertThat(geo.data().get(0, 0, 0).doubleValue()).isWithin(TOL).of(1.782);
      assertThat(geo.data().get(0, 11, 0).doubleValue()).isWithin(TOL).of(1.769);
    }
  }

  @Test
  public void TestNetcdfCurvilinear2Dsubset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc"; // NetCDF Curvilinear 2D only
    String gridName = "hs";
    System.out.printf("open %s %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getGrids()).hasSize(10);
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();
      assertThat(hcs.getShape()).isEqualTo(ImmutableList.of(1, 151, 171));

      LatLonRect bbox = new LatLonRect(43.489, -8.5353, 43.371, -8.2420);
      GridReferencedArray geo = grid.readData(new GridSubset().setTimePresent().setLatLonBoundingBox(bbox));

      int[] expectedShape = new int[] {1, 99, 105};
      assertThat(geo.data().getShape()).isEqualTo(expectedShape);

      assertThat(geo.data().get(0, 0, 0).doubleValue()).isWithin(1.7829999923706055);
      assertThat(geo.data().get(0, 11, 0).doubleValue()).isWithin(1.7669999599456787);
    }
  }

  @Test
  public void testNetcdf2D() throws Exception {
    String endpoint = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    String gridName = "salt";
    System.out.printf("open %s %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      LatLonRect bbox = new LatLonRect(43.489, -8.5353, 43.371, -8.2420);
      GridReferencedArray geo = grid.readData(new GridSubset().setTimePresent().setLatLonBoundingBox(bbox));

      int[] expectedShape = new int[] {1, 20, 64, 75};
      assertThat(geo.data().getShape()).isEqualTo(expectedShape);

      assertThat(geo.data().get(0, 0, 0).doubleValue()).isWithin(1.7829999923706055);
      assertThat(geo.data().get(0, 11, 0).doubleValue()).isWithin(1.7669999599456787);
    }
  }

  @Test
  public void TestGribCurvilinear() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    String gridName = "Mixed_layer_depth_surface";
    System.out.printf("open %s %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getGrids()).hasSize(13);
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      GridReferencedArray geo = grid.readData(new GridSubset());
      int[] expectedShape = new int[] {1, 165, 161};
      assertThat(geo.data().getShape()).isEqualTo(expectedShape);
    }
  }

  @Test
  public void TestGribCurvilinearSubset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    String gridName = "Mixed_layer_depth_surface";
    System.out.printf("open %s %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getGrids()).hasSize(7);
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      LatLonRect bbox = new LatLonRect(64.0, -61., 59.0, -52.);

      GridReferencedArray geo = grid.readData(new GridSubset().setTimePresent().setLatLonBoundingBox(bbox));
      int[] expectedShape = new int[] {1, 165, 161};
      assertThat(geo.data().getShape()).isEqualTo(expectedShape);
    }
  }

  @Test
  public void TestGribCurvilinearHorizStride() throws IOException, InvalidRangeException {
    // GRIB Curvilinear
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    String gridName = "Mixed_layer_depth_surface";
    System.out.printf("open %s %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      GridReferencedArray geo = grid.readData(new GridSubset().setTimePresent().setHorizStride(2));

      int[] expectedShape = new int[] {1, 20, 64, 75};
      assertThat(geo.data().getShape()).isEqualTo(expectedShape);

      assertThat(geo.data().get(0, 0, 0).doubleValue()).isWithin(1.7829999923706055);
      assertThat(geo.data().get(0, 11, 0).doubleValue()).isWithin(1.7669999599456787);
    }
  }
}
