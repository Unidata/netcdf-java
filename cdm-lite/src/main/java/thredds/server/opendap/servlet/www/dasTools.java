/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servlet.www;

import opendap.dap.BaseType;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DByte;
import opendap.dap.DDS;
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
import opendap.dap.DURL;
import opendap.dap.NoSuchVariableException;

public class dasTools {


  /**
   * This code could use a real `kill-file' some day -
   * about the same time that the rest of the server gets
   * an `rc' file... For the present just return
   * false (There is no killing going on here...)
   * <p/>
   * The C++ implementation looks like this:
   * <p/>
   * static bool
   * name_in_kill_file(const string &name)
   * {
   * static Regex dim(".*_dim_[0-9]*", 1); // HDF `dimension' attributes.
   * <p/>
   * return dim.match(name.c_str(), name.length()) != -1;
   * }
   */
  public static boolean nameInKillFile(String name) {
    return (false);
  }


  public static boolean nameInDDS(String name, DDS dds) {

    boolean found = true;

    try {
      dds.getVariable(name);
    } catch (NoSuchVariableException e) {

      found = false;
    }

    // System.out.println("nameInDDS(): "+found);
    return (found);
  }


  /*
   * C++ implementation
   * static bool
   * name_is_global(string &name)
   * {
   * static Regex global("\\(.*global.*\\)\\|\\(.*opendap.*\\)", 1);
   * downcase(name);
   * return global.match(name.c_str(), name.length()) != -1;
   * }
   */
  public static boolean nameIsGlobal(String name) {

    String lcName = name.toLowerCase();
    boolean global = false;

    if (lcName.indexOf("global") >= 0)
      global = true;

    if (lcName.indexOf("dods") >= 0)
      global = true;

    // System.out.println("nameIsGlobal(): "+global);

    return (global);
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  public static String fancyTypeName(BaseType bt) {


    if (bt instanceof DByte)
      return ("8 bit Byte");

    if (bt instanceof DUInt16)
      return ("16 bit Unsigned Integer");

    if (bt instanceof DInt16)
      return ("16 bit Integer");

    if (bt instanceof DUInt32)
      return ("32 bit Unsigned Integer");

    if (bt instanceof DInt32)
      return ("32 bit Integer");

    if (bt instanceof DFloat32)
      return ("32 bit Real");

    if (bt instanceof DFloat64)
      return ("64 bit Real");

    if (bt instanceof DURL)
      return ("URL");

    if (bt instanceof DString)
      return ("String");


    if (bt instanceof DArray) {
      DArray a = (DArray) bt;
      StringBuilder type = new StringBuilder();
      type.append("Array of ");
      type.append(fancyTypeName(a.getPrimitiveVector().getTemplate()));
      type.append("s ");

      for (DArrayDimension dad : a.getDimensions()) {
        type.append("[");
        type.append(dad.getEncodedName());
        type.append(" = 0..");
        type.append(dad.getSize() - 1);
        type.append("]");

      }
      type.append("\n");
      return (type.toString());
    }


    if (bt instanceof DStructure)
      return ("Structure");

    if (bt instanceof DSequence)
      return ("Sequence");

    if (bt instanceof DGrid)
      return ("Grid");

    return ("UNKNOWN");


  }
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


}


