/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.Iterator;

import ucar.array.Array;
import ucar.array.ArrayVlen;
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
 * Compare reading netcdf with Ma2 and same file with Array. Open separate files to prevent them from colliding.
 * Also use to test round trip through cmdr.
 */
public class CompareArrayToMa2 {

  public static boolean compareFiles(NetcdfFile ma2File, NetcdfFile arrayFile) throws IOException {
    return compareFiles(ma2File, arrayFile, true);
  }

  public static boolean compareFiles(NetcdfFile ma2File, NetcdfFile arrayFile, boolean justOne) throws IOException {
    Stopwatch stopwatchAll = Stopwatch.createStarted();
    // Just the header
    Formatter errlog = new Formatter();
    boolean ok = CompareNetcdf2.compareFiles(ma2File, arrayFile, errlog, false, false, false);
    if (!ok) {
      System.out.printf("FAIL %s %s%n", arrayFile.getLocation(), errlog);
      return false;
    }

    for (Variable v : ma2File.getVariables()) {
      ok &= compareVariable(ma2File, arrayFile, v.getFullName(), justOne);
    }
    System.out.printf("*** took %s%n", stopwatchAll.stop());
    return ok;
  }

  public static boolean compareVariable(NetcdfFile ma2File, NetcdfFile arrayFile, String varName, boolean justOne)
      throws IOException {
    Variable vorg = ma2File.findVariable(varName);
    Variable vnew = arrayFile.findVariable(varName);
    if (vnew == null) {
      System.out.printf("  Cant find variable %s in %s%n", varName, arrayFile.getLocation());
      return false;
    }

    if (vorg.getDataType() == DataType.SEQUENCE) {
      System.out.printf("  read sequence %s %s%n", vorg.getDataType(), vorg.getShortName());
      Sequence s = (Sequence) vnew;
      StructureDataIterator orgSeq = s.getStructureIterator(-1);
      Sequence copyv = (Sequence) arrayFile.findVariable(vorg.getFullName());
      Iterator<ucar.array.StructureData> array = copyv.iterator();
      Formatter f = new Formatter();
      boolean ok1 = CompareArrayToMa2.compareSequence(f, vorg.getShortName(), orgSeq, array);
      if (!ok1) {
        System.out.printf("%s%n", f);
        return false;
      }
    } else {
      ucar.ma2.Array org = vorg.read();
      Array<?> array = vnew.readArray();
      System.out.printf("  read variable %s %s%n", vorg.getDataType(), vorg.getNameAndDimensions());
      Formatter f = new Formatter();
      boolean ok1 = CompareArrayToMa2.compareData(f, vorg.getShortName(), org, array, justOne, true);
      if (!ok1) {
        System.out.printf("%s%n", f);
        return false;
      }
    }
    return true;
  }

