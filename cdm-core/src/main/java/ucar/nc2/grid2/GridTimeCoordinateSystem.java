/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import com.google.common.base.Preconditions;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/**
 * Manages the time coordinates of a GridCoordinateSystem.
 * The complexity is due to Forecast Model Run Collections (FMRC), in which the time coordinate
 * depends on the forecast run. This is handled in the subclasses.
 */
public abstract class GridTimeCoordinateSystem {
  public enum Type {
    Observation, // No runtimes
    SingleRuntime, // Single runtime
    Offset, // All runtimes have the same offsets
    OffsetRegular, // All runtimes, grouped by time since 0z, have the same offsets.
    OffsetIrregular // Runtimes have irregular offsets
  }

  public Type getType() {
    return type;
  }

  public CalendarDateUnit getCalendarDateUnit() {
    return calendarDateUnit;
  }

  public CalendarPeriod getOffsetPeriod() {
    return offsetPeriod;
  }

  // the earliest runtime or observation date.
  public CalendarDate getBaseDate() {
    return this.calendarDateUnit.getBaseDateTime();
  }

  public List<Integer> getNominalShape() {
    return getMaterializedShape();
  }

  // Use for MaterializedCoordinateSystem
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

  /**
   * Get the Runtime axis.
   * Null if type=Observation.
   */
  @Nullable
  public GridAxisPoint getRunTimeAxis() {
    return runTimeAxis;
  }

  public List<ucar.array.Range> getSubsetRanges() {
    List<ucar.array.Range> result = new ArrayList<>();
    if (getRunTimeAxis() != null) {
      result.add(getRunTimeAxis().getSubsetRange());
    }
    result.add(getTimeOffsetAxis(0).getSubsetRange());
    return result;
  }


  /**
   * Get the ith runtime CalendarDate.
   * Null if type=Observation.
   */
  @Nullable
  public abstract CalendarDate getRuntimeDate(int runIdx);

  /**
   * Get the ith timeOffset axis. The offsets are reletive to getBaseDate()
   * if type=Observation, SingleRuntime or Offset, runIdx is ignored, since the offsets are
   * always the same. LOOK does unit reflect getBaseDate() or getRuntimeDate(int runIdx) ?
   */
  public abstract GridAxis<?> getTimeOffsetAxis(int runIdx);

  /**
   * Get the forecast/valid dates for a given run.
   * if type=Observation or SingleRuntime, runIdx is ignored.
   * For intervals this is the midpoint.
   */
  public abstract List<CalendarDate> getTimesForRuntime(int runIdx);

  public abstract Optional<? extends GridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog);

  ////////////////////////////////////////////////////////
  protected final Type type;
  protected final @Nullable GridAxisPoint runTimeAxis;
  protected final GridAxis<?> timeOffsetAxis; // ??
  protected final CalendarDateUnit calendarDateUnit;
  protected final CalendarPeriod offsetPeriod;

  protected GridTimeCoordinateSystem(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit calendarDateUnit) {
    this.type = type;
    this.runTimeAxis = runTimeAxis;
    this.timeOffsetAxis = timeOffsetAxis;

    if (calendarDateUnit != null) {
      this.calendarDateUnit = calendarDateUnit;
    } else if (runTimeAxis != null) {
      this.calendarDateUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits()).orElseThrow();
    } else {
      throw new IllegalArgumentException("calendarDateUnit or runTimeAxis must not be null");
    }
    Preconditions.checkNotNull(this.calendarDateUnit);

    CalendarPeriod period = this.calendarDateUnit.getCalendarPeriod();
    if (period == null) {
      period = CalendarPeriod.of(timeOffsetAxis.getUnits());
    }
    this.offsetPeriod = period;
    Preconditions.checkNotNull(this.offsetPeriod);
  }

  @Override
  public String toString() {
    return "GridTimeCoordinateSystem type=" + type + ", calendarDateUnit=" + calendarDateUnit + ", offsetPeriod="
        + offsetPeriod + "\n runTimeAxis=" + runTimeAxis + "\n timeOffsetAxis=" + timeOffsetAxis;
  }
}
