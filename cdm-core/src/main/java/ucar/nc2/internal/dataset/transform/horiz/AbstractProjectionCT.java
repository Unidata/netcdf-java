/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.horiz;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Earth;

import java.util.Formatter;
import java.util.StringTokenizer;

/** Abstract superclass for implementations of HorizTransformBuilderIF */
public abstract class AbstractProjectionCT implements ProjectionBuilder {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractProjectionCT.class);

  protected double lat0, lon0, false_easting, false_northing, earth_radius;
  protected Earth earth;

  private Formatter errBuffer;

  public void setErrorBuffer(Formatter errBuffer) {
    this.errBuffer = errBuffer;
  }

  void readStandardParams(AttributeContainer ctv, String units) {
    lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    if (Double.isNaN(lon0))
      lon0 = ctv.findAttributeDouble(CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    lat0 = ctv.findAttributeDouble(CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    false_easting = ctv.findAttributeDouble(CF.FALSE_EASTING, 0.0);
    false_northing = ctv.findAttributeDouble(CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = TransformBuilders.getFalseEastingScaleFactor(units);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double semi_major_axis = ctv.findAttributeDouble(CF.SEMI_MAJOR_AXIS, Double.NaN);
    double semi_minor_axis = ctv.findAttributeDouble(CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = ctv.findAttributeDouble(CF.INVERSE_FLATTENING, 0.0);

    earth_radius = TransformBuilders.getEarthRadiusInKm(ctv);
    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
    }
  }

  /**
   * Read an attribute as double[2]. If only one value, make second same as first.
   *
   * @param att the attribute. May be numeric or String.
   * @return attribute value as a double[2]
   */
  double[] readAttributeDouble2(Attribute att) {
    if (att == null)
      return null;

    double[] val = new double[2];
    if (att.isString()) {
      StringTokenizer stoke = new StringTokenizer(att.getStringValue());
      val[0] = Double.parseDouble(stoke.nextToken());
      val[1] = stoke.hasMoreTokens() ? Double.parseDouble(stoke.nextToken()) : val[0];
    } else {
      val[0] = att.getNumericValue().doubleValue();
      val[1] = (att.getLength() > 1) ? att.getNumericValue(1).doubleValue() : val[0];
    }
    return val;
  }
}
