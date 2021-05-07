package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import ucar.nc2.time.CalendarPeriod.Field;

/** Test {@link ucar.nc2.time.CalendarPeriod} */
public class TestCalendarPeriod {

  @Test
  public void testFromUnitString() {
    CalendarPeriod.Field fld = CalendarPeriod.fromUnitString("days");
    assertThat(fld).isEqualTo(Field.Day);

    CalendarPeriod period1 = CalendarPeriod.of("days");
    assertThat(period1.getField()).isEqualTo(Field.Day);
    assertThat(period1.getValue()).isEqualTo(1);

    CalendarPeriod period = CalendarPeriod.of("11 months");
    assertThat(period.getField()).isEqualTo(Field.Month);
    assertThat(period.getValue()).isEqualTo(11);

    String val = null;
    assertThat(CalendarPeriod.of(val)).isNull();
    assertThat(CalendarPeriod.of("")).isNull();
    assertThat(CalendarPeriod.of("11 months from now")).isNull();
    assertThat(CalendarPeriod.of("months from now")).isNull();
  }

  @Test
  public void testOffset() {
    CalendarPeriod cp = CalendarPeriod.of(1, Field.Day);
    CalendarDate start = CalendarDate.parseUdunits(null, "3 days since 1970-01-01 12:00");
    CalendarDate end = CalendarDate.parseUdunits(null, "6 days since 1970-01-01 12:00");
    int offset = cp.getOffset(start, end);
    System.out.printf("offset=%d%n", offset);
  }

}
