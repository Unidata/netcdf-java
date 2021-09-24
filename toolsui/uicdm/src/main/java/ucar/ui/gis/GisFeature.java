/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.gis;


/**
 * An interface for GIS features, (analogous to ESRI Shapefile shapes).
 *
 * Created: Sat Feb 20 16:44:29 1999
 *
 * @author Russ Rew
 */
public interface GisFeature extends Iterable<GisPart> {

  /**
   * Get the bounding box for this feature.
   *
   * @return rectangle bounding this feature
   */
  java.awt.geom.Rectangle2D getBounds2D();

  /**
   * Get total number of points in all parts of this feature.
   *
   * @return total number of points in all parts of this feature.
   */
  int getNumPoints();

  /**
   * Get number of parts comprising this feature.
   *
   * @return number of parts comprising this feature.
   */
  int getNumParts();

} // GisFeature
