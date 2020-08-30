/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.ascii;

import java.io.PrintWriter;
import opendap.dap.BaseType;
import opendap.dap.DGrid;

/**
 */
public class asciiGrid extends DGrid implements toASCII {

  private static boolean _Debug = false;


  /**
   * Constructs a new <code>asciiGrid</code>.
   */
  public asciiGrid() {
    this(null);
  }

  /**
   * Constructs a new <code>asciiGrid</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public asciiGrid(String n) {
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

    if (_Debug)
      System.out.println("asciiGrid.toASCII(" + addName + ",'" + rootName + "')  getName(): " + getEncodedName());

    if (rootName != null)
      rootName += "." + getEncodedName();
    else
      rootName = getEncodedName();

    boolean firstPass = true;
    for (BaseType bt : getVariables()) {
      toASCII ta = (toASCII) bt;

      if (!newLine && !firstPass)
        pw.print(", ");

      ta.toASCII(pw, addName, rootName, newLine);

      firstPass = false;
    }

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

    StringBuilder s = new StringBuilder();
    boolean firstPass = true;
    for (BaseType bt : getVariables()) {
      toASCII ta = (toASCII) bt;
      if (!firstPass)
        s.append(", ");
      s.append(ta.toASCIIFlatName(rootName));
      firstPass = false;
    }


    return (s.toString());
  }


}


