/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;

/** Create a Orthographic Projection from the information in the Coordinate Transform Variable. */
public class Orthographic extends AbstractProjectionCT implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.ORTHOGRAPHIC;
  }

  public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);

    ucar.unidata.geoloc.projection.Orthographic proj = new ucar.unidata.geoloc.projection.Orthographic(lat0, lon0);
    return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
  }
}
