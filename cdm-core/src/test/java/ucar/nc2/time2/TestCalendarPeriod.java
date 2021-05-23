package ucar.nc2.time2;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CalendarPeriod} */
public class TestCalendarPeriod {

  @Test
  public void testFromUnitString() {
    CalendarPeriod.Field fld = CalendarPeriod.fromUnitString("days");
    assertThat(fld).isEqualTo(CalendarPeriod.Field.Day);

    CalendarPeriod period1 = CalendarPeriod.of("days");
    assertThat(period1.getField()).isEqualTo(CalendarPeriod.Field.Day);
    assertThat(period1.getValue()).isEqualTo(1);
    assertThat(period1.toString()).isEqualTo("1 days");

    CalendarPeriod period = CalendarPeriod.of("11 months");
    assertThat(period.getField()).isEqualTo(CalendarPeriod.Field.Month);
    assertThat(period.getValue()).isEqualTo(11);
    assertThat(period.toString()).isEqualTo("11 months");

    assertThat(CalendarPeriod.of(null)).isNull();
    assertThat(CalendarPeriod.of("")).isNull();
    assertThat(CalendarPeriod.of("11 months from now")).isNull();
    assertThat(CalendarPeriod.of("months from now")).isNull();
  }

  @Test
  public void testGet() {
    for (CalendarPeriod.Field field : CalendarPeriod.Field.values()) {
      assertThat(CalendarPeriod.fromUnitString(field.name())).isEqualTo(field);
    }

    assertThat(CalendarPeriod.fromUnitString("s")).isEqualTo(CalendarPeriod.Field.Second);
    assertThat(CalendarPeriod.fromUnitString("ms")).isEqualTo(CalendarPeriod.Field.Millisec);

    assertThat(CalendarPeriod.fromUnitString(null)).isNull();
    assertThat(CalendarPeriod.fromUnitString("bad")).isNull();
  }

}
