/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;
import ucar.unidata.geoloc.Earth;

/** Create a Polar Stereographic Projection from the information in the Coordinate Transform Variable. */
public class PolarStereographic extends AbstractProjectionCT implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.POLAR_STEREOGRAPHIC;
  }

  public ProjectionCT.Builder<?> makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon0 = ctv.findAttributeDouble(CF.STRAIGHT_VERTICAL_LONGITUDE_FROM_POLE, Double.NaN);
    if (Double.isNaN(lon0))
      lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    if (Double.isNaN(lon0))
      throw new IllegalArgumentException("No longitude parameter");

    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, 90.0);
    double latD = 60.0;

    double scale = ctv.findAttributeDouble(CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, Double.NaN);
    if (Double.isNaN(scale)) {
      double stdpar = ctv.findAttributeDouble(CF.STANDARD_PARALLEL, Double.NaN);
      if (!Double.isNaN(stdpar)) {
        // caclulate scale snyder (21-7)
        // k = 2 * k0/(1 +/- sin stdpar)
        // then to make scale = 1 at stdpar, k0 = (1 +/- sin(stdpar))/2
        // double sin = Math.sin( Math.toRadians( stdpar));
        // scale = (lat0 > 0) ? (1.0 + sin)/2 : (1.0 - sin)/2;

        double sin = Math.abs(Math.sin(Math.toRadians(Math.abs(stdpar))));
        scale = (1.0 + sin) / 2;
        latD = stdpar;

      } else {
        scale = 0.9330127018922193;
      }
    } else {
      // given the scale, calculate stdpar
      // k0 = (1 +/- sin(stdpar))/2
      // asin(2 * k0 - 1) = stdpar)
      double temp = 2 * scale - 1;
      latD = Math.toDegrees(Math.asin(temp));
    }
    double false_easting = ctv.findAttributeDouble(CF.FALSE_EASTING, 0.0);
    double false_northing = ctv.findAttributeDouble(CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = TransformBuilders.getFalseEastingScaleFactor(geoCoordinateUnits);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double earth_radius = TransformBuilders.getEarthRadiusInKm(ctv);
    double semi_major_axis = ctv.findAttributeDouble(CF.SEMI_MAJOR_AXIS, Double.NaN); // meters
    double semi_minor_axis = ctv.findAttributeDouble(CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = ctv.findAttributeDouble(CF.INVERSE_FLATTENING, 0.0);

    ucar.unidata.geoloc.Projection proj;

    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
      proj = new ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection(lat0, lon0, scale, latD,
          false_easting, false_northing, earth);
    } else {
      proj = new ucar.unidata.geoloc.projection.Stereographic(lat0, lon0, scale, false_easting, false_northing,
          earth_radius);
    }
    return ProjectionCT.builder().setName(ctv.getName()).setAuthority("FGDC").setProjection(proj);
  }
}
