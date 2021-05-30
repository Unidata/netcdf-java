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
import java.time.ZonedDateTime;
import java.time.chrono.AbstractChronology;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Era;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoField.EPOCH_DAY;

/**
 * CF Calendar noleap (365_day) and all_leap (366_day) chronologies.
 * Follows the normal ISO calendar, except for the leap years.
 * <p>
 * LOOK: initial implementation based on ISO8601, not Gregorian.
 * Adapted from java.time.IsoChronology
 * <p>
 * The fields are defined as follows:
 * <ul>
 * <li>proleptic-year - The proleptic year is a continuously numbered value extended from the current era.
 * <li>month-of-year - There are 12 months in a year, numbered from 1 to 12.
 * <li>day-of-month - There are between 28 and 31 days in a month, numbered from 1 to 31, same as the ISO calendar.
 * <li>day-of-year - There are 365 days in a standard ISO year and 366 in a leap year.
 * The days are numbered from 1 to 365 or 1 to 366.
 * <li>leap-year - Depending on the instance, all years are leap yours, or none are leap years.
 * </ul>
 */
public final class LeapYearChronology extends AbstractChronology implements Serializable {
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
   * Days in long month.
   */
  static final int DAYS_IN_MONTH_LONG = 31;
  /**
   * Days in normal month.
   */
  static final int DAYS_IN_MONTH = 28;
  /**
   * 52 weeks in a normal year.
   */
  static final int WEEKS_IN_YEAR = 52;
  /**
   * Highest year in the range.
   */
  private static final long MAX_YEAR = 1_000_000L;
  /**
   * Range of year.
   */
  static final ValueRange YEAR_RANGE = ValueRange.of(-MAX_YEAR, MAX_YEAR);
  /**
   * Range of proleptic month.
   */
  private static final ValueRange PROLEPTIC_MONTH_RANGE =
      ValueRange.of(-MAX_YEAR * MONTHS_IN_YEAR, MAX_YEAR * MONTHS_IN_YEAR - 1);
  /**
   * Range of day of month.
   */
  static final ValueRange DAY_OF_MONTH_RANGE = ValueRange.of(1, DAYS_IN_MONTH, DAYS_IN_MONTH_LONG);
  /**
   * Range of month of year.
   */
  static final ValueRange MONTH_OF_YEAR_RANGE = ValueRange.of(1, MONTHS_IN_YEAR);
  /**
   * Range of eras.
   */
  static final ValueRange ERA_RANGE = ValueRange.of(0, 1);

  //////////////////////////////////////////////////////
  public static final LeapYearChronology INSTANCE_ALL_LEAP = new LeapYearChronology(true);
  public static final LeapYearChronology INSTANCE_NO_LEAP = new LeapYearChronology(false);

  /**
   * The minimum supported {@code LeapYearDate}, '-999999999-01-01'.
   * This could be used by an application as a "far past" date.
   */
  public final LeapYearDate MIN_DATE = LeapYearDate.of(this, (int) -MAX_YEAR, 1, 1);
  /**
   * The maximum supported {@code LeapYearDate}, '+999999999-12-31'.
   * This could be used by an application as a "far future" date.
   */
  public final LeapYearDate MAX_DATE = LeapYearDate.of(this, (int) MAX_YEAR, 12, 31);
  /**
   * The epoch year {@code LeapYearDate}, '1970-01-01'.
   */
  public final LeapYearDate EPOCH_DATE = LeapYearDate.of(this, 1970, 1, 1);

  /** All leap year or never leap year. */
  private final boolean allLeap;
  private final int daysPerYear;
  private final int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

  private LeapYearChronology(boolean allLeap) {
    this.allLeap = allLeap;
    this.daysPerYear = 365 + (allLeap ? 1 : 0);
    if (allLeap) {
      daysInMonth[1] += 1;
    }
  }

  int daysInYear() {
    return daysPerYear;
  }

  int daysInMonth(int monthZeroBased) {
    return daysInMonth[monthZeroBased];
  }

  ValueRange epochDateRange() {
    return ValueRange.of(-MAX_YEAR * daysPerYear, MAX_YEAR * daysPerYear - 1);

  }

  @Override
  public String getId() {
    return allLeap ? "CF_all_leap" : "CF_noleap";
  }

  @Override
  public String getCalendarType() {
    return null;
  }

  @Override // override with covariant return type
  public LeapYearDate date(int prolepticYear, int month, int dayOfMonth) {
    return LeapYearDate.of(this, prolepticYear, month, dayOfMonth);
  }

