/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDateUnit;
import javax.annotation.Nonnull;
import java.util.List;

/**
 * Implementation of TrajectoryFeature
 * 
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class TrajectoryFeatureImpl extends PointCollectionImpl implements TrajectoryFeature {

  public TrajectoryFeatureImpl(String name, CalendarDateUnit timeUnit, String altUnits, int nfeatures) {
    super(name, timeUnit, altUnits);
    if (nfeatures >= 0) {
      getInfo(); // create the object
      info.nfeatures = nfeatures;
    }
  }

  public TrajectoryFeatureImpl(String name, List<CoordinateAxis> coords, int nfeatures) {
    super(name, coords);
    if (nfeatures >= 0) {
      getInfo(); // create the object
      info.nfeatures = nfeatures;
    }
  }

  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.TRAJECTORY;
  }

}
