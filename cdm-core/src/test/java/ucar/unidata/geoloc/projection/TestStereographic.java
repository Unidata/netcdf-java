/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.junit.Test;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.ProjectionPoint;

import static com.google.common.truth.Truth.assertThat;

public class TestStereographic {
  static final double TOL = Misc.defaultMaxRelativeDiffFloat;

  @Test
  public void testBasics() {
    Stereographic lc = Stereographic.factory(0.0, 0.0, 15.0);
    assertThat(lc.getTangentLat()).isWithin(TOL).of(0);
    assertThat(lc.getTangentLon()).isWithin(TOL).of(0);
    assertThat(lc.getNaturalOriginLat()).isWithin(TOL).of(0);
    assertThat(lc.getScale()).isWithin(TOL).of(0.6294095225512604);
    assertThat(lc.isNorth()).isFalse();
    assertThat(lc.isPolar()).isFalse();
    assertThat(lc.isLatLon()).isFalse();
    assertThat(lc.crossSeam(ProjectionPoint.create(50, 50), ProjectionPoint.create(5500, 5500))).isFalse();
  }

  @Test
  public void testPolarStereographic() {
    Stereographic lc = new Stereographic(0.0, 0.0, 0.0, true);
    assertThat(lc.getTangentLat()).isWithin(TOL).of(0);
    assertThat(lc.getTangentLon()).isWithin(TOL).of(0);
    assertThat(lc.getNaturalOriginLat()).isWithin(TOL).of(0);
    assertThat(lc.getScale()).isWithin(TOL).of(4.999599621739488E-17);
    assertThat(lc.isNorth()).isTrue();
    assertThat(lc.isPolar()).isTrue();
    assertThat(lc.isLatLon()).isFalse();
  }
}
