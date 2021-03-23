/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.Parameter;

/**
 * Lambert Conformal Projection, one or two standard parallels, spherical earth.
 * Projection plane is a cone whose vertex lies on the line of the earth's axis,
 * and intersects the earth at two parellels (par1, par2), or is tangent to the earth at
 * one parellel par1 = par2. The cone is flattened by splitting along the longitude = lon0+180.
 * <p/>
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 104
 */
@Immutable
public class LambertConformal extends AbstractProjection {
  private final double earth_radius;
  private final double lat0, lon0; // lat/lon in radians
  private final double par1, par2; // standard parallel 1 and 2 degrees
  private final double falseEasting, falseNorthing;

  // values passed in through the constructor
  // need for constructCopy
  private final double _lat0, _lon0;

  private final double n, F, rho; // constants from Snyder's equations
  private final double earthRadiusTimesF;// Earth's radius time F
  private final double lon0Degrees; // lon naught ??

  @Override
  public Projection constructCopy() {
    return new LambertConformal(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(), getFalseEasting(),
        getFalseNorthing(), earth_radius);
  }

  /**
   * Constructor with default parameters
   */
  public LambertConformal() {
    this(40.0, -105.0, 20.0, 60.0, 0.0, 0.0, EARTH_RADIUS);
  }

  /**
   * Construct a LambertConformal Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @throws IllegalArgumentException if lat0 > +/-90 or if par1, par2 >= +/-90 deg
   */
  public LambertConformal(double lat0, double lon0, double par1, double par2) {
    this(lat0, lon0, par1, par2, 0.0, 0.0, EARTH_RADIUS);
  }

  /**
   * Construct a LambertConformal Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @param false_easting natural_x_coordinate + false_easting = x coordinate in km
   * @param false_northing natural_y_coordinate + false_northing = y coordinate in km
   * @throws IllegalArgumentException if lat0 > +/-90 or if par1, par2 >= +/-90 deg
   */
  public LambertConformal(double lat0, double lon0, double par1, double par2, double false_easting,
      double false_northing) {
    this(lat0, lon0, par1, par2, false_easting, false_northing, EARTH_RADIUS);

  }

  /**
   * Construct a LambertConformal Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @param false_easting natural_x_coordinate + false_easting = x coordinate in km
   * @param false_northing natural_y_coordinate + false_northing = y coordinate in km
   * @param earth_radius radius of the earth in km
   * @throws IllegalArgumentException if lat0 > +/-90 or if par1, par2 >= +/-90 deg
   */
  public LambertConformal(double lat0, double lon0, double par1, double par2, double false_easting,
      double false_northing, double earth_radius) {

    super("LambertConformal", false);

    this._lat0 = lat0;
    this._lon0 = lon0;

    this.lon0Degrees = lon0;
    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);

    this.par1 = par1;
    this.par2 = par2;

    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;
    this.earth_radius = earth_radius;

    if (Math.abs(this.lat0) > PI_OVER_2) {
      throw new IllegalArgumentException("LambertConformal lat0 outside range (-90,90)");
    }
    if (Math.abs(par1) >= 90.0) {
      throw new IllegalArgumentException("LambertConformal abs(par1) >= 90");
    }
    if (Math.abs(par2) >= 90.0) {
      throw new IllegalArgumentException("LambertConformal abs(par2) >= 90");
    }

