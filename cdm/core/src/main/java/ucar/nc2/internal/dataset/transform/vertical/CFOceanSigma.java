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
import ucar.unidata.geoloc.vertical.OceanSigma;
import ucar.unidata.util.Parameter;

/**
 * Create a ocean_sigma_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class CFOceanSigma extends AbstractVerticalCT implements VertTransformBuilderIF {
  private String sigma, eta, depth;

  public String getTransformName() {
    return VerticalCT.Type.OceanSigma.name();
  }

  public VerticalCT.Builder<?> makeCoordinateTransform(NetcdfFile ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms)
      return null;

    String[] values = parseFormula(formula_terms, "sigma eta depth");
    if (values == null)
      return null;

    sigma = values[0];
    eta = values[1];
    depth = values[2];

    VerticalCT.Builder<?> rs = VerticalCT.builder().setName("OceanSigma_Transform_" + ctv.getName())
        .setAuthority(getTransformName()).setType(VerticalCT.Type.OceanSigma).setTransformBuilder(this);

    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));
    rs.addParameter((new Parameter("height_formula", "height(x,y,z) = eta(n,j,i) + sigma(k)*(depth(j,i)+eta(n,j,i))")));

    if (!addParameter(rs, OceanSigma.SIGMA, ds, sigma))
      return null;
    if (!addParameter(rs, OceanSigma.ETA, ds, eta))
      return null;
    if (!addParameter(rs, OceanSigma.DEPTH, ds, depth))
      return null;

    return rs;
  }


  public String toString() {
    return "OceanS:" + " sigma:" + sigma + " eta:" + eta + " depth:" + depth;
  }

  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return OceanSigma.create(ds, timeDim, vCT.getParameters());
  }
}

