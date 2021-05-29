/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

import org.junit.Test;
import ucar.nc2.time2.chrono.LeapYearDate;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoZonedDateTime;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/** Test {@link ucar.nc2.time2.chrono.Uniform30DayChronology} */
public class TestNoleapYearCalendar {
  private static final String noleapName = Calendar.noleap.toString();

  @Test
  public void testBasics() {
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-02T16:45:56").orElseThrow();
    assertThat(cd.getCalendar()).isEqualTo(Calendar.noleap);

    assertThat(cd).isInstanceOf(CalendarDateChrono.class);
    CalendarDateChrono chronoDate = (CalendarDateChrono) cd;
    ChronoZonedDateTime<?> chronoDateTime = chronoDate.chronoDateTime();
    assertThat(chronoDateTime.toLocalDate()).isInstanceOf(LeapYearDate.class);

    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(16);

    cd = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-02").orElseThrow();
    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(0);
  }

  @Test
  public void testFailures() {
    assertThat(CalendarDate.fromUdunitIsoDate(noleapName, null)).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate(noleapName, "")).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate(noleapName, "bad")).isEmpty();
  }

  @Test
  public void testFromUdunitIsoDate() {
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-01").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T00:00Z");

    cd = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-01T12:34").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34Z");

    cd = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-01T12:34:56").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34:56Z");

    cd = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-01T12:34:56.123").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34:56.123Z");
  }

  @Test
  public void testOrdering() {
    CalendarDate first = CalendarDate.of(Calendar.noleap, 1999, 1, 1, 1, 1, 1, 1, null);
    CalendarDate second = CalendarDate.of(Calendar.noleap, 1999, 1, 2, 1, 1, 1, 1, null);
    assertThat(first.isBefore(second)).isTrue();
    assertThat(first.isAfter(second)).isFalse();
    assertThat(second.isBefore(first)).isFalse();
    assertThat(second.isAfter(first)).isTrue();

    assertThat(first.isBefore(first)).isFalse();
    assertThat(first.isAfter(first)).isFalse();
  }

  @Test
  public void testGetFields() {
    CalendarDate cd = CalendarDate.of(Calendar.noleap, 1999, 1, 2, 3, 4, 5, 6, null);

    assertThat(cd.getCalendar()).isEqualTo(Calendar.noleap);
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
  public void testAddLeap() {
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-01").orElseThrow();

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
    assertThat(result.toString()).isEqualTo("1950-01-31T00:00Z");

    result = baseDate.add(CalendarPeriod.of(3, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1950-04-01T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1952-07-01T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Year));
    assertThat(result.toString()).isEqualTo("1980-01-01T00:00Z");
  }

  @Test
  public void testAddNoLeap() {
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(noleapName, "1951-01-01").orElseThrow();

    CalendarDate result = baseDate.add(CalendarPeriod.of(300, CalendarPeriod.Field.Millisec));
    assertThat(result.toString()).isEqualTo("1951-01-01T00:00:00.300Z");
    assertThat(result.compareTo(baseDate)).isGreaterThan(0);
    assertThat(result.equals(baseDate)).isFalse();
    assertThat(result.isAfter(baseDate)).isTrue();
    assertThat(result.isBefore(baseDate)).isFalse();

    result = baseDate.add(CalendarPeriod.of(3, CalendarPeriod.Field.Second));
    assertThat(result.toString()).isEqualTo("1951-01-01T00:00:03Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Minute));
    assertThat(result.toString()).isEqualTo("1951-01-01T00:30Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Hour));
    assertThat(result.toString()).isEqualTo("1951-01-02T06:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Day));
    assertThat(result.toString()).isEqualTo("1951-01-31T00:00Z");

    result = baseDate.add(CalendarPeriod.of(3, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1951-04-01T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1953-07-01T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Year));
    assertThat(result.toString()).isEqualTo("1981-01-01T00:00Z");
  }

  @Test
  public void testSubtract() {
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(noleapName, "1950-01-01").orElseThrow();

    CalendarDate result = baseDate.add(-1, CalendarPeriod.of(300, CalendarPeriod.Field.Millisec));
    assertThat(result.toString()).isEqualTo("1949-12-31T23:59:59.700Z");
    assertThat(result.compareTo(baseDate)).isEqualTo(-1);
    assertThat(result.equals(baseDate)).isFalse();
    assertThat(result.isAfter(baseDate)).isFalse();
    assertThat(result.isBefore(baseDate)).isTrue();

    result = baseDate.add(-1, CalendarPeriod.of(300, CalendarPeriod.Field.Second));
    assertThat(result.toString()).isEqualTo("1949-12-31T23:55Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Minute));
    assertThat(result.toString()).isEqualTo("1949-12-31T23:30Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Hour));
    assertThat(result.toString()).isEqualTo("1949-12-30T18:00Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Day));
    assertThat(result.toString()).isEqualTo("1949-12-02T00:00Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1947-07-01T00:00Z");

    result = baseDate.add(-1, CalendarPeriod.of(30, CalendarPeriod.Field.Year));
    assertThat(result.toString()).isEqualTo("1920-01-01T00:00Z");
  }

  @Test
  public void testTruncate() {
    CalendarDate baseDate = CalendarDate.of(Calendar.noleap, 2000, 1, 2, 3, 4, 5, 0, null);

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
    CalendarDate baseDate = CalendarDate.of(Calendar.noleap, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(Calendar.noleap, 2001, 2, 3, 4, 5, 6, 1000000, null);

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

    CalendarDate diffDate2 = CalendarDate.of(Calendar.noleap, 2001, 1, 2, 3, 4, 5, 0, null);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Year)).isEqualTo(1);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(12);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Day)).isEqualTo(360);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Hour)).isEqualTo(360 * 24);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Minute)).isEqualTo(360 * 24 * 60);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Second)).isEqualTo(360 * 24 * 60 * 60);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Millisec)).isEqualTo(31104000000L);

    CalendarDate diffDate3 = CalendarDate.of(Calendar.noleap, 2000, 1, 2, 3, 4, 5, 0, null);
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
    CalendarDate baseDate = CalendarDate.of(Calendar.noleap, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(Calendar.noleap, 2001, 1, 2, 4, 4, 5, 0, null);

    CalendarDateChrono chrono1 = (CalendarDateChrono) baseDate;
    CalendarDateChrono chrono2 = (CalendarDateChrono) diffDate;
    ChronoZonedDateTime<? extends ChronoLocalDate> cz1 = chrono1.chronoDateTime();
    ChronoZonedDateTime<? extends ChronoLocalDate> cz2 = chrono2.chronoDateTime();
    ChronoLocalDate localDate1 = cz1.toLocalDate();
    ChronoLocalDate localDate2 = cz2.toLocalDate();
    LeapYearDate leapYearDate1 = (LeapYearDate) localDate1;
    LeapYearDate leapYearDate2 = (LeapYearDate) localDate2;

    System.out.printf("leapYearDate1 = %d%n", leapYearDate1.toEpochDay());
    System.out.printf("leapYearDate2 = %d%n", leapYearDate2.toEpochDay());
    System.out.printf("diff = %d%n", leapYearDate2.toEpochDay() - leapYearDate1.toEpochDay());

    // assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(12);
  }

  @Test
  public void testDifferenceInMsecs() {
    CalendarDate baseDate = CalendarDate.of(Calendar.noleap, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(Calendar.noleap, 2000, 1, 3, 4, 5, 6, 999, null);
    assertThat(diffDate.getDifferenceInMsecs(baseDate)).isEqualTo(90061000);
    assertThat(diffDate.getDifferenceInMsecs(baseDate)).isEqualTo((25 * 3600 + 60 + 1) * 1000);
  }

  @Test
  public void testTimeZone() {
    String isoCET = "2012-04-27T16:00:00+0200";
    CalendarDate cetDate = CalendarDate.fromUdunitIsoDate(noleapName, isoCET).orElseThrow();
    String isoMST = "2012-04-27T08:00:00-0600";
    CalendarDate mstDate = CalendarDate.fromUdunitIsoDate(noleapName, isoMST).orElseThrow();
    String isoUTC = "2012-04-27T14:00Z";
    CalendarDate utcDate = CalendarDate.fromUdunitIsoDate(noleapName, isoUTC).orElseThrow();
    assertThat(mstDate.toString()).isEqualTo(cetDate.toString());
    assertThat(mstDate.toString()).isEqualTo(utcDate.toString());
  }

  @Test
  public void testTimeZoneLeap() {
    String isoMST = "2012-04-27T08:00:00-0600";
    CalendarDate cdate = CalendarDate.fromUdunitIsoDate(noleapName, isoMST).orElseThrow();
    System.out.printf("result = %s%n", cdate.toString());
    assertThat(cdate.toString()).isEqualTo("2012-04-27T14:00Z");
  }

  @Test
  public void testTimeZoneNoLeap() {
    String isoMST = "2011-04-27T08:00:00-0600";
    CalendarDate cdate = CalendarDate.fromUdunitIsoDate(noleapName, isoMST).orElseThrow();
    System.out.printf("result = %s%n", cdate.toString());
    assertThat(cdate.toString()).isEqualTo("2011-04-27T14:00Z");
  }

}
