/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import com.google.common.base.Preconditions;
import ucar.array.Arrays;
import ucar.array.Section;
import ucar.array.InvalidRangeException;

/**
 * LayoutSegmented has data stored in segments.
 * Assume that each segment size is a multiple of elemSize.
 * Used by HDF4.
 */
public class LayoutSegmented implements Layout {
  private static final boolean debugNext = false;

  private final long total;
  private final int elemSize; // size of each element

  private final long[] segPos; // bytes
  private final long[] segMax; // bytes
  private final long[] segMin; // bytes

  // outer chunk
  private final IndexChunker chunker;
  private IndexChunker.Chunk chunkOuter;

  // inner chunk = deal with segmentation
  private final IndexChunker.Chunk chunkInner = new IndexChunker.Chunk(0, 0, 0);

  private long done;
  private int needInner;
  private int doneInner;

  /**
   * Constructor.
   *
   * @param segPos starting address of each segment.
   * @param segSize number of bytes in each segment. Assume multiple of elemSize
   * @param elemSize size of an element in bytes.
   * @param srcShape shape of the entire data array.
   * @param wantSection the wanted section of data
   * @throws InvalidRangeException if ranges are misformed
   */
  public LayoutSegmented(long[] segPos, int[] segSize, int elemSize, int[] srcShape, Section wantSection)
      throws InvalidRangeException {
    Preconditions.checkArgument(segPos.length == segSize.length);
    this.segPos = segPos;

    int nsegs = segPos.length;
    segMin = new long[nsegs];
    segMax = new long[nsegs];
    long totalElems = 0;
    for (int i = 0; i < nsegs; i++) {
      Preconditions.checkArgument(segPos[i] >= 0);
      Preconditions.checkArgument(segSize[i] > 0);
      Preconditions.checkArgument((segSize[i] % elemSize) == 0);

      segMin[i] = totalElems;
      totalElems += segSize[i];
      segMax[i] = totalElems;
    }
    Preconditions.checkArgument(totalElems >= Arrays.computeSize(srcShape) * elemSize);

    chunker = new IndexChunker(srcShape, wantSection);
    this.total = chunker.getTotalNelems();
    this.done = 0;
    this.elemSize = elemSize;
  }

  @Override
  public long getTotalNelems() {
    return total;
  }

  @Override
  public int getElemSize() {
    return elemSize;
  }

  @Override
  public boolean hasNext() {
    return done < total;
  }

  ///////////////////

  private long getFilePos(long elem) {
    int segno = 0;
    while (elem >= segMax[segno])
      segno++;
    return segPos[segno] + elem - segMin[segno];
  }

  // how many more bytes are in this segment ?
  private int getMaxBytes(long start) {
    int segno = 0;
    while (start >= segMax[segno])
      segno++;
    return (int) (segMax[segno] - start);
  }

  @Override
  public Chunk next() {
    Chunk result;

    if (needInner > 0) {
      result = nextInner(false, 0);

    } else {
      result = nextOuter();
      int nbytes = getMaxBytes(chunkOuter.getSrcElem() * elemSize);
      if (nbytes < result.getNelems() * elemSize)
        result = nextInner(true, nbytes);
    }

    done += result.getNelems();
    doneInner += result.getNelems();
    needInner -= result.getNelems();

    if (debugNext)
      System.out.println(" next chunk: " + result);

    return result;
  }

  private Chunk nextInner(boolean first, int nbytes) {
    if (first) {
      chunkInner.setNelems(nbytes / elemSize);
      chunkInner.setDestElem(chunkOuter.getDestElem());
      needInner = chunkOuter.getNelems();
      doneInner = 0;

    } else {
      chunkInner.incrDestElem(chunkInner.getNelems()); // increment using last chunks' value
      nbytes = getMaxBytes((chunkOuter.getSrcElem() + doneInner) * elemSize);
      nbytes = Math.min(nbytes, needInner * elemSize);
      chunkInner.setNelems(nbytes / elemSize); // set this chunk's value
    }

    chunkInner.setSrcPos(getFilePos((chunkOuter.getSrcElem() + doneInner) * elemSize));
    return chunkInner;
  }

  public Chunk nextOuter() {
    chunkOuter = chunker.next();
    long srcPos = getFilePos(chunkOuter.getSrcElem() * elemSize);
    chunkOuter.setSrcPos(srcPos);
    return chunkOuter;
  }

}

