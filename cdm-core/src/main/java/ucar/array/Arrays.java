/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import ucar.nc2.util.Misc;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

/** Static helper classes for {@link Array} */
public class Arrays {

  private Arrays() {}

  /**
   * Create Array using java array of T, or java primitive array, as storage.
   * Do not use this for Vlens or Structures.
   *
   * @param dataType data type of the data. Vlen detected from the shape.
   * @param shape multidimensional shape, must have same total length as dataArray.
   * @param storage storage for type T.
   */
  public static <T> Array<T> factory(ArrayType dataType, int[] shape, Storage<T> storage) {
    switch (dataType) {
      case OPAQUE:
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case ENUM1:
      case UBYTE: {
        return (Array<T>) new ArrayByte(dataType, shape, (Storage<Byte>) storage);
      }
      case DOUBLE: {
        return (Array<T>) new ArrayDouble(shape, (Storage<Double>) storage);
      }
      case FLOAT: {
        return (Array<T>) new ArrayFloat(shape, (Storage<Float>) storage);
      }
      case INT:
      case ENUM4:
      case UINT: {
        return (Array<T>) new ArrayInteger(dataType, shape, (Storage<Integer>) storage);
      }
      case LONG:
      case ULONG: {
        return (Array<T>) new ArrayLong(dataType, shape, (Storage<Long>) storage);
      }
      case SHORT:
      case ENUM2:
      case USHORT: {
        return (Array<T>) new ArrayShort(dataType, shape, (Storage<Short>) storage);
      }
      case STRING: {
        return (Array<T>) new ArrayString(shape, (Storage<String>) storage);
      }
      default:
        throw new RuntimeException("Unimplemented ArrayType " + dataType);
    }
  }

  /**
   * Create Array using java array of T, or java primitive array, as storage.
   * Do not use this for Vlens or Structures.
   *
   * @param dataType data type of the data. Vlen detected from the shape.
   * @param shape multidimensional shape, must have same total length as dataArray.
   * @param dataArray must be java array of T, or java primitive array
   */
  public static <T> Array<T> factory(ArrayType dataType, int[] shape, Object dataArray) {
    switch (dataType) {
      case CHAR:
        if (dataArray instanceof char[]) {
          dataArray = ArraysConvert.convertCharToByte((char[]) dataArray);
        }
        // fall through
      case OPAQUE:
      case BOOLEAN:
      case BYTE:
      case ENUM1:
      case UBYTE: {
        Storage<Byte> storageS = new ArrayByte.StorageS((byte[]) dataArray);
        return (Array<T>) new ArrayByte(dataType, shape, storageS);
      }
      case DOUBLE: {
        Storage<Double> storageD = new ArrayDouble.StorageD((double[]) dataArray);
        return (Array<T>) new ArrayDouble(shape, storageD);
      }
      case FLOAT: {
        Storage<Float> storageF = new ArrayFloat.StorageF((float[]) dataArray);
        return (Array<T>) new ArrayFloat(shape, storageF);
      }
      case INT:
      case ENUM4:
      case UINT: {
        Storage<Integer> storageS = new ArrayInteger.StorageS((int[]) dataArray);
        return (Array<T>) new ArrayInteger(dataType, shape, storageS);
      }
      case LONG:
      case ULONG: {
        Storage<Long> storageS = new ArrayLong.StorageS((long[]) dataArray);
        return (Array<T>) new ArrayLong(dataType, shape, storageS);
      }
      case SHORT:
      case ENUM2:
      case USHORT: {
        Storage<Short> storageS = new ArrayShort.StorageS((short[]) dataArray);
        return (Array<T>) new ArrayShort(dataType, shape, storageS);
      }
      case STRING: {
        Storage<String> storageS = new ArrayString.StorageS((String[]) dataArray);
        return (Array<T>) new ArrayString(shape, storageS);
      }
      default:
        throw new RuntimeException("Unimplemented ArrayType= " + dataType);
    }
  }

