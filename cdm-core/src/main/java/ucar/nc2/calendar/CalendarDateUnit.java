/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import com.google.common.base.Preconditions;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

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

  /**
   * Create a CalendarDateUnit from a CalendarPeriod, and a base date
   *
   * @param period a CalendarPeriod like 1 Hour or 30 seconds
   * @param baseDate "since baseDate"
   * @return CalendarDateUnit
   */
  public static CalendarDateUnit of(CalendarPeriod period, boolean isCalendarField, CalendarDate baseDate) {
    return new CalendarDateUnit(period, isCalendarField, baseDate);
  }

  /**
   * Create a CalendarDateUnit from attributes.
   *
   * @param atts AttributeContainer, look for 'units' attribute and 'calendar' attribute
   * @param units the units string, may be null
   * @return CalendarDateUnit if possible
   */
  public static Optional<CalendarDateUnit> fromAttributes(AttributeContainer atts, @Nullable String units) {
    Preconditions.checkNotNull(atts);
    if (units != null && units.trim().isEmpty()) {
      units = null;
    }
    if (units == null) {
      units = atts.findAttributeString(CDM.UDUNITS, null);
    }
    if (units == null) {
      units = atts.findAttributeString(CDM.UNITS, null);
    }
    if (units == null) {
      return Optional.empty();
    }

    String calS = atts.findAttributeString(CF.CALENDAR, null);
    Calendar cal = Calendar.get(calS).orElse(null);
    return CalendarDateUnit.fromUdunitString(cal, units);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final CalendarPeriod period;
  private final CalendarDate baseDate;
  private final boolean isCalendarField; // used to require integral (non fractional) values

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
   * Add the given integer (value * period) to the baseDateTime to make a new CalendarDate.
   * 
   * @param value number of periods to add. May be negative.
   */
  public CalendarDate makeCalendarDate(long value) {
    return baseDate.add(value, period);
  }

  /**
   * Find the offset of date in integral units of this period from the baseDateTime.
   * Inverse of makeCalendarDate.
   * LOOK not working when period is month, see TestCalendarDateUnit
   */
  public long makeOffsetFromRefDate(CalendarDate date) {
    if (date.equals(baseDate)) {
      return 0;
    }
    return date.since(baseDate, period);
  }

  /**
   * Add the given (value * period) to the baseDateTime to make a new CalendarDate.
   * Inverse of makeFractionalOffsetFromRefDate.
   * This uses makeCalendarDate when its a calendar date or uses a non-ISO calendar, and
   * rounds the value to a long.
   * @param value number of (possibly non-integral) periods to add. May be negative.
   */
  public CalendarDate makeFractionalCalendarDate(double value) {
    if (isCalendarField() || (getBaseDateTime() instanceof CalendarDateChrono)) {
      return makeCalendarDate((long) value);
    } else {
      double convert = period.getConvertFactor(CalendarPeriod.Millisec);
      long baseMillis = baseDate.getMillisFromEpoch();
      long msecs = baseMillis + (long) (value / convert);
      // round to seconds
      long rounded = 1000 * Math.round(msecs / 1000.0);
      return CalendarDate.of(rounded);
    }
  }

  /**
   * Find the offset of date in fractional units of this period from the baseDateTime.
   * Inverse of makeFractionalCalendarDate.
   * This uses makeOffsetFromRefDate when its a calendar date or uses a non-ISO calendar, and
   * rounds the value to a long.
   * @return number of (possibly non-integral) periods since base date. May be negative.
   */
  public double makeFractionalOffsetFromRefDate(CalendarDate date) {
    if (date.equals(baseDate)) {
      return 0;
    }
    if (isCalendarField() || (getBaseDateTime() instanceof CalendarDateChrono)) {
      return (double) makeOffsetFromRefDate(date);
    } else {
      long diff = date.getDifferenceInMsecs(baseDate);
      double convert = period.getConvertFactor(CalendarPeriod.Millisec);
      return diff * convert;
    }
  }

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
      f.format("%s", UdunitCalendarDateParser.byCalendarString);
    }
    f.format("%s since %s", getCalendarField(), baseDate);
    return f.toString();
  }

}
