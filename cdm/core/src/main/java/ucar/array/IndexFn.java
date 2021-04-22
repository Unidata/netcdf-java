/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

/** Translate between multidimensional index and 1-d arrays. */
@Immutable
final class IndexFn implements Iterable<Integer> {

  /**
   * Get the 1-d index indicated by the list of multidimensional indices.
   *
   * @param index list of indices, one for each dimension. For vlen, the last is ignored.
   */
  public int get(int... index) {
    // scalar case
    if (this.rank == 0 && index.length == 1 && index[0] == 0) {
      return 0;
    }
    Preconditions.checkArgument(this.rank == index.length);
    int value = offset;
    for (int ii = 0; ii < rank; ii++) {
      if (index[ii] < 0 || index[ii] >= shape[ii]) {
        throw new IllegalArgumentException(String.format("IndexFn.get(%s) not inside of shape '%s'",
            java.util.Arrays.toString(index), java.util.Arrays.toString(shape)));
      }
      if (shape[ii] < 0) {
        break; // vlen
      }
      value += index[ii] * stride[ii];
    }
    return value;
  }

  /** Get the number of dimensions in the array. */
  public int getRank() {
    return rank;
  }

  /** Get the shape: length of array in each dimension. */
  public Section getSection() {
    try {
      return new Section(odometer(offset), shape); // LOOK strides
    } catch (InvalidRangeException e) {
      throw new IllegalStateException(e);
    }
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
    return new Odometer();
  }

  public Iterator<Integer> iterator(int start, long length) {
    return new Odometer(start, length);
  }

  /** Get the total number of elements in the array. */
  public long length() {
    return length;
  }

  public String toString2() {
    StringBuilder sbuff = new StringBuilder();
    boolean first = true;
    for (int i : this) {
      if (!first) {
        sbuff.append(", ");
      }
      sbuff.append(i);
      first = false;
    }
    return sbuff.toString();
  }

