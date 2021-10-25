/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.nc2.util.CancelTask; // ??
import java.io.IOException;

/** Reader of the data for a Variable. */
public interface ProxyReader {

  /** Read all the data for a Variable, returning ucar.array.Array. */
  ucar.array.Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException;

  /** Read a section of the data for a Variable, returning ucar.array.Array. */
  ucar.array.Array<?> proxyReadArray(Variable client, ucar.array.Section section, CancelTask cancelTask)
      throws IOException, ucar.array.InvalidRangeException;

}
