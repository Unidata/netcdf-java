/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.hdf4;

import org.junit.Test;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

// Test a file that should fail check without erroring
public class TestH4Invalid {

  @Test
  public void testSmall() throws IOException {
    try (RandomAccessFile raf =
        new RandomAccessFile(TestDir.cdmLocalTestDataDir + "hdf4/Level3_GYX_N0R_20151012_1441.nids.invalidhdf4", "r")) {
      assertThat(H4header.isValidFile(raf)).isFalse();
    }
  }

}
