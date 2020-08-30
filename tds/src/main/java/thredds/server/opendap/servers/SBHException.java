/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

/**
 * The Something Bad Happened (SBH) Exception.
 * This gets thrown in situations where something
 * pretty bad went down and we don't have a good
 * exception type to describe the problem, or
 * we don't really know what the hell is going on.
 * <p/>
 * Yes, its the garbage dump of our exception
 * classes.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */
public class SBHException extends DAP2ServerSideException {
  /**
   * Construct a <code>SBHException</code> with the specified
   * detail message.
   *
   * @param s the detail message.
   */
  public SBHException(String s) {
    super(UNKNOWN_ERROR, "Ow! Something Bad Happened! All I know is: " + s);
  }


  /**
   * Construct a <code>SBHException</code> with the specified
   * message and OPeNDAP error code see (<code>DAP2Exception</code>).
   *
   * @param err the OPeNDAP error code.
   * @param s the detail message.
   */
  public SBHException(int err, String s) {
    super(err, s);
  }
}


