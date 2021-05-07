/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import com.google.auto.value.AutoValue;
import ucar.nc2.util.Misc;

/**
 * Points on the Earth's surface, represented as (longitude,latitude), in units of degrees.
 * Longitude is always between -180 and +180 deg.
 * Latitude is always between -90 and +90 deg.
 */
@AutoValue
public abstract class LatLonPoint {
  public static LatLonPoint INVALID = LatLonPoint.create(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

  /** Create a LatLonPoint. */
  public static LatLonPoint create(double lat, double lon) {
    return new AutoValue_LatLonPoint(LatLonPoints.latNormal(lat), LatLonPoints.lonNormal(lon));
  }

  /** Returns the latitude, between +/- 90 degrees. */
  public abstract double getLatitude();

  /** Returns the longitude, between +/-180 degrees */
  public abstract double getLongitude();

  /**
   * Returns the result of {@link #nearlyEquals(LatLonPoint, double)}, with {@link Misc#defaultMaxRelativeDiffDouble}.
   */
  public boolean nearlyEquals(LatLonPoint that) {
    return nearlyEquals(that, Misc.defaultMaxRelativeDiffDouble);
  }

  /**
   * Returns {@code true} if this point is nearly equal to {@code that}. The "near equality" of points is determined
   * using {@link Misc#nearlyEquals(double, double, double)}, with the specified maxRelDiff.
   *
   * @param that the other point to check.
   * @param maxRelDiff the maximum {@link Misc#relativeDifference relative difference} the two points may have.
   * @return {@code true} if this point is nearly equal to {@code that}.
   */
  public boolean nearlyEquals(LatLonPoint that, double maxRelDiff) {
    boolean lonOk = Misc.nearlyEquals(that.getLongitude(), getLongitude(), maxRelDiff);
    if (!lonOk) {
      // We may be in a situation where "this.lon ≈ -180" and "that.lon ≈ +180", or vice versa.
      // Those longitudes are equivalent, but not "nearlyEquals()". So, we normalize them both to lie in
      // [0, 360] and compare them again.
      lonOk = Misc.nearlyEquals(LatLonPoints.lonNormal360(that.getLongitude()),
          LatLonPoints.lonNormal360(this.getLongitude()), maxRelDiff);
    }
    return lonOk && Misc.nearlyEquals(that.getLatitude(), getLatitude(), maxRelDiff);
  }

  @Override
  public String toString() {
    return LatLonPoints.toString(this, 5);
  }

}
