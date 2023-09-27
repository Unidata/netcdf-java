package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import ucar.nc2.time.CalendarPeriod.Field;

public class TestCalendarPeriod {

  @Test
  public void shouldGetOffset() {
    final CalendarPeriod cp = CalendarPeriod.of(1, Field.Day);
    final CalendarDate start = CalendarDate.parseUdunits(null, "3 days since 1970-01-01 12:00");
    final CalendarDate end = CalendarDate.parseUdunits(null, "6 days since 1970-01-01 12:00");
    final int offset = cp.getOffset(start, end);
    assertThat(offset).isEqualTo(3);
  }

}
