/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Stereographic projection, spherical earth.
 * Projection plane is a plane tangent to the earth at latt, lont.
 * see John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 153
 */
@Immutable
public class Stereographic extends AbstractProjection {
  /**
   * Construct a Stereographic Projection using latitude of true scale and calculating scale factor.
   * <p>
   * Since the scale factor at lat = k = 2*k0/(1+sin(lat)) [Snyder,Working Manual p157]
   * then to make scale = 1 at lat, set k0 = (1+sin(lat))/2
   *
   * @param latt tangent point of projection, also origin of projection coord system
   * @param lont tangent point of projection, also origin of projection coord system
   * @param latTrue latitude of true scale in degrees north; latitude where scale factor = 1.0
   * @return Stereographic projection
   */
  public static Stereographic factory(double latt, double lont, double latTrue) {
    double scale = (1.0 + Math.sin(Math.toRadians(latTrue))) / 2.0;
    return new Stereographic(latt, lont, scale);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  private final double falseEasting, falseNorthing;
  private final double scale, earthRadius;
  private final double latt, lont;
  private final double sinlatt, coslatt;
  private double latts;
  private boolean isNorth;
  private boolean isPolar;

  @Override
  public Projection constructCopy() {
    return new Stereographic(getTangentLat(), getTangentLon(), getScale(), getFalseEasting(), getFalseNorthing(), getEarthRadius());
  }

  /**
   * Constructor with default parameters = North Polar
   */
  public Stereographic() {
    this(90.0, -105.0, 1.0);
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt tangent point of projection, also origin of projection coord system
   * @param lont tangent point of projection, also origin of projection coord system
   * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
   */
  public Stereographic(double latt, double lont, double scale) {
    this(latt, lont, scale, 0, 0, EARTH_RADIUS);
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt tangent point of projection, also origin of projection coord system
   * @param lont tangent point of projection, also origin of projection coord system
   * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
   */
  public Stereographic(double latt, double lont, double scale, double false_easting, double false_northing) {
    this(latt, lont, scale, false_easting, false_northing, EARTH_RADIUS);
  }

  /**
   * Construct a polar Stereographic Projection, from the "natural origin" and the tangent point,
   * calculating the scale factor.
   *
   * @param lat_ts_deg Latitude at natural origin (degrees_north)
   * @param latt_deg tangent point of projection (degrees_north)
   * @param lont_deg tangent point of projection, also origin of projection coord system ((degrees_east)
   * @param north true if north pole, false if south pole
   */
  public Stereographic(double lat_ts_deg, double latt_deg, double lont_deg, boolean north) {
    super("PolarStereographic", false);

    this.latts = Math.toRadians(lat_ts_deg);
    this.latt = Math.toRadians(latt_deg);
    this.lont = Math.toRadians(lont_deg);
    this.isPolar = true;
    this.isNorth = north;
    this.earthRadius = EARTH_RADIUS;
    this.falseEasting = 0;
    this.falseNorthing = 0;

    this.sinlatt = Math.sin(latt);
    this.coslatt = Math.cos(latt);

    double scaleFactor = (lat_ts_deg == 90 || lat_ts_deg == -90) ? 1.0 : getScaleFactor(latts, north);
    this.scale = scaleFactor * earthRadius;

    addParameter(CF.GRID_MAPPING_NAME, "polar_stereographic");
    addParameter("longitude_of_projection_origin", lont_deg);
    addParameter("latitude_of_projection_origin", latt_deg);
    addParameter("scale_factor_at_projection_origin", scaleFactor);
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt tangent point of projection, also origin of projection coord system
   * @param lont tangent point of projection, also origin of projection coord system
   * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
   * @param false_easting false easting in units of x coords
   * @param false_northing false northing in units of y coords
   * @param radius earth radius in km
   */
  public Stereographic(double latt, double lont, double scale, double false_easting, double false_northing, double radius) {
    super("Stereographic", false);

    this.latt = Math.toRadians(latt);
    this.lont = Math.toRadians(lont);
    this.earthRadius = radius;
    this.scale = scale * earthRadius;
    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;

    this.sinlatt = Math.sin(this.latt);
    this.coslatt = Math.cos(this.latt);

    addParameter(CF.GRID_MAPPING_NAME, CF.STEREOGRAPHIC);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lont);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, latt);
    addParameter(CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, scale);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }
  }

  /**
   * Calculate polar stereographic scale factor based on the natural latitude and
   * longitude of the original
   * Ref: OGP Surveying and Positioning Guidance Note number 7, part 2 April 2009
   * http://www.epsg.org
   * added by Qun He <qunhe@unc.edu>
   *
   * @param lat_ts Latitude at natural origin
   * @param north Is it north polar?
   * @return scale factor
   */
  private double getScaleFactor(double lat_ts, boolean north) {
    double e = 0.081819191;
    double tf, mf, k0;
    double root = (1 + e * Math.sin(lat_ts)) / (1 - e * Math.sin(lat_ts));
    double power = e / 2;

    if (north)
      tf = Math.tan(Math.PI / 4 - lat_ts / 2) * (Math.pow(root, power));
    else
      tf = Math.tan(Math.PI / 4 + lat_ts / 2) / (Math.pow(root, power));

    mf = Math.cos(lat_ts) / Math.sqrt(1 - e * e * Math.pow(Math.sin(lat_ts), 2));
    k0 = mf * Math.sqrt(Math.pow(1 + e, 1 + e) * Math.pow(1 - e, 1 - e)) / (2 * tf);

    return Double.isNaN(k0) ? 1.0 : k0;
  }

  // bean properties

  /**
   * Get the scale
   *
   * @return the scale
   */
  public double getScale() {
    return scale / earthRadius;
  }

  /**
   * Get the latitude at natural origin
   *
   * @return latitude at natural origin
   */
  public double getNaturalOriginLat() {
    return Math.toDegrees(latts);
  }

  /**
   * Get the tangent longitude in degrees
   *
   * @return the origin longitude.
   */
  public double getTangentLon() {
    return Math.toDegrees(lont);
  }

  /**
   * Get the tangent latitude in degrees
   *
   * @return the origin latitude.
   */
  public double getTangentLat() {
    return Math.toDegrees(latt);
  }

  public double getEarthRadius() {
    return earthRadius;
  }

  public boolean isNorth() {
    return isNorth;
  }

  public boolean isPolar() {
    return isPolar;
  }

  @Override
  public String paramsToString() {
    return toString();
  }

  //////////////////////////////////////////////

  @Override
  public String toString() {
    return "Stereographic{" + "falseEasting=" + falseEasting + ", falseNorthing=" + falseNorthing + ", scale=" + scale
        + ", earthRadius=" + earthRadius + ", latt=" + latt + ", lont=" + lont + '}';
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return false; // LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Stereographic that = (Stereographic) o;

    if (Double.compare(that.earthRadius, earthRadius) != 0)
      return false;
    if (Double.compare(that.falseEasting, falseEasting) != 0)
      return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0)
      return false;
    if (Double.compare(that.latt, latt) != 0)
      return false;
    if (Double.compare(that.lont, lont) != 0)
      return false;
    if (Double.compare(that.scale, scale) != 0)
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = scale != +0.0d ? Double.doubleToLongBits(scale) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = latt != +0.0d ? Double.doubleToLongBits(latt) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = lont != +0.0d ? Double.doubleToLongBits(lont) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
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

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();


    double lat = Math.toRadians(fromLat);
    double lon = Math.toRadians(fromLon);
    // keep away from the singular point
    if ((Math.abs(lat + latt) <= TOLERANCE)) {
      lat = -latt * (1.0 - TOLERANCE);
    }

    double sdlon = Math.sin(lon - lont);
    double cdlon = Math.cos(lon - lont);
    double sinlat = Math.sin(lat);
    double coslat = Math.cos(lat);

    double k = 2.0 * scale / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
    toX = k * coslat * sdlon;
    toY = k * (coslatt * sinlat - sinlatt * coslat * cdlon);

    return ProjectionPoint.create(toX + falseEasting, toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;
    double phi, lam;

    double rho = Math.sqrt(fromX * fromX + fromY * fromY);
    double c = 2.0 * Math.atan2(rho, 2.0 * scale);
    double sinc = Math.sin(c);
    double cosc = Math.cos(c);

    if (Math.abs(rho) < TOLERANCE) {
      phi = latt;
    } else {
      phi = Math.asin(cosc * sinlatt + fromY * sinc * coslatt / rho);
    }

    toLat = Math.toDegrees(phi);

    if ((Math.abs(fromX) < TOLERANCE) && (Math.abs(fromY) < TOLERANCE)) {
      lam = lont;
    } else if (Math.abs(coslatt) < TOLERANCE) {
      lam = lont + Math.atan2(fromX, ((latt > 0) ? -fromY : fromY));
    } else {
      lam = lont + Math.atan2(fromX * sinc, rho * coslatt * cosc - fromY * sinc * sinlatt);
    }

    toLon = Math.toDegrees(lam);
    return LatLonPoint.create(toLat, toLon);
  }

}
