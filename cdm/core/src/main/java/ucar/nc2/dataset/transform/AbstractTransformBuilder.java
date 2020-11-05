/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.util.Parameter;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Formatter;

/**
 * Abstract superclass for implementations of HorizTransformBuilderIF and VertTransformBuilderIF
 * 
 * @deprecated will be a static helper class in ver7.
 */
@Deprecated
public abstract class AbstractTransformBuilder {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractTransformBuilder.class);

  /*
   * from CF: false_easting(false_northing):
   * The value added to all abscissa(ordinate) values in the rectangular coordinates for a map projection.
   * This value frequently is assigned to eliminate negative numbers.
   * Expressed in the unit of the coordinate variable identified by the standard name projection_x_coordinate
   * (projection_y_coordinate).
   */
  public static double getFalseEastingScaleFactor(NetcdfDataset ds, AttributeContainer ctv) {
    String units = getGeoCoordinateUnits(ds, ctv);
    return getFalseEastingScaleFactor(units);
  }

  public static String getGeoCoordinateUnits(NetcdfDataset ds, AttributeContainer ctv) {
    String units = ctv.findAttributeString(CDM.UNITS, null);
    if (units == null) {
      List<CoordinateAxis> axes = ds.getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        if (axis.getAxisType() == AxisType.GeoX) { // kludge - what if there's multiple ones?
          Variable v = axis.getOriginalVariable(); // LOOK why original variable ?
          units = (v == null) ? axis.getUnitsString() : v.getUnitsString();
          break;
        }
      }
      if (units == null) {
        Variable xvar = ds.getRootGroup().findVariableByAttribute(_Coordinate.AxisType, AxisType.GeoX.toString());
        if (xvar != null) {
          units = xvar.getUnitsString();
        }
      }
    }
    return units;
  }

  public static double getFalseEastingScaleFactor(String geoCoordinateUnits) {
    if (geoCoordinateUnits != null) {
      try {
        SimpleUnit unit = SimpleUnit.factoryWithExceptions(geoCoordinateUnits);
        return unit.convertTo(1.0, SimpleUnit.kmUnit);
      } catch (Exception e) {
        log.warn(geoCoordinateUnits + " not convertible to km");
      }
    }
    return 1.0;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  private Formatter errBuffer;
  protected double lat0, lon0, false_easting, false_northing, earth_radius;
  protected Earth earth;

  public void setErrorBuffer(Formatter errBuffer) {
    this.errBuffer = errBuffer;
  }

  public abstract String getTransformName();

  // WTF ?
  void readStandardParams(AttributeContainer ctv, String units) {
    lon0 = readAttributeDouble(ctv, CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    if (Double.isNaN(lon0))
      lon0 = readAttributeDouble(ctv, CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    lat0 = readAttributeDouble(ctv, CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    false_easting = readAttributeDouble(ctv, CF.FALSE_EASTING, 0.0);
    false_northing = readAttributeDouble(ctv, CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = getFalseEastingScaleFactor(units);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double semi_major_axis = readAttributeDouble(ctv, CF.SEMI_MAJOR_AXIS, Double.NaN);
    double semi_minor_axis = readAttributeDouble(ctv, CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = readAttributeDouble(ctv, CF.INVERSE_FLATTENING, 0.0);

    earth_radius = getEarthRadiusInKm(ctv);
    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
    }
  }

  /**
   * Read an attribute as a double. May be string or numeric
   *
   * @param atts the attribute container
   * @param attname name of variable
   * @param defValue default value if attribute is not found
   * @return attribute value as a double, else defValue if not found
   * @deprecated use atts.findAttributeDouble(attname, defValue);
   */
  @Deprecated
  protected double readAttributeDouble(AttributeContainer atts, String attname, double defValue) {
    Attribute att = atts.findAttributeIgnoreCase(attname);
    if (att == null)
      return defValue;
    else if (att.isString())
      return Double.parseDouble(att.getStringValue());
    else
      return att.getNumericValue().doubleValue();
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

  /**
   * Add a Parameter to a CoordinateTransform.
   * Make sure that the variable exists.
   *
   * @param rs the CoordinateTransform
   * @param paramName the parameter name
   * @param ds dataset
   * @param varNameEscaped escaped variable name
   * @return true if success, false is failed
   */
  protected boolean addParameter(CoordinateTransform rs, String paramName, NetcdfFile ds, String varNameEscaped) {
    if (null == (ds.findVariable(varNameEscaped))) {
      if (null != errBuffer)
        errBuffer.format("CoordTransBuilder %s: no Variable named %s%n", getTransformName(), varNameEscaped);
      return false;
    }

    rs.addParameter(new Parameter(paramName, varNameEscaped));
    return true;
  }

  String getFormula(AttributeContainer ctv) {
    String formula = ctv.findAttributeString("formula_terms", null);
    if (null == formula) {
      if (null != errBuffer)
        errBuffer.format("CoordTransBuilder %s: needs attribute 'formula_terms' on Variable %s%n", getTransformName(),
            ctv.getName());
      return null;
    }
    return formula;
  }

  String[] parseFormula(String formula_terms, String termString) {
    String[] formulaTerms = formula_terms.split("[\\s:]+"); // split on 1 or more whitespace or ':'
    String[] terms = termString.split("[\\s]+"); // split on 1 or more whitespace
    String[] values = new String[terms.length];

    for (int i = 0; i < terms.length; i++) {
      for (int j = 0; j < formulaTerms.length; j += 2) { // look at every other formula term
        if (terms[i].equals(formulaTerms[j])) { // if it matches
          values[i] = formulaTerms[j + 1]; // next term is the value
          break;
        }
      }
    }

    boolean ok = true;
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        if (null != errBuffer)
          errBuffer.format("Missing term=%s in the formula '%s' for the vertical transform= %s%n", terms[i],
              formula_terms, getTransformName());
        ok = false;
      }
    }

    return ok ? values : null;
  }

  /**
   * Get the earth radius in km from the attribute "earth_radius".
   * Normally this is in meters, convert to km if its > 10,000.
   * Use Earth.getRadius() as default.
   *
   * @param ctv coord transform variable
   * @return earth radius in km
   */
  static double getEarthRadiusInKm(AttributeContainer ctv) {
    double earth_radius = ctv.findAttributeDouble(CF.EARTH_RADIUS, Earth.WGS84_EARTH_RADIUS_METERS);
    if (earth_radius > 10000.0)
      earth_radius *= .001;
    return earth_radius;
  }
}