  /**
   * Create Array using empty java array of T, or java primitive array, same size as shape.
   * Do not use this for Vlens or Structures.
   * LOOK what is this used for?
   *
   * @param shape multidimensional shape
   */
  public static <T> Array<T> factory(ArrayType dataType, int[] shape) {
    switch (dataType) {
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case ENUM1:
      case UBYTE: {
        return (Array<T>) new ArrayByte(dataType, shape);
      }
      case DOUBLE: {
        return (Array<T>) new ArrayDouble(shape);
      }
      case FLOAT: {
        return (Array<T>) new ArrayFloat(shape);
      }
      case INT:
      case ENUM4:
      case UINT: {
        return (Array<T>) new ArrayInteger(dataType, shape);
      }
      case LONG:
      case ULONG: {
        return (Array<T>) new ArrayLong(dataType, shape);
      }
      case SHORT:
      case ENUM2:
      case USHORT: {
        return (Array<T>) new ArrayShort(dataType, shape);
      }
      case STRING: {
        return (Array<T>) new ArrayString(shape);
      }
      default:
        throw new RuntimeException("Unimplemented ArrayType " + dataType);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Experimental

  /**
   * Combine list of Array's by copying the underlying Array's into a single primitive array
   */
  public static <T> Array<T> factoryCopy(ArrayType dataType, int[] shape, List<Array<T>> dataArrays) {
    if (dataArrays.size() == 1) {
      return factory(dataType, shape, dataArrays.get(0).storage());
    }
    Object dataArray = combine(dataType, shape, dataArrays);
    return factory(dataType, shape, dataArray);
  }

  private static <T> Object combine(ArrayType dataType, int[] shape, List<Array<T>> dataArrays) {
    long size = Arrays.computeSize(shape);
    if (size > Integer.MAX_VALUE) {
      throw new OutOfMemoryError();
    }
    Object all;

    switch (dataType) {
      case BYTE:
      case ENUM1:
      case UBYTE:
        all = new byte[(int) size];
        break;
      case CHAR:
        all = new char[(int) size];
        break;
      case DOUBLE:
        all = new double[(int) size];
        break;
      case FLOAT:
        all = new float[(int) size];
        break;
      case INT:
      case ENUM4:
      case UINT:
        all = new int[(int) size];
        break;
      case LONG:
      case ULONG:
        all = new long[(int) size];
        break;
      case SHORT:
      case ENUM2:
      case USHORT:
        all = new short[(int) size];
        break;
      case STRING:
        all = new String[(int) size];
        break;
      default:
        throw new RuntimeException(" ArrayType " + dataType);
    }

    int start = 0;
    for (Array<?> dataArray : dataArrays) {
      dataArray.arraycopy(0, all, start, dataArray.length());
      start += dataArray.length();
    }
    return all;
  }

  // The only advantage over copying AFAICT is that it can handle arrays > 2G. as long as its broken up into
  // multiple arrays < 2G.

  /**
   * Experimental: keep list of Arrays separate. This allows length > 2Gb.
   */
  public static <T> Array<T> factoryArrays(ArrayType dataType, int[] shape, List<Array<?>> dataArrays) {
    if (dataArrays.size() == 1) {
      return factory(dataType, shape, (Storage<T>) dataArrays.get(0).storage());
    }

    switch (dataType) {
      case DOUBLE:
        Storage<Double> storageD = new ArrayDouble.StorageDM(dataArrays);
        return (Array<T>) new ArrayDouble(shape, storageD);
      case FLOAT:
        Storage<Float> storageF = new ArrayFloat.StorageFM(dataArrays);
        return (Array<T>) new ArrayFloat(shape, storageF);
      default:
        throw new RuntimeException();
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////

  /**
   * Create a new Array using same backing store as the org Array, by
   * flipping the index so that it runs from shape[index]-1 to 0.
   *
   * @param org original Array
   * @param dim dimension to flip
   * @return the new Array
   */
  public static <T> Array<T> flip(Array<T> org, int dim) {
    return org.createView(org.indexFn().flip(dim));
  }

  /**
   * Create a new Array using same backing store as the org Array, by
   * permuting the indices.
   *
   * @param org original Array
   * @param dims the old index dims[k] becomes the new kth index.
   * @return the new Array
   */
  public static <T> Array<T> permute(Array<T> org, int[] dims) {
    return org.createView(org.indexFn().permute(dims));
  }

  /**
   * Create a new Array using same backing store with given shape
   *
   * @param org original Array
   * @param shape the new shape
   * @return the new Array
   */
  public static <T> Array<T> reshape(Array<T> org, int[] shape) {
    return org.createView(org.indexFn().reshape(shape));
  }

  /**
   * Create a new Array using same backing store as the org Array, by
   * eliminating any dimensions with length one.
   *
   * @param org original Array
   * @return the new Array, or the same Array if no reduction was done
   */
  public static <T> Array<T> reduce(Array<T> org) {
    IndexFn ri = org.indexFn().reduce();
    if (ri == org.indexFn())
      return org;
    return org.createView(ri);
  }

  /**
   * Create a new Array using same backing store as the org Array, by
   * eliminating the specified dimension.
   *
   * @param org original Array
   * @param dim dimension to eliminate: must be of length one.
   * @return the new Array, or the same Array if no reduction was done
   */
  public static <T> Array<T> reduce(Array<T> org, int dim) {
    IndexFn ri = org.indexFn().reduce(dim);
    if (ri == org.indexFn())
      return org;
    return org.createView(ri);
  }

  /**
   * Create a new Array as a subsection of the org Array, without rank reduction.
   * No data is moved, so the new Array references the same backing store as the original.
   * Vlen is transferred over unchanged.
   *
   * @param org original Array
   * @param section list of Ranges that specify the array subset.
   *        Must be same rank as original Array.
   *        A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
   * @return the new Array
   */
  public static <T> Array<T> section(Array<T> org, Section section) throws InvalidRangeException {
    return org.createView(org.indexFn().section(section));
  }

  /**
   * Create a new Array using same backing store as the org Array, by
   * fixing the specified dimension at the specified index value. This reduces rank by 1.
   *
   * @param org original Array
   * @param dim which dimension to fix
   * @param value at what index value
   * @return a new Array, with reduced rank.
   */
  public static <T> Array<T> slice(Array<T> org, int dim, int value) throws InvalidRangeException {
    // all nulls except the specified dimension
    Section.Builder sb = Section.builder();
    for (int i = 0; i < org.getSection().getRank(); i++) {
      sb.appendRange(null);
    }
    sb.replaceRange(dim, new Range(value, value));
    Array<T> s = section(org, sb.build());
    return reduce(s, dim);
  }

  /*
   * public static <T> Array<T> slice(Array<T> org, int dim, int value) {
   * int[] origin = new int[rank];
   * int[] shape = getShape();
   * origin[dim] = value;
   * shape[dim] = 1;
   * try {
   * return sectionNoReduce(origin, shape, null).reduce(dim); // preserve other dim 1
   * } catch (InvalidRangeException e) {
   * throw new IllegalArgumentException();
   * }
   * }
   */

  /**
   * Create a new Array using same backing store as the org Array, by
   * transposing two of the indices.
   *
   * @param org original Array
   * @param dim1 transpose these two indices
   * @param dim2 transpose these two indices
   * @return the new Array
   */
  public static <T> Array<T> transpose(Array<T> org, int dim1, int dim2) {
    return org.createView(org.indexFn().transpose(dim1, dim2));
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Compute total number of elements in the array. Stop at vlen.
   *
   * @param shape length of array in each dimension.
   * @return total number of elements in the array.
   */
  public static long computeSize(int[] shape) {
    long product = 1;
    for (int aShape : shape) {
      if (aShape < 0) {
        break; // stop at vlen
      }
      product *= aShape;
    }
    return product;
  }

  /**
   * If there are any VLEN dimensions (length < 0), remove it and all dimensions to the right.
   *
   * @param shape multidimensional shape
   * @return modified shape, if needed.
   */
  // TODO this implies that vlen doesnt have to be rightmost dimension. Is that true?
  public static int[] removeVlen(int[] shape) {
    int prefixrank;
    for (prefixrank = 0; prefixrank < shape.length; prefixrank++) {
      if (shape[prefixrank] < 0) {
        break;
      }
    }
    if (prefixrank == shape.length) {
      return shape;
    }
    int[] newshape = new int[prefixrank];
    System.arraycopy(shape, 0, newshape, 0, prefixrank);
    return newshape;
  }

  public static Object copyPrimitiveArray(Array<?> data) {
    ArrayType dataType = data.getArrayType();
    int idx = 0;
    switch (dataType) {
      case OPAQUE:
      case ENUM1:
      case UBYTE:
      case BYTE: {
        Array<Byte> bdata = (Array<Byte>) data;
        byte[] parray = new byte[(int) data.length()];
        for (byte val : bdata) {
          parray[idx++] = val;
        }
        return parray;
      }
      case CHAR: {
        Array<Character> cdata = (Array<Character>) data;
        char[] parray = new char[(int) data.length()];
        for (char val : cdata) {
          parray[idx++] = val;
        }
        return parray;
      }
      case ENUM2:
      case USHORT:
      case SHORT: {
        Array<Short> sdata = (Array<Short>) data;
        short[] parray = new short[(int) data.length()];
        for (short val : sdata) {
          parray[idx++] = val;
        }
        return parray;
      }
      case ENUM4:
      case UINT:
      case INT: {
        Array<Integer> idata = (Array<Integer>) data;
        int[] parray = new int[(int) data.length()];
        for (int val : idata) {
          parray[idx++] = val;
        }
        return parray;
      }
      case ULONG:
      case LONG: {
        Array<Long> ldata = (Array<Long>) data;
        long[] parray = new long[(int) data.length()];
        for (long val : ldata) {
          parray[idx++] = val;
        }
        return parray;
      }
      case FLOAT: {
        Array<Float> fdata = (Array<Float>) data;
        float[] parray = new float[(int) data.length()];
        for (float val : fdata) {
          parray[idx++] = val;
        }
        return parray;
      }
      case DOUBLE: {
        Array<Double> ddata = (Array<Double>) data;
        double[] parray = new double[(int) data.length()];
        for (double val : ddata) {
          parray[idx++] = val;
        }
        return parray;
      }
      case STRING: {
        Array<String> sdata = (Array<String>) data;
        String[] parray = new String[(int) data.length()];
        for (String val : sdata) {
          parray[idx++] = val;
        }
        return parray;
      }
      default:
        throw new IllegalStateException("Unimplemented datatype " + dataType);
    }
  }

  /** Convert a numeric array to double values. */
  public static Array<Double> toDouble(Array<?> array) {
    if (array instanceof ArrayDouble) {
      return (Array<Double>) array;
    }
    Array<Number> conv = (Array<Number>) array;
    int n = (int) array.length();
    double[] storage = new double[n];
    int count = 0;
    for (Number val : conv) {
      storage[count++] = val.doubleValue();
    }
    return factory(ArrayType.DOUBLE, array.getShape(), storage);
  }

  /** Get the min and max of the array, skipping missing data if eval.hasMissing(). */
  public static MinMax getMinMaxSkipMissingData(Array<? extends Number> a, @Nullable IsMissingEvaluator eval) {
    Preconditions.checkNotNull(a);
    boolean hasEval = (eval != null && eval.hasMissing());
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    if (a instanceof ArrayDouble) {
      ArrayDouble ad = (ArrayDouble) a;
      for (double val : ad) {
        if ((hasEval && eval.isMissing(val)) || Double.isNaN(val)) {
          continue;
        }
        if (val > max)
          max = val;
        if (val < min)
          min = val;
      }
    } else {
      for (Number number : a) {
        double val = number.doubleValue();
        if (hasEval && eval.isMissing(val) || Double.isNaN(val)) {
          continue;
        }
        if (val > max)
          max = val;
        if (val < min)
          min = val;
      }
    }
    return MinMax.create(min, max);
  }

  /**
   * Make a numeric array from a start and incr.
   *
   * @param type type of array
   * @param npts number of points
   * @param start starting value
   * @param incr increment
   * @param shape shape of resulting array. if not set, use 1 dim array of length npts.
   */
  public static <T> Array<T> makeArray(ArrayType type, int npts, double start, double incr, int... shape) {
    Preconditions.checkArgument(type.isNumeric());
    if (shape.length == 0) {
      shape = new int[] {npts};
    }

    Object pvals;
    switch (type) {
      case BYTE: {
        byte[] bvals = new byte[npts];
        for (int i = 0; i < npts; i++) {
          bvals[i] = (byte) (start + i * incr);
        }
        pvals = bvals;
        break;
      }
      case DOUBLE: {
        double[] dvals = new double[npts];
        for (int i = 0; i < npts; i++) {
          dvals[i] = (float) (start + i * incr);
        }
        pvals = dvals;
        break;
      }
      case FLOAT: {
        float[] fvals = new float[npts];
        for (int i = 0; i < npts; i++) {
          fvals[i] = (float) (start + i * incr);
        }
        pvals = fvals;
        break;
      }
      case INT: {
        int[] ivals = new int[npts];
        for (int i = 0; i < npts; i++) {
          ivals[i] = (int) (start + i * incr);
        }
        pvals = ivals;
        break;
      }
      case SHORT: {
        short[] svals = new short[npts];
        for (int i = 0; i < npts; i++) {
          svals[i] = (short) (start + i * incr);
        }
        pvals = svals;
        break;
      }
      case LONG: {
        long[] lvals = new long[npts];
        for (int i = 0; i < npts; i++) {
          lvals[i] = (long) (start + i * incr);
        }
        pvals = lvals;
        break;
      }
      default:
        throw new IllegalArgumentException("makeArray od type " + type);
    }
    return factory(type, shape, pvals);
  }

  /////////////////////////////////////////////////
  // static methods on Array<Byte>

  /**
   * Create an Array of Strings out of an Array\<Byte> of any rank.
   * If there is a null (zero) value in from, the String will end there.
   * The null is not returned as part of the String.
   *
   * @return Array of Strings of rank - 1.
   */
  public static Array<String> makeStringsFromChar(Array<Byte> from) {
    return ((ArrayByte) from).makeStringsFromChar();
  }

  /**
   * Create a String out of an Array\<Byte> of rank zero or one.
   * If there is a null (zero) value in from, the String will end there.
   * The null is not returned as part of the String.
   */
  public static String makeStringFromChar(Array<Byte> from) {
    return ((ArrayByte) from).makeStringFromChar();
  }

  /** Convert an Array\<Byte> into a ByteBuffer. */
  public static ByteBuffer getByteBuffer(Array<Byte> from) {
    return ((ArrayByte) from).getByteBuffer();
  }

  /** Convert the Array into a ByteString. */
  public static ByteString getByteString(Array<Byte> from) {
    return ((ArrayByte) from).getByteString();
  }

  /**
   * Compare values which must be equal to within {@link Misc#defaultMaxRelativeDiffDouble}.
   */
  public static boolean equalDoubles(Array<Double> arr1, Array<Double> arr2) {
    if (arr1.length() != arr2.length()) {
      return false;
    }
    Iterator<Double> iter1 = arr1.iterator();
    Iterator<Double> iter2 = arr2.iterator();
    while (iter1.hasNext() && iter2.hasNext()) {
      double v1 = iter1.next();
      double v2 = iter2.next();
      if (!Misc.nearlyEquals(v1, v2)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compare values which must be equal to within {@link Misc#defaultMaxRelativeDiffFloat}.
   */
  public static boolean equalFloats(Array<Float> arr1, Array<Float> arr2) {
    if (arr1.length() != arr2.length()) {
      return false;
    }
    Iterator<Float> iter1 = arr1.iterator();
    Iterator<Float> iter2 = arr2.iterator();
    while (iter1.hasNext() && iter2.hasNext()) {
      float v1 = iter1.next();
      float v2 = iter2.next();
      if (!Misc.nearlyEquals(v1, v2)) {
        return false;
      }
    }
    return true;
  }
}
