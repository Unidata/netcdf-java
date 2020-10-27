/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.Indent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A 1-dimensional GridAxis whose coordinates are Calendar times.
 * A GridAxis1DTime has a CalendarDateUnit that allows converting between doubles and CalendarDates.
 */
public class GridAxis1DTime extends GridAxis1D {
  private static final Logger logger = LoggerFactory.getLogger(GridAxis1DTime.class);

  /** Get the the ith coordinate CalendarDate. */
  public CalendarDate getCalendarDate(int idx) {
    List<CalendarDate> cdates = getCalendarDates(); // in case we want to lazily evaluate
    return cdates.get(idx);
  }

  /** Get the calendar date range */
  public CalendarDateRange getCalendarDateRange() {
    List<CalendarDate> cd = getCalendarDates();
    int last = cd.size();
    return (last > 0) ? CalendarDateRange.of(cd.get(0), cd.get(last - 1)) : null;
  }

  // LOOK public CalendarDateRange getDateRange() { return timeHelper.getDateRange(startValue, endValue); }


  /**
   * Given a Date, find the corresponding time index on the time coordinate axis.
   * This will return
   * <ul>
   * <li>i, if time(i) <= d < time(i+1).
   * <li>0, if d < time(0)
   * <li>n-1, if d > time(n-1), where n is length of time coordinates
   * </ul>
   *
   * @param d date to look for
   * @return corresponding time index on the time coordinate axis
   */
  public int findTimeIndexFromCalendarDate(CalendarDate d) {
    List<CalendarDate> cdates = getCalendarDates(); // LOOK linear search, switch to binary
    int index = 0;
    while (index < cdates.size()) {
      if (d.compareTo(cdates.get(index)) < 0)
        break;
      index++;
    }
    return Math.max(0, index - 1);
  }

  /** See if the given CalendarDate appears as a coordinate */
  public boolean hasCalendarDate(CalendarDate date) {
    List<CalendarDate> cdates = getCalendarDates();
    for (CalendarDate cd : cdates) { // LOOK linear search, switch to binary
      if (date.equals(cd))
        return true;
    }
    return false;
  }

  /** Get the list of coordinates as CalendarDates. */
  public List<CalendarDate> getCalendarDates() {
    return cdates;
  }

  /** Get the bounds of the ith coordinate as a CalendarDates. */
  public CalendarDate[] getCoordBoundsDate(int i) {
    CalendarDate[] e = new CalendarDate[2];
    e[0] = timeHelper.makeCalendarDateFromOffset(getCoordEdge1(i));
    e[1] = timeHelper.makeCalendarDateFromOffset(getCoordEdge2(i));
    return e;
  }

  /** Get the midpoint of the ith coordinate as a CalendarDate. */
  public CalendarDate getCoordBoundsMidpointDate(int i) {
    return timeHelper.makeCalendarDateFromOffset(getCoordMidpoint(i));
  }

  /** Use the CalendarDateUnit to make a CalendarDate from a value. */
  public CalendarDate makeDate(double value) {
    return timeHelper.makeDate(value);
  }

  /** Use the CalendarDateUnit to make a value from a CalendarDate. */
  public double makeValue(CalendarDate date) {
    return timeHelper.offsetFromRefDate(date);
  }

  /** Get the CalendarDateUnit */
  public CalendarDateUnit getCalendarDateUnit() {
    return timeHelper.getCalendarDateUnit();
  }

  /** Get Reference Date of the CalendarDateUnit */
  public CalendarDate getRefDate() {
    return timeHelper.getRefDate();
  }

  /** Use the CalendarDateUnit to find the difference between two CalendarDates. */
  public double getOffsetInTimeUnits(CalendarDate start, CalendarDate end) {
    return timeHelper.getOffsetInTimeUnits(start, end);
  }

