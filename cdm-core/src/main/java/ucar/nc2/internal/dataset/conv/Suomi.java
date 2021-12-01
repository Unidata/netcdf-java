/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.nc2.util.CancelTask;

/** Suomi coord sys builder. */
public class Suomi extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "Suomi";

  private Suomi(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      Variable v = ncfile.findVariable("time_offset");
      if (v == null || !v.isCoordinateVariable())
        return false;
      String desc = v.getDescription();
      if (desc == null || (!desc.equals("Time delta from start_time")
          && !desc.equals("PWV window midpoint time delta from start_time")))
        return false;

      if (null == ncfile.findAttribute("start_date"))
        return false;
      return null != ncfile.findAttribute("start_time");
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new Suomi(datasetBuilder);
    }
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    String start_date = rootGroup.getAttributeContainer().findAttributeString("start_date", null);
    if (start_date == null)
      return;

    CalendarDateFormatter formatter = new CalendarDateFormatter("yyyy.DDD.HH.mm.ss"); // "2006.105.00.00.00"
    CalendarDate start = formatter.parse(start_date);

    rootGroup.findVariableLocal("time_offset")
        .ifPresent(v -> v.addAttribute(new Attribute(CDM.UNITS, "seconds since " + start)));

    rootGroup.addAttribute(new Attribute(CDM.CONVENTIONS, "Suomi-Station-CDM"));
  }

  @Override
  protected AxisType getAxisType(VariableDS.Builder<?> v) {
    String name = v.shortName;
    if (name.equals("time_offset"))
      return AxisType.Time;
    if (name.equals("lat"))
      return AxisType.Lat;
    if (name.equals("lon"))
      return AxisType.Lon;
    if (name.equals("height"))
      return AxisType.Height;
    return null;
  }
}
