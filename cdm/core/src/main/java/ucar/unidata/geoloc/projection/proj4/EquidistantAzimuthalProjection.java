/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection.proj4;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.AbstractProjection;

/**
 * AzimuthalEquidistant Projection.
 * Port from proj4.
 */
@Immutable
public class EquidistantAzimuthalProjection extends AbstractProjection {
  private static final int NORTH_POLE = 1;
  private static final int SOUTH_POLE = 2;
  private static final int EQUATOR = 3;
  private static final int OBLIQUE = 4;

  private static final double TOL = 1.e-8;

  private final double lat0, lon0; // degrees
  private final double projectionLatitude, projectionLongitude; // radians
  private final double falseEasting;
  private final double falseNorthing;
  private final Earth earth;
  private final double e, es, one_es;
  private final double totalScale;

  private final int mode;
  private final double sinphi0, cosphi0;

  // depends on the mode if these are defined
  private final double[] en;
  private final double N1;
  private final double Mp;
  private final double He;
  private final double G;

  public EquidistantAzimuthalProjection() {
    this(90, 0, 0, 0, new Earth());
  }

  public EquidistantAzimuthalProjection(double lat0, double lon0, double falseEasting, double falseNorthing,
      Earth earth) {
    super("EquidistantAzimuthalProjection", false);

    Objects.requireNonNull(earth, "Azimuthal equidistant constructor requires non-null Earth");

    this.lat0 = lat0;
    this.lon0 = lon0;

    this.projectionLatitude = Math.toRadians(lat0);
    this.projectionLongitude = Math.toRadians(lon0);

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;

    this.earth = earth;
    this.e = earth.getEccentricity();
    this.es = earth.getEccentricitySquared();
    this.one_es = 1 - es;
    this.totalScale = earth.getMajor() * .001; // scale factor for cartesion coords in km.

    addParameter(CF.GRID_MAPPING_NAME, CF.AZIMUTHAL_EQUIDISTANT);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.SEMI_MAJOR_AXIS, earth.getMajor());
    addParameter(CF.INVERSE_FLATTENING, 1.0 / earth.getFlattening());

    if (Math.abs(Math.abs(projectionLatitude) - MapMath.HALFPI) < MapMath.EPS10) {
      this.mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
      this.sinphi0 = projectionLatitude < 0. ? -1. : 1.;
      this.cosphi0 = 0.;

    } else if (Math.abs(projectionLatitude) < MapMath.EPS10) {
      this.mode = EQUATOR;
      this.sinphi0 = 0.;
      this.cosphi0 = 1.;

    } else {
      this.mode = OBLIQUE;
      this.sinphi0 = Math.sin(projectionLatitude);
      this.cosphi0 = Math.cos(projectionLatitude);
    }

    double[] enTemp = null;
    double N1Temp = Double.NaN;
    double MpTemp = Double.NaN;
    double HeTemp = Double.NaN;
    double GTemp = Double.NaN;

    if (!earth.isSpherical()) {
      enTemp = MapMath.enfn(es);
      switch (mode) {
        case NORTH_POLE:
          MpTemp = MapMath.mlfn(MapMath.HALFPI, 1., 0., enTemp);
          break;
        case SOUTH_POLE:
          MpTemp = MapMath.mlfn(-MapMath.HALFPI, -1., 0., enTemp);
          break;
        case EQUATOR:
        case OBLIQUE:
          N1Temp = 1. / Math.sqrt(1. - es * sinphi0 * sinphi0);
          HeTemp = e / Math.sqrt(one_es);
          GTemp = sinphi0 * HeTemp;
          HeTemp *= cosphi0;
          break;
      }
    }

