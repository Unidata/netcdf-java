/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

/** Superclass for implementations of multidimensional arrays. */
public abstract class Array<T> implements Iterable<T> {

  public static <T> Array<T> factoryCopy(DataType dataType, int[] shape, List<Array> dataArrays) {
    if (dataArrays.size() == 1) {
      return factory(dataType, shape, dataArrays.get(0).getPrimitiveArray());
    }
    Object dataArray = combine(dataType, shape, dataArrays);
    return factory(dataType, shape, dataArray);
  }

  private static Object combine(DataType dataType, int[] shape, List<Array> dataArrays) {
    Section section = new Section(shape);
    long size = section.getSize();
    if (size > Integer.MAX_VALUE) {
      throw new OutOfMemoryError();
    }

    switch (dataType) {
      case FLOAT: {
        float[] all = new float[(int) size];
        int start = 0;
        for (Array<?> result : dataArrays) {
          float[] array = (float[]) result.getPrimitiveArray();
          System.arraycopy(array, 0, all, start, array.length);
          start += array.length;
        }
        return all;
      }
      case DOUBLE: {
        double[] all = new double[(int) size];
        int start = 0;
        for (Array<?> result : dataArrays) {
          double[] array = (double[]) result.getPrimitiveArray();
          System.arraycopy(array, 0, all, start, array.length);
          start += array.length;
        }
        return all;
      }
      default:
        throw new RuntimeException(" DataType " + dataType);
    }
  }

  // The only advantage over copying AFAICT is that it can handle arrays > 2G. as long as its broken up into
  // multiple arrays < 2G.
  public static <T> Array<T> factoryArrays(DataType dataType, int[] shape, List<Array> dataArrays) {
    if (dataArrays.size() == 1) {
      return factory(dataType, shape, dataArrays.get(0).getPrimitiveArray());
    }

    switch (dataType) {
      case DOUBLE:
        Storage<Double> storageD = Storage.factoryArrays(dataType, dataArrays);
        return (Array<T>) new ArrayDouble(shape, storageD);
      case FLOAT:
        Storage<Float> storageF = Storage.factoryArrays(dataType, dataArrays);
        return (Array<T>) new ArrayFloat(shape, storageF);
      default:
        throw new RuntimeException();
    }
  }

  public static <T> Array<T> factory(DataType dataType, int[] shape, Object dataArray) {
    switch (dataType) {
      case DOUBLE:
        Storage<Double> storageD = Storage.factory(dataType, dataArray);
        return (Array<T>) new ArrayDouble(shape, storageD);
      case FLOAT:
        Storage<Float> storageF = Storage.factory(dataType, dataArray);
        return (Array<T>) new ArrayFloat(shape, storageF);
      default:
        throw new RuntimeException();
    }
  }

  ///////////////////////////////////////////////////////////////////////
  final DataType dataType;
  final Strides indexCalc;
  final int rank;

  Array(DataType dataType, int[] shape) {
    this.dataType = dataType;
    this.rank = shape.length;
    this.indexCalc = Strides.builder(shape).build();
  }

  Array(DataType dataType, Strides shape) {
    this.dataType = dataType;
    this.rank = shape.getRank();
    this.indexCalc = shape;
  }

  @Override
  public abstract Iterator<T> iterator();

  public abstract Iterator<T> fastIterator();

  // Something to touch every element
  public abstract T sum();

  /**
   * Get the element indicated by the list of multidimensional indices.
   * 
   * @param index list of indices, one for each dimension. For vlen, the last is ignored.
   */
  public abstract T get(int... index);

  /**
   * Get the element indicated by Index.
   * 
   * @param index list of indices, one for each dimension. For vlen, the last is ignored.
   */
  public abstract T get(Index index);

  /**
   * Get underlying primitive array storage.
   * Exposed for efficiency, use at your own risk.
   *
   * @return underlying primitive array storage
   */
  abstract Object getPrimitiveArray();

