/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.www;

import java.io.PrintWriter;
import opendap.dap.DAS;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;

public class wwwArray extends DArray implements BrowserForm {

  /**
   * Constructs a new <code>asciiArray</code>.
   */
  public wwwArray() {
    this(null);
  }

  /**
   * Constructs a new <code>asciiArray</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public wwwArray(String n) {
    super(n);
  }

  public void printBrowserForm(PrintWriter pw, DAS das) {

    /*--------------------------------------------------------
    // C++ implementation looks like this...
    
    os << "<script type=\"text/javascript\">\n"
       << "<!--\n"
       << name_for_js_code(name())
       << " = new dods_var(\""
       << name()
       << "\", \""
       << name_for_js_code(name())
       << "\", 1);\n"
       << "DODS_URL.add_dods_var("
       << name_for_js_code(name())
       << ");\n"
       << "// -->\n"
       << "</script>\n";
    
    os << "<b>"
       << "<input type=\"checkbox\" name=\"get_"
       << name_for_js_code(name())
       << "\"\n"
       << "onclick=\""
       << name_for_js_code(name())
       << ".handle_projection_change(get_"
       << name_for_js_code(name())
       << ")\">\n"
       << "<font size=\"+1\">"
       << name()
       << "</font>"
       << ": "
       << fancy_typename(this)
       << "</b><br>\n\n";
    
    Pix p = first_dim();
    for (int i = 0; p; ++i, next_dim(p)) {
    int size = dimension_size(p, true);
    string n = dimension_name(p);
    if (n != "")
        os << n << ":";
    os << "<input type=\"text\" name=\""
       << name_for_js_code(name())
       << "_"
       << i
       << "\" size=8 onfocus=\"describe_index()\""
       << " onChange=\"DODS_URL.update_url()\">\n";
    
    os << "<script type=\"text/javascript\">\n"
       << "<!--\n"
       << name_for_js_code(name())
       << ".add_dim("
       << size
       << ");\n"
       << "// -->\n"
       << "</script>\n";
    }
    
    os << "<br>\n\n";
    
    --------------------------------------------------------*/


    pw.print("<script type=\"text/javascript\">\n" + "<!--\n" + wwwOutPut.nameForJsCode(getEncodedName())
        + " = new dods_var(\"" + getEncodedName() + "\", \"" + wwwOutPut.nameForJsCode(getEncodedName()) + "\", 1);\n"
        + "DODS_URL.add_dods_var(" + wwwOutPut.nameForJsCode(getEncodedName()) + ");\n" + "// -->\n" + "</script>\n");

    pw.print("<b>" + "<input type=\"checkbox\" name=\"get_" + wwwOutPut.nameForJsCode(getEncodedName()) + "\"\n"
        + "onclick=\"" + wwwOutPut.nameForJsCode(getEncodedName()) + ".handle_projection_change(get_"
        + wwwOutPut.nameForJsCode(getEncodedName()) + ")\">\n" + "<font size=\"+1\">" + getEncodedName() + "</font>"
        + ": " + dasTools.fancyTypeName(this) + "</b><br>\n\n");

    int dimSize;
    int i = 0;
    String dimName;
    for (DArrayDimension dad : getDimensions()) {
      dimSize = dad.getSize();
      dimName = dad.getEncodedName();

      if (dimName != null)
        pw.print(dimName + ":");

      pw.print("<input type=\"text\" name=\"" + wwwOutPut.nameForJsCode(getEncodedName()) + "_" + i
          + "\" size=8 onfocus=\"describe_index()\"" + " onChange=\"DODS_URL.update_url()\">\n");

      pw.print("<script type=\"text/javascript\">\n" + "<!--\n" + wwwOutPut.nameForJsCode(getEncodedName())
          + ".add_dim(" + dimSize + ");\n" + "// -->\n" + "</script>\n");

      i++;
    }


    pw.println("<br>\n");
  }


}


