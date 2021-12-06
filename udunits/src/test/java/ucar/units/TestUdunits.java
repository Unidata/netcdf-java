/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.units;

import java.util.Date;
import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * Tests for udunits.
 */
public class TestUdunits {

  @Test
  public void testOffsetUnit() throws Exception {
    final BaseUnit kelvin =
        BaseUnit.getOrCreate(UnitName.newUnitName("kelvin", null, "K"), BaseQuantity.THERMODYNAMIC_TEMPERATURE);
    final OffsetUnit celsius = new OffsetUnit(kelvin, 273.15);
    System.out.println("celsius.equals(kelvin)=" + celsius.equals(kelvin));
    System.out.println("celsius.getUnit().equals(kelvin)=" + celsius.getUnit().equals(kelvin));
    assertThat(celsius).isNotEqualTo(kelvin);
    assertThat(celsius.getUnit()).isEqualTo(kelvin);

    final Unit celsiusKelvin = celsius.multiplyBy(kelvin);
    System.out.println("celsiusKelvin.divideBy(celsius)=" + celsiusKelvin.divideBy(celsius));
    System.out.println("celsius.divideBy(kelvin)=" + celsius.divideBy(kelvin));
    System.out.println("kelvin.divideBy(celsius)=" + kelvin.divideBy(celsius));
    System.out.println("celsius.raiseTo(2)=" + celsius.raiseTo(2));
    System.out.println("celsius.toDerivedUnit(1.)=" + celsius.toDerivedUnit(1.));
    System.out.println("celsius.toDerivedUnit(new float[]{1,2,3}, new float[3])[1]="
        + celsius.toDerivedUnit(new float[] {1, 2, 3}, new float[3])[1]);
    System.out.println("celsius.fromDerivedUnit(274.15)=" + celsius.fromDerivedUnit(274.15));
    System.out.println("celsius.fromDerivedUnit(new float[]{274.15f},new float[1])[0]="
        + celsius.fromDerivedUnit(new float[] {274.15f}, new float[1])[0]);
    System.out.println("celsius.equals(celsius)=" + celsius.equals(celsius));
    assertThat(celsius).isEqualTo(celsius);

    final OffsetUnit celsius100 = new OffsetUnit(celsius, 100.);
    System.out.println("celsius.equals(celsius100)=" + celsius.equals(celsius100));
    System.out.println("celsius.isDimensionless()=" + celsius.isDimensionless());
    assertThat(celsius).isNotEqualTo(celsius100);
    assertThat(celsius.isDimensionless()).isFalse();

    final BaseUnit radian = BaseUnit.getOrCreate(UnitName.newUnitName("radian", null, "rad"), BaseQuantity.PLANE_ANGLE);
    final OffsetUnit offRadian = new OffsetUnit(radian, 3.14159 / 2);
    System.out.println("offRadian.isDimensionless()=" + offRadian.isDimensionless());
    assertThat(offRadian.isDimensionless()).isTrue();
  }

  @Test
  public void testLogarithmicUnit() throws Exception {
    final BaseUnit meter = BaseUnit.getOrCreate(UnitName.newUnitName("meter", null, "m"), BaseQuantity.LENGTH);
    final ScaledUnit micron = new ScaledUnit(1e-6, meter);
    final Unit cubicMicron = micron.raiseTo(3);
    final LogarithmicUnit Bz = new LogarithmicUnit(cubicMicron, 10.0);
    assertThat(Bz.isDimensionless()).isTrue();
    assertThat(Bz).isEqualTo(Bz);
    assertThat(Bz.getReference()).isEqualTo(cubicMicron);
    assertThat(Bz.getBase()).isEqualTo(10.0);
    assertThat(Bz).isNotEqualTo(cubicMicron);
    assertThat(Bz).isNotEqualTo(micron);
    assertThat(Bz).isNotEqualTo(meter);

    assertThrows(MultiplyException.class, () -> Bz.multiplyBy(meter));
    assertThrows(DivideException.class, () -> Bz.divideBy(meter));
    assertThrows(RaiseException.class, () -> Bz.raiseTo(2));

    double value = Bz.toDerivedUnit(0);
    assertThat(0.9e-18 < value && value < 1.1e-18).isTrue();
    value = Bz.toDerivedUnit(1);
    assertThat(0.9e-17 < value && value < 1.1e-17).isTrue();
    value = Bz.fromDerivedUnit(1e-18);
    assertThat(-0.1 < value && value < 0.1).isTrue();
    value = Bz.fromDerivedUnit(1e-17);
    assertThat(0.9 < value && value < 1.1).isTrue();
    final String s = Bz.toString();
    assertThat(s).isEqualTo("lg(re 9.999999999999999E-19 m3)");
  }

