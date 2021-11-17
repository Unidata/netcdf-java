/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.ArrayVlen;
import ucar.array.Arrays;

import java.math.BigInteger;
import java.util.List;

/** Static helper methods for Aggregations. */
class Aggregations {

  // TODO unsigned?
  static Array<?> makeArray(ArrayType dtype, List<String> svals) throws NumberFormatException {
    int n = svals.size();
    int[] shape = new int[] {n};
    int count = 0;

    switch (dtype) {
      case STRING: {
        return Arrays.factory(dtype, shape, svals.toArray(new String[0]));
      }
      case OPAQUE: {
        byte[][] dataArray = new byte[n][];
        for (String s : svals) {
          dataArray[count++] = s.getBytes();
        }
        return ArrayVlen.factory(dtype, shape, dataArray);

      }
      case ULONG: {
        long[] dataArray = new long[n];
        for (String s : svals) {
          BigInteger biggy = new BigInteger(s);
          dataArray[count++] = biggy.longValue(); // > 63 bits will become "negetive".
        }
        return Arrays.factory(dtype, shape, dataArray);
      }
      case LONG: {
        long[] dataArray = new long[n];
        for (String s : svals) {
          dataArray[count++] = Long.parseLong(s);
        }
        return Arrays.factory(dtype, shape, dataArray);
      }
      case DOUBLE: {
        double[] dataArray = new double[n];
        for (String s : svals) {
          dataArray[count++] = Double.parseDouble(s);
        }
        return Arrays.factory(dtype, shape, dataArray);
      }
      case FLOAT: {
        float[] dataArray = new float[n];
        for (String s : svals) {
          dataArray[count++] = Float.parseFloat(s);
        }
        return Arrays.factory(dtype, shape, dataArray);
      }
      case UBYTE:
      case ENUM1: {
        byte[] darray = new byte[n];
        for (String s : svals) {
          darray[count++] = (byte) Integer.parseInt(s);
        }
        return Arrays.factory(dtype, shape, darray);
      }
      case CHAR:
      case BYTE: {
        byte[] darray = new byte[n];
        for (String s : svals) {
          darray[count++] = Byte.parseByte(s);
        }
        return Arrays.factory(dtype, shape, darray);
      }
      case UINT:
      case ENUM4: {
        int[] darray = new int[n];
        for (String s : svals) {
          darray[count++] = (int) Long.parseLong(s);
        }
        return Arrays.factory(dtype, shape, darray);
      }
      case INT: {
        int[] darray = new int[n];
        for (String s : svals) {
          darray[count++] = Integer.parseInt(s);
        }
        return Arrays.factory(dtype, shape, darray);
      }
      case USHORT:
      case ENUM2: {
        short[] darray = new short[n];
        for (String s : svals) {
          darray[count++] = (short) Integer.parseInt(s);
        }
        return Arrays.factory(dtype, shape, darray);
      }
      case SHORT: {
        short[] darray = new short[n];
        for (String s : svals) {
          darray[count++] = Short.parseShort(s);
        }
        return Arrays.factory(dtype, shape, darray);
      }
    }
    throw new IllegalArgumentException("Aggregations makeArray unsupported dataype " + dtype);
  }

  /**
   * Convert original array to desired type.
   *
   * @param org original array, must be a numeric type
   * @param wantType desired type, must be a numeric type.
   * @return converted data of desired type, or original array if already wantType or it is not numeric.
   */
  public static Array<?> convert(Array<?> org, ArrayType wantType) {
    if (org == null || (org.getArrayType() == wantType) || !wantType.isNumeric()) {
      return org;
    }
    Array<Number> orgn = (Array<Number>) org;
    int n = (int) org.getSize();
    int count = 0;

    switch (wantType) {
      case CHAR:
      case ENUM1:
      case UBYTE:
      case BYTE: {
        byte[] darray = new byte[n];
        for (Number val : orgn) {
          darray[count++] = val.byteValue();
        }
        return Arrays.factory(wantType, org.getShape(), darray);
      }
      case DOUBLE: {
        double[] darray = new double[n];
        for (Number val : orgn) {
          darray[count++] = val.doubleValue();
        }
        return Arrays.factory(wantType, org.getShape(), darray);
      }
      case FLOAT: {
        float[] darray = new float[n];
        for (Number val : orgn) {
          darray[count++] = val.floatValue();
        }
        return Arrays.factory(wantType, org.getShape(), darray);
      }
      case UINT:
      case ENUM4:
      case INT: {
        int[] darray = new int[n];
        for (Number val : orgn) {
          darray[count++] = val.intValue();
        }
        return Arrays.factory(wantType, org.getShape(), darray);
      }
      case ULONG:
      case LONG: {
        long[] darray = new long[n];
        for (Number val : orgn) {
          darray[count++] = val.longValue();
        }
        return Arrays.factory(wantType, org.getShape(), darray);
      }
      case USHORT:
      case ENUM2:
      case SHORT: {
        short[] darray = new short[n];
        for (Number val : orgn) {
          darray[count++] = val.shortValue();
        }
        return Arrays.factory(wantType, org.getShape(), darray);
      }
    }
    throw new IllegalArgumentException("Aggregations convert unsupported dataype " + wantType);
  }
}
