/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.constants.FeatureType;
import javax.annotation.Nonnull;

/**
 * Abstract superclass for multiply nested NestedPointFeatureCollection
 * Subclass must supply getNestedPointFeatureCollectionIterator().
 *
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class PointFeatureCCCImpl extends DsgCollectionImpl implements PointFeatureCCC {
  protected FeatureType collectionFeatureType;

  protected PointFeatureCCCImpl(String name, CalendarDateUnit timeUnit, String altUnits,
      FeatureType collectionFeatureType) {
    super(name, timeUnit, altUnits);
    this.collectionFeatureType = collectionFeatureType;
  }

  // All features in this collection have this feature type
  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return collectionFeatureType;
  }

}
