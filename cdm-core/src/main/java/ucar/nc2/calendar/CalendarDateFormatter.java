/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Static routines for date formatting.
 */
@ThreadSafe
public class CalendarDateFormatter {
  private static final DateTimeFormatter isof = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

  private static final DateTimeFormatter dtfNoSecs =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter dtf_with_millis_of_second =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter df_units =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'").withZone(ZoneOffset.UTC); // udunits

  public static String toDateTimeStringISO(CalendarDate cd) {
    return cd.toString();
  }

  public static String toDateTimeStringISO(Date d) {
    return toDateTimeStringISO(CalendarDate.of(d));
  }

  public static String toDateTimeStringISO(long millisecs) {
    return toDateTimeStringISO(CalendarDate.of(millisecs));
  }

  public static String toDateTimeStringNoSecs(CalendarDate cd) {
    if (cd.getCalendar().getChronology() == null) {
      return cd.format(dtfNoSecs);
    } else {
      return cd.format(dtfNoSecs.withChronology(cd.getCalendar().getChronology()));
    }
  }

  public static String toDateTimeStringWithMillis(CalendarDate cd) {
    if (cd.getCalendar().getChronology() == null) {
      return cd.format(dtf_with_millis_of_second);
    } else {
      return cd.format(dtf_with_millis_of_second.withChronology(cd.getCalendar().getChronology()));
    }
  }

  public static String toDateTimeString(CalendarDate cd) {
    if (cd.getCalendar().getChronology() == null) {
      return cd.format(dtf);
    } else {
      return cd.format(dtf.withChronology(cd.getCalendar().getChronology()));
    }
  }

  public static String toDateString(CalendarDate cd) {
    return cd.format(df);
  }

  /** Parse a date only in the form "yyyy-MM-dd". */
  public static CalendarDate parseDateString(String dateOnly) {
    LocalDate date = LocalDate.parse(dateOnly, df);
    OffsetDateTime dt = OffsetDateTime.of(date, LocalTime.of(0, 0), ZoneOffset.UTC);
    return new CalendarDateIso(dt);
  }

  /////////////////////////////////////////////
  private final DateTimeFormatter dflocal;

  /**
   * Date formatter with specified pattern.
   * 
   * @see <a href=
   *      "https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html">DateTimeFormatter
   *      javadoc</a>
   */
  public CalendarDateFormatter(String pattern) {
    dflocal = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
  }

  public String toString(CalendarDate cd) {
    return cd.format(dflocal);
  }

  public CalendarDate parse(String timeString) {
    try {
      OffsetDateTime dt = OffsetDateTime.parse(timeString, dflocal);
      return new CalendarDateIso(dt);
    } catch (Exception e) {
      // continue
    }
    LocalDate date = LocalDate.parse(timeString, dflocal);
    OffsetDateTime dt = OffsetDateTime.of(date, LocalTime.of(0, 0), ZoneOffset.UTC);
    return new CalendarDateIso(dt);
  }
}
