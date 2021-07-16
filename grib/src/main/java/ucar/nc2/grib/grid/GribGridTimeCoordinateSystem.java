/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import ucar.array.Range;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisDependenceType;
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

import static ucar.nc2.grid2.GridTimeCoordinateSystem.Type.Observation;

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
        CalendarDate refDate = time.time2d.getRefDate();
        CalendarPeriod period = time.time2d.getTimeUnit();
        CalendarDateUnit dateUnit = CalendarDateUnit.of(period, true, refDate);
        return new Observation(time, dateUnit);
      }
      case TwoD: {
        if (time.time2d.isOrthogonal()) {
          return new Offset(type, runtime, time);
        } else if (time.time2d.isRegular()) {
          return new OffsetRegular(type, runtime, time);
        } else {
          return new OffsetIrregular(type, runtime, time);
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
  public List<CalendarDate> getTimesForRuntime(int runIdx) {
    if (this.type == Observation) {
      return getTimesForObservation();
    } else {
      return getTimesForNonObservation(runIdx);
    }
  }

  private List<CalendarDate> getTimesForObservation() {
    if (timeOffsetAxis instanceof GridAxisPoint) {
      GridAxisPoint offsetPoint = (GridAxisPoint) timeOffsetAxis;
      return Streams.stream(offsetPoint).map(val -> calendarDateUnit.makeCalendarDate(val.longValue()))
          .collect(Collectors.toList());
    } else {
      GridAxisInterval offsetIntv = (GridAxisInterval) timeOffsetAxis;
      // LOOK cast double to long
      return Streams.stream(offsetIntv).map(intv -> calendarDateUnit.makeCalendarDate((long) intv.midpoint()))
          .collect(Collectors.toList());
    }
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
  public CalendarDateUnit getCalendarDateUnit() {
    return calendarDateUnit;
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

  //////////////////////////////////////////////////////////////////////////////
  final GridTimeCoordinateSystem.Type type;
  final @Nullable GridAxisPoint runTimeAxis;
  final GridAxis<?> timeOffsetAxis;
  final CalendarDateUnit calendarDateUnit;
  final CalendarPeriod offsetPeriod;

  GribGridTimeCoordinateSystem(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit calendarDateUnit) {
    this.type = type;
    this.runTimeAxis = runTimeAxis;
    this.timeOffsetAxis = timeOffsetAxis;
    this.offsetPeriod = CalendarPeriod.of(timeOffsetAxis.getUnits());

    if (runTimeAxis != null) {
      this.calendarDateUnit = CalendarDateUnit.fromUdunitString(null, runTimeAxis.getUnits()).orElseThrow();
    } else {
      this.calendarDateUnit = calendarDateUnit;
    }
  }

  // LOOK so what is special about GribGridTimeCoordinateSystem ??
  // LOOK should this be MaterializedTimeCoordinateSystem ?
  // it would appear the only thing needed out of it (besides geo referencing) is getSubsetRanges()
  // LOOK if it has a runtime, does it have a dimension of length 1?
  // are you allowed to subset across runtimes?
  public abstract Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog);

  //////////////////////////////////////////////////////////////////////////////
  private static class Observation extends GribGridTimeCoordinateSystem {

    private Observation(GribGridDataset.CoordAndAxis time, CalendarDateUnit calendarDateUnit) {
      // LOOK MRMS_Radar_20201027_0000.grib2.ncx4 time2D has runtime in seconds, but period name is minutes
      super(Type.Observation, null, time.axis, calendarDateUnit);
    }

    private Observation(GridAxis<?> timeOffset, CalendarDateUnit dateUnit) {
      super(Type.Observation, null, timeOffset, dateUnit);
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
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new Observation(t, this.calendarDateUnit));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class SingleRuntime extends GribGridTimeCoordinateSystem {
    private final CalendarDate runtimeDate;

    private SingleRuntime(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis timeOffset) {
      super(Type.SingleRuntime, (GridAxisPoint) runtime.axis, timeOffset.axis, null);
      Preconditions.checkArgument(runtime.coord.getNCoords() == 1);
      CoordinateRuntime runtimeCoord = (CoordinateRuntime) runtime.coord;
      this.runtimeDate = runtimeCoord.getFirstDate();
    }

    private SingleRuntime(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDate runtimeDate) {
      super(Type.SingleRuntime, runtime, timeOffset, null);
      this.runtimeDate = runtimeDate;
    }

    @Override
    public CalendarDate getBaseDate() {
      return runtimeDate;
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(1, timeOffsetAxis.getNominalSize());
    }

    @Override
    public CalendarDate getRuntimeDate(int idx) {
      return runtimeDate;
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
      super(Type.Offset, (GridAxisPoint) runtime.axis, time.axis, null);
      this.coord2D = time.time2d;
      this.type = type;
    }

    private Offset(GribCollectionImmutable.Type type, CoordinateTime2D coord2D, GridAxisPoint runtime,
        GridAxis<?> timeOffset) {
      super(Type.SingleRuntime, runtime, timeOffset, null);
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
      super(Type.OffsetRegular, (GridAxisPoint) runtime.axis, time.axis, null);
      this.coord2D = time.time2d;
      this.type = type;
    }

    private OffsetRegular(GribCollectionImmutable.Type type, CoordinateTime2D coord2D, GridAxisPoint runtime,
        GridAxis<?> timeOffset) {
      super(Type.OffsetRegular, runtime, timeOffset, null);
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

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetRegular(this.type, this.coord2D, helper.runtimeAxis, t));
    }

  }

  //////////////////////////////////////////////////////////////////////////////
  private static class OffsetIrregular extends GribGridTimeCoordinateSystem {
    private final CoordinateTime2D coord2D;
    private final GribCollectionImmutable.Type type;

    private OffsetIrregular(GribCollectionImmutable.Type type, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.OffsetIrregular, (GridAxisPoint) runtime.axis, time.axis, null);
      this.coord2D = time.time2d;
      this.type = type;
    }

    private OffsetIrregular(GribCollectionImmutable.Type type, CoordinateTime2D coord2D, GridAxisPoint runtime,
        GridAxis<?> timeOffset) {
      super(Type.OffsetRegular, runtime, timeOffset, null);
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

    @Override
    public List<ucar.array.Range> getSubsetRanges() {
      return ImmutableList.of(new Range(coord2D.getNruns()), new Range(coord2D.getNtimes()));
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetIrregular(this.type, this.coord2D, helper.runtimeAxis, t));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class MaterializedTimeCoordinateSystem extends GribGridTimeCoordinateSystem {

    private MaterializedTimeCoordinateSystem(Type type, GridAxisPoint runtime, GridAxis<?> time) {
      super(type, runtime, time, null);
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
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      return null;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      return Optional.empty();
    }
  }

}
