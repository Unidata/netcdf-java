/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import opendap.dap.DByte;
import opendap.dap.DFloat32;
import opendap.dap.DFloat64;
import opendap.dap.DGrid;
import opendap.dap.DInt16;
import opendap.dap.DInt32;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DStructure;
import opendap.dap.DUInt16;
import opendap.dap.DUInt32;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

/** Static utilities for opendap. */
public class DodsNetcdfFiles {
  static boolean debugCE = false;
  static boolean debugServerCall = false;
  static boolean debugOpenResult = false;
  static boolean debugDataResult = false;
  static boolean debugCharArray = false;
  static boolean debugConvertData = false;
  static boolean debugConstruct = false;
  static boolean debugPreload = false;
  static boolean debugTime = false;
  static boolean showNCfile = false;
  static boolean debugAttributes = false;
  static boolean debugCached = false;
  static boolean debugOpenTime = false;

  /** Debugging */
  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugCE = debugFlag.isSet("DODS/constraintExpression");
    debugServerCall = debugFlag.isSet("DODS/serverCall");
    debugOpenResult = debugFlag.isSet("DODS/debugOpenResult");
    debugDataResult = debugFlag.isSet("DODS/debugDataResult");
    debugCharArray = debugFlag.isSet("DODS/charArray");
    debugConstruct = debugFlag.isSet("DODS/constructNetcdf");
    debugPreload = debugFlag.isSet("DODS/preload");
    debugTime = debugFlag.isSet("DODS/timeCalls");
    showNCfile = debugFlag.isSet("DODS/showNCfile");
    debugAttributes = debugFlag.isSet("DODS/attributes");
    debugCached = debugFlag.isSet("DODS/cache");
  }

  /**
   * Create the canonical form of the URL.
   * If the urlName starts with "http:" or "https:", change it to start with "dods:", otherwise
   * leave it alone.
   *
   * @param urlName the url string
   * @return canonical form
   */
  public static String canonicalURL(String urlName) {
    if (urlName.startsWith("http:"))
      return "dods:" + urlName.substring(5);
    if (urlName.startsWith("https:"))
      return "dods:" + urlName.substring(6);
    return urlName;
  }

  /**
   * Return a variable name suitable for use in a DAP constraint expression.
   * [Original code seemed wrong because structures can be nested and hence
   * would have to use the full name just like non-structures]
   *
   * @param var The variable whose name will appear in the CE
   * @return The name in a form suitable for use in a cE
   */
  static String getDODSConstraintName(Variable var) {
    String vname = ((DODSNode) var).getDODSName();
    // The vname is backslash escaped, so we need to
    // modify to use DAP %xx escapes.
    return EscapeStringsDap.backslashToDAP(vname);
  }

  // full name

  public static String makeDODSname(DodsV dodsV) {
    DodsV parent = dodsV.parent;
    if (parent.bt != null)
      return (makeDODSname(parent) + "." + dodsV.bt.getEncodedName());
    return dodsV.bt.getEncodedName();
  }

  public static String makeShortName(String name) {
    String unescaped = EscapeStringsDap.unescapeDAPIdentifier(name);
    int index = unescaped.lastIndexOf('/');
    if (index < 0)
      index = -1;
    return unescaped.substring(index + 1, unescaped.length());
  }

  public static String makeDODSName(String name) {
    return EscapeStringsDap.unescapeDAPIdentifier(name);
  }

  /**
   * Get the DODS data class corresponding to the Netcdf data type.
   * This is the inverse of convertToNCType().
   *
   * @param dataType Netcdf data type.
   * @return the corresponding DODS type enum, from opendap.dap.Attribute.XXXX.
   */
  public static int convertToDODSType(DataType dataType) {
    if (dataType == DataType.STRING)
      return opendap.dap.Attribute.STRING;
    if (dataType == DataType.BYTE)
      return opendap.dap.Attribute.BYTE;
    if (dataType == DataType.FLOAT)
      return opendap.dap.Attribute.FLOAT32;
    if (dataType == DataType.DOUBLE)
      return opendap.dap.Attribute.FLOAT64;
    if (dataType == DataType.SHORT)
      return opendap.dap.Attribute.INT16;
    if (dataType == DataType.USHORT)
      return opendap.dap.Attribute.UINT16;
    if (dataType == DataType.INT)
      return opendap.dap.Attribute.INT32;
    if (dataType == DataType.UINT)
      return opendap.dap.Attribute.UINT32;
    if (dataType == DataType.BOOLEAN)
      return opendap.dap.Attribute.BYTE;
    if (dataType == DataType.LONG)
      return opendap.dap.Attribute.INT32; // LOOK no LONG type!

    // shouldnt happen
    return opendap.dap.Attribute.STRING;
  }

  /**
   * Get the Netcdf data type corresponding to the DODS data type.
   * This is the inverse of convertToDODSType().
   *
   * @param dodsDataType DODS type enum, from dods.dap.Attribute.XXXX.
   * @return the corresponding netcdf DataType.
   * @see #isUnsigned
   */
  public static DataType convertToNCType(int dodsDataType, boolean isUnsigned) {
    switch (dodsDataType) {
      case opendap.dap.Attribute.BYTE:
        return isUnsigned ? DataType.UBYTE : DataType.BYTE;
      case opendap.dap.Attribute.FLOAT32:
        return DataType.FLOAT;
      case opendap.dap.Attribute.FLOAT64:
        return DataType.DOUBLE;
      case opendap.dap.Attribute.INT16:
        return DataType.SHORT;
      case opendap.dap.Attribute.UINT16:
        return DataType.USHORT;
      case opendap.dap.Attribute.INT32:
        return DataType.INT;
      case opendap.dap.Attribute.UINT32:
        return DataType.UINT;
      default:
        return DataType.STRING;
    }
  }

  /**
   * Get the Netcdf data type corresponding to the DODS BaseType class.
   * This is the inverse of convertToDODSType().
   *
   * @param dtype DODS BaseType.
   * @return the corresponding netcdf DataType.
   * @see #isUnsigned
   */
  public static DataType convertToNCType(opendap.dap.BaseType dtype, boolean isUnsigned) {
    if (dtype instanceof DString)
      return DataType.STRING;
    else if ((dtype instanceof DStructure) || (dtype instanceof DSequence) || (dtype instanceof DGrid))
      return DataType.STRUCTURE;
    else if (dtype instanceof DFloat32)
      return DataType.FLOAT;
    else if (dtype instanceof DFloat64)
      return DataType.DOUBLE;
    else if (dtype instanceof DUInt32)
      return DataType.UINT;
    else if (dtype instanceof DUInt16)
      return DataType.USHORT;
    else if (dtype instanceof DInt32)
      return DataType.INT;
    else if (dtype instanceof DInt16)
      return DataType.SHORT;
    else if (dtype instanceof DByte)
      return isUnsigned ? DataType.UBYTE : DataType.BYTE;
    else
      throw new IllegalArgumentException("DODSVariable illegal type = " + dtype.getTypeName());
  }

  /**
   * Get whether this is an unsigned type.
   *
   * @param dtype DODS BaseType.
   * @return true if unsigned
   */
  public static boolean isUnsigned(opendap.dap.BaseType dtype) {
    return (dtype instanceof DByte) || (dtype instanceof DUInt16) || (dtype instanceof DUInt32);
  }

}
