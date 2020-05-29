/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

/**
 * A location on the earth, specified by lat, lon and optionally altitude.
 *
 * @author caron
 * @since Feb 18, 2008
 */
public interface EarthLocation {
  /** Standard way to create an EarthLocation. */
  static EarthLocation create(double lat, double lon, double alt) {
    return new EarthLocationImpl(lat, lon, alt);
  }

  /**
   * Returns the latitude in some unit. The unit is very likely decimal degrees north, but we don't enforce that
   * anywhere.
   *
   * @return the latitude in some unit.
   */
  // FIXME: Enforce the "decimal degrees north" unit in EarthLocationImpl and other subclasses.
  // Or, allow a different unit and make it available from EarthLocation.
  double getLatitude();

  /**
   * Returns the longitude in some unit. The unit is very likely decimal degrees east, but we don't enforce that
   * anywhere.
   *
   * @return the longitude in some unit.
   */
  // FIXME: Enforce the "decimal degrees east" unit in EarthLocationImpl and other subclasses.
  // Or, allow a different unit and make it available from EarthLocation.
  double getLongitude();

  /**
   * Returns the altitude in some unit.
   *
   * @return the altitude in some unit. A value of {@link Double#NaN} indicates "no altitude".
   */
  // FIXME: Make the unit available from EarthLocation.
  double getAltitude();

  /**
   * Get the lat/lon location
   * 
   * @return lat/lon location
   */
  LatLonPoint getLatLon();

  /**
   * Are either lat or lon missing?
   * 
   * @return true if lat or lon is missing
   */
  boolean isMissing();
}
