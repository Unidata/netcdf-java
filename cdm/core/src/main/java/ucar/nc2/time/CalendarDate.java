/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time;

import java.util.GregorianCalendar;

import com.google.common.base.Preconditions;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A Calendar Date, replaces java.util.Date, currently wraps joda.time.
 * Allows non-standard calendars. Default is Calendar.gregorian.
 * Always in UTC time zone.
 * LOOK should a CalendarDate have a CalendarDateUnit?
 *
 * @author caron
 * @since 3/21/11
 */
@Immutable
public class CalendarDate implements Comparable<CalendarDate> {
  public static final CalendarDate UNKNOWN = CalendarDate.of(0);
  public static final double MILLISECS_IN_SECOND = 1000;
  public static final double MILLISECS_IN_MINUTE = MILLISECS_IN_SECOND * 60;
  public static final double MILLISECS_IN_HOUR = MILLISECS_IN_MINUTE * 60;
  public static final double MILLISECS_IN_DAY = MILLISECS_IN_HOUR * 24;
  public static final double MILLISECS_IN_YEAR = 3.15569259747E10;
  public static final double MILLISECS_IN_MONTH = MILLISECS_IN_YEAR / 12;

  /**
   * Get a CalendarDate representing the present moment
   * 
   * @return CalendarDate representing the present moment in UTC
   */
  public static CalendarDate present() {
    return new CalendarDate(null, new DateTime());
  }

  public static CalendarDate present(Calendar cal) {
    return new CalendarDate(cal, new DateTime());
  }

  /**
   * Get Calendar date from fields. Uses UTZ time zone
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
    Chronology base = Calendar.getChronology(cal);
    DateTime dt = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, base);
    if (!Calendar.isDefaultChronology(cal))
      dt = dt.withChronology(Calendar.getChronology(cal));
    dt = dt.withZone(DateTimeZone.UTC);
    return new CalendarDate(cal, dt);
  }

  public static CalendarDate withDoy(Calendar cal, int year, int doy, int hourOfDay, int minuteOfHour,
      int secondOfMinute) {
    Chronology base = Calendar.getChronology(cal);
    DateTime dt = new DateTime(year, 1, 1, hourOfDay, minuteOfHour, secondOfMinute, base);
    dt = dt.withZone(DateTimeZone.UTC);
    dt = dt.withDayOfYear(doy);
    if (!Calendar.isDefaultChronology(cal))
      dt = dt.withChronology(Calendar.getChronology(cal));

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
    DateTime dt = new DateTime(date, DateTimeZone.UTC);
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
    // Constructs an instance set to the milliseconds from 1970-01-01T00:00:00Z using ISOChronology in the specified
    // time zone.
    DateTime dt = new DateTime(msecs, DateTimeZone.UTC);
    return new CalendarDate(null, dt);
  }

  /**
   * Create CalendarDate from msecs since epoch
   * Uses the given Calendar.
   * 
   * @param cal calendar to use, or null for default
   * @param msecs milliseconds from 1970-01-01T00:00:00Z
   * @return CalendarDate in UTC time zone.
   */
  public static CalendarDate of(Calendar cal, long msecs) {
    Chronology base = Calendar.getChronology(cal);
    DateTime dt = new DateTime(msecs, base);
    return new CalendarDate(cal, dt);
  }

  /**
   * Get CalendarDate from ISO date string
   * 
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param isoOrUdunits ISO or udunits date string
   * @return CalendarDate or null if not valid
   */
  @Nullable
  public static CalendarDate parseUdunitsOrIso(String calendarName, String isoOrUdunits) {
    CalendarDate result;
    try {
      result = parseISOformat(calendarName, isoOrUdunits);
    } catch (Exception e) {
      try {
        result = parseUdunits(calendarName, isoOrUdunits);
      } catch (Exception e2) {
        return null;
      }
    }
    return result;
  }

  /**
   * Get CalendarDate from ISO date string
   * 
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param isoDateString ISO date string
   * @return CalendarDate
   * @throws IllegalArgumentException if the String is not a valid ISO 8601 date
   */
  public static CalendarDate parseISOformat(String calendarName, String isoDateString) throws IllegalArgumentException {
    Calendar cal = Calendar.get(calendarName);
    if (cal == null) {
      cal = Calendar.getDefault();
    }
    return CalendarDateFormatter.isoStringToCalendarDate(cal, isoDateString);
  }

