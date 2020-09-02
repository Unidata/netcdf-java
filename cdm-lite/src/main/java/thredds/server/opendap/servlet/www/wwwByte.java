/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.www;

import java.io.PrintWriter;
import opendap.dap.DAS;
import opendap.dap.DByte;

/**
 */
public class wwwByte extends DByte implements BrowserForm {

  private static boolean _Debug = false;

  /**
   * Constructs a new <code>asciiByte</code>.
   */
  public wwwByte() {
    this(null);
  }

  /**
   * Constructs a new <code>asciiByte</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public wwwByte(String n) {
    super(n);
  }

  public void printBrowserForm(PrintWriter pw, DAS das) {
    wwwOutPut wOut = new wwwOutPut(pw);
    wOut.writeSimpleVar(pw, this);
  }

}


