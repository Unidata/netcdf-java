/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;
import java.util.Date;
import java.util.Objects;

/** A range of CalendarDates: the half open interval [start, end) */
@Immutable
public class CalendarDateRange {
  private final CalendarDate start, end;

  public static CalendarDateRange of(CalendarDate start, CalendarDate end) {
    return new CalendarDateRange(start, end);
  }

  public static CalendarDateRange of(Date start, Date end) {
    return new CalendarDateRange(CalendarDate.of(start), CalendarDate.of(end));
  }

  public static CalendarDateRange of(CalendarDate start, long durationInSecs) {
    return new CalendarDateRange(start, durationInSecs);
  }

  private CalendarDateRange(CalendarDate start, CalendarDate end) {
    Preconditions.checkNotNull(start);
    Preconditions.checkNotNull(end);
    this.start = start;
    this.end = end;
    assert start.getCalendar() == end.getCalendar();
  }

  private CalendarDateRange(CalendarDate start, long durationInSecs) {
    this.start = start;
    this.end = start.add((int) durationInSecs, CalendarPeriod.Field.Second);
  }

  /** Starting date. */
  public CalendarDate getStart() {
    return start;
  }

  /** Ending date. */
  public CalendarDate getEnd() {
    return end;
  }

  /** The duration of the range in seconds. */
  public long getDurationInSecs() {
    return (end.getMillisFromEpoch() - start.getMillisFromEpoch()) / 1000;
  }

  /** Extend the range by another range. */
  public CalendarDateRange extend(CalendarDateRange other) {
    CalendarDate cs = other.getStart();
    CalendarDate s = start.isBefore(cs) ? start : cs; // earlier one

    CalendarDate ce = other.getEnd();
    CalendarDate e = end.isBefore(ce) ? ce : end; // later one

    return CalendarDateRange.of(s, e);
  }

  /** Does the range include this date? */
  public boolean includes(CalendarDate cd) {
    if (start.isAfter(cd)) {
      return false;
    } else {
      return !end.isBefore(cd);
    }
  }

  /** Create a new CalendarDateRange as the intersection of this and the given other range. */
  public CalendarDateRange intersect(CalendarDateRange clip) {
    CalendarDate cs = clip.getStart();
    CalendarDate s = start.isBefore(cs) ? cs : start; // later one

    CalendarDate ce = clip.getEnd();
    CalendarDate e = end.isBefore(ce) ? end : ce; // earlier one

    return CalendarDateRange.of(s, e);
  }

  /** Does the range intersect another range? */
  public boolean intersects(CalendarDateRange other) {
    if (start.isAfter(other.getEnd())) {
      return false;
    } else {
      return !end.isBefore(other.getStart());
    }
  }

  @Override
  public String toString() {
    return "[" + start + "," + end + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CalendarDateRange that = (CalendarDateRange) o;
    return start.equals(that.start) && end.equals(that.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }
}
