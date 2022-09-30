/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import com.google.common.primitives.Ints;

import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Filter implementation for 32-bit checksums: Fletcher, Adler, and CRC
 */
public class Checksum32 extends Filter {

  public enum CType {
    FLETCHER("fletcher32", 3), ADLER("adler32", -1), CRC("crc32", -1);

    private final String name;

    private final int id;

    CType(String name, int id) {
      this.name = name;
      this.id = id;
    }
  }

  private static final int nbytes = 4; // number of bytes in the checksum

  private final CType type; // type of checksum

  public Checksum32(CType type) {
    this.type = type;
  }

  @Override
  public String getName() {
    return type.name;
  }

  @Override
  public int getId() {
    return type.id;
  }

  @Override
  public byte[] encode(byte[] dataIn) {
    // create a checksum
    int checksum = (int) getChecksum(dataIn);
    // append checksum in front or behind data
    // Adler and CRC are supported by Zarr, which follows the NumCodec spec with a checksum before the data
    // Fletcher is support by hdf5, which has the checksum after the data
    byte[] dataOut = new byte[dataIn.length + nbytes];
    int dataStart = this.type == CType.FLETCHER ? 0 : nbytes;
    System.arraycopy(dataIn, 0, dataOut, dataStart, dataIn.length);
    int checksumStart = this.type == CType.FLETCHER ? dataOut.length - nbytes : 0;
    // encode as little endian by default
    System.arraycopy(Ints.toByteArray(Integer.reverseBytes(checksum)), 0, dataOut, checksumStart, nbytes);;
    return dataOut;
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    // strip the checksum
    byte[] dataOut = new byte[dataIn.length - nbytes];
    // Adler and CRC are supported by Zarr, which follows the NumCodec spec with a checksum before the data
    // Fletcher is support by hdf5, which has the checksum after the data
    int dataStart = this.type == CType.FLETCHER ? 0 : nbytes;
    System.arraycopy(dataIn, dataStart, dataOut, 0, dataOut.length);
    // verify checksum
    int checksum = (int) getChecksum(dataOut);
    byte[] bytes = new byte[nbytes];
    int checksumStart = this.type == CType.FLETCHER ? dataIn.length - nbytes : 0;
    System.arraycopy(dataIn, checksumStart, bytes, 0, nbytes);
    int i = Integer.reverseBytes(Ints.fromByteArray(bytes)); // convert from little endian
    if (i != checksum) {
      throw new RuntimeException("Checksum invalid");
    }
    // return data
    return dataOut;
  }

  private long getChecksum(byte[] data) {
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
    return checksum.getValue();
  }

  private class Fletcher32 extends Adler32 {

    private long sum1 = 0;
    private long sum2 = 0;

    @Override
    public void update(byte[] b, int off, int len) {
      if (b == null) {
        throw new NullPointerException();
      }
      if (off < 0 || len < 0 || off > b.length - len) {
        throw new ArrayIndexOutOfBoundsException();
      }

      int i = 0;
      int end = len / 2;
      while (end > 0) {
        int blocklen = end > 360 ? 360 : end;
        end -= blocklen;
        do {
          sum1 += (b[i] & 0xff) << 8 | b[i + 1] & 0xff;
          sum2 += sum1;
          i += 2;
          blocklen--;
        } while (blocklen > 0);
        sum1 = (sum1 & 0xffff) + (sum1 >>> 16);
        sum2 = (sum2 & 0xffff) + (sum2 >>> 16);
      }

      // handle odd # of bytes
      if (len % 2 > 0) {
        sum1 += (b[len - 1] & 0xff) << 8;
        sum2 += sum1;
        sum1 = (sum1 & 0xffff) + (sum1 >>> 16);
        sum2 = (sum2 & 0xffff) + (sum2 >>> 16);
      }

      sum1 = (sum1 & 0xffff) + (sum1 >>> 16);
      sum2 = (sum2 & 0xffff) + (sum2 >>> 16);
    }

    @Override
    public long getValue() {
      return (sum2 << 16) | sum1;
    }
  }

  public static class Fletcher32Provider implements FilterProvider {

    @Override
    public String getName() {
      return CType.FLETCHER.name;
    }

    @Override
    public int getId() {
      return CType.FLETCHER.id;
    }

    @Override
    public Filter create(Map<String, Object> properties) {
      return new Checksum32(CType.FLETCHER);
    }
  }

  public static class Adler32Provider implements FilterProvider {

    @Override
    public String getName() {
      return CType.ADLER.name;
    }

    @Override
    public int getId() {
      return CType.ADLER.id;
    }

    @Override
    public Filter create(Map<String, Object> properties) {
      return new Checksum32(CType.ADLER);
    }
  }

  public static class CRC32Provider implements FilterProvider {

    @Override
    public String getName() {
      return CType.CRC.name;
    }

    @Override
    public int getId() {
      return CType.CRC.id;
    }

    @Override
    public Filter create(Map<String, Object> properties) {
      return new Checksum32(CType.CRC);
    }
  }
}
