/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import ucar.unidata.geoloc.*;

/**
 * Adapts a Projection interface into a subclass of
 * ProjectionImpl, so we can assume a Projection is a ProjectionImpl
 * without loss of generality.
 *
 * @deprecated do not use
 */
@Deprecated
public class ProjectionAdapter extends ProjectionImpl {

  /**
   * projection to adapt
   */
  private Projection proj;

  /**
   * Create a ProjectionImpl from the projection
   *
   * @param proj projection
   * @return a ProjectionImpl representing the projection
   */
  public static ProjectionImpl factory(Projection proj) {
    if (proj instanceof ProjectionImpl) {
      return (ProjectionImpl) proj;
    }
    return new ProjectionAdapter(proj);
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result = new ProjectionAdapter(proj);
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Create a new ProjectionImpl from a Projection
   *
   * @param proj projection to use
   */
  public ProjectionAdapter(Projection proj) {
    super(proj.getName(), proj instanceof LatLonProjection);
    this.proj = proj;
  }

  /**
   * Get the class name
   *
   * @return the class name
   */
  public String getClassName() {
    return proj.getClassName();
  }


  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return proj.paramsToString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProjectionAdapter that = (ProjectionAdapter) o;
    return proj.equals(that.proj);
  }

  @Override
  public int hashCode() {
    return proj.hashCode();
  }

  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latlon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl result) {
    return proj.latLonToProj(latlon, result);
  }

  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    return proj.projToLatLon(world, result);
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return proj.crossSeam(pt1, pt2);
  }

  /**
   * Get a reasonable bounding box for this projection.
   *
   * @return reasonable bounding box
   */
  public ProjectionRect getDefaultMapArea() {
    return proj.getDefaultMapArea();
  }
}
