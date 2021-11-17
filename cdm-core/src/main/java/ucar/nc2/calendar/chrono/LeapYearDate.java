/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.calendar.chrono;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.time.zone.ZoneRules;
import java.util.Objects;

import static java.time.temporal.ChronoField.YEAR;
import static ucar.nc2.calendar.chrono.LeapYearChronology.DAY_OF_MONTH_RANGE;
import static ucar.nc2.calendar.chrono.LeapYearChronology.MONTH_OF_YEAR_RANGE;
import static ucar.nc2.calendar.chrono.LeapYearChronology.YEAR_RANGE;

import static ucar.nc2.calendar.chrono.LeapYearChronology.DAYS_IN_WEEK;
import static ucar.nc2.calendar.chrono.LeapYearChronology.MONTHS_IN_YEAR;
import static ucar.nc2.calendar.chrono.LeapYearChronology.WEEKS_IN_MONTH;

/**
 * A date in the CF all_leap or CF noleap calendar system.
 * Adapted from org.threeten.extra.chrono.Symmetry454Date.
 */
public final class LeapYearDate extends AbstractDate implements Temporal, TemporalAdjuster, ChronoLocalDate {

  static final int SECONDS_PER_DAY = 3600 * 24;

  //////////////////////////////////////////////////////////////////////////////////////

  private final LeapYearChronology chronology;
  private final int prolepticYear;
  private final short month;
  private final short day;

  /**
   * Obtains the current date from the specified clock.
   * <p>
   * This will query the specified clock to obtain the current date - today.
   * Using this method allows the use of an alternate clock for testing.
   * The alternate clock may be introduced using {@link Clock dependency injection}.
   *
   * @param clock the clock to use, not null
   * @return the current date, not null
   */
  public static LeapYearDate now(LeapYearChronology chronology, Clock clock) {
    Objects.requireNonNull(clock, "clock");
    final Instant now = clock.instant(); // called once
    return ofInstant(chronology, now, clock.getZone());
  }

  /**
   * Obtains an instance of {@code LeapYearDate} from a year, month and day.
   * <p>
   * This returns a {@code LeapYearDate} with the specified year, month and day-of-month.
   * The day must be valid for the year and month, otherwise an exception will be thrown.
   *
   * @param year the year to represent, from MIN_YEAR to MAX_YEAR
   * @param month the month-of-year to represent, from 1 (January) to 12 (December)
   * @param dayOfMonth the day-of-month to represent, from 1 to 31
   * @return the local date, not null
   * @throws DateTimeException if the value of any field is out of range,
   *         or if the day-of-month is invalid for the month-year
   */
  public static LeapYearDate of(LeapYearChronology chronology, int year, int month, int dayOfMonth) {
    return create(chronology, year, month, dayOfMonth);
  }

