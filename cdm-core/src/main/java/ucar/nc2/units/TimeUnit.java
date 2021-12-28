/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.Format;
import ucar.units.ConversionException;
import ucar.units.Unit;
import ucar.units.UnitException;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.Calendar;

/**
 * Handles Units that are time durations, eg in seconds, hours, days, years.
 * It keeps track of the original unit name, rather than converting to canonical "seconds".
 * Note this is a duration of time, not a date, see {@link CalendarDate} instead.
 */
@Immutable
public class TimeUnit extends SimpleUnit {

  /**
   * Constructor from a String.
   * 
   * @param text [value] <time unit> eg "hours" or "13 hours". Time unit is from udunits.
   * @throws UnitException if bad format
   */
  public static TimeUnit create(String text) throws UnitException {
    double value;
    String unitString;

    StringTokenizer stoker = new StringTokenizer(text);
    int ntoke = stoker.countTokens();
    if (ntoke == 1) {
      value = 1.0;
      unitString = stoker.nextToken();
    } else if (ntoke == 2) {
      value = Double.parseDouble(stoker.nextToken());
      unitString = stoker.nextToken();
    } else {
      throw new IllegalArgumentException("Not TimeUnit = " + text);
    }

    Unit uu = SimpleUnit.makeUnit(unitString); // always a base unit
    double factor = uu.convertTo(1.0, SimpleUnit.secsUnit);

    return new TimeUnit(uu, value, factor, unitString);
  }

  /**
   * Constructor from a value and a unit name.
   * 
   * @param value amount of the unit.
   * @param unitString Time unit string from udunits.
   * @throws UnitException if parse fails
   */
  public static TimeUnit create(double value, String unitString) throws UnitException {
    Unit uu = SimpleUnit.makeUnit(unitString);
    double factor = uu.convertTo(1.0, SimpleUnit.secsUnit);

    return new TimeUnit(uu, value, factor, unitString);
  }

  /** Get the value. */
  @Override
  public double getValue() {
    return value;
  }

  /**
   * Get the factor that converts this unit to seconds.
   * getValueInSeconds = factor * value
   * 
   * @return factor that converts this unit to seconds.
   */
  public double getFactor() {
    return factor;
  }

  /** Create a new TimeUnit with the given value. */
  public TimeUnit newValue(double value) {
    return new TimeUnit(this.uu, value, this.factor, this.unitString);
  }

  /** Get the "base" unit String, eg "secs" or "days" */
  @Override
  public String getUnitString() {
    return unitString;
  }

  /** String representation. */
  @Override
  public String toString() {
    return Format.d(value, 5) + " " + unitString;
  }

  /**
   * Get the time duration in seconds.
   * 
   * @return get current value in units of seconds
   */
  public double getValueInSeconds() {
    return factor * value;
  }

  /** Create a new TimeUnit with the given value in seconds. */
  public TimeUnit newValueInSeconds(double secs) {
    return new TimeUnit(this.uu, secs / factor, this.factor, this.unitString);
  }

  // override

  /**
   * Convert given value of this unit to the new unit.
   * <em>NOTE: the current value of this unit ignored, the given value is used instead.
   * This is different than ucar.units or SimpleUnit.</em>
   * 
   * @param value in the units of this "base unit"
   * @param outputUnit convert to this base type, must be convertible to units of "seconds"
   * @return new value in the units of the "outputUnit
   */
  public double convertTo(double value, TimeUnit outputUnit) throws ConversionException {
    return uu.convertTo(value, outputUnit.uu);
  }

  /**
   * Add the time amount to the given Date, return a new Date.
   *
   * @param cd add to this CalendarDate
   * @return CalendarDate with getValueInSeconds() added to it.
   */
  public CalendarDate add(CalendarDate cd) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(cd.toDate());
    cal.add(Calendar.SECOND, (int) getValueInSeconds());
    return CalendarDate.of(cal.getTime());
  }

  /** TimeUnits with same value and unitString are equal */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    TimeUnit timeUnit = (TimeUnit) o;
    return Double.compare(timeUnit.value, value) == 0 && unitString.equals(timeUnit.unitString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, unitString);
  }

  ///////////////////////////////////////////////////////////////////////////////

  private final double value;
  private final double factor;
  private final String unitString;

  private TimeUnit(Unit uu, double value, double factor, String unitString) {
    super(uu);
    this.value = value;
    this.factor = factor;
    this.unitString = unitString;
  }

}
