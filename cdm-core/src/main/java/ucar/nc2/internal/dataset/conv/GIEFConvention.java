/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.util.CancelTask;

/**
 * GEIF Convention.
 * https://www.metnet.navy.mil/~hofschnr/GIEF-F/1.2/
 */
public class GIEFConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "GIEF";

  GIEFConvention(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    NcmlReader.wrapNcmlResource(datasetBuilder, CoordSystemFactory.resourcesDir + "GIEF.ncml", cancelTask);

    Variable.Builder<?> timeVar =
        rootGroup.findVariableLocal("time").orElseThrow(() -> new IllegalStateException("must have time variable"));
    String time_units = rootGroup.getAttributeContainer().findAttributeString("time_units", null);
    timeVar.addAttribute(new Attribute(CDM.UNITS, time_units));

    Variable.Builder<?> levelVar =
        rootGroup.findVariableLocal("level").orElseThrow(() -> new IllegalStateException("must have level variable"));
    String level_units = rootGroup.getAttributeContainer().findAttributeString("level_units", null);
    String level_name = rootGroup.getAttributeContainer().findAttributeString("level_name", null);
    levelVar.addAttribute(new Attribute(CDM.UNITS, level_units));
    levelVar.addAttribute(new Attribute(CDM.LONG_NAME, level_name));

    // may be 1 or 2 data variables
    String unit_name = rootGroup.getAttributeContainer().findAttributeString("unit_name", null);
    String parameter_name = rootGroup.getAttributeContainer().findAttributeString("parameter_name", null);
    for (Variable.Builder<?> v : rootGroup.vbuilders) {
      if (v.getRank() > 1) {
        v.addAttribute(new Attribute(CDM.UNITS, unit_name));
        v.addAttribute(new Attribute(CDM.LONG_NAME, v.shortName + " " + parameter_name));
        v.addAttribute(new Attribute(_Coordinate.Axes, "time level latitude longitude"));
      }
    }

    Attribute translation = rootGroup.getAttributeContainer().findAttributeIgnoreCase("translation");
    Attribute affine = rootGroup.getAttributeContainer().findAttributeIgnoreCase("affine_transformation");

    // add lat
    double startLat = translation.getNumericValue(1).doubleValue();
    double incrLat = affine.getNumericValue(6).doubleValue();
    Variable.Builder<?> latVar = rootGroup.findVariableLocal("latitude")
        .orElseThrow(() -> new IllegalStateException("must have latitude variable"));
    latVar.setAutoGen(startLat, incrLat);

    // add lon
    double startLon = translation.getNumericValue(0).doubleValue();
    double incrLon = affine.getNumericValue(3).doubleValue();
    Variable.Builder<?> lonVar = rootGroup.findVariableLocal("longitude")
        .orElseThrow(() -> new IllegalStateException("must have longitude variable"));
    lonVar.setAutoGen(startLon, incrLon);
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new GIEFConvention(datasetBuilder);
    }
  }

}