  @Test
  public void testTimeScaleUnit() throws Exception {
    final TimeZone tz = TimeZone.getTimeZone("UTC");
    final Calendar calendar = Calendar.getInstance(tz);
    calendar.clear();
    calendar.set(1970, 0, 1);
    TimeScaleUnit tunit = new TimeScaleUnit(TimeScaleUnit.SECOND, calendar.getTime());
    System.out.printf("%s%n", tunit);
  }

  @Test
  public void testUnknownUnit() throws Exception {
    final UnknownUnit unit1 = UnknownUnit.create("a");
    assertThat(unit1).isEqualTo(unit1);
    assertThat(unit1).isEqualTo(unit1);
    assertThat(unit1.isDimensionless()).isFalse();
    UnknownUnit unit2 = UnknownUnit.create("b");
    assertThat(unit1).isNotEqualTo(unit2);
    unit2 = UnknownUnit.create("A");
    assertThat(unit1).isEqualTo(unit2);
  }

  @Test
  public void testBaseQuantity() {
    System.out.println("AMOUNT_OF_SUBSTANCE.getName() = " + BaseQuantity.AMOUNT_OF_SUBSTANCE.getName());
    System.out.println("LUMINOUS_INTENSITY.getSymbol() = " + BaseQuantity.LUMINOUS_INTENSITY.getSymbol());
    System.out.println("PLANE_ANGLE.getSymbol() = " + BaseQuantity.PLANE_ANGLE.getSymbol());

    System.out.println("LENGTH.equals(LENGTH) = " + BaseQuantity.LENGTH.equals(BaseQuantity.LENGTH));
    System.out.println("LENGTH.equals(MASS) = " + BaseQuantity.LENGTH.equals(BaseQuantity.MASS));
    System.out.println("LENGTH.equals(PLANE_ANGLE) = " + BaseQuantity.LENGTH.equals(BaseQuantity.PLANE_ANGLE));
    System.out
        .println("PLANE_ANGLE.equals(PLANE_ANGLE) = " + BaseQuantity.PLANE_ANGLE.equals(BaseQuantity.PLANE_ANGLE));
    System.out
        .println("PLANE_ANGLE.equals(SOLID_ANGLE) = " + BaseQuantity.PLANE_ANGLE.equals(BaseQuantity.SOLID_ANGLE));

    System.out.println("LENGTH.compareTo(LENGTH) = " + BaseQuantity.LENGTH.compareTo(BaseQuantity.LENGTH));
    System.out.println("LENGTH.compareTo(MASS) = " + BaseQuantity.LENGTH.compareTo(BaseQuantity.MASS));
    System.out.println("LENGTH.compareTo(PLANE_ANGLE) = " + BaseQuantity.LENGTH.compareTo(BaseQuantity.PLANE_ANGLE));
    System.out.println(
        "PLANE_ANGLE.compareTo(PLANE_ANGLE) = " + BaseQuantity.PLANE_ANGLE.compareTo(BaseQuantity.PLANE_ANGLE));
    System.out.println(
        "PLANE_ANGLE.compareTo(SOLID_ANGLE) = " + BaseQuantity.PLANE_ANGLE.compareTo(BaseQuantity.SOLID_ANGLE));
  }

  @Test
  public void testBaseUnit() throws NameException {
    final BaseUnit meter = new BaseUnit(UnitName.newUnitName("meter", null, "m"), BaseQuantity.LENGTH);
    System.out.println("meter.getBaseQuantity()=" + meter.getBaseQuantity());
    System.out.println("meter.toDerivedUnit(1.)=" + meter.toDerivedUnit(1.));
    System.out
        .println("meter.toDerivedUnit(new float[] {2})[0]=" + meter.toDerivedUnit(new float[] {2}, new float[1])[0]);
    System.out.println("meter.fromDerivedUnit(1.)=" + meter.fromDerivedUnit(1.));
    System.out.println(
        "meter.fromDerivedUnit(new float[] {3})[0]=" + meter.fromDerivedUnit(new float[] {3}, new float[1])[0]);
    System.out.println("meter.isCompatible(meter)=" + meter.isCompatible(meter));
    final BaseUnit radian = new BaseUnit(UnitName.newUnitName("radian", null, "rad"), BaseQuantity.PLANE_ANGLE);
    System.out.println("meter.isCompatible(radian)=" + meter.isCompatible(radian));
    System.out.println("meter.isDimensionless()=" + meter.isDimensionless());
    System.out.println("radian.isDimensionless()=" + radian.isDimensionless());
  }

