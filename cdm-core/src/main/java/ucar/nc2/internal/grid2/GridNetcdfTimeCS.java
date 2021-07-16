/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisDependenceType;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static ucar.nc2.grid2.GridTimeCoordinateSystem.Type.Observation;

/** Implementation of GridTimeCoordinateSystem. */
@Immutable
public class GridNetcdfTimeCS implements GridTimeCoordinateSystem {

  public static GridNetcdfTimeCS create(GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis) {
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits()).orElseThrow();
    return new GridNetcdfTimeCS(runTimeAxis.getNominalSize() == 1 ? Type.SingleRuntime : Type.Offset, runTimeAxis,
        timeOffsetAxis, dateUnit);
  }

  public static GridNetcdfTimeCS create(GridAxis<?> timeAxis) {
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, timeAxis.getUnits()).orElseThrow();
    return new GridNetcdfTimeCS(Observation, null, timeAxis, dateUnit);
  }

  ////////////////////////////////////////////////////////
  private final Type type;
  private final @Nullable GridAxisPoint runTimeAxis;
  private final GridAxis<?> timeOffsetAxis;
  private final CalendarDateUnit calendarDateUnit;
  private final @Nullable CalendarPeriod offsetPeriod; // is null ok?

  private GridNetcdfTimeCS(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit calendarDateUnit) {
    this.type = type;
    this.runTimeAxis = runTimeAxis;
    this.timeOffsetAxis = timeOffsetAxis;
    this.calendarDateUnit = calendarDateUnit;
    this.offsetPeriod = CalendarPeriod.of(timeOffsetAxis.getUnits());
  }


  @Override
  public Type getType() {
    return type;
  }

  @Override
  public CalendarDateUnit getCalendarDateUnit() {
    return calendarDateUnit;
  }

  @Override
  public CalendarDate getBaseDate() {
    return this.calendarDateUnit.getBaseDateTime();
  }

  @Override
  public List<Integer> getNominalShape() {
    return getMaterializedShape();
  }

  @Override
  public List<Integer> getMaterializedShape() {
    List<Integer> result = new ArrayList<>();
    if (runTimeAxis != null && runTimeAxis.getDependenceType() == GridAxisDependenceType.independent) {
      result.add(runTimeAxis.getNominalSize());
    }
    if (timeOffsetAxis != null && timeOffsetAxis.getDependenceType() == GridAxisDependenceType.independent) {
      result.add(timeOffsetAxis.getNominalSize());
    }
    return result;
  }

  @Nullable
  @Override
  public GridAxisPoint getRunTimeAxis() {
    return runTimeAxis;
  }

  @Nullable
  @Override
  public CalendarDate getRuntimeDate(int runIdx) {
    if (this.type == Observation) {
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
    if (this.type == Observation) {
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

  @Override
  public String toString() {
    return "GridTimeCS{" + "type=" + type + ", runTimeAxis=" + runTimeAxis + ", timeOffsetAxis=" + timeOffsetAxis + '}';
  }

  // LOOK test use of SubsetTimeHelper, likely wrong.
  public Optional<GridNetcdfTimeCS> subset(GridSubset params, Formatter errlog) {
    SubsetTimeHelper helper = new SubsetTimeHelper(this);

    if (type == Observation || type == Type.SingleRuntime) {
      return helper.subsetTime(params, errlog).map(timeOffset -> GridNetcdfTimeCS.create(timeOffset));
    } else {
      return helper.subsetOffset(params, errlog)
          .map(timeOffset -> GridNetcdfTimeCS.create(helper.runtimeAxis, timeOffset));
    }
  }

}
