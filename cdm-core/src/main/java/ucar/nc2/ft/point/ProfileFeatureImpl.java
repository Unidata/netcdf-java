/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import javax.annotation.Nonnull;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Abstract superclass for implementations of ProfileFeature.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class ProfileFeatureImpl extends PointCollectionImpl implements ProfileFeature {
  private final LatLonPoint latlonPoint;
  protected double time;

  public ProfileFeatureImpl(String name, CalendarDateUnit timeUnit, String altUnits, double lat, double lon,
      double time, int nfeatures) {
    super(name, timeUnit, altUnits);
    this.latlonPoint = LatLonPoint.create(lat, lon);
    this.time = time;
    if (nfeatures >= 0) {
      getInfo(); // create the object
      info.nfeatures = nfeatures;
    }
  }

  @Override
  @Nonnull
  public LatLonPoint getLatLon() {
    return latlonPoint;
  }

  public Object getId() {
    return getName();
  }

  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.PROFILE;
  }

}
