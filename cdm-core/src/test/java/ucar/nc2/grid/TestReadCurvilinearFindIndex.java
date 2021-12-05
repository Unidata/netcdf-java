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
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link GridDataset} that is curvilinear. */
@Category(NeedsCdmUnitTest.class)
public class TestReadCurvilinearFindIndex {

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

      GridReader reader = grid.getReader().setTimeLatest();
      GridReferencedArray geoArray = reader.read();
      Array<Number> data = geoArray.data();
      System.out.printf("data = %s%n", data);
      data = Arrays.reduce(data);
      System.out.printf("reduced = %s%n", data);

      assertThat(data.get(50, 134).doubleValue()).isWithin(1e-4).of(5.955);
      assertThat(data.get(81, 30).doubleValue()).isWithin(1e-4).of(9.404);

      Optional<GridHorizCoordinateSystem.CoordReturn> cro = hcs.findXYindexFromCoord(-8.446327, 43.380638);
      assertThat(cro).isPresent();
      GridHorizCoordinateSystem.CoordReturn cr = cro.get();
      assertThat(cr.yindex).isEqualTo(79);
      assertThat(cr.xindex).isEqualTo(46);

      System.out.printf("CoordReturn = %s%n", cr);
      Number val = data.get(cr.yindex, cr.xindex);
      System.out.printf("val = %s%n", val);
      assertThat(val.doubleValue()).isWithin(1e-4).of(25.75);

      // ProjectionPoint = ProjectionPoint{x=-8.621608636575115, y=43.63934145361693}
      assertThat(hcs.findXYindexFromCoord(-8.621608636575115, 43.63934145361693).isPresent());

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordinateSystem().isLatLon()).isTrue();
      assertThat(mcs.getHorizCoordinateSystem().isCurvilinear()).isTrue();
      assertThat((Object) mcs.getXHorizAxis()).isNotNull();
      assertThat((Object) mcs.getYHorizAxis()).isNotNull();

      assertThat((Object) mcs.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat((Object) mcs.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());

      GridHorizCoordinateSystem mhcs = mcs.getHorizCoordinateSystem();
      assertThat(mhcs).isInstanceOf(GridHorizCurvilinear.class);
      System.out.printf("  llbb = %s%n", mhcs.getLatLonBoundingBox());
      System.out.printf("  mapArea = %s%n", mhcs.getBoundingBox());
    }
  }

  @Test
  public void TestGribCurvilinear() throws Exception {
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
      System.out.printf("GridCoordinateSystem = %s%n", cs);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();
      System.out.printf("GridHorizCoordinateSystem = %s%n", hcs);

      Optional<GridHorizCoordinateSystem.CoordReturn> cro = hcs.findXYindexFromCoord(-6.6, -6.6);
      assertThat(cro).isPresent();
      GridHorizCoordinateSystem.CoordReturn cr = cro.get();
      System.out.printf("%nCoordReturn = %s%n", cr);
      assertThat(cr.xindex).isEqualTo(124);
      assertThat(cr.yindex).isEqualTo(393);

      // -23.479818, -4.231693
      cro = hcs.findXYindexFromCoord(-23.479818, -4.231693);
      assertThat(cro).isPresent();
      cr = cro.get();
      System.out.printf("%nCoordReturn = %s%n", cr);
      assertThat(cr.xindex).isEqualTo(145);
      assertThat(cr.yindex).isEqualTo(523);

      // -23.152880, -4.558632
      cro = hcs.findXYindexFromCoord(-23.152880, -4.558632);
      assertThat(cro).isPresent();
      cr = cro.get();
      System.out.printf("%nCoordReturn = %s%n", cr);
      assertThat(cr.xindex).isEqualTo(143);
      assertThat(cr.yindex).isEqualTo(520);

      cro = hcs.findXYindexFromCoord(0, 0);
      assertThat(cro).isPresent();
      cr = cro.get();
      System.out.printf("%nCoordReturn = %s%n", cr);
      assertThat(cr.yindex).isEqualTo(343);
      assertThat(cr.xindex).isEqualTo(167);
    }
  }

}
