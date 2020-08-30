/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

import opendap.dap.InvalidDimensionException;
// import opendap.dap.NoSuchVariableException;


/**
 * This interface extends the <code>ArrayMethods</code> for OPeNDAP types that
 * extend <code>DArray</code> and <code>DGrid</code> classes. It contains
 * additional projection methods needed by the Server side implementations
 * of these types.
 * <p>
 * A projection for an array must include the start, stride and stop
 * information for each dimension of the array in addition to the basic
 * information that the array itself is <em>projected</em>. This interface
 * provides access to that information.
 *
 * @author jhrg & ndp
 * @version $Revision: 15901 $
 * @see opendap.dap.DArray
 * @see opendap.dap.DGrid
 * @see SDArray
 * @see SDGrid
 * @see ServerMethods
 * @see Operator
 */


public interface ServerArrayMethods extends ServerMethods {

  public void setProjection(int dimension, int start, int stride, int stop)
      throws InvalidDimensionException, SBHException;

  public int getStart(int dimension) throws InvalidDimensionException;

  public int getStride(int dimension) throws InvalidDimensionException;

  public int getStop(int dimension) throws InvalidDimensionException;

}


