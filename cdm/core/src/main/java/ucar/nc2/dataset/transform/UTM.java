/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;
import ucar.unidata.geoloc.projection.UtmProjection;

/** Create a UTM Projection from the information in the Coordinate Transform Variable. */
public class UTM extends AbstractProjectionCT implements HorizTransformBuilderIF {

  public String getTransformName() {
    return UtmProjection.GRID_MAPPING_NAME;
  }

  public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double zoned = ctv.findAttributeDouble(UtmProjection.UTM_ZONE1, Double.NaN);
    if (Double.isNaN(zoned))
      zoned = ctv.findAttributeDouble(UtmProjection.UTM_ZONE2, Double.NaN);
    if (Double.isNaN(zoned))
      throw new IllegalArgumentException("No zone was specified");

    int zone = (int) zoned;
    boolean isNorth = zone > 0;
    zone = Math.abs(zone);

    double axis = ctv.findAttributeDouble("semimajor_axis", 0.0);
    double f = ctv.findAttributeDouble("inverse_flattening", 0.0);

    // double a, double f, int zone, boolean isNorth
    UtmProjection proj = (axis != 0.0) ? new UtmProjection(axis, f, zone, isNorth) : new UtmProjection(zone, isNorth);
    return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
  }
}
