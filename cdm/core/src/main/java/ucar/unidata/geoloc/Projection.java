/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc;

import com.google.common.collect.ImmutableList;
import ucar.unidata.util.Parameter;

/** Projective geometry transformations from (lat,lon) to (x,y) on a projective cartesian surface. */
public interface Projection {

  /** The name of the implementing class. */
  String getClassName();

  /** The name of this projection. */
  String getName();

  /** Is this the lat/lon Projection? */
  boolean isLatLon();

  /** String representation of the projection parameters. */
  String paramsToString();

  /** Convert lat, lon to Projection point. */
  default ProjectionPoint latLonToProj(double lat, double lon) {
    return latLonToProj(LatLonPoint.create(lat, lon));
  }

  /** Convert a LatLonPoint to projection coordinates. */
  ProjectionPoint latLonToProj(LatLonPoint latlon);

  /** Convert projection x, y to LatLonPoint point. */
  default LatLonPoint projToLatLon(double x, double y) {
    return projToLatLon(ProjectionPoint.create(x, y));
  }

  /** Convert projection coordinates to a LatLonPoint. */
  LatLonPoint projToLatLon(ProjectionPoint ppt);

  /**
   * Does the line between these two points cross the projection "seam", which
   * is a discontinuity in the function latlon <-> projection plane
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam, or the line does not cross it.
   */
  boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2);

  /** Get projection parameters. */
  ImmutableList<Parameter> getProjectionParameters();

  /**
   * Convert a lat/lon bounding box to a world coordinate bounding box,
   * by finding the minimum enclosing box.
   * Handles lat/lon points that do not intersect the projection panel.
   *
   * @param latlonRect input lat,lon bounding box
   * @return minimum enclosing box in world coordinates, or null if no part of the LatLonRect intersects the projection
   *         plane
   */
  ProjectionRect latLonToProjBB(LatLonRect latlonRect);

  /**
   * Compute lat/lon bounding box from projection bounding box.
   *
   * @param bb projection bounding box
   * @return lat, lon bounding box.
   */
  LatLonRect projToLatLonBB(ProjectionRect bb);

}