  @Override // override with covariant return type
  public LeapYearDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
    return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
  }

  @Override // override with covariant return type
  public LeapYearDate dateYearDay(int prolepticYear, int dayOfYear) {
    return LeapYearDate.ofYearDay(this, prolepticYear, dayOfYear);
  }

  @Override // override with covariant return type
  public LeapYearDate dateEpochDay(long epochDay) {
    EPOCH_DAY.checkValidValue(epochDay);
    int year = 1970 + (int) (epochDay / this.daysPerYear);
    int doy = (int) (epochDay % this.daysPerYear);
    if (doy < 0) {
      year--;
      doy += this.daysPerYear;
    }
    int month = 0;
    int dom = doy;
    while (dom >= daysInMonth[month]) {
      dom -= daysInMonth[month];
      month++;
    }
    return LeapYearDate.of(this, year, month + 1, dom + 1);
  }

  // LOOK off by 1?
  // long epochDay = (long) (this.year - 1) * DAYS_IN_YEAR + this.dayOfYear - DAYS_0001_TO_1970 - 1;
  // long epochDay = (long) (this.year - 1) * DAYS_IN_YEAR + this.dayOfYear - 1970L * DAYS_IN_YEAR - 1;
  // long epochDay = (long) (this.year - 1 - 1970) * DAYS_IN_YEAR + this.dayOfYear - 1;
  /** inverse of dateEpochDay. */
  public long toEpochDay(LeapYearDate date) {
    return (date.getProlepticYear() - 1970L) * this.daysPerYear + date.getDayOfYear() - 1;
  }

  @Override // override with covariant return type
  public LeapYearDate date(TemporalAccessor temporal) {
    return LeapYearDate.from(this, temporal);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ChronoLocalDateTime<LeapYearDate> localDateTime(TemporalAccessor temporal) {
    if (temporal instanceof ChronoLocalDateTime) {
      ChronoLocalDateTime<?> chronoz = (ChronoLocalDateTime<?>) temporal;
      ChronoLocalDate localDate = chronoz.toLocalDate();
      if (localDate instanceof Uniform30DayDate) {
        return (ChronoLocalDateTime<LeapYearDate>) temporal;
      }
    }
    return (ChronoLocalDateTime<LeapYearDate>) super.localDateTime(temporal);
  }

  @Override // override with covariant return type
  @SuppressWarnings("unchecked")
  public ChronoZonedDateTime<LeapYearDate> zonedDateTime(TemporalAccessor temporal) {
    if (temporal instanceof ChronoZonedDateTime) {
      ChronoZonedDateTime<?> chronoz = (ChronoZonedDateTime<?>) temporal;
      ChronoLocalDate localDate = chronoz.toLocalDate();
      if (localDate instanceof LeapYearDate) {
        return (ChronoZonedDateTime<LeapYearDate>) temporal;
      }
    }
    return (ChronoZonedDateTime<LeapYearDate>) super.zonedDateTime(temporal);
  }

  /**
   * Obtains a ChronoZonedDateTime in this chronology from an {@code Instant}.
   * <p>
   * This is equivalent to {@link ZonedDateTime#ofInstant(Instant, ZoneId)}.
   *
   * @param instant the instant to create the date-time from, not null
   * @param zone the time-zone, not null
   * @return the zoned date-time, not null
   * @throws DateTimeException if the result exceeds the supported range
   */
  @Override
  @SuppressWarnings("unchecked")
  public ChronoZonedDateTime<LeapYearDate> zonedDateTime(Instant instant, ZoneId zone) {
    // return ZonedDateTime.ofInstant(instant, zone);
    return (ChronoZonedDateTime<LeapYearDate>) super.zonedDateTime(instant, zone);
  }

  @Override // override with covariant return type
  public LeapYearDate dateNow() {
    return dateNow(Clock.systemDefaultZone());
  }

  @Override // override with covariant return type
  public LeapYearDate dateNow(ZoneId zone) {
    return dateNow(Clock.system(zone));
  }

  @Override // override with covariant return type
  public LeapYearDate dateNow(Clock clock) {
    Objects.requireNonNull(clock, "clock");
    return date(LeapYearDate.now(this, clock));
  }

  /**
   * Checks if the year is a leap year.
   * This is the main override, changing the leap year to always or never.
   */
  @Override
  public boolean isLeapYear(long prolepticYear) {
    return allLeap;
  }

  // -----------------------------------------------------------------------
  /** Do not use Eras with this Chronology */
  @Override
  public int prolepticYear(Era era, int yearOfEra) {
    if (!(era instanceof IsoEra)) {
      throw new ClassCastException("Era must be IsoEra");
    }
    return (era == IsoEra.CE ? yearOfEra : 1 - yearOfEra);
  }

  /** Do not use Eras with this Chronology */
  @Override
  public IsoEra eraOf(int eraValue) {
    return IsoEra.of(eraValue);
  }

  /** Do not use Eras with this Chronology */
  @Override
  public List<Era> eras() {
    return List.of(IsoEra.values());
  }

  // -----------------------------------------------------------------------
  /*
   * LOOK see Uniform30DayChronology range(), why different?
   * 
   * @Override
   * public ValueRange range(ChronoField field) {
   * return field.range();
   * }
   */

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
        return ValueRange.of(1, this.daysPerYear);
      case EPOCH_DAY:
        return ValueRange.of(-MAX_YEAR * this.daysPerYear, MAX_YEAR * this.daysPerYear);
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
}
