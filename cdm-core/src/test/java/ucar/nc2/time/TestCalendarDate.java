/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import ucar.nc2.time.CalendarPeriod.Field;

/** Test {@link ucar.nc2.time.CalendarDate} */
public class TestCalendarDate {

  @Test
  public void testEquals() {
    CalendarDate first = CalendarDate.of(null, 1999, 1, 1, 1, 1, 1);
    CalendarDate second = CalendarDate.of(null, 1999, 1, 1, 1, 1, 1);

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void testOrdering() {
    CalendarDate first = CalendarDate.of(null, 1999, 1, 1, 1, 1, 1);
    CalendarDate second = CalendarDate.of(null, 1999, 1, 2, 1, 1, 1);
    assertThat(first.isBefore(second)).isTrue();
    assertThat(first.isAfter(second)).isFalse();
    assertThat(second.isBefore(first)).isFalse();
    assertThat(second.isAfter(first)).isTrue();

    assertThat(first.isBefore(first)).isFalse();
    assertThat(first.isAfter(first)).isFalse();
  }

  @Test
  public void testGetFields() {
    CalendarDate cd = CalendarDate.of(null, 1999, 1, 2, 3, 4, 5);

    assertThat(cd.getCalendar()).isEqualTo(Calendar.getDefault());
    assertThat(cd.getDayOfMonth()).isEqualTo(2);
    assertThat(cd.getHourOfDay()).isEqualTo(3);

    assertThat(cd.getFieldValue(Field.Year)).isEqualTo(1999);
    assertThat(cd.getFieldValue(Field.Month)).isEqualTo(1);
    assertThat(cd.getFieldValue(Field.Day)).isEqualTo(2);
    assertThat(cd.getFieldValue(Field.Hour)).isEqualTo(3);
    assertThat(cd.getFieldValue(Field.Minute)).isEqualTo(4);
    assertThat(cd.getFieldValue(Field.Second)).isEqualTo(5);
    // TODO cant set millisecs in CalendarDate
    assertThat(cd.getFieldValue(Field.Millisec)).isEqualTo(0);

    assertThat(cd.getTimeUnits()).isEqualTo("1999-01-02 03:04:05.000 UTC");
  }

  @Test
  public void testAddReturnsClosestDate() { // from https://github.com/jonescc
    String baseDate = "1950-01-01";
    double valueInMillisecs = 2025829799999.99977;
    String expectedResult = "2014-03-13T02:30:00Z";

    assertAddReturnsExpectedDate(baseDate, valueInMillisecs, Field.Millisec, expectedResult);
    assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_SECOND, Field.Second,
        expectedResult);
    assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_MINUTE, Field.Minute,
        expectedResult);
    assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_HOUR, Field.Hour,
        expectedResult);
    assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_DAY, Field.Day, expectedResult);
    assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_MONTH, Field.Month,
        expectedResult);
    assertAddReturnsExpectedDate(baseDate, valueInMillisecs / CalendarDate.MILLISECS_IN_YEAR, Field.Year,
        expectedResult);
  }

  private void assertAddReturnsExpectedDate(String baseDate, double value, Field units, String expectedResult) {
    CalendarDate base = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, baseDate);
    CalendarDate result = base.add(value, units);
    assertThat(expectedResult).isEqualTo(CalendarDateFormatter.toDateTimeStringISO(result));
  }

  @Test
  public void testAdd() {
    CalendarDate baseDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, "1950-01-01");

    CalendarDate result = baseDate.add(CalendarPeriod.of(300, Field.Millisec));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:00:00.300Z");

    result = baseDate.add(CalendarPeriod.of(300, Field.Second));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:05:00Z");

    result = baseDate.add(CalendarPeriod.of(30, Field.Minute));
    assertThat(result.toString()).isEqualTo("1950-01-01T00:30:00Z");

    result = baseDate.add(CalendarPeriod.of(30, Field.Hour));
    assertThat(result.toString()).isEqualTo("1950-01-02T06:00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, Field.Day));
    assertThat(result.toString()).isEqualTo("1950-01-31T00:00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, Field.Month));
    assertThat(result.toString()).isEqualTo("1952-07-01T00:00:00Z");

    result = baseDate.add(CalendarPeriod.of(30, Field.Year));
    assertThat(result.toString()).isEqualTo("1980-01-01T00:00:00Z");
  }

  @Test
  public void testSubtract() {
    CalendarDate baseDate = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, "1950-01-01");

    CalendarDate result = baseDate.subtract(CalendarPeriod.of(300, Field.Millisec));
    assertThat(result.toString()).isEqualTo("1949-12-31T23:59:59.700Z");

    result = baseDate.subtract(CalendarPeriod.of(300, Field.Second));
    assertThat(result.toString()).isEqualTo("1949-12-31T23:55:00Z");

    result = baseDate.subtract(CalendarPeriod.of(30, Field.Minute));
    assertThat(result.toString()).isEqualTo("1949-12-31T23:30:00Z");

    result = baseDate.subtract(CalendarPeriod.of(30, Field.Hour));
    assertThat(result.toString()).isEqualTo("1949-12-30T18:00:00Z");

    result = baseDate.subtract(CalendarPeriod.of(30, Field.Day));
    assertThat(result.toString()).isEqualTo("1949-12-02T00:00:00Z");

    result = baseDate.subtract(CalendarPeriod.of(30, Field.Month));
    assertThat(result.toString()).isEqualTo("1947-07-01T00:00:00Z");

    result = baseDate.subtract(CalendarPeriod.of(30, Field.Year));
    assertThat(result.toString()).isEqualTo("1920-01-01T00:00:00Z");
  }

  @Test
  public void testTrucate() {
    CalendarDate baseDate = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5);

    CalendarDate result = baseDate.truncate(Field.Millisec);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:04:05Z");

    result = baseDate.truncate(Field.Second);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:04:05Z");

    result = baseDate.truncate(Field.Minute);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:04:00Z");

    result = baseDate.truncate(Field.Hour);
    assertThat(result.toString()).isEqualTo("2000-01-02T03:00:00Z");

    result = baseDate.truncate(Field.Day);
    assertThat(result.toString()).isEqualTo("2000-01-02T00:00:00Z");

    result = baseDate.truncate(Field.Month);
    assertThat(result.toString()).isEqualTo("2000-01-01T00:00:00Z");

    result = baseDate.truncate(Field.Year);
    assertThat(result.toString()).isEqualTo("2000-01-01T00:00:00Z");
  }

  @Test
  public void testGetDifference() {
    CalendarDate baseDate = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5);
    CalendarDate diffDate = CalendarDate.of(null, 2001, 2, 3, 4, 5, 6);

    assertThat(diffDate.getDifference(baseDate, Field.Year)).isEqualTo(1);
    assertThat(diffDate.getDifference(baseDate, Field.Month)).isEqualTo(13);
    assertThat(diffDate.getDifference(baseDate, Field.Day)).isEqualTo(398);
    assertThat(diffDate.getDifference(baseDate, Field.Hour)).isEqualTo(9553);
    assertThat(diffDate.getDifference(baseDate, Field.Minute)).isEqualTo(573181);
    assertThat(diffDate.getDifference(baseDate, Field.Second)).isEqualTo(34390861);
    assertThat(diffDate.getDifference(baseDate, Field.Millisec)).isEqualTo(34390861000L);

    CalendarDate diffDate2 = CalendarDate.of(null, 2001, 1, 2, 3, 4, 5);
    assertThat(diffDate2.getDifference(baseDate, Field.Year)).isEqualTo(1);
    assertThat(diffDate2.getDifference(baseDate, Field.Month)).isEqualTo(12);
    assertThat(diffDate2.getDifference(baseDate, Field.Day)).isEqualTo(366);
    assertThat(diffDate2.getDifference(baseDate, Field.Hour)).isEqualTo(366 * 24);
    assertThat(diffDate2.getDifference(baseDate, Field.Minute)).isEqualTo(366 * 24 * 60);
    assertThat(diffDate2.getDifference(baseDate, Field.Second)).isEqualTo(366 * 24 * 60 * 60);
    assertThat(diffDate2.getDifference(baseDate, Field.Millisec)).isEqualTo(31622400000L);

    CalendarDate diffDate3 = CalendarDate.of(null, 2000, 1, 2, 3, 4, 5);
    assertThat(diffDate3.getDifference(baseDate, Field.Year)).isEqualTo(0);
    assertThat(diffDate3.getDifference(baseDate, Field.Month)).isEqualTo(0);
    assertThat(diffDate3.getDifference(baseDate, Field.Day)).isEqualTo(0);
    assertThat(diffDate3.getDifference(baseDate, Field.Hour)).isEqualTo(0);
    assertThat(diffDate3.getDifference(baseDate, Field.Minute)).isEqualTo(0);
    assertThat(diffDate3.getDifference(baseDate, Field.Second)).isEqualTo(0);
    assertThat(diffDate3.getDifference(baseDate, Field.Millisec)).isEqualTo(0);

  }


}
