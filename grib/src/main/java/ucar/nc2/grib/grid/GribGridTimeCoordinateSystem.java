/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;

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
    Preconditions.checkArgument(GridAxisPoint.class.isInstance(runtime.axis));
    Preconditions.checkNotNull(time);
    Preconditions.checkNotNull(time.time2d);

    switch (type) {
      case SRC: {
        // time2D (1 X ntimes) orthogonal:
        Preconditions.checkArgument(runtime.coord.getClass().isAssignableFrom(CoordinateRuntime.class));
        Preconditions.checkArgument(runtime.coord.getNCoords() == 1);
        Preconditions.checkArgument(time != null);
        return new SingleRuntime(runtime, time);
      }
      case MRUTC:
      case MRUTP: {
        // time2D (nruns X 1) orthogonal:
        Preconditions.checkNotNull(time);
        return new Observation(runtime, time);
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

  @Nullable
  @Override
  public GridAxisPoint getRunTimeAxis() {
    return runtimeAxis;
  }

  @Nullable
  @Override
  public GridAxis getTimeAxis() {
    return timeAxis;
  }

  @Nullable
  @Override
  public GridAxis getTimeOffsetAxis() {
    return timeOffsetAxis;
  }

  @Override
  public abstract List<Integer> getNominalShape();

  ///////////////////////////////////////
  private final Type type;
  private final GridAxisPoint runtimeAxis;
  private final GridAxis timeAxis;
  final GridAxis timeOffsetAxis;

  // LOOK time axis aleays null
  public GribGridTimeCoordinateSystem(Type type, GridAxisPoint runtimeAxis, GridAxis timeAxis,
      GridAxis timeOffsetAxis) {
    this.type = type;
    this.runtimeAxis = runtimeAxis;
    this.timeAxis = timeAxis;
    this.timeOffsetAxis = timeOffsetAxis;
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class Observation extends GribGridTimeCoordinateSystem
      implements GridTimeCoordinateSystem.Observation {

    private Observation(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis time) {
      super(Type.Observation, (GridAxisPoint) runtime.axis, null, time.axis);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(timeOffsetAxis.getNominalSize());
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class SingleRuntime extends GribGridTimeCoordinateSystem
      implements GridTimeCoordinateSystem.SingleRuntime {
    private final CalendarDate runtimeDate;

    private SingleRuntime(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis timeOffset) {
      super(Type.SingleRuntime, (GridAxisPoint) runtime.axis, null, timeOffset.axis);
      Preconditions.checkArgument(runtime.coord.getNCoords() == 1);
      CoordinateRuntime runtimeCoord = (CoordinateRuntime) runtime.coord;
      this.runtimeDate = runtimeCoord.getFirstDate();
    }

    @Override
    public CalendarDate getRunTime() {
      return runtimeDate;
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(timeOffsetAxis.getNominalSize());
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class Offset extends GribGridTimeCoordinateSystem implements GridTimeCoordinateSystem.Offset {
    private final CoordinateTime2D coord2D;
    private final GribCollectionImmutable.Type type;

    private Offset(GribCollectionImmutable.Type type, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, null, time.axis);
      this.coord2D = time.time2d;
      this.type = type;
    }

    @Override
    public GridAxis getTimeAxis(int runIdx) {
      // LOOK offset or time?
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class OffsetRegular extends GribGridTimeCoordinateSystem
      implements GridTimeCoordinateSystem.OffsetRegular {
    private final CoordinateTime2D coord2D;
    private final GribCollectionImmutable.Type type;

    private OffsetRegular(GribCollectionImmutable.Type type, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, null, time.axis);
      this.coord2D = time.time2d;
      this.type = type;
    }

    @Override
    public GridAxis getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public GridAxis getTimeAxis(int runIdx) {
      // LOOK offset or time?
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class Time2d extends GribGridTimeCoordinateSystem implements GridTimeCoordinateSystem.Time2d {
    private final CoordinateTime2D coord2D;
    private final GribCollectionImmutable.Type type;

    private Time2d(GribCollectionImmutable.Type type, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, null, time.axis);
      this.coord2D = time.time2d;
      this.type = type;
    }

    @Override
    public GridAxis getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public GridAxis getTimeAxis(int runIdx) {
      // LOOK offset or time?
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(type, timeCoord).axis;
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }
  }
}
