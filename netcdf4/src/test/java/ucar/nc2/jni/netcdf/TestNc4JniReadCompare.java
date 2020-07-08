/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Compare reading through jni with native java reading
 *
 * @author caron
 * @since 10/22/13
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNc4JniReadCompare {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix("cdl ncml");
    List<Object[]> result = new ArrayList<Object[]>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf3/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/", ff, result);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestNc4JniReadCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  int fail = 0;
  int success = 0;

  @Test
  public void compareDatasets() throws IOException {

    try (NetcdfFile ncfile = NetcdfFile.open(filename); NetcdfFile jni = openJni(filename)) {
      jni.setLocation(filename + " (jni)");
      System.err.println("Test input: " + ncfile.getLocation());
      System.err.println("Baseline: " + jni.getLocation());
      System.err.flush();

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, false);
      boolean ok = mind.compare(ncfile, jni, new CompareNetcdf2.Netcdf4ObjectFilter());
      if (!ok) {
        fail++;
        System.out.printf("--Compare %s%n", filename);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK%n", filename);
        success++;
      }
      Assert.assertTrue(filename, ok);
    }
  }

  private NetcdfFile openJni(String location) throws IOException {
    Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
    RandomAccessFile raf = new RandomAccessFile(location, "r");
    iosp.open(raf, ncfile, null);
    return ncfile;
  }

}

