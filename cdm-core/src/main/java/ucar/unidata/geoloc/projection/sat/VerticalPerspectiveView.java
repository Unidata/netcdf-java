/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection.sat;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.AbstractProjection;

/**
 * Vertical Perspective Projection, spherical earth.
 * <p/>
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 176
 */
@Immutable
public class VerticalPerspectiveView extends AbstractProjection {
  private final double lat0, lon0; // center lat/lon in radians
  private final double false_east, false_north;
  private final double R, H;

  // constants from Snyder's equations
  private final double P, lon0Degrees;
  private final double cosLat0, sinLat0;
  private final double maxR; // "map limit" circle of this radius from the origin, p 173

  @Override
  public Projection constructCopy() {
    return new VerticalPerspectiveView(getOriginLat(), getOriginLon(), R, getHeight(), false_east, false_north);
  }

  /**
   * Constructor with default parameters
   */
  public VerticalPerspectiveView() {
    this(0.0, 0.0, EARTH_RADIUS, 35800);
  }

  /**
   * Construct a VerticalPerspectiveView Projection
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param earthRadius radius of the earth (km)
   * @param distance height above the earth (km)
   */
  public VerticalPerspectiveView(double lat0, double lon0, double earthRadius, double distance) {
    this(lat0, lon0, earthRadius, distance, 0, 0);
  }

  /**
   * Construct a VerticalPerspectiveView Projection
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param earthRadius radius of the earth (km)
   * @param distance height above the earth (km)
   * @param false_easting easting offset (km)
   * @param false_northing northing offset (km)
   */
  public VerticalPerspectiveView(double lat0, double lon0, double earthRadius, double distance, double false_easting,
      double false_northing) {

    super("VerticalPerspectiveView", false);

    this.lon0Degrees = lon0;
    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);
    this.R = earthRadius;
    this.H = distance;
    this.false_east = false_easting;
    this.false_north = false_northing;

    this.sinLat0 = Math.sin(this.lat0);
    this.cosLat0 = Math.cos(this.lat0);
    this.P = 1.0 + H / R;

    // "map limit" circle of this radius from the origin, p 173
    this.maxR = .99 * R * Math.sqrt((P - 1) / (P + 1));

    addParameter(CF.GRID_MAPPING_NAME, CF.VERTICAL_PERSPECTIVE);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lon0);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);
    addParameter(CF.PERSPECTIVE_POINT_HEIGHT, distance * 1000);
    if (false_easting != 0 || false_northing != 0) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    VerticalPerspectiveView that = (VerticalPerspectiveView) o;

    if (Double.compare(that.H, H) != 0)
      return false;
    if (Double.compare(that.R, R) != 0)
      return false;
    if (Double.compare(that.false_east, false_east) != 0)
      return false;
    if (Double.compare(that.false_north, false_north) != 0)
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
    temp = false_east != +0.0d ? Double.doubleToLongBits(false_east) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = false_north != +0.0d ? Double.doubleToLongBits(false_north) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = R != +0.0d ? Double.doubleToLongBits(R) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = H != +0.0d ? Double.doubleToLongBits(H) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  // bean properties

  /**
   * Get the height above the earth
   *
   * @return the height above the earth
   */
  public double getHeight() {
    return H;
  }

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

  public double getP() {
    return P;
  }

  @Override
  public String toString() {
    return "VerticalPerspectiveView{" + "lat0=" + lat0 + ", lon0=" + lon0 + ", false_east=" + false_east
        + ", false_north=" + false_north + ", R=" + R + ", H=" + H + ", P=" + P + '}';
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
    if (LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2)) {
      return true;
    }
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
    double ksp = (P - 1.0) / (P - cosc);
    if (cosc < 1.0 / P) {
      toX = Double.POSITIVE_INFINITY;
      toY = Double.POSITIVE_INFINITY;
    } else {
      toX = false_east + R * ksp * Math.cos(fromLat) * Math.sin(lonDiff);
      toY = false_north + R * ksp * (cosLat0 * Math.sin(fromLat) - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
    }

    return ProjectionPoint.create(toX, toY);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = world.getX();
    double fromY = world.getY();


    fromX = fromX - false_east;
    fromY = fromY - false_north;
    double rho = Math.sqrt(fromX * fromX + fromY * fromY);
    double r = rho / R;
    double con = P - 1.0;
    double com = P + 1.0;
    double c = Math.asin((P - Math.sqrt(1.0 - (r * r * com) / con)) / (con / r + r / con));

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

  /**
   * Create a ProjectionRect from the given LatLonRect.
   * Handles lat/lon points that do not intersect the projection panel.
   *
   * @param rect the LatLonRect
   * @return ProjectionRect, or null if no part of the LatLonRect intersects the projection plane
   */
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect rect) {
    BoundingBoxHelper bbhelper = new BoundingBoxHelper(this, maxR);
    return bbhelper.latLonToProjBB(rect);
  }

}

