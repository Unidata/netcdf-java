/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package thredds.server.opendap.servers;

import java.util.List;

/**
 * Represents a server-side function, which evaluates to a boolean value.
 * Custom server-side functions which return boolean values
 * should implement this interface.
 *
 * @author joew
 * @see BoolFunctionClause
 */
public interface BoolFunction extends ServerSideFunction {

  /**
   * Evaluates the function using the argument list given.
   *
   * @throws DAP2ServerSideException Thrown if the function
   *         cannot evaluate successfully. The exact type of exception is up
   *         to the author of the server-side function.
   */
  public boolean evaluate(List args) throws DAP2ServerSideException;
}


