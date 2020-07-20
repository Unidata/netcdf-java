/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.unidata.geoloc.projection;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * FlatEarth Projection
 * This projection surface is tangent at some point (lat0, lon0) and has a y axis rotated from true North by some angle.
 * <p/>
 * We call it "flat" because it should only be used where the spherical geometry of the earth is not significant.
 * In actuallity, we use the simple "arclen" routine which computes dy along a meridian, and dx along a
 * latitude circle. We rotate the coordinate system to/from a true north system.
 * <p/>
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 145
 */
@Immutable
public class FlatEarth extends AbstractProjection {
  public static final String ROTATIONANGLE = "rotationAngle";

  /** constants from Snyder's equations */
  private final double rotAngle, radius;
  private final double lat0, lon0; // center lat/lon in radians

  // values passed in through the constructor
  // need for constructCopy
  private final double _lat0, _lon0;

  private final double cosRot, sinRot;

  @Override
  public Projection constructCopy() {
    return new FlatEarth(getOriginLat(), getOriginLon(), getRotationAngle());
  }

  /** Constructor with default parameters */
  public FlatEarth() {
    this(0.0, 0.0, 0.0, EARTH_RADIUS);
  }

  public FlatEarth(double lat0, double lon0) {
    this(lat0, lon0, 0.0, EARTH_RADIUS);
  }

  public FlatEarth(double lat0, double lon0, double rotAngle) {
    this(lat0, lon0, rotAngle, EARTH_RADIUS);
  }

  /**
   * Construct a FlatEarth Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param rotAngle angle of rotation, in degrees
   * @param radius earth radius in km
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public FlatEarth(double lat0, double lon0, double rotAngle, double radius) {
    super("FlatEarth", false);

    this._lat0 = lat0;
    this._lon0 = lon0;

    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);
    this.rotAngle = Math.toRadians(rotAngle);
    this.radius = radius;

    this.sinRot = Math.sin(this.rotAngle);
    this.cosRot = Math.cos(this.rotAngle);

    addParameter(CF.GRID_MAPPING_NAME, "flat_earth");
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lon0);
    addParameter(ROTATIONANGLE, rotAngle);
    addParameter(CF.EARTH_RADIUS, radius * 1000);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    FlatEarth flatEarth = (FlatEarth) o;

    if (Double.compare(flatEarth.lat0, lat0) != 0)
      return false;
    if (Double.compare(flatEarth.lon0, lon0) != 0)
      return false;
    if (Double.compare(flatEarth.radius, radius) != 0)
      return false;
    if (Double.compare(flatEarth.rotAngle, rotAngle) != 0)
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = rotAngle != +0.0d ? Double.doubleToLongBits(rotAngle) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = radius != +0.0d ? Double.doubleToLongBits(radius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = lat0 != +0.0d ? Double.doubleToLongBits(lat0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = lon0 != +0.0d ? Double.doubleToLongBits(lon0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  // bean properties

  /**
   * Get the origin longitude in degrees.
   *
   * @return the origin longitude in degrees.
   */
  public double getOriginLon() {
    return _lon0;
  }

  /**
   * Get the origin latitude in degrees.
   *
   * @return the origin latitude in degrees.
   */
  public double getOriginLat() {
    return _lat0;
  }

  /**
   * Get the rotation angle.
   *
   * @return the origin latitude.
   */
  public double getRotationAngle() {
    return rotAngle;
  }

  @Override
  public String getProjectionTypeLabel() {
    return "FlatEarth";
  }

  @Override
  public String paramsToString() {
    return toString();
  }

  @Override
  public String toString() {
    return "FlatEarth{" + "rotAngle=" + rotAngle + ", radius=" + radius + ", lat0=" + _lat0 + ", lon0=" + _lon0 + '}';
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();
    double dx, dy;

    fromLat = Math.toRadians(fromLat);

    dy = radius * (fromLat - lat0);
    dx = radius * Math.cos(fromLat) * (Math.toRadians(fromLon) - lon0);
    toX = cosRot * dx - sinRot * dy;
    toY = sinRot * dx + cosRot * dy;

    return ProjectionPoint.create(toX, toY);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double x = world.getX();
    double y = world.getY();
    double cosl;
    double xp, yp;

    xp = cosRot * x + sinRot * y;
    yp = -sinRot * x + cosRot * y;

    toLat = Math.toDegrees(lat0) + Math.toDegrees(yp / radius);
    // double lat2;
    // lat2 = lat0 + Math.toDegrees(yp/radius);
    cosl = Math.cos(Math.toRadians(toLat));
    if (Math.abs(cosl) < TOLERANCE) {
      toLon = Math.toDegrees(lon0);
    } else {
      toLon = Math.toDegrees(lon0) + Math.toDegrees(xp / cosl / radius);
    }
    toLon = LatLonPoints.lonNormal(toLon);

    return LatLonPoint.create(toLat, toLon);
  }

  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return (pt1.getX() * pt2.getX() < 0);
  }

}

