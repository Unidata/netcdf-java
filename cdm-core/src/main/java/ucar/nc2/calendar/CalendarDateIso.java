/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

  @Override
  public ZoneOffset getZoneOffset() {
    return ZoneOffset.UTC;
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

  /** Get the equivilent java.util.Date */
  @Override
  public java.util.Date toDate() {
    return new java.util.Date(getMillis());
  }

  //// visible for testing

  OffsetDateTime dateTime() {
    return dateTime;
  }

  public Instant toInstant() {
    return dateTime.toInstant();
  }
}
