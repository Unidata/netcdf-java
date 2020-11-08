/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.proj4.CylindricalEqualAreaProjection;

/** Lambert Cylindrical Equal Area Projection */
public class LambertCylindricalEqualArea extends AbstractProjectionCT implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.LAMBERT_CYLINDRICAL_EQUAL_AREA;
  }

  public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double par = ctv.findAttributeDouble(CF.STANDARD_PARALLEL, Double.NaN);

    readStandardParams(ctv, geoCoordinateUnits);

    // create spherical Earth obj if not created by readStandardParams w radii, flattening
    if (earth == null) {
      if (earth_radius > 0.) {
        // Earth radius obtained in readStandardParams is in km, but Earth object wants m
        earth = new Earth(earth_radius * 1000.);
      } else {
        earth = new Earth();
      }
    }

    Projection proj = new CylindricalEqualAreaProjection(lon0, par, false_easting, false_northing, earth);

    return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
  }
}
