/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.www;

import java.io.PrintWriter;
import opendap.dap.BaseType;
import opendap.dap.DAS;
import opendap.dap.DStructure;

/**
 */
public class wwwStructure extends DStructure implements BrowserForm {


  private static boolean _Debug = false;

  /**
   * Constructs a new <code>wwwStructure</code>.
   */
  public wwwStructure() {
    this(null);
  }

  /**
   * Constructs a new <code>wwwStructure</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public wwwStructure(String n) {
    super(n);
  }

  public void printBrowserForm(PrintWriter pw, DAS das) {

    /*-----------------------------------------------
    // C++ Implementation looks like this....
    
    os << "<b>Structure " << name() << "</b><br>\n";
    os << "<dl><dd>\n";
    
    for (Pix p = first_var(); p; next_var(p)) {
    var(p)->print_val(os, "", print_decls);
    wo.write_variable_attributes(var(p), global_das);
    os << "<p><p>\n";
    }
    os << "</dd></dl>\n";
    
    ------------------------------------------------*/

    wwwOutPut wOut = new wwwOutPut(pw);

    pw.println("<b>Structure " + getEncodedName() + "</b><br>");
    pw.println("<dl><dd>");

    for (BaseType bt : getVariables()) {
      ((BrowserForm) bt).printBrowserForm(pw, das);

      wOut.writeVariableAttributes(bt, das);
      pw.print("<p><p>\n");
    }
    pw.println("</dd></dl>");

  }


}


