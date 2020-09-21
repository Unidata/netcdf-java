/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr.tables;

import java.io.File;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;

/** Test problem when bad bufr lookup table is givem. */
public class TestBadBufrTableLookup {

  @Test
  public void addBadLookup() throws Exception {
    try {
      BufrTables.addLookupFile("resource:badTable.xml");
    } catch (Throwable t) {
      System.out.printf("%s%n", t);
    }

    File dataset = new File("src/test/data/test1.bufr");
    try (NetcdfFile ncFile = NetcdfDatasets.openDataset(dataset.getAbsolutePath())) {
    }
  }

}
