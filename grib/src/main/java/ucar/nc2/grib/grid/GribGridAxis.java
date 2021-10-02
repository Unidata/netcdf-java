/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.base.Preconditions;
import ucar.nc2.Attribute;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateEns;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeIntv;
import ucar.nc2.grib.coord.CoordinateVert;
import ucar.nc2.grib.coord.EnsCoordValue;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.coord.VertCoordValue;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisInterval;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisSpacing;
import ucar.nc2.units.SimpleUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ucar.nc2.grib.grid.GribGridDataset.CoordAndAxis;

/** Grib implementation of {@link GridAxis} */
public class GribGridAxis {

  public static CoordAndAxis create(GribCollectionImmutable.Type type, Coordinate gribCoord, @Nullable GribIosp iosp) {
    switch (gribCoord.getType()) {
      case runtime: {
        CoordinateRuntime rtCoord = (CoordinateRuntime) gribCoord;
        GridAxis<?> axis = Point.builder().setRuntimeCoordinate(rtCoord).setAxisType(AxisType.RunTime).build();
        return new CoordAndAxis(gribCoord, axis);
      }

      case time: {
        CoordinateTime timeCoord = (CoordinateTime) gribCoord;
        GridAxis<?> axis = Point.builder().setTimeOffsetCoordinate(timeCoord).setAxisType(AxisType.TimeOffset).build();
        return new CoordAndAxis(gribCoord, axis);
      }

      case timeIntv: {
        CoordinateTimeIntv timeIntvCoord = (CoordinateTimeIntv) gribCoord;
        GridAxis<?> axis =
            Interval.builder().setTimeOffsetIntervalCoordinate(timeIntvCoord).setAxisType(AxisType.TimeOffset).build();
        return new CoordAndAxis(gribCoord, axis);
      }

      case time2D: {
        CoordinateTime2D time2d = (CoordinateTime2D) gribCoord;
        switch (type) {
          case SRC: // the time coordinate is a time2D (1 X ntimes) orthogonal
            Preconditions.checkArgument(time2d.isOrthogonal());
            Preconditions.checkArgument(time2d.getNruns() == 1);
            Preconditions.checkNotNull(time2d.getOrthogonalTimes());
            return create(type, time2d.getOrthogonalTimes(), iosp).withTime2d(time2d);

          case MRUTP:
          case MRUTC: // the time coordinate is a time2D (nruns X 1) orthogonal
            Preconditions.checkArgument(time2d.isOrthogonal());
            Preconditions.checkArgument(time2d.getNtimes() == 1);
            Preconditions.checkNotNull(time2d.getOrthogonalTimes());
            return create(type, time2d.getOffsetTimes(), iosp).withTime2d(time2d);

          case MRC:
          case TwoD: // the time coordinate is a time2D (nruns X ntimes)
            if (time2d.isOrthogonal()) {
              Preconditions.checkNotNull(time2d.getOrthogonalTimes());
              return create(type, time2d.getOrthogonalTimes(), iosp).withTime2d(time2d);

            } else if (time2d.isRegular()) {
              return create(type, time2d.getMaximalTimes(), iosp).withTime2d(time2d);

            } else {
              return create(type, time2d.getMaximalTimes(), iosp).withTime2d(time2d);
            }

          default:
            throw new UnsupportedOperationException("not implemented " + type + " " + time2d);
        }
      }

      case vert:
        CoordinateVert vertCoord = (CoordinateVert) gribCoord;
        AxisType axisType = getVertType(vertCoord.getUnit());
        String desc = iosp != null ? iosp.getVerticalCoordDesc(vertCoord.getCode()) : "";

        if (vertCoord.isLayer()) {
          GridAxisInterval.Builder<?> axis = Interval.builder().setVertCoordinate(vertCoord).setAxisType(axisType);
          addVerticalAttributes(axis, vertCoord, iosp);
          return new CoordAndAxis(gribCoord, axis.build());
        } else {
          GridAxisPoint.Builder<?> axis = Point.builder().setVertCoordinate(vertCoord).setAxisType(axisType)
              .setUnits(vertCoord.getUnit()).setDescription(desc);
          addVerticalAttributes(axis, vertCoord, iosp);
          return new CoordAndAxis(gribCoord, axis.build());
        }

      case ens: {
        CoordinateEns ensCoord = (CoordinateEns) gribCoord;
        GridAxis<?> axis = Point.builder().setEnsCoordinate(ensCoord).setAxisType(AxisType.Ensemble).build();
        return new CoordAndAxis(gribCoord, axis);
      }

      default:
        throw new UnsupportedOperationException();
    }
  }

