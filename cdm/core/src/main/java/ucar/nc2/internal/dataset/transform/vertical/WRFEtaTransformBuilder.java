/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.WRFEta;

import java.util.Formatter;

/** Because the transform depends on NetcdfDataset and CoordinateSystem, must handle differently */
public class WRFEtaTransformBuilder implements VerticalCTBuilder, VerticalTransformBuilder {
  private final CoordinatesHelper.Builder coords;
  private final CoordinateSystem.Builder<?> cs;

  public WRFEtaTransformBuilder(CoordinatesHelper.Builder coords, CoordinateSystem.Builder<?> cs) {
    this.coords = coords;
    this.cs = cs;
  }

  // Note AttributeContainer ctv not used
  @Override
  public VerticalCT.Builder<?> makeVerticalCT(NetcdfFile ds, AttributeContainer ctv1) {
    VerticalCT.Type type = VerticalCT.Type.WRFEta;
    AttributeContainerMutable atts = new AttributeContainerMutable(getTransformName());
    atts.addAttribute(new Attribute("height formula", "height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81"));
    atts.addAttribute(new Attribute(WRFEta.PerturbationGeopotentialVariable, "PH"));
    atts.addAttribute(new Attribute(WRFEta.BaseGeopotentialVariable, "PHB"));
    atts.addAttribute(new Attribute("pressure formula", "pressure(x,y,z) = P(x,y,z) + PB(x,y,z)"));
    atts.addAttribute(new Attribute(WRFEta.PerturbationPressureVariable, "P"));
    atts.addAttribute(new Attribute(WRFEta.BasePressureVariable, "PB"));

    if (coords.findAxisByType(cs, AxisType.GeoX).isPresent())
      atts.addAttribute(new Attribute(WRFEta.IsStaggeredX, "" + isStaggered(AxisType.GeoX)));
    else
      atts.addAttribute(new Attribute(WRFEta.IsStaggeredX, "" + isStaggered2(AxisType.Lon, 1)));

    if (coords.findAxisByType(cs, AxisType.GeoY).isPresent())
      atts.addAttribute(new Attribute(WRFEta.IsStaggeredY, "" + isStaggered(AxisType.GeoY)));
    else
      atts.addAttribute(new Attribute(WRFEta.IsStaggeredY, "" + isStaggered2(AxisType.Lat, 0)));

    atts.addAttribute(new Attribute(WRFEta.IsStaggeredZ, "" + isStaggered(AxisType.GeoZ)));
    coords.findAxisByType(cs, AxisType.GeoZ)
        .ifPresent(a -> atts.addAttribute(new Attribute("eta", "" + a.getFullName())));

    return VerticalCT.builder().setName(getTransformName()).setCtvAttributes(atts)
        .setTransformType(TransformType.Vertical).setVerticalType(type).setTransformBuilder(this);
  }

  public String getTransformName() {
    return VerticalCT.Type.WRFEta.name() + cs.coordAxesNames;
  }

  @Override
  public void setErrorBuffer(Formatter sb) {}

  @Override
  public VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    return WRFEta.create(ds, timeDim, vCT.getParameters());
  }

  private boolean isStaggered(AxisType type) {
    return coords.findAxisByType(cs, type).map(a -> (a.shortName != null && a.shortName.endsWith("stag")))
        .orElse(false);
  }

  private boolean isStaggered2(AxisType type, int dimIndex) {
    return coords.findAxisByType(cs, type).map(a -> {
      String dimName = a.getDimensionName(dimIndex);
      return dimName != null && dimName.endsWith("stag");
    }).orElse(false);
  }

}

