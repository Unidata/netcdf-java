/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.AttributeContainer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.AtmosLnPressure;
import ucar.unidata.util.Parameter;

/**
 * implementation for CF vertical coordinate "atmosphere_ln_pressure_coordinate".
 * DO NOT USE: see CF1Convention.makeAtmLnCoordinate()
 * 
 * @author caron
 * @since May 6, 2008
 */
public class CFLnPressure extends AbstractVerticalCT implements VertTransformBuilderIF {
  private String p0, lev;

  public String getTransformName() {
    return VerticalCT.Type.LnPressure.name();
  }

  public VerticalCT.Builder<?> makeCoordinateTransform(NetcdfFile ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms)
      return null;

    // parse the formula string
    String[] values = parseFormula(formula_terms, "p0 lev");
    if (values == null)
      return null;

    p0 = values[0];
    lev = values[1];

    VerticalCT.Builder<?> rs = VerticalCT.builder().setName("AtmSigma_Transform_" + ctv.getName())
        .setAuthority(getTransformName()).setType(VerticalCT.Type.LnPressure).setTransformBuilder(this);

    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));
    rs.addParameter(new Parameter("formula", "pressure(z) = p0 * exp(-lev(k))"));

    if (!addParameter(rs, AtmosLnPressure.P0, ds, p0))
      return null;
    if (!addParameter(rs, AtmosLnPressure.LEV, ds, lev))
      return null;

    return rs;
  }

  public String toString() {
    return "AtmLnPressure:" + "p0:" + p0 + " lev:" + lev;
  }


  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return AtmosLnPressure.create(ds, timeDim, vCT.getParameters());
  }
}


