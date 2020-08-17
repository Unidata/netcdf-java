/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Test {@link ucar.unidata.geoloc.EarthLocation}, {@link ucar.unidata.geoloc.Station} */
public class TestStation {
  private final String name = "terrapin";
  private final String desc = "station";
  private final String wmo = "why";
  private final double lat = 40.0;
  private final double lon = -105.0;
  private final double alt = 5240.0;
  private final String altUnits = "ft";

  @Test
  public void testEarthLocation() {
    EarthLocation loc = EarthLocation.create(lat, lon, alt, altUnits);

    assertThat(loc.getLatitude()).isEqualTo(lat);
    assertThat(loc.getLongitude()).isEqualTo(lon);
    assertThat(loc.getAltitude()).isEqualTo(alt);
    assertThat(loc.getAltitudeUnits()).isEqualTo(altUnits);
    assertThat(loc.isMissing()).isFalse();

    EarthLocation loc1 = EarthLocation.create(lat, lon, Double.NaN);
    EarthLocation loc2 = new EarthLocation(lat, lon, Double.NaN, null);
    assertThat(loc1).isEqualTo(loc2);
    assertThat(loc1.hashCode()).isEqualTo(loc2.hashCode());
    assertThat(loc1.isMissing()).isFalse();
  }

  @Test
  public void testStation() {
    Station s = new Station(name, desc, wmo, lat, lon, alt, 99);
    Station sb = Station.builder("name").setName(name).setDescription(desc).setWmoId(wmo).setLatitude(lat)
        .setLongitude(lon).setAltitude(alt).setNobs(99).build();
    assertThat(s).isEqualTo(sb);
    assertThat(s.hashCode()).isEqualTo(sb.hashCode());

    assertThat(s.getName()).isEqualTo(name);
    assertThat(s.getDescription()).isEqualTo(desc);
    assertThat(s.getWmoId()).isEqualTo(wmo);
    assertThat(s.getNobs()).isEqualTo(99);

    assertThat(s.getLatitude()).isEqualTo(lat);
    assertThat(s.getLongitude()).isEqualTo(lon);
    assertThat(s.getAltitude()).isEqualTo(alt);
    assertThat(s.getAltitudeUnits()).isEqualTo(null);
    assertThat(s.isMissing()).isFalse();
  }

}
