/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

/**
 * Zarr object names as String constants
 */
public final class ZarrKeys {

  // object names
  public static final String ZARRAY = ".zarray";
  public static final String ZATTRS = ".zattrs";
  public static final String ZGROUP = ".zgroup";

  // key names
  public static final String SHAPE = "shape";
  public static final String CHUNKS = "chunks";
  public static final String DTYPE = "dtype";
  public static final String COMPRESSOR = "compressor";
  public static final String FILL_VALUE = "fill_value";
  public static final String ORDER = "order";
  public static final String FILTERS = "filters";
  public static final String DIMENSION_SEPARATOR = "dimension_separator";

}
