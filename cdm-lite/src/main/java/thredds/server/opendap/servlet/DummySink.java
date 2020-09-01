/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


/**
 * The Servlet to exercise what's available.
 *
 * @author Nathan David Potter
 */

public class DummySink extends DeflaterOutputStream {

  int count = 0;

  /**
   * Creates a new output stream with the specified compressor and
   * buffer size.
   *
   * @param out the output stream
   * @param def the compressor ("deflater")
   * @throws IllegalArgumentException if size is <= 0
   */
  public DummySink(OutputStream out, Deflater def, int size) {
    super(out);
    count = 0;
  }

  /**
   * Creates a new output stream with the specified compressor and
   * a default buffer size.
   *
   * @param out the output stream
   * @param def the compressor ("deflater")
   */
  public DummySink(OutputStream out, Deflater def) {
    this(out, def, 512);
  }

  /**
   * Creates a new output stream with a defaul compressor and buffer size.
   */
  public DummySink(OutputStream out) {
    this(out, new Deflater());
  }

  // Closes this output stream and releases any system resources associated with this stream.
  public void close() {}


  public void flush() {}

  public void write(int b) throws IOException {
    count++;
    super.write(b);
  }

  /**
   * Writes an array of bytes to the compressed output stream. This
   * method will block until all the bytes are written.
   *
   * @param off the start offset of the data
   * @param len the length of the data
   * @throws IOException if an I/O error has occurred
   */
  public void write(byte[] b, int off, int len) throws IOException {

    count += len;
    super.write(b, off, len);

  }

  public int getCount() {
    return count;
  }

  public void setCount(int c) {
    count = c;
  }

  public void resetCount() {
    count = 0;
  }


}


