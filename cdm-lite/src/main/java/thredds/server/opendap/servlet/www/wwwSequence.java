/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.www;

import java.io.PrintWriter;
import opendap.dap.BaseType;
import opendap.dap.DAS;
import opendap.dap.DSequence;

/**
 */
public class wwwSequence extends DSequence implements BrowserForm {

  private static boolean _Debug = false;

  /**
   * Constructs a new <code>wwwSeq</code>.
   */
  public wwwSequence() {
    this(null);
  }

  /**
   * Constructs a new <code>wwwSeq</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public wwwSequence(String n) {
    super(n);
  }

  public void printBrowserForm(PrintWriter pw, DAS das) {

    /*-----------------------------------------------------
    // C++ implementation looks like this...
    
    os << "<b>Sequence " << name() << "</b><br>\n";
    os << "<dl><dd>\n";
    
    for (Pix p = first_var(); p; next_var(p)) {
        var(p)->print_val(os, "", print_decls);
        wo.write_variable_attributes(var(p), global_das);
        os << "<p><p>\n";
    }
    
    os << "</dd></dl>\n";
    -----------------------------------------------------*/

    pw.print("<b>Sequence " + getEncodedName() + "</b><br>\n" + "<dl><dd>\n");

    wwwOutPut wOut = new wwwOutPut(pw);

    for (BaseType bt : getVariables()) {
      ((BrowserForm) bt).printBrowserForm(pw, das);

      wOut.writeVariableAttributes(bt, das);
      pw.print("<p><p>\n");

    }
    pw.println("</dd></dl>\n");

  }


}


