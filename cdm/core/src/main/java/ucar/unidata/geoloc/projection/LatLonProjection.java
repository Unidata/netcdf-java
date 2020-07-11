/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.Format;

/**
 * This is the "fake" identity projection where world coord = latlon coord.
 * Topologically its the same as a cylinder tangent to the earth at the equator.
 * The cylinder is cut at the "seam" = centerLon +- 180.
 * Longitude values are always kept in the range [centerLon +-180]
 */
@Immutable
public class LatLonProjection extends AbstractProjection {
  private final double centerLon;
  private final Earth earth;

  @Override
  public Projection constructCopy() {
    return new LatLonProjection(getName(), getEarth(), getCenterLon());
  }

  public LatLonProjection() {
    this("LatLonProjection", EarthEllipsoid.DEFAULT, 0.0);
  }

  public LatLonProjection(String name) {
    this(name, EarthEllipsoid.DEFAULT, 0.0);
  }

  public LatLonProjection(Earth earth) {
    this("LatLonProjection", earth, 0.0);
  }

  public LatLonProjection(String name, @Nullable Earth earth, double centerLon) {
    super(name, true);
    this.earth = earth == null ? EarthEllipsoid.DEFAULT : earth;
    this.centerLon = centerLon;

    addParameter(CF.GRID_MAPPING_NAME, CF.LATITUDE_LONGITUDE);
    if (earth.isSpherical())
      addParameter(CF.EARTH_RADIUS, earth.getEquatorRadius());
    else {
      addParameter(CF.SEMI_MAJOR_AXIS, earth.getEquatorRadius());
      addParameter(CF.SEMI_MINOR_AXIS, earth.getPoleRadius());
    }
  }

  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Lat/Lon";
  }

  /**
   * Get a String of the parameters
   *
   * @return a String of the parameters
   */
  public String paramsToString() {
    return "Center lon:" + Format.d(centerLon, 3);
  }


  /**
   * See if this projection equals the object in question
   *
   * @param o object in question
   * @return true if it is a LatLonProjection and covers the same area
   */

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    LatLonProjection that = (LatLonProjection) o;
    return Double.compare(that.centerLon, centerLon) == 0;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(centerLon);
    result = (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon) {
    return latLonToProj(latlon, centerLon);
  }

  private ProjectionPoint latLonToProj(LatLonPoint latlon, double centerLon) {
    return ProjectionPoint.create(LatLonPoints.lonNormal(latlon.getLongitude(), centerLon), latlon.getLatitude());
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    return LatLonPoint.create(world.getX(), world.getY());
  }

  /** Get the center of the Longitude range. It is normalized to +/- 180. */
  public double getCenterLon() {
    return centerLon;
  }

  /** Get the Earth used in this projection. */
  public Earth getEarth() {
    return earth;
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return Math.abs(pt1.getX() - pt2.getX()) > 270.0; // ?? LOOK: do I believe this
  }

  /**
   * Split a latlon rectangle to the equivalent ProjectionRect
   * using this LatLonProjection to split it at the seam if needed.
   *
   * @param latlonR the latlon rectangle to transform
   * @return 1 or 2 ProjectionRect. If it doesnt cross the seam,
   *         the second rectangle is null.
   */
  public ProjectionRect[] latLonToProjRect(LatLonRect latlonR) {

    double lat0 = latlonR.getLowerLeftPoint().getLatitude();
    double height = Math.abs(latlonR.getUpperRightPoint().getLatitude() - lat0);
    double width = latlonR.getWidth();
    double lon0 = LatLonPoints.lonNormal(latlonR.getLowerLeftPoint().getLongitude(), centerLon);
    double lon1 = LatLonPoints.lonNormal(latlonR.getUpperRightPoint().getLongitude(), centerLon);

    ProjectionRect[] rects = {new ProjectionRect(), new ProjectionRect()};
    if (lon0 < lon1) {
      rects[0].setRect(lon0, lat0, width, height);
      rects[1] = null;

    } else {
      double y = centerLon + 180 - lon0;
      rects[0].setRect(lon0, lat0, y, height);
      rects[1].setRect(lon1 - width + y, lat0, width - y, height);
    }

    return rects;
  }

  @Override
  public LatLonRect projToLatLonBB(ProjectionRect world) {
    double startLat = world.getMinY();
    double startLon = world.getMinX();

    double deltaLat = world.getHeight();
    double deltaLon = world.getWidth();

    LatLonPoint llpt = LatLonPoint.create(startLat, startLon);
    return new LatLonRect(llpt, deltaLat, deltaLon);
  }

  /**
   * Create a latlon rectangle and split it into the equivalent
   * ProjectionRect using this LatLonProjection. The latlon rect is
   * constructed from 2 lat/lon points. The lon values are considered
   * coords in the latlonProjection, and so do not have to be +/- 180.
   *
   * @param lat0 lat of point 1
   * @param lon0 lon of point 1
   * @param lat1 lat of point 1
   * @param lon1 lon of point 1
   * @return 1 or 2 ProjectionRect. If it doesnt cross the seam,
   *         the second rectangle is null.
   */
  public ProjectionRect[] latLonToProjRect(double lat0, double lon0, double lat1, double lon1) {

    double height = Math.abs(lat1 - lat0);
    lat0 = Math.min(lat1, lat0);
    double width = lon1 - lon0;
    if (width < 1.0e-8) {
      width = 360.0; // assume its the whole thing
    }
    lon0 = LatLonPoints.lonNormal(lon0, centerLon);
    lon1 = LatLonPoints.lonNormal(lon1, centerLon);

    ProjectionRect[] rects = {new ProjectionRect(), new ProjectionRect()};
    if (width >= 360.0) {
      rects[0].setRect(centerLon - 180.0, lat0, 360.0, height);
      rects[1] = null;
    } else if (lon0 < lon1) {
      rects[0].setRect(lon0, lat0, width, height);
      rects[1] = null;
    } else {
      double y = centerLon + 180 - lon0;
      rects[0].setRect(lon0, lat0, y, height);
      rects[1].setRect(lon1 - width + y, lat0, width - y, height);
    }
    return rects;
  }

  // Override so we can adjust centerLon to center of the rectangle.
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect latlonRect) {
    double centerLon = latlonRect.getCenterLon();

    LatLonPoint ll = latlonRect.getLowerLeftPoint();
    LatLonPoint ur = latlonRect.getUpperRightPoint();
    ProjectionPoint w1 = latLonToProj(ll, centerLon);
    ProjectionPoint w2 = latLonToProj(ur, centerLon);

    // make bounding box out of those two corners
    ProjectionRect world = new ProjectionRect(w1.getX(), w1.getY(), w2.getX(), w2.getY());

    LatLonPoint la = LatLonPoint.create(ur.getLatitude(), ll.getLongitude());
    LatLonPoint lb = LatLonPoint.create(ll.getLatitude(), ur.getLongitude());

    // now extend if needed to the other two corners
    world.add(latLonToProj(la, centerLon));
    world.add(latLonToProj(lb, centerLon));

    return world;
  }

}
