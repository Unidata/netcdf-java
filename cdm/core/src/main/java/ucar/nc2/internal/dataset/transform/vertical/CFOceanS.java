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
import ucar.unidata.geoloc.vertical.OceanS;
import ucar.unidata.util.Parameter;

/**
 * Create a ocean_s_coordinate Vertical Transform from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class CFOceanS extends AbstractVerticalCT implements VertTransformBuilderIF {
  private String s = "", eta = "", depth = "", a = "", b = "", depth_c = "";

  public String getTransformName() {
    return VerticalCT.Type.OceanS.name();
  }

  public VerticalCT.Builder<?> makeCoordinateTransform(NetcdfFile ds, AttributeContainer ctv) {
    String formula_terms = getFormula(ctv);
    if (null == formula_terms)
      return null;

    // parse the formula string
    String[] values = parseFormula(formula_terms, "s eta depth a b depth_c");
    if (values == null)
      return null;

    s = values[0];
    eta = values[1];
    depth = values[2];
    a = values[3];
    b = values[4];
    depth_c = values[5];

    VerticalCT.Builder<?> rs = VerticalCT.builder().setName("OceanS_Transform_" + ctv.getName())
        .setAuthority(getTransformName()).setType(VerticalCT.Type.OceanS).setTransformBuilder(this);

    rs.addParameter(new Parameter("standard_name", getTransformName()));
    rs.addParameter(new Parameter("formula_terms", formula_terms));
    // rs.addParameter((new Parameter("height_formula", "height(x,y,z) = eta(x,y)*(1+s(z)) + depth_c*s(z) +
    // (depth(x,y)-depth_c)*C(z)")));
    // -sachin 03/25/09 modify formula according to Hernan Arango
    rs.addParameter((new Parameter("height_formula",
        "height(x,y,z) = depth_c*s(z) + (depth(x,y)-depth_c)*C(z) + eta(x,y) * (1 + (depth_c*s(z) + (depth(x,y)-depth_c)*C(z))/depth(x,y) ")));
    rs.addParameter(
        (new Parameter("C_formula", "C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)")));

    if (!addParameter(rs, OceanS.ETA, ds, eta))
      return null;
    if (!addParameter(rs, OceanS.S, ds, s))
      return null;
    if (!addParameter(rs, OceanS.DEPTH, ds, depth))
      return null;

    if (!addParameter(rs, OceanS.DEPTH_C, ds, depth_c))
      return null;
    if (!addParameter(rs, OceanS.A, ds, a))
      return null;
    if (!addParameter(rs, OceanS.B, ds, b))
      return null;

    return rs;
  }

  public String toString() {
    return "OceanS:" + " s:" + s + " eta:" + eta + " depth:" + depth + " a:" + a + " b:" + b + " depth_c:" + depth_c;

  }

  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return OceanS.create(ds, timeDim, vCT.getParameters());
  }
}

