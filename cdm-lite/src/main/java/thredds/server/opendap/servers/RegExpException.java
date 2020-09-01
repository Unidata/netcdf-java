/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servers;

/**
 * Thrown by <code>Operator.op</code> when an attempt is made to parse a
 * improperly formed regular expression. Reular expressions should use
 * the same syntax as <i>grep</i>.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see Operator#op(int,opendap.dap.BaseType, opendap.dap.BaseType)
 */
public class RegExpException extends DAP2ServerSideException {
  /**
   * Construct a <code>RegExpException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public RegExpException(String s) {
    super(MALFORMED_EXPR, "Syntax Error In Regular Expression: " + s);
  }


  /**
   * Construct a <code>RegExpException</code> with the specified
   * message and OPeNDAP error code see (<code>DAP2Exception</code>).
   *
   * @param err the OPeNDAP error code.
   * @param s the detail message.
   */
  public RegExpException(int err, String s) {
    super(err, s);
  }
}


