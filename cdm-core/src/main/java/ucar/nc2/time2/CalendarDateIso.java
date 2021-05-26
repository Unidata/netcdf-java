/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Objects;

/** A CalendarDate using java.time.OffsetDateTime. */
@Immutable
class CalendarDateIso implements CalendarDate {
  private final OffsetDateTime dateTime;

  CalendarDateIso(OffsetDateTime dateTime) {
    this.dateTime = Preconditions.checkNotNull(dateTime);
  }

  CalendarDateIso() {
    this.dateTime = null;
  }

  @Override
  public Calendar getCalendar() {
    return Calendar.proleptic_gregorian;
  }

  /**
   * Gets the milliseconds of the datetime instant from the Java epoch
   * of 1970-01-01T00:00:00Z. LOOK
   *
   * @return the number of milliseconds since 1970-01-01T00:00:00Z
   */
  @Override
  public long getMillis() {
    return dateTime.toInstant().toEpochMilli();
  }

  @Override
  public int compareTo(CalendarDate o) {
    CalendarDateIso iso = (CalendarDateIso) o;
    return dateTime.compareTo(iso.dateTime);
  }

  @Override
  public boolean isAfter(CalendarDate o) {
    CalendarDateIso iso = (CalendarDateIso) o;
    return dateTime.isAfter(iso.dateTime);
  }

  @Override
  public boolean isBefore(CalendarDate o) {
    CalendarDateIso iso = (CalendarDateIso) o;
    return dateTime.isBefore(iso.dateTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CalendarDateIso that = (CalendarDateIso) o;
    return Objects.equals(dateTime, that.dateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dateTime);
  }

  /**
   * ISO formatted string
   * 
   * @return ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZ)
   */
  @Override
  public String toString() {
    return dateTime.toString();
  }

  @Override
  public String format(DateTimeFormatter dtf) {
    return dateTime.format(dtf);
  }

  @Override
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

  /**
   * Get the hour of day (0-23) field for this chronology.
   *
   * @return hour of day (0-23)
   */
  @Override
  public int getHourOfDay() {
    return dateTime.getHour();
  }

  @Override
  public int getDayOfMonth() {
    return dateTime.getDayOfMonth();
  }

  @Override
  public CalendarDate add(CalendarPeriod period) {
    return add(period.getValue(), period.getField());
  }

  @Override
  public CalendarDate add(long value, CalendarPeriod period) {
    return add(value * period.getValue(), period.getField());
  }

  // LOOK why does OffsetDateTime also have minus() ?
  @Override
  public CalendarDate add(long value, CalendarPeriod.Field unit) {
    switch (unit) {
      case Millisec:
        return new CalendarDateIso(dateTime.plusNanos(value * 1000000));
      case Second:
        return new CalendarDateIso(dateTime.plusSeconds(value));
      case Minute:
        return new CalendarDateIso(dateTime.plusMinutes(value));
      case Hour:
        return new CalendarDateIso(dateTime.plusHours(value));
      case Day:
        return new CalendarDateIso(dateTime.plusDays(value));
      case Month:
        return new CalendarDateIso(dateTime.plusMonths(value));
      case Year:
        return new CalendarDateIso(dateTime.plusYears(value));
    }
    throw new UnsupportedOperationException("period units = " + unit);
  }

  @Override
  public long since(CalendarDate base, CalendarPeriod.Field field) {
    CalendarDateIso iso = (CalendarDateIso) base;
    return iso.dateTime.until(this.dateTime, field.chronoUnit);
  }

  // LOOK what about period.value ?
  @Override
  public long since(CalendarDate base, CalendarPeriod period) {
    CalendarDateIso iso = (CalendarDateIso) base;
    return iso.dateTime.until(this.dateTime, period.getChronoUnit());
  }

  /**
   * truncate the CalendarDate, by zeroing all the fields that are less than the field.
   * So 2013-03-01T19:30 becomes 2013-03-01T00:00 if the field is "day"
   * 
   * @param fld set to 0 all fields less than this one
   * @return truncated result
   */
  @Override
  public CalendarDate truncate(CalendarPeriod.Field fld) {
    switch (fld) {
      case Minute:
        return CalendarDate.of(getCalendar(), dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
            dateTime.getHour(), dateTime.getMinute(), 0, 0, dateTime.getOffset());
      case Hour:
        return CalendarDate.of(getCalendar(), dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
            dateTime.getHour(), 0, 0, 0, dateTime.getOffset());
      case Day:
        return CalendarDate.of(getCalendar(), dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), 0,
            0, 0, 0, dateTime.getOffset());
      case Month:
        return CalendarDate.of(getCalendar(), dateTime.getYear(), dateTime.getMonthValue(), 1, 0, 0, 0, 0,
            dateTime.getOffset());
      case Year:
        return CalendarDate.of(getCalendar(), dateTime.getYear(), 1, 1, 0, 0, 0, 0, dateTime.getOffset());
    }
    return this;
  }

  /** Get the equivilent java.util.Date */
  @Override
  public java.util.Date toDate() {
    return new java.util.Date(getMillis());
  }

}
