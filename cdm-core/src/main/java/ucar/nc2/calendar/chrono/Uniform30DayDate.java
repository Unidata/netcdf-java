/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar.chrono;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Objects;

import static ucar.nc2.calendar.chrono.Uniform30DayChronology.DAYS_0001_TO_1970;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.DAYS_IN_MONTH;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.DAYS_IN_WEEK;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.DAYS_IN_YEAR;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.DAY_OF_MONTH_RANGE;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.DAY_OF_YEAR_RANGE;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.EPOCH_DAY_RANGE;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.ERA_RANGE;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.INSTANCE;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.MONTHS_IN_YEAR;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.MONTH_OF_YEAR_RANGE;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.WEEKS_IN_MONTH;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.WEEKS_IN_YEAR;
import static ucar.nc2.calendar.chrono.Uniform30DayChronology.YEAR_RANGE;

/**
 * A date in the Uniform30Day calendar system.
 * Adapted from org.threeten.extra.chrono.Symmetry454Date
 */
public final class Uniform30DayDate extends AbstractDate implements ChronoLocalDate {

  private final int prolepticYear;
  private final int month;
  private final int day;
  /**
   * The day of year.
   */
  private final transient int dayOfYear;

  // -----------------------------------------------------------------------
  /**
   * Obtains the current {@code Uniform30DayDate} from the system clock in the default time-zone.
   * <p>
   * This will query the {@link Clock#systemDefaultZone() system clock} in the default
   * time-zone to obtain the current date.
   * <p>
   * Using this method will prevent the ability to use an alternate clock for testing
   * because the clock is hard-coded.
   *
   * @return the current date using the system clock and default time-zone, not null
   */
  public static Uniform30DayDate now() {
    return now(Clock.systemDefaultZone());
  }

  /**
   * Obtains the current {@code Uniform30DayDate} from the system clock in the specified time-zone.
   * <p>
   * This will query the {@link Clock#system(ZoneId) system clock} to obtain the current date.
   * Specifying the time-zone avoids dependence on the default time-zone.
   * <p>
   * Using this method will prevent the ability to use an alternate clock for testing
   * because the clock is hard-coded.
   *
   * @param zone the zone ID to use, not null
   * @return the current date using the system clock, not null
   */
  public static Uniform30DayDate now(ZoneId zone) {
    return now(Clock.system(zone));
  }

  /**
   * Obtains the current {@code Uniform30DayDate} from the specified clock.
   * <p>
   * This will query the specified clock to obtain the current date - today.
   * Using this method allows the use of an alternate clock for testing.
   * The alternate clock may be introduced using {@linkplain Clock dependency injection}.
   *
   * @param clock the clock to use, not null
   * @return the current date, not null
   * @throws DateTimeException if the current date cannot be obtained
   */
  public static Uniform30DayDate now(Clock clock) {
    LocalDate now = LocalDate.now(clock);
    return Uniform30DayDate.ofEpochDay(now.toEpochDay());
  }

  /**
   * Obtains a {@code Uniform30DayDate} representing a date in the Uniform30Day calendar
   * system from the proleptic-year, month-of-year and day-of-month fields.
   * <p>
   * This returns a {@code Uniform30DayDate} with the specified fields.
   * The day must be valid for the year and month, otherwise an exception will be thrown.
   *
   * @param prolepticYear the Uniform30Day proleptic-year
   * @param month the Uniform30Day month-of-year, from 1 to 12
   * @param dayOfMonth the Uniform30Day day-of-month, from 1 to 28, or 1 to 35 in February, May, August, November
   *        and December in a Leap Year
   * @return the date in Uniform30Day calendar system, not null
   * @throws DateTimeException if the value of any field is out of range,
   *         or if the day-of-month is invalid for the month-year
   */
  public static Uniform30DayDate of(int prolepticYear, int month, int dayOfMonth) {
    return create(prolepticYear, month, dayOfMonth);
  }

