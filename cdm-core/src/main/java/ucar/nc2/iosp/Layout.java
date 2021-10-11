/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

/**
 * Iterator to read/write subsets of a multidimensional array, finding the contiguous chunks.
 * The iteration is monotonic in both src and dest positions.
 * <p/>
 * Example for Integers:
 * 
 * <pre>
  int[] read( Layout index, int[] src) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Layout.Chunk chunk = index.next();
      System.arraycopy(src, chunk.getSrcElem(), dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

  int[] read( Layout index, RandomAccessFile raf) {
    int[] dest = new int[index.getTotalNelems()];
    while (index.hasNext()) {
      Layout.Chunk chunk = index.next();
      raf.seek( chunk.getSrcPos());
      raf.readInt(dest, chunk.getDestElem(), chunk.getNelems());
    }
    return dest;
  }

   // note src and dest misnamed
    void write( Layout index, int[] src, RandomAccessFile raf) {
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.seek ( chunk.getSrcPos());
        for (int k=0; k&lt;chunk.getNelems(); k++)
          raf.writeInt(src, chunk.getDestElem(), chunk.getNelems());
          raf.writeInt( ii.getByteNext());
      }
 * 
 * </pre>
 */
public interface Layout {

  /** Get total number of elements in the wanted subset. */
  long getTotalNelems();

  /** Get size of each element in bytes. */
  int getElemSize();

  /** Is there more to do? */
  boolean hasNext();

  /** Get the next chunk, not null if hasNext() is true. */
  Chunk next();

  /**
   * A chunk of data that is contiguous in both the source and destination.
   * Read nelems from src at filePos, store in destination at startElem.
   * (or) Write nelems to file at filePos, from array at startElem.
   */
  interface Chunk {

    /** Get the position in source where to read or write: eg "file position" */
    long getSrcPos();

    /** Get number of elements to transfer contiguously (Note: elements, not bytes) */
    int getNelems();

    /**
     * Get starting element position as a 1D element index into the destination, eg the requested array with shape
     * "wantSection".
     * 
     * @return starting element in the array (Note: elements, not bytes)
     */
    long getDestElem();
  }
}
