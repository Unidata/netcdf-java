/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.NamedObject;

import java.util.*;

/**
 * A 1-dimensional Coordinate Axis representing Calendar time.
 * Its coordinate values can be represented as Dates.
 * <p/>
 * May use udunit dates, or ISO Strings.
 */
public class GridAxis1DTime extends GridAxis1D {
  private static final Logger logger = LoggerFactory.getLogger(GridAxis1DTime.class);

  @Override
  public RangeIterator getRangeIterator() {
    return crange != null ? crange : range;
  }

  /**
   * Get the the ith CalendarDate.
   *
   * @param idx index
   * @return the ith CalendarDate
   */
  public CalendarDate getCalendarDate(int idx) {
    List<CalendarDate> cdates = getCalendarDates(); // in case we want to lazily evaluate
    return cdates.get(idx);
  }

  /**
   * Get calendar date range
   *
   * @return calendar date range
   */
  public CalendarDateRange getCalendarDateRange() {
    List<CalendarDate> cd = getCalendarDates();
    int last = cd.size();
    return (last > 0) ? CalendarDateRange.of(cd.get(0), cd.get(last - 1)) : null;
  }

  /**
   * Given a Date, find the corresponding time index on the time coordinate axis.
   * Can only call this is hasDate() is true.
   * This will return
   * <ul>
   * <li>i, if time(i) <= d < time(i+1).
   * <li>0, if d < time(0)
   * <li>n-1, if d > time(n-1), where n is length of time coordinates
   * </ul>
   *
   * @param d date to look for
   * @return corresponding time index on the time coordinate axis
   * @throws UnsupportedOperationException is no time axis or isDate() false
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

  /**
   * See if the given CalendarDate appears as a coordinate
   *
   * @param date test this
   * @return true if equals a coordinate
   */
  public boolean hasCalendarDate(CalendarDate date) {
    List<CalendarDate> cdates = getCalendarDates();
    for (CalendarDate cd : cdates) { // LOOK linear search, switch to binary
      if (date.equals(cd))
        return true;
    }
    return false;
  }

  /**
   * Get the list of datetimes in this coordinate as CalendarDate objects.
   *
   * @return list of CalendarDates.
   */
  public List<CalendarDate> getCalendarDates() {
    return cdates;
  }

  public CalendarDate[] getCoordBoundsDate(int i) {
    CalendarDate[] e = new CalendarDate[2];
    e[0] = timeHelper.makeCalendarDateFromOffset(getCoordEdge1(i));
    e[1] = timeHelper.makeCalendarDateFromOffset(getCoordEdge2(i));
    return e;
  }

  public CalendarDate getCoordBoundsMidpointDate(int i) {
    return timeHelper.makeCalendarDateFromOffset(getCoordMidpoint(i));
  }

  @Override
  Optional<GridAxis1D.Builder<?>> subsetBuilder(SubsetParams params, Formatter errLog) {
    if (params == null) {
      return Optional.of(this.toBuilder());
    }

    GridAxis1DHelper helper = new GridAxis1DHelper(this);

    switch (getAxisType()) {
      case Time:
        if (params.isTrue(SubsetParams.timePresent))
          return Optional.of(helper.subsetLatest());

        CalendarDate date = (CalendarDate) params.get(SubsetParams.time);
        if (date != null)
          return Optional.of(helper.subsetClosest(date));

        Integer stride = (Integer) params.get(SubsetParams.timeStride);
        if (stride == null || stride < 0)
          stride = 1;

        CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.timeRange);
        if (dateRange != null)
          return helper.subset(dateRange, stride, errLog);

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
            return Optional.of(helper.subsetClosest(date));
          } else {
            return Optional.of(helper.subsetClosest(timeOffset));
          }
        }

        // If a time interval is sent, search for match.
        double[] timeOffsetIntv = params.getTimeOffsetIntv();
        if (timeOffsetIntv != null && runtime != null) {
          // double midOffset = (timeOffsetIntv[0] + timeOffsetIntv[1]) / 2;
          CalendarDate[] dateIntv = new CalendarDate[2];
          dateIntv[0] = makeDateInTimeUnits(runtime, timeOffsetIntv[0]);
          dateIntv[1] = makeDateInTimeUnits(runtime, timeOffsetIntv[1]);
          return Optional.of(helper.subsetClosest(dateIntv));
        }

        if (stride != 1)
          try {
            return Optional.of(helper.subsetByIndex(getRange().copyWithStride(stride)));
          } catch (InvalidRangeException e) {
            errLog.format(e.getMessage());
            return Optional.empty();
          }

        // default is all
        break;

      case RunTime:
        CalendarDate rundate = (CalendarDate) params.get(SubsetParams.runtime);
        if (rundate != null)
          return Optional.of(helper.subsetClosest(rundate));

        /*
         * CalendarDateRange rundateRange = (CalendarDateRange) params.get(SubsetParams.runtimeRange);
         * if (rundateRange != null)
         * return helper.subset(rundateRange, 1);
         */

        if (params.isTrue(SubsetParams.runtimeAll))
          break;

        // default is latest
        return Optional.of(helper.subsetLatest());

      case TimeOffset:
        Double oval = params.getDouble(SubsetParams.timeOffset);
        if (oval != null) {
          return Optional.of(helper.subsetClosest(oval));
        }

        // If a time interval is sent, search for match.
        timeOffsetIntv = params.getTimeOffsetIntv();
        if (timeOffsetIntv != null) {
          return Optional.of(helper.subsetClosest((timeOffsetIntv[0] + timeOffsetIntv[1]) / 2));
        }


        if (params.isTrue(SubsetParams.timeOffsetFirst)) {
          try {
            return Optional.of(helper.subsetByIndex(new Range(1)));
          } catch (InvalidRangeException e) {
            errLog.format(e.getMessage());
            return Optional.empty();
          }
        }
        // default is all
        break;
    }

    // otherwise return copy the original axis
    return Optional.of(this.toBuilder());
  }

  /** @deprecated will be moved in ver6 */
  @Deprecated
  @Override
  public List<NamedObject> getCoordValueNames() {
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < cdates.size(); i++) {
      result.add(NamedObject.create(cdates.get(i), getAxisType().toString()));
    }
    return result;
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

  public double convert(CalendarDate date) {
    return timeHelper.offsetFromRefDate(date);
  }

  public CalendarDate makeDate(double value) {
    return timeHelper.makeDate(value);
  }

  public CalendarDateRange getDateRange() {
    return timeHelper.getDateRange(startValue, endValue);
  }

  public double getOffsetInTimeUnits(CalendarDate start, CalendarDate end) {
    return timeHelper.getOffsetInTimeUnits(start, end);
  }

  public CalendarDate makeDateInTimeUnits(CalendarDate start, double addTo) {
    return timeHelper.makeDateInTimeUnits(start, addTo);
  }

  public CalendarDate getRefDate() {
    return timeHelper.getRefDate();
  }

  public Calendar getCalendar() {
    return timeHelper.getCalendar();
  }

  public CalendarDateUnit getCalendarDateUnit() {
    return timeHelper.getCalendarDateUnit();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final TimeHelper timeHelper; // AxisType = Time, RunTime only
  private final List<CalendarDate> cdates;

  protected GridAxis1DTime(Builder<?> builder) {
    super(builder);
    if (builder.timeHelper != null) {
      this.timeHelper = builder.timeHelper;
    } else {
      this.timeHelper = TimeHelper.factory(this.units, this.attributes);
    }
    this.cdates = builder.cdates;
    Preconditions.checkArgument(cdates.size() == this.getNcoords());
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
