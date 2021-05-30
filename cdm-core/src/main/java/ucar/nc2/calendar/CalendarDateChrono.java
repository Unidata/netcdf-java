/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.calendar;

import java.time.Instant;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Objects;


/** A CalendarDate using java.time.chrono.ChronoLocalDateTime. */
class CalendarDateChrono extends CalendarDateIso implements CalendarDate, Comparable<CalendarDate> {
  private final Calendar cal;
  private final ChronoZonedDateTime<? extends ChronoLocalDate> chronoLocalDateTime;

  CalendarDateChrono(Calendar cal, ChronoZonedDateTime<? extends ChronoLocalDate> dateTime) {
    this.cal = cal;
    this.chronoLocalDateTime = dateTime;
  }

  @Override
  public Calendar getCalendar() {
    return cal;
  }

  @Override
  public long getMillis() {
    return chronoLocalDateTime.toInstant().toEpochMilli();
  }

  @Override
  public int compareTo(CalendarDate o) {
    if (o instanceof CalendarDateChrono) {
      CalendarDateChrono co = (CalendarDateChrono) o;
      return chronoLocalDateTime.compareTo(co.chronoLocalDateTime);
    }
    throw new IllegalArgumentException("Must be a ChronoDate");
  }

  @Override
  public boolean isAfter(CalendarDate o) {
    if (o instanceof CalendarDateChrono) {
      CalendarDateChrono co = (CalendarDateChrono) o;
      return chronoLocalDateTime.isAfter(co.chronoLocalDateTime);
    }
    throw new IllegalArgumentException("Must be a ChronoDate");
  }

  @Override
  public boolean isBefore(CalendarDate o) {
    if (o instanceof CalendarDateChrono) {
      CalendarDateChrono co = (CalendarDateChrono) o;
      return chronoLocalDateTime.isBefore(co.chronoLocalDateTime);
    }
    throw new IllegalArgumentException("Must be a ChronoDate");
  }

  @Override
  public String format(DateTimeFormatter dtf) {
    return chronoLocalDateTime.format(dtf);
  }

  @Override
  public int getFieldValue(CalendarPeriod.Field fld) {
    switch (fld) {
      case Day:
        return chronoLocalDateTime.get(ChronoField.DAY_OF_MONTH);
      case Hour:
        return chronoLocalDateTime.get(ChronoField.HOUR_OF_DAY);
      case Millisec:
        return chronoLocalDateTime.get(ChronoField.MILLI_OF_SECOND);
      case Minute:
        return chronoLocalDateTime.get(ChronoField.MINUTE_OF_HOUR);
      case Month:
        return chronoLocalDateTime.get(ChronoField.MONTH_OF_YEAR);
      case Second:
        return chronoLocalDateTime.get(ChronoField.SECOND_OF_MINUTE);
      case Year:
        return chronoLocalDateTime.get(ChronoField.YEAR);
    }
    throw new UnsupportedOperationException("getFieldValue = " + fld);
  }

  @Override
  public CalendarDate add(long value, CalendarPeriod.Field unit) {
    switch (unit) {
      case Millisec:
        return new CalendarDateChrono(getCalendar(), chronoLocalDateTime.plus(value, ChronoUnit.MILLIS));
      case Second:
        return new CalendarDateChrono(getCalendar(), chronoLocalDateTime.plus(value, ChronoUnit.SECONDS));
      case Minute:
        return new CalendarDateChrono(getCalendar(), chronoLocalDateTime.plus(value, ChronoUnit.MINUTES));
      case Hour:
        return new CalendarDateChrono(getCalendar(), chronoLocalDateTime.plus(value, ChronoUnit.HOURS));
      case Day:
        return new CalendarDateChrono(getCalendar(), chronoLocalDateTime.plus(value, ChronoUnit.DAYS));
      case Month:
        return new CalendarDateChrono(getCalendar(), chronoLocalDateTime.plus(value, ChronoUnit.MONTHS));
      case Year:
        return new CalendarDateChrono(getCalendar(), chronoLocalDateTime.plus(value, ChronoUnit.YEARS));
    }
    throw new UnsupportedOperationException("add units = " + unit);
  }

  @Override
  public long since(CalendarDate base, CalendarPeriod.Field field) {
    if (base instanceof CalendarDateChrono) {
      CalendarDateChrono co = (CalendarDateChrono) base;
      return co.chronoLocalDateTime.until(this.chronoLocalDateTime, field.chronoUnit);
    }
    throw new IllegalArgumentException("Must be a ChronoDate");
  }

  // LOOK what about period.value ?
  @Override
  public long since(CalendarDate base, CalendarPeriod period) {
    if (base instanceof CalendarDateChrono) {
      CalendarDateChrono co = (CalendarDateChrono) base;
      return co.chronoLocalDateTime.until(this.chronoLocalDateTime, period.getChronoUnit());
    }
    throw new IllegalArgumentException("Must be a ChronoDate");
  }

  /////////////////////////////////////////////

  @Override
  public String toString() {
    if (chronoLocalDateTime.get(ChronoField.MILLI_OF_SECOND) == 0) {
      if (chronoLocalDateTime.get(ChronoField.SECOND_OF_MINUTE) == 0) {
        return CalendarDateFormatter.toDateTimeStringNoSecs(this);
      } else {
        return CalendarDateFormatter.toDateTimeString(this);
      }
    } else {
      return CalendarDateFormatter.toDateTimeStringWithMillis(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    CalendarDateChrono that = (CalendarDateChrono) o;
    return cal == that.cal && chronoLocalDateTime.equals(that.chronoLocalDateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), cal, chronoLocalDateTime);
  }

  //// visible for testing

  ChronoZonedDateTime<?> chronoDateTime() {
    return chronoLocalDateTime;
  }

  public Instant toInstant() {
    return chronoLocalDateTime.toInstant();
  }

}
