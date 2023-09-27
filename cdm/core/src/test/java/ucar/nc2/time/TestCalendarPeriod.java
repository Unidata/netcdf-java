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

  @Test
  public void shouldGiveSymmetricResultsWhenRounding() {
    final CalendarPeriod calendarPeriod = CalendarPeriod.of(1, Field.Hour);
    final CalendarDate start = CalendarDate.parseISOformat(null, "2000-01-01T00:00:00Z");

    final CalendarDate end = CalendarDate.parseISOformat(null, "2000-01-01T00:10:00Z");
    assertThat(calendarPeriod.subtract(start, end)).isEqualTo(-calendarPeriod.subtract(end, start));

    final CalendarDate end2 = CalendarDate.parseISOformat(null, "2000-01-01T00:50:00Z");
    assertThat(calendarPeriod.subtract(start, end2)).isEqualTo(-calendarPeriod.subtract(end2, start));

    final CalendarDate end3 = CalendarDate.parseISOformat(null, "2000-01-01T00:30:00Z");
    assertThat(calendarPeriod.subtract(start, end3)).isEqualTo(-calendarPeriod.subtract(end3, start));
  }
}
