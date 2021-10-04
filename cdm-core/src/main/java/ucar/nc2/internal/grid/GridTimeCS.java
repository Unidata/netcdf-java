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
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class GridTimeCS extends GridTimeCoordinateSystem {

  public static GridTimeCS createSingleOrOffset(GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis) {
    CalendarDateUnit dateUnit =
        CalendarDateUnit.fromAttributes(runTimeAxis.attributes(), runTimeAxis.getUnits()).orElseGet(
            () -> CalendarDateUnit.fromAttributes(timeOffsetAxis.attributes(), timeOffsetAxis.getUnits()).orElse(null));
    return create(runTimeAxis.getNominalSize() == 1 ? Type.SingleRuntime : Type.Offset, runTimeAxis, timeOffsetAxis,
        dateUnit, null, null);
  }

  public static GridTimeCS createObservation(GridAxis<?> timeAxis) {
    CalendarDateUnit dateUnit =
        CalendarDateUnit.fromAttributes(timeAxis.attributes(), timeAxis.getUnits()).orElseThrow();
    return create(Type.Observation, null, timeAxis, dateUnit, null, null);
  }

  public static GridTimeCS create(Type type, @Nullable GridAxisPoint runtimeAxis, // missing for Observation
      GridAxis<?> timeOffsetAxis, // time or timeOffset
      CalendarDateUnit runtimeDateUnit, // offsets are reletive to this
      Map<Integer, GridAxis<?>> timeOffsetMap, // OffsetRegular only
      List<GridAxis<?>> timeOffsets // OffsetIrregular only
  ) {
    switch (type) {
      case Observation:
        return new Observation(timeOffsetAxis, runtimeDateUnit);
      case SingleRuntime:
        return new SingleRuntime(runtimeAxis, timeOffsetAxis, runtimeDateUnit);
      case Offset:
        return new Offset(runtimeAxis, timeOffsetAxis, runtimeDateUnit);
      case OffsetRegular:
        return new OffsetRegular(runtimeAxis, timeOffsetAxis, runtimeDateUnit, timeOffsetMap);
      case OffsetIrregular:
        return new OffsetIrregular(runtimeAxis, timeOffsetAxis, runtimeDateUnit, timeOffsets);
    }
    throw new IllegalStateException("unkown type =" + type);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Optional<? extends GridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    return Optional.empty();
  }

  //////////////////////////////////////////////////////////////////////////////////////

  protected GridTimeCS(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit runtimeDateUnit) {
    super(type, runTimeAxis, timeOffsetAxis, runtimeDateUnit);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  static class Observation extends GridTimeCS {

    Observation(GridAxis<?> time, CalendarDateUnit runtimeDateUnit) {
      super(Type.Observation, null, time, runtimeDateUnit);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(timeOffsetAxis.getNominalSize());
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new Observation(t, this.runtimeDateUnit));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  static class SingleRuntime extends GridTimeCS {

    SingleRuntime(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit runtimeDateUnit) {
      super(Type.SingleRuntime, runtime, timeOffset, runtimeDateUnit);
      Preconditions.checkArgument(runtime.getNominalSize() == 1);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(1, timeOffsetAxis.getNominalSize());
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new SingleRuntime(runTimeAxis, t, this.runtimeDateUnit));
    }

  }

  //////////////////////////////////////////////////////////////////////////////
  static class Offset extends GridTimeCS {

    Offset(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit runtimeDateUnit) {
      super(Type.Offset, runtime, timeOffset, runtimeDateUnit);
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog).map(t -> new Offset(helper.runtimeAxis, t, runtimeDateUnit));
    }

  }

  //////////////////////////////////////////////////////////////////////////////
  static class OffsetRegular extends GridTimeCS {
    private final Map<Integer, GridAxis<?>> timeOffsets;
    private final int maxTimeOffsets;

    OffsetRegular(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit runtimeDateUnit,
        Map<Integer, GridAxis<?>> timeOffsets) {
      super(Type.OffsetRegular, runtime, timeOffset, runtimeDateUnit);
      this.timeOffsets = new TreeMap<>(timeOffsets);
      this.maxTimeOffsets =
          timeOffsets.values().stream().map(GridAxis::getNominalSize).max(Comparator.naturalOrder()).orElse(0);
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CalendarDate runtime = getRuntimeDate(runIdx);
      int hour = runtime.getHourOfDay();
      int min = runtime.getMinuteOfHour();
      return timeOffsets.get(hour * 60 + min);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(runTimeAxis.getNominalSize(), this.maxTimeOffsets);
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetRegular(helper.runtimeAxis, t, runtimeDateUnit, timeOffsets));
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
    private final int maxTimeOffsets;

    OffsetIrregular(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDateUnit runtimeDateUnit,
        List<GridAxis<?>> timeOffsets) {
      super(Type.OffsetIrregular, runtime, timeOffset, runtimeDateUnit);
      this.timeOffsets = timeOffsets;
      this.maxTimeOffsets = timeOffsets.stream().map(GridAxis::getNominalSize).max(Comparator.naturalOrder()).orElse(0);
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return timeOffsets.get(runIdx);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(runTimeAxis.getNominalSize(), this.maxTimeOffsets);
    }

    @Override
    public Optional<GridTimeCS> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetIrregular(helper.runtimeAxis, t, runtimeDateUnit, timeOffsets));
    }

  }

}