  /**
   * Get CalendarDate from udunit date string
   * 
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param udunitString must be valid udunits string
   * @return CalendarDate
   * @throws IllegalArgumentException if udunitString is not parseable
   */
  public static CalendarDate parseUdunits(String calendarName, String udunitString) {
    int pos = udunitString.indexOf(' ');
    if (pos < 0)
      return null;
    String valString = udunitString.substring(0, pos).trim();
    String unitString = udunitString.substring(pos + 1).trim();

    CalendarDateUnit cdu = CalendarDateUnit.of(calendarName, unitString);
    double val = Double.parseDouble(valString);
    return cdu.makeCalendarDate(val);
  }

  /**
   * Get CalendarDateUnit from udunit date string, by throwing away the value if it exists.
   *
   * @param calendarName get Calendar from Calendar.get(calendarName). may be null
   * @param udunits must be value (space) udunits string
   * @return CalendarDate
   */
  public static CalendarDateUnit parseUdunitsUnit(String calendarName, String udunits) {
    int pos = udunits.indexOf(' ');
    String unitString = udunits.substring(pos < 0 ? 0 : pos + 1).trim();
    return CalendarDateUnit.of(calendarName, unitString);
  }

  // internal use only
  static CalendarDate of(Calendar cal, DateTime dateTime) {
    return new CalendarDate(cal, dateTime);
  }

  ////////////////////////////////////////////////

  private final DateTime dateTime;
  private final Calendar cal;

  CalendarDate(@Nullable Calendar cal, DateTime dateTime) {
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
    return dateTime.getMillis();
  }

