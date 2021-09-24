/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.geoloc;

import ucar.unidata.geoloc.ProjectionPoint;
import java.awt.geom.Point2D;

/**
 * User wants to pick an object at 2D location.
 * 
 * @author John Caron
 */
public class PickEvent extends java.util.EventObject {
  ProjectionPoint where;

  public PickEvent(Object source, ProjectionPoint location) {
    super(source);
    this.where = location;
  }

  public PickEvent(Object source, Point2D location) {
    super(source);
    this.where = ProjectionPoint.create(location.getX(), location.getY());
  }

  public Point2D getLocationPoint() {
    return new Point2D.Double(where.getX(), where.getY());
  }

  public ProjectionPoint getLocation() {
    return where;
  }

}