  /**
   * Obtains an instance of {@code LeapYearDate} from a year and day-of-year.
   * <p>
   * This returns a {@code LeapYearDate} with the specified year and day-of-year.
   * The day-of-year must be valid for the year, otherwise an exception will be thrown.
   *
   * @param year the year to represent, from MIN_YEAR to MAX_YEAR
   * @param dayOfYear the day-of-year to represent, from 1 to 366
   * @return the local date, not null
   * @throws DateTimeException if the value of any field is out of range,
   *         or if the day-of-year is invalid for the year
   */
  public static LeapYearDate ofYearDay(LeapYearChronology chronology, int year, int dayOfYear) {
    boolean leap = chronology.isLeapYear(year);
    if (dayOfYear == 366 && !leap) {
      throw new DateTimeException("Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
    }
    Month moy = Month.of((dayOfYear - 1) / 31 + 1);
    int monthEnd = moy.firstDayOfYear(leap) + moy.length(leap) - 1;
    if (dayOfYear > monthEnd) {
      moy = moy.plus(1);
    }
    int dom = dayOfYear - moy.firstDayOfYear(leap) + 1;
    return new LeapYearDate(chronology, year, moy.getValue(), dom);
  }

  /**
   * Obtains an instance of {@code LeapYearDate} from an {@code Instant} and zone ID.
   * <p>
   * This creates a local date based on the specified instant.
   * First, the offset from UTC/Greenwich is obtained using the zone ID and instant,
   * which is simple as there is only one valid offset for each instant.
   * Then, the instant and offset are used to calculate the local date.
   *
   * @param instant the instant to create the date from, not null
   * @param zone the time-zone, which may be an offset, not null
   * @return the local date, not null
   * @throws DateTimeException if the result exceeds the supported range
   * @since 9
   */
  public static LeapYearDate ofInstant(LeapYearChronology chronology, Instant instant, ZoneId zone) {
    Objects.requireNonNull(instant, "instant");
    Objects.requireNonNull(zone, "zone");
    ZoneRules rules = zone.getRules();
    ZoneOffset offset = rules.getOffset(instant);
    long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
    long localEpochDay = Math.floorDiv(localSecond, SECONDS_PER_DAY);
    return chronology.dateEpochDay(localEpochDay);
  }

  static LeapYearDate ofEpochDay(LeapYearChronology chronology, long epochDay) {
    chronology.epochDateRange().checkValidValue(epochDay, ChronoField.EPOCH_DAY);
    long zeroDay = epochDay + 1969L * chronology.daysInYear();
    long year = zeroDay / chronology.daysInYear();
    long doy = zeroDay % chronology.daysInYear();
    return ofYearDay(chronology, (int) year + 1, (int) doy + 1);
  }

  /**
   * Obtains an instance of {@code LeapYearDate} from a temporal object.
   * <p>
   * This obtains a LeapYearDate based on the specified temporal.
   * A {@code TemporalAccessor} represents an arbitrary set of date and time information,
   * which this factory converts to an instance of {@code LeapYearDate}.
   * <p>
   * 
   * @param temporal the temporal object to convert, not null
   * @return the LeapYearDateQuery, not null
   * @throws DateTimeException if unable to convert to a {@code LeapYearDateQuery}
   */
  public static LeapYearDate from(LeapYearChronology chronology, TemporalAccessor temporal) {
    Objects.requireNonNull(temporal, "temporal");
    if (temporal instanceof LeapYearDate) {
      return (LeapYearDate) temporal;
    }
    return ofEpochDay(chronology, temporal.getLong(ChronoField.EPOCH_DAY));
  }

  /**
   * Creates a local date from the year, month and day fields.
   *
   * @param prolepticYear the year to represent, validated from MIN_YEAR to MAX_YEAR
   * @param month the month-of-year to represent, from 1 to 12, validated
   * @param dayOfMonth the day-of-month to represent, validated from 1 to 31
   * @return the local date, not null
   * @throws DateTimeException if the day-of-month is invalid for the month-year
   */
  private static LeapYearDate create(LeapYearChronology chronology, int prolepticYear, int month, int dayOfMonth) {
    YEAR_RANGE.checkValidValue(prolepticYear, ChronoField.YEAR_OF_ERA);
    MONTH_OF_YEAR_RANGE.checkValidValue(month, ChronoField.MONTH_OF_YEAR);
    DAY_OF_MONTH_RANGE.checkValidValue(dayOfMonth, ChronoField.DAY_OF_MONTH);

    if (dayOfMonth > 28) {
      int dom = 31;
      switch (month) {
        case 2:
          dom = (chronology.isLeapYear(prolepticYear) ? 29 : 28);
          break;
        case 4:
        case 6:
        case 9:
        case 11:
          dom = 30;
          break;
      }
      if (dayOfMonth > dom) {
        if (dayOfMonth == 29) {
          throw new DateTimeException("Invalid date 'February 29' as '" + prolepticYear + "' is not a leap year");
        } else {
          throw new DateTimeException("Invalid date '" + Month.of(month).name() + " " + dayOfMonth + "'");
        }
      }
    }
    return new LeapYearDate(chronology, prolepticYear, month, dayOfMonth);
  }

  /**
   * Constructor, previously validated.
   *
   * @param prolepticYear the year to represent, from MIN_YEAR to MAX_YEAR
   * @param month the month-of-year to represent, not null
   * @param dayOfMonth the day-of-month to represent, valid for year-month, from 1 to 31
   */
  private LeapYearDate(LeapYearChronology chronology, int prolepticYear, int month, int dayOfMonth) {
    this.chronology = chronology;
    this.prolepticYear = prolepticYear;
    this.month = (short) month;
    this.day = (short) dayOfMonth;
  }

  /**
   * Gets the range of valid values for the specified field.
   * <p>
   * The range object expresses the minimum and maximum valid values for a field.
   * This date is used to enhance the accuracy of the returned range.
   * If it is not possible to return the range, because the field is not supported
   * or for some other reason, an exception is thrown.
   * <p>
   * If the field is a {@link ChronoField} then the query is implemented here.
   * The {@link #isSupported(TemporalField) supported fields} will return
   * appropriate range instances.
   * All other {@code ChronoField} instances will throw an {@code UnsupportedTemporalTypeException}.
   * <p>
   * If the field is not a {@code ChronoField}, then the result of this method
   * is obtained by invoking {@code TemporalField.rangeRefinedBy(TemporalAccessor)}
   * passing {@code this} as the argument.
   * Whether the range can be obtained is determined by the field.
   *
   * @param field the field to query the range for, not null
   * @return the range of valid values for the field, not null
   * @throws DateTimeException if the range for the field cannot be obtained
   * @throws UnsupportedTemporalTypeException if the field is not supported
   */
  @Override
  public ValueRange range(TemporalField field) {
    if (field instanceof ChronoField) {
      ChronoField f = (ChronoField) field;
      if (f.isDateBased()) {
        switch (f) {
          case DAY_OF_MONTH:
            return ValueRange.of(1, lengthOfMonth());
          case DAY_OF_YEAR:
            return ValueRange.of(1, lengthOfYear());
          case ALIGNED_WEEK_OF_MONTH:
            return ValueRange.of(1, getMonthOfYearField() == Month.FEBRUARY && !isLeapYear() ? 4 : 5);
          case YEAR_OF_ERA:
            return (getProlepticYear() <= 0 ? ValueRange.of(1, Year.MAX_VALUE + 1) : ValueRange.of(1, Year.MAX_VALUE));
        }
        return field.range();
      }
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.rangeRefinedBy(this);
  }

  @Override
  public LeapYearChronology getChronology() {
    return chronology;
  }

  /** Do not use Eras with this Chronology */
  @Override
  public IsoEra getEra() {
    return (prolepticYear >= 1 ? IsoEra.CE : IsoEra.BCE);
  }

  /**
   * Gets the year field.
   * <p>
   * This method returns the primitive {@code int} value for the year.
   * <p>
   * The year returned by this method is proleptic as per {@code get(YEAR)}.
   * To obtain the year-of-era, use {@code get(YEAR_OF_ERA)}.
   *
   * @return the year, from MIN_YEAR to MAX_YEAR
   */
  @Override
  public int getProlepticYear() {
    return prolepticYear;
  }

  /**
   * Gets the month-of-year field from 1 to 12.
   * <p>
   * This method returns the month as an {@code int} from 1 to 12.
   * Application code is frequently clearer if the enum {@link Month}
   * is used by calling {@link #getMonthOfYearField()}.
   *
   * @return the month-of-year, from 1 to 12
   * @see #getMonthOfYearField()
   */
  @Override
  public int getMonth() {
    return month;
  }

  /**
   * Gets the month-of-year field using the {@code Month} enum.
   * <p>
   * This method returns the enum {@link Month} for the month.
   * This avoids confusion as to what {@code int} values mean.
   * If you need access to the primitive {@code int} value then the enum
   * provides the {@link Month#getValue() int value}.
   *
   * @return the month-of-year, not null
   * @see #getMonth()
   */
  public Month getMonthOfYearField() {
    return Month.of(month);
  }

  /**
   * Gets the day-of-month field.
   * <p>
   * This method returns the primitive {@code int} value for the day-of-month.
   *
   * @return the day-of-month, from 1 to 31
   */
  public int getDayOfMonth() {
    return day;
  }

  /**
   * Gets the day-of-year field.
   * <p>
   * This method returns the primitive {@code int} value for the day-of-year.
   *
   * @return the day-of-year, from 1 to 365, or 366 in a leap year
   */
  public int getDayOfYear() {
    return getMonthOfYearField().firstDayOfYear(isLeapYear()) + day - 1;
  }

  /**
   * Gets the day-of-week field, which is an enum {@code DayOfWeek}.
   * <p>
   * This method returns the enum {@link DayOfWeek} for the day-of-week.
   * This avoids confusion as to what {@code int} values mean.
   * If you need access to the primitive {@code int} value then the enum
   * provides the {@link DayOfWeek#getValue() int value}.
   * <p>
   * Additional information can be obtained from the {@code DayOfWeek}.
   * This includes textual names of the values.
   *
   * @return the day-of-week, not null
   */
  public DayOfWeek getDayOfWeekField() {
    int dow0 = Math.floorMod(toEpochDay() + 3, 7);
    return DayOfWeek.of(dow0 + 1);
  }

  @Override
  public boolean isLeapYear() {
    return chronology.isLeapYear(prolepticYear);
  }

  /** Returns the length of the month of this date. */
  @Override
  public int lengthOfMonth() {
    switch (month) {
      case 2:
        return (isLeapYear() ? 29 : 28);
      case 4:
      case 6:
      case 9:
      case 11:
        return 30;
      default:
        return 31;
    }
  }

  /** Returns the length of the year represented by this date. */
  @Override
  public int lengthOfYear() {
    return (isLeapYear() ? 366 : 365);
  }

  // TODO not sure all of with() is needed - see uniform30 and AbstractDate
  @Override
  public LeapYearDate with(TemporalAdjuster adjuster) {
    // optimizations
    if (adjuster instanceof LeapYearDate) {
      return (LeapYearDate) adjuster;
    }
    return (LeapYearDate) adjuster.adjustInto(this);
  }

  @Override
  public LeapYearDate with(TemporalField field, long newValue) {
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
          return create(chronology, prolepticYear, month, nval);
        default:
          break;
      }
    }
    return (LeapYearDate) super.with(field, newValue);
  }

