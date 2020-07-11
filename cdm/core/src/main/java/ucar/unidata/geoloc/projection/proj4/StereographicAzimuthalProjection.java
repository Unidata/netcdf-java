/*
 * Copyright 2006 Jerry Huxtable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 */
package ucar.unidata.geoloc.projection.proj4;

import java.util.Formatter;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.AbstractProjection;

/**
 * taken from the USGS PROJ package.
 *
 * @author Heiko.Klein@met.no
 */
@Immutable
public class StereographicAzimuthalProjection extends AbstractProjection {
  private static final int NORTH_POLE = 1;
  private static final int SOUTH_POLE = 2;
  private static final int EQUATOR = 3;
  private static final int OBLIQUE = 4;
  private static final double TOL = 1.e-8;

  // projection parameters
  private final double projectionLatitude, projectionLongitude; // origin in radian
  private final double n; // Math.sin(projectionLatitude)
  private final double scaleFactor, trueScaleLatitude; // scale or trueScale in radian
  private final double falseEasting, falseNorthing; // km

  // earth shape
  private final Earth earth;
  private final double e; // earth.getEccentricity
  private final double totalScale; // scale to convert cartesian coords in km

  private final double akm1;
  private final double sinphi0;
  private final double cosphi0;
  private final int mode;

  public StereographicAzimuthalProjection() { // polar stereographic with true longitude at 60 deg
    this(90.0, 0.0, 0.9330127018922193, 60., 0, 0, EarthEllipsoid.WGS84);
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt tangent point of projection, also origin of
   *        projection coord system, in degree
   * @param lont tangent point of projection, also origin of
   *        projection coord system, in degree
   * @param trueScaleLat latitude in degree where scale is scale
   * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
   */
  public StereographicAzimuthalProjection(double latt, double lont, double scale, double trueScaleLat,
      double false_easting, double false_northing, Earth earth) {
    super("StereographicAzimuthalProjection", false);

    projectionLatitude = Math.toRadians(latt);
    n = Math.abs(Math.sin(projectionLatitude));
    projectionLongitude = Math.toRadians(lont);
    trueScaleLatitude = Math.abs(Math.toRadians(trueScaleLat));
    scaleFactor = Math.abs(scale);
    falseEasting = false_easting;
    falseNorthing = false_northing;

    // earth figure
    this.earth = earth;
    this.e = earth.getEccentricity();
    // scale factor for cartesion coords in km. // issue if semimajor and semiminor axis defined in dataset?
    this.totalScale = earth.getMajor() * 0.001;

    double t = Math.abs(projectionLatitude);
    if (Math.abs(t - MapMath.HALFPI) < MapMath.EPS10)
      mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
    else
      mode = t > MapMath.EPS10 ? OBLIQUE : EQUATOR;

    double tsinp = 0.0;
    double tcosp = 0.0;
    double takm1;

    if (earth.isSpherical()) { // sphere
      switch (mode) {
        case OBLIQUE:
          tsinp = Math.sin(projectionLatitude);
          tcosp = Math.cos(projectionLatitude);
        case EQUATOR:
          takm1 = 2. * scaleFactor;
          break;
        case SOUTH_POLE:
        case NORTH_POLE:
          takm1 = Math.abs(trueScaleLatitude - MapMath.HALFPI) >= MapMath.EPS10
              ? Math.cos(trueScaleLatitude) / Math.tan(MapMath.QUARTERPI - .5 * trueScaleLatitude)
              : 2. * scaleFactor;
          break;
        default:
          throw new IllegalStateException();
      }
    } else { // ellipsoid
      switch (mode) {
        case NORTH_POLE:
        case SOUTH_POLE:
          if (Math.abs(trueScaleLatitude - MapMath.HALFPI) < MapMath.EPS10)
            takm1 = 2. * scaleFactor / Math.sqrt(Math.pow(1 + e, 1 + e) * Math.pow(1 - e, 1 - e));
          else {
            takm1 = Math.cos(trueScaleLatitude) / MapMath.tsfn(trueScaleLatitude, t = Math.sin(trueScaleLatitude), e);
            t *= e;
            takm1 /= Math.sqrt(1. - t * t);
          }
          break;
        case EQUATOR:
          takm1 = 2. * scaleFactor;
          break;
        case OBLIQUE:
          t = Math.sin(projectionLatitude);
          double X = 2. * Math.atan(ssfn(projectionLatitude, t, e)) - MapMath.HALFPI;
          t *= e;
          takm1 = 2. * scaleFactor * Math.cos(projectionLatitude) / Math.sqrt(1. - t * t);
          tsinp = Math.sin(X);
          tcosp = Math.cos(X);
          break;
        default:
          throw new IllegalStateException();
      }
    }
    this.akm1 = takm1;
    this.sinphi0 = tsinp;
    this.cosphi0 = tcosp;

    // parameters
    addParameter(CF.GRID_MAPPING_NAME, CF.STEREOGRAPHIC);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lont);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, latt);
    addParameter(CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, scale);
    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }

    // seems correct for case where dataset has semimajor axis information, but where is semiminor?
    addParameter(CF.SEMI_MAJOR_AXIS, earth.getMajor());
    // this gets us the semiminor axis from the semimajor (semimajor - flattening*semimajor)
    addParameter(CF.INVERSE_FLATTENING, 1.0 / earth.getFlattening());
  }

  private ProjectionPoint project(double lam, double phi) {
    double coslam = Math.cos(lam);
    double sinlam = Math.sin(lam);
    double sinphi = Math.sin(phi);
    double toX = 0, toY = 0;

    if (earth.isSpherical()) { // sphere
      double cosphi = Math.cos(phi);

      switch (mode) {
        case EQUATOR:
          double y = 1. + cosphi * coslam;
          if (y <= MapMath.EPS10)
            throw new RuntimeException("I");
          double x = (y = akm1 / y) * cosphi * sinlam;
          y *= sinphi;
          toX = x;
          toY = y;
          break;
        case OBLIQUE:
          y = 1. + sinphi0 * sinphi + cosphi0 * cosphi * coslam;
          if (y <= MapMath.EPS10)
            throw new RuntimeException("I");
          x = (y = akm1 / y) * cosphi * sinlam;
          y *= cosphi0 * sinphi - sinphi0 * cosphi * coslam;
          toX = x;
          toY = y;
          break;
        case NORTH_POLE:
          coslam = -coslam;
          phi = -phi;
          // coverity[missing_break]
        case SOUTH_POLE:
          if (Math.abs(phi - MapMath.HALFPI) < TOL)
            throw new RuntimeException("I");
          y = akm1 * Math.tan(MapMath.QUARTERPI + .5 * phi);
          x = sinlam * y;
          y *= coslam;
          toX = x;
          toY = y;
          break;
      }
    } else { // ellipsoid
      double sinX = 0, cosX = 0, X, A;

      if (mode == OBLIQUE || mode == EQUATOR) {
        sinX = Math.sin(X = 2. * Math.atan(ssfn(phi, sinphi, e)) - MapMath.HALFPI);
        cosX = Math.cos(X);
      }
      switch (mode) {
        case OBLIQUE:
          A = akm1 / (cosphi0 * (1. + sinphi0 * sinX + cosphi0 * cosX * coslam));
          double y = A * (cosphi0 * sinX - sinphi0 * cosX * coslam);
          double x = A * cosX;
          toX = x;
          toY = y;
          break;
        case EQUATOR:
          A = 2. * akm1 / (1. + cosX * coslam);
          y = A * sinX;
          x = A * cosX;
          toX = x;
          toY = y;
          break;
        case SOUTH_POLE:
          phi = -phi;
          coslam = -coslam;
          sinphi = -sinphi;
          // coverity[missing_break]
        case NORTH_POLE:
          x = akm1 * MapMath.tsfn(phi, sinphi, e);
          y = -x * coslam;
          toX = x;
          toY = y;
          break;
      }
      toX *= sinlam;
    }
    return ProjectionPoint.create(toX, toY);
  }

  private ProjectionPoint projectInverse(double x, double y) {
    double lpx = 0.;
    double lpy;
    double toX = 0, toY = 0;

    if (earth.isSpherical()) {
      double c, rh, sinc, cosc;

      sinc = Math.sin(c = 2. * Math.atan((rh = MapMath.distance(x, y)) / akm1));
      cosc = Math.cos(c);
      switch (mode) {
        case EQUATOR:
          if (Math.abs(rh) <= MapMath.EPS10)
            lpy = 0.;
          else
            lpy = Math.asin(y * sinc / rh);
          if (cosc != 0. || x != 0.)
            lpx = Math.atan2(x * sinc, cosc * rh);
          toX = lpx;
          toY = lpy;
          break;
        case OBLIQUE:
          if (Math.abs(rh) <= MapMath.EPS10)
            lpy = projectionLatitude;
          else
            lpy = Math.asin(cosc * sinphi0 + y * sinc * cosphi0 / rh);
          if ((c = cosc - sinphi0 * Math.sin(lpy)) != 0. || x != 0.)
            lpx = Math.atan2(x * sinc * cosphi0, c * rh);
          toX = lpx;
          toY = lpy;
          break;
        case NORTH_POLE:
          y = -y;
        case SOUTH_POLE:
          if (Math.abs(rh) <= MapMath.EPS10)
            lpy = projectionLatitude;
          else
            lpy = Math.asin(mode == SOUTH_POLE ? -cosc : cosc);
          lpx = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
          toX = lpx;
          toY = lpy;
          break;
      }
    } else {
      double cosphi, sinphi, tp, phi_l, rho, halfe, halfpi;

      rho = MapMath.distance(x, y);
      switch (mode) {
        case NORTH_POLE:
          y = -y;
        case SOUTH_POLE:
          phi_l = MapMath.HALFPI - 2. * Math.atan(tp = -rho / akm1);
          halfpi = -MapMath.HALFPI;
          halfe = -.5 * e;
          break;
        case OBLIQUE:
        case EQUATOR:
        default:
          cosphi = Math.cos(tp = 2. * Math.atan2(rho * cosphi0, akm1));
          sinphi = Math.sin(tp);
          phi_l = Math.asin(cosphi * sinphi0 + (y * sinphi * cosphi0 / rho));
          tp = Math.tan(.5 * (MapMath.HALFPI + phi_l));
          x *= sinphi;
          y = rho * cosphi0 * cosphi - y * sinphi0 * sinphi;
          halfpi = MapMath.HALFPI;
          halfe = .5 * e;
          break;
      }
      for (int i = 8; i-- != 0; phi_l = lpy) {
        sinphi = e * Math.sin(phi_l);
        lpy = 2. * Math.atan(tp * Math.pow((1. + sinphi) / (1. - sinphi), halfe)) - halfpi;
        if (Math.abs(phi_l - lpy) < MapMath.EPS10) {
          if (mode == SOUTH_POLE)
            lpy = -lpy;
          lpx = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
          toX = lpx;
          toY = lpy;
          return ProjectionPoint.create(toX, toY);
        }
      }
      throw new RuntimeException("Iteration didn't converge");
    }
    return ProjectionPoint.create(toX, toY);
  }

  private double ssfn(double phit, double sinphi, double eccen) {
    sinphi *= eccen;
    return Math.tan(.5 * (MapMath.HALFPI + phit)) * Math.pow((1. - sinphi) / (1. + sinphi), .5 * eccen);
  }

  @Override
  public String getProjectionTypeLabel() {
    return "Stereographic Azimuthal Ellipsoidal Earth";
  }

  @Override
  public Projection constructCopy() {
    return new StereographicAzimuthalProjection(Math.toDegrees(projectionLatitude), Math.toDegrees(projectionLongitude),
            scaleFactor, Math.toDegrees(trueScaleLatitude), falseEasting, falseNorthing, earth);
  }

  @Override
  public String paramsToString() {
    Formatter f = new Formatter();
    f.format("origin lat,lon=%f,%f scale,trueScaleLat=%f,%f earth=%s", Math.toDegrees(projectionLatitude),
        Math.toDegrees(projectionLongitude), scaleFactor, Math.toDegrees(trueScaleLatitude), earth);
    return f.toString();
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double fromLat = Math.toRadians(latLon.getLatitude());
    double theta = computeTheta(latLon.getLongitude());

    ProjectionPoint res = project(theta, fromLat);
    return ProjectionPoint.create(totalScale * res.getX() + falseEasting, totalScale * res.getY() + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double fromX = (world.getX() - falseEasting) / totalScale; // assumes cartesian coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;

    ProjectionPoint pt = projectInverse(fromX, fromY);
    double toLon = pt.getX();
    double toLat = pt.getY();
    if (toLon < -Math.PI)
      toLon = -Math.PI;
    else if (toLon > Math.PI)
      toLon = Math.PI;
    if (projectionLongitude != 0)
      toLon = MapMath.normalizeLongitude(toLon + projectionLongitude);

    return LatLonPoint.create(Math.toDegrees(toLat), Math.toDegrees(toLon));
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // TODO: not sure what this is, HK
    // just taken from ucar.unidata.geoloc.projection.Stereographic
    return false;
  }

  @Override
  public boolean equals(Object proj) {
    if (!(proj instanceof StereographicAzimuthalProjection)) {
      return false;
    }
    StereographicAzimuthalProjection oo = (StereographicAzimuthalProjection) proj;
    return ((this.projectionLatitude == oo.projectionLatitude) && (this.projectionLongitude == oo.projectionLongitude)
        && (this.scaleFactor == oo.scaleFactor) && (this.trueScaleLatitude == oo.trueScaleLatitude)
        && (this.falseEasting == oo.falseEasting) && (this.falseNorthing == oo.falseNorthing)
        && this.earth.equals(oo.earth));
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.projectionLatitude)
        ^ (Double.doubleToLongBits(this.projectionLatitude) >>> 32));
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.projectionLongitude)
        ^ (Double.doubleToLongBits(this.projectionLongitude) >>> 32));
    hash = 67 * hash
        + (int) (Double.doubleToLongBits(this.scaleFactor) ^ (Double.doubleToLongBits(this.scaleFactor) >>> 32));
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.trueScaleLatitude)
        ^ (Double.doubleToLongBits(this.trueScaleLatitude) >>> 32));
    hash = 67 * hash
        + (int) (Double.doubleToLongBits(this.falseEasting) ^ (Double.doubleToLongBits(this.falseEasting) >>> 32));
    hash = 67 * hash
        + (int) (Double.doubleToLongBits(this.falseNorthing) ^ (Double.doubleToLongBits(this.falseNorthing) >>> 32));
    hash = 67 * hash + (this.earth != null ? this.earth.hashCode() : 0);
    return hash;
  }

  private double computeTheta(double lon) {
    double dlon = LatLonPoints.lonNormal(lon - Math.toDegrees(projectionLongitude));
    return n * Math.toRadians(dlon);
  }

}
