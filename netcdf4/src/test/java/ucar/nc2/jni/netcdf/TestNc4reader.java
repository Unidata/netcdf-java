/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/** Test JNI netcdf-4 iosp. Compare reading with native java reading */
@Category(NeedsCdmUnitTest.class)
public class TestNc4reader {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static String testDir = TestDir.cdmUnitTestDir + "formats/hdf5/";
  private boolean showCompareResults = true;

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  // i dont trust our code, compare with jni reading
  @Test
  public void sectionStringsWithFilter() throws IOException, InvalidRangeException {
    String filename = testDir + "StringsWFilter.h5";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename); NetcdfFile jni = openJni(filename)) {
      Variable v = ncfile.findVariable("/sample/ids");
      assert v != null;
      int[] shape = v.getShape();
      Assert.assertEquals(1, shape.length);
      Assert.assertEquals(3107, shape[0]);

      Array dataSection = v.read("700:900:2"); // make sure to go acrross a chunk boundary
      Assert.assertEquals(1, dataSection.getRank());
      Assert.assertEquals(101, dataSection.getShape()[0]);

      Variable v2 = jni.findVariable("/sample/ids");
      assert v2 != null;
      int[] shape2 = v2.getShape();
      Assert.assertEquals(1, shape2.length);
      Assert.assertEquals(3107, shape2[0]);

      Array dataSection2 = v2.read("700:900:2");
      Assert.assertEquals(1, dataSection2.getRank());
      Assert.assertEquals(101, dataSection2.getShape()[0]);

      CompareNetcdf2.compareData(v2.getShortName(), dataSection, dataSection2);
    }
  }

  @Test
  public void testReadSubsection() throws IOException, InvalidRangeException {
    String location = TestDir.cdmUnitTestDir + "formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4";
    try (NetcdfFile ncfile = NetcdfFiles.open(location); NetcdfFile jni = openJni(location)) {
      // float salinity(time=1, depth=40, lat=667, lon=622);
      Array data1 = read(ncfile, "salinity", "0,11:12,22,:");
      // NCdumpW.printArray(data1);
      System.out.printf("Read from jni%n");
      Array data2 = read(jni, "salinity", "0,11:12,22,:");
      assert MAMath.nearlyEquals(data1, data2);
      System.out.printf("data is equal%n");
    }
  }

  private Array read(NetcdfFile ncfile, String vname, String section) throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable(vname);
    assert v != null;
    return v.read(section);
  }

  @Test
  public void testNestedStructure() throws IOException {
    doCompare(TestDir.cdmUnitTestDir + "formats/netcdf4/testNestedStructure.nc", true, false, true);
  }

  @Test
  public void cantOpenProblem() throws IOException {
    try (NetcdfFile jni = openJni(TestDir.cdmUnitTestDir + "formats/netcdf4/testEmptyAtts.nc")) {
      System.out.printf("%s%n", jni);
    }
  }

  // @Test
  public void timeRead() throws IOException {
    // file not found
    String location = TestDir.cdmUnitTestDir + "/NARR/narr-TMP-200mb_221_yyyymmdd_hh00_000.grb.grb2.nc4";

    try (NetcdfFile jni = openJni(location)) {
      Variable v = jni.findVariable("time");
      long start = System.currentTimeMillis();
      Array data = v.read();
      long took = System.currentTimeMillis() - start;
      System.out.printf(" jna took= %d msecs size=%d%n", took, data.getSize());
    }

    try (NetcdfFile ncfile = NetcdfFiles.open(location)) {
      Variable v = ncfile.findVariable("time");
      long start = System.currentTimeMillis();
      Array data = v.read();
      long took = System.currentTimeMillis() - start;
      System.out.printf(" java took= %d msecs size=%d%n", took, data.getSize());
    }
  }

  @Test
  public void fractalHeapProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/espresso_his_20130913_0000_0007.nc";
    System.out.printf("***READ %s%n", filename);
    doCompare(filename, false, false, false);

    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Assert.assertNotNull(ncfile.findVariable("h"));
    }
  }

  /*
   ** Missing dim phony_dim_0 = 15; not in file2
   * ...
   */
  // @Test
  public void problem() throws IOException {
    String filename =
        TestDir.cdmUnitTestDir + "formats\\hdf5\\OMI-Aura_L2G-OMCLDRRG_2007m0105_v003-2008m0105t101212.he5";
    System.out.printf("***READ %s%n", filename);
    doCompare(filename, false, false, false);
  }

  static boolean doCompare(String location, boolean showCompare, boolean showEach, boolean compareData)
      throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(location); NetcdfFile jni = openJni(location)) {
      Formatter f = new Formatter();
      CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
      boolean ok = tc.compare(ncfile, jni, new CompareNetcdf2.Netcdf4ObjectFilter());
      System.out.printf(" %s compare %s ok = %s%n", ok ? "" : "***", location, ok);
      if (!ok || showCompare) {
        System.out.printf("%s%n=====================================%n", f);
      }
      return ok;
    }
  }

  static NetcdfFile openJni(String location) throws IOException {
    Nc4reader iosp = new Nc4reader();
    RandomAccessFile raf = new RandomAccessFile(location, "r");
    return NetcdfFiles.build(iosp, raf, raf.getLocation(), null);
  }

}
