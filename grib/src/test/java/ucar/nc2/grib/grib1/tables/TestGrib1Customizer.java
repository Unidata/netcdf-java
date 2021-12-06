package ucar.nc2.grib.grib1.tables;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TestGrib1Customizer {

  @Test
  public void testStuff() {
    Grib1Customizer cust = new Grib1Customizer(0, null);
    String units = cust.getLevelUnits(110);
    assertThat(units).isNotNull();
  }

}
