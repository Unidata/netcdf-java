/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisInterval;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.nc2.internal.grid2.SubsetTimeHelper;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Grib implementation of {@link GridTimeCoordinateSystem}
 * Is it possible that much of this is general?
 */
@Immutable
public abstract class GribGridTimeCoordinateSystem implements GridTimeCoordinateSystem {

  public static GribGridTimeCoordinateSystem create(GribCollectionImmutable.Type type,
      List<GribGridDataset.CoordAndAxis> coordAndAxes) {

    GribGridDataset.CoordAndAxis runtime =
        coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.runtime).findFirst().orElse(null);
    GribGridDataset.CoordAndAxis time =
        coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.time).findFirst().orElse(null);
    if (time == null) {
      time =
          coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.timeIntv).findFirst().orElse(null);
    }
    Preconditions.checkNotNull(runtime);
    Preconditions.checkArgument(runtime.axis instanceof GridAxisPoint);
    Preconditions.checkNotNull(time);
    Preconditions.checkNotNull(time.time2d);

    switch (type) {
      case SRC: {
        // time2D (1 X ntimes) orthogonal:
        Preconditions.checkArgument(runtime.coord.getClass().isAssignableFrom(CoordinateRuntime.class));
        Preconditions.checkArgument(runtime.coord.getNCoords() == 1);
        return new SingleRuntime(runtime, time);
      }
      case MRUTC:
      case MRUTP: {
        // time2D (nruns X 1) orthogonal:
        Preconditions.checkNotNull(time);
        return new Observation(time);
      }
      case TwoD: {
        if (time.time2d.isOrthogonal()) {
          return new Offset(type, runtime, time);
        } else if (time.time2d.isRegular()) {
          return new OffsetRegular(type, runtime, time);
        } else {
          return new Time2d(type, runtime, time);
        }
      }
      default:
        throw new IllegalStateException("Type=" + type);
    }
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public abstract List<Integer> getNominalShape();

  // LOOK this is where orthohonality is assumed. Problem is runtime/time. How to handle ??
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
  public List<CalendarDate> getTimesForRuntime(int runIdx) {
    GridAxis<?> timeOffset = getTimeOffsetAxis(runIdx);
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, timeOffset.getUnits()).orElseThrow();
    if (timeOffset instanceof GridAxisPoint) {
      GridAxisPoint offsetPoint = (GridAxisPoint) timeOffset;
      return Streams.stream(offsetPoint).map(val -> dateUnit.makeCalendarDate(val.longValue()))
          .collect(Collectors.toList());
    } else {
      GridAxisInterval offsetIntv = (GridAxisInterval) timeOffset;
      // LOOK cast double to long
      return Streams.stream(offsetIntv).map(intv -> dateUnit.makeCalendarDate((long) intv.midpoint()))
          .collect(Collectors.toList());
    }
  }

  @Override
  public CalendarDateUnit getCalendarDateUnit() {
    return runtimeUnit.orElse(null);
  }

  //////////////////////////////////////////////////////////////////////////////
  final GridTimeCoordinateSystem.Type type;
  final @Nullable GridAxisPoint runTimeAxis;
  final GridAxis<?> timeOffsetAxis;
  final Optional<CalendarDateUnit> runtimeUnit; // can it really be optional? seems unlikely

  GribGridTimeCoordinateSystem(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis) {
    this.type = type;
    this.runTimeAxis = runTimeAxis;
    this.timeOffsetAxis = timeOffsetAxis;

    if (runTimeAxis != null) {
      runtimeUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits());
    } else {
      runtimeUnit = Optional.empty();
    }
  }

  // LOOK so what is special about GribGridTimeCoordinateSystem ??
  // LOOK should this be MaterializedTimeCoordinateSystem ?
  // it would appear the only thing needed out of it (besides geo referencing) is getSubsetRanges()
  // LOOK if it has a runtime, does it have a dimension of length 1?
  // are you allowed to subset across runtimes?
  public abstract Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog);

  /*
   * {
   * // theres always a timeOffset
   * Optional<? extends GridAxis<?>> timeOffsetOpt = timeOffsetAxis.isInterval() ? timeOffsetAxis.subset(params, errlog)
   * : ((GridAxisPoint) timeOffsetAxis).subset(this, params, errlog);
   * 
   * if (timeOffsetOpt.isEmpty()) {
   * return Optional.empty();
   * }
   * GridAxis<?> timeOffset = timeOffsetOpt.get();
   * 
   * // Handle runtime specially, need to convert dates, and may be very large.
   * if (runTimeAxis != null) {
   * Optional<? extends GridAxisPoint> runtimeSubset = runTimeAxis.subset(this, params, errlog);
   * // LOOK retun subtype or MaterializedTimeCoordinateSystem?
   * return runtimeSubset.map(rt -> new MaterializedTimeCoordinateSystem(this.type, rt, timeOffset));
   * 
   * } else {
   * return Optional.of(new MaterializedTimeCoordinateSystem(this.type, null, timeOffset));
   * }
   * }
   */

  //////////////////////////////////////////////////////////////////////////////
  private static class Observation extends GribGridTimeCoordinateSystem {
    private final CalendarDateUnit dateUnit;

    private Observation(GribGridDataset.CoordAndAxis time) {
      // LOOK MRMS_Radar_20201027_0000.grib2.ncx4 time2D has runtime in seconds, but period name is minutes
      super(Type.Observation, null, time.axis);
      if (time.time2d != null) {
        CalendarDate refDate = time.time2d.getRefDate();
        CalendarPeriod period = time.time2d.getTimeUnit();
        this.dateUnit = CalendarDateUnit.of(period, true, refDate);
      } else {
        this.dateUnit = CalendarDateUnit.fromUdunitString(null, timeOffsetAxis.getUnits()).orElseThrow();
      }
    }

    private Observation(GridAxis<?> timeOffset, CalendarDateUnit dateUnit) {
      super(Type.Observation, null, timeOffset);
      this.dateUnit = dateUnit;
    }

    @Override
    public CalendarDate getBaseDate() {
      return dateUnit.getBaseDateTime();
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(timeOffsetAxis.getNominalSize());
    }

    @Override
    @Nullable
    public CalendarDate getRuntimeDate(int idx) {
      return getBaseDate();
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new Observation(t, this.dateUnit));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class SingleRuntime extends GribGridTimeCoordinateSystem {
    private final CalendarDate runtimeDate;

    private SingleRuntime(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis timeOffset) {
      super(Type.SingleRuntime, (GridAxisPoint) runtime.axis, timeOffset.axis);
      Preconditions.checkArgument(runtime.coord.getNCoords() == 1);
      CoordinateRuntime runtimeCoord = (CoordinateRuntime) runtime.coord;
      this.runtimeDate = runtimeCoord.getFirstDate();
    }

    private SingleRuntime(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDate runtimeDate) {
      super(Type.SingleRuntime, runtime, timeOffset);
      this.runtimeDate = runtimeDate;
    }

    @Override
    public CalendarDate getBaseDate() {
      return runtimeDate;
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(timeOffsetAxis.getNominalSize());
    }

    @Override
    public CalendarDate getRuntimeDate(int idx) {
      return getBaseDate();
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new SingleRuntime(runTimeAxis, t, this.runtimeDate));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class Offset extends GribGridTimeCoordinateSystem {
    private final CoordinateTime2D coord2D;
    private final GribCollectionImmutable.Type type;

    private Offset(GribCollectionImmutable.Type type, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, time.axis);
      this.coord2D = time.time2d;
      this.type = type;
    }

    private Offset(GribCollectionImmutable.Type type, CoordinateTime2D coord2D, GridAxisPoint runtime,
        GridAxis<?> timeOffset) {
      super(Type.SingleRuntime, runtime, timeOffset);
      this.coord2D = coord2D;
      this.type = type;
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }

    @Nullable
    @Override
    public CalendarDate getRuntimeDate(int runIdx) {
      return runtimeUnit.map(unit -> unit.makeCalendarDate(this.runTimeAxis.getCoordinate(runIdx).longValue()))
          .orElse(null);
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog).map(t -> new Offset(this.type, this.coord2D, helper.runtimeAxis, t));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class OffsetRegular extends GribGridTimeCoordinateSystem {
    private final CoordinateTime2D coord2D;
    private final GribCollectionImmutable.Type type;

    private OffsetRegular(GribCollectionImmutable.Type type, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.OffsetRegular, (GridAxisPoint) runtime.axis, time.axis);
      this.coord2D = time.time2d;
      this.type = type;
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }

    @Override
    public CalendarDate getRuntimeDate(int runIdx) {
      return runtimeUnit.map(unit -> unit.makeCalendarDate(this.runTimeAxis.getCoordinate(runIdx).longValue()))
          .orElse(null);
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog).map(t -> new Offset(this.type, this.coord2D, helper.runtimeAxis, t));
    }

  }

  //////////////////////////////////////////////////////////////////////////////
  private static class Time2d extends GribGridTimeCoordinateSystem {
    private final CoordinateTime2D coord2D;
    private final GribCollectionImmutable.Type type;

    private Time2d(GribCollectionImmutable.Type type, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, time.axis);
      this.coord2D = time.time2d;
      this.type = type;
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }

    @Override
    public CalendarDate getRuntimeDate(int runIdx) {
      return runtimeUnit.map(unit -> unit.makeCalendarDate(this.runTimeAxis.getCoordinate(runIdx).longValue()))
          .orElse(null);
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog).map(t -> new Offset(this.type, this.coord2D, helper.runtimeAxis, t));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class MaterializedTimeCoordinateSystem extends GribGridTimeCoordinateSystem {

    private MaterializedTimeCoordinateSystem(Type type, GridAxisPoint runtime, GridAxis<?> time) {
      super(type, runtime, time);
    }

    @Override
    public CalendarDate getBaseDate() {
      return null;
    }

    @Override
    public List<Integer> getNominalShape() {
      return null;
    }

    @Override
    public CalendarDate getRuntimeDate(int runIdx) {
      return null;
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return null;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      return Optional.empty();
    }
  }

}
