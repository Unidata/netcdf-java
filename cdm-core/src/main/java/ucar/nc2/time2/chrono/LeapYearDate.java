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
import static ucar.nc2.time2.chrono.LeapYearChronology.DAY_OF_MONTH_RANGE;
import static ucar.nc2.time2.chrono.LeapYearChronology.MONTH_OF_YEAR_RANGE;
import static ucar.nc2.time2.chrono.LeapYearChronology.YEAR_RANGE;

import static ucar.nc2.time2.chrono.LeapYearChronology.DAYS_IN_WEEK;
import static ucar.nc2.time2.chrono.LeapYearChronology.MONTHS_IN_YEAR;
import static ucar.nc2.time2.chrono.LeapYearChronology.WEEKS_IN_MONTH;

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

  // -----------------------------------------------------------------------
  /**
   * LOOK
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

    /*
     * return LeapYearDate.from(chronology, temporal);
     * 
     * /*
     * if (temporal instanceof ChronoLocalDate) {
     * ChronoLocalDate localDate = (ChronoLocalDate) temporal;
     * // return LeapYearDate.ofEpochDay(localDate.getLong(ChronoField.EPOCH_DAY));
     * return LeapYearDate.of(chronology, localDate.get(YEAR), localDate.get(MONTH_OF_YEAR),
     * localDate.get(DAY_OF_MONTH));
     * 
     * } else if (temporal instanceof ChronoLocalDateTime) {
     * ChronoLocalDateTime<?> localDateTime = (ChronoLocalDateTime<?>) temporal;
     * // LOOK maybe have to turn it back to instance and reparse ??
     * return LeapYearDate.of(chronology, localDateTime.get(YEAR), localDateTime.get(MONTH_OF_YEAR),
     * localDateTime.get(DAY_OF_MONTH));
     * }
     * throw new DateTimeException("Unable to obtain LeapYearDate from TemporalAccessor: " + temporal + " of type "
     * + temporal.getClass().getName());
     */
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
  /*
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
   * 
   * @return the value for the field
   * 
   * @throws DateTimeException if a value for the field cannot be obtained or
   * the value is outside the range of valid values for the field
   * 
   * @throws UnsupportedTemporalTypeException if the field is not supported or
   * the range of values exceeds an {@code int}
   * 
   * @throws ArithmeticException if numeric overflow occurs
   *
   * @Override // override for Javadoc and performance
   * public int get(TemporalField field) {
   * if (field instanceof ChronoField) {
   * return get0(field);
   * }
   * return super.get(field);
   * }
   * 
   * /*
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
   * 
   * @return the value for the field
   * 
   * @throws DateTimeException if a value for the field cannot be obtained
   * 
   * @throws UnsupportedTemporalTypeException if the field is not supported
   * 
   * @throws ArithmeticException if numeric overflow occurs
   *
   * @Override
   * public long getLong(TemporalField field) {
   * if (field instanceof ChronoField) {
   * if (field == EPOCH_DAY) {
   * return toEpochDay();
   * }
   * if (field == PROLEPTIC_MONTH) {
   * return getProlepticMonth();
   * }
   * return get0(field);
   * }
   * return field.getFrom(this);
   * }
   * 
   * private int get0(TemporalField field) {
   * switch ((ChronoField) field) {
   * case DAY_OF_WEEK:
   * return getDayOfWeekField().getValue();
   * case ALIGNED_DAY_OF_WEEK_IN_MONTH:
   * return ((day - 1) % 7) + 1;
   * case ALIGNED_DAY_OF_WEEK_IN_YEAR:
   * return ((getDayOfYear() - 1) % 7) + 1;
   * case DAY_OF_MONTH:
   * return day;
   * case DAY_OF_YEAR:
   * return getDayOfYear();
   * case EPOCH_DAY:
   * throw new UnsupportedTemporalTypeException("Invalid field 'EpochDay' for get() method, use getLong() instead");
   * case ALIGNED_WEEK_OF_MONTH:
   * return ((day - 1) / 7) + 1;
   * case ALIGNED_WEEK_OF_YEAR:
   * return ((getDayOfYear() - 1) / 7) + 1;
   * case MONTH_OF_YEAR:
   * return month;
   * case PROLEPTIC_MONTH:
   * throw new UnsupportedTemporalTypeException(
   * "Invalid field 'ProlepticMonth' for get() method, use getLong() instead");
   * case YEAR_OF_ERA:
   * return (prolepticYear >= 1 ? prolepticYear : 1 - prolepticYear);
   * case YEAR:
   * return prolepticYear;
   * case ERA:
   * return (prolepticYear >= 1 ? 1 : 0);
   * }
   * throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
   * }
   */

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

  /*
   * @Override
   * public LeapYearDate with(TemporalField field, long newValue) {
   * if (field instanceof ChronoField) {
   * ChronoField f = (ChronoField) field;
   * f.checkValidValue(newValue);
   * switch (f) {
   * case DAY_OF_WEEK:
   * return plusDays(newValue - getDayOfWeekField().getValue());
   * case ALIGNED_DAY_OF_WEEK_IN_MONTH:
   * return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
   * case ALIGNED_DAY_OF_WEEK_IN_YEAR:
   * return plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
   * case DAY_OF_MONTH:
   * return withDayOfMonth((int) newValue);
   * case DAY_OF_YEAR:
   * return withDayOfYear((int) newValue);
   * case EPOCH_DAY:
   * return chronology.dateEpochDay(newValue);
   * case ALIGNED_WEEK_OF_MONTH:
   * return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_MONTH));
   * case ALIGNED_WEEK_OF_YEAR:
   * return plusWeeks(newValue - getLong(ALIGNED_WEEK_OF_YEAR));
   * case MONTH_OF_YEAR:
   * return withMonth((int) newValue);
   * case PROLEPTIC_MONTH:
   * return plusMonths(newValue - getProlepticMonth());
   * case YEAR_OF_ERA:
   * return withYear((int) (prolepticYear >= 1 ? newValue : 1 - newValue));
   * case YEAR:
   * return withYear((int) newValue);
   * case ERA:
   * return (getLong(ERA) == newValue ? this : withYear(1 - prolepticYear));
   * }
   * throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
   * }
   * return field.adjustInto(this, newValue);
   * }
   * 
   * private LeapYearDate withYear(int year) {
   * if (this.prolepticYear == year) {
   * return this;
   * }
   * YEAR.checkValidValue(year);
   * return resolvePreviousValid(chronology, year, month, day);
   * }
   * 
   * private LeapYearDate withMonth(int month) {
   * if (this.month == month) {
   * return this;
   * }
   * MONTH_OF_YEAR.checkValidValue(month);
   * return resolvePreviousValid(chronology, prolepticYear, month, day);
   * }
   * 
   * private LeapYearDate withDayOfMonth(int dayOfMonth) {
   * if (this.day == dayOfMonth) {
   * return this;
   * }
   * return of(chronology, prolepticYear, month, dayOfMonth);
   * }
   * 
   * /**
   * Returns a copy of this {@code LeapYearDate} with the day-of-year altered.
   * <p>
   * If the resulting date is invalid, an exception is thrown.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param dayOfYear the day-of-year to set in the result, from 1 to 365-366
   * 
   * @return a {@code LeapYearDate} based on this date with the requested day, not null
   * 
   * @throws DateTimeException if the day-of-year value is invalid,
   * or if the day-of-year is invalid for the year
   *
   * public LeapYearDate withDayOfYear(int dayOfYear) {
   * if (this.getDayOfYear() == dayOfYear) {
   * return this;
   * }
   * return ofYearDay(chronology, prolepticYear, dayOfYear);
   * }
   * 
   */

  @Override
  ValueRange rangeAlignedWeekOfMonth() {
    // never invoked LOOK can we remove from superclass?
    return ValueRange.of(1, WEEKS_IN_MONTH);
  }

  @Override
  LeapYearDate resolvePrevious(int prolepticYear, int newMonth, int dayOfMonth) {
    int monthR = Math.min(month, MONTHS_IN_YEAR);
    int dayR = Math.min(day, chronology.daysInMonth(monthR - 1));
    return create(this.chronology, prolepticYear, monthR, dayR);
  }

  // -----------------------------------------------------------------------
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

  // -----------------------------------------------------------------------
  /*
   * LOOK not sure all of plus() and minus() is needed - see uniform30 and AbstractDate
   * 
   * @Override
   * public LeapYearDate plus(TemporalAmount amountToAdd) {
   * if (amountToAdd instanceof Period) {
   * Period periodToAdd = (Period) amountToAdd;
   * return plusMonths(periodToAdd.toTotalMonths()).plusDays(periodToAdd.getDays());
   * }
   * Objects.requireNonNull(amountToAdd, "amountToAdd");
   * return (LeapYearDate) amountToAdd.addTo(this);
   * }
   * 
   * @Override
   * public LeapYearDate plus(long amountToAdd, TemporalUnit unit) {
   * if (unit instanceof ChronoUnit) {
   * ChronoUnit f = (ChronoUnit) unit;
   * switch (f) {
   * case DAYS:
   * return plusDays(amountToAdd);
   * case WEEKS:
   * return plusWeeks(amountToAdd);
   * case MONTHS:
   * return plusMonths(amountToAdd);
   * case YEARS:
   * return plusYears(amountToAdd);
   * case DECADES:
   * return plusYears(Math.multiplyExact(amountToAdd, 10));
   * case CENTURIES:
   * return plusYears(Math.multiplyExact(amountToAdd, 100));
   * case MILLENNIA:
   * return plusYears(Math.multiplyExact(amountToAdd, 1000));
   * case ERAS:
   * return with(ERA, Math.addExact(getLong(ERA), amountToAdd));
   * }
   * throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
   * }
   * return unit.addTo(this, amountToAdd);
   * }
   * 
   * public LeapYearDate plusYears(long yearsToAdd) {
   * if (yearsToAdd == 0) {
   * return this;
   * }
   * int newYear = YEAR.checkValidIntValue(prolepticYear + yearsToAdd); // safe overflow
   * return resolvePreviousValid(newYear, month, day);
   * }
   * 
   * 
   * public LeapYearDate plusWeeks(long weeksToAdd) {
   * return plusDays(Math.multiplyExact(weeksToAdd, 7));
   * }
   * 
   * public LeapYearDate plusDays(long daysToAdd) {
   * if (daysToAdd == 0) {
   * return this;
   * }
   * long dom = day + daysToAdd;
   * if (dom > 0) {
   * if (dom <= 28) {
   * return new LeapYearDate(this.chronology, prolepticYear, month, (int) dom);
   * } else if (dom <= 59) { // 59th Jan is 28th Feb, 59th Feb is 31st Mar
   * long monthLen = lengthOfMonth();
   * if (dom <= monthLen) {
   * return new LeapYearDate(this.chronology, prolepticYear, month, (int) dom);
   * } else if (month < 12) {
   * return new LeapYearDate(this.chronology, prolepticYear, month + 1, (int) (dom - monthLen));
   * } else {
   * YEAR.checkValidValue(prolepticYear + 1);
   * return new LeapYearDate(this.chronology, prolepticYear + 1, 1, (int) (dom - monthLen));
   * }
   * }
   * }
   * 
   * long mjDay = Math.addExact(toEpochDay(), daysToAdd);
   * return chronology.dateEpochDay(mjDay);
   * }
   * 
   * 
   * @Override
   * public LeapYearDate minus(TemporalAmount amountToSubtract) {
   * if (amountToSubtract instanceof Period) {
   * Period periodToSubtract = (Period) amountToSubtract;
   * return minusMonths(periodToSubtract.toTotalMonths()).minusDays(periodToSubtract.getDays());
   * }
   * Objects.requireNonNull(amountToSubtract, "amountToSubtract");
   * return (LeapYearDate) amountToSubtract.subtractFrom(this);
   * }
   * 
   * 
   * @Override
   * public LeapYearDate minus(long amountToSubtract, TemporalUnit unit) {
   * return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
   * : plus(-amountToSubtract, unit));
   * }
   * 
   * 
   * public LeapYearDate minusYears(long yearsToSubtract) {
   * return (yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-yearsToSubtract));
   * }
   * 
   * 
   * public LeapYearDate minusMonths(long monthsToSubtract) {
   * return (monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
   * : plusMonths(-monthsToSubtract));
   * }
   * 
   * 
   * public LeapYearDate minusWeeks(long weeksToSubtract) {
   * return (weeksToSubtract == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeksToSubtract));
   * }
   * 
   * 
   * public LeapYearDate minusDays(long daysToSubtract) {
   * return (daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract));
   * }
   * 
   */

  // -----------------------------------------------------------------------
  /*
   * @SuppressWarnings("unchecked")
   * 
   * @Override
   * public <R> R query(TemporalQuery<R> query) {
   * if (query == TemporalQueries.localDate()) {
   * return (R) this;
   * }
   * return super.query(query);
   * }
   * 
   * 
   * @Override // override for Javadoc
   * public Temporal adjustInto(Temporal temporal) {
   * return super.adjustInto(temporal);
   * }
   * 
   */

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

  /*
   * long daysUntil(LeapYearDate end) {
   * return end.toEpochDay() - toEpochDay(); // no overflow
   * }
   * 
   * private long monthsUntil(LeapYearDate end) {
   * long packed1 = getProlepticMonth() * 32L + getDayOfMonth(); // no overflow
   * long packed2 = end.getProlepticMonth() * 32L + end.getDayOfMonth(); // no overflow
   * return (packed2 - packed1) / 32;
   * }
   * 
   * /*
   * public Stream<LeapYearDate> datesUntil(LeapYearDate endExclusive) {
   * long end = endExclusive.toEpochDay();
   * long start = toEpochDay();
   * if (end < start) {
   * throw new IllegalArgumentException(endExclusive + " < " + this);
   * }
   * return LongStream.range(start, end).mapToObj(wtf -> chronology.dateEpochDay(wtf));
   * }
   * 
   * public Stream<LeapYearDate> datesUntil(LeapYearDate endExclusive, Period step) {
   * if (step.isZero()) {
   * throw new IllegalArgumentException("step is zero");
   * }
   * long end = endExclusive.toEpochDay();
   * long start = toEpochDay();
   * long until = end - start;
   * long months = step.toTotalMonths();
   * long days = step.getDays();
   * if ((months < 0 && days > 0) || (months > 0 && days < 0)) {
   * throw new IllegalArgumentException("period months and days are of opposite sign");
   * }
   * if (until == 0) {
   * return Stream.empty();
   * }
   * int sign = months > 0 || days > 0 ? 1 : -1;
   * if (sign < 0 ^ until < 0) {
   * throw new IllegalArgumentException(endExclusive + (sign < 0 ? " > " : " < ") + this);
   * }
   * if (months == 0) {
   * long steps = (until - sign) / days; // non-negative
   * return LongStream.rangeClosed(0, steps).mapToObj(n -> chronology.dateEpochDay(start + n * days));
   * }
   * // 48699/1600 = 365.2425/12, no overflow, non-negative result
   * long steps = until * 1600 / (months * 48699 + days * 1600) + 1;
   * long addMonths = months * steps;
   * long addDays = days * steps;
   * long maxAddMonths = months > 0 ? chronology.MAX_DATE.getProlepticMonth() - getProlepticMonth()
   * : getProlepticMonth() - chronology.MIN_DATE.getProlepticMonth();
   * // adjust steps estimation
   * if (addMonths * sign > maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
   * steps--;
   * addMonths -= months;
   * addDays -= days;
   * if (addMonths * sign > maxAddMonths || (plusMonths(addMonths).toEpochDay() + addDays) * sign >= end * sign) {
   * steps--;
   * }
   * }
   * return LongStream.rangeClosed(0, steps).mapToObj(n -> this.plusMonths(months * n).plusDays(days * n));
   * }
   * 
   */

  @Override // for covariant return type
  @SuppressWarnings("unchecked")
  public ChronoLocalDateTime<LeapYearDate> atTime(LocalTime localTime) {
    return (ChronoLocalDateTime<LeapYearDate>) super.atTime(localTime);
  }

  // -----------------------------------------------------------------------
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

  /*
   * @Override // override for Javadoc and performance
   * public boolean isAfter(ChronoLocalDate other) {
   * if (other instanceof LeapYearDate) {
   * return compareTo0((LeapYearDate) other) > 0;
   * }
   * return super.isAfter(other);
   * }
   * 
   * @Override // override for Javadoc and performance
   * public boolean isBefore(ChronoLocalDate other) {
   * if (other instanceof LeapYearDate) {
   * return compareTo0((LeapYearDate) other) < 0;
   * }
   * return super.isBefore(other);
   * }
   * 
   */

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
