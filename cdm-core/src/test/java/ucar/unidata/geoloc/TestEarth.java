/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import static com.google.common.truth.Truth.assertThat;
import static ucar.unidata.geoloc.Earth.WGS84_EARTH_RADIUS_METERS;

import java.io.IOException;
import org.junit.Test;

/** Test {@link ucar.unidata.geoloc.Earth}, {@link ucar.unidata.geoloc.EarthEllipsoid} */
public class TestEarth {

  @Test
  public void testSphericalEarth() {
    Earth earth = new Earth();
    Earth earth2 = new Earth(WGS84_EARTH_RADIUS_METERS);
    assertThat(earth).isEqualTo(earth2);
    assertThat(earth.hashCode()).isEqualTo(earth2.hashCode());

    assertThat(earth.getName()).isEqualTo("spherical_earth");
    assertThat(earth.toString()).isEqualTo("spherical_earth equatorRadius=6371229.000000 inverseFlattening=Infinity");
    assertThat(earth.getEccentricity()).isEqualTo(1.0);
    assertThat(earth.getEccentricitySquared()).isEqualTo(1.0);
    assertThat(earth.getEquatorRadius()).isEqualTo(WGS84_EARTH_RADIUS_METERS);
    assertThat(earth.getPoleRadius()).isEqualTo(WGS84_EARTH_RADIUS_METERS);
    assertThat(earth.getMajor()).isEqualTo(earth.getEquatorRadius());
    assertThat(earth.getMinor()).isEqualTo(earth.getPoleRadius());
    assertThat(earth.getFlattening()).isEqualTo(0.0);
  }

  @Test
  public void testEllipticalEarth() {
    EarthEllipsoid earth = EarthEllipsoid.getType("WGS84");
    assertThat(earth).isNotNull();
    Earth earth2 = EarthEllipsoid.getType(7030);
    assertThat(earth2).isNotNull();
    assertThat(earth).isEqualTo(earth2);
    assertThat(earth.hashCode()).isEqualTo(earth2.hashCode());

    assertThat(earth.getEpsgId()).isEqualTo(7030);
    assertThat(earth.getName()).isEqualTo("WGS84");
    assertThat(earth.toString()).isEqualTo("WGS84");
    assertThat(earth.getEquatorRadius()).isEqualTo(6378137.0);

    double flattening = 1.0 / 298.257223563;
    assertThat(earth.getFlattening()).isEqualTo(flattening);
    double ecc = 2 * flattening - flattening * flattening;
    assertThat(earth.getEccentricitySquared()).isEqualTo(ecc);
    assertThat(earth.getMajor()).isEqualTo(earth.getEquatorRadius());
    assertThat(earth.getMinor()).isEqualTo(earth.getPoleRadius());
  }

  @Test
  public void testEllipticalEarthEquals() {
    EarthEllipsoid earth = new EarthEllipsoid("test", 99, 3000.0, 4000.0, 0.0);
    EarthEllipsoid earth2 = new EarthEllipsoid("test", 999, 30900.0, 40900.0, 0.0);
    assertThat(earth).isEqualTo(earth2);
    assertThat(earth.hashCode()).isEqualTo(earth2.hashCode());

    EarthEllipsoid earth3 = new EarthEllipsoid("test3", 99, 3000.0, 4000.0, 0.0);
    assertThat(earth).isNotEqualTo(earth3);
    assertThat(earth.hashCode()).isNotEqualTo(earth3.hashCode());
  }

}
