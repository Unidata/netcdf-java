/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

/** Test {@link GridDataset} that is curvilinear. */
@Category(NeedsCdmUnitTest.class)
public class TestReadCurvilinearSubsetProjectRect {

  // NetCDF Curvilinear 2D only
  // classifier = time lat lon CURVILINEAR
  // xAxis= lon(y=151, x=171)
  // yAxis= lat(y=151, x=171)
  // zAxis=
  // tAxis= time(time=85)
  // rtAxis=
  // toAxis=
  // ensAxis=
  //
  // axes=(time, lat, lon, )
  @Test
  public void testNetcdfCurvilinear2D() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
    String gridName = "dirm";
    String subset = "-8.541168, 43.409361, 0.123220, 0.105206"; // -> [49, 31] [39, 91]";
    int ncols = 61;
    int nrows = 62;

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertWithMessage(errlog.toString()).that(gridDataset).isNotNull();
      System.out.println("readGridDataset: " + gridDataset.getLocation());
      assertThat(gridDataset.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);

      Grid grid = gridDataset.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      GridHorizCoordinateSystem hcs = gcs.getHorizCoordinateSystem();
      assertThat(hcs.isLatLon()).isTrue();
      assertThat(hcs.isCurvilinear()).isTrue();
      assertThat(hcs.getProjection()).isNotNull();
      assertThat(hcs).isInstanceOf(GridHorizCurvilinear.class);

      GridReader reader = grid.getReader().setTimeLatest().setProjectionBoundingBox(ProjectionRect.fromSpec(subset));
      GridReferencedArray geoArray = reader.read();
      Array<Number> data = geoArray.data();
      data = Arrays.reduce(data, 0);
      System.out.printf("reduced = %s%n", data);

      // assertThat(data.get(50, 134).doubleValue()).isWithin(1e-4).of(5.955);
      // assertThat(data.get(81, 30).doubleValue()).isWithin(1e-4).of(9.404);

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordinateSystem().isLatLon()).isTrue();
      assertThat(mcs.getHorizCoordinateSystem().isCurvilinear()).isTrue();
      assertThat((Object) mcs.getXHorizAxis()).isNotNull();
      assertThat((Object) mcs.getYHorizAxis()).isNotNull();

      assertThat(mcs.getXHorizAxis().getNominalSize()).isEqualTo(ncols);
      assertThat(mcs.getYHorizAxis().getNominalSize()).isEqualTo(nrows);

      GridHorizCoordinateSystem mhcs = mcs.getHorizCoordinateSystem();
      assertThat(mhcs).isInstanceOf(GridHorizCurvilinear.class);
      System.out.printf("  llbb = %s%n", mhcs.getLatLonBoundingBox());
      System.out.printf("  mapArea = %s%n", mhcs.getBoundingBox());

      GridHorizCurvilinear hcsc = (GridHorizCurvilinear) mhcs;
      Array<Double> latEdge = hcsc.getLatEdges();
      Array<Double> lonEdge = hcsc.getLonEdges();
      assertThat(latEdge.getShape()).isEqualTo(new int[] {nrows + 1, ncols + 1});
      assertThat(lonEdge.getShape()).isEqualTo(new int[] {nrows + 1, ncols + 1});

      for (GridHorizCoordinateSystem.CellBounds edge : hcsc.cells()) {
        assertThat(edge).isNotNull();
      }
    }
  }

  @Test
  public void testNetcdfCurvilinearNoIntersection() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
    String gridName = "dirm";
    String subsetOff = "8.402816, 43.467008, 0.230588, 0.127544";

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertWithMessage(errlog.toString()).that(gridDataset).isNotNull();
      System.out.println("readGridDataset: " + gridDataset.getLocation());
      assertThat(gridDataset.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);

      Grid grid = gridDataset.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();

      assertThrows(InvalidRangeException.class, () -> {
        GridReader reader =
            grid.getReader().setTimeLatest().setProjectionBoundingBox(ProjectionRect.fromSpec(subsetOff));
        reader.read();
      });
    }
  }

}