  /**
   * Obtains a {@code Uniform30DayDate} representing a date in the Uniform30Day calendar
   * system from the proleptic-year and day-of-year fields.
   * <p>
   * This returns a {@code Uniform30DayDate} with the specified fields.
   * The day must be valid for the year, otherwise an exception will be thrown.
   *
   * @param prolepticYear the Uniform30Day proleptic-year
   * @param dayOfYear the Uniform30Day day-of-year, from 1 to 360
   * @return the date in Uniform30Day calendar system, not null
   * @throws DateTimeException if the value of any field is out of range,
   *         or if the day-of-year is invalid for the year
   */
  static Uniform30DayDate ofYearDay(int prolepticYear, int dayOfYear) {
    YEAR_RANGE.checkValidValue(prolepticYear, ChronoField.YEAR_OF_ERA);
    DAY_OF_YEAR_RANGE.checkValidValue(dayOfYear, ChronoField.DAY_OF_YEAR);
    if (dayOfYear > DAYS_IN_YEAR) {
      throw new DateTimeException("Invalid date 'DayOfYear " + dayOfYear + "' as '" + prolepticYear);
    }

    dayOfYear--;
    int month = dayOfYear / DAYS_IN_MONTH;
    int day = dayOfYear % DAYS_IN_MONTH;
    return new Uniform30DayDate(prolepticYear, month + 1, day + 1);
  }

  /**
   * Obtains a {@code Uniform30DayDate} representing a date in the Uniform30Day calendar
   * system from the epoch-day.
   *
   * @param epochDay the epoch day to convert based on 1970-01-01 (ISO)
   * @return the date in Uniform30Day calendar system, not null
   * @throws DateTimeException if the epoch-day is out of range
   */
  static Uniform30DayDate ofEpochDay(long epochDay) {
    EPOCH_DAY_RANGE.checkValidValue(epochDay, ChronoField.EPOCH_DAY);
    long zeroDay = epochDay + DAYS_0001_TO_1970;
    long year = zeroDay / DAYS_IN_YEAR;
    long doy = zeroDay % DAYS_IN_YEAR;
    return ofYearDay((int) year + 1, (int) doy + 1);
  }

  /**
   * Obtains a {@code Uniform30DayDate} from a temporal object.
   * <p>
   * This obtains a date in the Uniform30Day calendar system based on the specified temporal.
   * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
   * which this factory converts to an instance of {@code Uniform30DayDate}.
   * <p>
   * The conversion typically uses the {@link ChronoField#EPOCH_DAY EPOCH_DAY}
   * field, which is standardized across calendar systems.
   * <p>
   * This method matches the signature of the functional interface {@link TemporalQuery}
   * allowing it to be used as a query via method reference, {@code Uniform30DayDate::from}.
   *
   * @param temporal the temporal object to convert, not null
   * @return the date in the Uniform30Day calendar system, not null
   * @throws DateTimeException if unable to convert to a {@code Uniform30DayDate}
   */
  public static Uniform30DayDate from(TemporalAccessor temporal) {
    if (temporal instanceof Uniform30DayDate) {
      return (Uniform30DayDate) temporal;
    }
    return Uniform30DayDate.ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
  }

  /**
   * Factory method, validates the given triplet year, month and dayOfMonth.
   *
   * @param prolepticYear the Uniform30Day proleptic-year
   * @param month the Uniform30Day month, from 1 to 12
   * @param dayOfMonth the Uniform30Day day-of-month, from 1 to 28, or 1 to 35 in February, May, August, November
   *        and December in a Leap Year
   * @return the Uniform30Day date
   * @throws DateTimeException if the date is invalid
   */
  static Uniform30DayDate create(int prolepticYear, int month, int dayOfMonth) {
    YEAR_RANGE.checkValidValue(prolepticYear, ChronoField.YEAR_OF_ERA);
    MONTH_OF_YEAR_RANGE.checkValidValue(month, ChronoField.MONTH_OF_YEAR);
    DAY_OF_MONTH_RANGE.checkValidValue(dayOfMonth, ChronoField.DAY_OF_MONTH);

    return new Uniform30DayDate(prolepticYear, month, dayOfMonth);
  }

  /**
   * Consistency check for dates manipulations after calls to
   * {@link #plus(long, TemporalUnit)},
   * {@link #minus(long, TemporalUnit)},
   * {@link #until(AbstractDate, TemporalUnit)} or
   * {@link #with(TemporalField, long)}.
   *
   * @param prolepticYear the Uniform30Day proleptic-year
   * @param month the Uniform30Day month, from 1 to 12
   * @return the resolved date
   */
  private static Uniform30DayDate resolvePreviousValid(int prolepticYear, int month, int day) {
    int monthR = Math.min(month, MONTHS_IN_YEAR);
    int dayR = Math.min(day, DAYS_IN_MONTH);

    return create(prolepticYear, monthR, dayR);
  }

