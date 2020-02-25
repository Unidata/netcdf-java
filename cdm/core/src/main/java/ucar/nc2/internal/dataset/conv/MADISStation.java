/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.StringTokenizer;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.util.CancelTask;

/** MADIS Station Convention. */
public class MADISStation extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "MADIS_Station_1.0";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      String s = ncfile.getRootGroup().attributes().findAttValueIgnoreCase("Conventions", "none");
      return s.startsWith("MADIS");
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new MADISStation(datasetBuilder);
    }
  }

  /////////////////////////////////////////////////////////////////

  private MADISStation(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) throws IOException {
    String timeVars = rootGroup.getAttributeContainer().findAttValueIgnoreCase("timeVariables", "");
    StringTokenizer stoker = new StringTokenizer(timeVars, ", ");
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      rootGroup.findVariable(vname)
          .ifPresent(v -> v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString())));
    }

    String locVars = rootGroup.getAttributeContainer().findAttValueIgnoreCase("stationLocationVariables", "");
    stoker = new StringTokenizer(locVars, ", ");
    int count = 0;
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      if (rootGroup.findVariable(vname).isPresent()) {
        Variable.Builder v = rootGroup.findVariable(vname).get();
        AxisType atype = count == 0 ? AxisType.Lat : count == 1 ? AxisType.Lon : AxisType.Height;
        v.addAttribute(new Attribute(_Coordinate.AxisType, atype.toString()));
      } else {
        parseInfo.format(" cant find time variable %s%n", vname);
      }
      count++;
    }
  }

}
