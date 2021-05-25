/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time2;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CalendarDate} */
public class TestCalendarDate {

  @Test
  public void testBasics() {
    assertThat(CalendarDate.present().getCalendar()).isEqualTo(Calendar.getDefault());
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
    assertThat(first.getMillis()).isEqualTo(doy.getMillis());
  }

  @Test
  public void testOfMillisecs() {
    CalendarDateUnit cdu = CalendarDateUnit.unixDateUnit;
    CalendarDate zeroEpoch = cdu.getBaseDateTime();

    CalendarDate zero = CalendarDate.of(0);

    assertThat(zero).isEqualTo(zeroEpoch);
    assertThat(zero.hashCode()).isEqualTo(zeroEpoch.hashCode());
    assertThat(zero.getMillis()).isEqualTo(zeroEpoch.getMillis());
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

  /*
   * @Test
   * public void testAddReturnsClosestDate() { // from https://github.com/jonescc
   * String baseDate = "1950-01-01";
   * double valueInMillisecs = 2025829799999.99977;
   * String expectedResult = "2014-03-13T02:30:00Z";
   * 
   * assertAddReturnsExpectedDate(baseDate, valueInMillisecs, CalendarPeriod.Field.Millisec, expectedResult);
   * assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_SECOND,
   * CalendarPeriod.Field.Second,
   * expectedResult);
   * assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_MINUTE,
   * CalendarPeriod.Field.Minute,
   * expectedResult);
   * assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_HOUR,
   * CalendarPeriod.Field.Hour,
   * expectedResult);
   * assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_DAY, CalendarPeriod.Field.Day,
   * expectedResult);
   * assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_MONTH,
   * CalendarPeriod.Field.Month,
   * expectedResult);
   * assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_YEAR,
   * CalendarPeriod.Field.Year,
   * expectedResult);
   * }
   * 
   * private void assertAddReturnsExpectedDate(String baseDate, double value, Field units, String expectedResult) {
   * CalendarDate base = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, baseDate);
   * CalendarDate result = base.add(value, units);
   * assertThat(expectedResult).isEqualTo(CalendarDateFormatter.toDateTimeStringISO(result));
   * }
   */

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
  public void testGetDifference() {
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


}
