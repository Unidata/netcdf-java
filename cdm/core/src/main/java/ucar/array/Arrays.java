/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import java.util.ArrayList;
import java.util.List;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

/** Static helper classes for {@link Array} */
public class Arrays {

  /** Combine list of Array's by copying the underlying Array's into a single primitive array */
  public static <T> Array<T> factoryCopy(DataType dataType, int[] shape, List<Array<?>> dataArrays) {
    if (dataArrays.size() == 1) {
      return factory(dataType, shape, dataArrays.get(0).storage());
    }
    Object dataArray = combine(dataType, shape, dataArrays);
    return factory(dataType, shape, dataArray);
  }

  private static Object combine(DataType dataType, int[] shape, List<Array<?>> dataArrays) {
    Section section = new Section(shape);
    long size = section.getSize();
    if (size > Integer.MAX_VALUE) {
      throw new OutOfMemoryError();
    }

    switch (dataType) {
      case FLOAT: {
        float[] all = new float[(int) size];
        int start = 0;
        for (Array<?> dataArray : dataArrays) {
          dataArray.arraycopy(0, all, start, dataArray.getSize());
          start += dataArray.getSize();
        }
        return all;
      }
      case DOUBLE: {
        double[] all = new double[(int) size];
        int start = 0;
        for (Array<?> dataArray : dataArrays) {
          dataArray.arraycopy(0, all, start, dataArray.getSize());
          start += dataArray.getSize();
        }
        return all;
      }
      default:
        throw new RuntimeException(" DataType " + dataType);
    }
  }

  // The only advantage over copying AFAICT is that it can handle arrays > 2G. as long as its broken up into
  // multiple arrays < 2G.
  /** Experimental: keep list of Arrays seperate. This allows length > 2Gb. */
  public static <T> Array<T> factoryArrays(DataType dataType, int[] shape, List<Array<?>> dataArrays) {
    if (dataArrays.size() == 1) {
      return factory(dataType, shape, dataArrays.get(0).storage());
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

  /**
   * Create Array using java array of T, or java primitive array, as storage.
   * 
   * @param dataType
   * @param shape multidimensional shape, must have same total length as dataArray.
   * @param dataArray must be java array of T, or java primitive array
   */
  public static <T> Array<T> factory(DataType dataType, int[] shape, Object dataArray) {
    switch (dataType) {
      case DOUBLE:
        Storage<Double> storageD = new ArrayDouble.StorageD((double[]) dataArray);
        return (Array<T>) new ArrayDouble(shape, storageD);
      case FLOAT:
        Storage<Float> storageF = new ArrayFloat.StorageF((float[]) dataArray);
        return (Array<T>) new ArrayFloat(shape, storageF);
      default:
        throw new RuntimeException();
    }
  }

  /**
   * Create a new Array using same backing store as the org Array, by
   * flipping the index so that it runs from shape[index]-1 to 0.
   *
   * @param org original Array
   * @param dim dimension to flip
   * @return the new Array
   */
  public static <T> Array<T> flip(Array<T> org, int dim) {
    return org.createView(org.strides().flip(dim));
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
    return org.createView(org.strides().permute(dims));
  }

  /**
   * Create a new Array using same backing store with given shape
   *
   * @param org original Array
   * @param shape the new shape
   * @return the new Array
   */
  public static <T> Array<T> reshape(Array<T> org, int[] shape) {
    return org.createView(org.strides().reshape(shape));
  }

  /**
   * Create a new Array using same backing store as the org Array, by
   * eliminating any dimensions with length one.
   *
   * @param org original Array
   * @return the new Array, or the same Array if no reduction was done
   */
  public static <T> Array<T> reduce(Array<T> org) {
    Strides ri = org.strides().reduce();
    if (ri == org.strides())
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
    Strides ri = org.strides().reduce(dim);
    if (ri == org.strides())
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
    return org.createView(org.strides().section(ranges));
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
    return org.createView(org.strides().transpose(dim1, dim2));
  }
}
