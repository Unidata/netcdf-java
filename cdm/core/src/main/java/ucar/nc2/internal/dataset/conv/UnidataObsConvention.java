/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.StringTokenizer;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;

/**
 * Unidata Observation Dataset v1.0
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html"
 */
public class UnidataObsConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "Unidata Observation Dataset v1.0";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new UnidataObsConvention(datasetBuilder);
    }
  }

  UnidataObsConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    if (!hasAxisType(AxisType.Lat)) { // already has _CoordinateAxisType
      if (!addAxisType("latitude", AxisType.Lat)) { // directly named
        String vname = rootGroup.getAttributeContainer().findAttValueIgnoreCase("latitude_coordinate", null);
        if (!addAxisType(vname, AxisType.Lat)) { // attribute named
          Variable.Builder v = hasUnits("degrees_north,degrees_N,degreesN,degree_north,degree_N,degreeN");
          if (v != null)
            addAxisType(v, AxisType.Lat); // CF-1
        }
      }
    }

    // longitude
    if (!hasAxisType(AxisType.Lon)) { // already has _CoordinateAxisType
      if (!addAxisType("longitude", AxisType.Lon)) { // directly named
        String vname = rootGroup.getAttributeContainer().findAttValueIgnoreCase("longitude_coordinate", null);
        if (!addAxisType(vname, AxisType.Lon)) { // attribute named
          Variable.Builder v = hasUnits("degrees_east,degrees_E,degreesE,degree_east,degree_E,degreeE");
          if (v != null)
            addAxisType(v, AxisType.Lon); // CF-1
        }
      }
    }

    // altitude
    if (!hasAxisType(AxisType.Height)) { // already has _CoordinateAxisType
      if (!addAxisType("altitude", AxisType.Height)) { // directly named
        if (!addAxisType("depth", AxisType.Height)) { // directly named
          String vname = rootGroup.getAttributeContainer().findAttValueIgnoreCase("altitude_coordinate", null);
          if (!addAxisType(vname, AxisType.Height)) { // attribute named
            for (Variable.Builder v : rootGroup.vbuilders) {
              String positive = v.getAttributeContainer().findAttValueIgnoreCase(CF.POSITIVE, null);
              if (positive != null) {
                addAxisType(v, AxisType.Height); // CF-1
                break;
              }
            }
          }
        }
      }
    }

    // time
    if (!hasAxisType(AxisType.Time)) { // already has _CoordinateAxisType
      if (!addAxisType("time", AxisType.Time)) { // directly named
        String vname = rootGroup.getAttributeContainer().findAttValueIgnoreCase("time_coordinate", null);
        if (!addAxisType(vname, AxisType.Time)) { // attribute named
          for (Variable.Builder v : rootGroup.vbuilders) {
            String unit = ((VariableDS.Builder) v).getUnits();
            if (unit == null)
              continue;
            if (SimpleUnit.isDateUnit(unit)) {
              addAxisType(v, AxisType.Time); // CF-1
              break;
            }
          }
        }
      }
    }

  }

  private boolean hasAxisType(AxisType a) {
    for (Variable.Builder v : rootGroup.vbuilders) {
      String axisType = v.getAttributeContainer().findAttValueIgnoreCase("CoordinateAxisType", null);
      if ((axisType != null) && axisType.equals(a.toString()))
        return true;
    }
    return false;
  }

  private VariableDS.Builder hasUnits(String unitList) {
    StringTokenizer stoker = new StringTokenizer(unitList, ",");
    while (stoker.hasMoreTokens()) {
      String unit = stoker.nextToken();
      for (Variable.Builder v : rootGroup.vbuilders) {
        VariableDS.Builder ve = (VariableDS.Builder) v;
        String hasUnit = ve.getUnits();
        if (hasUnit == null)
          continue;
        if (hasUnit.equalsIgnoreCase(unit))
          return ve;
      }
    }
    return null;
  }

  private boolean addAxisType(String vname, AxisType a) {
    if (vname == null)
      return false;
    rootGroup.findVariable(vname).ifPresent(v ->
      v.addAttribute(new Attribute(_Coordinate.AxisType, a.toString())));
    return rootGroup.findVariable(vname).isPresent();
  }

  private void addAxisType(Variable.Builder v, AxisType a) {
    v.addAttribute(new Attribute(_Coordinate.AxisType, a.toString()));
  }

}
