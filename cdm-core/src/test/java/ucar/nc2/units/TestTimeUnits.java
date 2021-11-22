/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import ucar.nc2.calendar.CalendarDate;

/** Test {@link ucar.nc2.units.TimeUnit} */
public class TestTimeUnits {

  @Test
  public void testTimeUnitConstruction() throws Exception {
    TimeUnit tu = TimeUnit.create(3.0, "hours");
    assertThat(tu.getValue()).isEqualTo(3.0);
    assertThat(3600.0 * tu.getValue()).isEqualTo(tu.getValueInSeconds());
    assertThat(tu.getUnitString()).isEqualTo("hours");

    assertThat(tu.getValueInSeconds()).isEqualTo(tu.getFactor() * tu.getValue());
    assertThat(tu.getFactor()).isEqualTo(3600.0);

    TimeUnit day = TimeUnit.create(1.0, "day");
    double hoursInDay = day.convertTo(1.0, tu);
    assertThat(hoursInDay).isEqualTo(24.0);

    // note the value is ignored, only the base unit is used
    day = TimeUnit.create(10.0, "day");
    hoursInDay = day.convertTo(1.0, tu);
    assertThat(hoursInDay).isEqualTo(24.0);

    hoursInDay = day.convertTo(10.0, tu);
    assertThat(hoursInDay).isEqualTo(240.0);

    assertThrows(IllegalArgumentException.class, () -> TimeUnit.create(""));
  }

  @Test
  public void testCopyConstructor() throws Exception {
    TimeUnit tu = TimeUnit.create(3.0, "hours");
    TimeUnit tuCopy = TimeUnit.create(tu.getValue(), tu.getUnitString());
    assertThat(tuCopy).isEqualTo(tu);
    assertThat(tuCopy.hashCode()).isEqualTo(tu.hashCode());
  }

  @Test
  public void testNewValueInSeconds() throws Exception {
    TimeUnit tu = TimeUnit.create(3.0, "hours");
    TimeUnit tun = tu.newValueInSeconds(1800);
    assertThat(tun.getValueInSeconds()).isEqualTo(1800);
    assertThat(tun.getValue()).isEqualTo(.5);

    assertThat(tun).isNotEqualTo(tu);
    assertThat(tun.hashCode()).isNotEqualTo(tu.hashCode());
  }

  @Test
  public void testAddCalendarDate() throws Exception {
    TimeUnit tu = TimeUnit.create(3.0, "hours");
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(null, "1950-01-02T16:45:56").orElseThrow();
    CalendarDate cdd = tu.add(cd);

    CalendarDate expect = CalendarDate.fromUdunitIsoDate(null, "1950-01-02T19:45:56").orElseThrow();
    assertThat(cdd).isEqualTo(expect);
  }

}
