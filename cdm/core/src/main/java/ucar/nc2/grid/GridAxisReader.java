/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import java.io.IOException;

/** Reads GridAxis values, allows lazy evaluation. */
public interface GridAxisReader {
  double[] readCoordValues(GridAxis coordAxis) throws IOException;
}
