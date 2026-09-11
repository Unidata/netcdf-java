/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.StringJoiner;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.table.WmoParamTable;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.StringUtil2;

/**
 * Static utilities for Grib-2
 *
 * @author caron
 * @since 3/29/11
 */
public class Grib2Utils {

  public static String clean(String s) {
    StringBuilder sb = new StringBuilder(s);
    StringUtil2.replace(sb, "/. ", "-p_");
    StringUtil2.removeAll(sb, "(),;");
    char c = sb.charAt(0);
    if (Character.isLetter(c)) {
      if (Character.isLowerCase(c))
        sb.setCharAt(0, Character.toUpperCase(c));
    } else {
      sb.insert(0, 'N');
    }

    return sb.toString().trim();
  }

  public static String cleanupHeader(byte[] raw) {
    String result = StringUtil2.cleanup(raw);
    int pos = result.indexOf("data");
    if (pos > 0)
      result = result.substring(pos);
    return result;
  }

  public static String getVariableName(Grib2Record gr) {
    GribTables.Parameter p = WmoParamTable.getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(),
        gr.getPDS().getParameterNumber());
    String s = (p == null) ? null : p.getName();
    if (s == null)
      s = "U" + gr.getDiscipline() + "-" + gr.getPDS().getParameterCategory() + "-" + gr.getPDS().getParameterNumber();
    return s;
  }

  @Nullable
  public static CalendarPeriod getCalendarPeriod(int timeUnit) {

    switch (timeUnit) { // code table 4.4
      case 0:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Minute);
      case 1:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
      case 2:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Day);
      case 3:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Month);
      case 4:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Year);
      case 5:
        return CalendarPeriod.of(10, CalendarPeriod.Field.Year);
      case 6:
        return CalendarPeriod.of(30, CalendarPeriod.Field.Year);
      case 7:
        return CalendarPeriod.of(100, CalendarPeriod.Field.Year);
      case 10:
        return CalendarPeriod.of(3, CalendarPeriod.Field.Hour);
      case 11:
        return CalendarPeriod.of(6, CalendarPeriod.Field.Hour);
      case 12:
        return CalendarPeriod.of(12, CalendarPeriod.Field.Hour);
      case 13:
        return CalendarPeriod.of(1, CalendarPeriod.Field.Second);
      default:
        return null;
    }
  }

  /**
   * <pre>
   *   Code table 4.91 - Type of Interval
   *   https://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc/grib2_table4-91.shtml
   *
   *      0  Smaller than first limit
   *      1  Greater than second limit
   *      2  Between first and second limit. The range includes the first limit but not the second limit.
   *      3  Greater than first limit
   *      4  Smaller than second limit
   *      5  Smaller or equal first limit
   *      6  Greater or equal second limit
   *      7  Between first and second limit. The range includes the first limit and the second limit.
   *      8  Greater or equal first limit
   *      9  Smaller or equal second limit
   *     10  Between first and second limit. The range includes the second limit but not the first limit.
   *     11  Equal to first limit
   * </pre>
   * 
   * @return the relational operator applied to the first limit of the interval, or null if not applicable
   */
  @Nullable
  public static String getFirstLimitOperator(int intervalType) {
    switch (intervalType) {
      case 0:
        return "<";
      case 2:
      case 7:
      case 8:
        return ">=";
      case 3:
      case 10:
        return ">";
      case 5:
        return "<=";
      case 11:
        return ""; // No need to put "=" sign in front of the value. The value by itself is fine.
      default:
        return null;
    }
  }

  /**
   * See {@link #getFirstLimitOperator}.
   * 
   * @return the relational operator applied to the second limit of the interval, or null if not applicable
   */
  @Nullable
  public static String getSecondLimitOperator(int intervalType) {
    switch (intervalType) {
      case 1:
        return ">";
      case 2:
      case 4:
        return "<";
      case 6:
        return ">=";
      case 7:
      case 9:
      case 10:
        return "<=";
      default:
        return null;
    }
  }

  @Nullable
  private static String rangeTermFor(String operator, double val, String unit) {
    if (operator == null) {
      return null;
    }
    String formattedValue = (int) val == val ? Integer.toString((int) val) : Double.toString(val);
    return operator + formattedValue + unit;
  }

  /**
   * Constructs a human-readable representation of an interval. Whether the first or second limit (or both) is used
   * depends on the type of interval. Examples: ">2.5um", ">=2.5um,<10um"
   *
   * @param intervalType code from Code table 4.91
   * @param firstLimit first limit of the interval
   * @param secondLimit second limit of the interval
   * @param unit the unit of the limits, applied as a suffix
   */
  @Nonnull
  public static String intervalToRangeDescriptor(int intervalType, double firstLimit, double secondLimit, String unit) {
    StringJoiner sj = new StringJoiner(",");
    String firstTerm = rangeTermFor(getFirstLimitOperator(intervalType), firstLimit, unit);
    if (firstTerm != null) {
      sj.add(firstTerm);
    }
    String secondTerm = rangeTermFor(getSecondLimitOperator(intervalType), secondLimit, unit);
    if (secondTerm != null) {
      sj.add(secondTerm);
    }
    return sj.toString();
  }

  public static String makeAerosolRangeSuffix(String aerosolRange) {
    return aerosolRange.replace('.', 'p').replace("<=", "le_").replace(">=", "ge_").replace("<", "lt_")
        .replace(">", "gt_").replace(',', '_');
  }

  /**
   * Check to see if this pds is a layer variable
   * 
   * @param pds record to check
   * @return true if a layer
   */
  public static boolean isLayer(Grib2Pds pds) {
    return pds.getLevelType2() != 255 && pds.getLevelType2() != 0;
  }

  public static boolean isLatLon(int gridTemplate, int center) {
    return ((gridTemplate < 4) || ((gridTemplate >= 40) && (gridTemplate < 44)));
  }

  //////////////////////////////////////////////////////////////////////////////////
  // pretty much lame stuff
  // possibly move to Customizer

  // check if grid template is "Curvilinear Orthogonal", (NCEP 204) methods below only used when thats true
  public static boolean isCurvilinearOrthogonal(int gridTemplate, int center) {
    return ((center == 7) && (gridTemplate == 204));
  }

  // isLatLon2D is true, check parameter to see if its a 2D lat/lon coordinate
  @Nullable
  public static LatLon2DCoord getLatLon2DcoordType(int discipline, int category, int parameter) {
    if ((discipline != 0) || (category != 2) || (parameter < 198 || parameter > 203))
      return null;
    switch (parameter) {
      case 198:
        return LatLon2DCoord.U_Latitude;
      case 199:
        return LatLon2DCoord.U_Longitude;
      case 200:
        return LatLon2DCoord.V_Latitude;
      case 201:
        return LatLon2DCoord.V_Longitude;
      case 202:
        return LatLon2DCoord.P_Latitude;
      case 203:
        return LatLon2DCoord.P_Longitude;
    }
    return null;
  }

  public enum LatLonCoordType {
    U, V, P
  }
  public enum LatLon2DCoord {
    U_Latitude, U_Longitude, V_Latitude, V_Longitude, P_Latitude, P_Longitude;

    public AxisType getAxisType() {
      return this.name().contains("Latitude") ? AxisType.Lat : AxisType.Lon;
    }
  }

  /**
   * This looks for snippets in the variable name/desc as to whether it wants U, V, or P 2D coordinates
   * 
   * @param desc variable name/desc
   * @return U, V, or P for normal variables, null for the coordinates themselves
   */
  public static LatLonCoordType getLatLon2DcoordType(String desc) {
    LatLonCoordType type;
    if (desc.contains("u-component"))
      type = LatLonCoordType.U;
    else if (desc.contains("v-component"))
      type = LatLonCoordType.V;
    else if (desc.contains("Latitude of") || desc.contains("Longitude of"))
      type = null;
    else
      type = LatLonCoordType.P;
    return type;
  }

}
