/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time;

import com.google.common.base.Splitter;
import org.joda.time.DateTime;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.units.DateRange;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Date;
import java.util.List;

/** A range of CalendarDates. */
@Immutable
public class CalendarDateRange {
  private final CalendarDate start, end;
  private final DateTime startDt, endDt;
  private final Calendar cal;

  public static CalendarDateRange of(CalendarDate start, CalendarDate end) {
    return new CalendarDateRange(start, end);
  }

  public static CalendarDateRange of(Date start, Date end) {
    return new CalendarDateRange(CalendarDate.of(start), CalendarDate.of(end));
  }

  private CalendarDateRange(CalendarDate start, CalendarDate end) {
    this.start = start;
    this.end = end;
    this.startDt = start.getDateTime();
    this.endDt = end.getDateTime();
    this.cal = start.getCalendar();
    assert this.cal == end.getCalendar();
  }

  public CalendarDateRange(CalendarDate start, long durationInSecs) {
    this.start = start;
    this.end = start.add((int) durationInSecs, CalendarPeriod.Field.Second);
    this.startDt = start.getDateTime();
    this.endDt = end.getDateTime();
    this.cal = start.getCalendar();
  }

  public CalendarDate getStart() {
    return start;
  }

  public CalendarDate getEnd() {
    return end;
  }

  public long getDurationInSecs() {
    return (endDt.getMillis() - startDt.getMillis()) / 1000;
  }

  // LOOK
  public CalendarDuration getDuration() {
    return null;
  }

  // LOOK
  public CalendarDuration getResolution() {
    return null;
  }

  // LOOK
  public void setResolution() {}

  public boolean intersects(CalendarDateRange o) {
    return intersects(o.getStart(), o.getEnd());
  }

  public boolean intersects(CalendarDate start, CalendarDate end) {
    if (startDt.isAfter(end.getDateTime()))
      return false;
    return !endDt.isBefore(start.getDateTime());
  }

  public boolean includes(CalendarDate cd) {
    DateTime dt = cd.getDateTime();
    if (startDt.isAfter(dt))
      return false;
    return !endDt.isBefore(dt);
  }

  public CalendarDateRange intersect(CalendarDateRange clip) {
    DateTime cs = clip.getStart().getDateTime();
    DateTime s = startDt.isBefore(cs) ? cs : startDt; // later one

    DateTime ce = clip.getEnd().getDateTime();
    DateTime e = endDt.isBefore(ce) ? endDt : ce; // earlier one

    return CalendarDateRange.of(CalendarDate.of(cal, s), CalendarDate.of(cal, e));
  }

  public CalendarDateRange extend(CalendarDateRange other) {
    DateTime cs = other.getStart().getDateTime();
    DateTime s = startDt.isBefore(cs) ? startDt : cs; // earlier one

    DateTime ce = other.getEnd().getDateTime();
    DateTime e = endDt.isBefore(ce) ? ce : endDt; // later one

    return CalendarDateRange.of(CalendarDate.of(cal, s), CalendarDate.of(cal, e));
  }

  public boolean isPoint() {
    return start.equals(end);
  }

  @Override
  public String toString() {
    return "[" + start + "," + end + "]";
  }

  /** The inverse of toString(), or null if cant parse */
  @Nullable
  public static CalendarDateRange parse(String source) {
    StringBuilder sourceb = new StringBuilder(source);
    StringUtil2.removeAll(sourceb, "[]");
    List<String> ss = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(sourceb);
    if (ss.size() != 2) {
      return null;
    }
    try {
      CalendarDate start = CalendarDate.parseISOformat(null, ss.get(0));
      CalendarDate end = CalendarDate.parseISOformat(null, ss.get(1));
      return CalendarDateRange.of(start, end);
    } catch (Exception e) {
      return null;
    }
  }

  ///////////////////////////////////////////////
  /**
   * Does not handle non-standard calendars
   * 
   * @deprecated do not use.
   */
  @Deprecated
  public static CalendarDateRange of(DateRange dr) {
    if (dr == null)
      return null;
    return CalendarDateRange.of(dr.getStart().getDate(), dr.getEnd().getDate());
  }

  /**
   * Does not handle non-standard calendars
   * 
   * @deprecated do not use.
   */
  @Deprecated
  public DateRange toDateRange() {
    return new DateRange(start.toDate(), end.toDate());
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CalendarDateRange that = (CalendarDateRange) o;

    // All other fields in this class are derived from start or end.
    return start.equals(that.start) && end.equals(that.end);
  }

  @Override
  public int hashCode() {
    int result = start.hashCode();
    result = 31 * result + end.hashCode();
    return result;
  }
}
