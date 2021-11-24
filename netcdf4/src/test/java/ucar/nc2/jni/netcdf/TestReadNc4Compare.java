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

/** Compare reading netcdf4 with java and jna iosp */
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
    readWithJnaIosp(filename);
  }

  public static void readWithJavaIosp(String filename) throws Exception {
    try (NetcdfFile file1 = NetcdfFiles.open(filename, null); NetcdfFile file2 = NetcdfFiles.open(filename, null)) {
      System.out.println("Read NetcdfFile with Java: " + file1.getLocation());

      Formatter f = new Formatter();
      CompareNetcdf2 tc = new CompareNetcdf2(f, false, false, true);
      boolean ok = tc.compare(file1, file2, new CompareNetcdf2.Netcdf4ObjectFilter());
      if (!ok) {
        System.out.printf("java = %s%n", file1);
        System.out.printf("%njna = %s%n", file2);
      }
      assertThat(ok).isTrue();
    }
  }

  public static void readWithJnaIosp(String filename) throws Exception {
    try (NetcdfFile file1 = NetcdfFiles.open(filename, iospOrg, -1, null, null);
        NetcdfFile file2 = NetcdfFiles.open(filename, iospArray, -1, null, null)) {
      System.out.println("Read NetcdfFile with Jna: " + file1.getLocation());

      Formatter f = new Formatter();
      CompareNetcdf2 tc = new CompareNetcdf2(f, false, false, true);
      boolean ok = tc.compare(file1, file2, new CompareNetcdf2.Netcdf4ObjectFilter());
      if (!ok) {
        System.out.printf("java = %s%n", file1);
        System.out.printf("%njna = %s%n", file2);
      }
      assertThat(ok).isTrue();
    }
  }

  public static void compareJavaAndJna(String filename) throws Exception {
    try (NetcdfFile javaFile = NetcdfFiles.open(filename, null);
        NetcdfFile jnaFile = NetcdfFiles.open(filename, iospArray, -1, null, null)) {
      System.out.println("Compare Java with Jna: " + javaFile.getLocation());

      Formatter f = new Formatter();
      CompareNetcdf2 tc = new CompareNetcdf2(f, false, false, true);
      boolean ok = tc.compare(javaFile, jnaFile, new CompareNetcdf2.Netcdf4ObjectFilter());
      if (!ok) {
        System.out.printf("java = %s%n", javaFile);
        System.out.printf("%njna = %s%n", jnaFile);
      }
      assertThat(ok).isTrue();
    }
  }

}