  /** Return the datatype for this array */
  public DataType getDataType() {
    return this.dataType;
  }

  /** An Index that can be used instead of int[] */
  public Index getIndex() {
    return new Index(this.rank);
  }

  /** Get the number of dimensions of the array. */
  public int getRank() {
    return rank;
  }

  /** Get the shape: length of array in each dimension. */
  public int[] getShape() {
    return indexCalc.getShape();
  }

  /** Get the total number of elements in the array. */
  public long getSize() {
    return indexCalc.getSize();
  }

  /** Get the total number of bytes in the array. */
  public long getSizeBytes() {
    return indexCalc.getSize() * dataType.getSize();
  }

  /**
   * Find whether the underlying data should be interpreted as unsigned.
   * Only affects byte, short, int, and long.
   * When true, conversions to wider types are handled correctly.
   *
   * @return true if the data is an unsigned integer type.
   */
  public boolean isUnsigned() {
    return dataType.isUnsigned();
  }

  public boolean isVlen() {
    return indexCalc.isVlen();
  }

  /**
   * Create new Array with given Strides and the same backing store
   *
   * @param index use this Strides
   * @return a view of the Array using the given Index
   */
  abstract Array<T> createView(Strides index);

  /**
   * Create a new Array using same backing store as this Array, by
   * flipping the index so that it runs from shape[index]-1 to 0.
   *
   * @param dim dimension to flip
   * @return the new Array
   */
  public Array<T> flip(int dim) {
    return createView(indexCalc.flip(dim));
  }

  /**
   * Create a new Array using same backing store as this Array, by
   * permuting the indices.
   *
   * @param dims the old index dims[k] becomes the new kth index.
   * @return the new Array
   * @throws IllegalArgumentException: wrong rank or dim[k] not valid
   */
  public Array<T> permute(int[] dims) {
    return createView(indexCalc.permute(dims));
  }

  /**
   * Create a new Array using same backing store with given shape
   *
   * @param shape the new shape
   * @return the new Array
   * @throws IllegalArgumentException new shape is not conformable
   */
  public Array<T> reshape(int[] shape) {
    return createView(indexCalc.reshape(shape));
  }

  /**
   * Create a new Array using same backing store as this Array, by
   * eliminating any dimensions with length one.
   *
   * @return the new Array, or the same array if no reduction was done
   */
  public Array<T> reduce() {
    Strides ri = indexCalc.reduce();
    if (ri == indexCalc)
      return this;
    return createView(ri);
  }

  /**
   * Create a new Array using same backing store as this Array, by
   * eliminating the specified dimension.
   *
   * @param dim dimension to eliminate: must be of length one, else IllegalArgumentException
   * @return the new Array
   */
  public Array<T> reduce(int dim) {
    return createView(indexCalc.reduce(dim));
  }

  /**
   * Create a new Array as a subsection of this Array, with rank reduction.
   * No data is moved, so the new Array references the same backing store as the original.
   *
   * @param ranges list of Ranges that specify the array subset.
   *        Must be same rank as original Array.
   *        A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
   *        If Range[dim].length == 1, then the rank of the resulting Array is reduced at that dimension.
   * @return the new Array
   * @throws InvalidRangeException if ranges is invalid
   */
  public Array<T> section(List<Range> ranges) throws InvalidRangeException {
    return createView(indexCalc.section(ranges));
  }

  /**
   * Create a new Array as a subsection of this Array, with rank reduction.
   * No data is moved, so the new Array references the same backing store as the original.
   * <p/>
   *
   * @param origin int array specifying the starting index. Must be same rank as original Array.
   * @param shape int array specifying the extents in each dimension.
   *        This becomes the shape of the returned Array. Must be same rank as original Array.
   *        If shape[dim] == 1, then the rank of the resulting Array is reduced at that dimension.
   * @return the new Array
   * @throws InvalidRangeException if ranges is invalid
   */
  public Array<T> section(int[] origin, int[] shape) throws InvalidRangeException {
    return section(origin, shape, null);
  }

