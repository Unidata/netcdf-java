/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

/** Test TestNcml - misccellaneous aggregation features. */
public class TestAggMisc {

  @Test
  public void testNestedValues() throws Exception {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n" // leavit
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' >\n" // leavit
        + "  <aggregation dimName='time' type='joinExisting'>\n" // leavit
        + "   <netcdf>\n" // leavit
        + "     <dimension name='time' isUnlimited='true' length='10'/>\n" // leavit
        + "     <variable name='time' shape='time' type='double'>\n" // leavit
        + "         <values start='0' increment='1' />\n" // leavit
        + "     </variable>\n" // leavit
        + "   </netcdf>\n" // leavit
        + "   <netcdf >\n" // leavit
        + "     <dimension name='time' isUnlimited='true' length='10'/>\n" // leavit
        + "     <variable name='time' shape='time' type='double'>\n" // leavit
        + "         <values start='10' increment='1' />\n" // leavit
        + "     </variable>\n" // leavit
        + "   </netcdf>\n" // leavit
        + "  </aggregation>\n" // leavit
        + "</netcdf>"; // leavit

    String location = "testNestedValues.ncml";

    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), location, null)) {
      TestDir.readAllData(ncfile);

      Variable v = ncfile.findVariable("time");
      Array data = v.readArray();
      assert data.getSize() == 20;
    }
  }

  @Test
  public void testNestedAgg() throws Exception {
    String filename = "file:./" + TestDir.cdmLocalTestDataDir + "testNested.ncml";

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      TestDir.readAllData(ncfile);

      Variable v = ncfile.findVariable("time");
      Array data = v.readArray();
      assert data.getSize() == 59;
    }
  }

  @Test
  public void testNestedScan() throws Exception {
    String filename = "file:./" + TestNcmlRead.topDir + "nested/TestNestedDirs.ncml";

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      TestDir.readAllData(ncfile);

      Variable v = ncfile.findVariable("time");
      Array data = v.readArray();
      assert data.getSize() == 3;
    }
  }
}