    if (Math.abs(par1 - 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par1 = 90");
    }
    if (Math.abs(par1 + 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par1 = -90");
    }
    if (Math.abs(par2 - 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par2 = 90");
    }
    if (Math.abs(par2 + 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par2 = -90");
    }

    double par1r = Math.toRadians(this.par1);
    double par2r = Math.toRadians(this.par2);

    double t1 = Math.tan(Math.PI / 4 + par1r / 2);
    double t2 = Math.tan(Math.PI / 4 + par2r / 2);

    if (Math.abs(par2 - par1) < TOLERANCE) { // single parallel
      this.n = Math.sin(par1r);
    } else {
      this.n = Math.log(Math.cos(par1r) / Math.cos(par2r)) / Math.log(t2 / t1);
    }

    double t1n = Math.pow(t1, n);
    this.F = Math.cos(par1r) * t1n / n;
    this.earthRadiusTimesF = earth_radius * F;

    double t0n = Math.pow(Math.tan(Math.PI / 4 + this.lat0 / 2), n);
    this.rho = earthRadiusTimesF / t0n;

    addParameter(CF.GRID_MAPPING_NAME, CF.LAMBERT_CONFORMAL_CONIC);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);
    if (par2 == par1) {
      addParameter(CF.STANDARD_PARALLEL, par1);
    } else {
      double[] data = new double[2];
      data[0] = par1;
      data[1] = par2;
      addParameter(new Parameter(CF.STANDARD_PARALLEL, data));
    }
    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }
    addParameter(CF.EARTH_RADIUS, earth_radius * 1000);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    LambertConformal that = (LambertConformal) o;

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
    temp = earth_radius != +0.0d ? Double.doubleToLongBits(earth_radius) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = lat0 != +0.0d ? Double.doubleToLongBits(lat0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
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
    return result;
  }

  // bean properties

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
   * Get the origin longitude in degrees
   *
   * @return the origin longitude in degrees.
   */
  public double getOriginLon() {
    return _lon0;
  }

  /**
   * Get the origin latitude in degrees
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
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Lambert conformal conic";
  }

  /**
   * Create a String of the parameters.
   *
   * @return a String of the parameters
   */
  public String paramsToString() {
    return toString();
  }

  @Override
  public String toString() {
    return "LambertConformal{" + "earth_radius=" + earth_radius + ", lat0=" + _lat0 + ", lon0=" + _lon0 + ", par1="
        + par1 + ", par2=" + par2 + ", falseEasting=" + falseEasting + ", falseNorthing=" + falseNorthing + '}';
  }

  /**
   * Create a WKS string
   *
   * @return WKS string
   */
  public String toWKS() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("PROJCS[\"").append(getName()).append("\",");
    if (true) {
      sbuff.append("GEOGCS[\"Normal Sphere (r=6371007)\",");
      sbuff.append("DATUM[\"unknown\",");
      sbuff.append("SPHEROID[\"sphere\",6371007,0]],");
    } else {
      sbuff.append("GEOGCS[\"WGS 84\",");
      sbuff.append("DATUM[\"WGS_1984\",");
      sbuff.append("SPHEROID[\"WGS 84\",6378137,298.257223563],");
      sbuff.append("TOWGS84[0,0,0,0,0,0,0]],");
    }
    sbuff.append("PRIMEM[\"Greenwich\",0],");
    sbuff.append("UNIT[\"degree\",0.0174532925199433]],");
    sbuff.append("PROJECTION[\"Lambert_Conformal_Conic_1SP\"],");
    sbuff.append("PARAMETER[\"latitude_of_origin\",").append(getOriginLat()).append("],"); // LOOK assumes getOriginLat
                                                                                           // = getParellel
    sbuff.append("PARAMETER[\"central_meridian\",").append(getOriginLon()).append("],");
    sbuff.append("PARAMETER[\"scale_factor\",1],");
    sbuff.append("PARAMETER[\"false_easting\",").append(falseEasting).append("],");
    sbuff.append("PARAMETER[\"false_northing\",").append(falseNorthing).append("],");

    return sbuff.toString();
  }

  /**
   * Get the scale at the given lat.
   *
   * @param lat lat to use
   * @return scale factor at this latitude
   */
  public double getScale(double lat) {
    lat = Math.toRadians(lat);
    double t = Math.tan(Math.PI / 4 + lat / 2);
    double tn = Math.pow(t, n);
    double r1 = n * F;
    double r2 = Math.cos(lat) * tn;
    return r1 / r2;
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
    // opposite signed X values, larger then 20,000 km
    return (pt1.getX() * pt2.getX() < 0) && (Math.abs(pt1.getX() - pt2.getX()) > 20000.0);
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();

    fromLat = Math.toRadians(fromLat);
    double dlon = LatLonPoints.lonNormal(fromLon - lon0Degrees);
    double theta = n * Math.toRadians(dlon);
    double tn = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
    double r = earthRadiusTimesF / tn;
    toX = r * Math.sin(theta);
    toY = rho - r * Math.cos(theta);

    return ProjectionPoint.create(toX + falseEasting, toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;
    double rhop = rho;

    if (n < 0) {
      rhop *= -1.0;
      fromX *= -1.0;
      fromY *= -1.0;
    }

    double yd = (rhop - fromY);
    double theta = Math.atan2(fromX, yd);
    double r = Math.sqrt(fromX * fromX + yd * yd);
    if (n < 0.0) {
      r *= -1.0;
    }

    toLon = (Math.toDegrees(theta / n + lon0));

    if (Math.abs(r) < TOLERANCE) {
      toLat = ((n < 0.0) ? -90.0 : 90.0);
    } else {
      double rn = Math.pow(earth_radius * F / r, 1 / n);
      toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
    }

    return LatLonPoint.create(toLat, toLon);
  }

}


