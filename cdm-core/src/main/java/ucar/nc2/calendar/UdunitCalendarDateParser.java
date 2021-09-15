/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import javax.annotation.concurrent.Immutable;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * Parse extended Udunit date unit strings: "[CALENDAR] unit SINCE date".
 * 
 * @see CalendarPeriod#fromUnitString
 */
@Immutable
class UdunitCalendarDateParser {
  private static final String datePatternString = "([\\+\\-?\\d]+)([ t]([\\.\\:?\\d]*)([ \\+\\-]\\S*)?z?)?$";
  private static final Pattern datePattern = Pattern.compile(datePatternString);
  private static final String udunitPatternString = "(\\w*)\\s*since\\s*" + datePatternString;
  private static final Pattern udunitPattern = Pattern.compile(udunitPatternString);
  static final String byCalendarString = "calendar ";

  /**
   * Parse extended udunit date unit string: "[CALENDAR] unit SINCE date".
   *
   * <pre>
     unit = "millisecond" | "millisec" | "msec" | "ms" |
           "second" | "sec" | "s" |
           "minute" | "min" |
           "hour" | "hr" | "h" |
           "day" | "d" |
           "year" | "yr"
           (plural forms allowed)
  
     date = ISO formatted String with some extensions to allow backwards compatibility with udunits, see parseUdunitDate().
   * </pre>
   *
   * @param udunitString the extended udunit date unit string.
   * @return the fields of the parsed string, or empty if invalid.
   * @see #parseUdunitIsoDate
   */
  static Optional<UdunitCalendarDateParser> parseUnitString(String udunitString) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(udunitString));

    udunitString = udunitString.trim();
    udunitString = udunitString.toLowerCase();

    boolean isCalendarField = udunitString.startsWith(byCalendarString);
    if (isCalendarField) {
      udunitString = udunitString.substring(byCalendarString.length()).trim();
    }

    Matcher m = udunitPattern.matcher(udunitString);
    if (!m.matches()) {
      return Optional.empty();
    }

    String unitString = m.group(1);
    CalendarPeriod period = CalendarPeriod.of(unitString);
    if (period == null) {
      return Optional.empty();
    }
    CalendarPeriod.Field periodField = CalendarPeriod.fromUnitString(unitString);

    int pos = udunitString.indexOf("since");
    String refDate = udunitString.substring(pos + 5);
    Optional<ComponentFields> fields = parseUdunitIsoDate(refDate);

    return fields.map(f -> new UdunitCalendarDateParser(period, periodField, isCalendarField, f));
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Parse udunit ISO date string into a ZonedDateTime.
   *
   * The CDM uses the W3C profile of ISO 8601 formatting for calendar dates, with changes to allow backwards
   * compatibility for legacy uduinits. We recommend using strict ISO 8601 for new code.
   *
   * @see <a href="https://docs.unidata.ucar.edu/netcdf-java/current/userguide/cdm_calendar_date_time_ref.html">CDM
   *      dates</a>
   *
   *      <pre>
   * The formats defined by the W3C profile of ISO 8601 are as follows.
   * Exactly the components shown here must be present, with exactly this punctuation. Note that the T appears literally
   * in the string, to indicate the beginning of the time element, as specified in ISO 8601.
   * <pre>
   * Year:
   *   YYYY (eg 1997)
   * Year and month:
   *   YYYY-MM (eg 1997-07)
   * Complete date:
   *   YYYY-MM-DD (eg 1997-07-16)
   * Complete date plus hours and minutes:
   *   YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
   * Complete date plus hours, minutes and seconds:
   *   YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
   * Complete date plus hours, minutes, seconds and a decimal fraction of a second
   *   YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
   *
   * where:
   *
   * YYYY = four-digit year
   * MM   = two-digit month (01=January, etc.)
   * DD   = two-digit day of month (01 through 31)
   * hh   = two digits of hour (00 through 23) (am/pm NOT allowed)
   * mm   = two digits of minute (00 through 59)
   * ss   = two digits of second (00 through 59)
   * s    = one or more digits representing a decimal fraction of a second
   * TZD  = time zone designator (Z or +hh:mm or -hh:mm)
   *
   * but with the following differences, to allow backwards compatibility with UDUNITS:
   *
   * You may use a space ( ) instead of the T
   * The year may be preceded by a + (ignored) or a - (makes the date BCE)
   * The date part uses a - delimiter instead of a fixed number of digits for each field
   * The time part uses a : delimiter instead of a fixed number of digits for each field
   * The time zone designator may be Z, UTC, or GMT (case insensitive) or +hh:mm or -hh:mm
   * The time zone may be omitted, and then UTC is assumed.
   *      </pre>
   *
   * @param udunitDate the udunit date string.
   * @return the parsed string, or empty if invalid.
   */
  static Optional<ComponentFields> parseUdunitIsoDate(String udunitDate) {
    udunitDate = udunitDate.trim();
    udunitDate = udunitDate.toLowerCase();

    Matcher m = datePattern.matcher(udunitDate);
    if (!m.matches()) {
      return Optional.empty();
    }

    String dateString = m.group(1);
    String timeString = m.group(3);
    String zoneString = m.group(4);

    // Set the defaults for any values that are not specified
    ComponentFields flds = new ComponentFields();

    try {
      boolean isMinus = false;
      if (dateString.startsWith("-")) {
        isMinus = true;
        dateString = dateString.substring(1);
      } else if (dateString.startsWith("+")) {
        dateString = dateString.substring(1);
      }

      if (dateString.contains("-")) {
        StringTokenizer dateTokenizer = new StringTokenizer(dateString, "-");
        if (dateTokenizer.hasMoreTokens()) {
          flds.year = Integer.parseInt(dateTokenizer.nextToken());
        }
        if (dateTokenizer.hasMoreTokens()) {
          flds.monthOfYear = Integer.parseInt(dateTokenizer.nextToken());
        }
        if (dateTokenizer.hasMoreTokens()) {
          flds.dayOfMonth = Integer.parseInt(dateTokenizer.nextToken());
        }
      } else {
        int dateLength = dateString.length();
        if (dateLength % 2 != 0) {
          throw new IllegalArgumentException(dateString + " is ambiguous. Cannot parse uneven "
              + "length date strings when no date delimiter is used.");
        } else {
          // dateString length must be even - only four digit year, two digit month, and
          // two digit day values are allowed when there is no date delimiter.
          if (dateLength > 3) {
            flds.year = Integer.parseInt(dateString.substring(0, 4));
          }
          if (dateLength > 5) {
            flds.monthOfYear = Integer.parseInt(dateString.substring(4, 6));
          }
          if (dateLength > 7) {
            flds.dayOfMonth = Integer.parseInt(dateString.substring(6, 8));
          }
        }
      }

      // Parse the time if present
      SecNano secNano = null;
      if (timeString != null && !timeString.isEmpty()) {
        if (timeString.contains(":")) {
          StringTokenizer timeTokenizer = new StringTokenizer(timeString, ":");
          if (timeTokenizer.hasMoreTokens()) {
            flds.hourOfDay = Integer.parseInt(timeTokenizer.nextToken());
          }
          if (timeTokenizer.hasMoreTokens()) {
            flds.minuteOfHour = Integer.parseInt(timeTokenizer.nextToken());
          }
          if (timeTokenizer.hasMoreTokens()) {
            secNano = parseSecondsToken(timeTokenizer.nextToken());
          }
        } else {
          int timeLengthNoSubseconds = timeString.length();
          // possible this contains a seconds value with subseconds (i.e. 25.125 seconds)
          // since the seconds value can be a Double, let's check check the length of the
          // time, without the subseconds (if they exist)
          if (timeString.contains(".")) {
            timeLengthNoSubseconds = timeString.split("\\.")[0].length();
          }
          // Ok, so this is a little tricky.
          // First: A udunit date of 1992-10-8t7 is valid. We want to make sure this still work, so
          // there is a special case of timeString.length() == 1;
          //
          // Second: We have the length of the timeString, without
          // any subseconds. Given that the values for hour, minute, and second must be two
          // digit numbers, the length of the timeString, without subseconds, should be even.
          // However, if someone has encoded time as hhms, there is no way we will be able to tell.
          // So, this is the best we can do to ensure we are parsing the time properly.
          // Known failure here: hhms will be interpreted as hhmm, but hhms is not following iso, and
          // a bad idea to use anyway.
          if (timeLengthNoSubseconds == 1) {
            flds.hourOfDay = Integer.parseInt(timeString);
          } else if (timeLengthNoSubseconds % 2 != 0) {
            throw new IllegalArgumentException(timeString + " is ambiguous. Cannot parse uneven "
                + "length time strings (ignoring subseconds) when no time delimiter is used.");
          } else {
            // dateString length must be even - only four digit year, two digit month, and
            // two digit day values are allowed when there is no date delimiter.
            if (timeString.length() > 1) {
              flds.hourOfDay = Integer.parseInt(timeString.substring(0, 2));
            }
            if (timeString.length() > 3) {
              flds.minuteOfHour = Integer.parseInt(timeString.substring(2, 4));
            }
            if (timeString.length() > 5) {
              secNano = parseSecondsToken(timeString.substring(4));
            }
          }
        }
        if (secNano != null) {
          flds.secondOfMinute = secNano.secs;
          flds.nanoOfSecond = secNano.nanos;
        }
      }

      if (isMinus) {
        flds.year *= -1;
      }

      // kludge to deal with legacy files using year 0. // 10/10/2013 jcaron
      // if ((year == 0) && (calt == Calendar.gregorian)) {
      // calt = Calendar.proleptic_gregorian;
      // }

      // if (year < -292275054 || year > 292278993)
      // throw new IllegalArgumentException(" incorrect date specification = " + iso);

      // Parse the time zone if present
      if (zoneString != null) {
        zoneString = zoneString.trim();
        if (!zoneString.isEmpty() && !zoneString.equalsIgnoreCase("Z") && !zoneString.equalsIgnoreCase("UTC")
            && !zoneString.equalsIgnoreCase("GMT")) {
          isMinus = false;
          if (zoneString.startsWith("-")) {
            isMinus = true;
            zoneString = zoneString.substring(1);
          } else if (zoneString.startsWith("+")) {
            zoneString = zoneString.substring(1);
          }

          // allow 01:00, 1:00, 01 or 0100
          int hourOffset;
          int minuteOffset = 0;
          int posColon = zoneString.indexOf(':');
          if (posColon > 0) {
            String hourS = zoneString.substring(0, posColon);
            String minS = zoneString.substring(posColon + 1);
            hourOffset = Integer.parseInt(hourS);
            minuteOffset = Integer.parseInt(minS);

          } else { // no colon - assume 2 digit hour, optional minutes
            if (zoneString.length() > 2) {
              String hourS = zoneString.substring(0, 2);
              String minS = zoneString.substring(2);
              hourOffset = Integer.parseInt(hourS);
              minuteOffset = Integer.parseInt(minS);
            } else {
              hourOffset = Integer.parseInt(zoneString);
            }
          }
          if (isMinus) {
            hourOffset = -hourOffset;
          }

          flds.zoneId = ZoneOffset.ofHoursMinutes(hourOffset, minuteOffset);
        }
      }

      // default time zone
      return Optional.of(flds);

    } catch (Throwable e) {
      return Optional.empty();
    }
  }

  private static class SecNano {
    int secs;
    int nanos;

    public SecNano(int secs, int nanos) {
      this.secs = secs;
      this.nanos = nanos;
    }
  }

  private static SecNano parseSecondsToken(String secToken) {
    int sec = 0;
    int nano = 0;
    StringTokenizer secTokenizer = new StringTokenizer(secToken, ".");
    if (secTokenizer.hasMoreTokens()) {
      sec = Integer.parseInt(secTokenizer.nextToken());
    }
    if (secTokenizer.hasMoreTokens()) {
      String nanoToken = secTokenizer.nextToken();
      if (nanoToken.length() > 9) {
        nanoToken = nanoToken.substring(0, 9);
      }
      nano = Integer.parseInt(nanoToken);
      int ndigits = nanoToken.length();
      int scale = tenToPower(9 - ndigits);
      nano *= scale;
    }
    return new SecNano(sec, nano);
  }

  private static int tenToPower(int power) {
    int result = 1;
    for (int p = 0; p < power; p++) {
      result *= 10;
    }
    return result;
  }


  // Now convert to the UTC time zone, retaining the same instant
  // return Optional.of(dateTimeWithZone.withOffsetSameInstant(ZoneOffset.UTC));
  static class ComponentFields {
    int year = 0;
    int monthOfYear = 1;
    int dayOfMonth = 1;
    int hourOfDay = 0;
    int minuteOfHour = 0;
    int secondOfMinute = 0;
    int nanoOfSecond = 0;
    ZoneOffset zoneId = ZoneOffset.UTC;

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      ComponentFields that = (ComponentFields) o;
      return year == that.year && monthOfYear == that.monthOfYear && dayOfMonth == that.dayOfMonth
          && hourOfDay == that.hourOfDay && minuteOfHour == that.minuteOfHour && secondOfMinute == that.secondOfMinute
          && nanoOfSecond == that.nanoOfSecond && zoneId.equals(that.zoneId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond, zoneId);
    }

    @Override
    public String toString() {
      return "ComponentFields{" + "year=" + year + ", monthOfYear=" + monthOfYear + ", dayOfMonth=" + dayOfMonth
          + ", hourOfDay=" + hourOfDay + ", minuteOfHour=" + minuteOfHour + ", secondOfMinute=" + secondOfMinute
          + ", nanoOfSecond=" + nanoOfSecond + ", zoneId=" + zoneId + '}';
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  final CalendarPeriod period;
  final CalendarPeriod.Field periodField;
  final boolean isCalendarField;
  final ComponentFields flds;

  private UdunitCalendarDateParser(CalendarPeriod period, CalendarPeriod.Field periodField, boolean isCalendarField,
      ComponentFields flds) {
    this.periodField = periodField;
    this.period = period;
    this.isCalendarField = isCalendarField;
    this.flds = flds;
  }
}

