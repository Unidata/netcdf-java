/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;

/** Create a LambertAzimuthal Projection from the information in the Coordinate Transform Variable. */
public class LambertAzimuthal extends AbstractProjectionCT implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.LAMBERT_AZIMUTHAL_EQUAL_AREA;
  }

  public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    readStandardParams(ctv, geoCoordinateUnits);
    ucar.unidata.geoloc.projection.LambertAzimuthalEqualArea proj =
        new ucar.unidata.geoloc.projection.LambertAzimuthalEqualArea(lat0, lon0, false_easting, false_northing,
            earth_radius);
    return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
  }
}

