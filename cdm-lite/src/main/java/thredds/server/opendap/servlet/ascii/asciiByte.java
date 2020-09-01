/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.ascii;

import java.io.PrintWriter;
import opendap.dap.DByte;

/**
 */
public class asciiByte extends DByte implements toASCII {

  private static boolean _Debug = false;

  /**
   * Constructs a new <code>asciiByte</code>.
   */
  public asciiByte() {
    this(null);
  }

  /**
   * Constructs a new <code>asciiByte</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public asciiByte(String n) {
    super(n);
  }


  /**
   * Returns a string representation of the variables value. This
   * is really foreshadowing functionality for Server types, but
   * as it may come in useful for clients it is added here. Simple
   * types (example: DFloat32) will return a single value. DConstuctor
   * and DVector types will be flattened. DStrings and DURL's will
   * have double quotes around them.
   *
   * @param addName is a flag indicating if the variable name should
   *        appear at the begining of the returned string.
   */
  public void toASCII(PrintWriter pw, boolean addName, String rootName, boolean newLine) {


    if (addName)
      pw.print(", ");

    pw.print((new Byte(getValue())).toString());

    if (newLine)
      pw.print("\n");

  }

  public String toASCIIAddRootName(PrintWriter pw, boolean addName, String rootName) {

    if (addName) {
      rootName = toASCIIFlatName(rootName);
      pw.print(rootName);
    }
    return (rootName);

  }

  public String toASCIIFlatName(String rootName) {
    String s;
    if (rootName != null) {
      s = rootName + "." + getEncodedName();
    } else {
      s = getEncodedName();
    }
    return (s);
  }


}


