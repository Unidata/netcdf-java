/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.io.IOException;

public abstract class Filter {

  public abstract byte[] encode(byte[] dataIn) throws IOException;

  public abstract byte[] decode(byte[] dataIn) throws IOException;

}