  // package private
  DateTime getDateTime() {
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
   * TODO rename toUdunitsString ?
   * udunits formatting
   * 
   * @return udunits formatted date
   */
  public String getTimeUnits() {
    return CalendarDateFormatter.toTimeUnits(this);
  }

  /**
   * Get the hour of day (0-23) field for this chronology.
   * 
   * @return hour of day (0-23)
   */
  public int getHourOfDay() {
    return dateTime.getHourOfDay();
  }

  /*
   * Millisec(PeriodType.millis()), Second(PeriodType.seconds()), Minute(PeriodType.minutes()),
   * Hour(PeriodType.hours()),
   * Day(PeriodType.days()), Month(PeriodType.months()), Year(PeriodType.years())
   */
  public int getFieldValue(CalendarPeriod.Field fld) {
    switch (fld) {
      case Day:
        return dateTime.get(DateTimeFieldType.dayOfMonth());
      case Hour:
        return dateTime.get(DateTimeFieldType.hourOfDay());
      case Millisec:
        return dateTime.get(DateTimeFieldType.millisOfSecond());
      case Minute:
        return dateTime.get(DateTimeFieldType.minuteOfHour());
      case Month:
        return dateTime.get(DateTimeFieldType.monthOfYear());
      case Second:
        return dateTime.get(DateTimeFieldType.secondOfMinute());
      case Year:
        return dateTime.get(DateTimeFieldType.year());
    }
    throw new IllegalArgumentException("unimplemented " + fld);
  }

  public int getDayOfMonth() {
    return dateTime.getDayOfMonth();
  }

  // old style udunits compatible.
  // fixes from https://github.com/jonescc 8/26/14

  /**
   * @deprecated use CalendarDate add(CalendarPeriod period)
   *             Note that CalendarPeriod has integral values
   */
  @Deprecated
  public CalendarDate add(double value, CalendarPeriod.Field unit) {
    switch (unit) {
      case Millisec:
        return new CalendarDate(cal, dateTime.plus(Math.round(value)));
      case Second:
        return new CalendarDate(cal, dateTime.plus(Math.round(value * MILLISECS_IN_SECOND)));
      case Minute:
        return new CalendarDate(cal, dateTime.plus(Math.round(value * MILLISECS_IN_MINUTE)));
      case Hour:
        return new CalendarDate(cal, dateTime.plus(Math.round(value * MILLISECS_IN_HOUR)));
      case Day:
        return new CalendarDate(cal, dateTime.plus(Math.round(value * MILLISECS_IN_DAY)));
      case Month: // LOOK should we throw warning ?
        return new CalendarDate(cal, dateTime.plus(Math.round(value * MILLISECS_IN_MONTH)));
      case Year: // LOOK should we throw warning ?
        return new CalendarDate(cal, dateTime.plus(Math.round(value * MILLISECS_IN_YEAR)));
    }
    throw new UnsupportedOperationException("period units = " + unit);
  }

  // calendar date field
  public CalendarDate add(CalendarPeriod period) {
    switch (period.getField()) {
      case Millisec:
        return new CalendarDate(cal, dateTime.plusMillis(period.getValue()));
      case Second:
        return new CalendarDate(cal, dateTime.plusSeconds(period.getValue()));
      case Minute:
        return new CalendarDate(cal, dateTime.plusMinutes(period.getValue()));
      case Hour:
        return new CalendarDate(cal, dateTime.plusHours(period.getValue()));
      case Day:
        return new CalendarDate(cal, dateTime.plusDays(period.getValue()));
      case Month:
        return new CalendarDate(cal, dateTime.plusMonths(period.getValue()));
      case Year:
        return new CalendarDate(cal, dateTime.plusYears(period.getValue()));
    }
    throw new UnsupportedOperationException("period units = " + period);
  }

  // calendar date field
  public CalendarDate subtract(CalendarPeriod period) {
    switch (period.getField()) {
      case Millisec:
        return new CalendarDate(cal, dateTime.minusMillis(period.getValue()));
      case Second:
        return new CalendarDate(cal, dateTime.minusSeconds(period.getValue()));
      case Minute:
        return new CalendarDate(cal, dateTime.minusMinutes(period.getValue()));
      case Hour:
        return new CalendarDate(cal, dateTime.minusHours(period.getValue()));
      case Day:
        return new CalendarDate(cal, dateTime.minusDays(period.getValue()));
      case Month:
        return new CalendarDate(cal, dateTime.minusMonths(period.getValue()));
      case Year:
        return new CalendarDate(cal, dateTime.minusYears(period.getValue()));
    }
    throw new UnsupportedOperationException("period units = " + period);
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
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
            dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), 0);
      case Hour:
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
            dateTime.getHourOfDay(), 0, 0);
      case Day:
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), 0, 0, 0);
      case Month:
        return CalendarDate.of(cal, dateTime.getYear(), dateTime.getMonthOfYear(), 1, 0, 0, 0);
      case Year:
        return CalendarDate.of(cal, dateTime.getYear(), 1, 1, 0, 0, 0);
    }
    return this;
  }

  /**
   * Get the equivilent java.util.Date
   * 
   * @return the equivalent Date
   */
  public java.util.Date toDate() {
    return dateTime.toDate();
  }

  /**
   * Get difference between two calendar dates in millisecs
   * 
   * @param o other calendar date
   * @return (this minus o) difference in millisecs
   */
  public long getDifferenceInMsecs(CalendarDate o) {
    return dateTime.getMillis() - o.dateTime.getMillis();
  }

  /**
   * Get difference between two calendar dates in given Field units
   * 
   * @param o other calendar date
   * @return (this minus o) difference in units of this Field
   */
  public long getDifference(CalendarDate o, CalendarPeriod.Field fld) {
    switch (fld) {
      case Millisec:
        return getDifferenceInMsecs(o);
      case Second:
        return (long) (getDifferenceInMsecs(o) / MILLISECS_IN_SECOND);
      case Minute:
        return (long) (getDifferenceInMsecs(o) / MILLISECS_IN_MINUTE);
      case Hour:
        return (long) (getDifferenceInMsecs(o) / MILLISECS_IN_HOUR);
      case Day:
        return (long) (getDifferenceInMsecs(o) / MILLISECS_IN_DAY);

      case Month:
        int tmonth = getFieldValue(CalendarPeriod.Field.Month);
        int omonth = o.getFieldValue(CalendarPeriod.Field.Month);
        int years = (int) this.getDifference(o, CalendarPeriod.Field.Year);
        return tmonth - omonth + 12 * years;

      case Year:
        int tyear = getFieldValue(CalendarPeriod.Field.Year);
        int oyear = o.getFieldValue(CalendarPeriod.Field.Year);
        return tyear - oyear;
    }
    return dateTime.getMillis() - o.dateTime.getMillis();
  }

  public GregorianCalendar toGregorianCalendar() {
    DateTimeZone zone = dateTime.getZone();
    GregorianCalendar cal = new GregorianCalendar(zone.toTimeZone());
    cal.setTime(toDate());
    return cal;
  }
}
