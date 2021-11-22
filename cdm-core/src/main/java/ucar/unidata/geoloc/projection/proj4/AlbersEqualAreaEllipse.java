/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection.proj4;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.AbstractProjection;
import java.util.Formatter;

/**
 * Adapted from com.jhlabs.map.proj.AlbersProjection
 *
 * @see "http://www.jhlabs.com/java/maps/proj/index.html"
 * @see "http://trac.osgeo.org/proj/"
 */
@Immutable
public class AlbersEqualAreaEllipse extends AbstractProjection {
  private static final double EPS10 = 1.e-10;
  private static final double TOL7 = 1.e-7;
  private static final int N_ITER = 15;
  private static final double EPSILON = 1.0e-7;
  private static final double TOL = 1.0e-10;

  // projection parameters
  private final double lat0deg, lon0deg; // projection origin, degrees
  private final double par1deg, par2deg; // standard parellels, degrees
  private final double falseEasting, falseNorthing; // km

  // earth shape
  private final Earth earth;
  private final double e; // earth.getEccentricitySquared
  private final double es; // earth.getEccentricitySquared
  private final double one_es; // 1-es
  private final double totalScale; // scale to convert cartesian coords in km

  private final double ec;
  private final double n;
  private final double c;
  private final double dd;
  private final double rho0;

  @Override
  public Projection constructCopy() {
    return new AlbersEqualAreaEllipse(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(),
        getFalseEasting(), getFalseNorthing(), getEarth());
  }

  /**
   * Constructor with default parameters
   */
  public AlbersEqualAreaEllipse() {
    this(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth(6378137.0, 0.0, 298.257222101));
  }

