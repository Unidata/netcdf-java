/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.grid2.DatasetClassifier;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link GridDataset} that is curvilinear. */
@Category(NeedsCdmUnitTest.class)
public class TestReadGridCurvilinear {

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
  public void testNetcdfCurvilinear2D() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
    testClassifier(filename);
    /*
     * readGrid(filename, "rtp", ImmutableList.of(85, 151, 171),
     * "time lat lon", true, 85, "seconds since 2012-04-25 12:00:00", "2006-09-26T03:00Z",
     * "2006-09-26T06:00Z", "2006-09-26T06:00Z", 0.0, 2.0, new int[] {1, 1, 103, 108});
     */
  }

  // NetCDF has 2D and 1D
  // classifier = time sigma lat ypos lon xpos CURVILINEAR
  // xAxis= lon(ypos=22, xpos=12) LOOK why does it choose the 2D? because thers no projection
  // yAxis= lat(ypos=22, xpos=12)
  // zAxis= sigma(sigma=11)
  // tAxis= time(time=432)
  // rtAxis=
  // toAxis=
  // ensAxis=
  //
  // axes=(time, sigma, lat, lon, )
  @Test
  public void testNetcdfCurvilinear() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc";
    testClassifier(filename);
  }

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
  public void testNetcdf2D() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    testClassifier(filename);
  }

  public void testClassifier(String filename) throws IOException {
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

  @Test
  public void testGribCurvilinear() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    readGrid(filename, "Sea_Surface_Height_Relative_to_Geoid_surface", ImmutableList.of(1, 1, 1684, 1200),
        "reftime time lataxis lonaxis", true, 1, "hours since 2009-11-22T00:00Z", "2009-11-22T00:00Z",
        "2009-11-23T00:00Z", "2009-11-23T00:00Z", "2009-11-23T00:00Z", null, null, new int[] {1, 1, 1684, 1200});
  }

  private void readGrid(String filename, String gridName, List<Integer> nominalShape, String gcsName, boolean isLatLon,
      int ntimes, String timeUnit, String firstRuntime, String firstTime, String lastTime, String wantDateS,
      Double wantVert, Double expectVert, int[] matShape) throws IOException, InvalidRangeException {

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gridDataset).isNotNull();
      System.out.println("readGridDataset: " + gridDataset.getLocation());

      Grid grid = gridDataset.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      GridHorizCoordinateSystem hcs = gcs.getHorizCoordinateSystem();
      assertThat(hcs.isLatLon()).isEqualTo(isLatLon);
      assertThat(hcs.getProjection()).isNotNull();
      assertThat((Object) gcs.getXHorizAxis()).isNotNull();
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis() != null).isEqualTo(wantVert != null);
      assertThat(gcs.getNominalShape()).isEqualTo(nominalShape);

      assertThat(gcs.getGridAxes()).hasSize(nominalShape.size());
      assertThat(gcs.getName()).isEqualTo(gcsName);

      GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
      assertThat(tcs == null).isEqualTo(ntimes == 0);
      if (tcs != null) {
        int shapeIdx = 0;
        if (tcs.getRunTimeAxis() != null) {
          assertThat(tcs.getRunTimeAxis().getNominalSize()).isEqualTo(nominalShape.get(shapeIdx++));
          assertThat(tcs.getRuntimeDate(0).toString()).isEqualTo(firstRuntime);
        }
        assertThat(tcs.getCalendarDateUnit().toString()).isEqualTo(timeUnit);
        assertThat((Object) tcs.getTimeOffsetAxis(0)).isNotNull();
        List<CalendarDate> dates = tcs.getTimesForRuntime(0);
        assertThat(dates.size()).isEqualTo(nominalShape.get(shapeIdx++));
        assertThat(dates.get(0).toString()).isEqualTo(firstTime);
        assertThat(dates.get(ntimes - 1).toString()).isEqualTo(lastTime);
      }

      CalendarDate wantDate =
          wantDateS == null ? CalendarDate.present() : CalendarDate.fromUdunitIsoDate(null, wantDateS).orElseThrow();
      GridReferencedArray geoArray =
          grid.getReader().setTime(wantDate).setVertCoord(wantVert == null ? 0.0 : wantVert).read();
      Array<Number> data = geoArray.data();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(data.getRank()).isEqualTo(matShape.length);
      assertThat(data.getShape()).isEqualTo(matShape);

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();
      assertThat(mcs.getHorizCoordSystem().isLatLon()).isEqualTo(isLatLon);
      assertThat((Object) mcs.getXHorizAxis()).isNotNull();
      assertThat((Object) mcs.getYHorizAxis()).isNotNull();
      assertThat(mcs.getVerticalAxis() == null).isEqualTo(wantVert == null);
      List<Integer> matShapeList = Ints.asList(matShape);
      assertThat(mcs.getMaterializedShape()).isEqualTo(matShapeList);

      assertThat((Object) mcs.getXHorizAxis()).isEqualTo(gcs.getXHorizAxis());
      assertThat((Object) mcs.getYHorizAxis()).isEqualTo(gcs.getYHorizAxis());
      GridAxis<?> vert = mcs.getVerticalAxis();
      if (vert != null) {
        assertThat(vert.getNominalSize()).isEqualTo(1);
        assertThat(vert.getCoordMidpoint(0)).isEqualTo(expectVert);
      }

      GridTimeCoordinateSystem mtcs = mcs.getTimeCoordSystem();
      assertThat(mtcs == null).isEqualTo(ntimes == 0);
      if (mtcs != null) {
        assertThat((Object) mtcs.getTimeOffsetAxis(0)).isNotNull();
        GridAxis<?> time = mtcs.getTimeOffsetAxis(0);
        assertThat(time.getNominalSize()).isEqualTo(1);
        List<CalendarDate> times = mtcs.getTimesForRuntime(0);
        assertThat(times.size()).isEqualTo(1);
        CalendarDate cd = times.get(0);
        assertThat(cd.toString()).isEqualTo(wantDate.toString());
      }
    }
  }

}
