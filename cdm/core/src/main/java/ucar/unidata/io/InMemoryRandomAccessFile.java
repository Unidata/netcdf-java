/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.unidata.io.spi.RandomAccessFileProvider;

/** A RandomAccessFile stored entirely in memory as a byte array. */
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

    if (debugLeaks)
      openFiles.add(location);
  }

  @Override
  public long length() {
    return dataEnd;
  }

  // @Override LOOK weird error
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
    return dest.write(ByteBuffer.wrap(buffer, (int) offset, (int) nbytes));
  }

  /**
   * Hook for service provider interface RandomAccessFileProvider
   */
  public static class Provider implements RandomAccessFileProvider {

    @Override
    public boolean isOwnerOf(String location) {
      return location.startsWith("slurp:");
    }

    @Override
    public RandomAccessFile open(String location) throws IOException {
      String scheme = location.split(":")[0];
      location = location.replace(scheme, "http");
      Optional<byte[]> contents;
      try (HTTPMethod method = HTTPFactory.Get(location)) {
        method.execute();
        contents = Optional.of(method.getResponseAsBytes());
      } catch (HTTPException he) {
        throw new IOException(he);
      }
      return new InMemoryRandomAccessFile(location, contents.get());
    }
  }

}
