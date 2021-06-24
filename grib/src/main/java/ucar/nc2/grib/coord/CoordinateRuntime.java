/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.internal.util.Counters;
import ucar.nc2.util.Indent;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Grib runtime coordinate
 * Effectively Immutable
 * 
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateRuntime implements Coordinate {
  private final long[] runtimes; // msecs since epoch, using ISO8601 UTC.
  private final CalendarDate firstDate;
  final CalendarPeriod timePeriod;

  private final CalendarDateUnit calendarDateUnit;
  final String periodName;
  private String name = "reftime"; // yeah yeah, not final, bugger off

  /**
   * LOOK doesnt work for month, year.
   * 
   * @param runtimeSorted millis from epoch. LOOK probably should be offsets in timeUnit
   * @param timeUnit default is Hours. trying to preserve original semantics
   */
  public CoordinateRuntime(List<Long> runtimeSorted, @Nullable CalendarPeriod timeUnit) {
    this.runtimes = new long[runtimeSorted.size()];
    int idx = 0;
    for (long val : runtimeSorted) {
      this.runtimes[idx++] = val;
    }
    this.firstDate = CalendarDate.of(runtimeSorted.get(0));
    if (timeUnit == null) {
      timeUnit = CalendarPeriod.Hour;
    }

    // check to see that the runtimes are compatible with the timeUnit. See Issue #725
    boolean unitsOk = true;
    for (long val : runtimeSorted) {
      CalendarDate anotherDate = CalendarDate.of(val);
      long offset = anotherDate.since(this.firstDate, timeUnit);
      CalendarDate check = this.firstDate.add(offset, timeUnit);
      if (!check.equals(this.firstDate)) {
        unitsOk = false;
        break;
      }
    }
    if (!unitsOk) {
      timeUnit = CalendarPeriod.Second;
    }

    this.timePeriod = timeUnit;
    CalendarPeriod.Field cf = this.timePeriod.getField();

    // LOOK
    boolean isCalendarField = (cf == CalendarPeriod.Field.Month || cf == CalendarPeriod.Field.Year);
    this.calendarDateUnit = CalendarDateUnit.of(this.timePeriod, isCalendarField, this.firstDate);
    if (isCalendarField) {
      this.periodName = "calendar " + cf; // LOOK maybe Period should know about isCalendarField ??
    } else {
      this.periodName = cf.toString();
    }
  }

  public CalendarPeriod getTimeUnits() {
    return timePeriod;
  }

  public CalendarDate getRuntimeDate(int idx) {
    return CalendarDate.of(runtimes[idx]);
  }

  public long getRuntime(int idx) {
    return runtimes[idx];
  }

  public String getPeriodName() {
    return periodName;
  }

  public CalendarDateUnit getCalendarDateUnit() {
    return calendarDateUnit;
  }

  /**
   * Get offsets from firstDate, in units of timeUnit
   * 
   * @return for each runtime, a list of values from firstdate
   *         LOOK Double
   */
  public List<Double> getOffsetsInTimeUnits() {
    double start = firstDate.getMillisFromEpoch();

    List<Double> result = new ArrayList<>(runtimes.length);
    for (int idx = 0; idx < runtimes.length; idx++) {
      double runtime = (double) getRuntime(idx);
      double msecs = (runtime - start);
      // LOOK doesnt work for month, year.
      // since runtimes is in millis, have no choice but to use fixed time period intervals.
      result.add(msecs / GribUtils.getValueInMillisecs(timePeriod));
    }
    return result;
  }

  public List<Long> getRuntimeOffsetsInTimeUnits() {
    List<Long> result = new ArrayList<>(runtimes.length);
    for (int idx = 0; idx < runtimes.length; idx++) {
      CalendarDate runtime = getRuntimeDate(idx);
      result.add(runtime.since(firstDate, timePeriod));
    }
    return result;
  }

  // Get the offset of the runtime dates from the given one
  public long getOffsetFrom(CalendarDate start) {
    return firstDate.since(start, timePeriod);
    // return timeUnit.getOffset(start, getFirstDate());
  }

  @Override
  public int getSize() {
    return runtimes.length;
  }

  @Override
  public int getNCoords() {
    return getSize();
  }

  @Override
  public Type getType() {
    return Type.runtime;
  }

  @Override
  public int estMemorySize() {
    return 616 + getSize() * (48);
  }

  @Override
  public String getUnit() {
    return periodName + " since " + firstDate;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) { // LOOK need to get this in the constructor to make the name final
    if (!this.name.equals("reftime"))
      throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  @Override
  public int getCode() {
    return 0;
  }

  public CalendarDate getFirstDate() {
    return firstDate;
  }

  public CalendarDate getLastDate() {
    return getRuntimeDate(getSize() - 1);
  }

  @Override
  public List<?> getValues() {
    List<Long> result = new ArrayList<>(runtimes.length);
    for (long val : runtimes)
      result.add(val);
    return result;
  }

  @Override
  public int getIndex(Object val) {
    long want;

    if (val instanceof CalendarDate)
      want = ((CalendarDate) val).getMillisFromEpoch();
    else if (val instanceof Number)
      want = ((Number) val).longValue();
    else
      throw new IllegalArgumentException(val.getClass().getName());

    return Arrays.binarySearch(runtimes, want);
  }

  @Override
  public Object getValue(int idx) {
    return runtimes[idx];
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
    for (int idx = 0; idx < getSize(); idx++)
      info.format(" %s,", getRuntimeDate(idx));
    info.format(" (%d) %n", runtimes.length);
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Run Times: %s (%s)%n", getName(), getUnit());
    List<Double> udunits = getOffsetsInTimeUnits();
    int count = 0;
    for (int idx = 0; idx < getSize(); idx++)
      info.format("   %s (%f)%n", getRuntimeDate(idx), udunits.get(count++));
  }

  @Override
  public Counters calcDistributions() {
    Counters counters = new Counters();
    counters.add("resol");

    List<Double> offsets = getOffsetsInTimeUnits();
    for (int i = 0; i < offsets.size() - 1; i++) {
      Double diff = offsets.get(i + 1) - offsets.get(i);
      counters.count("resol", diff);
    }

    return counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    CoordinateRuntime that = (CoordinateRuntime) o;

    if (!periodName.equals(that.periodName))
      return false;
    return Arrays.equals(runtimes, that.runtimes);

  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(runtimes);
    result = 31 * result + periodName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return name;
  }

  ///////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {

    CalendarPeriod timeUnit;

    public Builder2(CalendarPeriod timeUnit) {
      this.timeUnit = timeUnit;
    }

    @Override
    public Object extract(Grib2Record gr) {
      return gr.getReferenceDate().getMillisFromEpoch();
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Long> runtimeSorted = new ArrayList<>(values.size());
      for (Object val : values)
        runtimeSorted.add((Long) val);
      Collections.sort(runtimeSorted);
      return new CoordinateRuntime(runtimeSorted, timeUnit);
    }
  }

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    CalendarPeriod timeUnit;

    public Builder1(CalendarPeriod timeUnit) {
      this.timeUnit = timeUnit;
    }

    @Override
    public Object extract(Grib1Record gr) {
      return gr.getReferenceDate().getMillisFromEpoch();
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Long> runtimeSorted = new ArrayList<>(values.size());
      for (Object val : values)
        runtimeSorted.add((Long) val);
      Collections.sort(runtimeSorted);
      return new CoordinateRuntime(runtimeSorted, timeUnit);
    }
  }


}
