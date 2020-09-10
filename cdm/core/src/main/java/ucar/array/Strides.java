/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

/** Strides for Multidimensional arrays. Translate between multidimensional index and 1d arrays. */
@Immutable
public class Strides implements Iterable<Integer> {

  /**
   * Compute total number of elements in the array.
   * Stop at vlen
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

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the 1-d index indicated by the list of multidimensional indices.
   * 
   * @param index list of indices, one for each dimension. For vlen, the last is ignored.
   */
  public int get(int... index) {
    Preconditions.checkArgument(this.rank == index.length);
    int value = offset;
    for (int ii = 0; ii < rank; ii++) {
      if (shape[ii] < 0)
        break;// vlen
      value += index[ii] * stride[ii];
    }
    return value;
  }

  /** Get the number of dimensions in the array. */
  public int getRank() {
    return rank;
  }

  /** Get the shape: length of array in each dimension. */
  public int[] getShape() {
    int[] result = new int[rank];
    System.arraycopy(shape, 0, result, 0, rank);
    return result;
  }

  /** Get the length of the ith dimension. */
  public int getShape(int index) {
    Preconditions.checkArgument(index >= 0 && index < rank);
    return shape[index];
  }

  public Iterator<Integer> iterator() {
    return new IteratorImpl();
  }

  boolean isCanonicalOrder() {
    return canonicalOrder;
  }

  boolean isVlen() {
    return shape.length > 0 && shape[shape.length - 1] < 0;
  }

  /** Get the total number of elements in the array. */
  public long getSize() {
    return size;
  }

  /**
   * Create a new Index based on current one, except
   * flip the index so that it runs from shape[index]-1 to 0.
   *
   * @param index dimension to flip, may not be vlen
   * @return new index with flipped dimension
   */
  Strides flip(int index) {
    Preconditions.checkArgument(index >= 0 && index < rank);
    Preconditions.checkArgument(shape[index] >= 0);

    Strides.Builder ib = this.toBuilder();
    ib.offset += stride[index] * (shape[index] - 1);
    ib.stride[index] = -stride[index];
    ib.canonicalOrder = false;
    return ib.build();
  }

  /**
   * create a new Index based on a permutation of the current indices; vlen fails.
   *
   * @param dims: the old index dim[k] becomes the new kth index.
   * @return new Index with permuted indices
   */
  Strides permute(int[] dims) {
    Preconditions.checkArgument(dims.length == shape.length);
    Set<Integer> used = new HashSet<>();
    for (int dim : dims) {
      if ((dim < 0) || (dim >= rank)) {
        throw new IllegalArgumentException();
      }
      if (used.contains(dim)) {
        throw new IllegalArgumentException();
      }
      used.add(dim);
    }

    boolean isPermuted = false;
    Strides.Builder newIndex = toBuilder();
    for (int i = 0; i < dims.length; i++) {
      newIndex.stride[i] = stride[dims[i]];
      newIndex.shape[i] = shape[dims[i]];
      if (i != dims[i]) {
        isPermuted = true;
      }
    }

    newIndex.canonicalOrder = canonicalOrder && !isPermuted; // useful optimization
    return newIndex.build();
  }

  Strides reshape(int[] shape) {
    Preconditions.checkArgument(computeSize(shape) == getSize());
    return builder(shape).build();
  }

