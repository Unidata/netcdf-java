/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
//
// Author: James Gallagher <jgallagher@opendap.org>
//
// All rights reserved.
//
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
//
// - Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// - Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// - Neither the name of the OPeNDAP nor the names of its contributors may
// be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////



package opendap.servlet.www;

import java.io.*;
import java.util.Enumeration;
import opendap.dap.*;

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

    Enumeration e = getVariables();
    while (e.hasMoreElements()) {
      BaseType bt = (BaseType) e.nextElement();

      ((BrowserForm) bt).printBrowserForm(pw, das);

      wOut.writeVariableAttributes(bt, das);
      pw.print("<p><p>\n");

    }
    pw.println("</dd></dl>\n");

  }


}


