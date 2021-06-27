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
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisInterval;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

  @Nullable
  @Override
  public GridAxisPoint getRunTimeAxis() {
    return runtimeAxis;
  }

  /*
   * @Override
   * public CalendarDate getBaseDate() {
   * return getRuntimeDate(0);
   * }
   * 
   * 
   * @Nullable
   * 
   * @Override
   * public CalendarDate getRuntimeDate(int idx) {
   * return runtimeUnit.map(unit ->
   * unit.makeCalendarDate(this.runtimeAxis.getCoordinate(idx).longValue())).orElse(null);
   * }
   * 
   * @Override
   * public GridAxis getTimeOffsetAxis(int runIdx) {
   * return timeOffsetAxis;
   * }
   * 
   */

  @Override
  public List<CalendarDate> getTimesForRuntime(int runIdx) {
    GridAxis timeOffset = getTimeOffsetAxis(runIdx);
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

  ///////////////////////////////////////
  final GridTimeCoordinateSystem.Type type;
  final @Nullable GridAxisPoint runtimeAxis;
  final GridAxis timeOffsetAxis;
  final Optional<CalendarDateUnit> runtimeUnit;

  public GribGridTimeCoordinateSystem(Type type, @Nullable GridAxisPoint runtimeAxis, GridAxis timeOffsetAxis) {
    this.type = type;
    this.runtimeAxis = runtimeAxis;
    this.timeOffsetAxis = timeOffsetAxis;

    if (runtimeAxis != null) {
      runtimeUnit = CalendarDateUnit.fromUdunitString(null, runtimeAxis.getUnits());
    } else {
      runtimeUnit = Optional.empty();
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class Observation extends GribGridTimeCoordinateSystem {
    private final CalendarDateUnit dateUnit;

    private Observation(GribGridDataset.CoordAndAxis time) {
      super(Type.Observation, null, time.axis);
      this.dateUnit = CalendarDateUnit.fromUdunitString(null, timeOffsetAxis.getUnits()).orElseThrow();
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
    public GridAxis getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
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
    public GridAxis getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
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
      return runtimeUnit.map(unit -> unit.makeCalendarDate(this.runtimeAxis.getCoordinate(runIdx).longValue()))
          .orElse(null);
    }

    @Override
    public GridAxis getTimeOffsetAxis(int runIdx) {
      return timeOffsetAxis;
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
      return runtimeUnit.map(unit -> unit.makeCalendarDate(this.runtimeAxis.getCoordinate(runIdx).longValue()))
          .orElse(null);
    }

    @Override
    public GridAxis getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
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
      return runtimeUnit.map(unit -> unit.makeCalendarDate(this.runtimeAxis.getCoordinate(runIdx).longValue()))
          .orElse(null);
    }

    @Override
    public GridAxis getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

  }
}
