/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.calendar;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test {@link ucar.nc2.calendar.UdunitCalendarDateParser} */
public class TestUdunitCalendarDateParser {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testParsingNanos() {
    claimSameString("1992-10-08T15:15:42.123Z");
    claimSameString("1992-10-08T15:15:42.123456Z");
    claimSameString("1992-10-08T15:15:42.123456789Z");
  }

  @Test
  public void testParsingTooManyNanos() {
    claimString("1992-10-08T15:15:42.1234567890Z", "1992-10-08T15:15:42.123456789Z");
    claimString("1992-10-08T15:15:42.12345678901234567890Z", "1992-10-08T15:15:42.123456789Z");
  }

  @Test
  public void testBadWithException() {
    claimBadWithException("1143848700");
  }

  @Test
  public void testW3cIso() {
    claimGood("1997");
    claimGood("1997-07");
    claimGood("1997-07-16");
    claimGood("1997-07-16T19:20+01:00");
    claimGood("1997-07-16T19:20:30+01:00");
  }

  @Test
  public void testBasicFormatIso() {
    claimGood("19500101T000000Z"); // from https://github.com/Unidata/thredds/issues/772
    claimGood("199707");
    claimGood("19970716");
    claimGood("19970716T1920");
    claimGood("19970716T192030");
    claimGood("19970716T192030.1");
    claimGood("19970716T1920+01:00");
    claimGood("19970716T192030+0100");
    claimGood("19970716T192030+01");
    claimGood("19970716T192030.1+0100");
    claimGood("19970716T192030Z");
    claimGood("19970716T192030.1Z");
    // these should fail
    claimBad("19970716T192030.1UTC");
    claimBad("19501"); // fail because ambiguous
    claimBad("1950112"); // fail because ambiguous
    claimBad("19501120T121"); // fail because ambiguous
    claimBad("19501120T12151"); // fail because ambiguous
  }

  @Test
  public void testChangeoverDate() {
    claimGood("1997-01-01");
    claimGood("1582-10-16");
    claimGood("1582-10-15");
    claimGood("1582-10-01");
    claimGood("1582-10-02");
    claimGood("1582-10-03");
    claimGood("1582-10-04");
    // testBase("1582-10-14"); // fail
    // testBase("1582-10-06"); // fail
  }


  // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
  @Test
  public void testUdunits() {
    claimGood("1992-10-8 15:15:42.5 -6:00");
    claimGood("1992-10-8 15:15:42.5 +6");
    claimGood("1992-10-8 15:15:42.534");
    claimGood("1992-10-8 15:15:42");
    claimGood("1992-10-8 15:15");
    claimGood("1992-10-8 15");
    claimGood("1992-10-8T15");
    claimGood("1992-10-8");
    claimGood("199-10-8");
    claimGood("19-10-8");
    claimGood("1-10-8");
    claimGood("+1101-10-8");
    claimGood("-1101-10-8");
    claimGood("1992-10-8T7:00 -6:00");
    claimGood("1992-10-8T7:00 +6:00");
    claimGood("1992-10-8T7 -6:00");
    claimGood("1992-10-8T7 +6:00");
    claimGood("1992-10-8 7 -6:00");
    claimGood("1992-10-8 7 +6:00");
  }

  private void claimString(String s, String expected) {
    CalendarDate result = CalendarDate.fromUdunitIsoDate(null, s).orElseThrow();
    System.out.printf("%s == %s%n", s, result);
    assertThat(result.toString()).isEqualTo(expected);
  }

  private void claimSameString(String s) {
    CalendarDate result = CalendarDate.fromUdunitIsoDate(null, s).orElseThrow();
    System.out.printf("%s == %s%n", s, result);
    assertThat(result.toString()).isEqualTo(s);
  }

  private void claimGood(String s) {
    CalendarDate result = CalendarDate.fromUdunitIsoDate(null, s).orElseThrow();
    System.out.printf("%s == %s%n", s, result);
  }

  private void claimBad(String s) {
    System.out.printf("%s should fail%n", s);
    assertThat(CalendarDate.fromUdunitIsoDate(null, s).isEmpty());
  }

  private void claimBadWithException(String s) {
    System.out.printf("%s should fail%n", s);
    assertThrows(RuntimeException.class, () -> CalendarDate.fromUdunitIsoDate(null, s));
  }

  @Test
  public void testCalendarDateFormatter() {
    CalendarDate cd = CalendarDate.present();

    System.out.printf("%s%n", cd);
    System.out.printf("toDateTimeStringISO=%s%n", CalendarDateFormatter.toDateTimeStringISO(cd));
    System.out.printf("       toDateString=%s%n", CalendarDateFormatter.toDateString(cd));
    System.out.printf("===============================%n");
    Date d = cd.toDate();
    System.out.printf("cd.toDate()=%s%n", d);

    SimpleDateFormat udunitDF = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
    udunitDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    udunitDF.applyPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'");
    System.out.printf("           udunitDF=%s%n", udunitDF.format(d));
  }

