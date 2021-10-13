/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.unidata.geoloc.Projection;

/** Create a Rotated LatLon Projection from the information in the Coordinate Transform Variable. */
public class RotatedLatLon extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return ucar.unidata.geoloc.projection.RotatedLatLon.GRID_MAPPING_NAME;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon =
        ctv.findAttributeDouble(ucar.unidata.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_LONGITUDE, Double.NaN);
    double lat =
        ctv.findAttributeDouble(ucar.unidata.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_LATITUDE, Double.NaN);
    double angle =
        ctv.findAttributeDouble(ucar.unidata.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_ANGLE, Double.NaN);

    return new ucar.unidata.geoloc.projection.RotatedLatLon(lat, lon, angle);
  }

}