  /**
   * Construct a AlbersEqualAreaEllipse Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @param falseEasting false easting in km
   * @param falseNorthing false easting in km
   * @param earth shape of the earth
   * @throws IllegalArgumentException if Math.abs(par1 + par2) < 1.e-10
   */
  public AlbersEqualAreaEllipse(double lat0, double lon0, double par1, double par2, double falseEasting,
      double falseNorthing, Earth earth) {
    super("AlbersEqualAreaEllipse", false);

    this.lat0deg = lat0;
    this.lon0deg = lon0;
    this.par1deg = par1;
    this.par2deg = par2;

    // convert to radians
    double lat0rad = Math.toRadians(lat0);
    double phi1 = Math.toRadians(par1);
    double phi2 = Math.toRadians(par2);

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;

    this.earth = earth;
    this.e = earth.getEccentricity();
    this.es = earth.getEccentricitySquared();
    this.one_es = 1.0 - es;
    this.totalScale = earth.getMajor() * .001; // scale factor for cartesion coords in km.

    if (Math.abs(phi1 + phi2) < EPS10)
      throw new IllegalArgumentException("Math.abs(par1 + par2) < 1.e-10");

    double sinphi = Math.sin(phi1);
    double cosphi = Math.cos(phi1);
    boolean secant = Math.abs(phi1 - phi2) >= EPS10;

    if (!earth.isSpherical()) {
      double m1 = MapMath.msfn(sinphi, cosphi, es);
      double ml1 = MapMath.qsfn(sinphi, e, one_es);
      if (secant) { /* secant cone */
        sinphi = Math.sin(phi2);
        cosphi = Math.cos(phi2);
        double m2 = MapMath.msfn(sinphi, cosphi, es);
        double ml2 = MapMath.qsfn(sinphi, e, one_es);
        this.n = (m1 * m1 - m2 * m2) / (ml2 - ml1);
      } else {
        this.n = sinphi;
      }

      this.ec = 1. - .5 * one_es * Math.log((1. - e) / (1. + e)) / e;
      this.c = m1 * m1 + n * ml1;
      this.dd = 1. / n;
      this.rho0 = dd * Math.sqrt(c - n * MapMath.qsfn(Math.sin(lat0rad), e, one_es));

    } else { // sphere
      this.n = secant ? .5 * (sinphi + Math.sin(phi2)) : sinphi;
      this.c = cosphi * cosphi + 2 * n * sinphi;
      this.dd = 1. / n;
      this.rho0 = dd * Math.sqrt(c - 2 * n * Math.sin(lat0rad));
      this.ec = 0.0; // not used
    }

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

    addParameter(CF.SEMI_MAJOR_AXIS, earth.getMajor());
    addParameter(CF.INVERSE_FLATTENING, 1.0 / earth.getFlattening());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AlbersEqualAreaEllipse that = (AlbersEqualAreaEllipse) o;

    if (Double.compare(that.falseEasting, falseEasting) != 0)
      return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0)
      return false;
    if (Double.compare(that.lat0deg, lat0deg) != 0)
      return false;
    if (Double.compare(that.lon0deg, lon0deg) != 0)
      return false;
    if (Double.compare(that.par1deg, par1deg) != 0)
      return false;
    if (Double.compare(that.par2deg, par2deg) != 0)
      return false;
    return earth.equals(that.earth);
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(lat0deg);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon0deg);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(par1deg);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(par2deg);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(falseEasting);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(falseNorthing);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + earth.hashCode();
    return result;
  }

  // bean properties

  public Earth getEarth() {
    return earth;
  }

  /**
   * Get the second standard parallel
   *
   * @return the second standard parallel in degrees
   */
  public double getParallelTwo() {
    return par2deg;
  }

  /**
   * Get the first standard parallel
   *
   * @return the first standard parallel in degrees
   */
  public double getParallelOne() {
    return par1deg;
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude in degrees
   */
  public double getOriginLon() {
    return lon0deg;
  }


  /**
   * Get the origin latitude.
   *
   * @return the origin latitude in degrees
   */
  public double getOriginLat() {
    return lat0deg;
  }

  /**
   * Get the false easting, in km.
   *
   * @return the false easting in km
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Get the false northing, in km.
   *
   * @return the false northing in km
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
    return "Albers Equal Area Ellipsoidal Earth";
  }

  /**
   * Create a String of the parameters.
   *
   * @return a String of the parameters
   */
  public String paramsToString() {
    Formatter f = new Formatter();
    f.format("origin lat,lon=%f,%f parellels=%f,%f earth=%s", lat0deg, lon0deg, par1deg, par2deg, earth);
    return f.toString();
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
    return LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2);
  }

  private double computeTheta(double lon) {
    double dlon = LatLonPoints.lonNormal(lon - lon0deg);
    return n * Math.toRadians(dlon);
  }

  // also see Snyder p 101
  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double fromLat = Math.toRadians(latLon.getLatitude());
    double theta = computeTheta(latLon.getLongitude());

    double term = earth.isSpherical() ? 2 * n * Math.sin(fromLat) : n * MapMath.qsfn(Math.sin(fromLat), e, one_es);
    double rho = c - term;

    if (rho < 0.0)
      throw new RuntimeException("F");

    rho = dd * Math.sqrt(rho);

    double toX = rho * Math.sin(theta);
    double toY = rho0 - rho * Math.cos(theta);

    return ProjectionPoint.create(totalScale * toX + falseEasting, totalScale * toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = (world.getX() - falseEasting) / totalScale; // assumes cartesion coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;

    fromY = rho0 - fromY;
    double rho = MapMath.distance(fromX, fromY);

    if (rho == 0.0) {
      toLon = 0.0;
      toLat = n > 0.0 ? MapMath.HALFPI : -MapMath.HALFPI;

    } else {
      if (n < 0.0) {
        rho = -rho;
        fromX = -fromX;
        fromY = -fromY;
      }
      double lpphi = rho / dd;

      if (!earth.isSpherical()) {
        lpphi = (c - lpphi * lpphi) / n;
        if (Math.abs(ec - Math.abs(lpphi)) > TOL7) {
          if (Math.abs(lpphi) > 2.0)
            throw new IllegalArgumentException("AlbersEqualAreaEllipse x,y=" + world);

          lpphi = phi1_(lpphi, e, one_es);
          if (lpphi == Double.MAX_VALUE)
            throw new RuntimeException("I");
        } else {
          lpphi = (lpphi < 0.) ? -MapMath.HALFPI : MapMath.HALFPI;
        }

      } else { // spherical case
        lpphi = (c - lpphi * lpphi) / (2 * n);
        if (Math.abs(lpphi) <= 1.0) {
          lpphi = Math.asin(lpphi);
        } else {
          lpphi = (lpphi < 0.) ? -MapMath.HALFPI : MapMath.HALFPI;
        }
      }

      toLon = Math.atan2(fromX, fromY) / n;
      // coverity[swapped_arguments]
      toLat = lpphi;
    }

    return LatLonPoint.create(Math.toDegrees(toLat), Math.toDegrees(toLon) + lon0deg);
  }

  private static double phi1_(double qs, double Te, double Tone_es) {
    double phi, sinpi, cospi, con, com, dphi;

    phi = Math.asin(.5 * qs);
    if (Te < EPSILON)
      return (phi);

    int countIter = N_ITER;
    do {
      sinpi = Math.sin(phi);
      cospi = Math.cos(phi);
      con = Te * sinpi;
      com = 1. - con * con;
      dphi = .5 * com * com / cospi * (qs / Tone_es - sinpi / com + .5 / Te * Math.log((1. - con) / (1. + con)));
      phi += dphi;
    } while (Math.abs(dphi) > TOL && --countIter != 0);

    return (countIter != 0 ? phi : Double.MAX_VALUE);
  }
}