  @Test
  public void testParse() {
    String base = "2012-04-27T16:00:00+0200";
    CalendarDate basedate = CalendarDate.fromUdunitIsoDate(null, base).orElseThrow();

    UdunitCalendarDateParser parsed = UdunitCalendarDateParser.parseUnitString("days since " + base).orElseThrow();
    CalendarDate parsedDate =
        CalendarDate.of(null, parsed.flds.year, parsed.flds.monthOfYear, parsed.flds.dayOfMonth, parsed.flds.hourOfDay,
            parsed.flds.minuteOfHour, parsed.flds.secondOfMinute, parsed.flds.nanoOfSecond, parsed.flds.zoneId);
    assertThat(parsedDate).isEqualTo(basedate);
    assertThat(parsed.periodField).isEqualTo(CalendarPeriod.Field.Day);
    assertThat(parsed.period).isEqualTo(CalendarPeriod.of(1, CalendarPeriod.Field.Day));
    assertThat(parsed.isCalendarField).isFalse();

    parsed = UdunitCalendarDateParser.parseUnitString("calendar months since " + base).orElseThrow();
    parsedDate =
        CalendarDate.of(null, parsed.flds.year, parsed.flds.monthOfYear, parsed.flds.dayOfMonth, parsed.flds.hourOfDay,
            parsed.flds.minuteOfHour, parsed.flds.secondOfMinute, parsed.flds.nanoOfSecond, parsed.flds.zoneId);
    assertThat(parsedDate).isEqualTo(basedate);
    assertThat(parsed.periodField).isEqualTo(CalendarPeriod.Field.Month);
    assertThat(parsed.period).isEqualTo(CalendarPeriod.of(1, CalendarPeriod.Field.Month));
    assertThat(parsed.isCalendarField).isTrue();

    assertThat(UdunitCalendarDateParser.parseUnitString("days before 2012-04-27T16:00:00+0200")).isEmpty();
    assertThat(UdunitCalendarDateParser.parseUnitString("calendar days before 2012-04-27T16:00:00+0200")).isEmpty();
    assertThat(UdunitCalendarDateParser.parseUnitString("dates since 2012-04-27T16:00:00+0200")).isEmpty();
    assertThat(UdunitCalendarDateParser.parseUnitString("calendar dates since 2012-04-27T16:00:00+0200")).isEmpty();
  }

  @Test
  public void testISOparsing() {
    testSO("1992-10-08T15:15:42.123Z");
    testSO("1992-10-08T15:15:42.123456Z");
    testSO("1992-10-08T15:15:42.123456789Z");
    // testSO("1992-10-08T15:15:42.1234567890Z", "1992-10-08T15:15:42.123456789Z");
    // testSO("1992-10-08T15:15:42.12345678901234567890Z", "1992-10-08T15:15:42.123456789Z");
    testSO("1997");
    testSO("1997-07");
    testSO("1997-07-16");
    testSO("1997-07-16T19:20+01:00");
    testSO("1997-07-16T19:20:30+01:00");
    testSO("1997-01-01");
    testSO("1582-10-16");
    testSO("1582-10-15");
    testSO("1582-10-01");
    testSO("1582-10-02");
    testSO("1582-10-03");
    testSO("1582-10-04");
    testSO("1582-10-14");
    testSO("1582-10-06");
    // testSO("1992-10-08T15:15:42.5-6:00");
    // testSO("1992-10-08T15:15:42.5+6");
    testSO("1992-10-08T15:15:42.534");
    testSO("1992-10-08T15:15:42");
    testSO("1992-10-08T15:15");
    testSO("1992-10-08T15");
    testSO("1992-10-08T15");
    testSO("1992-10-08");
    // testSO("199-10-80");
    // testSO("19-10-08");
    // testSO("1-10-08");
    // testSO("+1101-10-08");
    testSO("-1101-10-08");
    testSO("1992-10-08T07:00-06:00");
    testSO("1992-10-08T07:00+06:00");
    testSO("1992-10-08T07-06:00");
    testSO("1992-10-08T07+06:00");
  }

  private void testSO(String s) {
    System.out.printf("'%s': ", s);
    CalendarDate isoDate = CalendarDate.fromUdunitIsoDate(null, s).orElseThrow();
    assertThat(isoDate).isInstanceOf(CalendarDateIso.class);
    OffsetDateTime isoOffset = ((CalendarDateIso) isoDate).dateTime();

    OffsetDateTime resultSO = parseSO(s);
    System.out.printf("%s == %s%n", resultSO, isoOffset);
    assertThat(resultSO).isEqualTo(isoOffset);
  }

  private static DateTimeFormatter dtf = new DateTimeFormatterBuilder().appendPattern(
      "uuuu[-MM[-dd]]['T'HH[:mm[:ss[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]]]][XXX]")
      .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1).parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
      .parseDefaulting(ChronoField.HOUR_OF_DAY, 0).parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
      .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0).parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
      .parseDefaulting(ChronoField.OFFSET_SECONDS, 0).toFormatter();

  private OffsetDateTime parseSO(String s) {
    try {
      OffsetDateTime result = OffsetDateTime.parse(s, dtf);
      return result.withOffsetSameInstant(ZoneOffset.UTC);

    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

}
