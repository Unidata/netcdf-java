/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servers;

/**
 * Thrown when a Server Side Function (SSF) is used incorrectly.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class SSFunctionException extends DAP2ServerSideException {
  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public SSFunctionException(String s) {
    super(MALFORMED_EXPR, "SeverSideFunction Exception: " + s);
  }


  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * message and OPeNDAP error code (see <code>DAP2Exception</code>).
   *
   * @param err the OPeNDAP error code.
   * @param s the detail message.
   */
  public SSFunctionException(int err, String s) {
    super(err, s);
  }
}


