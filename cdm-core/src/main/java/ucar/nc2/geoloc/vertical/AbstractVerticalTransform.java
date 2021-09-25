/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Implementation of a transformation to a vertical reference coordinate system,
 * such as height or pressure.
 */
@Immutable
abstract class AbstractVerticalTransform implements VerticalTransform {
  protected final String units;
  private final Dimension timeDim;

  /**
   * Construct a VerticalCoordinate
   *
   * @param timeDim time dimension
   * @param units vertical coordinate unit
   */
  AbstractVerticalTransform(Dimension timeDim, @Nullable String units) {
    this.timeDim = timeDim;
    this.units = units;
  }

  @Override
  @Nullable
  public String getUnitString() {
    return units;
  }

  @Override
  public boolean isTimeDependent() {
    return (timeDim != null);
  }

  /** Get the time Dimension, if it exists */
  protected Dimension getTimeDimension() {
    return timeDim;
  }

  /*
   * Read the data {@link Array} from the variable, at the specified
   * time index if applicable. If the variable does not have a time
   * dimension, the data array will have the same rank as the Variable.
   * If the variable has a time dimension, the data array will have rank-1.
   *
   * @param v variable to read
   * 
   * @param timeIndex time index, ignored if !isTimeDependent()
   * 
   * @return Array from the variable at that time index
   *
   * @throws IOException problem reading data
   * 
   * @throws InvalidRangeException _more_
   *
   * Array<Number> readArray(Variable v, int timeIndex) throws IOException, InvalidRangeException {
   * int[] shape = v.getShape();
   * int[] origin = new int[v.getRank()];
   * 
   * if (getTimeDimension() != null) {
   * int dimIndex = v.findDimensionIndex(getTimeDimension().getShortName());
   * if (dimIndex >= 0) {
   * shape[dimIndex] = 1;
   * origin[dimIndex] = timeIndex;
   * return v.readArray(origin, shape).reduce(dimIndex);
   * }
   * }
   * 
   * return v.readArray(origin, shape);
   * }
   */

  @Override
  public AbstractVerticalTransform subset(Range t_range, Range z_range, Range y_range, Range x_range) {
    return new VerticalTransformSubset(this, t_range, z_range, y_range, x_range);
  }

  static double readAndConvertUnit(String scalarVariable, String convertToUnit) {
    return 0.0;
  }

  static double readAndConvertUnit(Variable scalarVariable, String convertToUnit) {
    double result;
    try {
      result = scalarVariable.readScalarDouble();
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "VerticalTransform failed to read " + scalarVariable + " err= " + e.getMessage());
    }

    String scalarUnitStr = scalarVariable.findAttributeString(CDM.UNITS, null);
    // if no unit is given, assume its the correct unit
    if (scalarUnitStr != null && !convertToUnit.equalsIgnoreCase(scalarUnitStr)) {
      // Convert result to units of wantUnit
      double factor = convertUnitFactor(scalarUnitStr, convertToUnit);
      result *= factor;
    }
    return result;
  }

  static double convertUnitFactor(String unit, String convertToUnit) {
    SimpleUnit wantUnit = SimpleUnit.factory(convertToUnit);
    SimpleUnit haveUnit = SimpleUnit.factory(unit);
    return haveUnit.convertTo(1.0, wantUnit);
  }

  static String getFormula(AttributeContainer ctv, Formatter errlog) {
    String formula = ctv.findAttributeString("formula_terms", null);
    if (null == formula) {
      errlog.format("CoordTransBuilder %s: needs attribute 'formula_terms' on %s%n", ctv.getName(), ctv.getName());
      return null;
    }
    return formula;
  }

  static List<String> parseFormula(String formula_terms, String termString, Formatter errlog) {
    String[] formulaTerms = formula_terms.split("[\\s:]+"); // split on 1 or more whitespace or ':'
    Iterable<String> terms = StringUtil2.split(termString); // split on 1 or more whitespace
    List<String> values = new ArrayList<>();

    for (String term : terms) {
      for (int j = 0; j < formulaTerms.length; j += 2) { // look at every other formula term
        if (term.equals(formulaTerms[j])) { // if it matches
          values.add(formulaTerms[j + 1]); // next term is the value
          break;
        }
      }
    }
    return values;

    /*
     * boolean ok = true;
     * for (String value : values) {
     * if (values == null) {
     * errlog.format("Missing term=%s in the formula '%s' for the vertical transform= %s%n", terms[i],
     * formula_terms, getTransformName());
     * ok = false;
     * }
     * }
     * 
     * return ok ? values : null;
     */
  }

}

