package ucar.nc2.time;

import ucar.nc2.time.CalendarPeriod.Field;

public class TestCalendarPeriod {

  public void testStuff() {
    CalendarPeriod cp = CalendarPeriod.of(1, Field.Day);
    CalendarDate start = CalendarDate.parseUdunits(null, "3 days since 1970-01-01 12:00");
    CalendarDate end = CalendarDate.parseUdunits(null, "6 days since 1970-01-01 12:00");
    int offset = cp.getOffset(start, end);
    System.out.printf("offset=%d%n", offset);
  }

}
