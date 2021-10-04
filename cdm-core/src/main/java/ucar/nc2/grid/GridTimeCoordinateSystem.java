/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the time coordinates of a GridCoordinateSystem.
 * The complexity is due to Forecast Model Run Collections (FMRC), in which the time coordinate
 * depends on the forecast run. This is handled in the subclasses.
 */
public abstract class GridTimeCoordinateSystem {
  /** Types of GridTimeCoordinateSystem. */
  public enum Type {
    Observation, // No runtimes
    SingleRuntime, // Single runtime
    Offset, // All runtimes have the same offsets (orthogonal)
    OffsetRegular, // All runtimes, grouped by time since 0z, have the same offsets.
    OffsetIrregular // Runtimes have irregular offsets
  }

  /** Get the ith runtime CalendarDate. If type=Observation, equals getBaseDate(). */
  public CalendarDate getRuntimeDate(int runIdx) {
    if (this.runTimeAxis == null) {
      return this.runtimeDateUnit.getBaseDateTime();
    } else {
      return runtimeDateUnit.makeCalendarDate(this.runTimeAxis.getCoordinate(runIdx).longValue());
    }
  }

  /**
   * Get the ith timeOffset axis. The offsets are reletive to getRuntimeDate(int runIdx), in units of getOffsetPeriod().
   * if type=Observation, SingleRuntime, runIdx is ignored, since the offsets are
   * always the same, by convention, pass in 0. LOOK does unit = getOffsetPeriod?
   */
  public GridAxis<?> getTimeOffsetAxis(int runIdx) {
    return timeOffsetAxis;
  }

  /** The units of the TimeOffsetAxis. */
  public CalendarPeriod getOffsetPeriod() {
    return offsetPeriod;
  }

  /** The runtime CalendarDateUnit. Note this may have different CalendarPeriod from the offset. */
  public CalendarDateUnit getRuntimeDateUnit() {
    return runtimeDateUnit;
  }

  /** The earliest runtime or observation date. */
  public CalendarDate getBaseDate() {
    return this.runtimeDateUnit.getBaseDateTime();
  }

  /** Get the Runtime axis. Null if type=Observation */
  @Nullable
  public GridAxisPoint getRunTimeAxis() {
    return runTimeAxis;
  }

  /**
   * Get the forecast/valid dates for a given run.
   * If type=Observation or SingleRuntime, runIdx is ignored.
   * For intervals this is the midpoint.
   */
  public List<CalendarDate> getTimesForRuntime(int runIdx) {
    List<CalendarDate> result = new ArrayList<>();
    CalendarDateUnit cdu = makeOffsetDateUnit(runIdx);
    GridAxis<?> timeAxis = getTimeOffsetAxis(runIdx);
    for (int offsetIdx = 0; offsetIdx < timeAxis.getNominalSize(); offsetIdx++) {
      double coord = timeAxis.getCoordDouble(offsetIdx);
      result.add(cdu.makeFractionalCalendarDate(coord));
    }
    return result;
  }

  /** The CalendarDateUnit for the offset time axis for runIdx. */
  public CalendarDateUnit makeOffsetDateUnit(int runIdx) {
    return CalendarDateUnit.of(getOffsetPeriod(), getOffsetPeriod().isDefaultCalendarField(), getRuntimeDate(runIdx));
  }

  /** Create a subsetted GridTimeCoordinateSystem. */
  public abstract Optional<? extends GridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog);

  ////////////////////////////////////////////////////////
  protected final Type type;
  protected final @Nullable GridAxisPoint runTimeAxis;
  protected final GridAxis<?> timeOffsetAxis;
  protected final CalendarDateUnit runtimeDateUnit;
  protected final CalendarPeriod offsetPeriod;

  protected GridTimeCoordinateSystem(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit calendarDateUnit) {
    this.type = type;
    this.runTimeAxis = runTimeAxis;
    this.timeOffsetAxis = timeOffsetAxis;

    if (calendarDateUnit != null) {
      this.runtimeDateUnit = calendarDateUnit;
    } else if (runTimeAxis != null) {
      this.runtimeDateUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits()).orElseThrow();
    } else {
      throw new IllegalArgumentException("calendarDateUnit or runTimeAxis must not be null");
    }
    Objects.requireNonNull(this.runtimeDateUnit);

    CalendarPeriod period = CalendarPeriod.of(timeOffsetAxis.getUnits());
    if (period == null) {
      period = this.runtimeDateUnit.getCalendarPeriod();
    }
    this.offsetPeriod = Objects.requireNonNull(period);
  }

  // public by necessity, but would be nice not to be in the public API ??

  public Type getType() {
    return type;
  }

  // LOOK, not right
  public List<Integer> getNominalShape() {
    return getMaterializedShape();
  }

  // Used for MaterializedCoordinateSystem
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

  public List<ucar.array.Range> getSubsetRanges() {
    List<ucar.array.Range> result = new ArrayList<>();
    if (getRunTimeAxis() != null) {
      result.add(getRunTimeAxis().getSubsetRange());
    }
    result.add(getTimeOffsetAxis(0).getSubsetRange());
    return result;
  }

  @Override
  public String toString() {
    return "GridTimeCoordinateSystem type=" + type + ", runtimeDateUnit=" + runtimeDateUnit + ", offsetPeriod="
        + offsetPeriod + "\n runTimeAxis=" + runTimeAxis + "\n timeOffsetAxis=" + timeOffsetAxis;
  }
}
