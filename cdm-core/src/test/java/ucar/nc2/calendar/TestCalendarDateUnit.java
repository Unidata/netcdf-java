package ucar.nc2.calendar;

import org.junit.Test;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

/** Test {@link CalendarDateUnit} */
public class TestCalendarDateUnit {

  @Test
  public void testBasics() {
    CalendarDateUnit cdu =
        CalendarDateUnit.fromUdunitString(Calendar.proleptic_gregorian, "years since 1970-01-01").orElseThrow();
    assertThat(cdu.getCalendar()).isEqualTo(Calendar.proleptic_gregorian);
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of("years"));
    assertThat(cdu.getCalendarField()).isEqualTo(CalendarPeriod.fromUnitString("years"));
    assertThat(cdu.isCalendarField()).isEqualTo(false);

    CalendarDate refDate = CalendarDate.fromUdunitIsoDate("proleptic_gregorian", "1970-01-01").orElseThrow();
    assertThat(cdu.getBaseDateTime()).isEqualTo(refDate);

    CalendarDateUnit cdu2 = CalendarDateUnit.of(cdu.getCalendarField(), cdu.isCalendarField(), refDate);
    assertThat(cdu2).isEqualTo(cdu);
    assertThat(cdu2.hashCode()).isEqualTo(cdu.hashCode());

    CalendarDateUnit cdu3 = CalendarDateUnit.of(cdu.getCalendarPeriod(), cdu.isCalendarField(), refDate);
    assertThat(cdu3).isEqualTo(cdu);
    assertThat(cdu3.hashCode()).isEqualTo(cdu.hashCode());

