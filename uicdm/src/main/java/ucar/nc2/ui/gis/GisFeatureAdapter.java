/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis;


/**
 * This adapts a Gisfeature into a subclass of AbstractGisFeature.
 * Part of the ADT middleware pattern.
 *
 * @author John Caron
 */
public class GisFeatureAdapter extends AbstractGisFeature {
  private final GisFeature gisFeature; // adaptee

  public GisFeatureAdapter(GisFeature gisFeature) {
    this.gisFeature = gisFeature;
  }

  @Override
  public java.awt.geom.Rectangle2D getBounds2D() {
    return gisFeature.getBounds2D();
  }

  @Override
  public int getNumPoints() {
    return gisFeature.getNumPoints();
  }

  @Override

  public int getNumParts() {
    return gisFeature.getNumParts();
  }

  @Override
  public java.util.Iterator<GisPart> iterator() {
    return gisFeature.iterator();
  }

} // GisFeatureAdapter
