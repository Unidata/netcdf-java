/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.base.Preconditions;
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
public class GribGridTimeCoordinateSystem implements GridTimeCoordinateSystem {

  public static GribGridTimeCoordinateSystem create(GribCollectionImmutable.Type type,
      List<GribGridDataset.CoordAndAxis> coordAndAxes) {
    GribGridDataset.CoordAndAxis runtime =
        coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.runtime).findFirst().orElse(null);
    GribGridDataset.CoordAndAxis time2d =
        coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.time2D).findFirst().orElse(null);
    if (runtime != null && time2d != null) {
      CoordinateTime2D coord2D = (CoordinateTime2D) time2d.coord;
      Preconditions.checkArgument(coord2D.getRuntimeCoordinate().equals(runtime.coord));
    }

    GribGridDataset.CoordAndAxis time =
        coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.time).findFirst().orElse(null);
    if (time == null) {
      time =
          coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.timeIntv).findFirst().orElse(null);
    }
    Preconditions.checkNotNull(runtime);
    Preconditions.checkArgument(runtime.axis.getClass().isAssignableFrom(GridAxisPoint.class));

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
        Preconditions.checkNotNull(time2d);
        Preconditions.checkArgument(time2d.coord.getClass().isAssignableFrom(CoordinateTime2D.class));
        CoordinateTime2D coord2D = (CoordinateTime2D) time2d.coord;
        if (coord2D.isOrthogonal()) {
          return new Offset(runtime, time2d);
        } else if (coord2D.isRegular()) {
          return new OffsetRegular(runtime, time2d);
        } else {
          return new Time2d(runtime, time2d);
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

  ///////////////////////////////////////
  private final Type type;
  private final GridAxisPoint runtimeAxis;
  private final GridAxis timeAxis;
  private final GridAxis timeOffsetAxis;

  public GribGridTimeCoordinateSystem(Type type, GridAxisPoint runtimeAxis, GridAxis timeAxis,
      GridAxis timeOffsetAxis) {
    this.type = type;
    this.runtimeAxis = runtimeAxis;
    this.timeAxis = timeAxis;
    this.timeOffsetAxis = timeOffsetAxis;
  }

  ///////////////////////////////////////
  private static class Observation extends GribGridTimeCoordinateSystem
      implements GridTimeCoordinateSystem.Observation {
    private Observation(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis time) {
      super(Type.Observation, (GridAxisPoint) runtime.axis, time.axis, null);
    }
  }

  ///////////////////////////////////////
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
  }

  ///////////////////////////////////////
  private static class Offset extends GribGridTimeCoordinateSystem implements GridTimeCoordinateSystem.Offset {
    private final CoordinateTime2D coord2D;

    private Offset(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis time2d) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, null, time2d.axis);
      this.coord2D = (CoordinateTime2D) time2d.coord;
    }

    @Override
    public GridAxis getTimeAxis(int runIdx) {
      // LOOK offset or time?
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(timeCoord);
    }
  }


  ///////////////////////////////////////
  private static class OffsetRegular extends GribGridTimeCoordinateSystem
      implements GridTimeCoordinateSystem.OffsetRegular {
    private final CoordinateTime2D coord2D;

    private OffsetRegular(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis time2d) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, null, time2d.axis);
      this.coord2D = (CoordinateTime2D) time2d.coord;
    }

    @Override
    public GridAxis getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(timeCoord);
    }

    @Override
    public GridAxis getTimeAxis(int runIdx) {
      // LOOK offset or time?
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(timeCoord);
    }
  }

  ///////////////////////////////////////
  private static class Time2d extends GribGridTimeCoordinateSystem implements GridTimeCoordinateSystem.Time2d {
    private final CoordinateTime2D coord2D;

    private Time2d(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis time2d) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, null, time2d.axis);
      this.coord2D = (CoordinateTime2D) time2d.coord;
    }

    @Override
    public GridAxis getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(timeCoord);
    }

    @Override
    public GridAxis getTimeAxis(int runIdx) {
      // LOOK offset or time?
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(timeCoord);
    }
  }
}