  public static boolean compareData(Formatter f, String name, ucar.ma2.Array org, Array<?> array, boolean justOne,
      boolean testTypes) throws IOException {
    boolean ok = true;

    if (org.getSize() != array.length()) {
      f.format(" WARN  %s: data nelems %d !== %d%n", name, org.getSize(), array.length());
      // ok = false;
    }

    if (!Misc.compare(org.getShape(), array.getShape(), f)) {
      f.format(" WARN %s: data shape %s !== %s%n", name, java.util.Arrays.toString(org.getShape()),
          java.util.Arrays.toString(array.getShape()));
      // ok = false;
    }

    if (org.isVlen() != array.isVlen()) {
      f.format(" WARN  %s: data vlen %s !== %s%n", name, org.isVlen(), array.isVlen());
      // ok = false;
    }

    if (testTypes && org.getDataType().getArrayType() != array.getArrayType()) {
      if (array.getArrayType().isEnum()) {
        if (org.getDataType().getArrayType().getSize() != array.getArrayType().getSize()) {
          f.format(" WARN %s: data type %s !== %s%n", name, org.getDataType(), array.getArrayType());
          ok = false;
        }
      } else {
        f.format(" WARN %s: data type %s !== %s%n", name, org.getDataType(), array.getArrayType());
        ok = false;
      }
    }

    if (!ok) {
      return false;
    }

    DataType dt = org.getDataType();
    IndexIterator iter1 = org.getIndexIterator();

    if (dt != DataType.OPAQUE && (org.isVlen() || array.isVlen())) {
      // problem is ma2 is unwrapping scalar Vlens, array is not
      if (!org.isVlen() && array instanceof ArrayVlen && array.length() == 1) {
        ArrayVlen<?> vlen = (ArrayVlen<?>) array;
        array = vlen.get(0);
      }
      Iterator<Object> iter2 = (Iterator<Object>) array.iterator();

      while (iter1.hasNext() && iter2.hasNext()) {
        Object v1 = iter1.getObjectNext();
        Object v2 = iter2.next();
        if (v1 instanceof ucar.ma2.Array && v2 instanceof Array) {
          ok &= compareData(f, name, (ucar.ma2.Array) v1, (Array) v2, justOne, testTypes);
        } else if (!v1.equals(v2)) {
          f.format(" DIFF %s: Vlen %s != %s %n", name, v1, v2);
          ok = false;
          if (justOne)
            break;
        }
      }
      return ok;
    }

    switch (dt) {
      case BYTE:
      case ENUM1:
      case UBYTE: {
        Iterator<Byte> iter2 = (Iterator<Byte>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          byte v1 = iter1.getByteNext();
          byte v2 = iter2.next();
          if (v1 != v2) {
            f.format(makeNumericDataDiffMessage(dt, name, v1, v2, iter1));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }

      case CHAR: {
        Iterator<Byte> iter2 = (Iterator<Byte>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          byte v1 = (byte) iter1.getCharNext();
          byte v2 = iter2.next();
          if (v1 != v2) {
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
            f.format(makeNumericDataDiffMessage(dt, name, v1, v2, iter1));
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
            f.format(makeNumericDataDiffMessage(dt, name, v1, v2, iter1));
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
            f.format(makeNumericDataDiffMessage(dt, name, v1, v2, iter1));
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
            f.format(makeNumericDataDiffMessage(dt, name, v1, v2, iter1));
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
          Array<Byte> v2 = (Array<Byte>) iter2.next();
          if (v1.remaining() != v2.length()) {
            f.format(" DIFF %s: opaque sizes differ %d != %d%n", name, v1.remaining(), v2.length());
            ok = false;
          }
          for (int idx = 0; idx < v1.remaining() && idx < v2.length(); idx++) {
            if (v1.get(idx) != v2.get(idx)) {
              f.format(makeNumericDataDiffMessage(dt, name, v1.get(idx), v2.get(idx), iter1));
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
            f.format(makeNumericDataDiffMessage(dt, name, v1, v2, iter1));
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
          if ((v1 == null) != (v2 == null)) {
            f.format(" DIFF string %s: null %s != %s count=%s%n", name, (v1 == null), (v2 == null), iter1);
            ok = false;
          }
          if (v1 != null && !v1.equals(v2)) {
            f.format(" DIFF string %s: %s != %s count=%s%n", name, v1, v2, iter1);
            ok = false;
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

  private static String makeNumericDataDiffMessage(DataType dt, String name, Number v1, Number v2, IndexIterator iter) {
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
      Array<?> data2 = array.getMemberData(m2);
      if (data2 != null) {
        f.format("    compare member %s %s%n", m1.getDataType(), m1.getName());
        ok &= compareData(f, m1.getName(), data1, data2, justOne, false);
      }
    }

    return ok;
  }

  public static boolean compareSequence(Formatter f, String name, StructureDataIterator org,
      Iterator<ucar.array.StructureData> array) throws IOException {
    boolean ok = true;
    int obsrow = 0;
    System.out.printf(" compareSequence %s%n", name);
    while (org.hasNext() && array.hasNext()) {
      ok &= compareStructureData(f, org.next(), array.next(), false);
      obsrow++;
    }
    return ok;
  }

}

