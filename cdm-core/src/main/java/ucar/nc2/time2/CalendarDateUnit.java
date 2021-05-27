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
 * A Calendar Date Unit: "[CALENDAR] unit SINCE baseDate"
 * Its main job is to convert "value unit since baseDate" to a CalendarDate.
 */
@Immutable
public class CalendarDateUnit {
  // "seconds since the Unix epoch".
  public static final CalendarDateUnit unixDateUnit = CalendarDateUnit.of(CalendarPeriod.Field.Second, false,
      new CalendarDateIso(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)));

  /**
   * Create a CalendarDateUnit from a calendar and a udunit string = "unit since calendarDate"
   * 
   * @param calt use this Calendar, or null for default calendar
   * @param udunitString "unit since calendarDate"
   * @return CalendarDateUnit or empty if udunitString is not parseable
   */
  public static Optional<CalendarDateUnit> fromUdunitString(@Nullable Calendar calt, String udunitString) {
    if (udunitString == null || udunitString.isEmpty()) {
      return Optional.empty();
    }
    Optional<UdunitCalendarDateParser> udunit = UdunitCalendarDateParser.parseUnitString(udunitString);
    if (udunit.isEmpty()) {
      return Optional.empty();
    }
    UdunitCalendarDateParser ud = udunit.get();
    UdunitCalendarDateParser.ComponentFields flds = ud.flds;
    CalendarDate baseDate = CalendarDate.of(calt, flds.year, flds.monthOfYear, flds.dayOfMonth, flds.hourOfDay,
        flds.minuteOfHour, flds.secondOfMinute, flds.nanoOfSecond, flds.zoneId);
    return Optional.of(CalendarDateUnit.of(ud.periodField, ud.isCalendarField, baseDate));
  }

  /**
   * Create a CalendarDateUnit from a CalendarPeriod.Field, and a base date
   * 
   * @param periodField a CalendarPeriod.Field like Hour or second
   * @param baseDate "since baseDate"
   * @return CalendarDateUnit
   */
  public static CalendarDateUnit of(CalendarPeriod.Field periodField, boolean isCalendarField, CalendarDate baseDate) {
    return new CalendarDateUnit(CalendarPeriod.of(1, periodField), isCalendarField, baseDate);
  }

  public static CalendarDateUnit of(CalendarPeriod period, boolean isCalendarField, CalendarDate baseDate) {
    return new CalendarDateUnit(period, isCalendarField, baseDate);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final CalendarPeriod period;
  private final CalendarDate baseDate;
  private final boolean isCalendarField;

  private CalendarDateUnit(CalendarPeriod period, boolean isCalendarField, CalendarDate baseDate) {
    this.period = period;
    this.baseDate = baseDate;
    this.isCalendarField = isCalendarField;
  }

  public CalendarDate getBaseDateTime() {
    return baseDate;
  }

  public Calendar getCalendar() {
    return baseDate.getCalendar();
  }

  public CalendarPeriod.Field getCalendarField() {
    return period.getField();
  }

  public CalendarPeriod getCalendarPeriod() {
    return period;
  }

  public boolean isCalendarField() {
    return isCalendarField;
  }

  /**
   * Add the given (value * period) to the baseDateTime to make a new CalendarDate.
   * 
   * @param value number of periods to add. May be negative.
   */
  public CalendarDate makeCalendarDate(long value) {
    return baseDate.add(value, period);
  }

  /**
   * Find the offset of date in this unit (secs, days, etc) from the baseDateTime.
   * Inverse of makeCalendarDate.
   * LOOK not working when period is month.
   */
  public long makeOffsetFromRefDate(CalendarDate date) {
    if (date.equals(baseDate)) {
      return 0;
    }
    return date.since(baseDate, period);
  }

  /////////////////////////////////////


  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CalendarDateUnit that = (CalendarDateUnit) o;
    return isCalendarField == that.isCalendarField && period.equals(that.period) && baseDate.equals(that.baseDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(period, baseDate, isCalendarField);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    if (isCalendarField) {
      f.format("%s", UdunitDateParser.byCalendarString);
    }
    f.format("%s since %s", getCalendarField(), baseDate);
    return f.toString();
  }

}
