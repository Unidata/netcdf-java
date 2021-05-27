/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

import ucar.nc2.time2.chrono.Uniform30DayDate;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

/**
 * A Calendar Date, replaces java.util.Date, using java.time.
 * Allows non-standard calendars. Default is ISO8601. Always in UTC time zone.
 * A Calendar Date is always composed of year, month, day, hour, min, sec, nano integer fields.
 */
@Immutable
public interface CalendarDate extends Comparable<CalendarDate> {
  CalendarDate unixEpoch = new CalendarDateIso(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC));

  /** Get a CalendarDate representing the present moment in ISO8601 UTC */
  static CalendarDate present() {
    return new CalendarDateIso(OffsetDateTime.now());
  }

  /**
   * Get a CalendarDate from an ISO date string, with extensions for udunit backward compatibility.
   *
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param isoDateString ISO date string
   *
   * @return CalendarDate or empty if the String is not a valid ISO 8601 date
   */
  static Optional<CalendarDate> fromUdunitIsoDate(@Nullable String calendarName, String isoDateString)
      throws IllegalArgumentException {
    Calendar cal = Calendar.get(calendarName).orElse(Calendar.getDefault());
    Optional<UdunitCalendarDateParser.ComponentFields> udunit =
        UdunitCalendarDateParser.parseUdunitIsoDate(isoDateString);

    return udunit.map(flds -> CalendarDate.of(cal, flds.year, flds.monthOfYear, flds.dayOfMonth, flds.hourOfDay,
        flds.minuteOfHour, flds.secondOfMinute, flds.nanoOfSecond, flds.zoneId));
  }

  /**
   * Get ISO Calendar date from fields, using UTC
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
   * Get Calendar date from fields.
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

    if (zoneId == null) {
      zoneId = ZoneOffset.UTC;
    }

    if (cal == Calendar.uniform30day) {
      Uniform30DayDate chronoDate = Uniform30DayDate.of(year, monthOfYear, dayOfMonth);
      LocalTime localTime = LocalTime.of(hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond);
      ChronoLocalDateTime<Uniform30DayDate> dt = chronoDate.atTime(localTime);
      // LOOK if (zoneId != ZoneOffset.UTC) {
      // dt = dt.withOffsetSameInstant(ZoneOffset.UTC);
      // }
      return new CalendarDateChrono(cal, dt);
    } else {
      OffsetDateTime dt = OffsetDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
          nanoOfSecond, zoneId);
      if (zoneId != ZoneOffset.UTC) {
        dt = dt.withOffsetSameInstant(ZoneOffset.UTC);
      }
      return new CalendarDateIso(dt);
    }
    // throw new UnsupportedOperationException("Unsupported calendar = " + cal);
  }

  static CalendarDate ofDoy(int year, int doy, int hourOfDay, int minuteOfHour, int secondOfMinute, int nanoOfSecond) {
    OffsetDateTime dt =
        OffsetDateTime.of(year, 1, 1, hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond, ZoneOffset.UTC);
    dt = dt.withDayOfYear(doy);
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
   * Create CalendarDate from msecs since epoch.
   * Uses ISO8601 UTC.
   * 
   * @param msecs milliseconds from 1970-01-01T00:00:00Z
   * @return ISO8601 CalendarDate in UTC
   */
  static CalendarDate of(long msecs) {
    OffsetDateTime dt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(msecs), ZoneOffset.UTC);
    return new CalendarDateIso(dt);
  }

  ////////////////////////////////////////////////

  /** The Calendar used by this CalendarDate. */
  Calendar getCalendar();

  /** Get the milliseconds of the datetime instant from the Java epoch of 1970-01-01T00:00:00Z. */
  long getMillis();

  int getFieldValue(CalendarPeriod.Field fld);

  /** Get the hour of day field for this chronology. */
  default int getHourOfDay() {
    return getFieldValue(CalendarPeriod.Field.Hour);
  }

  /** Get the day of the month field for this chronology. */
  default int getDayOfMonth() {
    return getFieldValue(CalendarPeriod.Field.Day);
  }

  /** Get difference between two CalendarDates in millisecs. */
  default long getDifferenceInMsecs(CalendarDate o) {
    return getMillis() - o.getMillis();
  }

  /** Get the equivilent java.util.Date */
  java.util.Date toDate();

  /**
   * Truncate the CalendarDate, by zeroing all the fields that are less than the named field.
   * So 2013-03-01T19:30 becomes 2013-03-01T00:00 if the field is "day"
   *
   * @param fld set to 0 all fields less than this one
   * @return truncated result
   */
  CalendarDate truncate(CalendarPeriod.Field fld);

  /** String in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZ) */
  @Override
  String toString();

  /** String using the given formatter */
  String format(DateTimeFormatter dtf);

  //////////////////////////////////////////////////////////

  CalendarDate add(CalendarPeriod period);

  CalendarDate add(long value, CalendarPeriod period);

  CalendarDate add(long value, CalendarPeriod.Field unit);

  boolean isAfter(CalendarDate o);

  boolean isBefore(CalendarDate o);

  long since(CalendarDate base, CalendarPeriod.Field field);

  long since(CalendarDate base, CalendarPeriod period);

}
