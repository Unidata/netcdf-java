/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Formatter;
import java.util.Map;

public abstract class Filter {

  public abstract String getName();

  public abstract int getId();

  public abstract byte[] encode(byte[] dataIn) throws IOException;

  public abstract byte[] decode(byte[] dataIn) throws IOException;

  public String toString() {
    Formatter f = new Formatter();
    return f.format("Name: %s, ID: %d", getName(), getId()).toString();
  }
}
