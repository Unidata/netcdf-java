/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import java.util.ArrayList;
import java.util.List;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.*;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.*;
import java.util.Formatter;

/** Test copying files to netcdf4 with NetcdfCopier. Read back and compare to original. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNc4JniWriteCompare {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<Object[]>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf3", new MyFileFilter(), result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/files", new MyFileFilter(), result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/compound", new MyFileFilter(), result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/tst", new MyFileFilter(), result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/zender", new MyFileFilter(), result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/samples", new MyFileFilter(), result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/support", new MyFileFilter(), result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/support", new MyFileFilter(), result);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  private static class MyFileFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
      if (pathname.getName().contains("opaque")) {
        return false;
      }
      if (pathname.getName().contains("enumcmpnd")) {
        return false;
      }
      if (pathname.getName().contains("cenum")) {
        return false;
      }
      if (pathname.getName().contains("bitop")) {
        return false;
      }
      if (pathname.getName().endsWith(".ncml")) {
        return false;
      }
      return true;
    }
  }

  private final String filename;

  public TestNc4JniWriteCompare(String filename) {
    this.filename = filename;
  }

  @Test
  public void copyFile() throws IOException {
    String fileout = tempFolder.newFile().getAbsolutePath();
    System.out.printf("TestNc4IospWriting copy %s to %s%n", filename, fileout);

    try (NetcdfFile ncfileIn = ucar.nc2.NetcdfFiles.open(filename, null)) {
      NetcdfFormatWriter.Builder writer =
          NetcdfFormatWriter.builder().setLocation(fileout).setFormat(NetcdfFileFormat.NETCDF4);
      try (NetcdfCopier copier = NetcdfCopier.create(ncfileIn, writer)) {
        copier.write(null);
      }
      try (NetcdfFile ncfileOut = ucar.nc2.NetcdfFiles.open(fileout, null)) {
        compare(ncfileIn, ncfileOut, false, false, true);
      }
      try (NetcdfFile jni = TestNc4reader.openJni(fileout)) {
        compare(ncfileIn, jni, false, false, true);
      }
    }
  }

  private boolean compare(NetcdfFile nc1, NetcdfFile nc2, boolean showCompare, boolean showEach, boolean compareData) {
    Formatter f = new Formatter();
    CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
    boolean ok = tc.compare(nc1, nc2, new CompareNetcdf2.Netcdf4ObjectFilter());
    System.out.printf(" %s compare %s to %s ok = %s%n", ok ? "" : "***", nc1.getLocation(), nc2.getLocation(), ok);
    if (!ok) {
      fail();
      System.out.printf(" %s%n", f);
    }
    return ok;
  }

  private void fail() {}

}