/*
 * see https://docs.unidata.ucar.edu/netcdf-java/current/userguide/cdm_calendar_date_time_ref.html
 * <pre>
 * The CDM uses the W3C profile of ISO 8601 formatting for reading and writing calendar dates:
 * 
 * The formats defined by the W3C profile of ISO 8601 are as follows.
 * Exactly the components shown here must be present, with exactly this punctuation. Note that the T appears literally
 * in the string, to indicate the beginning of the time element, as specified in ISO 8601.
 * 
 * Year:
 * YYYY (eg 1997)
 * Year and month:
 * YYYY-MM (eg 1997-07)
 * Complete date:
 * YYYY-MM-DD (eg 1997-07-16)
 * Complete date plus hours and minutes:
 * YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
 * Complete date plus hours, minutes and seconds:
 * YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
 * Complete date plus hours, minutes, seconds and a decimal fraction of a second
 * YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
 * where:
 * 
 * YYYY = four-digit year
 * MM = two-digit month (01=January, etc.)
 * DD = two-digit day of month (01 through 31)
 * hh = two digits of hour (00 through 23) (am/pm NOT allowed)
 * mm = two digits of minute (00 through 59)
 * ss = two digits of second (00 through 59)
 * s = one or more digits representing a decimal fraction of a second
 * TZD = time zone designator (Z or +hh:mm or -hh:mm)
 * 
 * but with the following differences, to allow backwards compatibility with UDUNITS:
 * 
 * You may use a space ( ) instead of the T
 * The year may be preceded by a + (ignored) or a - (makes the date BCE)
 * The date part uses a - delimiter instead of a fixed number of digits for each field
 * The time part uses a : delimiter instead of a fixed number of digits for each field
 * The time zone designator may be Z, UTC, or GMT (case insensitive) or +hh:mm or -hh:mm
 * The time zone may be omitted, and then UTC is assumed.
 * </pre>
 * 
 * Also see https://www.unidata.ucar.edu/software/udunits/udunits-2.1.24/udunits2lib.html#Grammar
 * <pre>
 * TIMESTAMP: <year> (<month> <day>?)? "T" <hour> (<minute> <second>?)?
 * <year>: [+-]?[0-9]{1,4}
 * <month>: "0"?[1-9]|1[0-2]
 * <day>: "0"?[1-9]|[1-2][0-9]|"30"|"31"
 * <hour>: [+-]?[0-1]?[0-9]|2[0-3]
 * <minute>: [0-5]?[0-9]
 * <second>: (<minute>|60) (\.[0-9]*)?
 * </pre>
 *
 * The acceptable units for time are listed in the udunits.dat file. The most commonly used of these strings
 * includes day (d), hour (hr, h), minute (min) and second (sec, s). Plural forms are also acceptable.
 * The reference time string (appearing after the identifier since) may include date alone; date and time;
 * or date, time, and time zone. The reference time is required.
 * 
 * A reference time in year 0 has a special meaning (see Section 7.4, Climatological Statistics).
 *
 * Note: if the time zone is omitted the default is UTC, and if both time and time zone are omitted the default is
 * 00:00:00 UTC.
 *
 * We recommend that the unit year be used with caution. The Udunits package defines a year to be exactly 365.242198781
 * days
 * (the interval between 2 successive passages of the sun through vernal equinox). It is not a calendar year.
 * Udunits includes the following definitions for years: a common_year is 365 days, a leap_year is 366 days, a
 * Julian_year is 365.25 days, and a Gregorian_year is 365.2425 days.
 *
 * For similar reasons the unit month, which is defined in udunits.dat to be exactly year/12, should also be used with
 * caution.
 *
 */
