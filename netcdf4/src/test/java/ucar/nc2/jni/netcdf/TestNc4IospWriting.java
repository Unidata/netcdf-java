/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
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

/** Test copying files to netcdf4 with NetcdfCopier. Compare original. */
public class TestNc4IospWriting {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private int countNotOK = 0;

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void unlimDim0() throws IOException {
    File fin = new File(TestDir.cdmUnitTestDir + "formats/netcdf3/longOffset.nc");
    String datasetOut = tempFolder.newFile().getAbsolutePath();

    copyFile(fin.getAbsolutePath(), datasetOut, NetcdfFileFormat.NETCDF3);
    copyFile(fin.getAbsolutePath(), datasetOut, NetcdfFileFormat.NETCDF4);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void writeNetcdf4Files() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/files/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  @Ignore("not working")
  public void writeNetcdf4Compound() throws IOException {
    int count = 0;
    count +=
        TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/compound/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  // enum not ready

  // @Test
  @Category(NeedsCdmUnitTest.class)
  public void writeHdf5Samples() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/samples/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  // @Test
  @Category(NeedsCdmUnitTest.class)
  public void writeHdf5Support() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/support/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  // @Test
  @Category(NeedsCdmUnitTest.class)
  public void writeNetcdf4Tst() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/tst/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void writeNetcdf4Zender() throws IOException {
    int count = 0;
    count +=
        TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/zender/", new MyFileFilter(), new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  // @Test
  @Category(NeedsCdmUnitTest.class)
  public void readAllHDF5() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/", null, new MyAct(), true);
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  // @Test
  @Category(NeedsCdmUnitTest.class)
  public void writeAllNetcdf3() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf3/", null, new MyAct());
    System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
  }

  private class MyAct implements TestDir.Act {

    public int doAct(String datasetIn) throws IOException {
      String datasetOut = tempFolder.newFile().getAbsolutePath();

      if (!copyFile(datasetIn, datasetOut, NetcdfFileFormat.NETCDF4)) {
        countNotOK++;
      }
      return 1;
    }
  }

  private class MyFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
      if (pathname.getName().equals("tst_opaque_data.nc4")) {
        return false;
      }
      if (pathname.getName().equals("tst_opaques.nc4")) {
        return false;
      }
      if (pathname.getName().endsWith(".ncml")) {
        return false;
      }
      return true;
    }
  }

  private boolean copyFile(String datasetIn, String datasetOut, NetcdfFileFormat format) throws IOException {
    System.out.printf("TestNc4IospWriting copy %s to %s%n", datasetIn, datasetOut);
    try (NetcdfFile ncfileIn = ucar.nc2.NetcdfFiles.open(datasetIn, null)) {
      NetcdfFormatWriter.Builder writer = NetcdfFormatWriter.builder().setLocation(datasetOut).setFormat(format);
      try (NetcdfCopier copier = NetcdfCopier.create(ncfileIn, writer)) {
        copier.write(null);
      }
      try (NetcdfFile ncfileOut = ucar.nc2.NetcdfFiles.open(datasetOut, null)) {
        compare(ncfileIn, ncfileOut, false, false, true);
      }
    }
    return true;
  }

  private boolean compare(NetcdfFile nc1, NetcdfFile nc2, boolean showCompare, boolean showEach, boolean compareData) {
    Formatter f = new Formatter();
    CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
    boolean ok = tc.compare(nc1, nc2, new CompareNetcdf2.Netcdf4ObjectFilter());
    System.out.printf(" %s compare %s to %s ok = %s%n", ok ? "" : "***", nc1.getLocation(), nc2.getLocation(), ok);
    if (!ok) {
      System.out.printf(" %s%n", f);
    }
    return ok;
  }

  /////////////////////////////////////////////////

  // Demonstrates GitHub issue #301--badly writing subsetted arrays
  @Test
  public void writeSubset() throws IOException, InvalidRangeException {
    String fname = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, fname, null);

    writerb.addDimension(new Dimension("x", 5));
    writerb.addVariable("arr", DataType.FLOAT, "x");

    try (NetcdfFormatWriter writer = writerb.build()) {
      float[] data = new float[] {1.f, 2.f, 3.f, 4.f, 5.f, 6.f, 7.f, 8.f, 9.f, 10.f};
      Array arrData = Array.factory(DataType.FLOAT, new int[] {10}, data);
      Array subArr = arrData.sectionNoReduce(new int[] {1}, new int[] {5}, new int[] {2});

      // Write subsetted array
      writer.write("arr", subArr);
    }

    // Make sure file has what we expect
    try (NetcdfFile ncFile = NetcdfFiles.open(fname)) {
      Variable arr = ncFile.getRootGroup().findVariableLocal("arr");
      Assert.assertEquals(5, arr.getSize());
      Array arrData = arr.read();
      float[] data = (float[]) arrData.get1DJavaArray(Float.class);
      Assert.assertEquals(5, data.length);
      Assert.assertArrayEquals(new float[] {2.f, 4.f, 6.f, 8.f, 10.f}, data, 1e-6f);
    }
  }

}