  /**
   * Create a new Array as a subsection of this Array, with rank reduction.
   * No data is moved, so the new Array references the same backing store as the original.
   * <p/>
   *
   * @param origin int array specifying the starting index. Must be same rank as original Array.
   * @param shape int array specifying the extents in each dimension.
   *        This becomes the shape of the returned Array. Must be same rank as original Array.
   *        If shape[dim] == 1, then the rank of the resulting Array is reduced at that dimension.
   * @param stride int array specifying the strides in each dimension. If null, assume all ones.
   * @return the new Array
   * @throws InvalidRangeException if ranges is invalid
   */
  public Array<T> section(int[] origin, int[] shape, int[] stride) throws InvalidRangeException {
    List<Range> ranges = new ArrayList<>(origin.length);
    if (stride == null) {
      stride = new int[origin.length];
      Arrays.fill(stride, 1);
    }
    for (int i = 0; i < origin.length; i++) {
      ranges.add(new Range(origin[i], origin[i] + stride[i] * shape[i] - 1, stride[i]));
    }
    return createView(indexCalc.section(ranges));
  }

  /**
   * Create a new Array as a subsection of this Array, without rank reduction.
   * No data is moved, so the new Array references the same backing store as the original.
   * Vlen is transferred over unchanged.
   *
   * @param ranges list of Ranges that specify the array subset.
   *        Must be same rank as original Array.
   *        A particular Range: 1) may be a subset, or 2) may be null, meaning use entire Range.
   * @return the new Array
   * @throws InvalidRangeException if ranges is invalid
   */
  public Array<T> sectionNoReduce(List<Range> ranges) throws InvalidRangeException {
    return createView(indexCalc.sectionNoReduce(ranges));
  }

  /**
   * Create a new Array as a subsection of this Array, without rank reduction.
   * No data is moved, so the new Array references the same backing store as the original.
   *
   * @param origin int array specifying the starting index. Must be same rank as original Array.
   * @param shape int array specifying the extents in each dimension.
   *        This becomes the shape of the returned Array. Must be same rank as original Array.
   * @param stride int array specifying the strides in each dimension. If null, assume all ones.
   * @return the new Array
   * @throws InvalidRangeException if ranges is invalid
   */
  public Array<T> sectionNoReduce(int[] origin, int[] shape, int[] stride) throws InvalidRangeException {
    List<Range> ranges = new ArrayList<>(origin.length);
    if (stride == null) {
      stride = new int[origin.length];
      Arrays.fill(stride, 1);
    }
    for (int i = 0; i < origin.length; i++) {
      if (shape[i] < 0) // VLEN
        ranges.add(Range.VLEN);
      else
        ranges.add(new Range(origin[i], origin[i] + stride[i] * shape[i] - 1, stride[i]));
    }
    return createView(indexCalc.sectionNoReduce(ranges));
  }

  /**
   * Create a new Array using same backing store as this Array, by
   * fixing the specified dimension at the specified index value. This reduces rank by 1.
   *
   * @param dim which dimension to fix
   * @param value at what index value
   * @return a new Array
   */
  public Array<T> slice(int dim, int value) {
    int[] origin = new int[rank];
    int[] shape = getShape();
    origin[dim] = value;
    shape[dim] = 1;
    try {
      return sectionNoReduce(origin, shape, null).reduce(dim); // preserve other dim 1
    } catch (InvalidRangeException e) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Create a new Array using same backing store as this Array, by
   * transposing two of the indices.
   *
   * @param dim1 transpose these two indices
   * @param dim2 transpose these two indices
   * @return the new Array
   */
  public Array<T> transpose(int dim1, int dim2) {
    return createView(indexCalc.transpose(dim1, dim2));
  }

}

