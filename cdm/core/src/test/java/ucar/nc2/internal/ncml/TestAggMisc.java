/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ncml.TestNcMLRead;
import ucar.nc2.write.Ncdump;
import ucar.unidata.util.test.TestDir;

/** Test TestNcml - misccellaneous aggregation features. */
public class TestAggMisc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testNestedValues() throws IOException, InvalidRangeException, InterruptedException {
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

    try (NetcdfFile ncfile = NcMLReaderNew.readNcML(new StringReader(ncml), location, null).build()) {
      TestDir.readAllData(ncfile);

      Variable v = ncfile.findVariable("time");
      Array data = v.read();
      assert data.getSize() == 20;
      logger.debug(Ncdump.printArray(data));
    }
  }

  @Test
  public void testNestedAgg() throws IOException, InvalidRangeException, InterruptedException {
    String filename = "file:./" + TestDir.cdmLocalTestDataDir + "testNested.ncml";

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      TestDir.readAllData(ncfile);

      Variable v = ncfile.findVariable("time");
      Array data = v.read();
      assert data.getSize() == 59;
      logger.debug(Ncdump.printArray(data));
    }
  }

  @Test
  public void testNestedScan() throws IOException, InvalidRangeException, InterruptedException {
    String filename = "file:./" + TestNcMLRead.topDir + "nested/TestNestedDirs.ncml";

    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null)) {
      TestDir.readAllData(ncfile);

      Variable v = ncfile.findVariable("time");
      Array data = v.read();
      assert data.getSize() == 3;
      logger.debug(Ncdump.printArray(data));
    }
  }
}
