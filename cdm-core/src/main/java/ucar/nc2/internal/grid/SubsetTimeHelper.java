/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import com.google.common.math.DoubleMath;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridTimeCoordinateSystem;

import java.util.Formatter;
import java.util.Optional;

/**
 * Helper class for subsetting and searching, when you need the GridTimeCoordinateSystem.
 * Placed in this package so that its not part of the public API.
 */
public class SubsetTimeHelper {
  private final GridTimeCoordinateSystem tcs;
  public GridAxisPoint runtimeAxis;

  public SubsetTimeHelper(GridTimeCoordinateSystem tcs) {
    this.tcs = tcs;
    this.runtimeAxis = tcs.getRunTimeAxis();
  }

  /**
   * Called by Observation and SingleRuntime, so, runtime if any is ignored.
   * date, dateRange, timePresent
   * timeOffset, timeOffsetIntv, timeLatest, timeFirst; timeAll
   */
  public Optional<? extends GridAxis<?>> subsetTime(GridSubset params, Formatter errlog) {
    GridAxis<?> timeOffsetAxis = tcs.getTimeOffsetAxis(0); // Observation and SingleRuntime

    // anything requiting a date conversion must be done here
    Double wantOffset = null;
    CalendarDate wantTime = params.getDate();
    if (wantTime != null) {
      CalendarDateUnit cdu = tcs.makeOffsetDateUnit(0);
      wantOffset = cdu.makeFractionalOffsetFromRefDate(wantTime);
    } else if (params.getTimePresent()) {
      wantOffset = (double) tcs.makeOffsetDateUnit(0).makeOffsetFromRefDate(CalendarDate.present());
    }
    if (wantOffset != null) {
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetCoord(wantOffset), errlog);
    }

    if (params.getDateRange() != null) {
      CalendarDateRange wantRange = params.getDateRange();
      CalendarDateUnit cdu = tcs.makeOffsetDateUnit(0);
      double wantStart = cdu.makeFractionalOffsetFromRefDate(wantRange.getStart());
      double wantEnd = cdu.makeFractionalOffsetFromRefDate(wantRange.getEnd());
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetRange(CoordInterval.create(wantStart, wantEnd)),
          errlog);
    }

    // timeOffset, timeOffsetIntv, timeLatest, timeFirst; timeAll is the default
    return timeOffsetAxis.subset(params, errlog);
  }

  /**
   * Called by Offset, OffsetRegular, OffsetIrregular, if runtime not set, use the first one
   * runtime, runtimeLatest
   * date, dateRange, timePresent
   * timeOffset, timeOffsetIntv, timeLatest, timeFirst; timeAll
   */
  public Optional<? extends GridAxis<?>> subsetOffset(GridSubset params, Formatter errlog) {
    GridAxisPoint runtimeAxis = tcs.getRunTimeAxis();
    int runIdx = 0; // if nothing set, use the first one.

    if (runtimeAxis != null) {
      if (params.getRunTimeLatest()) {
        runIdx = runtimeAxis.getNominalSize() - 1;
      }

      // runtime, runtimeLatest
      CalendarDate wantRuntime = params.getRunTime();
      if (wantRuntime != null) {
        double want = tcs.getRuntimeDateUnit().makeOffsetFromRefDate(wantRuntime);
        runIdx = search(tcs.getRunTimeAxis(), want);
        if (runIdx < 0) {
          errlog.format("Cant find runtime = %s%n", wantRuntime);
          return Optional.empty();
        }
      } else if (params.getRunTimeLatest()) {
        runIdx = runtimeAxis.getNominalSize() - 1; // LOOK using nominal...
      }

      // LOOK what about subsetting across multiple runtimes ??
      SubsetPointHelper helper = new SubsetPointHelper(runtimeAxis);
      this.runtimeAxis = helper.makeSubsetByIndex(runIdx).build();
    }

    // suppose these were the options for time. Do they have to be processed differently for different
    // GridTimeCoordinateSystem.Type?

    GridAxis<?> timeOffsetAxis = tcs.getTimeOffsetAxis(runIdx);
    CalendarDate wantTime = params.getDate();
    if (wantTime != null) {
      double want = tcs.makeOffsetDateUnit(runIdx).makeOffsetFromRefDate(wantTime);
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetCoord(want), errlog);

    } else if (params.getDateRange() != null) {
      CalendarDateRange wantRange = params.getDateRange();
      CalendarDateUnit cdu = tcs.makeOffsetDateUnit(runIdx);
      double wantStart = cdu.makeFractionalOffsetFromRefDate(wantRange.getStart());
      double wantEnd = cdu.makeFractionalOffsetFromRefDate(wantRange.getEnd());
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetRange(CoordInterval.create(wantStart, wantEnd)),
          errlog);
    } else if (params.getTimePresent()) {
      double want = (double) tcs.makeOffsetDateUnit(runIdx).makeOffsetFromRefDate(CalendarDate.present());
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetCoord(want), errlog);
    }

    // timeOffset, timeOffsetIntv, timeLatest, timeFirst; timeAll is the default
    return timeOffsetAxis.subset(params, errlog);
  }

  private static int search(GridAxis<?> time, double want) {
    if (time.getNominalSize() == 1) {
      return DoubleMath.fuzzyEquals(want, time.getCoordDouble(0), 1.0e-8) ? 0 : -1;
    }
    if (time.isRegular()) {
      double fval = (want - time.getCoordDouble(0)) / time.getResolution();
      double ival = Math.rint(fval);
      return DoubleMath.fuzzyEquals(fval, ival, 1.0e-8) ? (int) ival : (int) -ival - 1; // LOOK
    }

    // otherwise do a binary search
    return time.binarySearch(want);
  }

}
