/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import org.junit.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/** Test {@link CalendarDate} */
public class TestCalendarDate {

  @Test
  public void testBasics() {
    assertThat(CalendarDate.present().getCalendar()).isEqualTo(Calendar.getDefault());

    CalendarDate cd = CalendarDate.fromUdunitIsoDate(null, "1950-01-02T16:45:56").orElseThrow();
    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(16);

    cd = CalendarDate.fromUdunitIsoDate(null, "1950-01-02").orElseThrow();
    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(0);

    assertThat(CalendarDate.fromUdunitIsoDate(Calendar.gregorian.toString(), "1950-01-02T16:45:56")).isPresent();
    assertThat(CalendarDate.fromUdunitIsoDate(Calendar.uniform30day.toString(), "1950-01-02T16:45:56")).isPresent();
  }

  @Test
  public void testFailures() {
    assertThat(CalendarDate.fromUdunitIsoDate(null, null)).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate(null, "")).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate(null, "bad")).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate("null", "1950-01-02T16:45:56")).isEmpty();
    assertThat(CalendarDate.fromUdunitIsoDate("gregarian", "1950-01-02T16:45:56")).isEmpty();
  }

  @Test
  public void testFromUdunitIsoDate() {
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(null, "1950-01-01").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T00:00Z");

    cd = CalendarDate.fromUdunitIsoDate(null, "1950-01-01T12:34").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34Z");

    cd = CalendarDate.fromUdunitIsoDate(null, "1950-01-01T12:34:56").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34:56Z");

    cd = CalendarDate.fromUdunitIsoDate(null, "1950-01-01T12:34:56.123").orElseThrow();
    assertThat(cd.toString()).isEqualTo("1950-01-01T12:34:56.123Z");
  }

  @Test
  public void testDoy() {
    CalendarDate first = CalendarDate.of(null, 1999, 1, 1, 1, 1, 1, 1, null);
    CalendarDate doy = CalendarDate.ofDoy(1999, 1, 1, 1, 1, 1);

    assertThat(first).isEqualTo(doy);
    assertThat(first.hashCode()).isEqualTo(doy.hashCode());
    assertThat(first.getMillisFromEpoch()).isEqualTo(doy.getMillisFromEpoch());
  }

  @Test
  public void testOfMillisecs() {
    CalendarDateUnit cdu = CalendarDateUnit.unixDateUnit;
    CalendarDate zeroEpoch = cdu.getBaseDateTime();

    CalendarDate zero = CalendarDate.of(0);

    assertThat(zero).isEqualTo(zeroEpoch);
    assertThat(zero.hashCode()).isEqualTo(zeroEpoch.hashCode());
    assertThat(zero.getMillisFromEpoch()).isEqualTo(zeroEpoch.getMillisFromEpoch());
  }

  @Test
  public void testOrdering() {
    CalendarDate first = CalendarDate.of(null, 1999, 1, 1, 1, 1, 1, 1, null);
    CalendarDate second = CalendarDate.of(null, 1999, 1, 2, 1, 1, 1, 1, null);
    assertThat(first.isBefore(second)).isTrue();
    assertThat(first.isAfter(second)).isFalse();
    assertThat(second.isBefore(first)).isFalse();
    assertThat(second.isAfter(first)).isTrue();

    assertThat(first.isBefore(first)).isFalse();
    assertThat(first.isAfter(first)).isFalse();
  }

  @Test
  public void testGetFields() {
    CalendarDate cd = CalendarDate.of(null, 1999, 1, 2, 3, 4, 5, 6, null);

    assertThat(cd.getCalendar()).isEqualTo(Calendar.getDefault());
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
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(null, "1950-01-01").orElseThrow();

    CalendarDate result = baseDate.add(CalendarPeriod.of(300, CalendarPeriod.Field.Millisec));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:00:00.300Z");
    assertThat(result.compareTo(baseDate)).isEqualTo(1);
    assertThat(result.equals(baseDate)).isFalse();
    assertThat(result.isAfter(baseDate)).isTrue();
    assertThat(result.isBefore(baseDate)).isFalse();

    result = baseDate.add(CalendarPeriod.of(300, CalendarPeriod.Field.Second));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:05Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Minute));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:30Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Hour));
    assertThat(result.toString()).isEqualTo("1950-01-02T06:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Day));
    assertThat(result.toString()).isEqualTo("1950-01-31T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Month));
    assertThat(result.toString()).isEqualTo("1952-07-01T00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, CalendarPeriod.Field.Year));
    assertThat(result.toString()).isEqualTo("1980-01-01T00:00Z");
  }

  @Test
  public void testSubtract() {
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(null, "1950-01-01").orElseThrow();

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
    CalendarDate baseDate = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5, 0, null);

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
    CalendarDate baseDate = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(null, 2001, 2, 3, 4, 5, 6, 0, null);

    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Year)).isEqualTo(1);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(13);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Day)).isEqualTo(398);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Hour)).isEqualTo(9553);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Minute)).isEqualTo(573181);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Second)).isEqualTo(34390861);
    assertThat(diffDate.since(baseDate, CalendarPeriod.Field.Millisec)).isEqualTo(34390861000L);

    CalendarDate diffDate2 = CalendarDate.of(null, 2001, 1, 2, 3, 4, 5, 0, null);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Year)).isEqualTo(1);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(12);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Day)).isEqualTo(366);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Hour)).isEqualTo(366 * 24);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Minute)).isEqualTo(366 * 24 * 60);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Second)).isEqualTo(366 * 24 * 60 * 60);
    assertThat(diffDate2.since(baseDate, CalendarPeriod.Field.Millisec)).isEqualTo(31622400000L);

    CalendarDate diffDate3 = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5, 0, null);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Year)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Month)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Day)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Hour)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Minute)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Second)).isEqualTo(0);
    assertThat(diffDate3.since(baseDate, CalendarPeriod.Field.Millisec)).isEqualTo(0);
  }

  @Test
  public void testSinceChrono() {
    CalendarDate baseDate = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(null, 2001, 2, 3, 4, 5, 6, 0, null);
  }

  @Test
  public void testDifferenceInMsecs() {
    CalendarDate baseDate = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5, 0, null);
    CalendarDate diffDate = CalendarDate.of(null, 2000, 1, 3, 4, 5, 6, 999, null);
    assertThat(diffDate.getDifferenceInMsecs(baseDate)).isEqualTo(90061000);
    assertThat(diffDate.getDifferenceInMsecs(baseDate)).isEqualTo((25 * 3600 + 60 + 1) * 1000);
  }

  @Test
  public void testTimeZone() {
    String isoCET = "2012-04-27T16:00:00+0200";
    CalendarDate cetDate = CalendarDate.fromUdunitIsoDate(null, isoCET).orElseThrow();
    String isoMST = "2012-04-27T08:00:00-0600";
    CalendarDate mstDate = CalendarDate.fromUdunitIsoDate(null, isoMST).orElseThrow();
    String isoUTC = "2012-04-27T14:00Z";
    CalendarDate utcDate = CalendarDate.fromUdunitIsoDate(null, isoUTC).orElseThrow();
    assertThat(mstDate).isEqualTo(cetDate);
    assertThat(mstDate).isEqualTo(utcDate);
  }

  @Test
  public void testSubtractFail() {
    // Start of forecast
    CalendarDate refTime = CalendarDate.fromUdunitIsoDate(null, "2005-09-01T15:00Z").orElseThrow();
    CalendarDate forecastTimeEnd = CalendarDate.fromUdunitIsoDate(null, "2005-09-01T18:00Z").orElseThrow();
    CalendarPeriod timeUnit = CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
    int forecastTime = 3;
    int timeLength = 6;

    // Start of forecast
    // forecastTimeEnd - 6
    CalendarDate forecastTimeStart = CalendarDate.fromUdunitIsoDate(null, "2005-09-01T12:00Z").orElseThrow();

    // so forecast interval is (-3,3) ? has (3, -3)

    // period.getOffset(CalendarDate start, CalendarDate end)
    // end.since(CalendarDate start, CalendarPeriod period);
    int startOffset = (int) forecastTimeStart.since(refTime, timeUnit);
    // int startOffset = timeUnit.getOffset(refDate, start); // LOOK wrong - not dealing with value ??
    int endOffset = (int) forecastTimeEnd.since(refTime, timeUnit);
    // int endOffset = timeUnit.getOffset(refDate, end);

    System.out.printf("start, end = (%d, %d)", startOffset, endOffset);
    assertThat(startOffset).isEqualTo(-3);
    assertThat(endOffset).isEqualTo(3);
  }

  @Test
  public void testFloatLimits() {
    long maxPrecise = (long) 10e7; // float has 7 sig digits
    Instant maxSecs = Instant.ofEpochSecond(maxPrecise);
    System.out.printf("maxDate in secs = %s%n", OffsetDateTime.ofInstant(maxSecs, ZoneOffset.UTC));

    Instant maxMins = Instant.ofEpochSecond(maxPrecise * 60);
    System.out.printf("maxDate in mins = %s%n", OffsetDateTime.ofInstant(maxMins, ZoneOffset.UTC));

    Instant maxHours = Instant.ofEpochSecond(maxPrecise * 3600);
    System.out.printf("maxDate in hours = %s%n", OffsetDateTime.ofInstant(maxHours, ZoneOffset.UTC));
  }

}
