/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

/**
 * Thrown when a <code>RelOp</code> operation is called
 * on two types for which it makes no sense to compre, such as
 * attempting to ascertain is a String is less than a Float.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class InvalidOperatorException extends DAP2ServerSideException {
  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public InvalidOperatorException(String s) {
    super(MALFORMED_EXPR, "Invalid Operator Exception: " + s);
  }


  /**
   * Construct a <code>InvalidOperatorException</code> with the specified
   * message and OPeNDAP error code (see <code>DAP2Exception</code>).
   *
   * @param err the OPeNDAP error code.
   * @param s the detail message.
   */
  public InvalidOperatorException(int err, String s) {
    super(err, s);
  }
}