  @Override
  public String toString() {
    return "IndexFn{" + "shape=" + java.util.Arrays.toString(shape) + ", stride=" + java.util.Arrays.toString(stride)
        + ", rank=" + rank + ", length=" + length + ", offset=" + offset + ", canonicalOrder=" + canonicalOrder + '}';
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////


  boolean isCanonicalOrder() {
    return canonicalOrder;
  }

  boolean isVlen() {
    return shape.length > 0 && shape[shape.length - 1] < 0;
  }

  /**
   * Create a new Index based on current one, except
   * flip the index so that it runs from shape[index]-1 to 0.
   *
   * @param index dimension to flip, may not be vlen
   * @return new index with flipped dimension
   */
  IndexFn flip(int index) {
    Preconditions.checkArgument(index >= 0 && index < rank);
    Preconditions.checkArgument(shape[index] >= 0);

    IndexFn.Builder ib = this.toBuilder();
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
  IndexFn permute(int[] dims) {
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
    IndexFn.Builder newIndex = toBuilder();
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

  IndexFn reshape(int[] shape) {
    Preconditions.checkArgument(Arrays.computeSize(shape) == length());
    return builder(shape).build();
  }

  /**
   * Create a new Strides based on a subsection of this one.
   *
   * @param section list of Ranges that specify the array subset.
   *        Must be same rank as original Array.
   *        A particular Range: 1) may be a subset; 2) may be null, meaning use entire Range.
   * @return new Index, with same rank as original.
   * @throws InvalidRangeException if ranges dont match current shape
   */
  IndexFn section(Section section) throws InvalidRangeException {
    Preconditions.checkArgument(section.getRank() == rank);
    for (int ii = 0; ii < rank; ii++) {
      Range r = section.getRange(ii);
      if (r == null || r == Range.VLEN) {
        continue;
      }
      if ((r.first() < 0) || (r.first() >= shape[ii])) {
        throw new InvalidRangeException("Bad range starting value at index " + ii + " == " + r.first());
      }
      if ((r.last() < 0) || (r.last() >= shape[ii])) {
        throw new InvalidRangeException("Bad range ending value at index " + ii + " == " + r.last());
      }
    }

    // allocate
    IndexFn.Builder newindex = builder(rank);
    newindex.offset = offset;
    int[] newstride = new int[rank];

    // calc shape, size, and index transformations
    // calc strides into original (backing) store
    for (int ii = 0; ii < rank; ii++) {
      Range r = section.getRange(ii);
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
    newindex.canonicalOrder = canonicalOrder && (Arrays.computeSize(newindex.shape) == length);
    return newindex.build();
  }

  /**
   * Create a new Index based on current one by
   * eliminating any dimensions with length one.
   *
   * @return the new Index
   */
  IndexFn reduce() {
    IndexFn c = this;
    for (int ii = 0; ii < rank; ii++)
      if (shape[ii] == 1) { // do this on the first one you find
        IndexFn newc = c.reduce(ii);
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
  IndexFn reduce(int dim) {
    if ((dim < 0) || (dim >= rank))
      throw new IllegalArgumentException("illegal reduce dim " + dim);
    if (shape[dim] != 1)
      throw new IllegalArgumentException("illegal reduce dim " + dim + " : length != 1");

    IndexFn.Builder newindex = builder(rank - 1);
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
  IndexFn transpose(int index1, int index2) {
    if ((index1 < 0) || (index1 >= rank))
      throw new IllegalArgumentException();
    if ((index2 < 0) || (index2 >= rank))
      throw new IllegalArgumentException();
    if (index1 == index2)
      return this;

    IndexFn.Builder newIndex = toBuilder();
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

  private final long length; // total number of elements
  private final int offset; // element = offset + stride[0]*current[0] + ...
  private final boolean canonicalOrder; // can use fast iterator if in canonical order

  private IndexFn(Builder builder) {
    Preconditions.checkNotNull(builder.shape);
    this.rank = builder.shape.length;

    this.shape = new int[rank];
    System.arraycopy(builder.shape, 0, this.shape, 0, rank);

    if (builder.stride == null) {
      stride = new int[rank];
      length = computeStrides(shape);
    } else {
      Preconditions.checkArgument(builder.stride.length == rank);
      this.stride = new int[rank];
      System.arraycopy(builder.stride, 0, this.stride, 0, rank);
      this.length = Arrays.computeSize(shape);
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
      this.shape = new int[shape.length];
      System.arraycopy(shape, 0, this.shape, 0, shape.length);
      return this;
    }

    /** Dimension strides (not Section strides) */
    Builder setStride(int[] stride) {
      this.stride = new int[stride.length];
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

    public IndexFn build() {
      return new IndexFn(this);
    }
  }

  /** what is the odometer (n-dim index) for element (1-d index)? */
  int[] odometer(long element) {
    int[] odometer = new int[rank];
    for (int dim = 0; dim < rank; dim++) {
      odometer[dim] = (int) (element / stride[dim]);
      element -= odometer[dim] * stride[dim];
    }
    return odometer;
  }

  private class Odometer implements Iterator<Integer> {
    private final long nelems;
    private final int[] current;
    private int count = 0;
    private int nextIndex;

    private Odometer() {
      nelems = length; // all elements
      current = new int[rank]; // starts at 0
      nextIndex = get(current);
    }

    private Odometer(int startElement, long nelems) {
      this.nelems = nelems; // this many elements
      current = odometer(startElement); // starts here
      nextIndex = get(current);
    }

    public boolean hasNext() {
      return count++ < nelems;
    }

    public Integer next() {
      int result = nextIndex;
      nextIndex = incr();
      return result;
    }

    private int incr() {
      int digit = rank - 1;
      while (digit >= 0) {
        if (shape[digit] < 0) { // do not increment vlen
          current[digit] = -1;
          continue;
        }
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
