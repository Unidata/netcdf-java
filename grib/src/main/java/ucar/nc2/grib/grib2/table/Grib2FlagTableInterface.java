package ucar.nc2.grib.grib2.table;

import java.util.List;

public interface Grib2FlagTableInterface {
  String getName();

  String getShortName();

  List<Entry> getEntries();

  /**
   * Find the Entry in this table with the given code.
   * 
   * @param code unsigned short.
   */
  Grib2FlagTableInterface.Entry getEntry(int code);

  interface Entry {
    /** Unsigned short */
    int getCode();

    List<Integer> getValues();

    String getName(int value);
  }
}
