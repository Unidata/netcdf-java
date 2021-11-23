/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.junit.Test;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.ProjectionPoint;

import static com.google.common.truth.Truth.assertThat;

public class TestAlbersEqualArea {
  static final double TOL = Misc.defaultMaxRelativeDiffFloat;

  @Test
  public void testGetScale() {
    AlbersEqualArea lc = new AlbersEqualArea(0., 0., 45., 45.);
    assertThat(lc.getScale(90.0)).isWithin(TOL).of(4.999599621739488E-17);
    assertThat(lc.getScale(45.0)).isEqualTo(1.0);
    assertThat(lc.getScale(0.0)).isWithin(TOL).of(0.8164965809277261);
    assertThat(lc.getScale(-45.0)).isWithin(TOL).of(0.4472135954999579);
    assertThat(lc.getScale(-80.0)).isWithin(TOL).of(0.12794491863935908);
    assertThat(lc.getScale(-90.0)).isWithin(TOL).of(4.9995996217394874E-17);
  }

  @Test
  public void testCrossSeam() {
    AlbersEqualArea lc = new AlbersEqualArea(0., 0., 45., 45.);
    assertThat(lc.crossSeam(ProjectionPoint.create(50, 50), ProjectionPoint.create(550, 550))).isFalse();
    assertThat(lc.crossSeam(ProjectionPoint.create(-50000, -50000), ProjectionPoint.create(50000, 50000))).isTrue();
  }
}
