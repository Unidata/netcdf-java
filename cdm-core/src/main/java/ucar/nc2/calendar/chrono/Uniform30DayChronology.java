/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar.chrono;

import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.AbstractChronology;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Era;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;

/**
 * CF Calendar uniform30day or 360_day: All years are 360 days long. Each year consists of 12 months, and each month is
 * 30 days long.
 * Adapted from org.threeten.extra.chrono.Symmetry454Chronology
 * <p>
 * The fields are defined as follows:
 * <ul>
 * <li>proleptic-year - The proleptic year is a continuously numbered value extended from the current era.
 * <li>month-of-year - There are 12 months in a year, numbered from 1 to 12.
 * <li>day-of-month - There are 30 days in all months, numbered from 1 to 30.
 * <li>day-of-year - There are 360 days in a year, numbered from 1 to 360.
 * <li>leap-year - There are no leap years.
 * </ul>
 *
 * <h3>Implementation Requirements</h3>
 * This class is immutable and thread-safe.
 */
public final class Uniform30DayChronology extends AbstractChronology implements Serializable {

  /**
   * Singleton instance for the Uniform30Day chronology.
   */
  public static final Uniform30DayChronology INSTANCE = new Uniform30DayChronology();

  /**
   * Standard 7 day weeks.
   */
  static final int DAYS_IN_WEEK = 7;
  /**
   * Standard 12 month years.
   */
  static final int MONTHS_IN_YEAR = 12;
  /**
   * Normal month is 4 weeks.
   */
  static final int WEEKS_IN_MONTH = 4;
  /**
   * Long month is 5 weeks.
   */
  static final int WEEKS_IN_MONTH_LONG = 5;
  /**
   * Days in year, 8 months of 28 days plus 4 months of 35 days, or 364 days in a normal year.
   */
  static final int DAYS_IN_YEAR = 360;
  /**
   * Days in normal month.
   */
  static final int DAYS_IN_MONTH = 30;
  /**
   * 52 weeks in a normal year.
   */
  static final int WEEKS_IN_YEAR = DAYS_IN_YEAR / DAYS_IN_WEEK;
  /**
   * Highest year in the range.
   */
  private static final long MAX_YEAR = 1_000_000L;
  /**
   * Range of year.
   */
  static final ValueRange YEAR_RANGE = ValueRange.of(-MAX_YEAR, MAX_YEAR);
  /**
   * The number of days from year zero to CE 1970.
   */
  public static final long DAYS_0001_TO_1970 = 1970L * DAYS_IN_YEAR;
  /**
   * Epoch day range.
   */
  static final ValueRange EPOCH_DAY_RANGE =
      ValueRange.of(-MAX_YEAR * DAYS_IN_YEAR - DAYS_0001_TO_1970, MAX_YEAR * DAYS_IN_YEAR - DAYS_0001_TO_1970);
  /**
   * Range of proleptic month.
   */
  private static final ValueRange PROLEPTIC_MONTH_RANGE =
      ValueRange.of(-MAX_YEAR * MONTHS_IN_YEAR, MAX_YEAR * MONTHS_IN_YEAR - 1);
  /**
   * Range of day of month.
   */
  static final ValueRange DAY_OF_MONTH_RANGE = ValueRange.of(1, DAYS_IN_MONTH);
  /**
   * Range of day of year.
   */
  static final ValueRange DAY_OF_YEAR_RANGE = ValueRange.of(1, DAYS_IN_YEAR);
  /**
   * Range of month of year.
   */
  static final ValueRange MONTH_OF_YEAR_RANGE = ValueRange.of(1, MONTHS_IN_YEAR);
  /**
   * Range of eras.
   */
  static final ValueRange ERA_RANGE = ValueRange.of(0, 1);

  @Override
  public String getId() {
    return "CF_uniform30day";
  }

  @Override
  public String getCalendarType() {
    return null;
  }

