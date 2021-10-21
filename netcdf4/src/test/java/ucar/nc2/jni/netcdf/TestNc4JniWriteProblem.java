/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading netcdf through jni with native java reading */
@Category(NeedsCdmUnitTest.class)
public class TestNc4JniWriteProblem {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void checkLibrary() {
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  /////////////////////////////////////////////////

  // Demonstrates GitHub issue #301--badly writing subsetted arrays
  @Test
  public void writeSubset() throws IOException, InvalidRangeException {
    String fname = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, fname, null);

    writerb.addDimension(new Dimension("x", 5));
    writerb.addVariable("arr", ArrayType.FLOAT, "x");

    try (NetcdfFormatWriter writer = writerb.build()) {
      float[] data = new float[] {1.f, 2.f, 3.f, 4.f, 5.f, 6.f, 7.f, 8.f, 9.f, 10.f};
      Array<?> arrData = Arrays.factory(ArrayType.FLOAT, new int[] {10}, data);
      Array<?> subArr = Arrays.section(arrData, Section.builder().appendRange(1, 9, 2).build());

      // Write subsetted array
      writer.write(writer.findVariable("arr"), subArr.getIndex(), subArr);
    }

    // Make sure file has what we expect
    try (NetcdfFile ncFile = NetcdfFiles.open(fname)) {
      Variable arr = ncFile.getRootGroup().findVariableLocal("arr");
      Assert.assertEquals(5, arr.getSize());
      Array<Float> arrData = (Array<Float>) arr.readArray();
      float[] expectedData = new float[] {2.f, 4.f, 6.f, 8.f, 10.f};
      assertThat(arrData.length()).isEqualTo(5);
      int count = 0;
      for (float val : arrData) {
        assertThat(val).isEqualTo(expectedData[count++]);
      }
    }
  }

  @Test
  public void problemWithEnumHdf() throws IOException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/hdf5/support/enum.h5";
    String fileOut = tempFolder.newFile().getAbsolutePath();
    copyFile(fileIn, fileOut, NetcdfFileFormat.NETCDF4);
  }

  /*
   * netcdf D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf5/support/cenum.h5 {
   * types:
   * enum color_name { 'RED' = 0, 'GREEN' = 1, 'BLUE' = 2, 'WHITE' = 3, 'BLACK' = 4};
   * variables:
   * Structure {
   * enum color_name color_name;
   * } enum(10);
   * }
   */
  @Test
  @Ignore("not ready")
  public void problemWithEnumMember() throws IOException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/hdf5/support/cenum.h5";
    String fileOut = tempFolder.newFile().getAbsolutePath();
    copyFile(fileIn, fileOut, NetcdfFileFormat.NETCDF4);
  }

  /*
   * netcdf D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf5/support/bitop.h5 {
   * group: typetests {
   * variables:
   * byte bitfield_1(32);
   * short bitfield_2(16);
   * opaque opaque_1(32);
   * :_opaqueDesc = "testing 1-byte opaque type";
   * opaque opaque_2(8);
   * :_opaqueDesc = "testing 4-byte opaque type";
   * }
   * }
   */
  @Test
  @Ignore("not ready")
  public void problemWithOpaqueHdf5() throws IOException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/hdf5/support/bitop.h5";
    String fileOut = tempFolder.newFile().getAbsolutePath();
    copyFile(fileIn, fileOut, NetcdfFileFormat.NETCDF4);
  }

  @Test
  public void problemWithEnumNc4() throws IOException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/tst_enums.nc";
    String fileOut = tempFolder.newFile().getAbsolutePath();
    copyFile(fileIn, fileOut, NetcdfFileFormat.NETCDF4);
  }

  @Test
  @Ignore("not ready")
  public void problemWithOpaque() throws IOException {
    String fileIn = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/tst_opaques.nc";
    String fileOut = tempFolder.newFile().getAbsolutePath();
    copyFile(fileIn, fileOut, NetcdfFileFormat.NETCDF4);
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
      try (NetcdfFile jni = TestNc4reader.openJni(datasetOut)) {
        compare(ncfileIn, jni, false, false, true);
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
      fail();
      System.out.printf(" %s%n", f);
    }
    return ok;
  }

  private void fail() {}

}