    this.en = enTemp;
    this.N1 = N1Temp;
    this.Mp = MpTemp;
    this.He = HeTemp;
    this.G = GTemp;
  }

  @Override
  public Projection constructCopy() {
    return new EquidistantAzimuthalProjection(lat0, lon0, falseEasting, falseNorthing, earth);
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon) {
    double lam = Math.toRadians(latlon.getLongitude() - lon0);
    double phi = Math.toRadians(latlon.getLatitude());
    double toX = 0, toY = 0;

    if (earth.isSpherical()) {
      double coslam, cosphi, sinphi;

      sinphi = Math.sin(phi);
      cosphi = Math.cos(phi);
      coslam = Math.cos(lam);
      switch (mode) {
        case EQUATOR:
        case OBLIQUE:
          if (mode == EQUATOR)
            toY = cosphi * coslam;
          else
            toY = sinphi0 * sinphi + cosphi0 * cosphi * coslam;

          if (Math.abs(Math.abs(toY) - 1.) < TOL) {
            if (toY < 0.)
              throw new IllegalStateException();
            else {
              toX = 0;
              toY = 0;
            }

          } else {
            double y = Math.acos(toY);
            y /= Math.sin(y);
            double x = y * cosphi * Math.sin(lam);
            y *= (mode == EQUATOR) ? sinphi : cosphi0 * sinphi - sinphi0 * cosphi * coslam;
            toY = y;
            toX = x;
          }
          break;

        case NORTH_POLE:
          phi = -phi;
          coslam = -coslam;

        case SOUTH_POLE:
          if (Math.abs(phi - MapMath.HALFPI) < MapMath.EPS10)
            throw new IllegalStateException();
          double y = (MapMath.HALFPI + phi);
          double x = y * Math.sin(lam);
          y *= coslam;
          toY = y;
          toX = x;
          break;
      }

    } else {
      double coslam, cosphi, sinphi, rho, s, H, H2, c, Az, t, ct, st, cA, sA;

      coslam = Math.cos(lam);
      cosphi = Math.cos(phi);
      sinphi = Math.sin(phi);
      switch (mode) {
        case NORTH_POLE:
          coslam = -coslam;
          // coverity[missing_break]
        case SOUTH_POLE:
          double x = (rho = Math.abs(Mp - MapMath.mlfn(phi, sinphi, cosphi, en))) * Math.sin(lam);
          toY = rho * coslam;
          toX = x;
          break;
        case EQUATOR:
        case OBLIQUE:
          if (Math.abs(lam) < MapMath.EPS10 && Math.abs(phi - projectionLatitude) < MapMath.EPS10) {
            toY = 0;
            toX = 0;
            break;
          }
          t = Math.atan2(one_es * sinphi + es * N1 * sinphi0 * Math.sqrt(1. - es * sinphi * sinphi), cosphi);
          ct = Math.cos(t);
          st = Math.sin(t);
          Az = Math.atan2(Math.sin(lam) * ct, cosphi0 * st - sinphi0 * coslam * ct);
          cA = Math.cos(Az);
          sA = Math.sin(Az);
          s = MapMath.asin(Math.abs(sA) < TOL ? (cosphi0 * st - sinphi0 * coslam * ct) / cA : Math.sin(lam) * ct / sA);
          H = He * cA;
          H2 = H * H;
          c = N1 * s * (1. + s * s * (-H2 * (1. - H2) / 6. + s * (G * H * (1. - 2. * H2 * H2) / 8.
              + s * ((H2 * (4. - 7. * H2) - 3. * G * G * (1. - 7. * H2)) / 120. - s * G * H / 48.))));
          toY = c * cA;
          toX = c * sA;
          break;
      }
    }

    return ProjectionPoint.create(totalScale * toX + falseEasting, totalScale * toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt) {
    double x = (ppt.getX() - falseEasting) / totalScale; // assumes cartesion coords in km
    double y = (ppt.getY() - falseNorthing) / totalScale;
    double toLat, toLon;

    if (earth.isSpherical()) {
      double cosc, c_rh, sinc;

      if ((c_rh = MapMath.distance(x, y)) > Math.PI) {
        if (c_rh - MapMath.EPS10 > Math.PI)
          throw new IllegalStateException();
        c_rh = Math.PI;

      } else if (c_rh < MapMath.EPS10) {
        return LatLonPoint.create(lat0, 0.0);
      }
      if (mode == OBLIQUE || mode == EQUATOR) {
        sinc = Math.sin(c_rh);
        cosc = Math.cos(c_rh);
        if (mode == EQUATOR) {
          toLat = Math.toDegrees(MapMath.asin(y * sinc / c_rh));
          x *= sinc;
          y = cosc * c_rh;

        } else {
          toLat = Math.toDegrees(MapMath.asin(cosc * sinphi0 + y * sinc * cosphi0 / c_rh));
          y = (cosc - sinphi0 * MapMath.sind(toLat)) * c_rh;
          x *= sinc * cosphi0;
        }
        toLon = Math.toDegrees(y == 0. ? 0. : Math.atan2(x, y));

      } else if (mode == NORTH_POLE) {
        toLat = Math.toDegrees(MapMath.HALFPI - c_rh);
        toLon = Math.toDegrees(Math.atan2(x, -y));

      } else {
        toLat = Math.toDegrees(c_rh - MapMath.HALFPI);
        toLon = Math.toDegrees(Math.atan2(x, y));
      }

    } else {
      double c, Az, cosAz, A, B, D, E, F, psi, t;

      if ((c = MapMath.distance(x, y)) < MapMath.EPS10) {
        return LatLonPoint.create(lat0, 0.0);
      }

      if (mode == OBLIQUE || mode == EQUATOR) {
        cosAz = Math.cos(Az = Math.atan2(x, y));
        t = cosphi0 * cosAz;
        B = es * t / one_es;
        A = -B * t;
        B *= 3. * (1. - A) * sinphi0;
        D = c / N1;
        E = D * (1. - D * D * (A * (1. + A) / 6. + B * (1. + 3. * A) * D / 24.));
        F = 1. - E * E * (A / 2. + B * E / 6.);
        psi = MapMath.asin(sinphi0 * Math.cos(E) + t * Math.sin(E));
        toLon = Math.toDegrees(MapMath.asin(Math.sin(Az) * Math.sin(E) / Math.cos(psi)));
        if ((t = Math.abs(psi)) < MapMath.EPS10)
          toLat = 0.0;
        else if (Math.abs(t - MapMath.HALFPI) < 0.)
          toLat = Math.toDegrees(MapMath.HALFPI);
        else
          toLat = Math.toDegrees(Math.atan((1. - es * F * sinphi0 / Math.sin(psi)) * Math.tan(psi) / one_es));
      } else {
        toLat = Math.toDegrees(MapMath.inv_mlfn(mode == NORTH_POLE ? Mp - c : Mp + c, es, en));
        toLon = Math.toDegrees(Math.atan2(x, mode == NORTH_POLE ? -y : y));
      }
    }

    return LatLonPoint.create(toLat, toLon + lon0);
  }

  @Override
  public String paramsToString() {
    return null;
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

    EquidistantAzimuthalProjection that = (EquidistantAzimuthalProjection) o;

    if (Double.compare(that.falseEasting, falseEasting) != 0)
      return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0)
      return false;
    if (Double.compare(that.projectionLatitude, projectionLatitude) != 0)
      return false;
    if (Double.compare(that.projectionLongitude, projectionLongitude) != 0)
      return false;
    if (!Objects.equals(earth, that.earth))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = projectionLatitude != +0.0d ? Double.doubleToLongBits(projectionLatitude) : 0L;
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