  // -----------------------------------------------------------------------
  /**
   * Obtains a local date in Uniform30Day calendar system from the
   * proleptic-year, month-of-year and day-of-month fields.
   *
   * @param prolepticYear the proleptic-year
   * @param month the month-of-year
   * @param dayOfMonth the day-of-month
   * @return the Uniform30Day local date, not null
   * @throws DateTimeException if unable to create the date
   */
  @Override
  public Uniform30DayDate date(int prolepticYear, int month, int dayOfMonth) {
    return Uniform30DayDate.of(prolepticYear, month, dayOfMonth);
  }

  /**
   * Obtains a local date in Uniform30Day calendar system from the
   * era, year-of-era and day-of-year fields.
   *
   * @param era the Uniform30Day era, not null
   * @param yearOfEra the year-of-era
   * @param dayOfYear the day-of-year
   * @return the Uniform30Day local date, not null
   * @throws DateTimeException if unable to create the date
   * @throws ClassCastException if the {@code era} is not a {@code IsoEra}
   */
  @Override
  public Uniform30DayDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
    return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
  }

  /**
   * Obtains a local date in Uniform30Day calendar system from the
   * proleptic-year and day-of-year fields.
   *
   * @param prolepticYear the proleptic-year
   * @param dayOfYear the day-of-year
   * @return the Uniform30Day local date, not null
   * @throws DateTimeException if unable to create the date
   */
  @Override
  public Uniform30DayDate dateYearDay(int prolepticYear, int dayOfYear) {
    return Uniform30DayDate.ofYearDay(prolepticYear, dayOfYear);
  }

  /**
   * Obtains a local date in the Uniform30Day calendar system from the epoch-day.
   *
   * @param epochDay the epoch day
   * @return the Uniform30Day local date, not null
   * @throws DateTimeException if unable to create the date
   */
  @Override // override with covariant return type
  public Uniform30DayDate dateEpochDay(long epochDay) {
    return Uniform30DayDate.ofEpochDay(epochDay);
  }

  // -------------------------------------------------------------------------
  /**
   * Obtains the current Uniform30Day local date from the system clock in the default time-zone.
   * <p>
   * This will query the {@link Clock#systemDefaultZone() system clock} in the default
   * time-zone to obtain the current date.
   * <p>
   * Using this method will prevent the ability to use an alternate clock for testing
   * because the clock is hard-coded.
   *
   * @return the current Uniform30Day local date using the system clock and default time-zone, not null
   * @throws DateTimeException if unable to create the date
   */
  @Override // override with covariant return type
  public Uniform30DayDate dateNow() {
    return Uniform30DayDate.now();
  }

  /**
   * Obtains the current Uniform30Day local date from the system clock in the specified time-zone.
   * <p>
   * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
   * Specifying the time-zone avoids dependence on the default time-zone.
   * <p>
   * Using this method will prevent the ability to use an alternate clock for testing
   * because the clock is hard-coded.
   *
   * @param zone the zone ID to use, not null
   * @return the current Uniform30Day local date using the system clock, not null
   * @throws DateTimeException if unable to create the date
   */
  @Override // override with covariant return type
  public Uniform30DayDate dateNow(ZoneId zone) {
    return Uniform30DayDate.now(zone);
  }

  /**
   * Obtains the current Uniform30Day local date from the specified clock.
   * <p>
   * This will query the specified clock to obtain the current date - today.
   * Using this method allows the use of an alternate clock for testing.
   * The alternate clock may be introduced using {@link Clock dependency injection}.
   *
   * @param clock the clock to use, not null
   * @return the current Uniform30Day local date, not null
   * @throws DateTimeException if unable to create the date
   */
  @Override // override with covariant return type
  public Uniform30DayDate dateNow(Clock clock) {
    return Uniform30DayDate.now(clock);
  }

  // -------------------------------------------------------------------------
  @Override // override with covariant return type
  public Uniform30DayDate date(TemporalAccessor temporal) {
    if (temporal instanceof Uniform30DayDate) {
      return (Uniform30DayDate) temporal;
    }
    return Uniform30DayDate.from(temporal);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ChronoLocalDateTime<Uniform30DayDate> localDateTime(TemporalAccessor temporal) {
    if (temporal instanceof ChronoLocalDateTime) {
      ChronoLocalDateTime<?> chronoz = (ChronoLocalDateTime<?>) temporal;
      ChronoLocalDate localDate = chronoz.toLocalDate();
      if (localDate instanceof Uniform30DayDate) {
        return (ChronoLocalDateTime<Uniform30DayDate>) temporal;
      }
    }
    return (ChronoLocalDateTime<Uniform30DayDate>) super.localDateTime(temporal);
  }

  @Override // override with covariant return type
  @SuppressWarnings("unchecked")
  public ChronoZonedDateTime<Uniform30DayDate> zonedDateTime(TemporalAccessor temporal) {
    if (temporal instanceof ChronoZonedDateTime) {
      ChronoZonedDateTime<?> chronoz = (ChronoZonedDateTime<?>) temporal;
      ChronoLocalDate localDate = chronoz.toLocalDate();
      if (localDate instanceof Uniform30DayDate) {
        return (ChronoZonedDateTime<Uniform30DayDate>) temporal;
      }
    }
    return (ChronoZonedDateTime<Uniform30DayDate>) super.zonedDateTime(temporal);
  }

  /**
   * Obtains a Uniform30Day zoned date-time in this chronology from an {@code Instant}.
   *
   * @param instant the instant to create the date-time from, not null
   * @param zone the time-zone, not null
   * @return the Uniform30Day zoned date-time, not null
   * @throws DateTimeException if the result exceeds the supported range
   */
  @Override
  @SuppressWarnings("unchecked")
  public ChronoZonedDateTime<Uniform30DayDate> zonedDateTime(Instant instant, ZoneId zone) {
    return (ChronoZonedDateTime<Uniform30DayDate>) super.zonedDateTime(instant, zone);
  }

  @Override
  public boolean isLeapYear(long year) {
    return false;
  }

  /** Do not use Eras with this Chronology */
  @Override
  public IsoEra eraOf(int eraValue) {
    return IsoEra.of(eraValue);
  }

  /** Do not use Eras with this Chronology */
  @Override
  public List<Era> eras() {
    return Arrays.asList(IsoEra.values());
  }

  // -----------------------------------------------------------------------
  @Override
  public ValueRange range(ChronoField field) {
    switch (field) {
      case ALIGNED_DAY_OF_WEEK_IN_YEAR:
      case ALIGNED_DAY_OF_WEEK_IN_MONTH:
      case DAY_OF_WEEK:
        return ValueRange.of(1, DAYS_IN_WEEK);
      case ALIGNED_WEEK_OF_MONTH:
        return ValueRange.of(1, WEEKS_IN_MONTH, WEEKS_IN_MONTH_LONG);
      case ALIGNED_WEEK_OF_YEAR:
        return ValueRange.of(1, WEEKS_IN_YEAR, WEEKS_IN_YEAR + 1);
      case DAY_OF_MONTH:
        return DAY_OF_MONTH_RANGE;
      case DAY_OF_YEAR:
        return DAY_OF_YEAR_RANGE;
      case EPOCH_DAY:
        return EPOCH_DAY_RANGE;
      case ERA:
        return ERA_RANGE;
      case MONTH_OF_YEAR:
        return MONTH_OF_YEAR_RANGE;
      case PROLEPTIC_MONTH:
        return PROLEPTIC_MONTH_RANGE;
      case YEAR_OF_ERA:
      case YEAR:
        return YEAR_RANGE;
      default:
        return field.range();
    }
  }

  /** Do not use Eras with this Chronology */
  @Override
  public int prolepticYear(Era era, int yearOfEra) {
    if (!(era instanceof IsoEra)) {
      throw new ClassCastException("Invalid era: " + era);
    }
    YEAR_RANGE.checkValidIntValue(yearOfEra, ChronoField.YEAR_OF_ERA);
    return yearOfEra;
  }
}
