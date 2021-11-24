/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.junit.Test;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.ProjectionPoint;

import static com.google.common.truth.Truth.assertThat;

public class TestMercator {
  static final double TOL = Misc.defaultMaxRelativeDiffFloat;

  @Test
  public void testBasics() {
    Mercator lc = new Mercator(0.0, 15.0);
    assertThat(lc.getFalseEasting()).isWithin(TOL).of(0);
    assertThat(lc.getFalseNorthing()).isWithin(TOL).of(0);
    assertThat(lc.getOriginLon()).isWithin(TOL).of(0);
    assertThat(lc.getParallel()).isWithin(TOL).of(15.0);
    assertThat(lc.isLatLon()).isFalse();

    assertThat(Mercator.convertScaleToStandardParallel(1.8917)).isWithin(TOL).of(58.08739826621518);
  }

  @Test
  public void testCrossSeam() {
    Mercator lc = new Mercator(0., 0., 45., 45.);
    assertThat(lc.crossSeam(ProjectionPoint.create(50, 50), ProjectionPoint.create(550, 550))).isFalse();
    assertThat(lc.crossSeam(ProjectionPoint.create(-50000, -50000), ProjectionPoint.create(50000, 50000))).isTrue();
  }
}
