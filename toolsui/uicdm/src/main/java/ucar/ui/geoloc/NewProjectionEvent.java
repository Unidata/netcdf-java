/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.geoloc;

import ucar.unidata.geoloc.Projection;

/**
 * Used to notify listeners that there is a new Projection.
 * 
 * @author John Caron
 */
public class NewProjectionEvent extends java.util.EventObject {
  private final Projection project;

  public NewProjectionEvent(Object source, Projection proj) {
    super(source);
    this.project = proj;
  }

  public Projection getProjection() {
    return project;
  }

}