  /**
   * Creates an instance from validated data.
   *
   * @param prolepticYear the Uniform30Day proleptic-year
   * @param month the Uniform30Day month, from 1 to 12
   * @param dayOfMonth the Uniform30Day day-of-month, from 1 to 30
   */
  private Uniform30DayDate(int prolepticYear, int month, int dayOfMonth) {
    this.prolepticYear = prolepticYear;
    this.month = month;
    this.day = dayOfMonth;
    this.dayOfYear = DAYS_IN_MONTH * (month - 1) + dayOfMonth;
  }

  @Override
  public ValueRange range(TemporalField field) {
    if (field instanceof ChronoField) {
      if (isSupported(field)) {
        ChronoField f = (ChronoField) field;
        switch (f) {
          case ALIGNED_DAY_OF_WEEK_IN_MONTH:
          case ALIGNED_DAY_OF_WEEK_IN_YEAR:
          case DAY_OF_WEEK:
            return ValueRange.of(1, DAYS_IN_WEEK);
          case ALIGNED_WEEK_OF_MONTH:
            return ValueRange.of(1, WEEKS_IN_MONTH);
          case ALIGNED_WEEK_OF_YEAR:
            return ValueRange.of(1, WEEKS_IN_YEAR);
          case DAY_OF_MONTH:
            return ValueRange.of(1, lengthOfMonth());
          case DAY_OF_YEAR:
            return ValueRange.of(1, lengthOfYear());
          case EPOCH_DAY:
            return EPOCH_DAY_RANGE;
          case ERA:
            return ERA_RANGE;
          case MONTH_OF_YEAR:
            return MONTH_OF_YEAR_RANGE;
          default:
            break;
        }
      } else {
        throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
      }
    }
    return super.range(field);
  }

  @Override
  ValueRange rangeAlignedWeekOfMonth() {
    // never invoked
    return ValueRange.of(1, WEEKS_IN_MONTH);
  }

  @Override
  Uniform30DayDate resolvePrevious(int newYear, int newMonth, int dayOfMonth) {
    return resolvePreviousValid(newYear, newMonth, dayOfMonth);
  }

  @Override
  public Uniform30DayChronology getChronology() {
    return INSTANCE;
  }

  /** Do not use Eras with this Chronology */
  @Override
  public IsoEra getEra() {
    return (prolepticYear >= 1 ? IsoEra.CE : IsoEra.BCE);
  }

  @Override
  public int lengthOfMonth() {
    return DAYS_IN_MONTH;
  }

  @Override
  public int lengthOfYear() {
    return DAYS_IN_YEAR;
  }

  @Override
  int getProlepticYear() {
    return prolepticYear;
  }

  @Override
  int getMonth() {
    return month;
  }

  @Override
  int getDayOfMonth() {
    return day;
  }

  @Override
  int getDayOfYear() {
    return dayOfYear;
  }

  @Override
  int lengthOfYearInMonths() {
    return MONTHS_IN_YEAR;
  }

  @Override
  int getAlignedDayOfWeekInMonth() {
    return getDayOfWeek();
  }

  @Override
  int getAlignedDayOfWeekInYear() {
    return getDayOfWeek();
  }

  @Override
  int getAlignedWeekOfMonth() {
    return ((day - 1) / DAYS_IN_WEEK) + 1;
  }

  @Override
  int getAlignedWeekOfYear() {
    return ((dayOfYear - 1) / DAYS_IN_WEEK) + 1;
  }

  @Override
  int getDayOfWeek() {
    return ((day - 1) % DAYS_IN_WEEK) + 1;
  }

  long getProlepticWeek() {
    return getProlepticMonth() * WEEKS_IN_MONTH + ((getDayOfMonth() - 1) / DAYS_IN_WEEK) - 1;
  }

  @Override
  public boolean isLeapYear() {
    return false;
  }

  @Override
  public Uniform30DayDate with(TemporalAdjuster adjuster) {
    return (Uniform30DayDate) adjuster.adjustInto(this);
  }

