/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** A location on the earth, specified by lat, lon and optionally altitude. */
@Immutable
public class EarthLocation {

  /**
   * Create an EarthLocation.
   * 
   * @param lat latitude in decimal degrees north.
   * @param lon longitude in decimal degrees north.
   * @param alt altitude in meters above the reference surface.
   */
  public static EarthLocation create(double lat, double lon, double alt) {
    return new EarthLocation(lat, lon, alt, null);
  }

  /**
   * Create an EarthLocation.
   * 
   * @param lat latitude in decimal degrees north.
   * @param lon longitude in decimal degrees north.
   * @param alt altitude in meters above the reference surface.
   * @param altUnits units of the altitude.
   */
  public static EarthLocation create(double lat, double lon, double alt, String altUnits) {
    return new EarthLocation(lat, lon, alt, altUnits);
  }

  // This is a value object; would like to use AutoValue, but current public version does not support subclassing.
  private final double latitude;
  private final double longitude;
  private final double altitude;
  private final String altUnits;

  EarthLocation(double latitude, double longitude, double altitude, String altUnits) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
    this.altUnits = altUnits;
  }

  /** The latitude in decimal degrees north. */
  public double getLatitude() {
    return latitude;
  }

  /** The longitude in decimal degrees east. */
  public double getLongitude() {
    return longitude;
  }

  /** Returns the altitude in some unit. A value of {@link Double#NaN} indicates "no altitude". */
  public double getAltitude() {
    return altitude;
  }

  /** Returns the units of the altitude. A null value means "no altitude". */
  @Nullable
  public String getAltitudeUnits() {
    return altUnits;
  }

  /** Get the lat/lon as a LatLonPoint. */
  public LatLonPoint getLatLon() {
    return LatLonPoint.create(getLatitude(), getLongitude());
  }

  /** Are either lat or lon missing? */
  public boolean isMissing() {
    return Double.isNaN(getLatitude()) || Double.isNaN(getLongitude());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    EarthLocation that = (EarthLocation) o;

    if (Double.compare(that.latitude, latitude) != 0) {
      return false;
    }
    if (Double.compare(that.longitude, longitude) != 0) {
      return false;
    }
    if (Double.compare(that.altitude, altitude) != 0) {
      return false;
    }
    return altUnits != null ? altUnits.equals(that.altUnits) : that.altUnits == null;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(latitude);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(longitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(altitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + (altUnits != null ? altUnits.hashCode() : 0);
    return result;
  }
}
