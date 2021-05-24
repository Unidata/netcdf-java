/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

import com.google.common.base.Preconditions;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
public class CalendarDate implements Comparable<CalendarDate> {
  public static final CalendarDate unixEpoch =
      CalendarDate.of(null, OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC));

  /**
   * Get a CalendarDate representing the present moment
   * 
   * @return CalendarDate representing the present moment in UTC
   */
  public static CalendarDate present() {
    return new CalendarDate(null, OffsetDateTime.now());
  }

  public static CalendarDate present(Calendar cal) {
    return new CalendarDate(cal, OffsetDateTime.now());
  }

  /**
   * Get Calendar date from fields. Uses UTC time zone
   * 
   * @param cal calendar to use, or null for default
   * @param year any integer
   * @param monthOfYear 1-12
   * @param dayOfMonth 1-31
   * @param hourOfDay 0-23
   * @param minuteOfHour 0-59
   * @param secondOfMinute 0-59
   * @return CalendarDate
   */
  public static CalendarDate of(Calendar cal, int year, int monthOfYear, int dayOfMonth, int hourOfDay,
      int minuteOfHour, int secondOfMinute) {
    OffsetDateTime dt =
        OffsetDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, 0, ZoneOffset.UTC);
    return new CalendarDate(cal, dt);
  }

  public static CalendarDate ofDoy(Calendar cal, int year, int doy, int hourOfDay, int minuteOfHour,
      int secondOfMinute) {
    OffsetDateTime dt = OffsetDateTime.of(year, 1, 1, hourOfDay, minuteOfHour, secondOfMinute, 0, ZoneOffset.UTC);
    dt = dt.withDayOfYear(doy);
    return new CalendarDate(cal, dt);
  }

  /**
   * Create CalendarDate from a java.util.Date.
   * Uses standard Calendar.
   * 
   * @param date java.util.Date
   * @return CalendarDate in UTC
   */
  public static CalendarDate of(java.util.Date date) {
    OffsetDateTime dt = OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    return new CalendarDate(null, dt);
  }

  /**
   * Create CalendarDate from msecs since epoch
   * Uses standard Calendar.
   * 
   * @param msecs milliseconds from 1970-01-01T00:00:00Z
   * @return CalendarDate in UTC
   */
  public static CalendarDate of(long msecs) {
    OffsetDateTime dt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(msecs), ZoneOffset.UTC);
    return new CalendarDate(null, dt);
  }

  /**
   * Get CalendarDate from an ISO date string, with extensions for udunit backward compatibility.
   * 
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param isoDateString ISO date string
   * @return CalendarDate or empty if the String is not a valid ISO 8601 date
   */
  public static Optional<CalendarDate> fromUdunitIsoDate(@Nullable String calendarName, String isoDateString)
      throws IllegalArgumentException {
    Calendar cal = Calendar.get(calendarName).orElse(Calendar.getDefault());
    Optional<OffsetDateTime> dt = UdunitDateParser.parseUdunitIsoDate(isoDateString);
    return dt.map(z -> new CalendarDate(cal, z));
  }

  // internal use only
  static CalendarDate of(@Nullable Calendar cal, OffsetDateTime dateTime) {
    return new CalendarDate(cal, dateTime);
  }

  ////////////////////////////////////////////////
  private final OffsetDateTime dateTime;
  private final Calendar cal;

  CalendarDate(@Nullable Calendar cal, OffsetDateTime dateTime) {
    this.cal = cal == null ? Calendar.getDefault() : cal;
    this.dateTime = Preconditions.checkNotNull(dateTime);
  }

  public Calendar getCalendar() {
    return cal;
  }

  /**
   * Gets the milliseconds of the datetime instant from the Java epoch
   * of 1970-01-01T00:00:00Z.
   *
   * @return the number of milliseconds since 1970-01-01T00:00:00Z
   */
  public long getMillis() {
    return dateTime.toInstant().toEpochMilli();
  }

  // LOOK maybe public ?
  OffsetDateTime getOffsetDateTime() {
    return dateTime;
  }

  @Override
  public int compareTo(CalendarDate o) {
    return dateTime.compareTo(o.dateTime);
  }

  public boolean isAfter(CalendarDate o) {
    return dateTime.isAfter(o.dateTime);
  }

  public boolean isBefore(CalendarDate o) {
    return dateTime.isBefore(o.dateTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof CalendarDate))
      return false;
    CalendarDate other = (CalendarDate) o;
    return other.cal == cal && other.dateTime.equals(dateTime);
  }

  @Override
  public int hashCode() {
    int result = 17;
    if (cal != null)
      result += 37 * result + cal.hashCode();
    result += 37 * result + dateTime.hashCode();
    return result;
  }

  /**
   * ISO formatted string
   * 
   * @return ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZ)
   */
  @Override
  public String toString() {
    return CalendarDateFormatter.toDateTimeStringISO(this);
  }

  /**
   * Get the hour of day (0-23) field for this chronology.
   * 
   * @return hour of day (0-23)
   */
  public int getHourOfDay() {
    return dateTime.getHour();
  }

  public int getFieldValue(CalendarPeriod.Field fld) {
    switch (fld) {
      case Day:
        return dateTime.get(ChronoField.DAY_OF_MONTH);
      case Hour:
        return dateTime.get(ChronoField.HOUR_OF_DAY);
      case Millisec:
        return dateTime.get(ChronoField.MILLI_OF_SECOND);
      case Minute:
        return dateTime.get(ChronoField.MINUTE_OF_HOUR);
      case Month:
        return dateTime.get(ChronoField.MONTH_OF_YEAR);
      case Second:
        return dateTime.get(ChronoField.SECOND_OF_MINUTE);
      case Year:
        return dateTime.get(ChronoField.YEAR);
    }
    throw new IllegalArgumentException("unimplemented " + fld);
  }

  public int getDayOfMonth() {
    return dateTime.getDayOfMonth();
  }

  public CalendarDate add(CalendarPeriod period) {
    return add(period.getValue(), period.getField());
  }

  public CalendarDate add(long value, CalendarPeriod period) {
    return add(value * period.getValue(), period.getField());
  }

  // LOOK why does ZonedDateTime also have minus() ?
  public CalendarDate add(long value, CalendarPeriod.Field unit) {
    switch (unit) {
      case Millisec:
        return new CalendarDate(cal, dateTime.plusNanos(value * 1000000));
      case Second:
        return new CalendarDate(cal, dateTime.plusSeconds(value));
      case Minute:
        return new CalendarDate(cal, dateTime.plusMinutes(value));
      case Hour:
        return new CalendarDate(cal, dateTime.plusHours(value));
      case Day:
        return new CalendarDate(cal, dateTime.plusDays(value));
      case Month:
        return new CalendarDate(cal, dateTime.plusMonths(value));
      case Year:
        return new CalendarDate(cal, dateTime.plusYears(value));
    }
    throw new UnsupportedOperationException("period units = " + unit);
  }

  public long since(CalendarDate base, CalendarPeriod.Field field) {
    return base.getOffsetDateTime().until(this.getOffsetDateTime(), field.chronoUnit);
  }

  // LOOK what about period.value ?
  public long since(CalendarDate base, CalendarPeriod period) {
    return base.getOffsetDateTime().until(this.getOffsetDateTime(), period.getChronoUnit());
  }

  /**
   * truncate the CalendarDate, by zeroing all the fields that are less than the field.
   * So 2013-03-01T19:30 becomes 2013-03-01T00:00 if the field is "day"
   * 
   * @param fld set to 0 all fields less than this one
   * @return truncated result
   */
  public CalendarDate truncate(CalendarPeriod.Field fld) {
    switch (fld) {
      case Minute:
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
            dateTime.getHour(), dateTime.getMinute(), 0);
      case Hour:
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
            dateTime.getHour(), 0, 0);
      case Day:
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), 0, 0, 0);
      case Month:
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthValue(), 1, 0, 0, 0);
      case Year:
        return CalendarDate.of(cal, dateTime.getYear(), 1, 1, 0, 0, 0);
    }
    return this;
  }

  /** Get the equivilent java.util.Date */
  public java.util.Date toDate() {
    return new java.util.Date(getMillis());
  }

  /**
   * Get difference between two calendar dates in millisecs
   * 
   * @param o other calendar date
   * @return (this minus o) difference in millisecs
   */
  public long getDifferenceInMsecs(CalendarDate o) {
    return getMillis() - o.getMillis();
  }
}
