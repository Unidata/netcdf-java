/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.Projection;

/** Create a Transverse Mercator Projection from the information in the Coordinate Transform Variable. */
public class TransverseMercator extends AbstractProjectionCT implements ProjectionBuilder {

  public String getTransformName() {
    return CF.TRANSVERSE_MERCATOR;
  }

  public Projection makeProjection(AttributeContainer ctv, String geoCoordinateUnits) {

    double scale = ctv.findAttributeDouble(CF.SCALE_FACTOR_AT_CENTRAL_MERIDIAN, Double.NaN);
    if (Double.isNaN(scale))
      scale = ctv.findAttributeDouble(CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, Double.NaN);
    double lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    if (Double.isNaN(lon0))
      lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double false_easting = ctv.findAttributeDouble(CF.FALSE_EASTING, 0.0);
    double false_northing = ctv.findAttributeDouble(CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = TransformBuilders.getFalseEastingScaleFactor(geoCoordinateUnits);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double earth_radius = TransformBuilders.getEarthRadiusInKm(ctv);
    double semi_major_axis = ctv.findAttributeDouble(CF.SEMI_MAJOR_AXIS, Double.NaN);
    double semi_minor_axis = ctv.findAttributeDouble(CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = ctv.findAttributeDouble(CF.INVERSE_FLATTENING, 0.0);

    ucar.unidata.geoloc.Projection proj;

    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
      proj = new ucar.unidata.geoloc.projection.proj4.TransverseMercatorProjection(earth, lon0, lat0, scale,
          false_easting, false_northing);
    } else {
      proj = new ucar.unidata.geoloc.projection.TransverseMercator(lat0, lon0, scale, false_easting, false_northing,
          earth_radius);
    }
    return proj;
  }
}
