/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Projection;

/** MSGnavigation projection */
public class MSGnavigation extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return "MSGnavigation";
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {

    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double minor_axis = ctv.findAttributeDouble(CF.SEMI_MINOR_AXIS, Double.NaN);
    double major_axis = ctv.findAttributeDouble(CF.SEMI_MAJOR_AXIS, Double.NaN);
    double height =
        ctv.findAttributeDouble(ucar.unidata.geoloc.projection.sat.MSGnavigation.HEIGHT_FROM_EARTH_CENTER, Double.NaN);
    double scale_x = ctv.findAttributeDouble(ucar.unidata.geoloc.projection.sat.MSGnavigation.SCALE_X, Double.NaN);
    double scale_y = ctv.findAttributeDouble(ucar.unidata.geoloc.projection.sat.MSGnavigation.SCALE_Y, Double.NaN);

    return new ucar.unidata.geoloc.projection.sat.MSGnavigation(lat0, lon0, major_axis, minor_axis, height, scale_x,
        scale_y);
  }

}
