/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.grid.GridAxis1DHelper;
import ucar.nc2.internal.grid.TimeHelper;
import ucar.nc2.time.*;
import ucar.nc2.time.Calendar;
import ucar.nc2.util.Indent;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * A 1-dimensional GridAxis whose coordinates can be converted to CalendarDates.
 * A GridAxis1DTime has a CalendarDateUnit that allows converting between doubles and CalendarDates.
 */
@Immutable
public class GridAxis1DTime extends GridAxis1D {
  private static final Logger logger = LoggerFactory.getLogger(GridAxis1DTime.class);

  /** Get the list of coordinates as CalendarDates. */
  public ImmutableList<CalendarDate> getCalendarDates() {
    return cdates;
  }

  /** Get the the ith coordinate CalendarDate. */
  public CalendarDate getCalendarDate(int idx) {
    return cdates.get(idx);
  }

  /** Get the calendar date range */
  public CalendarDateRange getCalendarDateRange() {
    int last = cdates.size();
    return (last > 0) ? CalendarDateRange.of(cdates.get(0), cdates.get(last - 1)) : null;
  }

  /** Use the CalendarDateUnit to make a value from a CalendarDate. */
  public double makeValue(CalendarDate date) {
    return timeHelper.offsetFromRefDate(date);
  }

  TimeHelper getTimeHelper() {
    return timeHelper;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // unused: candidates for removal
  // I think this functionality was needed when we were using time coords instead of time offsets.

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

  /** Get the bounds of the ith coordinate as a CalendarDate[2]. */
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


  /** Get the time from the runtime and offset */
  public static CalendarDate addOffset(CalendarDate runDate, double offset, String offsetUnits) {
    CalendarPeriod.Field field = CalendarPeriod.fromUnitString(offsetUnits);
    // the problem is if offset is not integral
    int offseti = (int) offset;
    if (offseti != offset) {
      throw new RuntimeException(String.format("Not integral offset = %f", offset));
    }
    return runDate.add(CalendarPeriod.of(offseti, field));
  }

  /** Get the CalendarDateUnit */
  public CalendarDateUnit getCalendarDateUnit() {
    return timeHelper.getCalendarDateUnit();
  }

  /** Get Reference Date of the CalendarDateUnit (not the runtime) */
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

  /** Use the CalendarDateUnit to make a CalendarDate from a value. */
  public CalendarDate makeDate(double value) {
    return timeHelper.makeDate(value);
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
      case Time: {
        if (params.getTimePresent()) {
          return helper.subsetLatest();
        }

        CalendarDate date = params.getTime();
        if (date != null) {
          return helper.subsetClosest(date);
        }

        // TODO, can time be discontinuous interval? if so need to add that case.
        Object value = params.getTimeCoord();
        if (value instanceof Double) {
          return helper.subsetClosest((Double) value);
        }

        Integer stride = params.getTimeStride();
        if (stride == null || stride < 0) {
          stride = 1;
        }

        CalendarDateRange dateRange = params.getTimeRange();
        if (dateRange != null) {
          return helper.subset(dateRange, stride, errLog).orElse(null);
        }

        // A time offset or time offset interval starts from the rundate of the offset
        Object timeOffset = params.getTimeOffsetCoord();
        CalendarDate runtime = params.getRunTime();
        if (timeOffset != null) {
          if (timeOffset instanceof Double) {
            if (runtime != null) {
              date = makeDateInTimeUnits(runtime, (Double) timeOffset);
              return helper.subsetClosest(date);
            } else {
              return helper.subsetClosest((Double) timeOffset);
            }
          } else if (timeOffset instanceof CoordInterval) {
            CoordInterval timeOffsetIntv = (CoordInterval) timeOffset;
            CalendarDate[] dateIntv = new CalendarDate[2];
            dateIntv[0] = makeDateInTimeUnits(runtime, timeOffsetIntv.start());
            dateIntv[1] = makeDateInTimeUnits(runtime, timeOffsetIntv.end());
            return helper.subsetClosest(dateIntv);
          }
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
      }

      case RunTime: {
        CalendarDate rundate = params.getRunTime();
        if (rundate != null) {
          return helper.subsetClosest(rundate);
        }

        if (params.getRunTimeAll()) {
          break;
        }

        // default is latest
        return helper.subsetLatest();
      }
    }

    // otherwise return copy of the original axis
    return this.toBuilder();
  }

  @Override
  public String getSummary() {
    // TODO why?
    if (axisType != AxisType.RunTime) {
      return super.getSummary();
    }

    if (ncoords < 7) {
      Formatter f = new Formatter();
      for (int i = 0; i < ncoords; i++) {
        CalendarDate cd = timeHelper.makeDate(getCoordMidpoint(i));
        if (i > 0)
          f.format(", ");
        f.format("%s", cd);
      }
      return f.toString();
    }

    Formatter f = new Formatter();
    CalendarDate start = timeHelper.makeDate(getStartValue());
    f.format("start=%s", start);
    CalendarDate end = timeHelper.makeDate(getEndValue());
    f.format(", end=%s", end);
    f.format(" (npts=%d spacing=%s)", getNcoords(), getSpacing());

    return f.toString();
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s dateUnit '%s' dates =%s%n", indent, timeHelper.getUdUnit(), cdates);
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
    return Objects.equals(timeHelper.getUdUnit(), that.timeHelper.getUdUnit());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), timeHelper, cdates);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final TimeHelper timeHelper;
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
      Preconditions.checkArgument(cdates.size() == this.getNcoords());
    } else if (builder.cdates != null) {
      this.cdates = ImmutableList.copyOf(builder.cdates);
      Preconditions.checkArgument(cdates.size() == this.getNcoords());
    } else {
      this.cdates = makeCalendarDateFromValues();
    }
  }

  private ImmutableList<CalendarDate> makeCalendarDateFromValues() {
    ArrayList<CalendarDate> result = new ArrayList<>(getNcoords());
    for (double val : getCoordsAsArray()) {
      result.add(timeHelper.makeCalendarDateFromOffset(val));
    }
    return ImmutableList.copyOf(result);
  }

  private ImmutableList<CalendarDate> subsetDatesByRange(List<CalendarDate> dates, RangeIterator range) {
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

    public T setDateUnits(String dateUnits) {
      this.timeHelper = TimeHelper.factory(dateUnits, null);
      return self();
    }

    public T setTimeHelper(TimeHelper timeHelper) {
      this.timeHelper = timeHelper;
      return self();
    }

    // You should only set this if you want to calculate dates from midpoints.
    // Otherwise just set the midpoints
    public T setCalendarDates(List<CalendarDate> cdates) {
      this.cdates = cdates;
      return self();
    }

    @Override
    public T subset(int ncoords, double startValue, double endValue, double resolution, Range range) {
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
      if (cdates != null && timeHelper != null) {
        setValues(makeValuesFromCalendarDate());
      }
      return new GridAxis1DTime(this);
    }

    private double[] makeValuesFromCalendarDate() {
      double[] values = new double[cdates.size()];
      int count = 0;
      for (CalendarDate cd : cdates) {
        values[count++] = timeHelper.offsetFromRefDate(cd);
      }
      return values;
    }
  }
}
