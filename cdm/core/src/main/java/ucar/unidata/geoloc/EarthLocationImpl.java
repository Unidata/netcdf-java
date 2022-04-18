/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

/**
 * A location on the Earth.
 *
 * @author caron
 * @deprecated only use EarthLocation in 6. This class will be renamed.
 */
@Deprecated
public class EarthLocationImpl implements EarthLocation {
  protected double lat, lon, alt;

  public double getLatitude() {
    return lat;
  }

  public double getLongitude() {
    return lon;
  }

  public double getAltitude() {
    return alt;
  }

  public LatLonPoint getLatLon() {
    return LatLonPoint.create(lat, lon);
  }

  public boolean isMissing() {
    return Double.isNaN(lat) || Double.isNaN(lon);
  }

  protected EarthLocationImpl() {}

  /** @deprecated use EarthLocation.create */
  @Deprecated
  public EarthLocationImpl(double lat, double lon, double alt) {
    this.lat = lat;
    this.lon = lon;
    this.alt = alt;
  }

  protected void setLatitude(double lat) {
    this.lat = lat;
  }

  protected void setLongitude(double lon) {
    this.lon = lon;
  }

  protected void setAltitude(double alt) {
    this.alt = alt;
  }

  public String toString() {
    return "lat=" + lat + " lon=" + lon + " alt=" + alt;
  }


}
