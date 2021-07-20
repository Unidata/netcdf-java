/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Implementation of GridTimeCoordinateSystem. */
@Immutable
public class GridNetcdfTimeCS extends GridTimeCoordinateSystem {

  public static GridNetcdfTimeCS create(GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis) {
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits()).orElseThrow();
    return new GridNetcdfTimeCS(runTimeAxis.getNominalSize() == 1 ? Type.SingleRuntime : Type.Offset, runTimeAxis,
        timeOffsetAxis, dateUnit);
  }

  public static GridNetcdfTimeCS create(GridAxis<?> timeAxis) {
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, timeAxis.getUnits()).orElseThrow();
    return new GridNetcdfTimeCS(Type.Observation, null, timeAxis, dateUnit);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////

  @Nullable
  @Override
  public CalendarDate getRuntimeDate(int runIdx) {
    if (this.runTimeAxis == null) {
      return null;
    } else {
      return calendarDateUnit.makeCalendarDate(this.runTimeAxis.getCoordinate(runIdx).longValue());
    }
  }

  @Override
  public GridAxis<?> getTimeOffsetAxis(int runIdx) {
    return timeOffsetAxis; // So we are only supporting Orthogonal times right now
  }

  @Override
  public List<CalendarDate> getTimesForRuntime(int runIdx) {
    if (this.type == Type.Observation) {
      return getTimesForObservation();
    } else {
      return getTimesForNonObservation(runIdx);
    }
  }

  private List<CalendarDate> getTimesForObservation() {
    List<CalendarDate> result = new ArrayList<>();
    for (int timeIdx = 0; timeIdx < timeOffsetAxis.getNominalSize(); timeIdx++) {
      result.add(this.calendarDateUnit.makeCalendarDate((long) timeOffsetAxis.getCoordMidpoint(timeIdx)));
    }
    return result;
  }

  private List<CalendarDate> getTimesForNonObservation(int runIdx) {
    Preconditions.checkArgument(runTimeAxis != null && runIdx >= 0 && runIdx < runTimeAxis.getNominalSize());
    CalendarDate baseForRun = getRuntimeDate(runIdx);
    GridAxis<?> timeAxis = getTimeOffsetAxis(runIdx);
    List<CalendarDate> result = new ArrayList<>();
    for (int offsetIdx = 0; offsetIdx < timeAxis.getNominalSize(); offsetIdx++) {
      result.add(baseForRun.add((long) timeAxis.getCoordMidpoint(offsetIdx), this.offsetPeriod));
    }
    return result;
  }

  // LOOK test use of SubsetTimeHelper, likely wrong.
  public Optional<GridNetcdfTimeCS> subset(GridSubset params, Formatter errlog) {
    SubsetTimeHelper helper = new SubsetTimeHelper(this);

    if (type == Type.Observation || type == Type.SingleRuntime) {
      return helper.subsetTime(params, errlog).map(timeOffset -> GridNetcdfTimeCS.create(timeOffset));
    } else {
      return helper.subsetOffset(params, errlog)
          .map(timeOffset -> GridNetcdfTimeCS.create(helper.runtimeAxis, timeOffset));
    }
  }

  ////////////////////////////////////////////////////////

  private GridNetcdfTimeCS(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit calendarDateUnit) {
    super(type, runTimeAxis, timeOffsetAxis, calendarDateUnit);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridNetcdfTimeCS that = (GridNetcdfTimeCS) o;
    return type == that.type && Objects.equals(runTimeAxis, that.runTimeAxis)
        && Objects.equals(timeOffsetAxis, that.timeOffsetAxis)
        && Objects.equals(calendarDateUnit, that.calendarDateUnit) && Objects.equals(offsetPeriod, that.offsetPeriod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, runTimeAxis, timeOffsetAxis, calendarDateUnit, offsetPeriod);
  }
}
