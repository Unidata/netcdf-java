/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.units;

import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.Format;
import ucar.units.ConversionException;
import ucar.units.Unit;
import ucar.units.UnitException;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.Calendar;

/**
 * Handles Units that are time durations, eg in seconds, hours, days, years.
 * It keeps track of the original unit name, rather than converting to canonical "seconds".
 * The unit name never changes, but the value may (will be Immutable in ver7)
 * <p>
 * This is a wrapper around ucar.units.
 * The underlying ucar.units.Unit always has a value of "1.0", ie is a base unit.
 *
 * TODO: 8/12/2020 make Immutable in version 7
 */
public class TimeUnit extends SimpleUnit {
  private double value;
  private final double factor;
  private final String unitString;

  private TimeUnit(Unit uu, double value, double factor, String unitString) {
    super(uu);
    this.value = value;
    this.factor = factor;
    this.unitString = unitString;
  }

  /**
   * Constructor from a String.
   * 
   * @param text [value] <time unit> eg "hours" or "13 hours". Time unit is from udunits.
   * @throws UnitException if bad format
   */
  public TimeUnit(String text) throws UnitException {
    StringTokenizer stoker = new StringTokenizer(text);
    int ntoke = stoker.countTokens();
    if (ntoke == 1) {
      this.value = 1.0;
      this.unitString = stoker.nextToken();

    } else if (ntoke == 2) {
      this.value = Double.parseDouble(stoker.nextToken());
      this.unitString = stoker.nextToken();
    } else {
      throw new IllegalArgumentException("Not TimeUnit = " + text);
    }

    uu = SimpleUnit.makeUnit(unitString); // always a base unit
    factor = uu.convertTo(1.0, SimpleUnit.secsUnit);
  }

  /**
   * Constructor from a value and a unit name.
   * 
   * @param value amount of the unit.
   * @param unitString Time unit string from udunits.
   * @throws UnitException if parse fails
   */
  public TimeUnit(double value, String unitString) throws UnitException {
    this.value = value;
    this.unitString = unitString;
    uu = SimpleUnit.makeUnit(unitString);
    factor = uu.convertTo(1.0, SimpleUnit.secsUnit);
  }

  /**
   * Copy Constructor.
   * 
   * @param src copy from here
   */
  public TimeUnit(TimeUnit src) {
    this.value = src.getValue();
    this.unitString = src.getUnitString();
    uu = src.uu;
    factor = src.getFactor();
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

  /**
   * Set the value in the original units.
   * 
   * @param value set value, must be in units of this
   * @deprecated use newValue(double)
   */
  @Deprecated
  public void setValue(double value) {
    this.value = value;
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

  /**
   * Get the time duration in seconds of the specified value.
   * 
   * @param value convert this value, must be in units of this
   * @return get the value in units of seconds
   */
  public double getValueInSeconds(double value) {
    return factor * value;
  }

  /**
   * Set the value, using the given number of seconds.
   * 
   * @param secs : number of seconds; convert this to the units of this TimeUnit.
   * @deprecated use newValueInSeconds(double)
   */
  @Deprecated
  public void setValueInSeconds(double secs) {
    value = secs / factor;
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
   * @param d add to this Date
   * @return Date with getValueInSeconds() added to it.
   * @deprecated use add(CalendarDate)
   */
  @Deprecated
  public Date add(Date d) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.add(Calendar.SECOND, (int) getValueInSeconds());
    return cal.getTime();
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
    if (!(o instanceof TimeUnit))
      return false;
    return o.hashCode() == this.hashCode();
  }

  /** Override hashcode to be consistent with equals. */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + unitString.hashCode();
      result = 37 * result + (int) (1000.0 * value);
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode;

}
