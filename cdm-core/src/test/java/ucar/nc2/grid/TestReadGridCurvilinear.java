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
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.grid.DatasetClassifier;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link GridDataset} that is curvilinear. */
@Category(NeedsCdmUnitTest.class)
public class TestReadGridCurvilinear {

  // NetCDF has 2D only
  // classifier = ocean_time sc_r lat_rho lon_rho CURVILINEAR
  // xAxis= lon_rho(eta_rho=64, xi_rho=128)
  // yAxis= lat_rho(eta_rho=64, xi_rho=128)
  // zAxis= sc_r(sc_r=20)
  // tAxis= ocean_time(ocean_time=1)
  // rtAxis=
  // toAxis=
  // ensAxis=
  //
  // axes=(ocean_time, sc_r, lat_rho, lon_rho, ) {
  @Test
  public void testNetcdf2D() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    testClassifier(filename);
  }

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
    testClassifier(filename);
    /*
     * readGrid(filename, "rtp", ImmutableList.of(85, 151, 171),
     * "time lat lon", true, 85, "seconds since 2012-04-25 12:00:00", "2006-09-26T03:00Z",
     * "2006-09-26T06:00Z", "2006-09-26T06:00Z", 0.0, 2.0, new int[] {1, 1, 103, 108});
     */
  }

  // NetCDF has 2D and 1D with no projection
  // classifier = time sigma lat ypos lon xpos CURVILINEAR
  // xAxis= xpos(xpos=12)
  // yAxis= ypos(ypos=22)
  // latAxis= lat(ypos=22, xpos=12)
  // lonAxis= lon(ypos=22, xpos=12)
  // zAxis= sigma(sigma=11)
  // tAxis= time(time=432)
  // rtAxis=
  // toAxis=
  // ensAxis=
  //
  // axes=(time, sigma, ypos, xpos, )
  @Test
  public void testNetcdfCurvilinear() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc";
    testClassifier(filename);
    readGrid(filename, "wv", ImmutableList.of(432, 22, 12), "time ypos xpos", null, null, new int[] {1, 22, 12});
  }

  // TODO may be flakey because when ncx is generated, time coordinate naming is arbitrary.
  // If persists, maybe dont check cs name.
  @Test
  public void testGribCurvilinear() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    readGrid(filename, "Sea_Surface_Height_Relative_to_Geoid_surface", ImmutableList.of(1, 1, 1684, 1200),
        "reftime time1 Lat Lon", "2009-11-23T00:00Z", null, new int[] {1, 1, 1684, 1200});
  }

  @Test
  public void testCurvilinearGrib() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    String gridName = "3-D_Temperature_depth_below_sea";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
    }
  }

  private void readGrid(String filename, String gridName, List<Integer> nominalShape, String gcsName, String wantDateS,
      Double wantVert, int[] matShape) throws IOException, InvalidRangeException {

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

      assertThat((Object) gcs.getXHorizAxis()).isNotNull();
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis() != null).isEqualTo(wantVert != null);
      assertThat(gcs.getNominalShape()).isEqualTo(nominalShape);

      assertThat(gcs.getGridAxes()).hasSize(nominalShape.size());
      assertThat(gcs.getName()).isEqualTo(gcsName);

      GridReader reader = grid.getReader();
      if (wantDateS != null) {
        reader.setDate(CalendarDate.fromUdunitIsoDate(null, wantDateS).orElseThrow());
      } else {
        reader.setTimeLatest();
      }
      if (wantVert != null) {
        reader.setVertCoord(wantVert);
      }
      GridReferencedArray geoArray = reader.read();
      Array<Number> data = geoArray.data();
      assertThat(data.getRank()).isEqualTo(matShape.length);
      assertThat(data.getShape()).isEqualTo(matShape);

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordinateSystem().isLatLon()).isTrue();
      assertThat(mcs.getHorizCoordinateSystem().isCurvilinear()).isTrue();
      assertThat((Object) mcs.getXHorizAxis()).isNotNull();
      assertThat((Object) mcs.getYHorizAxis()).isNotNull();
      List<Integer> matShapeList = Ints.asList(matShape);
      assertThat(mcs.getMaterializedShape()).isEqualTo(matShapeList);

      assertThat((Object) mcs.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat((Object) mcs.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());

      GridHorizCoordinateSystem mhcs = mcs.getHorizCoordinateSystem();
      assertThat(mhcs).isInstanceOf(GridHorizCurvilinear.class);
      int count = 0;
      for (GridHorizCoordinateSystem.CellBounds bound : mhcs.cells()) {
        count++;
      }
      assertThat(count).isEqualTo(Arrays.computeSize(matShape));
      System.out.printf("  llbb = %s%n", mhcs.getLatLonBoundingBox());
      System.out.printf("  mapArea = %s%n", mhcs.getBoundingBox());
    }
  }

  private void testClassifier(String filename) throws IOException {
    System.out.printf("testClassifier: %s%n", filename);
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
      DatasetClassifier.CoordSysClassifier classifier =
          dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
      assertThat(classifier).isNotNull();
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      System.out.printf("classifier = %s%n", classifier);
    }
  }
}
