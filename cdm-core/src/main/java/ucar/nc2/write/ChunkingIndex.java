/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// contributed by cwardgar@usgs.gov 4/12/2010

import ucar.ma2.Index;

/**
 * An index that computes chunk shapes. It is intended to be used to compute the origins and shapes for a series
 * of contiguous writes to a multidimensional array.
 * It writes the first n elements (n &lt; maxChunkElems), then the next, etc.
 * Contributed by cwardgar@usgs.gov 4/12/2010
 */
public class ChunkingIndex extends Index {
  public ChunkingIndex(int[] shape) {
    super(shape);
  }

  /**
   * Computes the shape of the largest possible <b>contiguous</b> chunk, starting at {@link #getCurrentCounter()}
   * and with {@code numElems <= maxChunkElems}.
   *
   * @param maxChunkElems the maximum number of elements in the chunk shape. The actual element count of the shape
   *        returned is likely to be different, and can be found with {@link Index#computeSize}.
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
