/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.constants.AxisType;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestReadTime2D {
  private static final double TOL = 1.0e-7f;

  /** Test Grib 2D time that is regular, not orthogonal. */
  @Test
  public void testRegularTime2D() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "datasets/NDFD-CONUS-5km/NDFD-CONUS-5km.ncx4";
    String gridName = "Maximum_temperature_height_above_ground_12_Hour_Maximum";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(4, 4, 1, 689, 1073));

      GridAxisPoint reftime = (GridAxisPoint) gcs.findCoordAxisByType(AxisType.RunTime);
      assertThat((Object) reftime).isNotNull();
      assertThat(reftime.getNominalSize()).isEqualTo(4);
      double[] want = new double[] {0., 12., 24., 36.}; // used to be in hours
      int hour = 0;
      for (Number value : reftime) {
        assertThat(value.doubleValue()).isEqualTo(3600.0 * want[hour++]);
      }

      GridAxisInterval validtime = (GridAxisInterval) gcs.findCoordAxisByType(AxisType.TimeOffset);
      assertThat((Object) validtime).isNotNull();
      assertThat(validtime.getNominalSize()).isEqualTo(4);
      double[] start = new double[] {84, 108, 132, 156}; // used to be in hours
      double[] end = new double[] {96, 120, 144, 168}; // used to be in hours
      hour = 0;
      for (CoordInterval intv : validtime) {
        assertThat(intv.start()).isEqualTo(start[hour]);
        assertThat(intv.end()).isEqualTo(end[hour]);
        hour++;
      }
    }
  }

  /** Test Grib 2D time where runtime and time have different units. */
  @Test
  public void testTimeUnitsDiffer() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    String gridName = "Vertical_speed_shear_tropopause";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(6, 21, 65, 93));

      GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      assertThat(tcs.getNominalShape()).isEqualTo(ImmutableList.of(6, 21));
      assertThat(tcs.getOffsetPeriod()).isEqualTo(CalendarPeriod.of(1, CalendarPeriod.Field.Hour));
      assertThat(tcs.getRuntimeDateUnit().getCalendarPeriod())
          .isEqualTo(CalendarPeriod.of(1, CalendarPeriod.Field.Second));

      GridAxisPoint reftime = tcs.getRunTimeAxis();
      assertThat((Object) reftime).isNotNull();
      assertThat(reftime.getNominalSize()).isEqualTo(6);
      assertThat(reftime.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(reftime.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      CalendarDate startDate = tcs.getRuntimeDate(0);
      assertThat(startDate).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2014-10-24T00:00Z").orElseThrow());
      assertThat(tcs.getRuntimeDateUnit())
          .isEqualTo(CalendarDateUnit.fromUdunitString(null, "secs since 2014-10-24T00:00Z").orElseThrow());

      int count = 0;
      for (Number value : reftime) {
        assertThat(value.doubleValue()).isEqualTo(6 * 3600.0 * count); // seconds
        count++;
      }

      GridAxisPoint validtime = (GridAxisPoint) tcs.getTimeOffsetAxis(0);
      assertThat((Object) validtime).isNotNull();
      assertThat(validtime.getNominalSize()).isEqualTo(21);
      assertThat(validtime.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(validtime.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat(validtime.getUnits()).isEqualTo("hours");

      double[] vals = new double[validtime.getNominalSize()];
      count = 0;
      for (Number value : validtime) {
        vals[count] = value.doubleValue();
        assertThat(value.doubleValue()).isEqualTo(6 * count); // hours
        count++;
      }

      CalendarDate[] cdates = new CalendarDate[validtime.getNominalSize()];
      count = 0;
      for (CalendarDate value : tcs.getTimesForRuntime(0)) {
        cdates[count] = value;
        count++;
      }
    }
  }

  // problem here is fractional units:
  // days since 2009-06-14 04:00:00; values are 132.00346, 132.0104, 132.01736...
  // coordsys, dt, coverage dont work correctly, probably never did (!)
  @Test
  public void testFractionalTimeValues() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc";
    String gridName = "elev";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(432, 22, 12));

      GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      assertThat(tcs.getNominalShape()).isEqualTo(ImmutableList.of(432));
      assertThat(tcs.getOffsetPeriod()).isEqualTo(CalendarPeriod.of(1, CalendarPeriod.Field.Day));
      assertThat(tcs.getRuntimeDateUnit().getCalendarPeriod()).isEqualTo(tcs.getOffsetPeriod());

      assertThat((Object) tcs.getRunTimeAxis()).isNull();

      GridAxisPoint validtime = (GridAxisPoint) tcs.getTimeOffsetAxis(0);
      assertThat((Object) validtime).isNotNull();
      assertThat(validtime.getNominalSize()).isEqualTo(432);
      assertThat(validtime.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(validtime.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat(validtime.getUnits()).isEqualTo("days since 2009-06-14 04:00:00 +00:00");

      CalendarDateUnit cdu = tcs.makeOffsetDateUnit(0);
      int count = 0;
      for (CalendarDate cdate : tcs.getTimesForRuntime(0)) {
        double value = cdu.makeFractionalOffsetFromRefDate(cdate);
        assertThat(value).isWithin(TOL).of(validtime.getCoordinate(count).doubleValue());
        count++;
      }
    }
  }
}
