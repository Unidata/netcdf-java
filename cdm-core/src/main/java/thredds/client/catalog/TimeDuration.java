/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.units.TimeUnit;
import ucar.units.ConversionException;
import ucar.units.UnitException;

import javax.annotation.concurrent.Immutable;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Calendar;
import java.util.Objects;

/**
 * The thredds "duration" XML element type: specifies a length of time.
 * This is really the same as a ucar.nc2.units.TimeUnit, but it allows xsd:duration syntax as well
 * as udunits syntax. It also keeps track if the text is empty.
 * <p/>
 * A duration can be one of the following:
 * <ol>
 * <li>a valid udunits string compatible with "secs"
 * <li>an xsd:duration type specified in the following form "PnYnMnDTnHnMnS" where:
 * <ul>
 * <li>P indicates the period (required)
 * <li>nY indicates the number of years
 * <li>nM indicates the number of months
 * <li>nD indicates the number of days
 * <li>T indicates the start of a time section (required if you are going to specify hours, minutes, or seconds)
 * <li>nH indicates the number of hours
 * <li>nM indicates the number of minutes
 * <li>nS indicates the number of seconds
 * </ul>
 * </ol>
 *
 * @author john caron
 * @see "https://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#durationType"
 */
@Immutable
public class TimeDuration {

  /**
   * Construct from 1) udunit time unit string, 2) xsd:duration syntax, 3) blank string.
   *
   * @param text parse this text.
   * @throws java.text.ParseException if invalid text.
   */
  public static TimeDuration parse(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();

    // see if its blank
    if (text.isEmpty()) {
      try {
        TimeUnit timeUnit = new TimeUnit("1 sec"); // LOOK why is this needed?
        return new TimeDuration(text, timeUnit);
      } catch (Exception e) { // cant happen
        throw new java.text.ParseException(e.getMessage(), 0);
      }
    }

    // see if its a udunits string
    try {
      TimeUnit timeUnit = new TimeUnit(text);
      return new TimeDuration(text, timeUnit);
    } catch (Exception e) {
      // see if its a xsd:duration
      try {
        return parseW3CDuration(text);
      } catch (Exception e1) {
        throw new java.text.ParseException(e.getMessage(), 0);
      }
    }
  }

  /**
   * A time span as defined in the W3C XML Schema 1.0 specification:
   * "PnYnMnDTnHnMnS, where nY represents the number of years, nM the number of months, nD the number of days,
   * 'T' is the date/time separator, nH the number of hours, nM the number of minutes and nS the number of seconds.
   * The number of seconds can include decimal digits to arbitrary precision."
   *
   * @param text parse this text, format PnYnMnDTnHnMnS
   * @return TimeDuration
   * @throws java.text.ParseException when text is misformed
   */
  public static TimeDuration parseW3CDuration(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();

    try {
      DatatypeFactory factory = DatatypeFactory.newInstance();
      Duration d = factory.newDuration(text);

      Calendar c = Calendar.getInstance();
      c.set(1900, 0, 1, 0, 0, 0);
      long secs = d.getTimeInMillis(c.getTime()) / 1000;

      TimeUnit timeUnit = new TimeUnit(secs + " secs");
      return new TimeDuration(text, timeUnit);
    } catch (Exception e) {
      throw new java.text.ParseException(e.getMessage(), 0);
    }
  }

  //////////////////////////////////////////////////
  private final String text;
  private final TimeUnit timeUnit;
  private final boolean isBlank;

  /** Construct from a TimeUnit. */
  public TimeDuration(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
    this.text = timeUnit.toString();
    this.isBlank = false;
  }

  private TimeDuration(String text, TimeUnit timeUnit) {
    this.text = text.trim();
    this.isBlank = this.text.isEmpty();
    this.timeUnit = timeUnit;
  }

  /** Get the duration in natural units, ie units of getTimeUnit() */
  public double getValue() {
    return timeUnit.getValue();
  }

  /**
   * Get the time duration in a specified unit of time.
   *
   * @param want in these units
   * @return the duration in units
   * @throws IllegalArgumentException if specified unit is not compatible with time
   */
  public double getValue(TimeUnit want) {
    try {
      return timeUnit.convertTo(timeUnit.getValue(), want);
    } catch (ConversionException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Get the time duration in a specified unit of time.
   *
   * @param unit unit spec from new TimeUnit(String)
   * @return the duration in units
   * @throws IllegalArgumentException if specified unit is not compatible with time
   */
  public double getValue(String unit) {
    try {
      TimeUnit tdayUnit = new TimeUnit(unit);
      return getValue(tdayUnit);
    } catch (UnitException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public CalendarPeriod toCalendarPeriod() {
    return CalendarPeriod.of((int) getValueInSeconds(), CalendarPeriod.Field.Second);
  }

  /** Get the duration in seconds */
  public double getValueInSeconds() {
    return timeUnit.getValueInSeconds();
  }

  /** If this is a blank string */
  public boolean isBlank() {
    return isBlank;
  }

  /** Get the time unit */
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  /** Get the text */
  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return getText();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    TimeDuration that = (TimeDuration) o;
    return isBlank == that.isBlank && Objects.equals(text, that.text) && Objects.equals(timeUnit, that.timeUnit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text, timeUnit, isBlank);
  }
}
