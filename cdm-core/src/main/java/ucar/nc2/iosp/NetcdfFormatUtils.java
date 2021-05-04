/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import ucar.array.ArrayType;
import ucar.ma2.DataType;

/** Utilities and Constants specific to the Netcdf file format (Netcdf-3 and Netcdf-4) */
public class NetcdfFormatUtils {
  // Default fill values, used unless _FillValue variable attribute is set.
  public static final byte NC_FILL_BYTE = -127;
  public static final char NC_FILL_CHAR = (char) 0;
  public static final short NC_FILL_SHORT = (short) -32767;
  public static final int NC_FILL_INT = -2147483647;
  public static final float NC_FILL_FLOAT = 9.9692099683868690e+36f; /* near 15 * 2^119 */
  public static final double NC_FILL_DOUBLE = 9.9692099683868690e+36;

  public static final byte NC_FILL_UBYTE = (byte) 255;
  public static final short NC_FILL_USHORT = (short) 65535;
  public static final int NC_FILL_UINT = (int) 4294967295L;
  public static final long NC_FILL_INT64 = -9223372036854775806L; // 0x8000000000000002. Only bits 63 and 1 set.

  // We want to use 18446744073709551614ULL here (see https://goo.gl/buBal9), but that's too big to fit into a
  // signed long (Java doesn't have unsigned types). So, assign the hex string that WOULD correspond to that value
  // *if it were treated as unsigned*. Java will treat it as signed and see "-2", but we don't much care.
  public static final long NC_FILL_UINT64 = 0xfffffffffffffffeL;

  public static final String NC_FILL_STRING = "";

  //// Special netcdf-4 specific stuff. used by both Java (H5header) and JNA interface (Nc4Iosp)
  // @see "https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_4_spec.html"

  // only on the multi-dimensional coordinate variables of the netCDF model (2D chars).
  // appears to hold the dimension ids of the 2 dimensions.
  public static final String NETCDF4_COORDINATES = "_Netcdf4Coordinates";

  // on dimension scales, holds a scalar H5T_NATIVE_INT,
  // which is the (zero-based) dimension ID for this dimension. used to maintain creation order
  public static final String NETCDF4_DIMID = "_Netcdf4Dimid";

  // global - when using classic model
  public static final String NETCDF4_STRICT = "_nc3_strict";

  // appended to variable when it conflicts with dimension scale
  public static final String NETCDF4_NON_COORD = "_nc4_non_coord_";

  /** @deprecated use getFillValueDefault(ArrayType) */
  @Deprecated
  public static Number getFillValueDefault(DataType dtype) {
    return getFillValueDefault(dtype.getArrayType());
  }

  public static Number getFillValueDefault(ArrayType dtype) {
    if ((dtype == ArrayType.BYTE) || (dtype == ArrayType.ENUM1))
      return NC_FILL_BYTE;
    if (dtype == ArrayType.UBYTE)
      return NC_FILL_UBYTE;
    if (dtype == ArrayType.CHAR)
      return (byte) 0;
    if ((dtype == ArrayType.SHORT) || (dtype == ArrayType.ENUM2))
      return NC_FILL_SHORT;
    if (dtype == ArrayType.USHORT)
      return NC_FILL_USHORT;
    if ((dtype == ArrayType.INT) || (dtype == ArrayType.ENUM4))
      return NC_FILL_INT;
    if (dtype == ArrayType.UINT)
      return NC_FILL_UINT;
    if (dtype == ArrayType.LONG)
      return NC_FILL_INT64;
    if (dtype == ArrayType.ULONG)
      return NC_FILL_UINT64;
    if (dtype == ArrayType.FLOAT)
      return NC_FILL_FLOAT;
    if (dtype == ArrayType.DOUBLE)
      return NC_FILL_DOUBLE;
    return null;
  }

  /**
   * Determine if the given name can be used for a NetCDF object, i.e. a Dimension, Attribute, or Variable.
   * The allowed name syntax (in RE form) is:
   *
   * <pre>
   * ([a-zA-Z0-9_]|{UTF8})([^\x00-\x1F\x7F/]|{UTF8})*
   * </pre>
   *
   * where UTF8 represents a multi-byte UTF-8 encoding. Also, no trailing spaces are permitted in names. We do not
   * allow '/' because HDF5 does not permit slashes in names as slash is used as a group separator. If UTF-8 is
   * supported, then a multi-byte UTF-8 character can occur anywhere within an identifier.
   *
   * @param name the name to validate.
   * @return {@code true} if the name is valid.
   */
  // Implements "int NC_check_name(const char *name)" from NetCDF-C:
  // https://github.com/Unidata/netcdf-c/blob/v4.3.3.1/libdispatch/dstring.c#L169
  // Should match makeValidNetcdfObjectName()
  public static boolean isValidNetcdfObjectName(String name) {
    if (name == null || name.isEmpty()) { // Null and empty names disallowed
      return false;
    }

    int cp = name.codePointAt(0);

    // First char must be [a-z][A-Z][0-9]_ | UTF8
    if (cp <= 0x7f) {
      if (!('A' <= cp && cp <= 'Z') && !('a' <= cp && cp <= 'z') && !('0' <= cp && cp <= '9') && cp != '_') {
        return false;
      }
    }

    for (int i = 1; i < name.length(); ++i) {
      cp = name.codePointAt(i);

      // handle simple 0x00-0x7f characters here
      if (cp <= 0x7f) {
        if (cp < ' ' || cp > 0x7E || cp == '/') { // control char, DEL, or forward-slash
          return false;
        }
      }
    }

    // trailing spaces disallowed
    return cp > 0x7f || !Character.isWhitespace(cp);
  }

  /**
   * Convert a name to a legal netcdf-3 name.
   *
   * @param name the name to convert.
   * @return the converted name.
   * @see #isValidNetcdfObjectName(String)
   */
  public static String makeValidNetcdfObjectName(String name) {
    StringBuilder sb = new StringBuilder(name);

    while (sb.length() > 0) {
      int cp = sb.codePointAt(0);

      // First char must be [a-z][A-Z][0-9]_ | UTF8
      if (cp <= 0x7f) {
        if (!('A' <= cp && cp <= 'Z') && !('a' <= cp && cp <= 'z') && !('0' <= cp && cp <= '9') && cp != '_') {
          sb.deleteCharAt(0);
          continue;
        }
      }
      break;
    }

    for (int pos = 1; pos < sb.length(); ++pos) {
      int cp = sb.codePointAt(pos);

      // handle simple 0x00-0x7F characters here
      if (cp <= 0x7F) {
        if (cp < ' ' || cp > 0x7E || cp == '/') { // control char, DEL, or forward-slash
          sb.deleteCharAt(pos);
          --pos;
        }
      }
    }

    while (sb.length() > 0) {
      int cp = sb.codePointAt(sb.length() - 1);

      if (cp <= 0x7f && Character.isWhitespace(cp)) {
        sb.deleteCharAt(sb.length() - 1);
      } else {
        break;
      }
    }

    if (sb.length() == 0) {
      throw new IllegalArgumentException(String.format("Illegal NetCDF object name: '%s'", name));
    }

    return sb.toString();
  }

}
