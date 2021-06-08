/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/** A RandomAccessFile stored entirely in memory as a byte array. Read only, do not call write() methods. */
public class InMemoryRandomAccessFile extends ucar.unidata.io.RandomAccessFile {

  /**
   * Constructor.
   *
   * @param name used as the location
   * @param data the complete data file
   */
  public InMemoryRandomAccessFile(String name, byte[] data) {
    super(1);
    this.location = name;
    this.file = null;
    if (data == null)
      throw new IllegalArgumentException("data array is null");

    buffer = data;
    bufferStart = 0;
    dataSize = buffer.length;
    dataEnd = buffer.length;
    filePosition = 0;
    endOfFile = false;

    if (debugLeaks) {
      openFiles.add(location);
    }
  }

  @Override
  public long length() {
    return dataEnd;
  }

  @Override
  public void setBufferSize(int bufferSize) {
    // do nothing
  }

  @Override
  protected int read_(long pos, byte[] b, int offset, int len) {
    len = Math.min(len, (int) (buffer.length - pos));
    // copy out of buffer
    System.arraycopy(buffer, (int) pos, b, offset, len);
    return len;
  }

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    long length = Math.min(nbytes, buffer.length - offset);
    ByteBuffer src = ByteBuffer.wrap(buffer, (int) offset, (int) length);
    int n = dest.write(src);
    return n;
  }

}

