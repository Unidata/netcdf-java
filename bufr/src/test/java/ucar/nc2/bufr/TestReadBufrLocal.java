/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.bufr;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertThrows;

/** Read local bufr data. */
public class TestReadBufrLocal {

  @Test
  public void testProblem() throws Exception {
    String filename = TestBufrReadAllData.bufrLocalFromTop + "RadiosondeStationData.bufr";
    TestBufrReadAllData.readArrays(filename);
    TestBufrReadAllData.bitCount(filename);
  }

  @Test
  public void testNoDatasets() {
    String filename = TestBufrReadAllData.bufrLocalFromTop + "temp_20210824133030_IUSK11_AMMC_241200.bufr";
    assertThrows(java.io.IOException.class, () -> TestBufrReadAllData.readArrays(filename)).getMessage()
        .contains("No data messages in the file");
  }

  @Test
  @Ignore("Issue 982")
  public void testEmbeddedBits() throws Exception {
    String filename = TestBufrReadAllData.bufrLocalFromTop + "embedded.bufr";
    // TestBufrReadAllData.readArrays(filename);
    TestBufrReadAllData.bitCount(filename);
  }

}

