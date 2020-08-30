/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

import java.util.List;
import opendap.dap.BaseType;

/**
 * Represents a server-side function, which evaluates to a BaseType.
 * Custom server-side functions which return non-boolean values should
 * implement this interface. For an efficient implementation, it is
 * suggested, when possible, to use the same BaseType for the getType()
 * method and for each successive invocation of evaluate(), changing only
 * the BaseType's value. This avoids creation of large numbers of
 * BaseTypes during a data request.
 *
 * @author joew
 * @see BTFunctionClause
 */
public interface BTFunction extends ServerSideFunction {

  /**
   * A given function must always evaluate to the same class
   * of BaseType. Only the value held by the BaseType may change.
   * This method can be used to discover the BaseType class of a
   * function without actually evaluating it.
   */
  public BaseType getReturnType(List args) throws InvalidParameterException;

  /**
   * Evaluates the function using the argument list given.
   *
   * @throws DAP2ServerSideException Thrown if the function
   *         cannot evaluate successfully. The exact type of exception is up
   *         to the author of the server-side function.
   */
  public BaseType evaluate(List args) throws DAP2ServerSideException;
}


