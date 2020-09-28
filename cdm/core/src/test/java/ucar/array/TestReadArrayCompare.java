/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.array;

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
import ucar.ma2.ArraySequence;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare reading netcdf with Array */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadArrayCompare {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf3/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/samples/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/support/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/complex/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/wrf/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/xmdf/", ff, result);
      // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/", ff, result, false);

      result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc"});
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadArrayCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void compareArrays() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      System.out.println("Test input: " + ncfile.getLocation());

      boolean ok = true;
      for (Variable v : ncfile.getVariables()) {
        ucar.ma2.Array org = v.read();
        Array<?> array = v.readArray();
        if (array != null) {
          System.out.printf("  check %s %s%n", v.getDataType(), v.getNameAndDimensions());
          Formatter f = new Formatter();
          boolean ok1 = TestReadArrayCompare.compareData(f, v.getShortName(), org, array, false, true);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;
        }
      }
      Assert.assertTrue(filename, ok);
    }
  }

  static boolean compareData(Formatter f, String name, ucar.ma2.Array org, ucar.array.Array<?> array, boolean justOne,
      boolean testTypes) throws IOException {
    boolean ok = true;

    if (org.getSize() != array.length()) {
      f.format(" WARN  %s: data nelems %d !== %d%n", name, org.getSize(), array.length());
      // ok = false;
    }

    if (!Misc.compare(org.getShape(), array.getShape(), f)) {
      f.format(" WARN shape %s: data shape %s !== %s%n", name, java.util.Arrays.toString(org.getShape()),
          Arrays.toString(array.getShape()));
      // ok = false;
    }

    if (array.getDataType() == DataType.VLEN) {
      testTypes = false;
    }
    if (testTypes && org.getDataType() != array.getDataType()) {
      f.format(" WARN %s: data type %s !== %s%n", name, org.getDataType(), array.getDataType());
      ok = false;
    }

    if (!ok) {
      return false;
    }

    DataType dt = org.getDataType();
    IndexIterator iter1 = org.getIndexIterator();

    switch (dt) {
      case BYTE:
      case ENUM1:
      case UBYTE: {
        Iterator<Byte> iter2 = (Iterator<Byte>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          byte v1 = iter1.getByteNext();
          byte v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case CHAR: {
        Iterator<Character> iter2 = (Iterator<Character>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          char v1 = iter1.getCharNext();
          char v2 = iter2.next();
          if (!Misc.nearlyEquals(v1, v2)) {
            f.format(" DIFF %s %s: %s != %s;  count = %s%n", dt, name, v1, v2, iter1);
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case DOUBLE: {
        Iterator<Double> iter2 = (Iterator<Double>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          double v1 = iter1.getDoubleNext();
          double v2 = iter2.next();
          if (!Misc.nearlyEquals(v1, v2)) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case FLOAT: {
        Iterator<Float> iter2 = (Iterator<Float>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          float v1 = iter1.getFloatNext();
          float v2 = iter2.next();
          if (!Misc.nearlyEquals(v1, v2)) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case INT:
      case ENUM4:
      case UINT: {
        Iterator<Integer> iter2 = (Iterator<Integer>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          int v1 = iter1.getIntNext();
          int v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case LONG:
      case ULONG: {
        Iterator<Long> iter2 = (Iterator<Long>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          long v1 = iter1.getLongNext();
          long v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case OPAQUE: {
        Iterator<Object> iter2 = (Iterator<Object>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          ByteBuffer v1 = (ByteBuffer) iter1.getObjectNext();
          v1.rewind();
          byte[] v2 = (byte[]) iter2.next();
          if (v1.remaining() != v2.length) {
            f.format(" DIFF %s: opaque sizes differ %d != %d%n", name, v1.remaining(), v2.length);
            ok = false;
          }
          for (int idx = 0; idx < v1.remaining() && idx < v2.length; idx++) {
            if (v1.get(idx) != v2[idx]) {
              f.format(createNumericDataDiffMessage(dt, name, v1.get(idx), v2[idx], iter1));
              ok = false;
              if (justOne)
                break;
            }
          }
        }
        break;
      }

      case SHORT:
      case ENUM2:
      case USHORT: {
        Iterator<Short> iter2 = (Iterator<Short>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          short v1 = iter1.getShortNext();
          short v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case STRING: {
        Iterator<String> iter2 = (Iterator<String>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          String v1 = (String) iter1.getObjectNext();
          String v2 = iter2.next();
          if (!v1.equals(v2)) {
            f.format(" DIFF string %s: %s != %s count=%s%n", name, v1, v2, iter1);
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }


      case SEQUENCE: {
        ArraySequence orgSeq = (ArraySequence) org;
        StructureDataIterator sditer = orgSeq.getStructureDataIterator();
        Iterator<ucar.array.StructureData> iter2 = (Iterator<ucar.array.StructureData>) array.iterator();
        int row = 0;
        while (sditer.hasNext() && iter2.hasNext()) {
          ok &= compareStructureData(f, sditer.next(), iter2.next(), justOne);
          row++;
        }
        break;
      }

      case STRUCTURE: {
        Iterator<ucar.array.StructureData> iter2 = (Iterator<ucar.array.StructureData>) array.iterator();
        int row = 0;
        while (iter1.hasNext() && iter2.hasNext()) {
          ok &= compareStructureData(f, (StructureData) iter1.next(), iter2.next(), justOne);
          row++;
        }
        break;
      }

      default: {
        ok = false;
        f.format(" %s: Unknown data type %s%n", name, org.getDataType());
      }
    }

    return ok;
  }

  private static String createNumericDataDiffMessage(DataType dt, String name, Number v1, Number v2,
      IndexIterator iter) {
    return String.format(" DIFF %s %s: %s != %s;  count = %s, absDiff = %s, relDiff = %s %n", dt, name, v1, v2, iter,
        Misc.absoluteDifference(v1.doubleValue(), v2.doubleValue()),
        Misc.relativeDifference(v1.doubleValue(), v2.doubleValue()));
  }

  private static boolean compareStructureData(Formatter f, StructureData org, ucar.array.StructureData array,
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
      if (data2 != null) {
        f.format("    compare member %s %s%n", m1.getDataType(), m1.getName());
        ok &= compareData(f, m1.getName(), data1, data2, justOne, false);
      }
    }

    return ok;
  }

  static boolean compareSequence(Formatter f, String name, StructureDataIterator org,
      Iterator<ucar.array.StructureData> array) throws IOException {
    boolean ok = true;
    int obsrow = 0;
    while (org.hasNext() && array.hasNext()) {
      ok &= compareStructureData(f, org.next(), array.next(), false);
      obsrow++;
    }
    return ok;
  }

}

