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


package opendap.servlet;

import java.nio.charset.StandardCharsets;
import java.io.*;
import ucar.nc2.constants.CDM;

/**
 * A minimal implementation of a logging facility.
 */

public class Logx {

  private static PrintWriter logger = null;
  private static ByteArrayOutputStream buff = null;

  public static void println(String s) {
    if (logger != null)
      logger.println(s);
  }

  public static void printDODSException(opendap.dap.DAP2Exception de) {
    if (logger != null) {
      de.print(logger);
      de.printStackTrace(logger);
    }
  }

  public static void printThrowable(Throwable t) {
    if (logger != null) {
      logger.println(t.getMessage());
      t.printStackTrace(logger);
    }
  }

  public static void reset() {
    buff = new ByteArrayOutputStream();
    logger = new PrintWriter(new OutputStreamWriter(buff, StandardCharsets.UTF_8));
  }

  public static boolean isOn() {
    return (logger != null);
  }

  public static void close() {
    logger = null;
    buff = null;
  }

  public static String getContents() {
    if (buff == null)
      return "null";
    else
      try {
        logger.flush();
        return buff.toString(CDM.UTF8);
      } catch (UnsupportedEncodingException nee) {
        throw new IllegalStateException(nee);
      }
  }

}

/**
 * $Log: Log.java,v $
 * Revision 1.1 2003/08/12 23:51:27 ndp
 * Mass check in to begin Java-OPeNDAP development work
 *
 * Revision 1.1 2002/09/24 18:32:35 caron
 * add Log.java
 *
 *
 */