  @Test
  public void testDerivedUnitImpl() throws Exception {
    final BaseUnit second = BaseUnit.getOrCreate(UnitName.newUnitName("second", null, "s"), BaseQuantity.TIME);
    System.out.println("second = \"" + second + '"');
    final BaseUnit meter = BaseUnit.getOrCreate(UnitName.newUnitName("meter", null, "m"), BaseQuantity.LENGTH);
    System.out.println("meter = \"" + meter + '"');
    final DerivedUnitImpl meterSecond = (DerivedUnitImpl) meter.myMultiplyBy(second);
    System.out.println("meterSecond = \"" + meterSecond + '"');
    final DerivedUnitImpl meterPerSecond = (DerivedUnitImpl) meter.myDivideBy(second);
    System.out.println("meterPerSecond = \"" + meterPerSecond + '"');
    final DerivedUnitImpl secondPerMeter = (DerivedUnitImpl) second.myDivideBy(meter);
    System.out.println("secondPerMeter = \"" + secondPerMeter + '"');
    System.out
        .println("meterPerSecond.isReciprocalOf(secondPerMeter)=" + meterPerSecond.isReciprocalOf(secondPerMeter));
    System.out.println("meter.toDerivedUnit(1.0)=" + meter.toDerivedUnit(1.0));
    System.out.println("meter.toDerivedUnit(new double[] {1,2,3}, new double[3])[1]="
        + meter.toDerivedUnit(new double[] {1, 2, 3}, new double[3])[1]);
    System.out.println("meter.fromDerivedUnit(1.0)=" + meter.fromDerivedUnit(1.0));
    System.out.println("meter.fromDerivedUnit(new double[] {1,2,3}, new double[3])[2]="
        + meter.fromDerivedUnit(new double[] {1, 2, 3}, new double[3])[2]);
    System.out.println("meter.isCompatible(meter)=" + meter.isCompatible(meter));
    System.out.println("meter.isCompatible(second)=" + meter.isCompatible(second));
    System.out.println("meter.equals(meter)=" + meter.equals(meter));
    System.out.println("meter.equals(second)=" + meter.equals(second));
    System.out.println("meter.isDimensionless()=" + meter.isDimensionless());
    final Unit sPerS = second.myDivideBy(second);
    System.out.println("sPerS = \"" + sPerS + '"');
    System.out.println("sPerS.isDimensionless()=" + sPerS.isDimensionless());
    meterPerSecond.raiseTo(2);
    meter.myDivideBy(meterPerSecond);
  }

  @Test
  public void testQuantityDimension() {
    System.out.println("new QuantityDimension() = \"" + new QuantityDimension() + '"');
    QuantityDimension timeDimension = new QuantityDimension(BaseQuantity.TIME);
    System.out.println("timeDimension = \"" + timeDimension + '"');
    QuantityDimension lengthDimension = new QuantityDimension(BaseQuantity.LENGTH);
    System.out.println("lengthDimension = \"" + lengthDimension + '"');
    System.out.println(
        "lengthDimension.isReciprocalOf(timeDimension) = \"" + lengthDimension.isReciprocalOf(timeDimension) + '"');
    QuantityDimension hertzDimension = timeDimension.raiseTo(-1);
    System.out.println("hertzDimension = \"" + hertzDimension + '"');
    System.out.println(
        "hertzDimension.isReciprocalOf(timeDimension) = \"" + hertzDimension.isReciprocalOf(timeDimension) + '"');
    System.out.println("lengthDimension.divideBy(timeDimension) = \"" + lengthDimension.divideBy(timeDimension) + '"');
    System.out.println("lengthDimension.divideBy(timeDimension).raiseTo(2) = \""
        + lengthDimension.divideBy(timeDimension).raiseTo(2) + '"');
  }


