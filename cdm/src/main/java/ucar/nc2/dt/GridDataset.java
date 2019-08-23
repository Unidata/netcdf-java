/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import ucar.nc2.ft.FeatureDataset;
import ucar.unidata.geoloc.ProjectionRect;
import java.util.*;

/**
 * A dataset containing Grid objects.
 * 
 * @author caron
 */

public interface GridDataset extends FeatureDataset {

  /**
   * get the list of GridDatatype objects contained in this dataset.
   * 
   * @return list of GridDatatype
   */
  List<GridDatatype> getGrids();

  /**
   * find the named GridDatatype.
   * 
   * @param name full unescaped name
   * @return the named GridDatatype, or null if not found
   */
  GridDatatype findGridDatatype(String name);

  GridDatatype findGridByShortName(String shortName);


  ProjectionRect getProjBoundingBox();

  /**
   * Return GridDatatype objects grouped by GridCoordSystem. All GridDatatype in a Gridset
   * have the same GridCoordSystem.
   * 
   * @return List of type GridDataset.Gridset
   */
  List<Gridset> getGridsets();

  /**
   * A set of GridDatatype objects with the same Coordinate System.
   */
  interface Gridset {

    /**
     * Get list of GridDatatype objects with same Coordinate System
     * 
     * @return list of GridDatatype
     */
    List<GridDatatype> getGrids();

    /**
     * all the GridDatatype in this Gridset use this GridCoordSystem
     * 
     * @return the common GridCoordSystem
     */
    ucar.nc2.dt.GridCoordSystem getGeoCoordSystem();
  }

}
