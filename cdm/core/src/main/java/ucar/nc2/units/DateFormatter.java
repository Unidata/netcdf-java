/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.units;

import java.util.Date;

/**
 * Date parsing and formatting. Always uses GMT.
 * These are not thread-safe.
 * These use java.util.Date and java.text.SimpleDateFormat.
 *
 * @deprecated use {@link ucar.nc2.time.CalendarDateFormatter}
 */
@Deprecated
public class DateFormatter {

  private java.text.SimpleDateFormat isoDateTimeFormat, isoDateNoSecsFormat, stdDateTimeFormat, stdDateNoSecsFormat,
      dateOnlyFormat;

  private void isoDateTimeFormat() {
    if (isoDateTimeFormat != null)
      return;
    isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  private void isoDateNoSecsFormat() {
    if (isoDateNoSecsFormat != null)
      return;
    isoDateNoSecsFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    isoDateNoSecsFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  private void stdDateTimeFormat() {
    if (stdDateTimeFormat != null)
      return;
    stdDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    stdDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  private void stdDateNoSecsFormat() {
    if (stdDateNoSecsFormat != null)
      return;
    stdDateNoSecsFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
    stdDateNoSecsFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); // same as UTC
  }

  private void dateOnlyFormat() {
    if (dateOnlyFormat != null)
      return;
    dateOnlyFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
    dateOnlyFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  ////////////////////////////////////////////////////////////////////////////////


  /**
   * Parse the text in W3C profile of ISO 8601 format.
   * 
   * @param text parse this text
   * @return equivalent Date or null if failure
   * @see <a href="http://www.w3.org/TR/NOTE-datetime">W3C profile of ISO 8601</a>
   * 
   * @deprecated As of netCDF-JAVA 4.3.10. Use {@link ucar.nc2.time.CalendarDateFormatter#isoStringToDate(String) }
   *             instead
   * 
   */
  @Deprecated
  public Date getISODate(String text) {
    Date result;

    // try "yyyy-MM-dd HH:mm:ss"
    try {
      result = stdDateTimeFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try "yyyy-MM-dd'T'HH:mm:ss"
    try {
      result = isoDateTimeFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try "yyyy-MM-dd'T'HH:mm"
    try {
      result = isoDateNoSecsFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try "yyyy-MM-dd HH:mm"
    try {
      result = stdDateNoSecsFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    // now try "yyyy-MM-dd"
    try {
      result = dateOnlyFormat(text);
      return result;
    } catch (java.text.ParseException e) {
    }

    return null;
  }

  /**
   * Parse text in the format "yyyy-MM-dd HH:mm:ss"
   * 
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  private Date stdDateTimeFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    stdDateTimeFormat();
    return stdDateTimeFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd HH:mm"
   * 
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  private Date stdDateNoSecsFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    stdDateNoSecsFormat();
    return stdDateNoSecsFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd'T'HH:mm:ss"
   * 
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  private Date isoDateTimeFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    isoDateTimeFormat();
    return isoDateTimeFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd'T'HH:mm"
   * 
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  private Date isoDateNoSecsFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    isoDateNoSecsFormat();
    return isoDateNoSecsFormat.parse(text);
  }

  /**
   * Parse text in the format "yyyy-MM-dd"
   * 
   * @param text parse this text
   * @return equivalent Date
   * @throws java.text.ParseException if not formatted correctly
   */
  private Date dateOnlyFormat(String text) throws java.text.ParseException {
    text = (text == null) ? "" : text.trim();
    dateOnlyFormat();
    return dateOnlyFormat.parse(text);
  }

  ////////////


  /**
   * Return standard GMT date format; show date only, not time. Format = "yyyy-MM-dd"
   * 
   * @deprecated use toDateOnlyString
   */
  public String getStandardDateOnlyString(Date date) {
    return toDateOnlyString(date);
  }

  /**
   * date only format= yyyy-MM-dd
   *
   * @deprecated use {@link ucar.nc2.time.CalendarDateFormatter#toDateString(Date)}
   */
  @Deprecated
  public String toDateString(Date date) {
    return toDateOnlyString(date);
  }

  /**
   * date only format= yyyy-MM-dd
   * 
   * @param date format this date
   * @return date formatted as date only
   * @deprecated use {@link ucar.nc2.time.CalendarDateFormatter#toDateString(Date)}
   */
  @Deprecated
  public String toDateOnlyString(Date date) {
    dateOnlyFormat();
    return dateOnlyFormat.format(date);
  }

  /**
   * Return standard formatted GMT date and time String. Format = "yyyy-MM-dd HH:mm:ss'Z'"
   *
   * @deprecated use {@link ucar.nc2.time.CalendarDateFormatter#toDateTimeString(Date)}
   */
  @Deprecated
  public String getStandardDateString2(Date date) {
    return toDateTimeString(date);
  }

  /**
   * "standard date format" = yyyy-MM-dd HH:mm:ssZ
   * 
   * @param date format this date
   * @return date formatted as date/time
   * @deprecated use {@link ucar.nc2.time.CalendarDateFormatter#toDateTimeString(Date)}
   */
  @Deprecated
  public String toDateTimeString(Date date) {
    if (date == null)
      return "Unknown";
    stdDateTimeFormat();
    return stdDateTimeFormat.format(date) + "Z";
  }

  /**
   * Return standard formatted GMT date and time String. Format = "yyyy-MM-dd'T'HH:mm:ss'Z'"
   *
   * @deprecated use {@link ucar.nc2.time.CalendarDateFormatter#toDateTimeStringISO}
   */
  @Deprecated
  public String getStandardDateString(Date date) {
    return toDateTimeStringISO(date);
  }

  /**
   * "ISO date format" = yyyy-MM-dd'T'HH:mm:ssZ
   * 
   * @param date format this date
   * @return date formatted as ISO date string
   * @deprecated use {@link ucar.nc2.time.CalendarDateFormatter#toDateTimeStringISO}
   */
  @Deprecated
  public String toDateTimeStringISO(Date date) {
    isoDateTimeFormat();
    return isoDateTimeFormat.format(date) + "Z";
  }
}
