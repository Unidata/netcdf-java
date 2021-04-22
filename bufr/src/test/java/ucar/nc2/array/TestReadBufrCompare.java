/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.array;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.array.StructureData;
import ucar.ma2.DataType;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading bufr with ma2.array and array.Array */
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
  public void compareSequence() throws IOException {
    compareSequence(filename);
  }

  public static void compareSequence(String filename) throws IOException {
    try (NetcdfFile org = NetcdfFiles.open(filename, -1, null);
        NetcdfFile copy = NetcdfFiles.open(filename, -1, null)) {
      System.out.println("Test NetcdfFile: " + org.getLocation());

      boolean ok = true;
      for (Variable v : org.getVariables()) {
        if (v.getDataType() == DataType.SEQUENCE) {
          System.out.printf("  read sequence %s %s%n", v.getDataType(), v.getShortName());
          Sequence s = (Sequence) v;
          StructureDataIterator orgSeq = s.getStructureIterator(-1);
          Sequence copyv = (Sequence) copy.findVariable(v.getFullName());
          Iterator<StructureData> array = copyv.iterator();
          Formatter f = new Formatter();
          boolean ok1 = compareMa2WithArray(f, v.getShortName(), orgSeq, array);
          System.out.printf("%s%n", f);
          ok &= ok1;
        }
      }
      assertThat(ok).isTrue();
    } catch (FileNotFoundException e) {
      File file = new File(filename);
      System.out.printf("File.getAbsolutePath = %s%n", file.getAbsolutePath());
      throw e;
    }
  }

  static boolean compareMa2WithArray(Formatter f, String name, StructureDataIterator org,
      Iterator<ucar.array.StructureData> array) throws IOException {
    boolean ok = true;
    int obsrow = 0;
    while (org.hasNext() && array.hasNext()) {
      ok &= compareStructureData(f, org.next(), array.next(), false);
      obsrow++;
    }
    return ok;
  }

  private static boolean compareStructureData(Formatter f, ucar.ma2.StructureData org, ucar.array.StructureData array,
      boolean justOne) throws IOException {
    boolean ok = true;

    StructureMembers sm1 = org.getStructureMembers();
    ucar.array.StructureMembers sm2 = array.getStructureMembers();
    if (sm1.getMembers().size() != sm2.getMembers().size()) {
      f.format(" membersize %d !== %d%n", sm1.getMembers().size(), sm2.getMembers().size());
      ok = false;
    }

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      ucar.array.StructureMembers.Member m2 = sm2.findMember(m1.getName());
      if (m2 == null) {
        System.out.printf("Cant find %s in copy%n", m1.getName());
        continue;
      }
      ucar.ma2.Array data1 = org.getArray(m1);
      ucar.array.Array<?> data2 = array.getMemberData(m2);
      if (data1 != null && data2 != null) {
        f.format("    compare member %s %s%n", m1.getDataType(), m1.getName());
        ok &= CompareArrayToMa2.compareData(f, m1.getName(), data1, data2, justOne, false);
      } else {
        f.format("    %s data MISSING %s %s%n", m1.getName(), data1 != null, data2 != null);
      }
    }
    return ok;
  }

}

