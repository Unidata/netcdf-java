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
 *
 * Changes by Bernhard Jenny, May 2007: added missing toString() and
 * isEqualArea(); this class now derives from CylindricalProjection instead of
 * Projection; removed isRectilinear, which is defined in the new superclass
 * CylindricalProjection; and removed trueScaleLatitude that did hide a field
 * of the Projection class of the same name.
 */

package ucar.unidata.geoloc.projection.proj4;

import com.google.common.base.Preconditions;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.AbstractProjection;

/**
 * CylindricalEqualArea Projection.
 * Port from proj4.
 */
@Immutable
public class CylindricalEqualAreaProjection extends AbstractProjection {
  private final double trueScaleLatitude, lon0; // degrees
  private final double scaleFactor;
  private final double projectionLongitude; // radians
  private final double falseEasting;
  private final double falseNorthing;
  private final Earth earth;
  private final double e;
  private final double one_es;
  private final double totalScale;

  private final double qp;
  private final double[] apa;

  // default WSG ellipsoid
  public CylindricalEqualAreaProjection() {
    this(0.0, 1.0, 0.0, 0.0, EarthEllipsoid.WGS84);
  }

  public CylindricalEqualAreaProjection(double lon0, double trueScaleLatitude, double falseEasting,
      double falseNorthing, Earth earth) {
    super("CylindricalEqualAreaProjection", false);

    Preconditions.checkNotNull(earth, "CylindricalEqualAreaProjection constructor requires non-null Earth");

    this.lon0 = lon0;

    this.projectionLongitude = Math.toRadians(lon0);
    this.trueScaleLatitude = trueScaleLatitude;

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;

    this.earth = earth;
    this.e = earth.getEccentricity();
    double es = earth.getEccentricitySquared();
    this.one_es = 1 - es;
    this.totalScale = earth.getMajor() * .001; // scale factor for cartesion coords in km.

    double t = Math.toRadians(trueScaleLatitude);
    double cost = Math.cos(t);

    if (!earth.isSpherical()) {
      t = Math.sin(t);
      scaleFactor = cost / Math.sqrt(1. - es * t * t);
      apa = MapMath.authset(es);
      qp = MapMath.qsfn(1., e, one_es);
    } else {
      scaleFactor = cost;
      apa = null; // not used
      qp = Double.NaN;
    }

    addParameter(CF.GRID_MAPPING_NAME, CF.ALBERS_CONICAL_EQUAL_AREA);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);
    addParameter(CF.STANDARD_PARALLEL, trueScaleLatitude);

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.SEMI_MAJOR_AXIS, earth.getMajor());
    addParameter(CF.INVERSE_FLATTENING, 1.0 / earth.getFlattening());
  }

  @Override
  public Projection constructCopy() {
    return new CylindricalEqualAreaProjection(lon0, trueScaleLatitude, falseEasting, falseNorthing, earth);
  }

  @Override
  public String paramsToString() {
    return null;
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon) {
    double lam = Math.toRadians(latlon.getLongitude() - lon0);
    double phi = Math.toRadians(latlon.getLatitude());
    double toX, toY;
    if (earth.isSpherical()) {
      toX = scaleFactor * lam;
      toY = Math.sin(phi) / scaleFactor;
    } else {
      toX = scaleFactor * lam;
      toY = .5 * MapMath.qsfn(Math.sin(phi), e, one_es) / scaleFactor;
    }

    return ProjectionPoint.create(totalScale * toX + falseEasting, totalScale * toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt) {
    double x = (ppt.getX() - falseEasting) / totalScale; // assumes cartesion coords in km
    double y = (ppt.getY() - falseNorthing) / totalScale;
    double toLat, toLon;

    if (earth.isSpherical()) {
      y *= scaleFactor;
      double t = Math.abs(y);

      if (t - MapMath.EPS10 <= 1.) {
        if (t >= 1.) {
          toLat = Math.toDegrees(y < 0. ? -MapMath.HALFPI : MapMath.HALFPI);
        } else {
          toLat = Math.toDegrees(Math.asin(y));
        }
        toLon = Math.toDegrees(x / scaleFactor);
      } else {
        throw new IllegalStateException();
      }
    } else {
      toLat = Math.toDegrees(MapMath.authlat(Math.asin(2. * y * scaleFactor / qp), apa));
      toLon = lon0 + Math.toDegrees(x / scaleFactor);
    }
    return LatLonPoint.create(toLat, toLon);
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    CylindricalEqualAreaProjection that = (CylindricalEqualAreaProjection) o;

    if (Double.compare(that.falseEasting, falseEasting) != 0)
      return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0)
      return false;
    if (Double.compare(that.projectionLongitude, projectionLongitude) != 0)
      return false;
    if (Double.compare(that.scaleFactor, scaleFactor) != 0)
      return false;
    if (!Objects.equals(earth, that.earth))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = scaleFactor != +0.0d ? Double.doubleToLongBits(scaleFactor) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = projectionLongitude != +0.0d ? Double.doubleToLongBits(projectionLongitude) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + (earth != null ? earth.hashCode() : 0);
    return result;
  }
}

