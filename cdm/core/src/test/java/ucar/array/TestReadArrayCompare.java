/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.array;

import java.io.FileFilter;
import java.io.IOException;
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
import ucar.ma2.StructureData;
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
    List<Object[]> result = new ArrayList<Object[]>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf3/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/samples/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/support/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/complex/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/wrf/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/xmdf/", ff, result);
      // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/hdf5/", ff, result, false);
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
      boolean testTypes) {
    boolean ok = true;
    if (org.getSize() != array.length()) {
      f.format(" DIFF %s: data size %d !== %d%n", name, org.getSize(), array.length());
      ok = false;
    }

    if (testTypes && org.getDataType() != array.getDataType()) {
      f.format(" DIFF %s: data type %s !== %s%n", name, org.getDataType(), array.getDataType());
      ok = false;
    }

    if (!Misc.compare(org.getShape(), array.getShape(), f)) {
      f.format(" DIFF %s: data shape %s !== %s%n", name, java.util.Arrays.toString(org.getShape()),
          Arrays.toString(array.getShape()));
      ok = false;
    }

    if (!ok) {
      return false;
    }

    DataType dt = org.getDataType();
    IndexIterator iter1 = org.getIndexIterator();

    if (dt == DataType.BYTE || dt == DataType.UBYTE || dt == DataType.ENUM1) {
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

    } else if (dt == DataType.CHAR) {
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

    } else if (dt == DataType.DOUBLE) {
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

    } else if (dt == DataType.FLOAT) {
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

    } else if (dt == DataType.INT || dt == DataType.UINT || dt == DataType.ENUM4) {
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

    } else if (dt == DataType.LONG || dt == DataType.ULONG) {
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

    } else if (dt == DataType.SHORT || dt == DataType.USHORT || dt == DataType.ENUM2) {
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

    } else if (dt == DataType.STRING) {
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

    } else if (dt == DataType.STRUCTURE) {
      Iterator<ucar.array.StructureData> iter2 = (Iterator<ucar.array.StructureData>) array.iterator();
      while (iter1.hasNext() && iter2.hasNext()) {
        compareStructureData(f, (StructureData) iter1.next(), iter2.next(), justOne);
      }

    } else {
      ok = false;
      f.format(" %s: Unknown data type %s%n", name, org.getDataType());
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
      boolean justOne) {
    boolean ok = true;

    StructureMembers sm1 = org.getStructureMembers();
    ucar.array.StructureMembers sm2 = array.getStructureMembers();
    if (sm1.getMembers().size() != sm2.getMembers().size()) {
      f.format(" size %d !== %d%n", sm1.getMembers().size(), sm2.getMembers().size());
      ok = false;
    }

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      ucar.array.StructureMembers.Member m2 = sm2.findMember(m1.getName());
      ucar.ma2.Array data1 = org.getArray(m1);
      ucar.array.Array<?> data2 = array.getMemberData(m2);
      if (data2 != null) {
        System.out.printf("    compare variable %s %s%n", m1.getDataType(), m1.getName());
        ok &= compareData(f, m1.getName(), data1, data2, justOne, true);
      }
    }

    return ok;
  }

}

