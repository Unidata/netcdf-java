/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.array.Section;
import ucar.array.InvalidRangeException;

/**
 * Indexer into data that has a "regular" layout, like netcdf-3 and hdf5 compact and contiguous storage.
 * The data is contiguous, with outer dimension varying fastest.
 * Given a Section, this calculates the set of contiguous "chunks" of the wanted data into the stored data.
 * The wanted section is always a subset of the data section (see RegularSectionLayout where thats not the case).
 */
@Immutable
public class LayoutRegular implements Layout {
  private final IndexChunker chunker;
  private final long startPos; // starting position
  private final int elemSize; // size of each element

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of an element in bytes.
   * @param varShape shape of the entire data array.
   * @param wantSection the wanted section of data, contains a List of Range objects.
   * @throws InvalidRangeException if ranges are misformed
   */
  public LayoutRegular(long startPos, int elemSize, int[] varShape, @Nullable Section wantSection)
      throws InvalidRangeException {
    Preconditions.checkArgument(startPos >= 0);
    Preconditions.checkArgument(elemSize > 0);

    this.startPos = startPos;
    this.elemSize = elemSize;
    this.chunker = new IndexChunker(varShape, wantSection);
  }

  @Override
  public long getTotalNelems() {
    return chunker.getTotalNelems();
  }

  @Override
  public int getElemSize() {
    return elemSize;
  }

  @Override
  public boolean hasNext() {
    return chunker.hasNext();
  }

  @Override
  public Chunk next() {
    IndexChunker.Chunk chunk = chunker.next();
    chunk.setSrcPos(startPos + chunk.getSrcElem() * elemSize);
    return chunk;
  }
}
