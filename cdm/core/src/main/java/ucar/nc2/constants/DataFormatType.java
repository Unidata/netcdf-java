/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.constants;

import javax.annotation.Nullable;

/**
 * Redo thredds.catalog.DataFormatType as enum in order to break dependency of ucar.nc2 on server catalog.
 * LOOK could use standard Mime type ??
 */
public enum DataFormatType {
  BUFR(null), //
  ESML(null), //
  GEMPAK(null), //
  GINI(null), //
  GRIB1("GRIB-1"), //
  GRIB2("GRIB-2"), //
  HDF4(null), //
  HDF5(null), //
  MCIDAS_AREA("McIDAS-AREA"), //
  NCML("NcML"), //
  NETCDF("NetCDF-3"), //
  NETCDF4("NetCDF-4"), //
  NEXRAD2(null), //
  NIDS(null), //
  //
  GIF("image/gif"), //
  JPEG("image/jpeg"), //
  TIFF("image/tiff"), //
  //
  CSV("text/csv"), //
  HTML("text/html"), //
  PLAIN("text/plain"), //
  TSV("text/tab-separated-values"), //
  XML("text/xml"), //
  //
  MPEG("video/mpeg"), //
  QUICKTIME("video/quicktime"), //
  REALTIME("video/realtime"); //

  private final String desc;

  DataFormatType(String desc) {
    this.desc = (desc == null) ? toString() : desc;
  }

  /** case insensitive name lookup. */
  @Nullable
  public static DataFormatType getType(String name) {
    if (name == null)
      return null;
    for (DataFormatType m : values()) {
      if (m.desc.equalsIgnoreCase(name))
        return m;
      if (m.toString().equalsIgnoreCase(name))
        return m;
    }
    return null;
  }

  @Nullable
  public String getDescription() {
    return desc;
  }
}
