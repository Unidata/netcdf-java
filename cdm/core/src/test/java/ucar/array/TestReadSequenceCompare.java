/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.array;

import static com.google.common.truth.Truth.assertThat;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare reading netcdf with Array */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadSequenceCompare {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);

    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadSequenceCompare(String filename) {
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
          boolean ok1 = TestReadArrayCompare.compareSequence(f, v.getShortName(), orgSeq, array);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;
        }
      }
      assertThat(ok).isTrue();
    }
  }

  @Test
  public void compareDataset() throws IOException {
    compareDataset(filename);
  }

  public static void compareDataset(String filename) throws IOException {

    try (NetcdfDataset org = NetcdfDatasets.openDataset(filename);
        NetcdfDataset copy = NetcdfDatasets.openDataset(filename)) {
      System.out.println("Test NetcdfDataset: " + org.getLocation());

      boolean ok = true;
      for (Variable v : org.getVariables()) {
        if (v.getDataType() == DataType.SEQUENCE) {
          System.out.printf("  read sequence %s %s%n", v.getDataType(), v.getShortName());
          Sequence s = (Sequence) v;
          StructureDataIterator orgSeq = s.getStructureIterator(-1);
          Sequence copyv = (Sequence) copy.findVariable(v.getFullName());
          Iterator<StructureData> array = copyv.iterator();
          Formatter f = new Formatter();
          boolean ok1 = TestReadArrayCompare.compareSequence(f, v.getShortName(), orgSeq, array);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;
        }
      }
      assertThat(ok).isTrue();
    }
  }

}

