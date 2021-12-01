/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset.conv;

import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil2;

/**
 * NCAR RAF / NIMBUS
 * 
 * @see "https://web.archive.org/web/20180621013727/https://www.eol.ucar.edu/raf/Software/netCDF.html"
 */
public class Nimbus extends CoardsConventions {
  private static final String CONVENTION_NAME = "NCAR-RAF/nimbus";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new Nimbus(datasetBuilder);
    }
  }

  private Nimbus(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) {
    rootGroup.addAttribute(new Attribute("cdm_data_type", ucar.nc2.constants.FeatureType.TRAJECTORY.name()));
    rootGroup.addAttribute(new Attribute(CF.FEATURE_TYPE, ucar.nc2.constants.FeatureType.TRAJECTORY.name()));

    if (!setAxisType("LATC", AxisType.Lat))
      if (!setAxisType("LAT", AxisType.Lat))
        setAxisType("GGLAT", AxisType.Lat);

    if (!setAxisType("LONC", AxisType.Lon))
      if (!setAxisType("LON", AxisType.Lon))
        setAxisType("GGLON", AxisType.Lon);

    if (!setAxisType("PALT", AxisType.Height))
      setAxisType("GGALT", AxisType.Height);

    boolean hasTime = setAxisType("Time", AxisType.Time);
    if (!hasTime)
      hasTime = setAxisType("time", AxisType.Time);

    if (!hasTime) {
      rootGroup.findVariableLocal("time_offset").ifPresent(time -> {
        try {
          VariableDS.Builder<?> base = (VariableDS.Builder<?>) rootGroup.findVariableLocal("base_time")
              .orElseThrow(() -> new IllegalStateException("Cant find variable base_time"));
          int base_time = base.orgVar.readScalarInt();
          CalendarDateUnit cdunit = CalendarDateUnit.unixDateUnit;
          CalendarDate cdate = cdunit.makeCalendarDate(base_time);
          String time_units = "seconds since " + CalendarDateFormatter.toDateTimeStringISO(cdate);

          time.addAttribute(new Attribute(CDM.UNITS, time_units));
          time.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.name()));
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }

    // look for coordinates
    String coordinates = rootGroup.getAttributeContainer().findAttributeString("coordinates", null);
    if (coordinates != null) {
      for (String vname : StringUtil2.split(coordinates)) {
        rootGroup.findVariableLocal(vname).ifPresent(v -> {
          AxisType atype = getAxisType((VariableDS.Builder<?>) v);
          if (atype != null) {
            v.addAttribute(new Attribute(_Coordinate.AxisType, atype.name()));
          }
        });
      }
    }
  }

  private boolean setAxisType(String varName, AxisType atype) {
    if (rootGroup.findVariableLocal(varName).isEmpty()) {
      return false;
    }
    rootGroup.findVariableLocal(varName)
        .ifPresent(v -> v.addAttribute(new Attribute(_Coordinate.AxisType, atype.toString())));
    return true;
  }
}
