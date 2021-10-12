/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.bufr;

import java.io.IOException;

public interface EmbeddedTableIF {
  void addTable(Message m);

  TableLookup getTableLookup() throws IOException;
}
