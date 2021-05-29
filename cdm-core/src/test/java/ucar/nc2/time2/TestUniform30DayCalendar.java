/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

import org.junit.Test;
import ucar.nc2.time2.chrono.Uniform30DayDate;

import java.time.Instant;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoField;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/** Test {@link ucar.nc2.time2.chrono.Uniform30DayChronology} */
public class TestUniform30DayCalendar {
  private static final String calendarName = Calendar.uniform30day.toString();

  @Test
  public void testBasics() {
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-02T16:45:56").orElseThrow();
    assertThat(cd.getCalendar()).isEqualTo(Calendar.uniform30day);

    assertThat(cd).isInstanceOf(CalendarDateChrono.class);
    CalendarDateChrono chronoDate = (CalendarDateChrono) cd;
    ChronoZonedDateTime<?> chronoDateTime = chronoDate.chronoDateTime();
    assertThat(chronoDateTime.toLocalDate()).isInstanceOf(Uniform30DayDate.class);

    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(16);

    cd = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-02").orElseThrow();
    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(0);
  }

  @Test
  public void testFailures() {
    assertThat(CalendarDate.fromUdunitIsoDate(calendarName, null)).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate(calendarName, "")).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate(calendarName, "bad")).isEmpty();
  }

  @Test
  public void testFromUdunitIsoDate() {
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-01").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T00:00Z");

    cd = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-01T12:34").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34Z");

    cd = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-01T12:34:56").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34:56Z");

    cd = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-01T12:34:56.123").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34:56.123Z");
  }

  @Test
  public void testOrdering() {
    CalendarDate first = CalendarDate.of(Calendar.uniform30day, 1999, 1, 1, 1, 1, 1, 1, null);
    CalendarDate second = CalendarDate.of(Calendar.uniform30day, 1999, 1, 2, 1, 1, 1, 1, null);
    assertThat(first.isBefore(second)).isTrue();
    assertThat(first.isAfter(second)).isFalse();
    assertThat(second.isBefore(first)).isFalse();
    assertThat(second.isAfter(first)).isTrue();

    assertThat(first.isBefore(first)).isFalse();
    assertThat(first.isAfter(first)).isFalse();
  }

  @Test
  public void testGetFields() {
    CalendarDate cd = CalendarDate.of(Calendar.uniform30day, 1999, 1, 2, 3, 4, 5, 6, null);

    assertThat(cd.getCalendar()).isEqualTo(Calendar.uniform30day);
    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(3);

    assertThat(cd.getFieldValue(CalendarPeriod.Field.Year)).isEqualTo(1999);
    assertThat(cd.getFieldValue(CalendarPeriod.Field.Month)).isEqualTo(1);
    assertThat(cd.getFieldValue(CalendarPeriod.Field.Day)).isEqualTo(2);
    assertThat(cd.getFieldValue(CalendarPeriod.Field.Hour)).isEqualTo(3);
    assertThat(cd.getFieldValue(CalendarPeriod.Field.Minute)).isEqualTo(4);
    assertThat(cd.getFieldValue(CalendarPeriod.Field.Second)).isEqualTo(5);
    assertThat(cd.getFieldValue(CalendarPeriod.Field.Millisec)).isEqualTo(0);
  }

  @Test
  public void testAdd() {
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-01").orElseThrow();

    CalendarDate result = baseDate.add(CalendarPeriod.of(300, CalendarPeriod.Field.Millisec));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:00:00.300Z");
    assertThat(result.compareTo(baseDate)).isGreaterThan(0);
    assertThat(result.equals(baseDate)).isFalse();
    assertThat(result.isAfter(baseDate)).isTrue();
    assertThat(result.isBefore(baseDate)).isFalse();

    result = baseDate.add(CalendarPeriod.of(3, CalendarPeriod.Field.Second));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:00:03Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Minute));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:30Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Hour));
    assertThat(result.toString()).isEqualTo("1950-01-02T06:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Day));
    assertThat(result.toString()).isEqualTo("1950-02-01T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1952-07-01T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Year));
    assertThat(result.toString()).isEqualTo("1980-01-01T00:00Z");
  }

  @Test
  public void testSubtract() {
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(calendarName, "1950-01-01").orElseThrow();

    CalendarDate result = baseDate.add(-1, CalendarPeriod.of(300, CalendarPeriod.Field.Millisec));
    assertThat(result.toString()).isEqualTo("1949-12-30T23:59:59.700Z");
    assertThat(result.compareTo(baseDate)).isEqualTo(-1);
    assertThat(result.equals(baseDate)).isFalse();
    assertThat(result.isAfter(baseDate)).isFalse();
    assertThat(result.isBefore(baseDate)).isTrue();

    result = baseDate.add(-1, CalendarPeriod.of(300, CalendarPeriod.Field.Second));
    assertThat(result.toString()).isEqualTo("1949-12-30T23:55Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Minute));
    assertThat(result.toString()).isEqualTo("1949-12-30T23:30Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Hour));
    assertThat(result.toString()).isEqualTo("1949-12-29T18:00Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Day));
    assertThat(result.toString()).isEqualTo("1949-12-01T00:00Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1947-07-01T00:00Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Year));
    assertThat(result.toString()).isEqualTo("1920-01-01T00:00Z");
  }

  @Test
  public void testTruncate() {
    CalendarDate baseDate = CalendarDate.of(Calendar.uniform30day, 2000, 1, 2, 3, 4, 5, 0, null);

    CalendarDate result = baseDate.truncate(CalendarPeriod.Field.Millisec);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:04:05Z");

    result = baseDate.truncate(CalendarPeriod.Field.Second);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:04:05Z");

    result = baseDate.truncate(CalendarPeriod.Field.Minute);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:04Z");

    result = baseDate.truncate(CalendarPeriod.Field.Hour);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:00Z");

    result = baseDate.truncate(CalendarPeriod.Field.Day);
    assertThat(result.toString()).isEqualTo("2000-01-02T00:00Z");

    result = baseDate.truncate(CalendarPeriod.Field.Month);
    assertThat(result.toString()).isEqualTo("2000-01-01T00:00Z");

    result = baseDate.truncate(CalendarPeriod.Field.Year);
    assertThat(result.toString()).isEqualTo("2000-01-01T00:00Z");
  }

  @Test
  public void testSince() {
    CalendarDate baseDate = CalendarDate.of(Calendar.uniform30day, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(Calendar.uniform30day, 2001, 2, 3, 4, 5, 6, 1000000, null);

    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Year)).isEqualTo(1);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(13);
    long days = 360 + 30 + 1;
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Day)).isEqualTo(days);
    long hours = days * 24 + 1;
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Hour)).isEqualTo(hours);
    long minutes = hours * 60 + 1;
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Minute)).isEqualTo(minutes);
    long secs = minutes * 60 + 1;
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Second)).isEqualTo(secs);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Millisec)).isEqualTo(secs * 1000 + 1);

    CalendarDate diffDate2 = CalendarDate.of(Calendar.uniform30day, 2001, 1, 2, 3, 4, 5, 0, null);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Year)).isEqualTo(1);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(12);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Day)).isEqualTo(360);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Hour)).isEqualTo(360 * 24);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Minute)).isEqualTo(360 * 24 * 60);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Second)).isEqualTo(360 * 24 * 60 * 60);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Millisec)).isEqualTo(31104000000L);

    CalendarDate diffDate3 = CalendarDate.of(Calendar.uniform30day, 2000, 1, 2, 3, 4, 5, 0, null);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Year)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Day)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Hour)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Minute)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Second)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Millisec)).isEqualTo(0);
  }

  @Test
  public void testSinceFail() {
    CalendarDate baseDate = CalendarDate.of(Calendar.uniform30day, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(Calendar.uniform30day, 2001, 2, 11, 4, 5, 6, 1000000, null);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(13);
  }

  @Test
  public void testDifferenceInMsecs() {
    CalendarDate baseDate = CalendarDate.of(Calendar.uniform30day, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(Calendar.uniform30day, 2000, 1, 3, 4, 5, 6, 999, null);
    assertThat(diffDate.getDifferenceInMsecs(baseDate)).isEqualTo(90061000);
    assertThat(diffDate.getDifferenceInMsecs(baseDate)).isEqualTo((25 * 3600 + 60 + 1) * 1000);
  }

  @Test
  public void testTimeZone() {
    String isoCET = "2012-04-27T16:00:00+0200";
    CalendarDate cetDate = CalendarDate.fromUdunitIsoDate(calendarName, isoCET).orElseThrow();
    String isoMST = "2012-04-27T08:00:00-0600";
    CalendarDate mstDate = CalendarDate.fromUdunitIsoDate(calendarName, isoMST).orElseThrow();
    String isoUTC = "2012-04-27T14:00Z";
    CalendarDate utcDate = CalendarDate.fromUdunitIsoDate(calendarName, isoUTC).orElseThrow();
    assertThat(mstDate.toString()).isEqualTo(cetDate.toString());
    assertThat(mstDate.toString()).isEqualTo(utcDate.toString());
  }

  @Test
  public void testTimeZone2() {
    String isoMST = "2012-04-27T08:00:00-0600";
    CalendarDate cdate = CalendarDate.fromUdunitIsoDate(calendarName, isoMST).orElseThrow();
    System.out.printf("result = %s%n", cdate.toString());
    assertThat(cdate.toString()).isEqualTo("2012-04-27T14:00Z");
  }

  // @Test
  public void testInstant() {
    String isoMST = "2012-04-27T08:00:00-0600";
    CalendarDate cdate = CalendarDate.fromUdunitIsoDate(calendarName, isoMST).orElseThrow();
    assertThat(cdate.toString()).isEqualTo("2012-04-27T14:00Z");

    Instant instant = cdate.toInstant();
    CalendarDate cdate2 = CalendarDate.of(Calendar.uniform30day, instant); // LOOK fails

    assertThat(cdate.toString()).isEqualTo(cdate2.toString());
    assertThat(cdate.hashCode()).isEqualTo(cdate2.hashCode());
    assertThat(cdate).isEqualTo(cdate2);
  }

  @Test
  public void testInstantIso() {
    String isoMST = "2012-04-27T08:00:00-0600";
    CalendarDate cdate = CalendarDate.fromUdunitIsoDate(null, isoMST).orElseThrow();

    Instant instant = cdate.toInstant();
    CalendarDate cdate2 = CalendarDate
        .of(instant.getLong(ChronoField.INSTANT_SECONDS) * 1000 + instant.get(ChronoField.NANO_OF_SECOND) / 1000000);

    assertThat(cdate.toString()).isEqualTo("2012-04-27T14:00Z");
    assertThat(cdate.toString()).isEqualTo(cdate2.toString());
    assertThat(cdate.hashCode()).isEqualTo(cdate2.hashCode());
    assertThat(cdate).isEqualTo(cdate2);

    cdate2 = CalendarDate.of(null, instant);
    assertThat(cdate.toString()).isEqualTo(cdate2.toString());
    assertThat(cdate.hashCode()).isEqualTo(cdate2.hashCode());
    assertThat(cdate).isEqualTo(cdate2);
  }

}
