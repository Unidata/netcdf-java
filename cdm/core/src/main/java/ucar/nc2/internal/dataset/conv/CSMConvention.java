/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.util.CancelTask;

/** CSM-1 Convention. Deprecated: use CF */
class CSMConvention extends CoardsConventions {
  private static final String CONVENTION_NAME = "NCAR-CSM";

  CSMConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    for (Variable.Builder vb : rootGroup.vbuilders) {
      VariableDS.Builder vds = (VariableDS.Builder) vb;
      String unit = vds.getUnits();
      if (unit != null && (unit.equalsIgnoreCase("hybrid_sigma_pressure") || unit.equalsIgnoreCase("sigma_level"))) {
        // both a coordinate axis and transform
        vds.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()));
        vds.addAttribute(new Attribute(_Coordinate.TransformType, TransformType.Vertical.toString()));
        vds.addAttribute(new Attribute(_Coordinate.Axes, vds.getFullName()));
      }
    }
  }

  @Override
  protected void identifyCoordinateAxes() {
    // coordinates is an alias for _CoordinateAxes
    for (VarProcess vp : varList) {
      if (vp.coordinateAxes == null) { // dont override if already set
        String coordsString = vp.vb.getAttributeContainer().findAttValueIgnoreCase(CF.COORDINATES, null);
        if (coordsString != null) {
          vp.coordinates = coordsString;
        }
      }
    }
    super.identifyCoordinateAxes();
  }

  public static class Factory implements CoordSystemBuilderFactory {

    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new CSMConvention(datasetBuilder);
    }
  }

}
