/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.geoloc;

import ucar.unidata.geoloc.ProjectionRect;

/**
 * Used to notify listeners that there is a new world bounding box.
 * 
 * @author John Caron
 */
public class NewMapAreaEvent extends java.util.EventObject {
  private final ProjectionRect mapArea;

  public NewMapAreaEvent(Object source, ProjectionRect mapArea) {
    super(source);
    this.mapArea = mapArea;
  }

  public ProjectionRect getMapArea() {
    return mapArea;
  }
}