  @Test
  public void testScaledUnit() throws Exception {
    final BaseUnit meter = BaseUnit.getOrCreate(UnitName.newUnitName("meter", null, "m"), BaseQuantity.LENGTH);
    final ScaledUnit nauticalMile = new ScaledUnit(1852f, meter);
    System.out.println("nauticalMile.getUnit().equals(meter)=" + nauticalMile.getUnit().equals(meter));
    final ScaledUnit nauticalMileMeter = (ScaledUnit) nauticalMile.multiplyBy(meter);
    System.out.println("nauticalMileMeter.divideBy(nauticalMile)=" + nauticalMileMeter.divideBy(nauticalMile));
    System.out.println("meter.divideBy(nauticalMile)=" + meter.divideBy(nauticalMile));
    System.out.println("nauticalMile.raiseTo(2)=" + nauticalMile.raiseTo(2));
    System.out.println("nauticalMile.toDerivedUnit(1.)=" + nauticalMile.toDerivedUnit(1.));
    System.out.println("nauticalMile.toDerivedUnit(new float[]{1,2,3}, new float[3])[1]="
        + nauticalMile.toDerivedUnit(new float[] {1, 2, 3}, new float[3])[1]);
    System.out.println("nauticalMile.fromDerivedUnit(1852.)=" + nauticalMile.fromDerivedUnit(1852.));
    System.out.println("nauticalMile.fromDerivedUnit(new float[]{1852},new float[1])[0]="
        + nauticalMile.fromDerivedUnit(new float[] {1852}, new float[1])[0]);
    System.out.println("nauticalMile.equals(nauticalMile)=" + nauticalMile.equals(nauticalMile));
    final ScaledUnit nautical2Mile = new ScaledUnit(2, nauticalMile);
    System.out.println("nauticalMile.equals(nautical2Mile)=" + nauticalMile.equals(nautical2Mile));
    System.out.println("nauticalMile.isDimensionless()=" + nauticalMile.isDimensionless());
    final BaseUnit radian = BaseUnit.getOrCreate(UnitName.newUnitName("radian", null, "rad"), BaseQuantity.PLANE_ANGLE);
    final ScaledUnit degree = new ScaledUnit(3.14159 / 180, radian);
    System.out.println("degree.isDimensionless()=" + degree.isDimensionless());
  }

  @Test
  public void testStandardPrefixDB() throws Exception {
    final PrefixDB db = StandardPrefixDB.instance();
    System.out.println("db.getPrefixBySymbol(\"cm\") = \"" + db.getPrefixBySymbol("cm") + '"');
    System.out.println("db.getPrefixBySymbol(\"dm\") = \"" + db.getPrefixBySymbol("dm") + '"');
  }


  @Test
  public void testStandardUnitDB() throws Exception {
    final UnitDB db = StandardUnitDB.instance();
    System.out.println("db.get(\"meter\")=" + db.get("meter"));
    System.out.println("db.get(\"meters\")=" + db.get("meters"));
    System.out.println("db.get(\"metre\")=" + db.get("metre"));
    System.out.println("db.get(\"metres\")=" + db.get("metres"));
    System.out.println("db.get(\"m\")=" + db.get("m"));
    System.out.println("db.get(\"newton\")=" + db.get("newton"));
    System.out.println("db.get(\"Cel\")=" + db.get("Cel"));
    System.out.println("db.get(\"Roentgen\")=" + db.get("Roentgen"));
    System.out.println("db.get(\"rad\")=" + db.get("rad"));
    System.out.println("db.get(\"rd\")=" + db.get("rd"));
    System.out.println("db.get(\"perches\")=" + db.get("perches"));
    System.out.println("db.get(\"jiffies\")=" + db.get("jiffies"));
    System.out.println("db.get(\"foo\")=" + db.get("foo"));
  }

