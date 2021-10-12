/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import com.google.common.primitives.Ints;

import java.nio.ByteOrder;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Filter implementation for 32-bit checksums: Fletcher, Adler, and CRC
 */
public class Checksum32 extends Filter {

  public enum CType {
    FLETCHER, ADLER, CRC
  }

  private static final int nbytes = 4; // number of bytes in the checksum

  private final CType type; // type of checksum

  private final ByteOrder byteOrder;

  public Checksum32(CType type, ByteOrder bo) {
    this.type = type;
    this.byteOrder = bo;
  }

  public Checksum32(CType type) {
    this(type, ByteOrder.BIG_ENDIAN);
  }

  @Override
  public byte[] encode(byte[] dataIn) {
    // create a checksum
    int checksum = getChecksum(dataIn);
    // append checksum in front of data
    byte[] dataOut = new byte[dataIn.length + nbytes];
    System.arraycopy(dataIn, 0, dataOut, nbytes, dataIn.length);
    System.arraycopy(Ints.toByteArray(checksum), 0, dataOut, 0, nbytes);;
    return dataOut;
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    // strip the checksum
    byte[] dataOut = new byte[dataIn.length - nbytes];
    System.arraycopy(dataIn, nbytes, dataOut, 0, dataOut.length);
    // verify checksum
    int checksum = getChecksum(dataOut);
    byte[] bytes = new byte[nbytes];
    System.arraycopy(dataIn, 0, bytes, 0, nbytes);
    int i = Ints.fromByteArray(bytes);
    if (i != checksum) {
      throw new RuntimeException("Checksum invalid");
    }
    // return data
    return dataOut;
  }

  private int getChecksum(byte[] data) {
    Checksum checksum;
    switch (type) {
      case ADLER:
        checksum = new Adler32();
        break;
      case CRC:
        checksum = new CRC32();
        break;
      case FLETCHER:
      default:
        checksum = new Fletcher32();
        break;
    }
    checksum.update(data, 0, data.length);
    int val = (int) checksum.getValue();
    // reverse bytes for little endian
    if (this.byteOrder == ByteOrder.LITTLE_ENDIAN) {
      val = Integer.reverseBytes(val);
    }
    return val;
  }

  private class Fletcher32 extends Adler32 {

    private int sum1 = 0;
    private int sum2 = 0;

    @Override
    public void update(byte[] b, int off, int len) {
      if (b == null) {
        throw new NullPointerException();
      }
      if (off < 0 || len < 0 || off > b.length - len) {
        throw new ArrayIndexOutOfBoundsException();
      }
      for (int i = off; i < len; i++) {
        sum1 = (sum1 + (b[i] & 0xff)) % 65535;
        sum2 = (sum2 + sum1) % 65535;
      }
    }
  }

  public static class Fletcher32Provider implements FilterProvider {

    private static final String name = "fletcher32";

    private static final int id = -1;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int getId() {
      return id;
    }

    @Override
    public Filter create(Map<String, Object> properties) {
      return new Checksum32(CType.FLETCHER);
    }
  }

  public static class Adler32Provider implements FilterProvider {

    private static final String name = "adler32";

    private static final int id = -1; // not yet implemented by id

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int getId() {
      return id;
    }

    @Override
    public Filter create(Map<String, Object> properties) {
      return new Checksum32(CType.ADLER);
    }
  }

  public static class CRC32Provider implements FilterProvider {

    private static final String name = "crc32";

    private static final int id = -1; // not yet implemented by id

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int getId() {
      return id;
    }

    @Override
    public Filter create(Map<String, Object> properties) {
      return new Checksum32(CType.CRC);
    }
  }
}
