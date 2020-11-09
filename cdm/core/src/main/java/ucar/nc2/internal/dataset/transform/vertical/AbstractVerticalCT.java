/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.unidata.util.Parameter;
import java.util.Formatter;

/** Abstract superclass for implementations of VertTransformBuilderIF */
public abstract class AbstractVerticalCT {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractVerticalCT.class);

  private Formatter errBuffer;

  public void setErrorBuffer(Formatter errBuffer) {
    this.errBuffer = errBuffer;
  }

  public abstract String getTransformName();

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
  protected boolean addParameter(CoordinateTransform.Builder<?> rs, String paramName, NetcdfFile ds,
      String varNameEscaped) {
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
}