  private static AxisType getVertType(String unit) {
    if (SimpleUnit.isCompatible("m", unit)) {
      return AxisType.Height;
    } else if (SimpleUnit.isCompatible("mbar", unit)) {
      return AxisType.Pressure;
    } else {
      return AxisType.GeoZ;
    }
  }

  private static void addVerticalAttributes(GridAxis.Builder<?> axis, CoordinateVert vc, @Nullable GribIosp iosp) {
    axis.setUnits(vc.getUnit());

    String desc = iosp != null ? iosp.getVerticalCoordDesc(vc.getCode()) : "";
    axis.setDescription(desc);

    axis.addAttribute(new Attribute(CF.POSITIVE, vc.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));
    axis.addAttribute(new Attribute("Grib_level_type", vc.getCode()));
    VertCoordType vu = vc.getVertUnit();
    if (vu != null) {
      if (vu.getDatum() != null) {
        axis.addAttribute(new Attribute("datum", vu.getDatum()));
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  @Immutable
  public static class Point extends GridAxisPoint {
    private final Coordinate gribCoord; // not immutable, so dont let this escape

    private Point(Builder<?> builder) {
      super(builder);
      this.gribCoord = builder.gribCoord;
    }

    public Point.Builder<?> toBuilder() {
      return addLocalFieldsToBuilder(builder());
    }

    // Add local fields to the builder.
    protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
      builder.setCoordinate(this.gribCoord);
      return (Builder<?>) super.addLocalFieldsToBuilder(builder);
    }

    public static Builder<?> builder() {
      return new Builder2();
    }

    private static class Builder2 extends Builder<Builder2> {
      @Override
      protected Builder2 self() {
        return this;
      }
    }

    public static abstract class Builder<T extends Builder<T>> extends GridAxisPoint.Builder<T> {
      Coordinate gribCoord;
      CalendarDateUnit cdu;
      private boolean built = false;

      public T setCoordinate(Coordinate gribCoord) {
        this.gribCoord = gribCoord;
        setName(gribCoord.getName());
        setUnits(gribCoord.getUnit());
        return self();
      }

      public T setEnsCoordinate(CoordinateEns ensCoord) {
        this.gribCoord = ensCoord;
        List<Number> values =
            ensCoord.getValues().stream().map(ens -> ((EnsCoordValue) ens).getEnsMember()).collect(Collectors.toList());
        RegularValues regular = calcPointIsRegular(values);
        if (regular != null) {
          setRegular(regular.ncoords, regular.start, regular.increment);
        } else {
          setSpacing(GridAxisSpacing.irregularPoint);
          setValues(values);
        }
        return setCoordinate(ensCoord);
      }

      public T setRuntimeCoordinate(CoordinateRuntime rtCoord) {
        this.gribCoord = rtCoord;
        this.cdu = rtCoord.getCalendarDateUnit();
        List<Number> values = new ArrayList<>(rtCoord.getRuntimeOffsetsInTimeUnits());
        RegularValues regular = calcPointIsRegular(values);
        if (regular != null) {
          setRegular(regular.ncoords, regular.start, regular.increment);
        } else {
          setSpacing(GridAxisSpacing.irregularPoint);
          setValues(values);
        }
        return setCoordinate(rtCoord);
      }

      public T setTimeOffsetCoordinate(CoordinateTime timeCoord) {
        this.gribCoord = timeCoord;
        List<Number> values = timeCoord.getValues().stream().map(v -> (Integer) v).collect(Collectors.toList());
        RegularValues regular = calcPointIsRegular(values);
        if (regular != null) {
          setRegular(regular.ncoords, regular.start, regular.increment);
        } else {
          setSpacing(GridAxisSpacing.irregularPoint);
          setValues(values);
        }
        return setCoordinate(timeCoord);
      }

      public T setVertCoordinate(CoordinateVert vertCoord) {
        this.gribCoord = vertCoord;
        List<Number> values =
            vertCoord.getValues().stream().map(vcv -> ((VertCoordValue) vcv).getValue1()).collect(Collectors.toList());
        RegularValues regular = calcPointIsRegular(values);
        if (regular != null) {
          setRegular(regular.ncoords, regular.start, regular.increment);
        } else {
          setSpacing(GridAxisSpacing.irregularPoint);
          setValues(values);
        }
        return setCoordinate(vertCoord);
      }

      public GribGridAxis.Point build() {
        if (built)
          throw new IllegalStateException("already built");
        built = true;
        if (this.resolution == 0 && this.values != null && this.values.length > 1) {
          this.resolution = (this.values[this.values.length - 1] - this.values[0]) / (this.values.length - 1);
        }
        return new GribGridAxis.Point(this);
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  @Immutable
  public static class Interval extends GridAxisInterval {
    private final Coordinate gribCoord; // not immutable so dont let this escape

    private Interval(Builder<?> builder) {
      super(builder);
      this.gribCoord = builder.gribCoord;
    }

    public Interval.Builder<?> toBuilder() {
      return addLocalFieldsToBuilder(builder());
    }

    // Add local fields to the builder.
    protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
      builder.setCoordinate(this.gribCoord);
      return (Builder<?>) super.addLocalFieldsToBuilder(builder);
    }

    public static Builder<?> builder() {
      return new Builder2();
    }

    private static class Builder2 extends Builder<Builder2> {
      @Override
      protected Builder2 self() {
        return this;
      }
    }

    public static abstract class Builder<T extends Builder<T>> extends GridAxisInterval.Builder<T> {
      Coordinate gribCoord;
      private boolean built = false;

      public T setCoordinate(Coordinate gribCoord) {
        this.gribCoord = gribCoord;
        setName(gribCoord.getName());
        setUnits(gribCoord.getUnit());
        return self();
      }

      public T setTimeOffsetIntervalCoordinate(CoordinateTimeIntv timeOffsetIntv) {
        List<Number> ivalues = new ArrayList<>();
        for (TimeCoordIntvValue intvValues : timeOffsetIntv.getTimeIntervals()) {
          ivalues.add(intvValues.getBounds1());
          ivalues.add(intvValues.getBounds2());
        }
        setNcoords(timeOffsetIntv.getNCoords());

        IntervalSpacing ispacing = calcIntervalSpacing(ivalues);
        if (ispacing.spacing == GridAxisSpacing.regularInterval) {
          setRegular(ispacing.ncoords, ispacing.start, ispacing.increment);
        } else {
          setSpacing(ispacing.spacing);
          setValues(ispacing.values);
        }
        return setCoordinate(timeOffsetIntv);
      }

      public T setVertCoordinate(CoordinateVert vertCoord) {
        List<Number> ivalues = new ArrayList<>();
        for (VertCoordValue intvValues : vertCoord.getLevelSorted()) {
          ivalues.add(intvValues.getValue1());
          ivalues.add(intvValues.getValue2());
        }
        setNcoords(vertCoord.getNCoords());

        IntervalSpacing ispacing = calcIntervalSpacing(ivalues);
        if (ispacing.spacing == GridAxisSpacing.regularInterval) {
          setRegular(ispacing.ncoords, ispacing.start, ispacing.increment);
        } else {
          setSpacing(ispacing.spacing);
          setValues(ispacing.values);
        }
        return setCoordinate(vertCoord);
      }

      public GribGridAxis.Interval build() {
        if (built)
          throw new IllegalStateException("already built");
        built = true;
        if (this.resolution == 0 && this.spacing == GridAxisSpacing.contiguousInterval && this.values.length > 1) {
          this.resolution = (this.values[this.values.length - 1] - this.values[0]) / (this.values.length - 1);
        }
        return new GribGridAxis.Interval(this);
      }
    }
  }

  ///////////////////////////////////////////////////////////////
  // re: CoordToGridAxis1D does this for NetcdfDataset

  private static final double incrTol = 5.0e-3; // LOOK why so large?

  private static class RegularValues {
    int ncoords;
    double start;
    double increment;

    RegularValues(int ncoords, double start, double increment) {
      this.ncoords = ncoords;
      this.start = start;
      this.increment = increment;
    }
  }

  @Nullable
  private static RegularValues calcPointIsRegular(List<Number> values) {
    int nvalues = values.size();
    double[] dvalues = new double[nvalues];
    for (int i = 0; i < nvalues; i++) {
      dvalues[i] = values.get(i).doubleValue();
    }

    if (nvalues == 1) {
      return new RegularValues(nvalues, dvalues[0], 0);
    } else if (nvalues == 2) {
      return new RegularValues(nvalues, dvalues[0], dvalues[1] - dvalues[0]);
    } else {
      double increment = dvalues[1] - dvalues[0];
      for (int i = 1; i < nvalues; i++) {
        if (!ucar.nc2.util.Misc.nearlyEquals(dvalues[i] - dvalues[i - 1], increment, incrTol)) {
          return null;
        }
      }
      return new RegularValues(nvalues, dvalues[0], increment);
    }
  }

  private static class IntervalSpacing {
    GridAxisSpacing spacing;
    int ncoords;
    double start;
    double increment;
    List<Number> values;

    IntervalSpacing(GridAxisSpacing spacing, int ncoords, double start, double increment, List<Number> values) {
      this.spacing = spacing;
      this.ncoords = ncoords;
      this.start = start;
      this.increment = increment;
      this.values = values;
    }
  }

  // 2*n values, low0, hi0, low1, hi1, ... ascending or descending
  private static IntervalSpacing calcIntervalSpacing(List<Number> values) {
    int ncoords = values.size() / 2;
    double[] value1 = new double[ncoords];
    double[] value2 = new double[ncoords];
    int count = 0;
    for (int i = 0; i < ncoords; i++) {
      value1[i] = values.get(count++).doubleValue();
      value2[i] = values.get(count++).doubleValue();
    }

    // is it regular ?
    if (ncoords == 1) {
      return new IntervalSpacing(GridAxisSpacing.regularInterval, ncoords, value1[0], value2[0] - value1[0], null);
    } else {
      boolean isRegular = true;
      double increment = value2[0] - value1[0];
      for (int i = 0; i < ncoords - 1; i++) {
        if (!ucar.nc2.util.Misc.nearlyEquals(value2[i] - value1[i], increment, incrTol)) {
          isRegular = false;
          break;
        }
        if (!ucar.nc2.util.Misc.nearlyEquals(value1[i + 1] - value1[i], increment, incrTol)) {
          isRegular = false;
          break;
        }
      }
      if (isRegular) {
        return new IntervalSpacing(GridAxisSpacing.regularInterval, ncoords, value1[0], increment, null);
      }
    }
    // is it contiguous?
    boolean isContiguous = true;
    boolean isAscending = value1[0] < value1[1];
    List<Number> contigValues = new ArrayList<>();
    if (isAscending) {
      contigValues.add(value1[0]);
      for (int i = 0; i < ncoords - 1; i++) {
        contigValues.add(value2[i]);
        if (!ucar.nc2.util.Misc.nearlyEquals(value1[i + 1], value2[i])) {
          isContiguous = false;
          break;
        }
      }
      contigValues.add(value2[ncoords - 1]);
    } else {
      contigValues.add(value2[0]);
      for (int i = 0; i < ncoords - 1; i++) {
        if (!ucar.nc2.util.Misc.nearlyEquals(value1[i], value2[i + 1])) {
          isContiguous = false;
          break;
        }
        contigValues.add(value1[i]);
      }
      contigValues.add(value1[ncoords - 1]);
    }

    if (isContiguous) {
      return new IntervalSpacing(GridAxisSpacing.contiguousInterval, ncoords, value1[0], 0, contigValues);
    } else {
      return new IntervalSpacing(GridAxisSpacing.discontiguousInterval, ncoords, value1[0], 0, values);
    }
  }
}
