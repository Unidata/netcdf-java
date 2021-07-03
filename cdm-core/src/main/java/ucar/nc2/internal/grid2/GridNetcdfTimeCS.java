/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisDependenceType;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** Implementation of GridTimeCoordinateSystem. */
public class GridNetcdfTimeCS implements GridTimeCoordinateSystem {

  public static GridNetcdfTimeCS create(GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis) {
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits()).orElseThrow();
    return new GridNetcdfTimeCS(runTimeAxis.getNominalSize() == 1 ? Type.SingleRuntime : Type.Offset, runTimeAxis,
        timeOffsetAxis, dateUnit);
  }

  public static GridNetcdfTimeCS create(GridAxis<?> timeAxis) {
    // LOOK time, not timeOffset. OK for obs??
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, timeAxis.getUnits()).orElseThrow();
    return new GridNetcdfTimeCS(Type.Observation, null, timeAxis, dateUnit);
  }

  ////////////////////////////////////////////////////////
  private final Type type;
  private final @Nullable GridAxisPoint runTimeAxis;
  private final GridAxis<?> timeOffsetAxis;
  private final CalendarDateUnit dateUnit;

  private GridNetcdfTimeCS(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit dateUnit) {
    this.type = type;
    this.runTimeAxis = runTimeAxis;
    this.timeOffsetAxis = timeOffsetAxis;
    this.dateUnit = dateUnit;
  }


  @Override
  public Type getType() {
    return type;
  }

  @Override
  public CalendarDateUnit getCalendarDateUnit() {
    return dateUnit;
  }

  @Override
  public CalendarDate getBaseDate() {
    return this.dateUnit.getBaseDateTime();
  }

  @Override
  public List<Integer> getNominalShape() {
    List<Integer> result = new ArrayList<>();
    if (runTimeAxis != null && runTimeAxis.getDependenceType() == GridAxisDependenceType.independent) {
      result.add(runTimeAxis.getNominalSize());
    }
    if (timeOffsetAxis != null && timeOffsetAxis.getDependenceType() == GridAxisDependenceType.independent) {
      result.add(timeOffsetAxis.getNominalSize());
    }
    return result;
  }

  @Override
  public List<ucar.array.Range> getSubsetRanges() {
    List<ucar.array.Range> result = new ArrayList<>();
    if (runTimeAxis != null) {
      result.add(runTimeAxis.getSubsetRange());
    }
    if (timeOffsetAxis != null) {
      result.add(timeOffsetAxis.getSubsetRange());
    }
    return result;
  }

  @Nullable
  @Override
  public GridAxisPoint getRunTimeAxis() {
    return runTimeAxis;
  }

  @Override
  public CalendarDate getRuntimeDate(int runIdx) {
    if (runTimeAxis == null) {
      return null;
    }
    return this.dateUnit.makeCalendarDate((long) runTimeAxis.getCoordMidpoint(runIdx));
  }

  @Nullable
  @Override
  public GridAxis<?> getTimeOffsetAxis(int runIdx) {
    return timeOffsetAxis;
  }

  @Override
  public List<CalendarDate> getTimesForRuntime(int runIdx) {
    GridAxis<?> timeAxis = getTimeOffsetAxis(runIdx);
    List<CalendarDate> result = new ArrayList<>();
    for (int i = 0; i < timeAxis.getNominalSize(); i++) {
      result.add(this.dateUnit.makeCalendarDate((long) timeAxis.getCoordMidpoint(i)));
    }
    return result;
  }

  @Override
  public String toString() {
    return "GridTimeCS{" + "type=" + type + ", runTimeAxis=" + runTimeAxis + ", timeOffsetAxis=" + timeOffsetAxis + '}';
  }

  public Optional<GridNetcdfTimeCS> subset(GridSubset params, Formatter errlog) {
    // theres always a timeOffset
    Optional<? extends GridAxis<?>> timeOffsetOpt = timeOffsetAxis.isInterval() ? timeOffsetAxis.subset(params, errlog)
        : ((GridAxisPoint) timeOffsetAxis).subset(this, params, errlog);

    if (timeOffsetOpt.isEmpty()) {
      return Optional.empty();
    }
    GridAxis<?> timeOffset = timeOffsetOpt.get();

    // Handle runtime specially, need to convert dates, and may be very large.
    if (runTimeAxis != null) {
      Optional<? extends GridAxisPoint> runtimeSubset = runTimeAxis.subset(this, params, errlog);
      return runtimeSubset.map(rt -> GridNetcdfTimeCS.create(rt, timeOffset));

    } else {
      return Optional.of(GridNetcdfTimeCS.create(timeOffset));
    }
  }

}