    CalendarDateUnit cdu4 =
        CalendarDateUnit.of(CalendarPeriod.of(42, cdu.getCalendarField()), cdu.isCalendarField(), refDate);
    assertThat(cdu4).isNotEqualTo(cdu);
    assertThat(cdu4.hashCode()).isNotEqualTo(cdu.hashCode());
  }

  @Test
  public void testFactory() {
    CalendarDateUnit cdu =
        CalendarDateUnit.fromAttributes(new AttributeContainerMutable("empty"), "years since 1970-01-01").orElseThrow();
    assertThat(cdu.getCalendar()).isEqualTo(Calendar.proleptic_gregorian);
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of("years"));
    assertThat(cdu.getCalendarField()).isEqualTo(CalendarPeriod.fromUnitString("years"));
    CalendarDate refDate = CalendarDate.fromUdunitIsoDate("proleptic_gregorian", "1970-01-01").orElseThrow();
    assertThat(cdu.getBaseDateTime()).isEqualTo(refDate);

    AttributeContainerMutable atts = new AttributeContainerMutable("full");
    atts.addAttribute(CDM.UNITS, "days since 1970-01-01");
    atts.addAttribute(CF.CALENDAR, Calendar.noleap.toString());
    cdu = CalendarDateUnit.fromAttributes(atts, null).orElseThrow();
    assertThat(cdu.getCalendar()).isEqualTo(Calendar.noleap);
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of("days"));
    refDate = CalendarDate.fromUdunitIsoDate("noleap", "1970-01-01").orElseThrow();
    assertThat(cdu.getBaseDateTime()).isEqualTo(refDate);

    atts = new AttributeContainerMutable("full");
    atts.addAttribute(CDM.UNITS, "days since 2021-01-01");
    atts.addAttribute(CF.CALENDAR, Calendar.noleap.toString());
    cdu = CalendarDateUnit.fromAttributes(atts, "years since 1970-01-01").orElseThrow();
    assertThat(cdu.getCalendar()).isEqualTo(Calendar.noleap);
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of("years"));
    refDate = CalendarDate.fromUdunitIsoDate("noleap", "1970-01-01").orElseThrow();
    assertThat(cdu.getBaseDateTime()).isEqualTo(refDate);

    assertThat(CalendarDateUnit.fromAttributes(new AttributeContainerMutable("empty"), null)).isEmpty();
    assertThrows(NullPointerException.class, () -> CalendarDateUnit.fromAttributes(null, "ok"));
  }

  @Test
  public void testFailures() {
    assertThat(CalendarDateUnit.fromUdunitString(Calendar.proleptic_gregorian, null)).isEmpty();
    assertThat(CalendarDateUnit.fromUdunitString(Calendar.proleptic_gregorian, "")).isEmpty();
    assertThat(CalendarDateUnit.fromUdunitString(Calendar.proleptic_gregorian, "bad")).isEmpty();
    assertThat(CalendarDateUnit.fromUdunitString(Calendar.proleptic_gregorian, "beers since 1970-01-01")).isEmpty();
    assertThat(CalendarDateUnit.fromUdunitString(Calendar.proleptic_gregorian, "years since youve been gone"))
        .isEmpty();
  }

  @Test
  public void testFromUdunitString() {
    testUnit("days", false);
    testUnit("hours", false);
    testUnit("months", true);
    testUnit("years", true);
  }

  private void testUnit(String unitP, boolean badok) {
    String unit = unitP + " since 2008-02-29";
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow();
    assertThat(cdu.getCalendar()).isEqualTo(Calendar.getDefault());
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of(unitP));
    assertThat(cdu.getCalendarField()).isEqualTo(CalendarPeriod.fromUnitString(unitP));
    assertThat(cdu.isCalendarField()).isEqualTo(false);

    for (int i = 0; i < 13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%d %s == %s%n", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));

      // This fails for month, year
      if (!badok) {
        assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i);
      }
    }
    System.out.printf("%n");
  }

  @Test
  public void testCalendarUnit() {
    testCalendarUnit("hours", CalendarPeriod.Field.Hour);
    testCalendarUnit("days", CalendarPeriod.Field.Day);
    testCalendarUnit("months", CalendarPeriod.Field.Month);
    testCalendarUnit("years", CalendarPeriod.Field.Year);
  }

  private void testCalendarUnit(String unitP, CalendarPeriod.Field field) {
    String bases = "2008-03-31";
    String unit = "calendar " + unitP + " since  " + bases;
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(null, bases).orElseThrow();
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow();

    assertThat(cdu.getCalendar()).isEqualTo(Calendar.getDefault());
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of(unitP));
    assertThat(cdu.getCalendarField()).isEqualTo(CalendarPeriod.fromUnitString(unitP));
    assertThat(cdu.isCalendarField()).isEqualTo(true);

    for (int i = 0; i < 15; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%2d %s == %s", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      CalendarDate expected = baseDate.add(1, CalendarPeriod.of(i, field));
      assertThat(cd).isEqualTo(expected);
      long offset = cdu.makeOffsetFromRefDate(cd);
      System.out.printf(" (%d) %s%n", offset, offset == i ? "" : "***");
      // assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i);
    }

    for (int i = 0; i < 13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i * 10);
      System.out.printf("%2d %s == %s", i * 10, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      CalendarDate expected = baseDate.add(1, CalendarPeriod.of(i * 10, field));
      assertThat(cd).isEqualTo(expected);
      long offset = cdu.makeOffsetFromRefDate(cd);
      System.out.printf(" (%d) %s%n", offset, offset == i * 10 ? "" : "***");
      // assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i * 10);
    }
    System.out.printf("%n");
  }

  @Test
  public void testBig() {
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, "years since 1970-01-01").orElseThrow();
    long val = 50 * 1000 * 1000;
    CalendarDate cd = cdu.makeCalendarDate(val);
    System.out.printf("%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
    assertThat(cd.toString()).isEqualTo("+50001970-01-01T00:00Z");

    cdu = CalendarDateUnit.fromUdunitString(null, "calendar years since 1970-01-01").orElseThrow();;
    cd = cdu.makeCalendarDate(val);
    System.out.printf("%n%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
    assertThat(cd.toString()).isEqualTo("+50001970-01-01T00:00Z");
  }

}
