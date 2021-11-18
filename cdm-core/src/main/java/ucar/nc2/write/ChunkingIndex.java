/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

/**
 * An index that computes chunk shapes. It is intended to be used to compute the origins and shapes for a series
 * of contiguous writes to a multidimensional array.
 * It writes the first n elements (n &lt; maxChunkElems), then the next, etc.
 * Contributed by cwardgar@usgs.gov 4/12/2010
 * TODO this assumes strides are always 1
 * TODO can we use nc2.write.IndexChunker?
 */
public class ChunkingIndex {

  /** Compute total number of elements for the given shape. */
  public static long computeSize(int[] shape) {
    long product = 1;
    for (int aShape : shape) {
      product *= aShape;
    }
    return product;
  }

  /////////////////////////////////////////////////////////////////////////////////////
  private final int[] shape;
  private final long[] stride; // long ?
  private final int rank;
  private final long size; // total number of elements
  private final int offset; // element = offset + stride[0]*current[0] + ...
  private final int[] current; // current element's index

  public ChunkingIndex(int[] shape) {
    this.shape = new int[shape.length]; // optimization over clone
    System.arraycopy(shape, 0, this.shape, 0, shape.length);

    rank = shape.length;
    current = new int[rank];
    stride = new long[rank];
    size = computeStrides(shape);
    offset = 0;
  }

  /**
   * Compute standard strides based on array's shape.
   *
   * @param shape length of array in each dimension.
   * @return standard strides based on array's shape.
   */
  private long computeStrides(int[] shape) {
    long product = 1;
    for (int ii = shape.length - 1; ii >= 0; ii--) {
      int thisDim = shape[ii];
      this.stride[ii] = (int) product;
      product *= thisDim;
    }
    return product;
  }

  /** Get the current counter. */
  public int[] currentCounter() {
    return current.clone();
  }

  /** Get the current element's index into the 1D backing array. */
  public long currentElement() {
    long value = offset;
    for (int ii = 0; ii < rank; ii++) {
      value += current[ii] * stride[ii];
    }
    return value;
  }

  public long size() {
    return size;
  }

  /**
   * Increment the current element by 1.
   * 
   * @return currentElement()
   */
  public long incr() {
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
    return currentElement();
  }

  /**
   * Set the current counter from the 1D "current element"
   * currElement = offset + stride[0]*current[0] + ...
   *
   * @param currElement set to this value
   */
  public void setCurrentCounter(long currElement) {
    currElement -= offset;
    for (int ii = 0; ii < rank; ii++) { // general rank
      if (shape[ii] < 0) {
        current[ii] = -1;
        break;
      }
      current[ii] = (int) (currElement / stride[ii]);
      currElement -= current[ii] * stride[ii];
    }
  }

  /**
   * Computes the shape of the largest possible <b>contiguous</b> chunk, starting at {@link #currentCounter()}
   * and with {@code numElems <= maxChunkElems}.
   *
   * @param maxChunkElems the maximum number of elements in the chunk shape. The actual element count of the shape
   *        returned is likely to be different, and can be found with {@link #computeSize}.
   * @return the shape of the largest possible contiguous chunk.
   */
  public int[] computeChunkShape(long maxChunkElems) {
    int[] chunkShape = new int[rank];

    for (int iDim = 0; iDim < rank; ++iDim) {
      int size = (int) (maxChunkElems / stride[iDim]);
      size = (size == 0) ? 1 : size;
      size = Math.min(size, shape[iDim] - current[iDim]);
      chunkShape[iDim] = size;
    }

    return chunkShape;
  }
}
