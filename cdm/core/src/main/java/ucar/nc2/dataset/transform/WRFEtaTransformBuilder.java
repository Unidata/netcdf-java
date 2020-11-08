/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.NetcdfFile;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.WRFEta;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.*;
import ucar.unidata.util.Parameter;

/**
 * Create WRF Eta vertical transform.
 * This is different from other VertTransformBuilderIF because it is specific to a CoordinateSystem.
 */
public class WRFEtaTransformBuilder extends AbstractVerticalCT implements VertTransformBuilderIF {
  private CoordinateSystem cs; // can we figure out cs from ds?

  public WRFEtaTransformBuilder(CoordinateSystem cs) {
    this.cs = cs;
  }

  public VerticalCT.Builder<?> makeCoordinateTransform(NetcdfFile ds, AttributeContainer ctv) {
    VerticalCT.Type type = VerticalCT.Type.WRFEta;
    VerticalCT.Builder<?> rs = VerticalCT.builder().setName(type.toString()).setAuthority(getTransformName())
        .setType(type).setTransformBuilder(this);

    rs.addParameter(new Parameter("height formula", "height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81"));
    rs.addParameter(new Parameter(WRFEta.PerturbationGeopotentialVariable, "PH"));
    rs.addParameter(new Parameter(WRFEta.BaseGeopotentialVariable, "PHB"));
    rs.addParameter(new Parameter("pressure formula", "pressure(x,y,z) = P(x,y,z) + PB(x,y,z)"));
    rs.addParameter(new Parameter(WRFEta.PerturbationPressureVariable, "P"));
    rs.addParameter(new Parameter(WRFEta.BasePressureVariable, "PB"));

    if (cs.getXaxis() != null)
      rs.addParameter(new Parameter(WRFEta.IsStaggeredX, "" + isStaggered(cs.getXaxis())));
    else
      rs.addParameter(new Parameter(WRFEta.IsStaggeredX, "" + isStaggered2(cs.getLonAxis(), 1)));

    if (cs.getYaxis() != null)
      rs.addParameter(new Parameter(WRFEta.IsStaggeredY, "" + isStaggered(cs.getYaxis())));
    else
      rs.addParameter(new Parameter(WRFEta.IsStaggeredY, "" + isStaggered2(cs.getLatAxis(), 0)));

    rs.addParameter(new Parameter(WRFEta.IsStaggeredZ, "" + isStaggered(cs.getZaxis())));
    rs.addParameter(new Parameter("eta", "" + cs.getZaxis().getFullName()));

    return rs;
  }

  public String getTransformName() {
    return VerticalCT.Type.WRFEta.name();
  }

  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return WRFEta.create(ds, timeDim, vCT.getParameters());
  }

  private boolean isStaggered(CoordinateAxis axis) {
    if (axis == null)
      return false;
    String name = axis.getShortName();
    return name != null && name.endsWith("stag");
  }

  private boolean isStaggered2(CoordinateAxis axis, int dimIndex) {
    if (axis == null)
      return false;
    Dimension dim = axis.getDimension(dimIndex);
    return dim != null && dim.getShortName().endsWith("stag");
  }

}

