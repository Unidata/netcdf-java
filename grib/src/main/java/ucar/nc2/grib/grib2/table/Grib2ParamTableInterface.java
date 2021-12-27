package ucar.nc2.grib.grib2.table;

import javax.annotation.Nullable;
import ucar.nc2.grib.GribTables;

import java.util.List;

public interface Grib2ParamTableInterface {
  String getName();

  String getShortName();

  List<GribTables.Parameter> getParameters();

  /**
   * Find the Parameter in this table with the given number.
   * 
   * @param number unsigned byte.
   */
  @Nullable
  GribTables.Parameter getParameter(int number);
}
