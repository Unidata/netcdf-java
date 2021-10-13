/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.LatLonProjection;

/**
 * This grid mapping defines the canonical 2D geographical coordinate system based upon latitude and longitude
 * coordinates on a spherical Earth. It is included so that the figure of the Earth can be described.
 *
 * @author cwardgar
 * @since 2018-03-24
 * @see <a href=
 *      "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/cf-conventions.html#_latitude_longitude"
 *      >CF Conventions</a>
 */
public class LatLon extends AbstractProjectionCT implements ProjectionBuilder {
  @Override
  public String getTransformName() {
    return CF.LATITUDE_LONGITUDE;
  }

  @Override
  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    readStandardParams(ctv, geoCoordinateUnits);

    // create spherical Earth obj if not created by readStandardParams w radii, flattening
    if (earth == null) {
      if (earth_radius > 0) {
        // Earth radius obtained in readStandardParams is in km, but Earth object wants m
        earth = new Earth(earth_radius * 1000);
      } else {
        earth = new Earth();
      }
    }

    return new LatLonProjection(earth);
  }
}
