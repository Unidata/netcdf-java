/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the time coordinates of a GridCoordinateSystem.
 * The complexity is due to Forecast Model Run Collections (FMRC), in which the time coordinate
 * depends on the forecast run.
 * LOOK could specialize FmrcTimeCoordinateSystem
 */
public interface GridTimeCoordinateSystem {
  enum Type {
    Observation, // No runtimes
    SingleRuntime, // Single runtime
    Offset, // All runtimes have the same offsets
    OffsetRegular, // All runtimes, grouped by time since 0z, have the same offsets.
    OffsetIrregular // Runtimes have irregular offsets
  }

  Type getType();

  CalendarDateUnit getCalendarDateUnit();

  CalendarDate getBaseDate(); // the earliest runtime or observation date.

  List<Integer> getNominalShape();

  List<Integer> getMaterializedShape();

  default List<ucar.array.Range> getSubsetRanges() {
    List<ucar.array.Range> result = new ArrayList<>();
    if (getRunTimeAxis() != null) {
      result.add(getRunTimeAxis().getSubsetRange());
    }
    result.add(getTimeOffsetAxis(0).getSubsetRange());
    return result;
  }

  /**
   * Get the Runtime axis.
   * Null if type=Observation.
   */
  @Nullable
  GridAxisPoint getRunTimeAxis();

  /**
   * Get the ith runtime CalendarDate.
   * Null if type=Observation.
   */
  @Nullable
  CalendarDate getRuntimeDate(int runIdx);

  /**
   * Get the ith timeOffset axis. The offsets are reletive to getBaseDate()
   * if type=Observation, SingleRuntime or Offset, runIdx is ignored, since the offsets are
   * always the same. LOOK does unit reflect getBaseDate() or getRuntimeDate(int runIdx) ?
   */
  GridAxis<?> getTimeOffsetAxis(int runIdx);

  /**
   * Get the forecast/valid dates for a given run.
   * if type=Observation or SingleRuntime, runIdx is ignored.
   * For intervals this is the midpoint.
   */
  List<CalendarDate> getTimesForRuntime(int runIdx);
}