  @Override
  public Uniform30DayDate with(TemporalField field, long newValue) {
    if (field instanceof ChronoField) {
      if (newValue == 0) {
        return this;
      }
      ChronoField f = (ChronoField) field;
      getChronology().range(f).checkValidValue(newValue, f);
      int nval = (int) newValue;
      switch (f) {
        case ALIGNED_DAY_OF_WEEK_IN_MONTH:
        case ALIGNED_DAY_OF_WEEK_IN_YEAR:
        case DAY_OF_WEEK:
          range(f).checkValidValue(newValue, field);
          int dom = ((getDayOfMonth() - 1) / DAYS_IN_WEEK) * DAYS_IN_WEEK;
          return resolvePreviousValid(prolepticYear, month, dom + nval);
        case ALIGNED_WEEK_OF_MONTH:
          range(f).checkValidValue(newValue, field);
          int d = day % DAYS_IN_WEEK;
          return resolvePreviousValid(prolepticYear, month, (nval - 1) * DAYS_IN_WEEK + d);
        case ALIGNED_WEEK_OF_YEAR:
          range(f).checkValidValue(newValue, field);
          int newMonth = 1 + ((nval - 1) / WEEKS_IN_MONTH);
          int newDay = ((nval - 1) % WEEKS_IN_MONTH) * DAYS_IN_WEEK + 1 + ((day - 1) % DAYS_IN_WEEK);
          return resolvePreviousValid(prolepticYear, newMonth, newDay);
        case DAY_OF_MONTH:
          return create(prolepticYear, month, nval);
        default:
          break;
      }
    }
    return (Uniform30DayDate) super.with(field, newValue);
  }

  @Override
  Uniform30DayDate withDayOfYear(int value) {
    return ofYearDay(prolepticYear, value);
  }

  @Override
  public Uniform30DayDate plus(TemporalAmount amount) {
    return (Uniform30DayDate) amount.addTo(this);
  }

  @Override
  public Uniform30DayDate plus(long amountToAdd, TemporalUnit unit) {
    return (Uniform30DayDate) super.plus(amountToAdd, unit);
  }

  @Override
  public Uniform30DayDate minus(TemporalAmount amount) {
    return (Uniform30DayDate) amount.subtractFrom(this);
  }

  @Override
  public Uniform30DayDate minus(long amountToSubtract, TemporalUnit unit) {
    return (Uniform30DayDate) super.minus(amountToSubtract, unit);
  }

  @Override // for covariant return type
  @SuppressWarnings("unchecked")
  public ChronoLocalDateTime<Uniform30DayDate> atTime(LocalTime localTime) {
    return (ChronoLocalDateTime<Uniform30DayDate>) super.atTime(localTime);
  }

  @Override
  public long until(Temporal endExclusive, TemporalUnit unit) {
    return until(Uniform30DayDate.from(endExclusive), unit);
  }

  @Override
  public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
    Uniform30DayDate end = Uniform30DayDate.from(endDateExclusive);
    int years = Math.toIntExact(yearsUntil(end));
    // Get to the same "whole" year.
    Uniform30DayDate sameYearEnd = (Uniform30DayDate) plusYears(years);
    int months = (int) sameYearEnd.monthsUntil(end);
    int days = (int) sameYearEnd.plusMonths(months).daysUntil(end);
    return getChronology().period(years, months, days);
  }

  /**
   * Get the number of years from this date to the given day.
   *
   * @param end The end date.
   * @return The number of years from this date to the given day.
   */
  long yearsUntil(Uniform30DayDate end) {
    long startYear = this.prolepticYear * 512L + this.getDayOfYear();
    long endYear = end.prolepticYear * 512L + end.getDayOfYear();
    return (endYear - startYear) / 512L;
  }

  @Override
  long weeksUntil(AbstractDate end) {
    Uniform30DayDate endDate = Uniform30DayDate.from(end);
    long startWeek = this.getProlepticWeek() * 8L + this.getDayOfWeek();
    long endWeek = endDate.getProlepticWeek() * 8L + endDate.getDayOfWeek();
    return (endWeek - startWeek) / 8L;
  }

  @Override
  long monthsUntil(AbstractDate end) {
    Uniform30DayDate date = Uniform30DayDate.from(end);
    long monthStart = this.getProlepticMonth() * 64L + this.getDayOfMonth();
    long monthEnd = date.getProlepticMonth() * 64L + date.getDayOfMonth();
    return (monthEnd - monthStart) / 64L;
  }

  @Override
  public long toEpochDay() {
    return (long) (this.prolepticYear - 1) * DAYS_IN_YEAR + this.dayOfYear - DAYS_0001_TO_1970 - 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    Uniform30DayDate that = (Uniform30DayDate) o;
    return prolepticYear == that.prolepticYear && month == that.month && day == that.day && dayOfYear == that.dayOfYear;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), prolepticYear, month, day, dayOfYear);
  }

  @Override
  public String toString() {
    return getChronology().toString() + ' ' + getEra() + ' ' + getYearOfEra()
        + (this.month < 10 && this.month > 0 ? "/0" : '/') + this.month + (this.day < 10 ? "/0" : '/') + this.day;
  }

}
