/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.AtmosSigma;

/**
 * Create a atmosphere_sigma_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 * *
 * 
 * @author caron
 */
public class CFSigma extends AbstractVerticalCTBuilder implements VerticalTransformBuilder {
  private String sigma = "", ps = "", ptop = "";

  public String getTransformName() {
    return VerticalCT.Type.Sigma.name();
  }

  public TransformType getTransformType() {
    return TransformType.Vertical;
  }

  public VerticalCT.Builder<?> makeVerticalCT(NetcdfFile ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms)
      return null;

    // parse the formula string
    String[] values = parseFormula(formula_terms, "sigma ps ptop");
    if (values == null)
      return null;

    sigma = values[0];
    ps = values[1];
    ptop = values[2];

    VerticalCT.Builder<?> rs = VerticalCT.builder().setName("AtmSigma_Transform_" + ctv.getName())
        .setAuthority(getTransformName()).setVerticalType(VerticalCT.Type.Sigma).setTransformBuilder(this);

    rs.addParameter(new Attribute("standard_name", getTransformName()));
    rs.addParameter(new Attribute("formula_terms", formula_terms));
    rs.addParameter(new Attribute("formula", "pressure(x,y,z) = ptop + sigma(z)*(surfacePressure(x,y)-ptop)"));

    if (!addParameter(rs, AtmosSigma.PS, ds, ps))
      return null;
    if (!addParameter(rs, AtmosSigma.SIGMA, ds, sigma))
      return null;
    if (!addParameter(rs, AtmosSigma.PTOP, ds, ptop))
      return null;

    return rs;
  }

  public String toString() {
    return "Sigma:" + "sigma:" + sigma + " ps:" + ps + " ptop:" + ptop;
  }

  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return AtmosSigma.create(ds, timeDim, vCT.getCtvAttributes());
  }
}

