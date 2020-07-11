/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import java.util.ArrayList;
import java.util.List;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.geoloc.vertical.WRFEta;
import ucar.unidata.util.Parameter;

/** Because the transform depends on NetcdfDataset and CoordinateSystem, must handle differently */
public class WRFEtaTransformBuilder implements VerticalCTBuilder {
  private final CoordinatesHelper.Builder coords;
  private final CoordinateSystem.Builder cs;

  public WRFEtaTransformBuilder(CoordinatesHelper.Builder coords, CoordinateSystem.Builder cs) {
    this.coords = coords;
    this.cs = cs;
  }

  @Override
  public VerticalCT makeVerticalCT(NetcdfDataset ds) {
    VerticalCT.Type type = VerticalCT.Type.WRFEta;
    List<Parameter> params = new ArrayList<>();
    params.add(new Parameter("height formula", "height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81"));
    params.add(new Parameter(WRFEta.PerturbationGeopotentialVariable, "PH"));
    params.add(new Parameter(WRFEta.BaseGeopotentialVariable, "PHB"));
    params.add(new Parameter("pressure formula", "pressure(x,y,z) = P(x,y,z) + PB(x,y,z)"));
    params.add(new Parameter(WRFEta.PerturbationPressureVariable, "P"));
    params.add(new Parameter(WRFEta.BasePressureVariable, "PB"));

    if (coords.findAxisByType(cs, AxisType.GeoX).isPresent())
      params.add(new Parameter(WRFEta.IsStaggeredX, "" + isStaggered(AxisType.GeoX)));
    else
      params.add(new Parameter(WRFEta.IsStaggeredX, "" + isStaggered2(AxisType.Lon, 1)));

    if (coords.findAxisByType(cs, AxisType.GeoY).isPresent())
      params.add(new Parameter(WRFEta.IsStaggeredY, "" + isStaggered(AxisType.GeoY)));
    else
      params.add(new Parameter(WRFEta.IsStaggeredY, "" + isStaggered2(AxisType.Lat, 0)));

    params.add(new Parameter(WRFEta.IsStaggeredZ, "" + isStaggered(AxisType.GeoZ)));
    coords.findAxisByType(cs, AxisType.GeoZ).ifPresent(a -> params.add(new Parameter("eta", "" + a.getFullName())));

    return new WRFEtaTransform(getTransformName(), type.toString(), type, params);
  }

  public String getTransformName() {
    return VerticalCT.Type.WRFEta.name() + cs.coordAxesNames;
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

  static class WRFEtaTransform extends VerticalCT implements VerticalTransformBuilder {
    WRFEtaTransform(String name, String authority, Type type, List<Parameter> params) {
      super(name, authority, type, params);
    }

    @Override
    public VerticalTransform makeVerticalTransform(NetcdfDataset ds, Dimension timeDim) {
      return new WRFEta(ds, timeDim, this.getParameters());
    }
  }

}

