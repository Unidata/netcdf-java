/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import ucar.ma2.InvalidRangeException;

import java.io.Closeable;
import java.io.IOException;

/** Reads Grid values. */
public interface GridReader extends Closeable {

  String getLocation();

  GridReferencedArray readData(Grid grid, GridSubset subset, boolean canonicalOrder)
      throws IOException, InvalidRangeException;

}
