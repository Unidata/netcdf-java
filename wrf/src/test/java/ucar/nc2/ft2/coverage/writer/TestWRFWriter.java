/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.writer;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileSystems;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.ft2.coverage.writer.NcWRFWriter;
import ucar.unidata.util.test.TestDir;

/**
 * Test the WRFWriter command line tool
 * Sept. 2019
 * hvandam
 */
public class TestWRFWriter {

//  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PrintStream systemOut = System.out;

  private ByteArrayOutputStream testOut;

  @Before
  public void setUpOutput() {
    testOut = new ByteArrayOutputStream();

    System.setOut(new PrintStream(testOut));
  }

  private String getOutput() {
    return testOut.toString();
  }

  @After
  public void restoreSystemInputOutput() {
    System.setOut(systemOut);
  }


  @Test
  public void testHelp() throws IOException {

    NcWRFWriter.main("-h");

 //   Path path = FileSystems.getDefault().getPath("out/test/resources", "help_output.txt");
 //   String data = new String(java.nio.file.Files.readAllBytes(path));

 //   Assert.assertEquals(data, getOutput());
  }

  /********************
  @Test
  public void testShowvars() throws IOException {

    String[] command = {"-i", "/Users/hvandam/test_data/tds_test_data/cdmUnitTest/gribCollections/gfs_2p5deg/GFS_Global_2p5deg_20150301_0000.grib2",
        "-v", "out/test/resources/Vtable.GFS", "-o", "bob", "-s"};

    NcWRFWriter.main(command);

    Path path = FileSystems.getDefault().getPath("out/test/resources", "showvars_output.txt");
    String data = new String(java.nio.file.Files.readAllBytes(path));

    Assert.assertEquals(data, getOutput());
  }

  @Test
  public void testNestedGroups() throws IOException {
    try (StringWriter sw = new StringWriter()) {
      NCdumpW.print(TestDir.cdmLocalTestDataDir + "testNestedGroups.ncml",
          sw, true, true, false, false, null, null);

      File expectedOutputFile = new File(TestDir.cdmLocalTestDataDir, "testNestedGroups.dump");
      String expectedOutput = Files.toString(expectedOutputFile, Charsets.UTF_8);

      Assert.assertEquals(toUnixEOLs(expectedOutput), toUnixEOLs(sw.toString()));
    }
  }

   *******************************/

  public static String toUnixEOLs(String input) {
    return input.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
  }

}
