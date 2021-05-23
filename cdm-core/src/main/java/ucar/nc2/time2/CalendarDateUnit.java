/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Formatter;
import java.util.Objects;
import java.util.Optional;

/**
 * A Calendar Date Unit: "unit since date".
 * Its main job is to convert "value unit since date" to a CalendarDate.
 */
@Immutable
public class CalendarDateUnit {
  public static final CalendarDateUnit unixDateUnit = CalendarDateUnit.of(null, CalendarPeriod.Field.Second,
      OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), false);

  /**
   * Create a CalendarDateUnit from a calendar and a udunit string = "unit since calendarDate"
   * 
   * @param calt use this Calendar, or null for default calendar
   * @param udunitString "unit since calendarDate"
   * @return CalendarDateUnit or empty if udunitString is not parseable
   */
  public static Optional<CalendarDateUnit> fromUdunitString(@Nullable Calendar calt, String udunitString) {
    Optional<UdunitDateParser> udunit = UdunitDateParser.parseUnitString(udunitString);
    return udunit.map(u -> new CalendarDateUnit(calt, u.periodField, u.baseDate, u.isCalendarField));
  }

  /**
   * Create a CalendarDateUnit from a calendar, a CalendarPeriod.Field, and a base date
   * 
   * @param calt use this Calendar, or null for default calendar
   * @param periodField a CalendarPeriod.Field like Hour or second
   * @param baseDate "since baseDate"
   * @return CalendarDateUnit
   */
  public static CalendarDateUnit of(Calendar calt, CalendarPeriod.Field periodField, OffsetDateTime baseDate,
      boolean isCalendarField) {
    return new CalendarDateUnit(calt, periodField, baseDate, isCalendarField);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final Calendar cal;
  private final CalendarPeriod period;
  private final CalendarPeriod.Field periodField;
  private final CalendarDate baseDate;
  private final boolean isCalendarField;

  private CalendarDateUnit(@Nullable Calendar calt, CalendarPeriod.Field periodField, OffsetDateTime baseDate,
      boolean isCalendarField) {
    this.cal = calt == null ? Calendar.getDefault() : calt;
    this.periodField = periodField;
    this.period = CalendarPeriod.of(1, periodField);
    this.baseDate = new CalendarDate(calt, baseDate);
    this.isCalendarField = isCalendarField;
  }

  /* Find the offset of date in this unit (secs, days, etc) from the baseDate. inverse of makeCalendarDate. */
  public long makeOffsetFromRefDate(CalendarDate date) {
    if (date.equals(baseDate)) {
      return 0;
    }
    return date.since(baseDate, period);
  }

  public CalendarDate makeCalendarDate(long value) {
    return baseDate.add(value, period);
  }

  public CalendarDate getBaseDateTime() {
    return baseDate;
  }

  public CalendarPeriod getCalendarPeriod() {
    return period;
  }

  public CalendarPeriod.Field getCalendarField() {
    return periodField;
  }

  public Calendar getCalendar() {
    return cal;
  }

  public boolean isCalendarField() {
    return isCalendarField;
  }

  /////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CalendarDateUnit that = (CalendarDateUnit) o;
    return isCalendarField == that.isCalendarField && cal == that.cal && period.equals(that.period)
        && periodField == that.periodField && baseDate.equals(that.baseDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cal, period, periodField, baseDate, isCalendarField);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    if (isCalendarField) {
      f.format("%s", UdunitDateParser.byCalendarString);
    }
    f.format("%s since %s", periodField, baseDate);
    return f.toString();
  }

}
