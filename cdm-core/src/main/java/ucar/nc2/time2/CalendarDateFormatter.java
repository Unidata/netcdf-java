/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

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

  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter dtf_with_millis_of_second =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
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

  public static String toDateTimeString(CalendarDate cd) {
    if (cd.getMillis() == 0) {
      return cd.format(dtf);
    } else {
      return cd.format(dtf_with_millis_of_second);
    }
  }

  public static String toDateTimeStringShort(CalendarDate cd) {
    return cd.format(dtf);
  }

  public static String toDateTimeString(Date date) {
    return toDateTimeString(CalendarDate.of(date));
  }

  public static String toDateString(CalendarDate cd) {
    return cd.format(df);
  }

  /////////////////////////////////////////////
  private final DateTimeFormatter dflocal;

  /**
   * Date formatter with specified pattern.
   * NOTE: we are using jodatime patterns right now, but may switch to jsr-310 when thats available in java 8.
   * Not sure whether these patterns will still work then, so use this formatter at the risk of having to
   * change it eventually. OTOH, its likely that the same functionality will be present in jsr-310.
   * <p>
   * The pattern syntax is mostly compatible with java.text.SimpleDateFormat -
   * time zone names cannot be parsed and a few more symbols are supported.
   * All ASCII letters are reserved as pattern letters, which are defined as follows:
   * </p>
   * 
   * <pre>
   * Symbol  Meaning                      Presentation  Examples
   * ------  -------                      ------------  -------
   * G       era                          text          AD
   * C       century of era (&gt;=0)         number        20
   * Y       year of era (&gt;=0)            year          1996
   *
   * x       weekyear                     year          1996
   * w       week of weekyear             number        27
   * e       day of week                  number        2
   * E       day of week                  text          Tuesday; Tue
   *
   * y       year                         year          1996
   * D       day of year                  number        189
   * M       month of year                month         July; Jul; 07
   * d       day of month                 number        10
   *
   * a       halfday of day               text          PM
   * K       hour of halfday (0~11)       number        0
   * h       clockhour of halfday (1~12)  number        12
   *
   * H       hour of day (0~23)           number        0
   * k       clockhour of day (1~24)      number        24
   * m       minute of hour               number        30
   * s       second of minute             number        55
   * S       fraction of second           number        978
   *
   * z       time zone                    text          Pacific Standard Time; PST
   * Z       time zone offset/id          zone          -0800; -08:00; America/Los_Angeles
   *
   * '       escape for text              delimiter
   * ''      single quote                 literal       '
   * </pre>
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
