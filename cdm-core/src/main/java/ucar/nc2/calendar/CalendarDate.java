/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import com.google.common.annotations.VisibleForTesting;
import ucar.nc2.calendar.chrono.LeapYearChronology;
import ucar.nc2.calendar.chrono.LeapYearDate;
import ucar.nc2.calendar.chrono.Uniform30DayDate;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.temporal.ChronoField;
import java.util.Optional;

/**
 * A Calendar Date, replaces java.util.Date, using java.time.
 * Allows non-standard calendars. Default is ISO8601. Always in UTC time zone.
 * A Calendar Date is always composed of year, month, day, hour, min, sec, nano integer fields.
 */
@Immutable
public interface CalendarDate extends Comparable<CalendarDate> {
  /** The Unix epoch (Jan, 1, 1970 00:00 UTC) */
  CalendarDate unixEpoch = new CalendarDateIso(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC));

  /** Create a CalendarDate representing the present moment in ISO8601 UTC */
  static CalendarDate present() {
    return new CalendarDateIso(OffsetDateTime.now());
  }

  /**
   * Create a CalendarDate from an ISO date string, with extensions for udunit backward compatibility.
   *
   * @param calendarName get Calendar from Calendar.get(calendarName). Must be null, or a valid calendar name.
   * @param udunitString udunit ISO date string
   *
   * @return CalendarDate or empty if the String is not a valid ISO 8601 date
   */
  static Optional<CalendarDate> fromUdunitIsoDate(@Nullable String calendarName, String udunitString)
      throws IllegalArgumentException {
    if (udunitString == null || udunitString.isEmpty()) {
      return Optional.empty();
    }
    Calendar cal;
    if (calendarName == null) {
      cal = Calendar.getDefault();
    } else {
      Optional<Calendar> calO = Calendar.get(calendarName);
      if (calO.isEmpty()) {
        return Optional.empty();
      }
      cal = calO.get();
    }

    Optional<UdunitCalendarDateParser.ComponentFields> udunit =
        UdunitCalendarDateParser.parseUdunitIsoDate(udunitString);
    return udunit.map(flds -> CalendarDate.of(cal, flds.year, flds.monthOfYear, flds.dayOfMonth, flds.hourOfDay,
        flds.minuteOfHour, flds.secondOfMinute, flds.nanoOfSecond, flds.zoneId));
  }

  /**
   * Create Calendar date from fields, using UTC and ISO8601 calendar.
   *
   * @param year any integer
   * @param monthOfYear 1-12
   * @param dayOfMonth 1-31
   * @param hourOfDay 0-23
   * @param minuteOfHour 0-59
   * @param secondOfMinute 0-59
   * @return CalendarDate
   */
  static CalendarDate of(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour,
      int secondOfMinute) {
    return of(null, year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, 0, null);
  }

  /**
   * Create Calendar date from fields.
   *
   * @param cal calendar to use, or null for default
   * @param year any integer
   * @param monthOfYear 1-12
   * @param dayOfMonth may depend on calendar, but typically 1-31
   * @param hourOfDay 0-23
   * @param minuteOfHour 0-59
   * @param secondOfMinute 0-59
   * @param nanoOfSecond from 0 to 999,999,999
   * @param zoneId may be null, indicating ZoneOffset.UTC
   * @return CalendarDate
   */
  static CalendarDate of(@Nullable Calendar cal, int year, int monthOfYear, int dayOfMonth, int hourOfDay,
      int minuteOfHour, int secondOfMinute, int nanoOfSecond, @Nullable ZoneOffset zoneId) {
    if (cal == null) {
      cal = Calendar.getDefault();
    }
    if (zoneId == null) {
      zoneId = ZoneOffset.UTC;
    }

    switch (cal) {
      case uniform30day: {
        Uniform30DayDate chronoDate = Uniform30DayDate.of(year, monthOfYear, dayOfMonth);
        LocalTime localTime = LocalTime.of(hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond);
        ChronoLocalDateTime<Uniform30DayDate> dt = chronoDate.atTime(localTime);
        ChronoZonedDateTime<Uniform30DayDate> dtz = dt.atZone(zoneId);
        return new CalendarDateChrono(cal, dtz);
      }
      case noleap:
      case all_leap: {
        LeapYearChronology chronology =
            cal == Calendar.all_leap ? LeapYearChronology.INSTANCE_ALL_LEAP : LeapYearChronology.INSTANCE_NO_LEAP;
        LeapYearDate leapYearDate = LeapYearDate.of(chronology, year, monthOfYear, dayOfMonth);
        LocalTime localTime = LocalTime.of(hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond);
        ChronoLocalDateTime<LeapYearDate> dt = leapYearDate.atTime(localTime);
        ChronoZonedDateTime<LeapYearDate> dtz = dt.atZone(zoneId);
        return new CalendarDateChrono(cal, dtz);
      }
      default: {
        OffsetDateTime dt = OffsetDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
            nanoOfSecond, zoneId);
        if (zoneId != ZoneOffset.UTC) {
          dt = dt.withOffsetSameInstant(ZoneOffset.UTC);
        }
        return new CalendarDateIso(dt);
      }
    }
    // throw new UnsupportedOperationException("Unsupported calendar = " + cal);
  }

  /**
   * Create Calendar date from dayOfYear and other fields, using UTC and ISO8601 calendar.
   *
   * @param year any integer
   * @param dayOfYear 1-366
   * @param hourOfDay 0-23
   * @param minuteOfHour 0-59
   * @param secondOfMinute 0-59
   * @param nanoOfSecond from 0 to 999,999,999
   * @return CalendarDate
   */
  static CalendarDate ofDoy(int year, int dayOfYear, int hourOfDay, int minuteOfHour, int secondOfMinute,
      int nanoOfSecond) {
    OffsetDateTime dt =
        OffsetDateTime.of(year, 1, 1, hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond, ZoneOffset.UTC);
    dt = dt.withDayOfYear(dayOfYear);
    return new CalendarDateIso(dt);
  }

  /**
   * Create CalendarDate from a java.util.Date.
   * Uses ISO8601 UTC.
   * 
   * @param date java.util.Date
   * @return ISO8601 CalendarDate in UTC
   */
  static CalendarDate of(java.util.Date date) {
    OffsetDateTime dt = OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    return new CalendarDateIso(dt);
  }

  /**
   * Create CalendarDate from msecs since epoch, using ISO8601 UTC.
   * 
   * @param msecs milliseconds from 1970-01-01T00:00:00Z
   * @return ISO8601 CalendarDate in UTC
   */
  static CalendarDate of(long msecs) {
    OffsetDateTime dt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(msecs), ZoneOffset.UTC);
    return new CalendarDateIso(dt);
  }

  ////////////////////////////////////////////////

  /** Get the Calendar used by this CalendarDate. */
  Calendar getCalendar();

  /** Get the milliseconds of the datetime instant from the Java epoch of 1970-01-01T00:00:00Z. */
  long getMillisFromEpoch();

  /** Get the value of the given field, eg Year, Month, Day, Hour... */
  int getFieldValue(CalendarPeriod.Field fld);

  /** Get the equivilent java.util.Date. Must be in ISO calendar. */
  java.util.Date toDate();

  /** Get the year field for this chronology. */
  default int getYear() {
    return getFieldValue(CalendarPeriod.Field.Year);
  }

  /** Get the month of year field for this chronology. */
  default int getMonthOfYear() {
    return getFieldValue(CalendarPeriod.Field.Month);
  }

  /** Get the day of the month field for this chronology. */
  default int getDayOfMonth() {
    return getFieldValue(CalendarPeriod.Field.Day);
  }

  /** Get the hour of day field for this chronology. */
  default int getHourOfDay() {
    return getFieldValue(CalendarPeriod.Field.Hour);
  }

  /** Get the hour of day field for this chronology. */
  default int getMinuteOfHour() {
    return getFieldValue(CalendarPeriod.Field.Minute);
  }

  /** Get the hour of day field for this chronology. */
  default int getSecondOfMinute() {
    return getFieldValue(CalendarPeriod.Field.Second);
  }

  /** Get the hour of day field for this chronology. */
  default int getMillisOfSecond() {
    return getFieldValue(CalendarPeriod.Field.Millisec);
  }

  /** Get difference between (this - other) in millisecs. Must be same Calendar. */
  default long getDifferenceInMsecs(CalendarDate o) {
    return getMillisFromEpoch() - o.getMillisFromEpoch();
  }

  /**
   * Truncate the CalendarDate, by zeroing all the fields that are less than the named field.
   * So 2013-03-01T19:30 becomes 2013-03-01T00:00 if the field is "day"
   *
   * @param fld set to 0 all fields less than this one
   * @return truncated result
   */
  default CalendarDate truncate(CalendarPeriod.Field fld) {
    switch (fld) {
      case Minute:
        return CalendarDate.of(getCalendar(), getYear(), getMonthOfYear(), getDayOfMonth(), getHourOfDay(),
            getMinuteOfHour(), 0, 0, ZoneOffset.UTC);
      case Hour:
        return CalendarDate.of(getCalendar(), getYear(), getMonthOfYear(), getDayOfMonth(), getHourOfDay(), 0, 0, 0,
            ZoneOffset.UTC);
      case Day:
        return CalendarDate.of(getCalendar(), getYear(), getMonthOfYear(), getDayOfMonth(), 0, 0, 0, 0, ZoneOffset.UTC);
      case Month:
        return CalendarDate.of(getCalendar(), getYear(), getMonthOfYear(), 1, 0, 0, 0, 0, ZoneOffset.UTC);
      case Year:
        return CalendarDate.of(getCalendar(), getYear(), 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }
    return this;
  }

  /** String in ISO8601 format */
  @Override
  String toString();

  /** String using the given formatter */
  String format(DateTimeFormatter dtf);

  //////////////////////////////////////////////////////////

  /** Add the given period to this CalendarDate, return a new one. */
  CalendarDate add(CalendarPeriod period);

  /** Multiply the given period and add to this CalendarDate, return a new one. */
  CalendarDate add(long multiply, CalendarPeriod period);

  /** Add the given period (value and unit) to this CalendarDate, return a new one. */
  CalendarDate add(long value, CalendarPeriod.Field unit);

  /** Return true if this CalendarDate is after the given one. Must be same Calendar. */
  boolean isAfter(CalendarDate o);

  /** Return true if this CalendarDate is before the given one. Must be same Calendar. */
  boolean isBefore(CalendarDate o);

  /** Return value of (this - start) in the given Field units. Must be same Calendar. */
  long since(CalendarDate start, CalendarPeriod.Field field);

  /** Return value of (this - start) in the given CalendarPeriod units. Must be same Calendar. */
  long since(CalendarDate start, CalendarPeriod period);

  ////////////////////////////////////////////////////////////////////////////////////
  // experimental - not working yet

  @VisibleForTesting
  /** Public by accident. */
  Instant toInstant();

  @VisibleForTesting
  /** Public by accident. */
  static CalendarDate ofInstant(@Nullable Calendar cal, Instant instant) {
    return of(instant.getLong(ChronoField.INSTANT_SECONDS) * 1000 + instant.getLong(ChronoField.MILLI_OF_SECOND));
  }

}
