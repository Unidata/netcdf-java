/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.array;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.DataType;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare reading netcdf with Array */
@Category(NeedsCdmUnitTest.class)
public class TestReadArrayProblem {

  @Test
  public void testOpaque() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc"; // opaque
    compareArrays(filename);
  }

  @Test
  public void testHdf4() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF";
    compareArrays(filename);
  }

  @Test
  public void testStructureWithChar() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/support/cstr.h5";
    compareArrays(filename);
  }

  @Test
  public void testStructure() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/wrf/wrf_bdy_par.h5";
    compareArrays(filename);
  }

  @Test
  public void testBufrUncompressed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/MSSARG_00217064.bufr";
    compareSequence(filename);
  }

  @Test
  public void testBufrUncompressed2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/5900.20030601.rass";
    compareSequence(filename);
  }

  @Test
  public void testBufrCompressed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/WMO_v16_3-10-61.bufr";
    compareSequence(filename);
  }

  @Test
  public void testBufrUncompressedNestedStructure() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    compareSequence(filename);
  }

  @Test
  public void testBufrCompressedNestedStructure() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/TimeIncr0.bufr";
    compareSequence(filename);
  }

  private void compareSequence(String filename) throws IOException {
    try (NetcdfFile org = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
        NetcdfFile copy = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.println("Test input: " + org.getLocation());

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

  private void compareArrays(String filename) throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.println("Test input: " + ncfile.getLocation());

      boolean ok = true;
      for (Variable v : ncfile.getVariables()) {
        System.out.printf("  read variable %s %s", v.getDataType(), v.getShortName());
        ucar.ma2.Array org = v.read();
        try {
          Array<?> array = v.readArray();
          if (array != null) {
            System.out.printf("  COMPARE%n");
            Formatter f = new Formatter();
            boolean ok1 = TestReadArrayCompare.compareData(f, v.getShortName(), org, array, false, true);
            if (!ok1) {
              System.out.printf("%s%n", f);
            }
            ok &= ok1;
          } else {
            System.out.printf("%n");
          }
        } catch (Exception e) {
          System.out.printf(" BAD%n");
          e.printStackTrace();
          ok = false;
        }
      }
      Assert.assertTrue(filename, ok);
    }
  }
}

