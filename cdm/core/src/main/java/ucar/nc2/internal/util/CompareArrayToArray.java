/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.Formatter;

import ucar.ma2.Array;
import ucar.ma2.ArraySequence;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.util.Misc;

/**
 * Compare reading netcdf with Array. Open separate files to prevent them from colliding.
 * Also use to test round trip through cmdr.
 */
public class CompareArrayToArray {

  public static boolean compareFiles(NetcdfFile arrayFile, NetcdfFile arrayFile2) throws IOException {
    Stopwatch stopwatchAll = Stopwatch.createStarted();
    // Just the header
    Formatter errlog = new Formatter();
    boolean ok = CompareNetcdf2.compareFiles(arrayFile, arrayFile2, errlog, false, false, false);
    if (!ok) {
      System.out.printf("FAIL %s %s%n", arrayFile.getLocation(), errlog);
      return false;
    }

    for (Variable v : arrayFile.getVariables()) {
      ok &= compareVariable(arrayFile, arrayFile2, v.getFullName(), false);
    }
    System.out.printf("*** took %s%n", stopwatchAll.stop());
    return ok;
  }

  public static boolean compareVariable(NetcdfFile arrayFile1, NetcdfFile arrayFile2, String varName, boolean justOne)
      throws IOException {
    boolean ok = true;

    Variable vorg = arrayFile1.findVariable(varName);
    if (vorg == null) {
      System.out.printf("  Cant find variable %s in original %s%n", varName, arrayFile1.getLocation());
      return false;
    }
    Variable vnew = arrayFile2.findVariable(varName);
    if (vnew == null) {
      System.out.printf("  Cant find variable %s in copy %s%n", varName, arrayFile2.getLocation());
      return false;
    }

    if (vorg.getDataType() == DataType.SEQUENCE) {
      System.out.printf("  read sequence %s %s%n", vorg.getDataType(), vorg.getShortName());
      Sequence s = (Sequence) vorg;
      StructureDataIterator orgSeq = s.getStructureIterator(-1);
      Sequence copyv = (Sequence) vnew;
      StructureDataIterator array = copyv.getStructureIterator(-1);
      Formatter f = new Formatter();
      boolean ok1 = compareSequence(f, vorg.getShortName(), orgSeq, array);
      if (!ok1) {
        System.out.printf("%s%n", f);
      }
      ok &= ok1;

    } else {
      long size = vorg.getSize();
      if (size < Integer.MAX_VALUE) {
        Array org = vorg.read();
        Array array = vnew.read();
        System.out.printf("  compareData %s %s%n", vorg.getDataType(), vorg.getNameAndDimensions());
        Formatter f = new Formatter();
        boolean ok1 = compareData(f, vorg.getShortName(), org, array, justOne, true);
        if (!ok1) {
          System.out.printf("%s%n", f);
        }
        ok &= ok1;
      }
    }

    return ok;
  }

  public static boolean compareData(String name, Array org, Array array) throws IOException {
    Formatter f = new Formatter();
    boolean ok = compareData(f, name, org, array, false, true);
    if (f.toString().isEmpty()) {
      System.out.printf("%s%n", f);
    }
    return ok;
  }

