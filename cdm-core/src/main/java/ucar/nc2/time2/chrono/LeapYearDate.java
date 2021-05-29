/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.time2.chrono;

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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.time.zone.ZoneRules;
import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;
import static ucar.nc2.time2.chrono.Uniform30DayChronology.DAYS_IN_MONTH;
import static ucar.nc2.time2.chrono.Uniform30DayChronology.MONTHS_IN_YEAR;
import static ucar.nc2.time2.chrono.Uniform30DayChronology.WEEKS_IN_MONTH;

/**
 * A date in the CF all_leap or noleap calendar system.
 * Adapted from java.time.LocalDate
 * <p>
 * {@code LeapYearDate} is an immutable date-time object that represents a date,
 * often viewed as year-month-day. Other date fields, such as day-of-year,
 * day-of-week and week-of-year, can also be accessed.
 * For example, the value "2nd October 2007" can be stored in a {@code LeapYearDate}.
 * <p>
 * This class does not store or represent a time or time-zone.
 * Instead, it is a description of the date, as used for birthdays.
 * It cannot represent an instant on the time-line without additional information
 * such as an offset or time-zone.
 * <p>
 * The ISO-8601 calendar system is the modern civil calendar system used today
 * in most of the world. It is equivalent to the proleptic Gregorian calendar
 * system, in which today's rules for leap years are applied for all time.
 * For most applications written today, the ISO-8601 rules are entirely suitable.
 * However, any application that makes use of historical dates, and requires them
 * to be accurate will find the ISO-8601 approach unsuitable.
 *
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code LeapYearDate} may have unpredictable results and should be avoided.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec
 *           This class is immutable and thread-safe.
 *
 * @since 1.8
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

  // -----------------------------------------------------------------------
  /**
   * Obtains an instance of {@code LeapYearDate} from a year, month and day.
   * <p>
   * This returns a {@code LeapYearDate} with the specified year, month and day-of-month.
   * The day must be valid for the year and month, otherwise an exception will be thrown.
   *
   * @param prolepticYear the year to represent, from MIN_YEAR to MAX_YEAR
   * @param month the month-of-year to represent, not null
   * @param dayOfMonth the day-of-month to represent, from 1 to 31
   * @return the local date, not null
   * @throws DateTimeException if the value of any field is out of range,
   *         or if the day-of-month is invalid for the month-year
   */
  public static LeapYearDate of(LeapYearChronology chronology, int prolepticYear, Month month, int dayOfMonth) {
    YEAR.checkValidValue(prolepticYear);
    Objects.requireNonNull(month, "month");
    DAY_OF_MONTH.checkValidValue(dayOfMonth);
    return create(chronology, prolepticYear, month.getValue(), dayOfMonth);
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
    YEAR.checkValidValue(year);
    MONTH_OF_YEAR.checkValidValue(month);
    DAY_OF_MONTH.checkValidValue(dayOfMonth);
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
    YEAR.checkValidValue(year);
    DAY_OF_YEAR.checkValidValue(dayOfYear);
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

  // -----------------------------------------------------------------------
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
    if (temporal instanceof ChronoLocalDate) {
      ChronoLocalDate localDate = (ChronoLocalDate) temporal;
      // return LeapYearDate.ofEpochDay(localDate.getLong(ChronoField.EPOCH_DAY));
      return LeapYearDate.of(chronology, localDate.get(YEAR), localDate.get(MONTH_OF_YEAR),
          localDate.get(DAY_OF_MONTH));

    } else if (temporal instanceof ChronoLocalDateTime) {
      ChronoLocalDateTime localDateTime = (ChronoLocalDateTime) temporal;
      // LOOK maybe have to turn it back to instance and reparse ??
      return LeapYearDate.of(chronology, localDateTime.get(YEAR), localDateTime.get(MONTH_OF_YEAR),
          localDateTime.get(DAY_OF_MONTH));
    }
    throw new DateTimeException("Unable to obtain LeapYearDate from TemporalAccessor: " + temporal + " of type "
        + temporal.getClass().getName());
  }

  // -----------------------------------------------------------------------
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
   * Resolves the date, resolving days past the end of month.
   *
   * @param year the year to represent, validated from MIN_YEAR to MAX_YEAR
   * @param month the month-of-year to represent, validated from 1 to 12
   * @param day the day-of-month to represent, validated from 1 to 31
   * @return the resolved date, not null
   */
  private static LeapYearDate resolvePreviousValid(LeapYearChronology chronology, int year, int month, int day) {
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

  // -----------------------------------------------------------------------
  /**
   * Checks if the specified field is supported.
   * <p>
   * This checks if this date can be queried for the specified field.
   * If false, then calling the {@link #range(TemporalField) range},
   * {@link #get(TemporalField) get} and {@link #with(TemporalField, long)}
   * methods will throw an exception.
   * <p>
   * If the field is a {@link ChronoField} then the query is implemented here.
   * The supported fields are:
   * <ul>
   * <li>{@code DAY_OF_WEEK}
   * <li>{@code ALIGNED_DAY_OF_WEEK_IN_MONTH}
   * <li>{@code ALIGNED_DAY_OF_WEEK_IN_YEAR}
   * <li>{@code DAY_OF_MONTH}
   * <li>{@code DAY_OF_YEAR}
   * <li>{@code EPOCH_DAY}
   * <li>{@code ALIGNED_WEEK_OF_MONTH}
   * <li>{@code ALIGNED_WEEK_OF_YEAR}
   * <li>{@code MONTH_OF_YEAR}
   * <li>{@code PROLEPTIC_MONTH}
   * <li>{@code YEAR_OF_ERA}
   * <li>{@code YEAR}
   * <li>{@code ERA}
   * </ul>
   * All other {@code ChronoField} instances will return false.
   * <p>
   * If the field is not a {@code ChronoField}, then the result of this method
   * is obtained by invoking {@code TemporalField.isSupportedBy(TemporalAccessor)}
   * passing {@code this} as the argument.
   * Whether the field is supported is determined by the field.
   *
   * @param field the field to check, null returns false
   * @return true if the field is supported on this date, false if not
   */
  @Override // override for Javadoc
  public boolean isSupported(TemporalField field) {
    return super.isSupported(field);
  }

  /**
   * Checks if the specified unit is supported.
   * <p>
   * This checks if the specified unit can be added to, or subtracted from, this date.
   * If false, then calling the {@link #plus(long, TemporalUnit)} and
   * {@link #minus(long, TemporalUnit) minus} methods will throw an exception.
   * <p>
   * If the unit is a {@link ChronoUnit} then the query is implemented here.
   * The supported units are:
   * <ul>
   * <li>{@code DAYS}
   * <li>{@code WEEKS}
   * <li>{@code MONTHS}
   * <li>{@code YEARS}
   * <li>{@code DECADES}
   * <li>{@code CENTURIES}
   * <li>{@code MILLENNIA}
   * <li>{@code ERAS}
   * </ul>
   * All other {@code ChronoUnit} instances will return false.
   * <p>
   * If the unit is not a {@code ChronoUnit}, then the result of this method
   * is obtained by invoking {@code TemporalUnit.isSupportedBy(Temporal)}
   * passing {@code this} as the argument.
   * Whether the unit is supported is determined by the unit.
   *
   * @param unit the unit to check, null returns false
   * @return true if the unit can be added/subtracted, false if not
   */
  @Override // override for Javadoc
  public boolean isSupported(TemporalUnit unit) {
    return super.isSupported(unit);
  }

  // -----------------------------------------------------------------------
  /**
   * LOOK
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

  // ----------------------------------------------------------------------------------------
  // LOOK 30Day doesnt have any get or getLong or get0
  /**
   * Gets the value of the specified field from this date as an {@code int}.
   * <p>
   * This queries this date for the value of the specified field.
   * The returned value will always be within the valid range of values for the field.
   * If it is not possible to return the value, because the field is not supported
   * or for some other reason, an exception is thrown.
   * <p>
   * If the field is a {@link ChronoField} then the query is implemented here.
   * The {@link #isSupported(TemporalField) supported fields} will return valid
   * values based on this date, except {@code EPOCH_DAY} and {@code PROLEPTIC_MONTH}
   * which are too large to fit in an {@code int} and throw an {@code UnsupportedTemporalTypeException}.
   * All other {@code ChronoField} instances will throw an {@code UnsupportedTemporalTypeException}.
   * <p>
   * If the field is not a {@code ChronoField}, then the result of this method
   * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
   * passing {@code this} as the argument. Whether the value can be obtained,
   * and what the value represents, is determined by the field.
   *
   * @param field the field to get, not null
   * @return the value for the field
   * @throws DateTimeException if a value for the field cannot be obtained or
   *         the value is outside the range of valid values for the field
   * @throws UnsupportedTemporalTypeException if the field is not supported or
   *         the range of values exceeds an {@code int}
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override // override for Javadoc and performance
  public int get(TemporalField field) {
    if (field instanceof ChronoField) {
      return get0(field);
    }
    return super.get(field);
  }

  /**
   * Gets the value of the specified field from this date as a {@code long}.
   * <p>
   * This queries this date for the value of the specified field.
   * If it is not possible to return the value, because the field is not supported
   * or for some other reason, an exception is thrown.
   * <p>
   * If the field is a {@link ChronoField} then the query is implemented here.
   * The {@link #isSupported(TemporalField) supported fields} will return valid
   * values based on this date.
   * All other {@code ChronoField} instances will throw an {@code UnsupportedTemporalTypeException}.
   * <p>
   * If the field is not a {@code ChronoField}, then the result of this method
   * is obtained by invoking {@code TemporalField.getFrom(TemporalAccessor)}
   * passing {@code this} as the argument. Whether the value can be obtained,
   * and what the value represents, is determined by the field.
   *
   * @param field the field to get, not null
   * @return the value for the field
   * @throws DateTimeException if a value for the field cannot be obtained
   * @throws UnsupportedTemporalTypeException if the field is not supported
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public long getLong(TemporalField field) {
    if (field instanceof ChronoField) {
      if (field == EPOCH_DAY) {
        return toEpochDay();
      }
      if (field == PROLEPTIC_MONTH) {
        return getProlepticMonth();
      }
      return get0(field);
    }
    return field.getFrom(this);
  }

  private int get0(TemporalField field) {
    switch ((ChronoField) field) {
      case DAY_OF_WEEK:
        return getDayOfWeekField().getValue();
      case ALIGNED_DAY_OF_WEEK_IN_MONTH:
        return ((day - 1) % 7) + 1;
      case ALIGNED_DAY_OF_WEEK_IN_YEAR:
        return ((getDayOfYear() - 1) % 7) + 1;
      case DAY_OF_MONTH:
        return day;
      case DAY_OF_YEAR:
        return getDayOfYear();
      case EPOCH_DAY:
        throw new UnsupportedTemporalTypeException("Invalid field 'EpochDay' for get() method, use getLong() instead");
      case ALIGNED_WEEK_OF_MONTH:
        return ((day - 1) / 7) + 1;
      case ALIGNED_WEEK_OF_YEAR:
        return ((getDayOfYear() - 1) / 7) + 1;
      case MONTH_OF_YEAR:
        return month;
      case PROLEPTIC_MONTH:
        throw new UnsupportedTemporalTypeException(
            "Invalid field 'ProlepticMonth' for get() method, use getLong() instead");
      case YEAR_OF_ERA:
        return (prolepticYear >= 1 ? prolepticYear : 1 - prolepticYear);
      case YEAR:
        return prolepticYear;
      case ERA:
        return (prolepticYear >= 1 ? 1 : 0);
    }
    throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
  }


  // -----------------------------------------------------------------------
  /**
   * Gets the chronology of this date, which is the ISO calendar system.
   * <p>
   * The {@code Chronology} represents the calendar system in use.
   * The ISO-8601 calendar system is the modern civil calendar system used today
   * in most of the world. It is equivalent to the proleptic Gregorian calendar
   * system, in which today's rules for leap years are applied for all time.
   *
   * @return the ISO chronology, not null
   */
  @Override
  public LeapYearChronology getChronology() {
    return chronology;
  }

  /**
   * Gets the era applicable at this date.
   * <p>
   * The official ISO-8601 standard does not define eras, however {@code LeapYearChronology} does.
   * It defines two eras, 'CE' from year one onwards and 'BCE' from year zero backwards.
   * Since dates before the Julian-Gregorian cutover are not in line with history,
   * the cutover between 'BCE' and 'CE' is also not aligned with the commonly used
   * eras, often referred to using 'BC' and 'AD'.
   * <p>
   * Users of this class should typically ignore this method as it exists primarily
   * to fulfill the {@link ChronoLocalDate} contract where it is necessary to support
   * the Japanese calendar system.
   *
   * @return the IsoEra applicable at this date, not null
   */
  @Override // override for Javadoc
  public IsoEra getEra() {
    return (getProlepticYear() >= 1 ? IsoEra.CE : IsoEra.BCE);
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

  // -----------------------------------------------------------------------
  /**
   * Checks if the year is a leap year, according to the ISO proleptic
   * calendar system rules.
   * <p>
   * This method applies the current rules for leap years across the whole time-line.
   * In general, a year is a leap year if it is divisible by four without
   * remainder. However, years divisible by 100, are not leap years, with
   * the exception of years divisible by 400 which are.
   * <p>
   * For example, 1904 is a leap year it is divisible by 4.
   * 1900 was not a leap year as it is divisible by 100, however 2000 was a
   * leap year as it is divisible by 400.
   * <p>
   * The calculation is proleptic - applying the same rules into the far future and far past.
   * This is historically inaccurate, but is correct for the ISO-8601 standard.
   *
   * @return true if the year is leap, false otherwise
   */
  @Override // override for Javadoc and performance
  public boolean isLeapYear() {
    return chronology.isLeapYear(prolepticYear);
  }

  /**
   * Returns the length of the month represented by this date.
   * <p>
   * This returns the length of the month in days.
   * For example, a date in January would return 31.
   *
   * @return the length of the month in days
   */
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

  /**
   * Returns the length of the year represented by this date.
   * <p>
   * This returns the length of the year in days, either 365 or 366.
   *
   * @return 366 if the year is leap, 365 otherwise
   */
  @Override // override for Javadoc and performance
  public int lengthOfYear() {
    return (isLeapYear() ? 366 : 365);
  }

  // -----------------------------------------------------------------------
  // LOOK not sure all of with() is needed - see uniform30 and AbstractDate
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
      ChronoField f = (ChronoField) field;
      f.checkValidValue(newValue);
      switch (f) {
        case DAY_OF_WEEK:
          return plusDays(newValue - getDayOfWeekField().getValue());
        case ALIGNED_DAY_OF_WEEK_IN_MONTH:
          return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
        case ALIGNED_DAY_OF_WEEK_IN_YEAR:
          return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
        case DAY_OF_MONTH:
          return withDayOfMonth((int) newValue);
        case DAY_OF_YEAR:
          return withDayOfYear((int) newValue);
        case EPOCH_DAY:
          return chronology.dateEpochDay(newValue);
        case ALIGNED_WEEK_OF_MONTH:
          return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_MONTH));
        case ALIGNED_WEEK_OF_YEAR:
          return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_YEAR));
        case MONTH_OF_YEAR:
          return withMonth((int) newValue);
        case PROLEPTIC_MONTH:
          return plusMonths(newValue - getProlepticMonth());
        case YEAR_OF_ERA:
          return withYear((int) (prolepticYear >= 1 ? newValue : 1 - newValue));
        case YEAR:
          return withYear((int) newValue);
        case ERA:
          return (getLong(ERA) == newValue ? this : withYear(1 - prolepticYear));
      }
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }
    return field.adjustInto(this, newValue);
  }

  private LeapYearDate withYear(int year) {
    if (this.prolepticYear == year) {
      return this;
    }
    YEAR.checkValidValue(year);
    return resolvePreviousValid(chronology, year, month, day);
  }

  private LeapYearDate withMonth(int month) {
    if (this.month == month) {
      return this;
    }
    MONTH_OF_YEAR.checkValidValue(month);
    return resolvePreviousValid(chronology, prolepticYear, month, day);
  }

  private LeapYearDate withDayOfMonth(int dayOfMonth) {
    if (this.day == dayOfMonth) {
      return this;
    }
    return of(chronology, prolepticYear, month, dayOfMonth);
  }

  /**
   * Returns a copy of this {@code LeapYearDate} with the day-of-year altered.
   * <p>
   * If the resulting date is invalid, an exception is thrown.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param dayOfYear the day-of-year to set in the result, from 1 to 365-366
   * @return a {@code LeapYearDate} based on this date with the requested day, not null
   * @throws DateTimeException if the day-of-year value is invalid,
   *         or if the day-of-year is invalid for the year
   */
  public LeapYearDate withDayOfYear(int dayOfYear) {
    if (this.getDayOfYear() == dayOfYear) {
      return this;
    }
    return ofYearDay(chronology, prolepticYear, dayOfYear);
  }

  @Override
  ValueRange rangeAlignedWeekOfMonth() {
    // never invoked LOOK can we remove from superclass?
    return ValueRange.of(1, WEEKS_IN_MONTH);
  }

  @Override
  LeapYearDate resolvePrevious(int prolepticYear, int newMonth, int dayOfMonth) {
    int monthR = Math.min(month, MONTHS_IN_YEAR);
    int dayR = Math.min(day, DAYS_IN_MONTH);
    return create(this.chronology, prolepticYear, monthR, dayR);
  }

  // -----------------------------------------------------------------------
  // LOOK not sure all of plus() and minus() is needed - see uniform30 and AbstractDate

  @Override
  public LeapYearDate plus(TemporalAmount amountToAdd) {
    if (amountToAdd instanceof Period) {
      Period periodToAdd = (Period) amountToAdd;
      return plusMonths(periodToAdd.toTotalMonths()).plusDays(periodToAdd.getDays());
    }
    Objects.requireNonNull(amountToAdd, "amountToAdd");
    return (LeapYearDate) amountToAdd.addTo(this);
  }

  @Override
  public LeapYearDate plus(long amountToAdd, TemporalUnit unit) {
    if (unit instanceof ChronoUnit) {
      ChronoUnit f = (ChronoUnit) unit;
      switch (f) {
        case DAYS:
          return plusDays(amountToAdd);
        case WEEKS:
          return plusWeeks(amountToAdd);
        case MONTHS:
          return plusMonths(amountToAdd);
        case YEARS:
          return plusYears(amountToAdd);
        case DECADES:
          return plusYears(Math.multiplyExact(amountToAdd, 10));
        case CENTURIES:
          return plusYears(Math.multiplyExact(amountToAdd, 100));
        case MILLENNIA:
          return plusYears(Math.multiplyExact(amountToAdd, 1000));
        case ERAS:
          return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
      }
      throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
    return unit.addTo(this, amountToAdd);
  }

  public LeapYearDate plusYears(long yearsToAdd) {
    if (yearsToAdd == 0) {
      return this;
    }
    int newYear = YEAR.checkValidIntValue(prolepticYear + yearsToAdd); // safe overflow
    return resolvePreviousValid(chronology, newYear, month, day);
  }

  /**
   * Returns a copy of this {@code LeapYearDate} with the specified number of months added.
   * <p>
   * This method adds the specified amount to the months field in three steps:
   * <ol>
   * <li>Add the input months to the month-of-year field</li>
   * <li>Check if the resulting date would be invalid</li>
   * <li>Adjust the day-of-month to the last valid day if necessary</li>
   * </ol>
   * <p>
   * For example, 2007-03-31 plus one month would result in the invalid date
   * 2007-04-31. Instead of returning an invalid result, the last valid day
   * of the month, 2007-04-30, is selected instead.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param monthsToAdd the months to add, may be negative
   * @return a {@code LeapYearDate} based on this date with the months added, not null
   * @throws DateTimeException if the result exceeds the supported date range
   */
  public LeapYearDate plusMonths(long monthsToAdd) {
    if (monthsToAdd == 0) {
      return this;
    }
    long monthCount = prolepticYear * 12L + (month - 1);
    long calcMonths = monthCount + monthsToAdd; // safe overflow
    int newYear = YEAR.checkValidIntValue(Math.floorDiv(calcMonths, 12));
    int newMonth = Math.floorMod(calcMonths, 12) + 1;
    return resolvePreviousValid(chronology, newYear, newMonth, day);
  }

  /**
   * Returns a copy of this {@code LeapYearDate} with the specified number of weeks added.
   * <p>
   * This method adds the specified amount in weeks to the days field incrementing
   * the month and year fields as necessary to ensure the result remains valid.
   * The result is only invalid if the maximum/minimum year is exceeded.
   * <p>
   * For example, 2008-12-31 plus one week would result in 2009-01-07.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param weeksToAdd the weeks to add, may be negative
   * @return a {@code LeapYearDate} based on this date with the weeks added, not null
   * @throws DateTimeException if the result exceeds the supported date range
   */
  public LeapYearDate plusWeeks(long weeksToAdd) {
    return plusDays(Math.multiplyExact(weeksToAdd, 7));
  }

  /**
   * Returns a copy of this {@code LeapYearDate} with the specified number of days added.
   * <p>
   * This method adds the specified amount to the days field incrementing the
   * month and year fields as necessary to ensure the result remains valid.
   * The result is only invalid if the maximum/minimum year is exceeded.
   * <p>
   * For example, 2008-12-31 plus one day would result in 2009-01-01.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param daysToAdd the days to add, may be negative
   * @return a {@code LeapYearDate} based on this date with the days added, not null
   * @throws DateTimeException if the result exceeds the supported date range
   */
  public LeapYearDate plusDays(long daysToAdd) {
    if (daysToAdd == 0) {
      return this;
    }
    long dom = day + daysToAdd;
    if (dom > 0) {
      if (dom <= 28) {
        return new LeapYearDate(this.chronology, prolepticYear, month, (int) dom);
      } else if (dom <= 59) { // 59th Jan is 28th Feb, 59th Feb is 31st Mar
        long monthLen = lengthOfMonth();
        if (dom <= monthLen) {
          return new LeapYearDate(this.chronology, prolepticYear, month, (int) dom);
        } else if (month < 12) {
          return new LeapYearDate(this.chronology, prolepticYear, month + 1, (int) (dom - monthLen));
        } else {
          YEAR.checkValidValue(prolepticYear + 1);
          return new LeapYearDate(this.chronology, prolepticYear + 1, 1, (int) (dom - monthLen));
        }
      }
    }

    long mjDay = Math.addExact(toEpochDay(), daysToAdd);
    return chronology.dateEpochDay(mjDay);
  }

  // -----------------------------------------------------------------------
  /**
   * Returns a copy of this date with the specified amount subtracted.
   * <p>
   * This returns a {@code LeapYearDate}, based on this one, with the specified amount subtracted.
   * The amount is typically {@link Period} but may be any other type implementing
   * the {@link TemporalAmount} interface.
   * <p>
   * The calculation is delegated to the amount object by calling
   * {@link TemporalAmount#subtractFrom(Temporal)}. The amount implementation is free
   * to implement the subtraction in any way it wishes, however it typically
   * calls back to {@link #minus(long, TemporalUnit)}. Consult the documentation
   * of the amount implementation to determine if it can be successfully subtracted.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param amountToSubtract the amount to subtract, not null
   * @return a {@code LeapYearDate} based on this date with the subtraction made, not null
   * @throws DateTimeException if the subtraction cannot be made
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public LeapYearDate minus(TemporalAmount amountToSubtract) {
    if (amountToSubtract instanceof Period) {
      Period periodToSubtract = (Period) amountToSubtract;
      return minusMonths(periodToSubtract.toTotalMonths()).minusDays(periodToSubtract.getDays());
    }
    Objects.requireNonNull(amountToSubtract, "amountToSubtract");
    return (LeapYearDate) amountToSubtract.subtractFrom(this);
  }

  /**
   * Returns a copy of this date with the specified amount subtracted.
   * <p>
   * This returns a {@code LeapYearDate}, based on this one, with the amount
   * in terms of the unit subtracted. If it is not possible to subtract the amount,
   * because the unit is not supported or for some other reason, an exception is thrown.
   * <p>
   * This method is equivalent to {@link #plus(long, TemporalUnit)} with the amount negated.
   * See that method for a full description of how addition, and thus subtraction, works.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param amountToSubtract the amount of the unit to subtract from the result, may be negative
   * @param unit the unit of the amount to subtract, not null
   * @return a {@code LeapYearDate} based on this date with the specified amount subtracted, not null
   * @throws DateTimeException if the subtraction cannot be made
   * @throws UnsupportedTemporalTypeException if the unit is not supported
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public LeapYearDate minus(long amountToSubtract, TemporalUnit unit) {
    return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
        : plus(-amountToSubtract, unit));
  }

  // -----------------------------------------------------------------------
  /**
   * Returns a copy of this {@code LeapYearDate} with the specified number of years subtracted.
   * <p>
   * This method subtracts the specified amount from the years field in three steps:
   * <ol>
   * <li>Subtract the input years from the year field</li>
   * <li>Check if the resulting date would be invalid</li>
   * <li>Adjust the day-of-month to the last valid day if necessary</li>
   * </ol>
   * <p>
   * For example, 2008-02-29 (leap year) minus one year would result in the
   * invalid date 2007-02-29 (standard year). Instead of returning an invalid
   * result, the last valid day of the month, 2007-02-28, is selected instead.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param yearsToSubtract the years to subtract, may be negative
   * @return a {@code LeapYearDate} based on this date with the years subtracted, not null
   * @throws DateTimeException if the result exceeds the supported date range
   */
  public LeapYearDate minusYears(long yearsToSubtract) {
    return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract));
  }

  /**
   * Returns a copy of this {@code LeapYearDate} with the specified number of months subtracted.
   * <p>
   * This method subtracts the specified amount from the months field in three steps:
   * <ol>
   * <li>Subtract the input months from the month-of-year field</li>
   * <li>Check if the resulting date would be invalid</li>
   * <li>Adjust the day-of-month to the last valid day if necessary</li>
   * </ol>
   * <p>
   * For example, 2007-03-31 minus one month would result in the invalid date
   * 2007-02-31. Instead of returning an invalid result, the last valid day
   * of the month, 2007-02-28, is selected instead.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param monthsToSubtract the months to subtract, may be negative
   * @return a {@code LeapYearDate} based on this date with the months subtracted, not null
   * @throws DateTimeException if the result exceeds the supported date range
   */
  public LeapYearDate minusMonths(long monthsToSubtract) {
    return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
        : plusMonths(-monthsToSubtract));
  }

  /**
   * Returns a copy of this {@code LeapYearDate} with the specified number of weeks subtracted.
   * <p>
   * This method subtracts the specified amount in weeks from the days field decrementing
   * the month and year fields as necessary to ensure the result remains valid.
   * The result is only invalid if the maximum/minimum year is exceeded.
   * <p>
   * For example, 2009-01-07 minus one week would result in 2008-12-31.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param weeksToSubtract the weeks to subtract, may be negative
   * @return a {@code LeapYearDate} based on this date with the weeks subtracted, not null
   * @throws DateTimeException if the result exceeds the supported date range
   */
  public LeapYearDate minusWeeks(long weeksToSubtract) {
    return (weeksToSubtract == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeksToSubtract));
  }

  /**
   * Returns a copy of this {@code LeapYearDate} with the specified number of days subtracted.
   * <p>
   * This method subtracts the specified amount from the days field decrementing the
   * month and year fields as necessary to ensure the result remains valid.
   * The result is only invalid if the maximum/minimum year is exceeded.
   * <p>
   * For example, 2009-01-01 minus one day would result in 2008-12-31.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param daysToSubtract the days to subtract, may be negative
   * @return a {@code LeapYearDate} based on this date with the days subtracted, not null
   * @throws DateTimeException if the result exceeds the supported date range
   */
  public LeapYearDate minusDays(long daysToSubtract) {
    return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
  }

  // -----------------------------------------------------------------------
  /**
   * Queries this date using the specified query.
   * <p>
   * This queries this date using the specified query strategy object.
   * The {@code TemporalQuery} object defines the logic to be used to
   * obtain the result. Read the documentation of the query to understand
   * what the result of this method will be.
   * <p>
   * The result of this method is obtained by invoking the
   * {@link TemporalQuery#queryFrom(TemporalAccessor)} method on the
   * specified query passing {@code this} as the argument.
   *
   * @param <R> the type of the result
   * @param query the query to invoke, not null
   * @return the query result, null may be returned (defined by the query)
   * @throws DateTimeException if unable to query (defined by the query)
   * @throws ArithmeticException if numeric overflow occurs (defined by the query)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <R> R query(TemporalQuery<R> query) {
    if (query == TemporalQueries.localDate()) {
      return (R) this;
    }
    return super.query(query);
  }

  /**
   * Adjusts the specified temporal object to have the same date as this object.
   * <p>
   * This returns a temporal object of the same observable type as the input
   * with the date changed to be the same as this.
   * <p>
   * The adjustment is equivalent to using {@link Temporal#with(TemporalField, long)}
   * passing {@link ChronoField#EPOCH_DAY} as the field.
   * <p>
   * In most cases, it is clearer to reverse the calling pattern by using
   * {@link Temporal#with(TemporalAdjuster)}:
   * 
   * <pre>
   * // these two lines are equivalent, but the second approach is recommended
   * temporal = thisLocalDate.adjustInto(temporal);
   * temporal = temporal.with(thisLocalDate);
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param temporal the target object to be adjusted, not null
   * @return the adjusted object, not null
   * @throws DateTimeException if unable to make the adjustment
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override // override for Javadoc
  public Temporal adjustInto(Temporal temporal) {
    return super.adjustInto(temporal);
  }

  @Override
  public long until(Temporal endExclusive, TemporalUnit unit) {
    return until(LeapYearDate.from(chronology, endExclusive), unit);
  }

  /*
   * @Override
   * public long until(Temporal endExclusive, TemporalUnit unit) {
   * LeapYearDate end = LeapYearDate.from(chronology, endExclusive);
   * if (unit instanceof ChronoUnit) {
   * switch ((ChronoUnit) unit) {
   * case DAYS: return daysUntil(end);
   * case WEEKS: return daysUntil(end) / 7;
   * case MONTHS: return monthsUntil(end);
   * case YEARS: return monthsUntil(end) / 12;
   * case DECADES: return monthsUntil(end) / 120;
   * case CENTURIES: return monthsUntil(end) / 1200;
   * case MILLENNIA: return monthsUntil(end) / 12000;
   * case ERAS: return end.getLong(ERA) - getLong(ERA);
   * }
   * throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
   * }
   * return unit.between(this, end);
   * }
   */

  long daysUntil(LeapYearDate end) {
    return end.toEpochDay() - toEpochDay(); // no overflow
  }

  private long monthsUntil(LeapYearDate end) {
    long packed1 = getProlepticMonth() * 32L + getDayOfMonth(); // no overflow
    long packed2 = end.getProlepticMonth() * 32L + end.getDayOfMonth(); // no overflow
    return (packed2 - packed1) / 32;
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

  /**
   * Returns a sequential ordered stream of dates. The returned stream starts from this date
   * (inclusive) and goes to {@code endExclusive} (exclusive) by an incremental step of 1 day.
   * <p>
   * This method is equivalent to {@code datesUntil(endExclusive, Period.ofDays(1))}.
   *
   * @param endExclusive the end date, exclusive, not null
   * @return a sequential {@code Stream} for the range of {@code LeapYearDate} values
   * @throws IllegalArgumentException if end date is before this date
   * @since 9
   */
  public Stream<LeapYearDate> datesUntil(LeapYearDate endExclusive) {
    long end = endExclusive.toEpochDay();
    long start = toEpochDay();
    if (end < start) {
      throw new IllegalArgumentException(endExclusive + " < " + this);
    }
    return LongStream.range(start, end).mapToObj(wtf -> chronology.dateEpochDay(wtf));
  }

  /**
   * Returns a sequential ordered stream of dates by given incremental step. The returned stream
   * starts from this date (inclusive) and goes to {@code endExclusive} (exclusive).
   * <p>
   * The n-th date which appears in the stream is equal to {@code this.plus(step.multipliedBy(n))}
   * (but the result of step multiplication never overflows). For example, if this date is
   * {@code 2015-01-31}, the end date is {@code 2015-05-01} and the step is 1 month, then the
   * stream contains {@code 2015-01-31}, {@code 2015-02-28}, {@code 2015-03-31}, and
   * {@code 2015-04-30}.
   *
   * @param endExclusive the end date, exclusive, not null
   * @param step the non-zero, non-negative {@code Period} which represents the step.
   * @return a sequential {@code Stream} for the range of {@code LeapYearDate} values
   * @throws IllegalArgumentException if step is zero, or {@code step.getDays()} and
   *         {@code step.toTotalMonths()} have opposite sign, or end date is before this date
   *         and step is positive, or end date is after this date and step is negative
   * @since 9
   */
  public Stream<LeapYearDate> datesUntil(LeapYearDate endExclusive, Period step) {
    if (step.isZero()) {
      throw new IllegalArgumentException("step is zero");
    }
    long end = endExclusive.toEpochDay();
    long start = toEpochDay();
    long until = end - start;
    long months = step.toTotalMonths();
    long days = step.getDays();
    if ((months < 0 && days > 0) || (months > 0 && days < 0)) {
      throw new IllegalArgumentException("period months and days are of opposite sign");
    }
    if (until == 0) {
      return Stream.empty();
    }
    int sign = months > 0 || days > 0 ? 1 : -1;
    if (sign < 0 ^ until < 0) {
      throw new IllegalArgumentException(endExclusive + (sign < 0 ? " > " : " < ") + this);
    }
    if (months == 0) {
      long steps = (until - sign) / days; // non-negative
      return LongStream.rangeClosed(0, steps).mapToObj(n -> chronology.dateEpochDay(start + n * days));
    }
    // 48699/1600 = 365.2425/12, no overflow, non-negative result
    long steps = until * 1600 / (months * 48699 + days * 1600) + 1;
    long addMonths = months * steps;
    long addDays = days * steps;
    long maxAddMonths = months > 0 ? chronology.MAX_DATE.getProlepticMonth() - getProlepticMonth()
        : getProlepticMonth() - chronology.MIN_DATE.getProlepticMonth();
    // adjust steps estimation
    if (addMonths * sign > maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
      steps--;
      addMonths -= months;
      addDays -= days;
      if (addMonths * sign > maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
        steps--;
      }
    }
    return LongStream.rangeClosed(0, steps).mapToObj(n -> this.plusMonths(months * n).plusDays(days * n));
  }

  /**
   * Formats this date using the specified formatter.
   * <p>
   * This date will be passed to the formatter to produce a string.
   *
   * @param formatter the formatter to use, not null
   * @return the formatted date string, not null
   * @throws DateTimeException if an error occurs during printing
   */
  @Override // override for Javadoc and performance
  public String format(DateTimeFormatter formatter) {
    Objects.requireNonNull(formatter, "formatter");
    return formatter.format(this);
  }

  @Override // for covariant return type
  @SuppressWarnings("unchecked")
  public ChronoLocalDateTime<LeapYearDate> atTime(LocalTime localTime) {
    return (ChronoLocalDateTime<LeapYearDate>) super.atTime(localTime);
  }

  /**
   * Combines this date with a time to create a {@code LocalDateTime}.
   * <p>
   * This returns a {@code LocalDateTime} formed from this date at the
   * specified hour and minute.
   * The seconds and nanosecond fields will be set to zero.
   * The individual time fields must be within their valid range.
   * All possible combinations of date and time are valid.
   *
   * @param hour the hour-of-day to use, from 0 to 23
   * @param minute the minute-of-hour to use, from 0 to 59
   * @return the local date-time formed from this date and the specified time, not null
   * @throws DateTimeException if the value of any field is out of range
   */
  public ChronoLocalDateTime<LeapYearDate> atTime(int hour, int minute) {
    return atTime(LocalTime.of(hour, minute));
  }

  /**
   * Combines this date with a time to create a {@code LocalDateTime}.
   * <p>
   * This returns a {@code LocalDateTime} formed from this date at the
   * specified hour, minute and second.
   * The nanosecond field will be set to zero.
   * The individual time fields must be within their valid range.
   * All possible combinations of date and time are valid.
   *
   * @param hour the hour-of-day to use, from 0 to 23
   * @param minute the minute-of-hour to use, from 0 to 59
   * @param second the second-of-minute to represent, from 0 to 59
   * @return the local date-time formed from this date and the specified time, not null
   * @throws DateTimeException if the value of any field is out of range
   */
  public ChronoLocalDateTime<LeapYearDate> atTime(int hour, int minute, int second) {
    return atTime(LocalTime.of(hour, minute, second));
  }

  /**
   * Combines this date with a time to create a {@code LocalDateTime}.
   * <p>
   * This returns a {@code LocalDateTime} formed from this date at the
   * specified hour, minute, second and nanosecond.
   * The individual time fields must be within their valid range.
   * All possible combinations of date and time are valid.
   *
   * @param hour the hour-of-day to use, from 0 to 23
   * @param minute the minute-of-hour to use, from 0 to 59
   * @param second the second-of-minute to represent, from 0 to 59
   * @param nanoOfSecond the nano-of-second to represent, from 0 to 999,999,999
   * @return the local date-time formed from this date and the specified time, not null
   * @throws DateTimeException if the value of any field is out of range
   */
  public ChronoLocalDateTime<LeapYearDate> atTime(int hour, int minute, int second, int nanoOfSecond) {
    return atTime(LocalTime.of(hour, minute, second, nanoOfSecond));
  }

  /*
   * Combines this date with an offset time to create an {@code OffsetDateTime}.
   * <p>
   * This returns an {@code OffsetDateTime} formed from this date at the specified time.
   * All possible combinations of date and time are valid.
   *
   * @param time the time to combine with, not null
   * 
   * @return the offset date-time formed from this date and the specified time, not null
   *
   * public OffsetDateTime atTime(OffsetTime time) {
   * return OffsetDateTime.of(LocalDateTime.of(this, time.toLocalTime()), time.getOffset());
   * }
   * 
   * /*
   * Combines this date with the time of midnight to create a {@code LocalDateTime}
   * at the start of this date.
   * <p>
   * This returns a {@code LocalDateTime} formed from this date at the time of
   * midnight, 00:00, at the start of this date.
   *
   * @return the local date-time of midnight at the start of this date, not null
   *
   * public ChronoLocalDateTime<LeapYearDate> atStartOfDay() {
   * return LocalDateTime.of(this, LocalTime.MIDNIGHT);
   * }
   * 
   * /*
   * Returns a zoned date-time from this date at the earliest valid time according
   * to the rules in the time-zone.
   * <p>
   * Time-zone rules, such as daylight savings, mean that not every local date-time
   * is valid for the specified zone, thus the local date-time may not be midnight.
   * <p>
   * In most cases, there is only one valid offset for a local date-time.
   * In the case of an overlap, there are two valid offsets, and the earlier one is used,
   * corresponding to the first occurrence of midnight on the date.
   * In the case of a gap, the zoned date-time will represent the instant just after the gap.
   * <p>
   * If the zone ID is a {@link ZoneOffset}, then the result always has a time of midnight.
   * <p>
   * To convert to a specific time in a given time-zone call {@link #atTime(LocalTime)}
   * followed by {@link LocalDateTime#atZone(ZoneId)}.
   *
   * @param zone the zone ID to use, not null
   * 
   * @return the zoned date-time formed from this date and the earliest valid time for the zone, not null
   *
   * public ZonedDateTime atStartOfDay(ZoneId zone) {
   * Objects.requireNonNull(zone, "zone");
   * // need to handle case where there is a gap from 11:30 to 00:30
   * // standard ZDT factory would result in 01:00 rather than 00:30
   * LocalDateTime ldt = atTime(LocalTime.MIDNIGHT);
   * if (zone instanceof ZoneOffset == false) {
   * ZoneRules rules = zone.getRules();
   * ZoneOffsetTransition trans = rules.getTransition(ldt);
   * if (trans != null && trans.isGap()) {
   * ldt = trans.getDateTimeAfter();
   * }
   * }
   * return ZonedDateTime.of(ldt, zone);
   * }
   */

  // -----------------------------------------------------------------------
  @Override
  public long toEpochDay() {
    return chronology.toEpochDay(this);
  }

  /**
   * Converts this {@code LeapYearDate} to the number of seconds since the epoch
   * of 1970-01-01T00:00:00Z.
   * <p>
   * This combines this local date with the specified time and
   * offset to calculate the epoch-second value, which is the
   * number of elapsed seconds from 1970-01-01T00:00:00Z.
   * Instants on the time-line after the epoch are positive, earlier
   * are negative.
   *
   * @param time the local time, not null
   * @param offset the zone offset, not null
   * @return the number of seconds since the epoch of 1970-01-01T00:00:00Z, may be negative
   * @since 9
   */
  public long toEpochSecond(LocalTime time, ZoneOffset offset) {
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(offset, "offset");
    long secs = toEpochDay() * SECONDS_PER_DAY + time.toSecondOfDay();
    secs -= offset.getTotalSeconds();
    return secs;
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

  /**
   * Checks if this date is after the specified date.
   * <p>
   * This checks to see if this date represents a point on the
   * local time-line after the other date.
   * 
   * <pre>
   *   LeapYearDate a = LeapYearDate.of(2012, 6, 30);
   *   LeapYearDate b = LeapYearDate.of(2012, 7, 1);
   *   a.isAfter(b) == false
   *   a.isAfter(a) == false
   *   b.isAfter(a) == true
   * </pre>
   * <p>
   * This method only considers the position of the two dates on the local time-line.
   * It does not take into account the chronology, or calendar system.
   * This is different from the comparison in {@link #compareTo(ChronoLocalDate)},
   * but is the same approach as {@link ChronoLocalDate#timeLineOrder()}.
   *
   * @param other the other date to compare to, not null
   * @return true if this date is after the specified date
   */
  @Override // override for Javadoc and performance
  public boolean isAfter(ChronoLocalDate other) {
    if (other instanceof LeapYearDate) {
      return compareTo0((LeapYearDate) other) > 0;
    }
    return super.isAfter(other);
  }

  /**
   * Checks if this date is before the specified date.
   * <p>
   * This checks to see if this date represents a point on the
   * local time-line before the other date.
   * 
   * <pre>
   *   LeapYearDate a = LeapYearDate.of(2012, 6, 30);
   *   LeapYearDate b = LeapYearDate.of(2012, 7, 1);
   *   a.isBefore(b) == true
   *   a.isBefore(a) == false
   *   b.isBefore(a) == false
   * </pre>
   * <p>
   * This method only considers the position of the two dates on the local time-line.
   * It does not take into account the chronology, or calendar system.
   * This is different from the comparison in {@link #compareTo(ChronoLocalDate)},
   * but is the same approach as {@link ChronoLocalDate#timeLineOrder()}.
   *
   * @param other the other date to compare to, not null
   * @return true if this date is before the specified date
   */
  @Override // override for Javadoc and performance
  public boolean isBefore(ChronoLocalDate other) {
    if (other instanceof LeapYearDate) {
      return compareTo0((LeapYearDate) other) < 0;
    }
    return super.isBefore(other);
  }

  /**
   * Checks if this date is equal to the specified date.
   * <p>
   * This checks to see if this date represents the same point on the
   * local time-line as the other date.
   * 
   * <pre>
   *   LeapYearDate a = LeapYearDate.of(2012, 6, 30);
   *   LeapYearDate b = LeapYearDate.of(2012, 7, 1);
   *   a.isEqual(b) == false
   *   a.isEqual(a) == true
   *   b.isEqual(a) == false
   * </pre>
   * <p>
   * This method only considers the position of the two dates on the local time-line.
   * It does not take into account the chronology, or calendar system.
   * This is different from the comparison in {@link #compareTo(ChronoLocalDate)}
   * but is the same approach as {@link ChronoLocalDate#timeLineOrder()}.
   *
   * @param other the other date to compare to, not null
   * @return true if this date is equal to the specified date
   */
  @Override // override for Javadoc and performance
  public boolean isEqual(ChronoLocalDate other) {
    if (other instanceof LeapYearDate) {
      return compareTo0((LeapYearDate) other) == 0;
    }
    return super.isEqual(other);
  }

  // -----------------------------------------------------------------------
  /**
   * Checks if this date is equal to another date.
   * <p>
   * Compares this {@code LeapYearDate} with another ensuring that the date is the same.
   * <p>
   * Only objects of type {@code LeapYearDate} are compared, other types return false.
   * To compare the dates of two {@code TemporalAccessor} instances, including dates
   * in two different chronologies, use {@link ChronoField#EPOCH_DAY} as a comparator.
   *
   * @param obj the object to check, null returns false
   * @return true if this is equal to the other date
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LeapYearDate) {
      return compareTo0((LeapYearDate) obj) == 0;
    }
    return false;
  }

  /**
   * A hash code for this date.
   *
   * @return a suitable hash code
   */
  @Override
  public int hashCode() {
    int yearValue = prolepticYear;
    int monthValue = month;
    int dayValue = day;
    return (yearValue & 0xFFFFF800) ^ ((yearValue << 11) + (monthValue << 6) + (dayValue));
  }

  // -----------------------------------------------------------------------
  /**
   * Outputs this date as a {@code String}, such as {@code 2007-12-03}.
   * <p>
   * The output will be in the ISO-8601 format {@code uuuu-MM-dd}.
   *
   * @return a string representation of this date, not null
   */
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
