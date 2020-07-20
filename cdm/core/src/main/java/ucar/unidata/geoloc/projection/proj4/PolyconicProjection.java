/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation. Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.unidata.geoloc.projection.proj4;

import java.util.Formatter;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.AbstractProjection;
import ucar.unidata.geoloc.ProjectionPoint;

/**
 * Polyconic Projection.
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 *
 * Bernhard Jenny, 19 September 2010: fixed spherical inverse.
 * 
 * @author ghansham@sac.isro.gov.in 1/8/2012
 */
@Immutable
public class PolyconicProjection extends AbstractProjection {
  private static final double TOL = 1e-10;
  private static final double CONV = 1e-10;
  private static final int N_ITER = 10;
  private static final int I_ITER = 20;
  private static final double ITOL = 1.e-12;

  private final double ml0;
  private final double[] en;

  // New variables added
  private final boolean spherical = true;
  private final double projectionLatitude;
  private final double projectionLongitude;
  private final Earth ellipsoid;
  private final double es;
  private final double falseEasting;
  private final double falseNorthing;
  private final double totalScale;

  // values passed in through the constructor
  // need for constructCopy
  private final double _lat0, _lon0;

  public PolyconicProjection() {
    this(23.56, 76.54);
  }

  public PolyconicProjection(double lat0, double lon0) {
    this(lat0, lon0, new Earth());
  }

  public PolyconicProjection(double lat0, double lon0, Earth ellipsoid) {
    this(lat0, lon0, 0.0, 0.0, ellipsoid);
  }

