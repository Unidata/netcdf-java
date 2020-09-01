/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

/**
 * Report a type-mismatch problem in the constraint expression. Examples are
 * using relational operators on arrays, using the dot notation on variables
 * that are not aggregate types.
 *
 * @author jhrg
 * @version $Revision: 15901 $
 * @see DAP2ServerSideException
 * @see opendap.dap.DAP2Exception
 */

public class WrongTypeException extends DAP2ServerSideException {
  /**
   * Construct a <code>WrongTypeException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public WrongTypeException(String s) {
    super(MALFORMED_EXPR, s);
  }
}


