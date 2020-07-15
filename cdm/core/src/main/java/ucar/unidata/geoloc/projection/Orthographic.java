/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Orthographic Projection spherical earth.
 * <p/>
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 145
 */
@Immutable
public class Orthographic extends AbstractProjection {
  private final double lat0, lon0; // center lat/lon in radians
  private final double R; // radius

  /** constants from Snyder's equations */
  private double lon0Degrees;
  private double cosLat0, sinLat0;

  @Override
  public Projection constructCopy() {
    return new Orthographic(getOriginLat(), getOriginLon(), R);
  }

  public Orthographic() {
    this(0.0, 0.0);
  }

  /**
   * Construct a Orthographic Projection.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   */
  public Orthographic(double lat0, double lon0) {
    this(lat0, lon0, EARTH_RADIUS);
  }

  /**
   * Construct a Orthographic Projection, specify earth radius
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param earthRadius radius of the earth
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public Orthographic(double lat0, double lon0, double earthRadius) {
    super("Orthographic", false);

    this.lon0Degrees = lon0;
    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);
    this.R = earthRadius;

    this.sinLat0 = Math.sin(this.lat0);
    this.cosLat0 = Math.cos(this.lat0);

    addParameter(CF.GRID_MAPPING_NAME, CF.ORTHOGRAPHIC);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lon0);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Orthographic that = (Orthographic) o;

    if (Double.compare(that.R, R) != 0)
      return false;
    if (Double.compare(that.lat0, lat0) != 0)
      return false;
    if (Double.compare(that.lon0, lon0) != 0)
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = lat0 != +0.0d ? Double.doubleToLongBits(lat0) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = lon0 != +0.0d ? Double.doubleToLongBits(lon0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = R != +0.0d ? Double.doubleToLongBits(R) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  // bean properties

  /**
   * Get the origin longitude in degrees
   *
   * @return the origin longitude.
   */
  public double getOriginLon() {
    return Math.toDegrees(lon0);
  }

  /**
   * Get the origin latitude in degrees
   *
   * @return the origin latitude.
   */
  public double getOriginLat() {
    return Math.toDegrees(lat0);
  }

  @Override
  public String getProjectionTypeLabel() {
    return "Orthographic";
  }

  @Override
  public String paramsToString() {
    return toString();
  }

  @Override
  public String toString() {
    return "Orthographic{" + "lat0=" + lat0 + ", lon0=" + lon0 + ", R=" + R + '}';
  }

  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   *
   * @param pt1 point 1
   * @param pt2 point 2
   * @return true when the line between pt1 and pt2 crosses the seam.
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2))
      return true;

    // opposite signed X values, larger then 5000 km
    return (pt1.getX() * pt2.getX() < 0) && (Math.abs(pt1.getX() - pt2.getX()) > 5000.0);
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();


    fromLat = Math.toRadians(fromLat);
    double lonDiff = Math.toRadians(LatLonPoints.lonNormal(fromLon - lon0Degrees));
    double cosc = sinLat0 * Math.sin(fromLat) + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);
    if (cosc >= 0) {
      toX = R * Math.cos(fromLat) * Math.sin(lonDiff);
      toY = R * (cosLat0 * Math.sin(fromLat) - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
    } else {
      toX = Double.POSITIVE_INFINITY;
      toY = Double.POSITIVE_INFINITY;
    }

    return ProjectionPoint.create(toX, toY);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = world.getX();
    double fromY = world.getY();

    double rho = Math.sqrt(fromX * fromX + fromY * fromY);
    double c = Math.asin(rho / R);

    toLon = lon0;
    double temp = 0;
    if (Math.abs(rho) > TOLERANCE) {
      toLat = Math.asin(Math.cos(c) * sinLat0 + (fromY * Math.sin(c) * cosLat0 / rho));
      if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) { // not 90 or -90
        temp = rho * cosLat0 * Math.cos(c) - fromY * sinLat0 * Math.sin(c);
        toLon = lon0 + Math.atan(fromX * Math.sin(c) / temp);
      } else if (Double.compare(lat0, PI_OVER_4) == 0) {
        toLon = lon0 + Math.atan(fromX / -fromY);
        temp = -fromY;
      } else {
        toLon = lon0 + Math.atan(fromX / fromY);
        temp = fromY;
      }
    } else {
      toLat = lat0;
    }
    toLat = Math.toDegrees(toLat);
    toLon = Math.toDegrees(toLon);
    if (temp < 0) {
      toLon += 180;
    }
    toLon = LatLonPoints.lonNormal(toLon);

    return LatLonPoint.create(toLat, toLon);
  }

}