  public PolyconicProjection(double lat0, double lon0, double falseEasting, double falseNorthing, Earth ellipsoid) {
    super("Polyconic", false);

    this._lat0 = lat0;
    this._lon0 = lon0;

    // Initialization
    this.projectionLatitude = Math.toRadians(lat0);
    this.projectionLongitude = Math.toRadians(lon0);
    this.ellipsoid = ellipsoid;

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;
    this.es = this.ellipsoid.getEccentricitySquared();
    this.totalScale = this.ellipsoid.getMajor() * .001;

    if (!spherical) {
      en = MapMath.enfn(es);
      ml0 = MapMath.mlfn(projectionLatitude, Math.sin(projectionLatitude), Math.cos(projectionLatitude), en);
    } else {
      en = null;
      ml0 = -projectionLatitude;
    }

    // Adding parameters
    addParameter(CF.GRID_MAPPING_NAME, name);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);
    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.SEMI_MAJOR_AXIS, ellipsoid.getMajor());
    addParameter(CF.INVERSE_FLATTENING, 1.0 / ellipsoid.getFlattening());
  }

  private ProjectionPoint project(double lplam, double lpphi) {
    if (spherical) {
      double cot, E;

      if (Math.abs(lpphi) <= TOL) {
        return ProjectionPoint.create(lplam, ml0);
      } else {
        cot = 1. / Math.tan(lpphi);
        double x = Math.sin(E = lplam * Math.sin(lpphi)) * cot;
        double y = lpphi - projectionLatitude + cot * (1. - Math.cos(E));
        return ProjectionPoint.create(x, y);
      }
    } else {

      /*
       * FORWARD(e_forward); ellipsoid
       * double ms, sp, cp;
       * 
       * if (fabs(lp.phi) <= TOL) {
       * xy.x = lp.lam; xy.y = -P->ml0; }
       * else {
       * sp = sin(lp.phi);
       * ms = fabs(cp = cos(lp.phi)) > TOL ? pj_msfn(sp, cp, P->es) / sp : 0.;
       * xy.x = ms * sin(lp.lam *= sp); // LOOK
       * xy.y = (pj_mlfn(lp.phi, sp, cp, P->en) - P->ml0) + ms * (1. - cos(lp.lam));
       * }
       * return (xy);
       * }
       */
      double ms, sp, cp;

      if (Math.abs(lpphi) <= TOL) {
        return ProjectionPoint.create(lplam, -ml0);
      } else {
        sp = Math.sin(lpphi);
        ms = Math.abs(cp = Math.cos(lpphi)) > TOL ? MapMath.msfn(sp, cp, es) / sp : 0.;
        lplam *= sp; // LOOK
        double x = ms * Math.sin(lplam);
        double y = (MapMath.mlfn(lpphi, sp, cp, en) - ml0) + ms * (1. - Math.cos(lplam));
        return ProjectionPoint.create(x, y);
      }
    }
  }

  private ProjectionPoint projectInverse(double xyx, double xyy) {
    double lpphi;
    double toX, toY;

    if (spherical) {
      double B, dphi, tp;
      int i;

      if (Math.abs(xyy = projectionLatitude + xyy) <= TOL) {
        toX = xyx;
        toY = 0;

      } else {
        lpphi = xyy;
        B = xyx * xyx + xyy * xyy;
        i = N_ITER;
        do {
          tp = Math.tan(lpphi);
          lpphi -=
              (dphi = (xyy * (lpphi * tp + 1.) - lpphi - .5 * (lpphi * lpphi + B) * tp) / ((lpphi - xyy) / tp - 1.));
        } while (Math.abs(dphi) > CONV && --i > 0);

        if (i == 0) { // LOOK check original
          toX = Double.NaN;
          toY = Double.NaN;
        }
        toX = Math.asin(xyx * Math.tan(lpphi)) / Math.sin(lpphi);
        toY = lpphi;
      }
    } else {
      xyy += ml0;
      if (Math.abs(xyy) <= TOL) {
        toX = xyx;
        toY = 0;

      } else {
        double r, c, sp, cp, s2ph, ml, mlb, mlp, dPhi;
        int i;

        r = xyy * xyy + xyx * xyx;
        for (lpphi = xyy, i = I_ITER; i > 0; --i) {
          sp = Math.sin(lpphi);
          s2ph = sp * (cp = Math.cos(lpphi));
          if (Math.abs(cp) < ITOL) {
            throw new RuntimeException("I");
          }
          c = sp * (mlp = Math.sqrt(1. - es * sp * sp)) / cp;
          ml = MapMath.mlfn(lpphi, sp, cp, en);
          mlb = ml * ml + r;
          mlp = (1.0 / es) / (mlp * mlp * mlp);
          lpphi += (dPhi = (ml + ml + c * mlb - 2. * xyy * (c * ml + 1.))
              / (es * s2ph * (mlb - 2. * xyy * ml) / c + 2. * (xyy - ml) * (c * mlp - 1. / s2ph) - mlp - mlp));
          if (Math.abs(dPhi) <= ITOL) {
            break;
          }
        }

        if (i == 0) { // LOOK check original
          toX = Double.NaN;
          toY = Double.NaN;
        }
        c = Math.sin(lpphi);
        toX = Math.asin(xyx * Math.tan(lpphi) * Math.sqrt(1. - es * c * c)) / Math.sin(lpphi);
        toY = lpphi;
      }
    }
    return ProjectionPoint.create(toX, toY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    PolyconicProjection that = (PolyconicProjection) o;

    if (Double.compare(that.falseEasting, falseEasting) != 0)
      return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0)
      return false;
    if (Double.compare(that.projectionLatitude, projectionLatitude) != 0)
      return false;
    if (Double.compare(that.projectionLongitude, projectionLongitude) != 0)
      return false;
    return ellipsoid.equals(that.ellipsoid);

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(projectionLatitude);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(projectionLongitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + ellipsoid.hashCode();
    temp = Double.doubleToLongBits(falseEasting);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(falseNorthing);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  // Beans properties
  public Earth getEarth() {
    return ellipsoid;
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude in degrees
   */
  public double getOriginLatitude() {
    return _lat0;
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude in degrees
   */
  public double getOriginLongitude() {
    return _lon0;
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
    return "Polyconic Projection";
  }

  /**
   * Create a String of the parameters.
   *
   * @return a String of the parameters
   */
  @Override
  public String paramsToString() {
    Formatter f = new Formatter();
    f.format("origin lat=%f, origin lon=%f earth=%s", _lat0, _lon0, ellipsoid);
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
  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    if (LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2)) {
      return true;
    }


    // opposite signed X values, larger then 20,000 kml... similar to LambertConformal Conic
    return (pt1.getX() * pt2.getX() < 0) && (Math.abs(pt1.getX() - pt2.getX()) > 20000.0);


  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon) {
    double fromLat = Math.toRadians(latlon.getLatitude());
    double theta = Math.toRadians(latlon.getLongitude());
    if (projectionLongitude != 0 && !Double.isNaN(theta)) {
      theta = MapMath.normalizeLongitude(theta - projectionLongitude);
    }

    ProjectionPoint out = project(theta, fromLat);
    return ProjectionPoint.create(totalScale * out.getX() + falseEasting, totalScale * out.getY() + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double fromX = (world.getX() - falseEasting) / totalScale; // assumes cartesian coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;
    double toLat, toLon;

    ProjectionPoint pp = projectInverse(fromX, fromY);
    toLon = pp.getX();
    toLat = pp.getY();
    if (pp.getX() < -Math.PI) {
      toLon = -Math.PI;
    } else if (pp.getX() > Math.PI) {
      toLon = Math.PI;
    }
    if (projectionLongitude != 0 && !Double.isNaN(pp.getX())) {
      toLon = MapMath.normalizeLongitude(toLon + projectionLongitude);
    }

    return LatLonPoint.create(Math.toDegrees(toLat), Math.toDegrees(toLon));
  }

  @Override
  public Projection constructCopy() {
    return new PolyconicProjection(getOriginLatitude(), getOriginLongitude(), getFalseEasting(), getFalseNorthing(),
        getEarth());
  }
}
