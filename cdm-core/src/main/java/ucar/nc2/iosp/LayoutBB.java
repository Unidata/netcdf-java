/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import java.nio.*;

/**
 * A Layout that supplies the "source" ByteBuffer.
 * This is used when the data must be massaged after being read, eg uncompresed or filtered.
 * The modified data is placed in a ByteBuffer, which may change for different chunks, and
 * so is supplied by each chunk.
 * 
 * <p/>
 * Example for Integers:
 * 
 * <pre>
 * int[] read(LayoutBB index, int[] pa) {
 *   while (index.hasNext()) {
 *     LayoutBB.Chunk chunk = index.next();
 *     IntBuffer buff = chunk.getIntBuffer();
 *     buff.position(chunk.getSrcElem());
 *     int pos = (int) chunk.getDestElem();
 *     for (int i = 0; i &lt; chunk.getNelems(); i++)
 *       pa[pos++] = buff.get();
 *   }
 *   return pa;
 * }
 * </pre>
 */

public interface LayoutBB extends Layout {

  @Override
  LayoutBB.Chunk next(); // covariant return.

  /**
   * A contiguous chunk of data as a ByteBuffer.
   * Read nelems from ByteBuffer at filePos, store in destination at startElem.
   */
  interface Chunk extends Layout.Chunk {

    /** Get the position as a element index where to read or write: "buffer position" */
    int getSrcElem();

    ByteBuffer getByteBuffer();

    ShortBuffer getShortBuffer();

    IntBuffer getIntBuffer();

    FloatBuffer getFloatBuffer();

    DoubleBuffer getDoubleBuffer();

    LongBuffer getLongBuffer();
  }
}