  /** Use the CalendarDateUnit to add a value to the given CalendarDate. */
  public CalendarDate makeDateInTimeUnits(CalendarDate start, double addTo) {
    return timeHelper.makeDateInTimeUnits(start, addTo);
  }

  /** Get the Calendar used by this axis. */
  public Calendar getCalendar() {
    return timeHelper.getCalendar();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  /*
   * @Override
   * public Optional<GridAxis1D> subset(double minValue, double maxValue, int stride, Formatter errLog) {
   * GridAxis1DHelper helper = new GridAxis1DHelper(this);
   * Optional<GridAxis1D.Builder<?>> buildero = helper.subset(minValue, maxValue, stride, errLog);
   * return buildero.map(b -> new GridAxis1DTime((GridAxis1DTime.Builder<?>) b));
   * }
   */

  @Override
  @Nullable
  public GridAxis subset(GridSubset params, Formatter errLog) {
    if (params == null) {
      return this;
    }
    GridAxis1D.Builder<?> builder = subsetBuilder(params, errLog);
    return (builder == null) ? null : builder.build();
  }

  @Nullable
  private GridAxis1D.Builder<?> subsetBuilder(GridSubset params, Formatter errLog) {
    GridAxis1DHelper helper = new GridAxis1DHelper(this);
    switch (getAxisType()) {
      case Time:
        if (params.isTrue(GridSubset.timePresent)) {
          return helper.subsetLatest();
        }

        CalendarDate date = (CalendarDate) params.get(GridSubset.time);
        if (date != null) {
          return helper.subsetClosest(date);
        }

        // TODO, can time be discontinuous interval, if so need to add that case.
        Double value = (Double) params.get(GridSubset.timeCoord);
        if (value != null) {
          return helper.subsetClosest(value);
        }

        Integer stride = (Integer) params.get(GridSubset.timeStride);
        if (stride == null || stride < 0) {
          stride = 1;
        }

        CalendarDateRange dateRange = (CalendarDateRange) params.get(GridSubset.timeRange);
        if (dateRange != null) {
          return helper.subset(dateRange, stride, errLog).orElse(null);
        }

        // If no time range or time point, a timeOffset can be used to specify the time point.
        /*
         * CalendarDate timeOffsetDate = params.getTimeOffsetDate();
         * if (timeOffsetDate != null) {
         * return Optional.of(helper.subsetClosest(timeOffsetDate));
         * }
         */

        // A time offset or time offset interval starts from the rundate of the offset
        Double timeOffset = params.getTimeOffset();
        CalendarDate runtime = params.getRunTime();
        if (timeOffset != null) {
          if (runtime != null) {
            date = makeDateInTimeUnits(runtime, timeOffset);
            return helper.subsetClosest(date);
          } else {
            return helper.subsetClosest(timeOffset);
          }
        }

        // If a time interval is sent, search for match.
        double[] timeOffsetIntv = params.getTimeOffsetIntv();
        if (timeOffsetIntv != null && runtime != null) {
          // double midOffset = (timeOffsetIntv[0] + timeOffsetIntv[1]) / 2;
          CalendarDate[] dateIntv = new CalendarDate[2];
          dateIntv[0] = makeDateInTimeUnits(runtime, timeOffsetIntv[0]);
          dateIntv[1] = makeDateInTimeUnits(runtime, timeOffsetIntv[1]);
          return helper.subsetClosest(dateIntv);
        }

        if (stride != 1)
          try {
            return helper.makeSubsetByIndex(getRange().copyWithStride(stride));
          } catch (InvalidRangeException e) {
            errLog.format(e.getMessage());
            return null;
          }

        // default is all
        break;

      case RunTime:
        CalendarDate rundate = (CalendarDate) params.get(GridSubset.runtime);
        if (rundate != null) {
          return helper.subsetClosest(rundate);
        }

        /*
         * CalendarDateRange rundateRange = (CalendarDateRange) params.get(SubsetParams.runtimeRange);
         * if (rundateRange != null)
         * return helper.subset(rundateRange, 1);
         */

        if (params.isTrue(GridSubset.runtimeAll)) {
          break;
        }

        // default is latest
        return helper.subsetLatest();

      case TimeOffset:
        Double oval = params.getDouble(GridSubset.timeOffset);
        if (oval != null) {
          return helper.subsetClosest(oval);
        }

        // If a time interval is sent, search for match.
        timeOffsetIntv = params.getTimeOffsetIntv();
        if (timeOffsetIntv != null) {
          return helper.subsetClosest((timeOffsetIntv[0] + timeOffsetIntv[1]) / 2);
        }

        if (params.isTrue(GridSubset.timeOffsetFirst)) {
          return helper.makeSubsetByIndex(new Range(1));
        }
        // default is all
        break;
    }

    // otherwise return copy of the original axis
    return this.toBuilder();
  }

  @Override
  public String getSummary() {
    if (axisType != AxisType.RunTime) {
      return super.getSummary();
    }

    if (ncoords < 7) {
      Formatter f = new Formatter();
      for (int i = 0; i < ncoords; i++) {
        CalendarDate cd = makeDate(getCoordMidpoint(i));
        if (i > 0)
          f.format(", ");
        f.format("%s", cd);
      }
      return f.toString();
    }

    Formatter f = new Formatter();
    CalendarDate start = makeDate(getStartValue());
    f.format("start=%s", start);
    CalendarDate end = makeDate(getEndValue());
    f.format(", end=%s", end);
    f.format(" (npts=%d spacing=%s)", getNcoords(), getSpacing());

    return f.toString();
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s dates =%s", indent, cdates);
    f.format("%n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    GridAxis1DTime that = (GridAxis1DTime) o;
    return Objects.equal(timeHelper, that.timeHelper) && Objects.equal(cdates, that.cdates);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), timeHelper, cdates);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final TimeHelper timeHelper; // AxisType = Time, RunTime only
  private final ImmutableList<CalendarDate> cdates;

  protected GridAxis1DTime(Builder<?> builder) {
    super(builder);
    if (builder.timeHelper != null) {
      this.timeHelper = builder.timeHelper;
    } else {
      this.timeHelper = TimeHelper.factory(this.units, this.attributes);
    }
    if (range != null && builder.cdates != null) {
      this.cdates = subsetDatesByRange(builder.cdates, range);
    } else {
      this.cdates = ImmutableList.copyOf(builder.cdates);
    }
    Preconditions.checkArgument(cdates.size() == this.getNcoords());
  }

  private ImmutableList<CalendarDate> subsetDatesByRange(List<CalendarDate> dates, Range range) {
    ImmutableList.Builder<CalendarDate> builder = ImmutableList.builder();
    for (int index : range) {
      builder.add(dates.get(index));
    }
    return builder.build();
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setTimeHelper(this.timeHelper).setCalendarDates(this.cdates);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /** A builder taking fields from a VariableDS */
  public static GridAxis1DTime.Builder<?> builder(VariableDS vds) {
    return builder().initFromVariableDS(vds);
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

  public static abstract class Builder<T extends Builder<T>> extends GridAxis1D.Builder<T> {
    private boolean built;
    private TimeHelper timeHelper;
    private List<CalendarDate> cdates;

    protected abstract T self();

    public T setTimeHelper(TimeHelper timeHelper) {
      this.timeHelper = timeHelper;
      return self();
    }

    public T setCalendarDates(List<CalendarDate> cdates) {
      this.cdates = cdates;
      return self();
    }

    @Override
    T subset(int ncoords, double startValue, double endValue, double resolution, Range range) {
      super.subset(ncoords, startValue, endValue, resolution, range);
      return self();
    }

    public GridAxis1DTime build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      if (axisType == null) {
        axisType = AxisType.Time;
      }
      return new GridAxis1DTime(this);
    }
  }
}
