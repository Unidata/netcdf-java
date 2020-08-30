/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

/**
 * DAP2 Exception for use by a server.
 * This is the root of all the OPeNDAP Server exception classes.
 *
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class DAP2ServerSideException extends opendap.dap.DAP2Exception {
  /**
   * Construct a <code>DAP2ServerSideException</code> with the specified detail
   * message.
   *
   * @param err The error number. See opendap.dap.DAP2Exception for the values
   *        and their meaning.
   * @param s the detail message.
   * @see opendap.dap.DAP2Exception
   */
  public DAP2ServerSideException(int err, String s) {
    super(err, s);
  }
}


