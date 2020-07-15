/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import ucar.unidata.geoloc.Projection;

/**
 * Used to notify listeners that there is a new Projection.
 * 
 * @author John Caron
 */
public class NewProjectionEvent extends java.util.EventObject {
  private Projection project;

  public NewProjectionEvent(Object source, Projection proj) {
    super(source);
    this.project = proj;
  }

  public Projection getProjection() {
    return project;
  }

}
