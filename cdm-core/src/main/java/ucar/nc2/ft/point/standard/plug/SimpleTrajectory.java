/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.time2.CalendarDateUnit;
import ucar.nc2.units.SimpleUnit;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * SimpleTrajectory netcdf files
 * One trajectory, one dimension (time), per file.
 * The EOL trajectory datasets follow this simple layout.
 *
 * @author sarms
 * @since June 4, 2015
 */

public class SimpleTrajectory extends TableConfigurerImpl {

  private static final String timeDimName = "time";
  private static final String timeVarName = "time";
  private static final String latVarName = "latitude";
  private static final String lonVarName = "longitude";
  private static final String elevVarName = "altitude";

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    List<Dimension> list = ds.getRootGroup().getDimensions();

    // should have only one dimension
    if (list.size() != 1) {
      return false;
    }

    // dimension name should be "time"
    Dimension d = list.get(0);
    if (!d.getShortName().equals(timeDimName)) {
      return false;
    }

    // Check that have variable time(time) with units that are udunits time
    Variable timeVar = ds.getRootGroup().findVariableLocal(timeVarName);
    if (timeVar == null) {
      return false;
    }
    list = timeVar.getDimensions();
    if (list.size() != 1) {
      return false;
    }
    d = list.get(0);
    if (!d.getShortName().equals(timeDimName)) {
      return false;
    }
    String units = timeVar.findAttributeString("units", "");
    // check that it parses
    try {
      CalendarDateUnit.fromUdunitString(null, units);
    } catch (IllegalArgumentException e) {
      return false;
    }
    /*
     * was
     * Date date = DateUnit.getStandardDate("0 " + units); // leave this as it doesnt throw an exception on failure
     * if (date == null) {
     * return false;
     * }
     */

    // Check for variable latitude(time) with units of "deg".
    Variable latVar = ds.getRootGroup().findVariableLocal(latVarName);
    if (latVar == null) {
      return false;
    }
    list = latVar.getDimensions();
    if (list.size() != 1) {
      return false;
    }
    d = list.get(0);
    if (!d.getShortName().equals(timeDimName)) {
      return false;
    }
    units = latVar.findAttribute("units").getStringValue();
    if (!SimpleUnit.isCompatible(units, "degrees_north")) {
      return false;
    }

    // Check for variable longitude(time) with units of "deg".
    Variable lonVar = ds.getRootGroup().findVariableLocal(lonVarName);
    if (lonVar == null) {
      return false;
    }
    list = lonVar.getDimensions();
    if (list.size() != 1) {
      return false;
    }
    d = (Dimension) list.get(0);
    if (!d.getShortName().equals(timeDimName)) {
      return false;
    }
    units = lonVar.findAttribute("units").getStringValue();
    if (!SimpleUnit.isCompatible(units, "degrees_east")) {
      return false;
    }


    // Check for variable altitude(time) with units of "m".
    Variable elevVar = ds.getRootGroup().findVariableLocal(elevVarName);
    if (elevVar == null) {
      return false;
    }
    list = elevVar.getDimensions();
    if (list.size() != 1) {
      return false;
    }
    d = (Dimension) list.get(0);
    if (!d.getShortName().equals(timeDimName)) {
      return false;
    }
    units = elevVar.findAttribute("units").getStringValue();
    return SimpleUnit.isCompatible(units, "meters");
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    PointConfigXML reader = new PointConfigXML();
    return reader.readConfigXMLfromResource("resources/nj22/pointConfig/SimpleTrajectory.xml", wantFeatureType, ds,
        errlog);
  }
}
