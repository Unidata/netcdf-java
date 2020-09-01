/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.www;

import java.io.PrintWriter;
import opendap.dap.DAS;
import opendap.dap.DFloat64;

/**
 */
public class wwwF64 extends DFloat64 implements BrowserForm {

  private static boolean _Debug = false;

  /**
   * Constructs a new <code>asciiF64</code>.
   */
  public wwwF64() {
    this(null);
  }

  /**
   * Constructs a new <code>asciiF64</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public wwwF64(String n) {
    super(n);
  }

  public void printBrowserForm(PrintWriter pw, DAS das) {
    wwwOutPut wOut = new wwwOutPut(pw);
    wOut.writeSimpleVar(pw, this);
  }


}