  /**
   * Resolves the date, resolving days past the end of month.
   *
   * @param year the year to represent, validated from MIN_YEAR to MAX_YEAR
   * @param month the month-of-year to represent, validated from 1 to 12
   * @param day the day-of-month to represent, validated from 1 to 31
   * @return the resolved date, not null
   */
  private LeapYearDate resolvePreviousValid(int year, int month, int day) {
    switch (month) {
      case 2:
        day = Math.min(day, chronology.isLeapYear(year) ? 29 : 28);
        break;
      case 4:
      case 6:
      case 9:
      case 11:
        day = Math.min(day, 30);
        break;
    }
    return new LeapYearDate(chronology, year, month, day);
  }

  @Override
  ValueRange rangeAlignedWeekOfMonth() {
    return ValueRange.of(1, WEEKS_IN_MONTH);
  }

  @Override
  LeapYearDate resolvePrevious(int prolepticYear, int newMonth, int dayOfMonth) {
    int monthR = Math.min(month, MONTHS_IN_YEAR);
    int dayR = Math.min(day, chronology.daysInMonth(monthR - 1));
    return create(this.chronology, prolepticYear, monthR, dayR);
  }

  @Override
  public LeapYearDate plus(TemporalAmount amount) {
    return (LeapYearDate) amount.addTo(this);
  }

