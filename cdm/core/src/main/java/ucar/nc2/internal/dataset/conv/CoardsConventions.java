package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import javax.annotation.Nullable;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.time.Calendar;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;

/**
 * COARDS Convention. see http://ferret.wrc.noaa.gov/noaa_coop/coop_cdf_profile.html
 * Obsolete, do not use for new data.
 */
class CoardsConventions extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "COARDS";

  public static class Factory implements CoordSystemBuilderFactory {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new CoardsConventions(datasetBuilder);
    }
  }

  /*
   * The COARDS standard offers limited support for climatological time. For compatibility with COARDS, time coordinates
   * should also be
   * recognised as climatological if they have a units attribute of time-units relative to midnight on 1 January in year
   * 0
   * i.e. since 0-1-1 in udunits syntax , and provided they refer to the real-world calendar. We do not recommend this
   * convention because
   * (a) it does not provide any information about the intervals used to compute the climatology, and
   * (b) there is no standard for how dates since year 1 will be encoded with units having a reference time in year 0,
   * since this year does not exist; consequently there may be inconsistencies among software packages in the
   * interpretation of the
   * time coordinates. Year 0 may be a valid year in non-real-world calendars, and therefore cannot be used to signal
   * climatological
   * time in such cases.
   */

  CoardsConventions(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  boolean checkTimeVarForCalendar(VariableDS.Builder<?> vb) {
    boolean hasChanged = false;
    String unit = vb.getUnits();
    if (unit != null) {
      unit = unit.trim();
      if (SimpleUnit.isDateUnit(unit)) {
        Attribute calAttr = vb.getAttributeContainer().findAttributeIgnoreCase(CF.CALENDAR);
        if (calAttr == null) {
          calAttr = new Attribute(CF.CALENDAR, Calendar.gregorian.toString());
          vb.addAttribute(calAttr);
          hasChanged = true;
        }
      }
    }
    return hasChanged;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    for (Variable.Builder<?> vb : rootGroup.vbuilders) {
      if (vb instanceof VariableDS.Builder) {
        checkTimeVarForCalendar((VariableDS.Builder<?>) vb);
      }
    }
  }

  protected boolean checkForMeter = true;

  // we assume that coordinate axes get identified by being coordinate variables
  @Override
  @Nullable
  protected AxisType getAxisType(VariableDS.Builder<?> vb) {
    String unit = vb.getUnits();
    if (unit == null) {
      return null;
    }
    unit = unit.trim();

    if (unit.equalsIgnoreCase("degrees_east") || unit.equalsIgnoreCase("degrees_E") || unit.equalsIgnoreCase("degreesE")
        || unit.equalsIgnoreCase("degree_east") || unit.equalsIgnoreCase("degree_E")
        || unit.equalsIgnoreCase("degreeE")) {
      return AxisType.Lon;
    }

    if (unit.equalsIgnoreCase("degrees_north") || unit.equalsIgnoreCase("degrees_N")
        || unit.equalsIgnoreCase("degreesN") || unit.equalsIgnoreCase("degree_north")
        || unit.equalsIgnoreCase("degree_N") || unit.equalsIgnoreCase("degreeN")) {
      return AxisType.Lat;
    }

    if (SimpleUnit.isDateUnit(unit)) {
      return AxisType.Time;
    }
    // look for other z coordinate
    if (SimpleUnit.isCompatible("mbar", unit)) {
      return AxisType.Pressure;
    }
    if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level")) {
      return AxisType.GeoZ;
    }

    String positive = vb.getAttributeContainer().findAttributeString(CF.POSITIVE, null);
    if (positive != null) {
      if (SimpleUnit.isCompatible("m", unit)) {
        return AxisType.Height;
      } else {
        return AxisType.GeoZ;
      }
    }

    // a bad idea, but CDC SST relies on it :
    // :Source = "NOAA/National Climatic Data Center";
    // :Contact = "Dick Reynolds, email: Richard.W.Reynolds@noaa.gov & Chunying Liu, email: Chunying.liu@noaa.gov";
    // :netcdf_Convention = "COARDS";
    // if (checkForMeter && SimpleUnit.isCompatible("m", unit))
    // return AxisType.Height;

    return null;
  }

}


