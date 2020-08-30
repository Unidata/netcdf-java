/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servlet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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


