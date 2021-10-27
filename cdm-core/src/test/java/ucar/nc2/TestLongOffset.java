/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

/** Test reading a ncfile with long offsets "large format". */
public class TestLongOffset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private File tempFile;
  private FileOutputStream out;

  protected void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new FileOutputStream(tempFile);
  }

  protected void tearDown() throws Exception {
    out.close();
    if (!tempFile.delete())
      logger.debug("delete failed on {}", tempFile);
  }

  @Test
  public void testReadLongOffset() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "longOffset.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      StringWriter sw = new StringWriter();
      NcdumpArray.ncdump(ncfile, "-vall", sw, null);
      logger.debug(sw.toString());
    }
  }

  @Test
  public void testReadLongOffsetV3mode() throws IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("longOffset.nc")) {
      StringWriter sw = new StringWriter();
      NcdumpArray.ncdump(ncfile, "-vall", sw, null);
      logger.debug(sw.toString());
    }
  }
}
