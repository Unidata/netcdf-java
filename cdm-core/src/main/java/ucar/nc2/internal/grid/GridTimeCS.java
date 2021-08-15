/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class GridTimeCS extends GridTimeCoordinateSystem {

  public static GridTimeCS createSingleOrOffset(GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis) {
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits()).orElseThrow();
    return create(runTimeAxis.getNominalSize() == 1 ? Type.SingleRuntime : Type.Offset, runTimeAxis, timeOffsetAxis,
        dateUnit, null, null);
  }

  public static GridTimeCS createObservation(GridAxis<?> timeAxis) {
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, timeAxis.getUnits()).orElseThrow();
    return create(Type.Observation, null, timeAxis, dateUnit, null, null);
  }

  public static GridTimeCS create(Type type, @Nullable GridAxisPoint runtimeAxis, // missing for Observation
      GridAxis<?> timeOffsetAxis, // time or timeOffset
      CalendarDateUnit calendarDateUnit, // offsets are reletive to this
      Map<Integer, GridAxis<?>> timeOffsetMap, // OffsetRegular only
      List<GridAxis<?>> timeOffsets // OffsetIrregular only
  ) {
    switch (type) {
      case Observation:
        return new Observation(timeOffsetAxis, calendarDateUnit);
      case SingleRuntime:
        return new SingleRuntime(runtimeAxis, timeOffsetAxis, calendarDateUnit);
      case Offset:
        return new Offset(runtimeAxis, timeOffsetAxis, calendarDateUnit);
      case OffsetRegular:
        return new OffsetRegular(runtimeAxis, timeOffsetAxis, calendarDateUnit, timeOffsetMap);
      case OffsetIrregular:
        return new OffsetIrregular(runtimeAxis, timeOffsetAxis, calendarDateUnit, timeOffsets);
    }
    throw new IllegalStateException("unkown type =" + type);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

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
    return timeOffsetAxis; // default
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

  @Override
  public Optional<? extends GridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    return Optional.empty();
  }

  //////////////////////////////////////////////////////////////////////////////////////

  protected GridTimeCS(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit calendarDateUnit) {
    super(type, runTimeAxis, timeOffsetAxis, calendarDateUnit);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  static class Observation extends GridTimeCS {

    Observation(GridAxis<?> time, CalendarDateUnit calendarDateUnit) {
      // LOOK MRMS_Radar_20201027_0000.grib2.ncx4 time2D has runtime in seconds, but period name is minutes
      super(Type.Observation, null, time, calendarDateUnit);
    }

    @Override
    public CalendarDate getBaseDate() {
      return calendarDateUnit.getBaseDateTime();
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(timeOffsetAxis.getNominalSize());
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new Observation(t, this.calendarDateUnit));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  static class SingleRuntime extends GridTimeCS {

    SingleRuntime(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit calendarDateUnit) {
      super(Type.SingleRuntime, runtime, timeOffset, calendarDateUnit);
      Preconditions.checkArgument(runtime.getNominalSize() == 1);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(1, timeOffsetAxis.getNominalSize());
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new SingleRuntime(runTimeAxis, t, this.calendarDateUnit));
    }

  }

  //////////////////////////////////////////////////////////////////////////////
  static class Offset extends GridTimeCS {

    Offset(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit calendarDateUnit) {
      super(Type.Offset, runtime, timeOffset, calendarDateUnit);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(runTimeAxis.getNominalSize(), timeOffsetAxis.getNominalSize());
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog).map(t -> new Offset(helper.runtimeAxis, t, calendarDateUnit));
    }

  }

  //////////////////////////////////////////////////////////////////////////////
  static class OffsetRegular extends GridTimeCS {
    private final Map<Integer, GridAxis<?>> timeOffsets;

    OffsetRegular(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit calendarDateUnit,
        Map<Integer, GridAxis<?>> timeOffsets) {
      super(Type.OffsetRegular, runtime, timeOffset, calendarDateUnit);
      this.timeOffsets = new TreeMap(timeOffsets);
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(runTimeAxis.getNominalSize(), timeOffsetAxis.getNominalSize());
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CalendarDate runtime = getRuntimeDate(runIdx);
      int hour = runtime.getHourOfDay();
      int min = runtime.getMinuteOfHour();
      return timeOffsets.get(hour * 60 + min);
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetRegular(helper.runtimeAxis, t, calendarDateUnit, timeOffsets));
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("%s%n", super.toString());
      for (Integer offset : timeOffsets.keySet()) {
        f.format("  %d == %s%n", offset, timeOffsets.get(offset));
      }
      return f.toString();
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  static class OffsetIrregular extends GridTimeCS {
    private final List<GridAxis<?>> timeOffsets;

    OffsetIrregular(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit calendarDateUnit,
        List<GridAxis<?>> timeOffsets) {
      super(Type.OffsetIrregular, runtime, timeOffset, calendarDateUnit);
      this.timeOffsets = timeOffsets;
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(runTimeAxis.getNominalSize(), timeOffsetAxis.getNominalSize());
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return timeOffsets.get(runIdx);
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetIrregular(helper.runtimeAxis, t, calendarDateUnit, timeOffsets));
    }

  }

}
