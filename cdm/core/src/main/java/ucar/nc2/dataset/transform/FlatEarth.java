/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;

/** Create a "FlatEarth" Projection from the information in the Coordinate Transform Variable. */
public class FlatEarth extends AbstractProjectionCT implements HorizTransformBuilderIF {

  public String getTransformName() {
    return "flat_earth";
  }

  public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double rot = ctv.findAttributeDouble(ucar.unidata.geoloc.projection.FlatEarth.ROTATIONANGLE, 0.0);
    double earth_radius = TransformBuilders.getEarthRadiusInKm(ctv);

    ucar.unidata.geoloc.projection.FlatEarth proj =
        new ucar.unidata.geoloc.projection.FlatEarth(lat0, lon0, rot, earth_radius);
    return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
  }
}
