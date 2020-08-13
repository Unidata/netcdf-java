package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.CalendarPeriod.Field;
import ucar.nc2.units.DateUnit;
import org.junit.Test;
import java.lang.invoke.MethodHandles;
import java.util.Date;

/** Test {@link ucar.nc2.time.CalendarDateUnit} */
public class TestCalendarDateUnit {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testUnit() {
    testUnit("days", false);
    testUnit("hours", false);
    testUnit("months", true);
    testUnit("years", true);
  }

  private void testUnit(String unitP, boolean badok) {
    String unit = unitP + " since 2008-02-29";
    CalendarDateUnit cdu = CalendarDateUnit.of(null, unit);

    for (int i = 0; i < 13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%d %s == %s%n", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      testDate(i + " " + unit, badok);

      // LOOK note that this fails for month, year
      if (!badok) {
        assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i);
      }
    }
    System.out.printf("%n");
  }

  private void testDate(String udunits, boolean badok) {
    Date uddate = DateUnit.getStandardDate(udunits);
    CalendarDate cd = CalendarDate.parseUdunits(null, udunits);
    boolean bad = !cd.toDate().equals(uddate);
    if (bad) {
      System.out.printf("  BAD %s == %s != %s (diff = %d)%n", udunits, CalendarDateFormatter.toDateTimeString(uddate),
          cd, cd.toDate().getTime() - uddate.getTime());
      if (!badok) {
        assertThat(cd.toDate()).isEqualTo(uddate);
      }
    }
  }

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
