/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading netcdf4 with old ma2.Array and new array.Array IOSPs */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadNc4Compare {
  public static String iospOrg = "ucar.nc2.jni.netcdf.Nc4reader";
  public static String iospArray = "ucar.nc2.jni.netcdf.Nc4reader";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    // UpperDeschutes_t4p10_swemelt.nc is too big
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml UpperDeschutes_t4p10_swemelt.nc");
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf3/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4/", ff, result);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadNc4Compare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void doOne() throws Exception {
    compareMa2Array(filename);
  }

  public static void compareMa2Array(String filename) throws Exception {
    try (NetcdfFile ma2File = NetcdfFiles.open(filename, iospOrg, -1, null, null);
        NetcdfFile arrayFile = NetcdfFiles.open(filename, iospArray, -1, null, null)) {
      System.out.println("Test NetcdfFile: " + arrayFile.getLocation());

      boolean ok = CompareArrayToArray.compareFiles(ma2File, arrayFile);
      assertThat(ok).isTrue();
    }
  }

  // compare to check complete read. need seperate files, or else they interfere
  public static void readOrg(String filename) throws Exception {
    try (NetcdfFile ma2File = NetcdfFiles.open(filename, iospOrg, -1, null, null);
        NetcdfFile maFile = NetcdfFiles.open(filename, iospOrg, -1, null, null)) {
      System.out.println("Test NetcdfFile: " + ma2File.getLocation());

      boolean ok = CompareNetcdf2.compareFiles(ma2File, maFile, new Formatter());
      assertThat(ok).isTrue();
    }
  }

  // compare to check complete read. need seperate files, or else they interfere
  public static void readArrays(String filename) throws Exception {
    try (NetcdfFile arrayFile = NetcdfFiles.open(filename, iospArray, -1, null, null);
        NetcdfFile arrayFile2 = NetcdfFiles.open(filename, iospArray, -1, null, null)) {
      System.out.println("Test NetcdfFile: " + arrayFile.getLocation());

      boolean ok = CompareArrayToArray.compareFiles(arrayFile, arrayFile2);
      assertThat(ok).isTrue();
    }
  }

}

