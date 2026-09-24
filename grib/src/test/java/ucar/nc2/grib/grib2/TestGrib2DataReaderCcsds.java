/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.io.InMemoryRandomAccessFile;

@RunWith(Parameterized.class)
public class TestGrib2DataReaderCcsds {
  // Unsigned, most-significant byte first, with preprocessing (GRIB2 code table 5.42).
  private static final int COMPRESSION_OPTIONS = 4 | 8;
  private static final int BLOCK_SIZE = 8;
  private static final int REFERENCE_SAMPLE_INTERVAL = 16;
  private static final int GRID_POINTS = 8;
  private static final int NX = 4;

  // Small unsigned sample streams encoded with libaec, using the settings above.
  @Parameterized.Parameters(name = "{0} bits per sample")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{8, "C01F8084425529F0", "C01E03844AA7C000"},
        {16, "E0000FC040020020010028028014FFF8", "E0000F01C0020020050053FFE0000000"},
        {32, "F000000007E02000000100000010000000800000140000014000000A7FFFFFFC",
            "F00000000780E00000010000001000000280000029FFFFFFF000000000000000"}});
  }

  private final int bits;
  private final byte[] fullGrid;
  private final byte[] withBitmap;

  public TestGrib2DataReaderCcsds(int bits, String fullGrid, String withBitmap) {
    this.bits = bits;
    this.fullGrid = BaseEncoding.base16().decode(fullGrid);
    this.withBitmap = BaseEncoding.base16().decode(withBitmap);
  }

  @Test
  public void decodedArrayHasOneValuePerGridPoint() throws IOException {
    long[] values = {0, 1, 2, 3, 10, 20, 30, (1L << bits) - 1};
    assertThat(read(values, null, 0)).isEqualTo(scaled(values));
  }

  @Test
  public void bitmapRetainsMissingGridPoints() throws IOException {
    long[] values = {0, 1, 2, 10, 20, (1L << bits) - 1};
    float[] scaled = scaled(values);
    float[] expected = {scaled[0], Float.NaN, scaled[1], scaled[2], Float.NaN, scaled[3], scaled[4], scaled[5]};
    assertThat(read(values, new byte[] {(byte) 0b10110111}, 0)).isEqualTo(expected);
  }

  @Test
  public void negativeXScanReversesEachRow() throws IOException {
    long[] values = {0, 1, 2, 3, 10, 20, 30, (1L << bits) - 1};
    float[] scaled = scaled(values);
    float[] expected = {scaled[3], scaled[2], scaled[1], scaled[0], scaled[7], scaled[6], scaled[5], scaled[4]};
    assertThat(read(values, null, 128)).isEqualTo(expected);
  }

  @Test
  public void alternatingScanReversesOnlyAlternateRows() throws IOException {
    long[] values = {0, 1, 2, 3, 10, 20, 30, (1L << bits) - 1};
    float[] scaled = scaled(values);
    float[] expected = {scaled[0], scaled[1], scaled[2], scaled[3], scaled[7], scaled[6], scaled[5], scaled[4]};
    assertThat(read(values, null, 16)).isEqualTo(expected);
  }

  @Test
  public void decodedValuesRemainIndependentAfterOtherReads() throws IOException {
    long[] values = {0, 1, 2, 3, 10, 20, 30, (1L << bits) - 1};
    float[] first = read(values, null, 0);
    for (int i = 0; i < 16; i++) {
      float[] next = read(new long[] {0, 1, 2, 10, 20, (1L << bits) - 1}, new byte[] {(byte) 0b10110111}, 0);
      Arrays.fill(next, Float.NaN);
    }
    assertThat(first).isEqualTo(scaled(values));
  }

  private float[] read(long[] values, byte[] bitmap, int scanMode) throws IOException {
    byte[] encoded = bitmap == null ? fullGrid : withBitmap;
    int bitmapLength = 6 + (bitmap == null ? 0 : bitmap.length);
    int dataLength = 5 + encoded.length;
    ByteBuffer buffer = ByteBuffer.allocate(14 + bitmapLength + dataLength);
    // Template 5.42, beginning at its reference value (octet 12).
    buffer.putFloat(-2).putShort((short) 1).putShort((short) 1).put((byte) bits).put((byte) 0);
    buffer.put((byte) COMPRESSION_OPTIONS).put((byte) BLOCK_SIZE).putShort((short) REFERENCE_SAMPLE_INTERVAL);
    buffer.putInt(bitmapLength).put((byte) 6).put((byte) (bitmap == null ? 255 : 0));
    if (bitmap != null) {
      buffer.put(bitmap);
    }
    long dataStart = buffer.position();
    buffer.putInt(dataLength).put((byte) 7).put(encoded);

    try (InMemoryRandomAccessFile raf = new InMemoryRandomAccessFile("ccsds", buffer.array())) {
      Grib2Drs.Type42 drs = new Grib2Drs.Type42(raf);
      Grib2SectionBitMap bitmapSection = new Grib2SectionBitMap(raf);
      Grib2DataReader reader = new Grib2DataReader(42, GRID_POINTS, values.length, scanMode, NX, dataStart, dataLength);
      return reader.getData(raf, bitmapSection, drs);
    }
  }

  private static float[] scaled(long[] values) {
    float[] result = new float[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (-2 + values[i] * 2.0f) / 10.0f;
    }
    return result;
  }
}
