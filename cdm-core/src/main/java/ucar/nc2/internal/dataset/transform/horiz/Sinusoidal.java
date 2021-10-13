/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Projection;

/** Create a Sinusoidal Projection from the information in the Coordinate Transform Variable. */
public class Sinusoidal extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.SINUSOIDAL;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double centralMeridian = ctv.findAttributeDouble(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    double false_easting = ctv.findAttributeDouble(CF.FALSE_EASTING, 0.0);
    double false_northing = ctv.findAttributeDouble(CF.FALSE_NORTHING, 0.0);
    double earth_radius = TransformBuilders.getEarthRadiusInKm(ctv);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = TransformBuilders.getFalseEastingScaleFactor(geoCoordinateUnits);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    return new ucar.unidata.geoloc.projection.Sinusoidal(centralMeridian, false_easting, false_northing, earth_radius);
  }
}
