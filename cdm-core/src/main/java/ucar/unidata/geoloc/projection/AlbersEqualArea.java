/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Albers Equal Area Conic Projection, one or two standard parallels, spherical earth.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 98
 */
@Immutable
public class AlbersEqualArea extends AbstractProjection {
  private final double lat0, lon0; // radians
  private final double par1, par2; // degrees
  private final double falseEasting, falseNorthing;
  private final double earth_radius; // radius in km

  // values passed in through the constructor
  // need for constructCopy
  private final double _lat0, _lon0;

  /** constants from Snyder's equations */
  private final double n, C, rho0, lon0Degrees;

  /** copy constructor - avoid clone !! */
  public Projection constructCopy() {
    return new AlbersEqualArea(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(), getFalseEasting(),
        getFalseNorthing(), getEarthRadius());
  }

  /**
   * Constructor with default parameters
   */
  public AlbersEqualArea() {
    this(23, -96, 29.5, 45.5);
  }

  /**
   * Construct a AlbersEqualArea Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public AlbersEqualArea(double lat0, double lon0, double par1, double par2) {
    this(lat0, lon0, par1, par2, 0, 0, Earth.WGS84_EARTH_RADIUS_KM);
  }

  /**
   * Construct a AlbersEqualArea Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @param falseEasting false easting in km
   * @param falseNorthing false easting in km
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public AlbersEqualArea(double lat0, double lon0, double par1, double par2, double falseEasting,
      double falseNorthing) {
    this(lat0, lon0, par1, par2, falseEasting, falseNorthing, Earth.WGS84_EARTH_RADIUS_KM);
  }

  /**
   * Construct a AlbersEqualArea Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @param falseEasting false easting in km
   * @param falseNorthing false easting in km
   * @param earth_radius radius of the earth in km
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public AlbersEqualArea(double lat0, double lon0, double par1, double par2, double falseEasting, double falseNorthing,
      double earth_radius) {
    super("AlbersEqualArea", false);

    this._lat0 = lat0;
    this._lon0 = lon0;

    this.lon0Degrees = lon0;
    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);

    this.par1 = par1;
    this.par2 = par2;

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;
    this.earth_radius = earth_radius;

    double par1r = Math.toRadians(this.par1);
    double par2r = Math.toRadians(this.par2);

    if (Math.abs(par2 - par1) < TOLERANCE) { // single parallel
      this.n = Math.sin(par1r);
    } else {
      this.n = (Math.sin(par1r) + Math.sin(par2r)) / 2.0;
    }

    double c2 = Math.pow(Math.cos(par1r), 2);
    this.C = c2 + 2 * n * Math.sin(par1r);
    this.rho0 = computeRho(this.lat0);

    addParameter(CF.GRID_MAPPING_NAME, CF.ALBERS_CONICAL_EQUAL_AREA);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);

    if (par2 == par1) {
      addParameter(CF.STANDARD_PARALLEL, par1);
    } else {
      addParameter(Attribute.builder(CF.STANDARD_PARALLEL).setValues(ImmutableList.of(par1, par2), false).build());
    }

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.EARTH_RADIUS, earth_radius * 1000); // must be in meters
  }

  private double computeRho(double lat) {
    return earth_radius * Math.sqrt(C - 2 * n * Math.sin(lat)) / n;
  }

  private double computeTheta(double lon) {
    double dlon = LatLonPoints.lonNormal(Math.toDegrees(lon) - lon0Degrees);
    return n * Math.toRadians(dlon);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AlbersEqualArea that = (AlbersEqualArea) o;

    if (Double.compare(that.earth_radius, earth_radius) != 0)
      return false;
    if (Double.compare(that.falseEasting, falseEasting) != 0)
      return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0)
      return false;
    if (Double.compare(that.lat0, lat0) != 0)
      return false;
    if (Double.compare(that.lon0, lon0) != 0)
      return false;
    if (Double.compare(that.par1, par1) != 0)
      return false;
    if (Double.compare(that.par2, par2) != 0)
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
    temp = par1 != +0.0d ? Double.doubleToLongBits(par1) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = par2 != +0.0d ? Double.doubleToLongBits(par2) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = earth_radius != +0.0d ? Double.doubleToLongBits(earth_radius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  ///////////// bean properties

  /**
   * Get the second standard parallel
   *
   * @return the second standard parallel
   */
  public double getParallelTwo() {
    return par2;
  }

  /**
   * Get the first standard parallel
   *
   * @return the first standard parallel
   */
  public double getParallelOne() {
    return par1;
  }

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
   * Get the false easting, in km.
   *
   * @return the false easting.
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Get the false northing, in km.
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  /**
   * Earth radius in km
   * 
   * @return Earth radius in km
   */
  public double getEarthRadius() {
    return this.earth_radius;
  }

  @Override
  public String toString() {
    return "AlbersEqualArea{" + "name='" + name + '\'' + ", lat0=" + lat0 + ", lon0=" + lon0 + ", par1=" + par1
        + ", par2=" + par2 + ", falseEasting=" + falseEasting + ", falseNorthing=" + falseNorthing + ", earth_radius="
        + earth_radius + ", _lat0=" + _lat0 + ", _lon0=" + _lon0 + '}';
  }

  /**
   * Get the scale at the given lat.
   *
   * @param lat lat to use
   * @return scale factor at that latitude
   */
  public double getScale(double lat) {
    lat = Math.toRadians(lat);
    double n = Math.cos(lat);
    double d = Math.sqrt(C - 2 * n * Math.sin(lat));
    return n / d;
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
    fromLon = Math.toRadians(fromLon);
    double rho = computeRho(fromLat);
    double theta = computeTheta(fromLon);

    toX = rho * Math.sin(theta) + falseEasting;
    toY = rho0 - rho * Math.cos(theta) + falseNorthing;

    return ProjectionPoint.create(toX, toY);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;
    double rrho0 = rho0;

    if (n < 0) {
      rrho0 *= -1.0;
      fromX *= -1.0;
      fromY *= -1.0;
    }

    double yd = rrho0 - fromY;
    double rho = Math.sqrt(fromX * fromX + yd * yd);
    double theta = Math.atan2(fromX, yd);
    if (n < 0) {
      rho *= -1.0;
    }
    toLat = Math.toDegrees(Math.asin((C - Math.pow((rho * n / earth_radius), 2)) / (2 * n)));
    toLon = Math.toDegrees(theta / n + lon0);

    return LatLonPoint.create(toLat, toLon);
  }

}