  @Override
  public LeapYearDate plus(long amountToAdd, TemporalUnit unit) {
    return (LeapYearDate) super.plus(amountToAdd, unit);
  }

  @Override
  public LeapYearDate minus(TemporalAmount amount) {
    return (LeapYearDate) amount.subtractFrom(this);
  }

  @Override
  public LeapYearDate minus(long amountToSubtract, TemporalUnit unit) {
    return (LeapYearDate) super.minus(amountToSubtract, unit);
  }

  @Override
  public long until(Temporal endExclusive, TemporalUnit unit) {
    return until(LeapYearDate.from(chronology, endExclusive), unit);
  }

  @Override
  public Period until(ChronoLocalDate endDateExclusive) {
    LeapYearDate end = LeapYearDate.from(chronology, endDateExclusive);
    long totalMonths = end.getProlepticMonth() - this.getProlepticMonth(); // safe
    int days = end.day - this.day;
    if (totalMonths > 0 && days < 0) {
      totalMonths--;
      LeapYearDate calcDate = this.plusMonths(totalMonths);
      days = (int) (end.toEpochDay() - calcDate.toEpochDay()); // safe
    } else if (totalMonths < 0 && days > 0) {
      totalMonths++;
      days -= end.lengthOfMonth();
    }
    long years = totalMonths / 12; // safe
    int months = (int) (totalMonths % 12); // safe
    return Period.of(Math.toIntExact(years), months, days);
  }

