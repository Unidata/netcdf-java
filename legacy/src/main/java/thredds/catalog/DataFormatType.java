/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

/**
 * Type-safe enumeration of THREDDS data format types.
 *
 * @author john caron
 */

public final class DataFormatType {
  private static java.util.List<DataFormatType> members = new java.util.ArrayList<DataFormatType>(20);

  public static final DataFormatType NONE = new DataFormatType("");

  public static final DataFormatType BUFR = new DataFormatType("BUFR");
  public static final DataFormatType ESML = new DataFormatType("ESML");
  public static final DataFormatType GEMPAK = new DataFormatType("GEMPAK");
  public static final DataFormatType GINI = new DataFormatType("GINI");
  public static final DataFormatType GRIB1 = new DataFormatType("GRIB-1");
  public static final DataFormatType GRIB2 = new DataFormatType("GRIB-2");
  public static final DataFormatType HDF4 = new DataFormatType("HDF4");
  public static final DataFormatType HDF5 = new DataFormatType("HDF5");
  public static final DataFormatType NETCDF = new DataFormatType("netCDF");
  public static final DataFormatType NETCDF4 = new DataFormatType("netCDF-4");
  public static final DataFormatType NEXRAD2 = new DataFormatType("NEXRAD-2");
  public static final DataFormatType NCML = new DataFormatType("NcML");
  public static final DataFormatType NIDS = new DataFormatType("NEXRAD-3");
  public static final DataFormatType MCIDAS_AREA = new DataFormatType("McIDAS-AREA");

  public static final DataFormatType GIF = new DataFormatType("image/gif");
  public static final DataFormatType JPEG = new DataFormatType("image/jpeg");
  public static final DataFormatType TIFF = new DataFormatType("image/tiff");

  public static final DataFormatType PLAIN = new DataFormatType("text/plain");
  public static final DataFormatType TSV = new DataFormatType("text/tab-separated-values");
  public static final DataFormatType XML = new DataFormatType("text/xml");

  public static final DataFormatType MPEG = new DataFormatType("video/mpeg");
  public static final DataFormatType QUICKTIME = new DataFormatType("video/quicktime");
  public static final DataFormatType REALTIME = new DataFormatType("video/realtime");
  public static final DataFormatType OTHER_UNKNOWN = new DataFormatType("other/unknown");

  private String name;

  private DataFormatType(String s) {
    this.name = s;
    members.add(this);
  }

  private DataFormatType(String s, boolean fake) {
    this.name = s;
  }

  /**
   * Return all DataFormatType objects
   *
   * @return Collection of known DataFormatType-s
   */
  public static java.util.Collection<DataFormatType> getAllTypes() {
    return members;
  }

  /**
   * Find the known DataFormatType that matches the given name (ignoring case)
   * or null if the name is unknown.
   *
   * @param name name of the desired DataFormatType.
   * @return DataFormatType or null if no match.
   */
  public static DataFormatType findType(String name) {
    if (name == null)
      return null;
    for (DataFormatType m : members) {
      if (m.name.equalsIgnoreCase(name))
        return m;
    }
    return null;
  }

  /**
   * Return a DataFormatType for the given name by either matching
   * a known type (ignoring case) or creating an unknown type.
   *
   * @param name name of the desired DataFormatType.
   * @return the named DataFormatType or null if given name is null.
   */
  public static DataFormatType getType(String name) {
    if (name == null)
      return null;
    DataFormatType t = findType(name);
    return t != null ? t : new DataFormatType(name, false);
  }

  /**
   * Return the DataFormatType name.
   */
  public String toString() {
    return name;
  }

  /** Override Object.hashCode() to be consistent with this equals. */
  public int hashCode() {
    return name.hashCode();
  }

  /** DataFormatType with same name are equal. */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DataFormatType))
      return false;
    return o.hashCode() == this.hashCode();
  }

}
