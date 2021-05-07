/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.Format;
import java.lang.invoke.MethodHandles;
import java.util.Random;

/** Test basic projection methods */
public class TestBasic {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final boolean debug1 = false;

  //////////////////// testLatLonNormal ///////////////////////////

  void showLatLonNormal(double lon, double center) {
    System.out.println(
        Format.formatDouble(lon, 8, 5) + " => " + Format.formatDouble(LatLonPoints.lonNormal(lon, center), 8, 5));
  }

  void runCenter(double center) {
    for (double lon = 0.0; lon < 380.0; lon += 22.5) {
      if (debug1)
        showLatLonNormal(lon, center);
      double result = LatLonPoints.lonNormal(lon, center);
      assert (result >= center - 180.);
      assert (result <= center + 180.);
      assert ((result == lon) || (Math.abs(result - lon) == 360) || (Math.abs(result - lon) == 720));
    }
  }

  @Test
  public void testLatLonNormal() {
    runCenter(10.45454545454547);
    runCenter(110.45454545454547);
    runCenter(210.45454545454547);
    runCenter(-10.45454545454547);
    runCenter(-110.45454545454547);
    runCenter(-210.45454545454547);
    runCenter(310.45454545454547);
  }

}