  /**
   * create a new Index based on a subsection of this one, with rank reduction if
   * dimension length == 1.
   *
   * @param ranges array of Ranges that specify the array subset.
   *        Must be same rank as original Array.
   *        A particular Range: 1) may be a subset; 2) may be null, meaning use entire Range.
   * @return new Index, with same or smaller rank as original.
   * @throws InvalidRangeException if ranges dont match current shape
   */
  Strides section(List<Range> ranges) throws InvalidRangeException {
    Preconditions.checkArgument(ranges.size() == rank);
    for (int ii = 0; ii < rank; ii++) {
      Range r = ranges.get(ii);
      if (r == null)
        continue;
      if (r == Range.VLEN)
        continue;
      if ((r.first() < 0) || (r.first() >= shape[ii]))
        throw new InvalidRangeException("Bad range starting value at index " + ii + " == " + r.first());
      if ((r.last() < 0) || (r.last() >= shape[ii]))
        throw new InvalidRangeException("Bad range ending value at index " + ii + " == " + r.last());
    }

    int reducedRank = rank;
    for (Range r : ranges) {
      if ((r != null) && (r.length() == 1))
        reducedRank--;
    }
    Strides.Builder newindex = builder(reducedRank);
    newindex.offset = offset;
    int[] newstride = new int[reducedRank];

    // calc shape, size, and index transformations
    // calc strides into original (backing) store
    int newDim = 0;
    for (int ii = 0; ii < rank; ii++) {
      Range r = ranges.get(ii);
      if (r == null) { // null range means use the whole original dimension
        newindex.shape[newDim] = shape[ii];
        newstride[newDim] = stride[ii];
        // if (name != null) newindex.name[newDim] = name[ii];
        newDim++;
      } else if (r.length() != 1) {
        newindex.shape[newDim] = r.length();
        newstride[newDim] = stride[ii] * r.stride();
        newindex.offset += stride[ii] * r.first();
        // if (name != null) newindex.name[newDim] = name[ii];
        newDim++;
      } else {
        newindex.offset += stride[ii] * r.first(); // constant due to rank reduction
      }
    }
    newindex.setStride(newstride);

    // if equal, then its not a real subset, so can still use fastIterator
    newindex.canonicalOrder = canonicalOrder && (computeSize(newindex.shape) == size);
    return newindex.build();
  }

  /**
   * create a new Index based on a subsection of this one, without rank reduction.
   *
   * @param ranges list of Ranges that specify the array subset.
   *        Must be same rank as original Array.
   *        A particular Range: 1) may be a subset; 2) may be null, meaning use entire Range.
   * @return new Index, with same rank as original.
   * @throws InvalidRangeException if ranges dont match current shape
   */
  Strides sectionNoReduce(List<Range> ranges) throws InvalidRangeException {
    Preconditions.checkArgument(ranges.size() == rank);
    for (int ii = 0; ii < rank; ii++) {
      Range r = ranges.get(ii);
      if (r == null)
        continue;
      if (r == Range.VLEN)
        continue;
      if ((r.first() < 0) || (r.first() >= shape[ii]))
        throw new InvalidRangeException("Bad range starting value at index " + ii + " == " + r.first());
      if ((r.last() < 0) || (r.last() >= shape[ii]))
        throw new InvalidRangeException("Bad range ending value at index " + ii + " == " + r.last());
    }

    // allocate
    Strides.Builder newindex = builder(rank);
    newindex.offset = offset;
    int[] newstride = new int[rank];

    // calc shape, size, and index transformations
    // calc strides into original (backing) store
    for (int ii = 0; ii < rank; ii++) {
      Range r = ranges.get(ii);
      if (r == null) { // null range means use the whole original dimension
        newindex.shape[ii] = shape[ii];
        newstride[ii] = stride[ii];
      } else {
        newindex.shape[ii] = r.length();
        newstride[ii] = stride[ii] * r.stride();
        newindex.offset += stride[ii] * r.first();
      }
    }
    newindex.setStride(newstride);

    // if equal, then its not a real subset, so can still use fastIterator
    newindex.canonicalOrder = canonicalOrder && (computeSize(newindex.shape) == size);
    return newindex.build();
  }

  /**
   * Create a new Index based on current one by
   * eliminating any dimensions with length one.
   *
   * @return the new Index
   */
  Strides reduce() {
    Strides c = this;
    for (int ii = 0; ii < rank; ii++)
      if (shape[ii] == 1) { // do this on the first one you find
        Strides newc = c.reduce(ii);
        return newc.reduce(); // any more to do?
      }
    return c;
  }

  /**
   * Create a new Index based on current one by
   * eliminating the specified dimension;
   *
   * @param dim: dimension to eliminate: must be of length one, else IllegalArgumentException
   * @return the new Index
   */
  Strides reduce(int dim) {
    if ((dim < 0) || (dim >= rank))
      throw new IllegalArgumentException("illegal reduce dim " + dim);
    if (shape[dim] != 1)
      throw new IllegalArgumentException("illegal reduce dim " + dim + " : length != 1");

    Strides.Builder newindex = builder(rank - 1);
    newindex.offset = offset;
    int[] newstride = new int[rank - 1];

    int count = 0;
    for (int ii = 0; ii < rank; ii++) {
      if (ii != dim) {
        newindex.shape[count] = shape[ii];
        newstride[count] = stride[ii];
        count++;
      }
    }
    newindex.setStride(newstride);
    newindex.canonicalOrder = canonicalOrder;
    return newindex.build();
  }