  public static boolean compareData(Formatter f, String name, Array org, Array array, boolean justOne,
      boolean testTypes) throws IOException {
    boolean ok = true;

    if (org.getSize() != array.getSize()) {
      f.format(" WARN  %s: data nelems %d !== %d%n", name, org.getSize(), array.getSize());
      // ok = false;
    }

    if (org.getDataType() != array.getDataType()) {
      f.format(" WARN  %s: dataType %s !== %s%n", name, org.getDataType(), array.getDataType());
      // ok = false;
    }

    if (!Misc.compare(org.getShape(), array.getShape(), f)) {
      f.format(" WARN %s: data shape %s !== %s%n", name, java.util.Arrays.toString(org.getShape()),
          java.util.Arrays.toString(array.getShape()));
      // ok = false;
    }

    if (org.isVlen() != array.isVlen()) {
      f.format(" WARN %s: vlens dont match %s !~= %s%n", name, org.isVlen(), array.isVlen());
      ok = false;
    }

    if (!ok) {
      return false;
    }

    DataType dt = org.getDataType();

    switch (dt) {
      case CHAR:
      case OPAQUE:
      case BYTE:
      case ENUM1:
      case UBYTE: {
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          byte v1 = iter1.getByteNext();
          byte v2 = iter2.getByteNext();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case DOUBLE: {
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          double v1 = iter1.getDoubleNext();
          double v2 = iter2.getDoubleNext();
          if (!Misc.nearlyEquals(v1, v2)) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case FLOAT: {
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          float v1 = iter1.getFloatNext();
          float v2 = iter2.getFloatNext();
          if (!Misc.nearlyEquals(v1, v2)) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
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
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          int v1 = iter1.getIntNext();
          int v2 = iter2.getIntNext();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case LONG:
      case ULONG: {
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          long v1 = iter1.getLongNext();
          long v2 = iter2.getLongNext();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      /*
       * case OPAQUE: {
       * Iterator<Object> iter1 = (Iterator<Object>) org.iterator();
       * Iterator<Object> iter2 = (Iterator<Object>) array.iterator();
       * while (iter1.hasNext() && iter2.hasNext()) {
       * // Weve already unwrapped the VLEN part.
       * Array<Byte> v1 = (Array<Byte>) iter1.next();
       * Array<Byte> v2 = (Array<Byte>) iter2.next();
       * if (v1.length() != v2.length()) {
       * f.format(" DIFF %s: opaque sizes differ %d != %d%n", name, v1.length(), v2.length());
       * ok = false;
       * }
       * for (int idx = 0; idx < v1.length() && idx < v2.length(); idx++) {
       * if (!v1.get(idx).equals(v2.get(idx))) {
       * f.format(createNumericDataDiffMessage(dt, name, v1.get(idx), v2.get(idx), idx));
       * ok = false;
       * if (justOne)
       * break;
       * }
       * }
       * }
       * break;
       * }
       */

      case SHORT:
      case ENUM2:
      case USHORT: {
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          short v1 = iter1.getShortNext();
          short v2 = iter2.getShortNext();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case STRING: {
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          String v1 = (String) iter1.getObjectNext();
          String v2 = (String) iter2.getObjectNext();
          if (!v1.equals(v2)) {
            f.format(" DIFF string %s: %s != %s count=%s%n", name, v1, v2, 0);
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }


      case SEQUENCE: {
        StructureDataIterator iter1 = ((ArraySequence) org).getStructureDataIterator();
        StructureDataIterator iter2 = ((ArraySequence) array).getStructureDataIterator();
        int row = 0;
        while (iter1.hasNext() && iter2.hasNext()) {
          ok &= compareStructureData(f, iter1.next(), iter2.next(), justOne);
          row++;
        }
        break;
      }

      case STRUCTURE: {
        IndexIterator iter1 = org.getIndexIterator();
        IndexIterator iter2 = array.getIndexIterator();
        int row = 0;
        while (iter1.hasNext() && iter2.hasNext()) {
          ok &= compareStructureData(f, (StructureData) iter1.next(), (StructureData) iter2.next(), justOne);
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

  private static String createNumericDataDiffMessage(DataType dt, String name, Number v1, Number v2, int idx) {
    return String.format(" DIFF %s %s: %s != %s;  count = %s, absDiff = %s, relDiff = %s %n", dt, name, v1, v2, idx,
        Misc.absoluteDifference(v1.doubleValue(), v2.doubleValue()),
        Misc.relativeDifference(v1.doubleValue(), v2.doubleValue()));
  }

  private static boolean compareStructureData(Formatter f, StructureData org, StructureData array, boolean justOne)
      throws IOException {
    boolean ok = true;

    StructureMembers sm1 = org.getStructureMembers();
    StructureMembers sm2 = array.getStructureMembers();
    if (sm1.getMembers().size() != sm2.getMembers().size()) {
      f.format(" membersize %d !== %d%n", sm1.getMembers().size(), sm2.getMembers().size());
      ok = false;
    }

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      StructureMembers.Member m2 = sm2.findMember(m1.getName());
      if (m2 == null) {
        System.out.printf("Cant find %s in copy%n", m1.getName());
        continue;
      }
      Array data1 = org.getArray(m1);
      Array data2 = array.getArray(m2);
      if (data2 != null) {
        f.format("    compare member %s %s%n", m1.getDataType(), m1.getName());
        ok &= compareData(f, m1.getName(), data1, data2, justOne, false);
      }
    }

    return ok;
  }

  public static boolean compareSequence(Formatter f, String name, StructureDataIterator org,
      StructureDataIterator array) throws IOException {
    boolean ok = true;
    int obsrow = 0;
    System.out.printf(" compareSequence %s%n", name);
    while (org.hasNext() && array.hasNext()) {
      ok &= compareStructureData(f, org.next(), array.next(), false);
      obsrow++;
    }
    if (org.hasNext() != array.hasNext()) {
      System.out.printf(" mismatch length of sequence %s row %d: %s != %s%n", name, obsrow, org.hasNext(),
          array.hasNext());
      ok = false;
    }
    return ok;
  }

}

