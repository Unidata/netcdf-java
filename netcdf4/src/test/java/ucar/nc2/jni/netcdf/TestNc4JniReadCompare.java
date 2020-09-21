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
import ucar.nc2.*;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/** Compare reading netcdf through jni with native java reading */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNc4JniReadCompare {

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix("cdl ncml IntTimSciSamp.nc");
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
    try (NetcdfFile ncfile = NetcdfFiles.open(filename); NetcdfFile jni = TestNc4reader.openJni(filename)) {
      System.err.println("Test input: " + ncfile.getLocation());
      System.err.println("Baseline: " + jni.getLocation());
      System.err.flush();

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
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

}

