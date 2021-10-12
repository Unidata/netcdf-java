/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.array;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading bufr with old ma2.array and new array.Array */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadBufrCompare {
  public static String bufrLocalFromTop = "src/test/data/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml");
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(bufrLocalFromTop, ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/embeddedTable", ff, result, false);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadBufrCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void doOne() throws Exception {
    compareMa2Array(filename);
  }

  public static void compareMa2Array(String filename) throws Exception {
    try (NetcdfFile ma2File = NetcdfFiles.open(filename, "ucar.nc2.iosp.bufr.BufrIosp", -1, null, null);
        NetcdfFile arrayFile = NetcdfFiles.open(filename, "ucar.nc2.iosp.bufr.BufrArrayIosp", -1, null, null)) {
      System.out.println("Test NetcdfFile: " + arrayFile.getLocation());

      boolean ok = CompareArrayToMa2.compareFiles(ma2File, arrayFile);
      assertThat(ok).isTrue();
    }
  }

  // compare to check complete read. need seperate files, or else they interfere
  public static void readOrg(String filename) throws Exception {
    try (NetcdfFile ma2File = NetcdfFiles.open(filename, "ucar.nc2.iosp.bufr.BufrIosp", -1, null, null);
        NetcdfFile maFile = NetcdfFiles.open(filename, "ucar.nc2.iosp.bufr.BufrIosp", -1, null, null)) {
      System.out.println("Test NetcdfFile: " + ma2File.getLocation());

      boolean ok = CompareNetcdf2.compareFiles(ma2File, maFile, new Formatter());
      assertThat(ok).isTrue();
    }
  }

  // compare to check complete read. need seperate files, or else they interfere
  public static void readArrays(String filename) throws Exception {
    try (NetcdfFile arrayFile = NetcdfFiles.open(filename, "ucar.nc2.iosp.bufr.BufrArrayIosp", -1, null, null);
        NetcdfFile arrayFile2 = NetcdfFiles.open(filename, "ucar.nc2.iosp.bufr.BufrArrayIosp", -1, null, null)) {
      System.out.println("Test NetcdfFile: " + arrayFile.getLocation());

      boolean ok = CompareArrayToArray.compareFiles(arrayFile, arrayFile2);
      assertThat(ok).isTrue();
    }
  }


}

