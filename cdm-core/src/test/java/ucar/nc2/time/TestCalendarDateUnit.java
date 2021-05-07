package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.CalendarPeriod.Field;
import ucar.nc2.units.DateUnit;
import org.junit.Test;
import ucar.units.UnitException;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Formatter;

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

  @Test
  public void testNimbusConversion() throws UnitException {
    int value = 42;
    String udunits = "seconds since 1970-01-01 00:00";
    DateUnit dunit = new DateUnit(udunits);
    String time_units = "seconds since " + dunit.makeStandardDateString(value);

    CalendarDateUnit cdunit = CalendarDateUnit.withCalendar(null, udunits);
    CalendarDate cdate = cdunit.makeCalendarDate(value);
    String time_units2 = "seconds since " + CalendarDateFormatter.toDateTimeStringISO(cdate);

    assertThat(time_units).isEqualTo(time_units2);
  }

  @Test
  public void testAggregationOuterCoordValueVar() throws UnitException {
    CalendarDate baseUnits = CalendarDate.parseUdunits(null, "31 days since 1970-01-01 00:00");

    String udunits = "days since 1970-03-01 00:00";
    DateUnit du = new DateUnit(udunits);
    double val1 = du.makeValue(baseUnits.toDate());

    CalendarDateUnit cdunit = CalendarDateUnit.withCalendar(null, udunits);
    double val2 = cdunit.makeOffsetFromRefDate(baseUnits);

    assertThat(val2).isEqualTo(val1);

    System.out.printf("Convert '%s' to '%s' = %f%n", cdunit, baseUnits, val2);
  }

  @Test
  public void testUnitConvert() throws UnitException {
    String command = "42 hours since 1900-01-01 00:00:00.0";
    DateUnit du = new DateUnit(command);
    Formatter f = new Formatter();
    f.format("From udunits: '%s' isDateUnit = '%s'%n", command, du);
    Date d = du.getDate();
    f.format(" getStandardDateString = %s%n", CalendarDateFormatter.toDateTimeString(d));
    f.format(" getDateOrigin = %s%n", CalendarDateFormatter.toDateTimeString(du.getDateOrigin()));

    Date d2 = DateUnit.getStandardOrISO(command);
    if (d2 == null) {
      f.format(" DateUnit.getStandardOrISO = false%n");
    } else {
      f.format(" DateUnit.getStandardOrISO = '%s'%n", CalendarDateFormatter.toDateTimeString(d2));
    }
    System.out.printf("%s", f);

    Formatter f2 = new Formatter();
    CalendarDateUnit cdu = CalendarDate.parseUdunitsUnit(null, command);
    f2.format("%nFrom udunits: '%s' CalendarDateUnit = '%s'%n", command, cdu);
    f2.format("getBaseCalendarDate = %s%n", CalendarDateFormatter.toDateTimeString(cdu.getBaseCalendarDate()));
    CalendarDate cd = CalendarDate.parseUdunitsOrIso(null, command);
    if (cd != null) {
      f2.format("parseUdunitsOrIso = %s%n", CalendarDateFormatter.toDateTimeString(cd));
    } else {
      f2.format("parseUdunitsOrIso is null%n");
    }
    System.out.printf("%s", f2);

    assertThat(CalendarDateFormatter.toDateTimeString(du.getDateOrigin()))
        .isEqualTo(CalendarDateFormatter.toDateTimeString(cdu.getBaseCalendarDate()));
    assertThat(CalendarDateFormatter.toDateTimeString(d)).isEqualTo(CalendarDateFormatter.toDateTimeString(cd));

  }

}
