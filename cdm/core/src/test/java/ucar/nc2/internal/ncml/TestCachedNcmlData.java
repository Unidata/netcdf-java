/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

public class TestCachedNcmlData {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCachedData() throws IOException {

    NetcdfFile ncd = null;
    try {
      ncd = NetcdfDatasets.openFile(TestDir.cdmLocalTestDataDir + "point/profileMultidim.ncml", null);
      Variable v = ncd.findVariable("data");
      assert v != null;
      Array data = v.read();
      assert data.getSize() == 50 : data.getSize();
    } finally {
      if (ncd != null)
        ncd.close();
    }
  }

  // doesnt work
  public void testCachedDataWithStructure() throws IOException {

    NetcdfFile ncd = null;
    try {
      ncd = NetcdfDatasets.openFile(TestDir.cdmLocalTestDataDir + "point/profileMultidim.ncml", null);
      boolean ok = (Boolean) ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      assert ok;

      Variable s = ncd.findVariable("record");
      assert s != null;
      assert s instanceof Structure;
      assert s.getSize() == 5 : s.getSize();

      Array data = s.read();
      assert data.getSize() == 5 : data.getSize();

    } finally {
      if (ncd != null)
        ncd.close();
    }
  }


}