  @Override
  public LeapYearDate plusMonths(long monthsToAdd) {
    if (monthsToAdd == 0) {
      return this;
    }
    long monthCount = prolepticYear * 12L + (month - 1);
    long calcMonths = monthCount + monthsToAdd; // safe overflow
    int newYear = YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
    int newMonth = Math.floorMod(calcMonths, 12) + 1;
    return resolvePreviousValid(newYear, newMonth, day);
  }

  @Override // for covariant return type
  @SuppressWarnings("unchecked")
  public ChronoLocalDateTime<LeapYearDate> atTime(LocalTime localTime) {
    return (ChronoLocalDateTime<LeapYearDate>) super.atTime(localTime);
  }

  @Override
  public long toEpochDay() {
    return chronology.toEpochDay(this);
  }


  // -----------------------------------------------------------------------
  /**
   * Compares this date to another date.
   * <p>
   * The comparison is primarily based on the date, from earliest to latest.
   * It is "consistent with equals", as defined by {@link Comparable}.
   * <p>
   * If all the dates being compared are instances of {@code LeapYearDate},
   * then the comparison will be entirely based on the date.
   * If some dates being compared are in different chronologies, then the
   * chronology is also considered, see {@link java.time.chrono.ChronoLocalDate#compareTo}.
   *
   * @param other the other date to compare to, not null
   * @return the comparator value, negative if less, positive if greater
   */
  @Override // override for Javadoc and performance
  public int compareTo(ChronoLocalDate other) {
    if (other instanceof LeapYearDate) {
      return compareTo0((LeapYearDate) other);
    }
    return super.compareTo(other);
  }

  int compareTo0(LeapYearDate otherDate) {
    int cmp = (prolepticYear - otherDate.prolepticYear);
    if (cmp == 0) {
      cmp = (month - otherDate.month);
      if (cmp == 0) {
        cmp = (day - otherDate.day);
      }
    }
    return cmp;
  }

  @Override // override for Javadoc and performance
  public boolean isEqual(ChronoLocalDate other) {
    if (other instanceof LeapYearDate) {
      return compareTo0((LeapYearDate) other) == 0;
    }
    return super.isEqual(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    LeapYearDate that = (LeapYearDate) o;
    return prolepticYear == that.prolepticYear && month == that.month && day == that.day
        && chronology.equals(that.chronology);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), chronology, prolepticYear, month, day);
  }

  @Override
  public String toString() {
    int yearValue = prolepticYear;
    int monthValue = month;
    int dayValue = day;
    int absYear = Math.abs(yearValue);
    StringBuilder buf = new StringBuilder(10);
    if (absYear < 1000) {
      if (yearValue < 0) {
        buf.append(yearValue - 10000).deleteCharAt(1);
      } else {
        buf.append(yearValue + 10000).deleteCharAt(0);
      }
    } else {
      if (yearValue > 9999) {
        buf.append('+');
      }
      buf.append(yearValue);
    }
    return buf.append(monthValue < 10 ? "-0" : "-").append(monthValue).append(dayValue < 10 ? "-0" : "-")
        .append(dayValue).toString();
  }
}