  /**
   * create a new Index based on current one, except
   * transpose two of the indices.
   *
   * @param index1 transpose these two indices
   * @param index2 transpose these two indices
   * @return new Index with transposed indices
   */
  Strides transpose(int index1, int index2) {
    if ((index1 < 0) || (index1 >= rank))
      throw new IllegalArgumentException();
    if ((index2 < 0) || (index2 >= rank))
      throw new IllegalArgumentException();
    if (index1 == index2)
      return this;

    Strides.Builder newIndex = toBuilder();
    newIndex.stride[index1] = stride[index2];
    newIndex.stride[index2] = stride[index1];
    newIndex.shape[index1] = shape[index2];
    newIndex.shape[index2] = shape[index1];

    newIndex.setCanonicalOrder(false);
    return newIndex.build();
  }

  public static Builder builder(int rank) {
    return new Builder(rank);
  }

  public static Builder builder(int[] shape) {
    return new Builder(shape.length).setShape(shape);
  }

  public Builder toBuilder() {
    return new Builder(rank).setShape(this.shape).setStride(this.stride).setOffset(this.offset)
        .setCanonicalOrder(this.canonicalOrder);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final int[] shape;
  private final int[] stride;
  private final int rank;

  private final long size; // total number of elements
  private final int offset; // element = offset + stride[0]*current[0] + ...
  private final boolean canonicalOrder; // can use fast iterator if in canonical order

  private Strides(Builder builder) {
    Preconditions.checkNotNull(builder.shape);
    this.rank = builder.shape.length;

    this.shape = new int[rank];
    System.arraycopy(builder.shape, 0, this.shape, 0, rank);

    if (builder.stride == null) {
      stride = new int[rank];
      size = computeStrides(shape);
    } else {
      Preconditions.checkArgument(builder.stride.length == rank);
      this.stride = new int[rank];
      System.arraycopy(builder.stride, 0, this.stride, 0, rank);
      this.size = computeSize(shape);
    }
    this.offset = builder.offset;
    this.canonicalOrder = builder.canonicalOrder;
  }

  /** Compute standard strides based on array's shape. Ignore vlen */
  private long computeStrides(int[] shape) {
    long product = 1;
    for (int ii = shape.length - 1; ii >= 0; ii--) {
      int thisDim = shape[ii];
      if (thisDim < 0)
        continue; // ignore vlen
      this.stride[ii] = (int) product;
      product *= thisDim;
    }
    return product;
  }

  public static class Builder {
    int[] shape;
    int[] stride;
    int offset = 0;
    boolean canonicalOrder = true;

    Builder(int rank) {
      shape = new int[rank];
    }

    Builder setShape(int[] shape) {
      Preconditions.checkArgument(shape.length == this.shape.length);
      System.arraycopy(shape, 0, this.shape, 0, shape.length);
      return this;
    }

    Builder setStride(int[] stride) {
      Preconditions.checkArgument(stride.length == this.shape.length);
      this.stride = new int[this.shape.length];
      System.arraycopy(stride, 0, this.stride, 0, stride.length);
      return this;
    }

    Builder setOffset(int offset) {
      this.offset = offset;
      return this;
    }

    Builder setCanonicalOrder(boolean canonicalOrder) {
      this.canonicalOrder = canonicalOrder;
      return this;
    }

    public Strides build() {
      return new Strides(this);
    }
  }

  private class IteratorImpl implements Iterator<Integer> {
    private int count = 0;
    private final int[] current = new int[rank];

    public boolean hasNext() {
      return count++ < size;
    }

    public Integer next() {
      return incr();
    }

    private int incr() {
      int digit = rank - 1;
      while (digit >= 0) {
        if (shape[digit] < 0) {
          current[digit] = -1;
          continue;
        } // do not increment vlen
        current[digit]++;
        if (current[digit] < shape[digit])
          break; // normal exit
        current[digit] = 0; // else, carry
        digit--;
      }
      return get(current);
    }
  }

}
