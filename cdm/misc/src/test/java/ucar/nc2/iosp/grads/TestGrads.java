/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.grads;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.cache.FileCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Misc tests on grads files */
@Category(NeedsCdmUnitTest.class)
public class TestGrads {
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
  public void testGrads() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/grads/mask.ctl";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFiles.open(fileIn)) {
      logger.debug("open {}", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("mask");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      Assert2.assertNearlyEquals(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[] {1, 1, 180, 360});
    }
  }

  @Test
  @Ignore("dunno what kind of grads file this is")
  public void testGrads2() throws IOException, InvalidRangeException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/grads/pdef.ctl";
    try (ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFiles.open(fileIn)) {
      logger.debug("open {}", ncf.getLocation());

      ucar.nc2.Variable v = ncf.findVariable("pdef");
      assert v != null;
      assert v.getDataType() == DataType.FLOAT;
      Attribute att = v.findAttribute(CDM.MISSING_VALUE);
      assert att != null;
      assert att.getDataType() == DataType.FLOAT;
      Assert2.assertNearlyEquals(att.getNumericValue().floatValue(), -9999.0f);

      Array data = v.read();
      assert Arrays.equals(data.getShape(), new int[] {1, 1, 180, 360});
    }
  }

}

