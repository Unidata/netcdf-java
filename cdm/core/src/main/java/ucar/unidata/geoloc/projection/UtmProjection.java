/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Universal Transverse Mercator.
 * Ellipsoidal earth.
 * <p/>
 * Origin of coordinate system is reletive to the point where the
 * central meridian and the equator cross.
 * This point has x,y value = (500, 0) km for north hemisphere.
 * and (500, 10,0000) km for south hemisphere.
 * Increasing values always go north and east.
 * <p/>
 * The central meridian = (zone * 6 - 183) degrees, where zone in [1,60].
 */
@Immutable
public class UtmProjection extends AbstractProjection {
  public static final String GRID_MAPPING_NAME = "universal_transverse_mercator";
  public static final String UTM_ZONE1 = "utm_zone_number";
  public static final String UTM_ZONE2 = "UTM_zone";

  private final Utm_To_Gdc_Converter convert2latlon;
  private final Gdc_To_Utm_Converter convert2xy;

  private final double a;
  private final double f;
  private final int zone;
  private final boolean isNorth;

  @Override
  public Projection constructCopy() {
    return new UtmProjection(a, f, getZone(), isNorth());
  }

  /**
   * Constructor with default parameters
   */
  public UtmProjection() {
    this(5, true);
  }

  /**
   * Constructor with default WGS 84 ellipsoid.
   *
   * @param zone the UTM zone number (1-60)
   * @param isNorth true if the UTM coordinate is in the northern hemisphere
   */
  public UtmProjection(int zone, boolean isNorth) {
    super("UtmProjection", false);
    convert2latlon = new Utm_To_Gdc_Converter(zone, isNorth);
    convert2xy = new Gdc_To_Utm_Converter(zone, isNorth);

    this.a = convert2latlon.getA();
    this.f = 1.0 / convert2latlon.getF();
    this.zone = zone;
    this.isNorth = isNorth;

    addParameter(CF.GRID_MAPPING_NAME, GRID_MAPPING_NAME);
    addParameter(CF.SEMI_MAJOR_AXIS, convert2latlon.getA());
    addParameter(CF.INVERSE_FLATTENING, convert2latlon.getF());
    addParameter(UTM_ZONE1, zone);
  }

  /**
   * Construct a Universal Transverse Mercator Projection.
   *
   * @param a the semi-major axis (meters) for the ellipsoid
   * @param f the inverse flattening for the ellipsoid
   * @param zone the UTM zone number (1-60)
   * @param isNorth true if the UTM coordinate is in the northern hemisphere
   */
  public UtmProjection(double a, double f, int zone, boolean isNorth) {
    super("UtmProjection", false);
    this.a = a;
    this.f = f;
    this.zone = zone;
    this.isNorth = isNorth;

    convert2latlon = new Utm_To_Gdc_Converter(a, f, zone, isNorth);
    convert2xy = new Gdc_To_Utm_Converter(a, f, zone, isNorth);

    addParameter(CF.GRID_MAPPING_NAME, GRID_MAPPING_NAME);
    addParameter(CF.SEMI_MAJOR_AXIS, a);
    addParameter(CF.INVERSE_FLATTENING, f);
    addParameter(UTM_ZONE1, zone);
  }

  /**
   * Get the zone number = [1,60]
   *
   * @return zone number
   */
  public int getZone() {
    return convert2latlon.getZone();
  }

  /**
   * Get whether in North or South Hemisphere.
   *
   * @return true if north
   */
  public boolean isNorth() {
    return convert2latlon.isNorth();
  }

  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Universal transverse mercator";
  }

  /*
   * Getting the central meridian in degrees. depends on the zone
   * 
   * @return the central meridian in degrees.
   */
  public double getCentralMeridian() {
    return convert2xy.getCentralMeridian();
  }

  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return getZone() + " " + isNorth();
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UtmProjection that = (UtmProjection) o;

    if (Double.compare(that.a, a) != 0) {
      return false;
    }
    if (Double.compare(that.f, f) != 0) {
      return false;
    }
    if (zone != that.zone) {
      return false;
    }
    return isNorth == that.isNorth;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(a);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(f);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + zone;
    result = 31 * result + (isNorth ? 1 : 0);
    return result;
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();

    return convert2xy.latLonToProj(fromLat, fromLon);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    return convert2latlon.projToLatLon(world.getX(), world.getY());
  }

}


