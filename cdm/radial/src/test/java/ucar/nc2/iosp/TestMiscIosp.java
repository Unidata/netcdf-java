/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.cache.FileCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/** Misc tests on iosp, mostly just sanity (opens ok) */
@Category(NeedsCdmUnitTest.class)
public class TestMiscIosp {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static int leaks;

  @BeforeClass
  static public void startup() {
    RandomAccessFile.setDebugLeaks(true);
    RandomAccessFile.enableDefaultGlobalFileCache();
    leaks = RandomAccessFile.getOpenFiles().size();
  }

  @AfterClass
  static public void checkLeaks() {
    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    assert leaks == TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false);
  }

  @Test
  public void testUamiv() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "formats/uamiv/uamiv.grid", null)) {
      logger.debug("open {}", ncfile.getLocation());
      ucar.nc2.Variable v = ncfile.findVariable("UP");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[] {12, 5, 7, 6});
    }
  }
}
