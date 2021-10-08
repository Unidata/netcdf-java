/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

/** Test {@link Ncdump} */
public class TestNcdump {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Asserts that the issue identified in PCM-232977 has been fixed.
  // See https://andy.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=28658.
  // Asserts that GitHub issue #929 has been fixed. See https://github.com/Unidata/thredds/issues/929
  @Test
  public void testUnsignedFillValue() throws IOException {
    try (
        NetcdfFile ncfile = NetcdfDatasets.openFile(TestDir.cdmLocalTestDataDir + "testUnsignedFillValue.ncml", null)) {
      Ncdump ncdump = Ncdump.builder(ncfile).setShowAllValues().build();
      String ncdumpOut = ncdump.print();

      File expectedOutputFile = new File(TestDir.cdmLocalTestDataDir, "testUnsignedFillValueNew.dump");
      String expectedOutput = Files.toString(expectedOutputFile, StandardCharsets.UTF_8);

      Assert.assertEquals(toUnixEOLs(expectedOutput), toUnixEOLs(ncdumpOut));
    }
  }

  // Make sure the indentation is correct with a complex, nested structure.
  @Test
  public void testNestedGroups() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(TestDir.cdmLocalTestDataDir + "testNestedGroups.ncml", null)) {
      Ncdump ncdump = Ncdump.builder(ncfile).setShowAllValues().build();
      String ncdumpOut = ncdump.print();

      File expectedOutputFile = new File(TestDir.cdmLocalTestDataDir, "testNestedGroups.dump");
      String expectedOutput = Files.toString(expectedOutputFile, StandardCharsets.UTF_8);

      Assert.assertEquals(toUnixEOLs(expectedOutput), toUnixEOLs(ncdumpOut));
    }
  }

  private static String toUnixEOLs(String input) {
    return input.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
  }
}
