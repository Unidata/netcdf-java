/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.units.ScaledUnit;
import ucar.units.UnitException;

/** Test {@link ucar.nc2.units.SimpleUnit} */
public class TestSimpleUnit {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testConvert() {
    SimpleUnit t1 = SimpleUnit.factory("1 days");
    SimpleUnit t2 = SimpleUnit.factory("1 hour");
    double v = t1.convertTo(1.0, t2);
    System.out.println(t1 + " convertTo " + t2 + " = " + v);
    assert v == 24.0;
  }

  @Test
  public void testConvertFail() {
    SimpleUnit t1 = SimpleUnit.factory("1 days");
    SimpleUnit t2 = SimpleUnit.factory("1 meter");
    assertThrows(IllegalArgumentException.class, () -> t1.convertTo(1.0, t2));
  }

  @Test
  public void testUnits() {
    SimpleUnit su = SimpleUnit.factory("11 hPa");
    assert !(su instanceof TimeUnit);
    assert su.getValue() == 1100.0 : su;
    assert su.getUnitString().equals("Pa") : su;

    su = SimpleUnit.factory("11 km");
    assert !(su instanceof TimeUnit);
    assert su.getValue() == 11000.0 : su;
    assert su.getUnitString().equals("m") : su;
  }

  @Test
  public void testTimeUnits() {
    SimpleUnit tu = SimpleUnit.factory("3 days");
    assertThat(tu).isInstanceOf(TimeUnit.class);
    assertThat(tu.getUnitString()).isEqualTo("days");
    assertThat(tu.getValue()).isEqualTo(3.0);
    assertThat(SimpleUnit.isTimeUnit("3 days")).isTrue();

    String text = "3 days since 1930-07-27 12:00:00-05:00";
    SimpleUnit du = SimpleUnit.factory(text);
    System.out.println(text + " == standard format " + du);
    assertThat(du).isNotInstanceOf(TimeUnit.class);
    assertThat(SimpleUnit.isDateUnit(text)).isTrue();

    text = "hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory(text);
    System.out.println(text + " == standard format " + du);
    assertThat(du).isNotInstanceOf(TimeUnit.class);
    assertThat(SimpleUnit.isDateUnit(text)).isTrue();

    text = "1 hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory(text);
    System.out.println(text + " == standard format " + du);
    assertThat(du).isNotInstanceOf(TimeUnit.class);
    assertThat(SimpleUnit.isDateUnit(text)).isTrue();

    // TODO is this ok?
    assertThat(SimpleUnit.isDateUnit("0 hours since 1930-07-29T01:00:00-08:00")).isFalse();
  }

  @Test
  public void testCompatible() {
    SimpleUnit su = SimpleUnit.factory("11 hPa");
    assertThat(su.isCompatible("mbar")).isTrue();
    assertThat(su.isCompatible("m")).isFalse();
    assertThat(su.isCompatible("sec")).isFalse();
    assertThat(su.isCompatible("3 days since 1930-07-27 12:00:00-05:00")).isFalse();
  }

  @Test
  public void testCompatibleWithException() throws UnitException {
    assertThat(SimpleUnit.isCompatibleWithExceptions("11 hPa", "mbar")).isTrue();
    assertThat(SimpleUnit.isCompatibleWithExceptions("11 hPa", "m")).isFalse();
    assertThat(SimpleUnit.isCompatibleWithExceptions("11 hPa", "sec")).isFalse();
    assertThrows(ucar.units.UnitParseException.class,
        () -> SimpleUnit.isCompatibleWithExceptions("11 hPa", "3 bad days since 1930-07-27 12:00:00-05:00"));
  }

  @Test
  public void testDates() throws Exception {
    String text = "months since 1930-01-01";
    DateUnit du = new DateUnit(text);
    for (int i = 0; i < 12; i++) {
      System.out.printf("%d %s == %s%n", i, text, CalendarDateFormatter.toDateTimeStringISO(du.makeDate(i)));
    }

    text = "years since 1850-01-01";
    du = new DateUnit(text);
    for (int i = 0; i < 100; i += 10) {
      System.out.printf("%d %s == %s%n", i, text, CalendarDateFormatter.toDateTimeStringISO(du.makeDate(i)));
    }
  }

  @Test
  public void testConversionFactor() throws Exception {
    assertThat(SimpleUnit.getConversionFactor("m", "km")).isEqualTo(1 / 1000.0);
    assertThat(SimpleUnit.getConversionFactor("km", "m")).isEqualTo(1000.0);
  }

  @Test
  public void testUnknownUnit() {
    SimpleUnit uu = SimpleUnit.factory("sigmoid");
    assertThat(uu).isNotNull();
    assertThat(uu.isUnknownUnit()).isTrue();
    assertThat(uu.getImplementingClass()).isEqualTo("ucar.units.UnknownUnit");
  }

  @Test
  public void testCanonicalString() {
    String text = "99 mbar";
    SimpleUnit uu = SimpleUnit.factory(text);
    assertThat(uu).isNotNull();
    assertThat(uu.getUnitString()).isEqualTo("Pa");
    assertThat(uu.getCanonicalString()).isEqualTo("9900.0 Pa");
  }

  @Test
  public void testScaledUnit() {
    String text = "99 mbar";
    SimpleUnit uu = SimpleUnit.factory(text);
    assertThat(uu).isNotNull();

    assertThat(uu.getUnit()).isInstanceOf(ScaledUnit.class);
    ScaledUnit scaled = (ScaledUnit) uu.getUnit();
    assertThat(scaled.getScale()).isEqualTo(9900.0);
  }

}
