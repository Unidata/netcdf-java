/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

/**
 * Used to indicate that one of the passed parameters to a method
 * is either the wrong type, is missing, or it's value is
 * unacceptable.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class InvalidParameterException extends DAP2ServerSideException {
  /**
   * Construct a <code>InvalidParameterException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public InvalidParameterException(String s) {
    super(MALFORMED_EXPR, "Invalid Parameter Exception: " + s);
  }


  /**
   * Construct a <code>InvalidParameterException</code> with the specified
   * message and OPeNDAP error code (see <code>DAP2Exception</code>).
   *
   * @param err the OPeNDAP error code.
   * @param s the detail message.
   */
  public InvalidParameterException(int err, String s) {
    super(err, s);
  }
}


