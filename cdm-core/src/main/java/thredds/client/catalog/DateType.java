/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import com.google.common.base.Preconditions;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.units.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Optional;

/**
 * The thredds "dateType" and "dateTypeFormatted" XML element types.
 * This is mostly a general way to specify dates in a string.
 * It allows a date to mean "present". <strong>"Present" always sorts after any date, including dates in the
 * future.</strong>
 * It allows an optional attribute called "type" which is an enumeration like "created", "modified", etc
 * taken from Dublin Core vocabulary.
 * <p/>
 * A DateType can be specified in the following ways:
 * <ol>
 * <li>an xsd:date, with form "CCYY-MM-DD"
 * <li>an xsd:dateTime with form "CCYY-MM-DDThh:mm:ss"
 * <li>a valid udunits date string
 * <li>the string "present"
 * </ol>
 *
 * @author john caron
 * @see <a href=""https://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#dateType"">THREDDS
 *      dateType</a>
 */
@Immutable
public class DateType {

  /** Get the DateType representing the present time. */
  public static DateType present() {
    return new DateType();
  }

  /**
   * Parse text to get a DateType.
   *
   * @param text string representation
   */
  public static DateType parse(String text) throws java.text.ParseException {
    return parse(text, null, null, null);
  }

  /**
   * Parse text to get a DateType.
   *
   * @param text string representation
   * @param format using java.text.SimpleDateFormat, or null
   * @param type type of date, or null
   * @param calendar the calendar, or null for default
   * @throws java.text.ParseException or IllegalArgumentException if error parsing text
   */
  @Nullable
  public static DateType parse(@Nullable String text, @Nullable String format, @Nullable String type,
      @Nullable ucar.nc2.calendar.Calendar calendar) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();

    if (text.isEmpty()) {
      return new DateType(text, format, type, null);
    }

    // see if its the string "present"
    if (text.equalsIgnoreCase("present")) {
      return present();
    }

    // see if its got a format
    if (format != null) {
      CalendarDateFormatter dateFormat = new CalendarDateFormatter(format);
      CalendarDate date = dateFormat.parse(text); // LOOK not using the calendar
      return new DateType(text, format, type, date);
    }

    // see if its a udunits string
    String calName = calendar == null ? null : calendar.name();
    Optional<CalendarDate> date = CalendarDate.fromUdunitIsoDate(calName, text);
    if (date.isPresent()) {
      return new DateType(text, format, type, date.get());
    }

    return null;
  }

  //////////////////////////////////////////////////////////
  @Nullable
  private final String format;
  @Nullable
  private final String type;
  @Nullable
  private final CalendarDate date;
  private final String text;
  private final boolean isPresent;
  private final boolean isBlank;

  private DateType() {
    this.date = null;
    this.text = "present";

    this.isPresent = true;
    this.isBlank = false;
    this.format = null;
    this.type = null;
  }

  /**
   * Constructor using a CalendarDate
   * 
   * @param date the given CalendarDate
   */
  public DateType(CalendarDate date) {
    this.date = Preconditions.checkNotNull(date);
    this.text = toDateTimeString();

    this.isPresent = false;
    this.isBlank = false;
    this.format = null;
    this.type = null;
  }

  private DateType(String text, @Nullable String format, @Nullable String type, CalendarDate date) {
    this.text = text == null ? "" : text.trim();
    this.isBlank = this.text.isEmpty();
    this.format = format;
    this.type = type;
    this.date = Preconditions.checkNotNull(date);

    this.isPresent = false;
  }

  /**
   * Get the CalendarDate, may be null if isBlank().
   */
  @Nullable
  public CalendarDate toCalendarDate() {
    return isPresent() ? CalendarDate.present() : date;
  }

  /**
   * Does this represent the present time?
   */
  public boolean isPresent() {
    return isPresent;
  }

  /**
   * Was blank text passed to the constructor?
   */
  public boolean isBlank() {
    return isBlank;
  }

  /**
   * Get the original text passed to the constructor.
   */
  public String getText() {
    return text;
  }

  /**
   * Get the SimpleDateFormat format for parsing the text, passed into the constructor.
   */
  @Nullable
  public String getFormat() {
    return format;
  }

  /**
   * Get the type of Date, passed into the constructor.
   */
  @Nullable
  public String getType() {
    return type;
  }

  /** Same as CalendarDateFormatter.toDateString */
  public String toDateString() {
    return CalendarDateFormatter.toDateString(toCalendarDate());
  }

  /** Same as CalendarDateFormatter.toDateTimeStringISO */
  public String toDateTimeString() {
    return CalendarDateFormatter.toDateTimeStringISO(toCalendarDate());
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
    DateType dateType = (DateType) o;
    return isPresent == dateType.isPresent && isBlank == dateType.isBlank && Objects.equals(text, dateType.text)
        && Objects.equals(format, dateType.format) && Objects.equals(type, dateType.type)
        && Objects.equals(date, dateType.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text, format, type, isPresent, isBlank, date);
  }

  /**
   * Is this date before the given date. if d.isPresent, always true, else if this.isPresent, false.
   *
   * @param d test against this date
   * @return true if this date before the given date
   */
  public boolean before(DateType d) {
    if (d.isPresent())
      return true;
    if (this.isPresent())
      return false;
    if (this.date == null) {
      return false;
    }
    if (d.toCalendarDate() == null) {
      return false;
    }
    return this.date.isBefore(d.toCalendarDate());
  }

  public DateType add(TimeDuration d) {
    return add(d.getTimeUnit());
  }

  public DateType add(TimeUnit d) {
    CalendarDate useDate = toCalendarDate();
    CalendarDate result = useDate.add((int) d.getValueInSeconds(), CalendarPeriod.Field.Second);
    return new DateType(result);
  }

  public DateType subtract(TimeDuration d) {
    return subtract(d.getTimeUnit());
  }

  public DateType subtract(TimeUnit d) {
    CalendarDate useDate = toCalendarDate();
    CalendarDate result = useDate.add((int) -d.getValueInSeconds(), CalendarPeriod.Field.Second);
    return new DateType(result);
  }
}

