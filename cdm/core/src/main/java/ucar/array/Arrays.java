/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import java.util.ArrayList;
import java.util.List;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import ucar.ma2.*;
import ucar.nc2.util.MinMax;

/** Static helper classes for {@link Array} */
public class Arrays {

  /**
   * Create Array using java array of T, or java primitive array, as storage.
   * Do not use this for Vlens or Structures.
   *
   * @param dataType data type of the data. Vlen detected from the shape.
   * @param shape multidimensional shape, must have same total length as dataArray.
   * @param storage storage for type T.
   */
  public static <T> Array<T> factory(DataType dataType, int[] shape, Storage<T> storage) {
    switch (dataType) {
      case OPAQUE:
      case BOOLEAN:
      case BYTE:
      case ENUM1:
      case UBYTE: {
        return (Array<T>) new ArrayByte(dataType, shape, (Storage<Byte>) storage);
      }
      case CHAR: {
        return (Array<T>) new ArrayChar(shape, (Storage<Character>) storage);
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
        throw new RuntimeException("Unimplemented DataType " + dataType);
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
  public static <T> Array<T> factory(DataType dataType, int[] shape, Object dataArray) {
    switch (dataType) {
      case OPAQUE:
      case BOOLEAN:
      case BYTE:
      case ENUM1:
      case UBYTE: {
        Storage<Byte> storageS = new ArrayByte.StorageS((byte[]) dataArray);
        return (Array<T>) new ArrayByte(dataType, shape, storageS);
      }
      case CHAR: {
        Storage<Character> storageS = new ArrayChar.StorageS((char[]) dataArray);
        return (Array<T>) new ArrayChar(shape, storageS);
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
        throw new RuntimeException("Unimplemented DataType= " + dataType);
    }
  }

  /**
   * Create Array using empty java array of T, or java primitive array, same size as shape.
   * Do not use this for Vlens or Structures.
   *
   * @param dataType
   * @param shape multidimensional shape;
   */
  public static <T> Array<T> factory(DataType dataType, int[] shape) {
    switch (dataType) {
      case BOOLEAN:
      case BYTE:
      case ENUM1:
      case UBYTE: {
        return (Array<T>) new ArrayByte(dataType, shape);
      }
      case CHAR: {
        return (Array<T>) new ArrayChar(shape);
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
        throw new RuntimeException("Unimplemented DataType " + dataType);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Experimental

  /**
   * Combine list of Array's by copying the underlying Array's into a single primitive array
   */
  public static <T> Array<T> factoryCopy(DataType dataType, int[] shape, List<Array<T>> dataArrays) {
    if (dataArrays.size() == 1) {
      return factory(dataType, shape, dataArrays.get(0).storage());
    }
    Object dataArray = combine(dataType, shape, dataArrays);
    return factory(dataType, shape, dataArray);
  }

  private static <T> Object combine(DataType dataType, int[] shape, List<Array<T>> dataArrays) {
    Section section = new Section(shape);
    long size = section.getSize();
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
        throw new RuntimeException(" DataType " + dataType);
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
   * Experimental: keep list of Arrays seperate. This allows length > 2Gb.
   */
  public static <T> Array<T> factoryArrays(DataType dataType, int[] shape, List<Array<?>> dataArrays) {
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
   * @param ranges list of Ranges that specify the array subset.
   *        Must be same rank as original Array.
   *        A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
   * @return the new Array
   * @throws InvalidRangeException if ranges is invalid
   */
  public static <T> Array<T> section(Array<T> org, List<Range> ranges) throws InvalidRangeException {
    return org.createView(org.indexFn().section(ranges));
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
    ArrayList<Range> ranges = new ArrayList<>(org.getSection().getRanges());
    ranges.set(dim, new Range(value, value));
    Array<T> s = section(org, ranges);
    return reduce(s, dim);
  }

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

  /**
   * Compute total number of elements in the array. Stop at vlen.
   *
   * @param shape length of array in each dimension.
   * @return total number of elements in the array.
   */
  public static long computeSize(int[] shape) {
    long product = 1;
    for (int aShape : shape) {
      if (aShape < 0)
        break; // stop at vlen
      product *= aShape;
    }
    return product;
  }

  /**
   * If there are any VLEN dimensions (length < 0), remove it and all dimensions to the right.
   *
   * @param shape
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
    DataType dataType = data.getDataType();
    int idx = 0;
    switch (dataType) {
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
    return factory(DataType.DOUBLE, array.getShape(), storage);
  }

  public static MinMax getMinMaxSkipMissingData(Array<? extends Number> a, IsMissingEvaluator eval) {
    Preconditions.checkNotNull(a);
    boolean hasEval = (eval == null || !eval.hasMissing());
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    if (a instanceof ArrayDouble) {
      ArrayDouble ad = (ArrayDouble) a;
      for (double val : ad) {
        if (hasEval && eval.isMissing(val)) {
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
        if (hasEval && eval.isMissing(val)) {
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

}
