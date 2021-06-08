/*
 *  Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */

package ucar.nc2.internal.io;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Static utility routines for streaming data, from NcStream. */
public class Streams {

  public static int readFully(InputStream is, byte[] b) throws IOException {
    int done = 0;
    int want = b.length;
    while (want > 0) {
      int bytesRead = is.read(b, done, want);
      if (bytesRead == -1)
        break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  public static ByteBuffer readByteBuffer(InputStream in) throws IOException {
    int vsize = readVInt(in);
    byte[] b = new byte[vsize];
    readFully(in, b);
    return ByteBuffer.wrap(b);
  }

  public static String readString(InputStream in) throws IOException {
    int vsize = readVInt(in);
    byte[] b = new byte[vsize];
    readFully(in, b);
    return new String(b, StandardCharsets.UTF_8);
  }

  public static int readVInt(InputStream is) throws IOException {
    int ib = is.read();
    if (ib == -1)
      return -1;

    byte b = (byte) ib;
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      ib = is.read();
      if (ib == -1)
        return -1;
      b = (byte) ib;
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  public static int readVInt(RandomAccessFile raf) throws IOException {
    int ib = raf.read();
    if (ib == -1)
      return -1;

    byte b = (byte) ib;
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      ib = raf.read();
      if (ib == -1)
        return -1;
      b = (byte) ib;
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  public static boolean readAndTest(InputStream is, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    readFully(is, b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i])
        return false;
    return true;
  }

  public static boolean readAndTest(RandomAccessFile raf, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    raf.readFully(b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i])
        return false;
    return true;
  }

  public static int writeVInt(OutputStream out, int value) throws IOException {
    int count = 0;

    // stolen from protobuf.CodedOutputStream.writeRawVarint32()
    while (true) {
      if ((value & ~0x7F) == 0) {
        writeByte(out, (byte) value);
        break;
      } else {
        writeByte(out, (byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    return count + 1;
  }

  public static int writeVInt(RandomAccessFile out, int value) throws IOException {
    int count = 0;

    while (true) {
      if ((value & ~0x7F) == 0) {
        out.write((byte) value);
        break;
      } else {
        out.write((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    return count + 1;
  }

  static int writeByte(OutputStream out, byte b) throws IOException {
    out.write(b);
    return 1;
  }

  public static int writeByteBuffer(OutputStream out, ByteBuffer bb) throws IOException {
    int vsize = writeVInt(out, bb.limit());
    bb.rewind();
    out.write(bb.array());
    return vsize + bb.limit();
  }

  public static int writeString(OutputStream out, String s) throws IOException {
    int vsize = writeVInt(out, s.length());
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    out.write(b);
    return vsize + b.length;
  }

}