  @Test
  public void testStandardUnitFormat() throws Exception {
    StandardUnitFormat parser = StandardUnitFormat.instance();
    final Unit m = parser.parse("m");
    final Unit s = parser.parse("s");
    final Unit epoch = parser.parse("s @ 1970-01-01 00:00:00 UTC");
    myAssert(parser, "m m", m.multiplyBy(m));
    myAssert(parser, "m.m", m.multiplyBy(m));
    myAssert(parser, "(m)(m)", m.multiplyBy(m));
    myAssert(parser, "m/s/s", m.divideBy(s).divideBy(s));
    myAssert(parser, "m2", m.raiseTo(2));
    myAssert(parser, "m2.s", m.raiseTo(2).multiplyBy(s));
    myAssert(parser, "m2/s", m.raiseTo(2).divideBy(s));
    myAssert(parser, "m^2/s", m.raiseTo(2).divideBy(s));
    myAssert(parser, "m s @ 5", m.multiplyBy(s).shiftTo(5.0));
    myAssert(parser, "m2 s @ 5", m.raiseTo(2).multiplyBy(s).shiftTo(5));
    myAssert(parser, "m2 s-1 @ 5", m.raiseTo(2).multiplyBy(s.raiseTo(-1)).shiftTo(5));
    myAssert(parser, "m s from 5", m.multiplyBy(s).shiftTo(5));
    myAssert(parser, "s@19700101", epoch);
    myAssert(parser, "s@19700101T000000", epoch);
    myAssert(parser, "s@19700101T000000.00", epoch);
    myAssert(parser, "s @ 1970-01-01T00:00:00.00", epoch);
    myAssert(parser, "s @ 1970-01-01 00:00:00.00", epoch);
    myAssert(parser, "s @ 1970-01-01 00:00:00.00 +0", epoch);
    myAssert(parser, "s @ 1970-01-01T00:00:00.00 -12", epoch.shiftTo(new Date(12 * 60 * 60 * 1000)));
    if (!parser.parse("days since 2009-06-14 04:00:00").equals(parser.parse("days since 2009-06-14 04:00:00 +00:00"))) {
      throw new AssertionError();
    }
    myAssert(parser, "lg(re: 1)", DerivedUnitImpl.DIMENSIONLESS.log(10));
    myAssert(parser, "0.1 lg(re 1 mm)", m.multiplyBy(1e-3).log(10).multiplyBy(0.1));
    myAssert(parser, "m", m);
    myAssert(parser, "2 m s", m.multiplyBy(s).multiplyBy(2));
    myAssert(parser, "3.14 m.s", m.multiplyBy(s).multiplyBy(3.14));
    myAssert(parser, "1e9 (m)", m.multiplyBy(1e9));
    myAssert(parser, "(m s)2", m.multiplyBy(s).raiseTo(2));
    myAssert(parser, "m2.s-1", m.raiseTo(2).divideBy(s));
    myAssert(parser, "m2 s^-1", m.raiseTo(2).divideBy(s));
    myAssert(parser, "(m/s)2", m.divideBy(s).raiseTo(2));
    myAssert(parser, "m2/s-1", m.raiseTo(2).divideBy(s.raiseTo(-1)));
    myAssert(parser, "m2/s^-1", m.raiseTo(2).divideBy(s.raiseTo(-1)));
    myAssert(parser, ".5 m/(.25 s)2", m.multiplyBy(.5).divideBy(s.multiplyBy(.25).raiseTo(2)));
    myAssert(parser, "m.m-1.m", m.multiplyBy(m.raiseTo(-1)).multiplyBy(m));
    myAssert(parser, "2.0 m 1/2 s-1*(m/s^1)^-1 (1e9 m-1)(1e9 s-1)-1.m/s",
        m.multiplyBy(2).multiplyBy(1. / 2.).multiplyBy(s.raiseTo(-1)).multiplyBy(m.divideBy(s.raiseTo(1)).raiseTo(-1))
            .multiplyBy(m.raiseTo(-1).multiplyBy(1e9)).multiplyBy(s.raiseTo(-1).multiplyBy(1e9).raiseTo(-1))
            .multiplyBy(m).divideBy(s));
    myAssert(parser, "m/km", m.divideBy(m.multiplyBy(1e3)));
  }

  private static void myAssert(StandardUnitFormat parser, final String spec, final Unit unit) throws Exception {
    if (!parser.parse(spec).equals(unit)) {
      throw new AssertionError(spec + " != " + unit);
    }
    System.out.println(spec + " -> " + unit);
  }

  @Test
  public void testUnitDimension() throws Exception {
    System.out.println("new UnitDimension() = \"" + new UnitDimension() + '"');
    UnitDimension timeDimension =
        new UnitDimension(BaseUnit.getOrCreate(UnitName.newUnitName("second", null, "s"), BaseQuantity.TIME));
    System.out.println("timeDimension = \"" + timeDimension + '"');
    UnitDimension lengthDimension =
        new UnitDimension(BaseUnit.getOrCreate(UnitName.newUnitName("meter", null, "m"), BaseQuantity.LENGTH));
    System.out.println("lengthDimension = \"" + lengthDimension + '"');
    System.out.println(
        "lengthDimension.isReciprocalOf(timeDimension) = \"" + lengthDimension.isReciprocalOf(timeDimension) + '"');
    UnitDimension hertzDimension = timeDimension.raiseTo(-1);
    System.out.println("hertzDimension = \"" + hertzDimension + '"');
    System.out.println(
        "hertzDimension.isReciprocalOf(timeDimension) = \"" + hertzDimension.isReciprocalOf(timeDimension) + '"');
    System.out.println("lengthDimension.divideBy(timeDimension) = \"" + lengthDimension.divideBy(timeDimension) + '"');
    System.out.println("lengthDimension.divideBy(timeDimension).raiseTo(2) = \""
        + lengthDimension.divideBy(timeDimension).raiseTo(2) + '"');
  }
}
