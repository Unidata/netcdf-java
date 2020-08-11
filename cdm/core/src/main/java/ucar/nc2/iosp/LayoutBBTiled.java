/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import java.io.IOException;
import java.nio.*;

/**
 * For datasets where the data are stored in chunks, and must be processed, eg compressed or filtered.
 * The data is read, processed, and placed in a ByteBuffer. Chunks have an offset into the ByteBuffer.
 * "Tiled" means that all chunks are assumed to be equal size.
 * Chunks do not necessarily cover the array, missing data is possible.
 * Used by HDF4 and HDF5.
 */
public class LayoutBBTiled implements LayoutBB {
  private static final boolean debug = false, debugIntersection = false;

  private final Section want;
  private final int[] chunkSize; // all chunks assumed to be the same size
  private final int elemSize;
  private final DataChunkIterator chunkIterator; // iterate across chunks

  // track the overall iteration
  private final long totalNelems;
  private long totalNelemsDone;

  private IndexChunkerTiled index; // iterate within a chunk
  private LayoutBBTiled.Chunk next;

  /**
   * Constructor.
   *
   * @param chunkIterator iterator over all data chunks
   * @param chunkSize all chunks assumed to be the same size
   * @param elemSize size of an element in bytes.
   * @param wantSection the wanted section of data, contains a List of Range objects. Must be complete.
   */
  public LayoutBBTiled(DataChunkIterator chunkIterator, int[] chunkSize, int elemSize, Section wantSection) {
    this.chunkIterator = chunkIterator;
    this.chunkSize = chunkSize;
    this.elemSize = elemSize;
    this.want = wantSection;
    if (debug)
      System.out.println(" want section=" + this.want);

    this.totalNelems = this.want.computeSize();
    this.totalNelemsDone = 0;
  }

  @Override
  public long getTotalNelems() {
    return totalNelems;
  }

  @Override
  public int getElemSize() {
    return elemSize;
  }

  @Override
  public boolean hasNext() { // have to actually fetch the thing
    if (totalNelemsDone >= totalNelems)
      return false;

    if ((index == null) || !index.hasNext()) { // get new data node
      try {
        Section dataSection;
        DataChunk dataChunk;

        while (true) { // look for intersecting sections
          if (!chunkIterator.hasNext()) {
            next = null;
            return false;
          }

          // get next dataChunk
          try {
            dataChunk = chunkIterator.next();
          } catch (IOException e) {
            e.printStackTrace();
            next = null;
            return false;
          }

          // make the dataSection for this chunk
          dataSection = new Section(dataChunk.getOffset(), chunkSize);
          if (debugIntersection)
            System.out.println(" test intersecting: " + dataSection + " want: " + want);
          if (dataSection.intersects(want)) // does it intersect ?
            break;
        }

        if (debug)
          System.out.println(
              " found intersecting dataSection: " + dataSection + " intersect= " + dataSection.intersect(want));

        index = new IndexChunkerTiled(dataSection, want); // new indexer into this chunk
        next = new Chunk(dataChunk.getByteBuffer()); // this does the uncompression

      } catch (InvalidRangeException | IOException e) {
        throw new IllegalStateException(e);
      }
    }

    IndexChunker.Chunk chunk = index.next();
    totalNelemsDone += chunk.getNelems();
    next.setDelegate(chunk);

    return true;
  }

  public LayoutBB.Chunk next() {
    return next;
  }

  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("want=").append(want).append("; ");
    sbuff.append("chunkSize=[");
    for (int i = 0; i < chunkSize.length; i++) {
      if (i > 0)
        sbuff.append(",");
      sbuff.append(chunkSize[i]);
    }
    sbuff.append("] totalNelems=").append(totalNelems);
    sbuff.append(" elemSize=").append(elemSize);
    return sbuff.toString();
  }

  public interface DataChunkIterator {
    boolean hasNext();

    DataChunk next() throws IOException;
  }

  public interface DataChunk {
    int[] getOffset();

    ByteBuffer getByteBuffer() throws IOException;
  }

  /**
   * A chunk of data that is contiguous in both the source and destination.
   * Everything is done in elements, not bytes.
   * Read nelems from src at srcPos, store in destination at destPos.
   */
  private static class Chunk implements LayoutBB.Chunk {
    IndexChunker.Chunk delegate;

    private final ByteBuffer bb;
    private ShortBuffer sb;
    private IntBuffer ib;
    private LongBuffer longb;
    private FloatBuffer fb;
    private DoubleBuffer db;

    Chunk(ByteBuffer bb) {
      this.bb = bb;
    }

    public void setDelegate(IndexChunker.Chunk delegate) {
      this.delegate = delegate;
    }

    @Override
    public int getSrcElem() {
      return (int) delegate.getSrcElem();
    }

    @Override
    public int getNelems() {
      return delegate.getNelems();
    }

    @Override
    public long getDestElem() {
      return delegate.getDestElem();
    }

    @Override
    public ByteBuffer getByteBuffer() {
      return bb;
    }

    @Override
    public ShortBuffer getShortBuffer() {
      if (sb == null)
        sb = bb.asShortBuffer();
      return sb;
    }

    @Override
    public IntBuffer getIntBuffer() {
      if (ib == null)
        ib = bb.asIntBuffer();
      return ib;
    }

    @Override
    public LongBuffer getLongBuffer() {
      if (longb == null)
        longb = bb.asLongBuffer();
      return longb;
    }

    @Override
    public FloatBuffer getFloatBuffer() {
      if (fb == null)
        fb = bb.asFloatBuffer();
      return fb;
    }

    @Override
    public DoubleBuffer getDoubleBuffer() {
      if (db == null)
        db = bb.asDoubleBuffer();
      return db;
    }

    @Override
    public String toString() {
      return delegate.toString();
    }

    // artifact of overriding Layout
    @Override
    public long getSrcPos() {
      throw new UnsupportedOperationException();
    }

  }

}
