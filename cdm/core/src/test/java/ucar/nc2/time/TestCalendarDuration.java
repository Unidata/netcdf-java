package ucar.nc2.time;

import org.junit.Test;

public class TestCalendarDuration {

  static private void test(String unit, String result) {
    org.joda.time.Period jp = CalendarDuration.convertToPeriod(1, unit);
    assert jp != null;
    System.out.printf("%s == %s%n", unit, jp);
    assert jp.toString().equals(result) : jp.toString() + " != " + result;
  }

  @Test
  public void testStuff() {
    test("sec", "PT1S");
    test("secs", "PT1S");
    test("minute", "PT1M");
    test("minutes", "PT1M");
    test("hour", "PT1H");
    test("hours", "PT1H");
    test("hr", "PT1H");
    test("day", "P1D");
    test("days", "P1D");
    test("week", "P1W");
    test("weeks", "P1W");
    test("month", "P1M");
    test("months", "P1M");
    test("year", "P1Y");
    test("years", "P1Y");
  }

}
