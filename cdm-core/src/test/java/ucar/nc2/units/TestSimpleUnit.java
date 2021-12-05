/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import ucar.units.ScaledUnit;
import ucar.units.UnitException;

/** Test {@link ucar.nc2.units.SimpleUnit} */
public class TestSimpleUnit {

  @Test
  public void testConvert() {
    SimpleUnit t1 = SimpleUnit.factory("1 days");
    SimpleUnit t2 = SimpleUnit.factory("1 hour");
    double v = t1.convertTo(1.0, t2);
    System.out.println(t1 + " convertTo " + t2 + " = " + v);
    assertThat(v).isEqualTo(24.0);
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
    assertThat(su).isNotInstanceOf(TimeUnit.class);
    assertThat(su.getValue()).isEqualTo(1100.0);
    assertThat(su.getUnitString()).isEqualTo("Pa");

    su = SimpleUnit.factory("11 km");
    assertThat(su).isNotInstanceOf(TimeUnit.class);
    assertThat(su.getValue()).isEqualTo(11000.0);
    assertThat(su.getUnitString()).isEqualTo("m");
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
    System.out.println(text + ").isEqualTo(standard format " + du);
    assertThat(du).isNotInstanceOf(TimeUnit.class);
    assertThat(SimpleUnit.isDateUnit(text)).isTrue();

    text = "hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory(text);
    System.out.println(text + ").isEqualTo(standard format " + du);
    assertThat(du).isNotInstanceOf(TimeUnit.class);
    assertThat(SimpleUnit.isDateUnit(text)).isTrue();

    text = "1 hours since 1930-07-29T01:00:00-08:00";
    du = SimpleUnit.factory(text);
    System.out.println(text + ").isEqualTo(standard format " + du);
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
    assertThat(uu.isUnknownUnit()).isFalse();
  }

  @Test
  public void testScaledUnit() {
    String text = "99 mbar";
    SimpleUnit uu = SimpleUnit.factory(text);
    assertThat(uu).isNotNull();

    assertThat(uu.getUnit()).isInstanceOf(ScaledUnit.class);
    ScaledUnit scaled = (ScaledUnit) uu.getUnit();
    assertThat(scaled.getScale()).isEqualTo(9900.0);
    assertThat(uu.isUnknownUnit()).isFalse();
  }

}
