/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import java.util.Formatter;

/**
 * Static utilities for LatLonPoint.
 *
 * @author caron
 * @since 3/15/2020.
 */
public class LatLonPoints {

  /**
   * Test if point lies between two longitudes, deal with wrapping.
   *
   * @param lon point to test
   * @param lonBeg beginning longitude
   * @param lonEnd ending longitude
   * @return true if lon is between lonBeg and lonEnd.
   */
  public static boolean betweenLon(double lon, double lonBeg, double lonEnd) {
    lonBeg = lonNormal(lonBeg, lon);
    lonEnd = lonNormal(lonEnd, lon);
    return (lon >= lonBeg) && (lon <= lonEnd);
  }

  /**
   * put longitude into the range [-180, 180] deg
   *
   * @param lon lon to normalize
   * @return longitude in range [-180, 180] deg
   */
  public static double range180(double lon) {
    return lonNormal(lon);
  }

  /**
   * put longitude into the range [0, 360] deg
   *
   * @param lon lon to normalize
   * @return longitude into the range [0, 360] deg
   */
  public static double lonNormal360(double lon) {
    return lonNormal(lon, 180.0);
  }

  /**
   * put longitude into the range [center +/- 180] deg
   *
   * @param lon lon to normalize
   * @param center center point
   * @return longitude into the range [center +/- 180] deg
   */
  public static double lonNormal(double lon, double center) {
    return center + Math.IEEEremainder(lon - center, 360.0);
  }

  /**
   * put longitude into the range [start, start+360] deg
   *
   * @param lon lon to normalize
   * @param start starting point
   * @return longitude into the [start, start+360] deg
   */
  public static double lonNormalFrom(double lon, double start) {
    while (lon < start)
      lon += 360;
    while (lon > start + 360)
      lon -= 360;
    return lon;
  }

  /**
   * Normalize the longitude to lie between +/-180
   *
   * @param lon east latitude in degrees
   * @return normalized lon
   */
  public static double lonNormal(double lon) {
    if ((lon < -180.) || (lon > 180.)) {
      return Math.IEEEremainder(lon, 360.0);
    } else {
      return lon;
    }
  }

  /**
   * Find difference (lon1 - lon2) normalized so that maximum value is += 180.
   *
   * @param lon1 start
   * @param lon2 end
   * @return normalized difference
   */
  public static double lonDiff(double lon1, double lon2) {
    return Math.IEEEremainder(lon1 - lon2, 360.0);
  }

  /**
   * Normalize the latitude to lie between +/-90
   *
   * @param lat north latitude in degrees
   * @return normalized lat
   */
  public static double latNormal(double lat) {
    if (lat < -90.) {
      return -90.;
    } else if (lat > 90.) {
      return 90.;
    } else {
      return lat;
    }
  }

  /**
   * Make a nicely formatted representation of a latitude, eg 40.34N or 12.9S.
   *
   * @param lat the latitude.
   * @param ndec number of digits to right of decimal point
   * @return String representation.
   */
  public static String latToString(double lat, int ndec) {
    boolean is_north = (lat >= 0.0);
    if (!is_north)
      lat = -lat;

    String f = "%." + ndec + "f";

    Formatter latBuff = new Formatter();
    latBuff.format(f, lat);
    latBuff.format("%s", is_north ? "N" : "S");

    return latBuff.toString();
  }

  /**
   * Make a nicely formatted representation of a longitude, eg 120.3W or 99.99E.
   *
   * @param lon the longitude.
   * @param ndec number of digits to right of decimal point
   * @return String representation.
   */
  public static String lonToString(double lon, int ndec) {
    double wlon = lonNormal(lon);
    boolean is_east = (wlon >= 0.0);
    if (!is_east)
      wlon = -wlon;

    String f = "%." + ndec + "f";

    Formatter latBuff = new Formatter();
    latBuff.format(f, wlon);
    latBuff.format("%s", is_east ? "E" : "W");

    return latBuff.toString();
  }

  /**
   * See if either coordinate in <code>pt</code> is +/- infinite.
   * This happens sometimes in projective geometry.
   *
   * @param pt point to check
   * @return true if either coordinate is +/- infinite.
   */
  public static boolean isInfinite(ProjectionPoint pt) {
    return (pt.getX() == java.lang.Double.POSITIVE_INFINITY) || (pt.getX() == java.lang.Double.NEGATIVE_INFINITY)
        || (pt.getY() == java.lang.Double.POSITIVE_INFINITY) || (pt.getY() == java.lang.Double.NEGATIVE_INFINITY);
  }

}
