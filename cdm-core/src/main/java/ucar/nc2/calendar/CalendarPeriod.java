/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A CalendarPeriod is a logical duration of time, it requires a Calendar to convert to an actual duration of time.
 * A CalendarPeriod is expressed as {integer x Field}.
 */
@Immutable
public class CalendarPeriod {
  // LOOK is this needed?
  private static final Cache<CalendarPeriod, CalendarPeriod> cache = CacheBuilder.newBuilder().maximumSize(100).build();

  public static final CalendarPeriod Hour = CalendarPeriod.of(1, Field.Hour);
  public static final CalendarPeriod Second = CalendarPeriod.of(1, Field.Second);

  public enum Field {
    Millisec("millisecs", ChronoUnit.MILLIS), //
    Second("seconds", ChronoUnit.SECONDS), //
    Minute("minutes", ChronoUnit.MINUTES), //
    Hour("hours", ChronoUnit.HOURS), //
    Day("days", ChronoUnit.DAYS), //
    Month("months", ChronoUnit.MONTHS), //
    Year("years", ChronoUnit.YEARS); //

    final String name;
    final ChronoUnit chronoUnit;

    Field(String name, ChronoUnit chronoUnit) {
      this.name = name;
      this.chronoUnit = chronoUnit;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * Convert a udunit period string into a CalendarPeriod.Field.
   * 
   * @param udunit period string
   * @return CalendarPeriod.Field enum or null if not valid format
   */
  public static @Nullable Field fromUnitString(String udunit) {
    if (udunit == null) {
      return null;
    }
    udunit = udunit.trim();
    udunit = udunit.toLowerCase();

    if (udunit.equals("s")) {
      return Field.Second;
    }
    if (udunit.equals("ms")) {
      return Field.Millisec;
    }

    // eliminate plurals
    if (udunit.endsWith("s")) {
      udunit = udunit.substring(0, udunit.length() - 1);
    }

    switch (udunit) {
      case "second":
      case "sec":
      case "s":
        return Field.Second;
      case "millisecond":
      case "millisec":
      case "msec":
        return Field.Millisec;
      case "minute":
      case "min":
        return Field.Minute;
      case "hour":
      case "hr":
      case "h":
        return Field.Hour;
      case "day":
      case "d":
        return Field.Day;
      case "month":
      case "mon":
        return Field.Month;
      case "year":
      case "yr":
        return Field.Year;
      default:
        return null;
    }
  }

  // minimize memory use by interning. wacko shit in GribPartitionBuilder TimeCoordinate, whoduhthunk?
  public static CalendarPeriod of(int value, Field field) {
    CalendarPeriod want = new CalendarPeriod(value, field);
    if (cache == null)
      return want;
    CalendarPeriod got = cache.getIfPresent(want);
    if (got != null)
      return got;
    cache.put(want, want);
    return want;
  }

  /**
   * Convert a udunit period string into a CalendarPeriod
   * 
   * @param udunit period string : "[val] unit"
   * @return CalendarPeriod or null if illegal format or unknown unit
   */
  @Nullable
  public static CalendarPeriod of(String udunit) {
    if (udunit == null || udunit.isEmpty()) {
      return null;
    }
    int value;
    String units;

    ImmutableList<String> split = StringUtil2.splitList(udunit);
    if (split.size() == 1) {
      value = 1;
      units = split.get(0);

    } else if (split.size() == 2) {
      try {
        value = Integer.parseInt(split.get(0));
      } catch (Throwable t) {
        return null;
      }
      units = split.get(1);
    } else {
      return null;
    }

    Field unit = CalendarPeriod.fromUnitString(units);
    if (unit == null) {
      return null;
    }
    return CalendarPeriod.of(value, unit);
  }

  ////////////////////////
  // the common case of a single field
  private final int value;
  private final Field field;

  private CalendarPeriod(int value, Field field) {
    this.value = value;
    this.field = field;
  }

  public CalendarPeriod withValue(int value) {
    return new CalendarPeriod(value, this.field);
  }

  public int getValue() {
    return value;
  }

  public Field getField() {
    return field;
  }

  public ChronoUnit getChronoUnit() {
    return field.chronoUnit;
  }

  /*
   * Get the conversion factor of the other CalendarPeriod to this one
   * 
   * @param from convert from this
   * 
   * @return conversion factor, so that getConvertFactor(from) * from = this
   * 
   * @deprecated dont use because these are fixed length and thus approximate.
   *
   * @Deprecated
   * public double getConvertFactor(CalendarPeriod from) {
   * if (field == Field.Month || field == Field.Year) {
   * log.warn(" CalendarDate.convert on Month or Year");
   * }
   * 
   * return (double) from.millisecs() / millisecs();
   * }
   * 
   * /*
   * Get the duration in milliseconds -+
   * 
   * @return the duration in seconds
   * 
   * @deprecated dont use because these are fixed length and thus approximate.
   *
   * @Deprecated
   * public double getValueInMillisecs() {
   * if (field == Field.Month)
   * return 30.0 * 24.0 * 60.0 * 60.0 * 1000.0 * value;
   * else if (field == Field.Year)
   * return 365.0 * 24.0 * 60.0 * 60.0 * 1000.0 * value;
   * else
   * return millisecs();
   * }
   * 
   * private int millisecs() {
   * if (field == Field.Millisec)
   * return value;
   * else if (field == Field.Second)
   * return 1000 * value;
   * else if (field == Field.Minute)
   * return 60 * 1000 * value;
   * else if (field == Field.Hour)
   * return 60 * 60 * 1000 * value;
   * else if (field == Field.Day)
   * return 24 * 60 * 60 * 1000 * value;
   * 
   * else
   * throw new IllegalStateException("Illegal Field = " + field);
   * }
   */

  /*
   * LOOK from old TimeCoord code, which is better ??
   * public static int getOffset(CalendarDate refDate, CalendarDate cd, CalendarPeriod timeUnit) {
   * long msecs = cd.getDifferenceInMsecs(refDate);
   * return (int) Math.round(msecs / timeUnit.getValueInMillisecs());
   * }
   */

  /*
   * @deprecated use getOffset().
   * 
   * @Deprecated
   * public int subtract(CalendarDate start, CalendarDate end) {
   * long diff = end.getDifferenceInMsecs(start);
   * int thislen = millisecs();
   * if ((diff % thislen != 0))
   * log.warn("roundoff error");
   * return (int) (diff / thislen);
   * }
   */

  /*
   * TODO: review: start and end must have same calendar. should not convert to CalendarDate in the first place.
   * offset from start to end, in these units.
   * start + offset = end.
   * If not even, will round down and log a warning
   *
   * @param start start date
   * 
   * @param end end date
   * 
   * @return difference in units of this period
   *
   * public int getOffset(CalendarDate start, CalendarDate end) {
   * if (start.equals(end)) {
   * return 0;
   * }
   * long start_millis = start.getDateTime().getMillis();
   * long end_millis = end.getDateTime().getMillis();
   * 
   * // 5 second slop
   * Period p;
   * if (start_millis < end_millis)
   * p = new Period(start_millis, end_millis + 5000, getPeriodType());
   * else
   * p = new Period(start_millis + 5000, end_millis, getPeriodType());
   * 
   * return p.get(getDurationFieldType());
   * }
   * 
   * PeriodType getPeriodType() {
   * return getField().p;
   * }
   * 
   * DurationFieldType getDurationFieldType() {
   * return getField().p.getFieldType(0);
   * }
   */

  @Override
  public String toString() {
    return value + " " + field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CalendarPeriod that = (CalendarPeriod) o;
    return value == that.value && field == that.field;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, field);
  }
}
