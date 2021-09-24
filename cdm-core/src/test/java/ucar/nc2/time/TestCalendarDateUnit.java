package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;

import ucar.nc2.time.CalendarPeriod.Field;
import org.junit.Test;

/** Test {@link ucar.nc2.time.CalendarDateUnit} */
public class TestCalendarDateUnit {

  @Test
  public void testCalendarUnit() {
    testCalendarUnit("calendar days", Field.Day);
    testCalendarUnit("calendar hours", Field.Hour);
    testCalendarUnit("calendar months", Field.Month);
    testCalendarUnit("calendar years", Field.Year);
  }

  private void testCalendarUnit(String unitP, Field field) {
    String unit = unitP + " since 2008-02-29";
    CalendarDate baseDate = CalendarDate.parseISOformat(null, "2008-02-29");
    CalendarDateUnit cdu = CalendarDateUnit.of(null, unit);
    for (int i = 0; i < 15; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%2d %s == %s%n", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      CalendarDate expected = baseDate.add(CalendarPeriod.of(i, field));
      assertThat(cd).isEqualTo(expected);

      assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i);
    }

    for (int i = 0; i < 13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i * 10);
      System.out.printf("%2d %s == %s%n", i * 10, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      CalendarDate expected = baseDate.add(CalendarPeriod.of(i * 10, field));
      assertThat(cd).isEqualTo(expected);

      assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i * 10);
    }
    System.out.printf("%n");
  }

  @Test
  public void testBig() {
    CalendarDateUnit cdu = CalendarDateUnit.of(null, "years since 1970-01-01");
    long val = 50 * 1000 * 1000;
    CalendarDate cd = cdu.makeCalendarDate(val);
    System.out.printf("%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));

    cdu = CalendarDateUnit.of(null, "calendar years since 1970-01-01");
    cd = cdu.makeCalendarDate(val);
    System.out.printf("%n%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
  }

}
