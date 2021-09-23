/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Mostly checks GridReferencedArray matches request.
 * Requires that the data exist, not just the gbx9.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGribGridSubsetP {

  @BeforeClass
  public static void before() {
    GribDataReader.validator = new GribCoverageValidator();
  }

  @AfterClass
  public static void after() {
    GribDataReader.validator = null;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    ////////////// dt
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", "RH", null, "2003-06-12T22:00:00Z",
        null, 400.0}); // projection, no reftime, no timeOffset

    ////////////// grib
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1",
        "Pressure_surface", "2012-02-27T00:00:00Z", null, 42.0, null}); // projection, scalar reftime

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4",
        "Momentum_flux_u-component_surface_Mixed_intervals_Average", "2015-03-01T06:00:00Z", "2015-03-01T12:00:00Z",
        null, null}); // time

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4",
        "Momentum_flux_u-component_surface_Mixed_intervals_Average", "2015-03-01T06:00:00Z", null, 213.0, null}); // time
                                                                                                                  // offset

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4",
        "Ozone_Mixing_Ratio_isobaric", "2015-03-01T06:00:00Z", null, 213.0, null}); // all levels

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4",
        "Ozone_Mixing_Ratio_isobaric", "2015-03-01T06:00:00Z", null, 213.0, 10000.}); // specific level

    return result;
  }

  String endpoint, covName;
  CalendarDate rt_val, time_val;
  Double time_offset, vert_level;

  public TestGribGridSubsetP(String endpoint, String covName, String rt_val, String time_val, Double time_offset,
      Double vert_level) {
    this.endpoint = endpoint;
    this.covName = covName;
    this.rt_val = (rt_val == null) ? null : CalendarDate.fromUdunitIsoDate(null, rt_val).orElseThrow();
    this.time_val = (time_val == null) ? null : CalendarDate.fromUdunitIsoDate(null, time_val).orElseThrow();
    this.time_offset = time_offset;
    this.vert_level = vert_level;
  }

  @Test
  public void testGridCoverageDatasetFmrc() throws IOException, ucar.array.InvalidRangeException {
    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid cover = gds.findGrid(covName).orElseThrow();
      assertThat(cover).isNotNull();

      readOne(cover, rt_val, time_val, time_offset, vert_level);
    }
  }

  static void readOne(Grid grid, CalendarDate rt_val, CalendarDate time_val, Double time_offset, Double vert_level)
      throws IOException, ucar.array.InvalidRangeException {
    System.out.printf("===Request Subset %s runtime=%s time=%s timeOffset=%s vert=%s%n", grid.getName(), rt_val,
        time_val, time_offset, vert_level);

    GridSubset subset = new GridSubset();
    if (rt_val != null)
      subset.setRunTime(rt_val);
    if (time_val != null)
      subset.setDate(time_val);
    if (time_offset != null)
      subset.setTimeOffsetCoord(time_offset);
    if (vert_level != null)
      subset.setVertCoord(vert_level);

    GridReferencedArray geoArray = grid.readData(subset);
    MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
    assertThat(mcs).isNotNull();
    System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));

    GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
    assertThat(hcs2).isNotNull();

    GridTimeCoordinateSystem tcs = mcs.getTimeCoordSystem();
    if (rt_val != null) {
      assertThat(tcs).isNotNull();
      GridAxisPoint runAxis = tcs.getRunTimeAxis();
      assertThat((Object) runAxis).isNotNull();
      assertThat(runAxis.getNominalSize()).isEqualTo(1);
      double val = runAxis.getCoordDouble(0);
      CalendarDate runDate = tcs.getRuntimeDateUnit().makeCalendarDate((long) val);
      assertThat(runDate).isEqualTo(rt_val);
    }

    if (time_val != null || time_offset != null) {
      assertThat(tcs).isNotNull();
      GridAxis<?> timeOffsetAxis = tcs.getTimeOffsetAxis(0);
      assertThat((Object) timeOffsetAxis).isNotNull();

      assertThat(timeOffsetAxis.getNominalSize()).isEqualTo(1);
      CalendarDateUnit cdu = tcs.makeOffsetDateUnit(0);
      assertThat(cdu).isNotNull();

      if (time_val != null) {
        if (timeOffsetAxis.isInterval()) {
          CoordInterval intv = timeOffsetAxis.getCoordInterval(0);
          CalendarDate edge1 = cdu.makeCalendarDate((long) intv.start());
          CalendarDate edge2 = cdu.makeCalendarDate((long) intv.end());

          assertThat(!edge1.isAfter(time_val)).isTrue();
          assertThat(!edge2.isBefore(time_val)).isTrue();

        } else {
          double val2 = timeOffsetAxis.getCoordDouble(0);
          CalendarDate forecastDate = cdu.makeCalendarDate((long) val2);
          assertThat(forecastDate).isEqualTo(time_val);
        }

      } else {
        if (timeOffsetAxis.isInterval()) {
          CoordInterval intv = timeOffsetAxis.getCoordInterval(0);
          assertThat(intv.start() <= time_offset).isTrue();
          assertThat(intv.end() >= time_offset).isTrue();

        } else {
          double val2 = timeOffsetAxis.getCoordDouble(0);
          assertThat(val2).isEqualTo(time_offset);
        }
      }

      if (vert_level != null) {
        GridAxis<?> zAxis = mcs.getVerticalAxis();
        assertThat((Object) zAxis).isNotNull();
        assertThat(zAxis.getNominalSize()).isEqualTo(1);
        double val = zAxis.getCoordDouble(0);
        assertThat(val).isEqualTo(vert_level);
      }
    }
  }


  @Test
  public void testFmrcStride() throws IOException, ucar.array.InvalidRangeException {
    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid cover = gds.findGrid(covName).orElseThrow();
      assertThat(cover).isNotNull();
      GridHorizCoordinateSystem hcs = cover.getHorizCoordinateSystem();

      GridReferencedArray geoArray = cover.getReader().setHorizStride(2).read();
      MaterializedCoordinateSystem msys = geoArray.getMaterializedCoordinateSystem();
      GridHorizCoordinateSystem mhcs = msys.getHorizCoordinateSystem();

      int dim = 0;
      for (int size : mhcs.getShape()) {
        assertThat(size).isEqualTo((1 + hcs.getShape().get(dim++)) / 2);
      }
    }
  }

}
