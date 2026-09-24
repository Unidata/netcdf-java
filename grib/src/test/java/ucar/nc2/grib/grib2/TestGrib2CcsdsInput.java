/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.io.ByteStreams;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.Test;
import ucar.unidata.io.InMemoryRandomAccessFile;

public class TestGrib2CcsdsInput {
  private static final int GRID_POINTS = 32776;
  private static final int HEADER_SIZE = 25;

  @Test
  public void largeCompressedFieldUsesBoundedScratchStorage() throws IOException {
    byte[] encoded = encodedField();
    assertThat(encoded.length).isGreaterThan(64 * 1024);
    try (TrackingFile file = file(encoded, encoded.length, Integer.MAX_VALUE, true)) {
      assertThat(read(file, GRID_POINTS, encoded.length)).isEqualTo(expectedValues());
      assertThat(file.maximumBufferSize).isAtMost(64 * 1024);
      assertThat(file.readCalls).isGreaterThan(1);
      assertThat(file.getFilePointer()).isEqualTo(HEADER_SIZE + encoded.length);
      assertThat(file.read()).isEqualTo(0x42);
    }
  }

  @Test
  public void partialReadsPreserveEverySample() throws IOException {
    byte[] encoded = encodedField();
    try (TrackingFile file = file(encoded, encoded.length, 97, true)) {
      assertThat(read(file, GRID_POINTS, encoded.length)).isEqualTo(expectedValues());
      assertThat(file.getFilePointer()).isEqualTo(HEADER_SIZE + encoded.length);
      assertThat(file.read()).isEqualTo(0x42);
    }
  }

  @Test
  public void truncatedCompressedFieldStillThrowsEof() throws IOException {
    byte[] encoded = encodedField();
    try (TrackingFile file = file(Arrays.copyOf(encoded, encoded.length - 1), encoded.length, 97, false)) {
      assertThrows(EOFException.class, () -> read(file, GRID_POINTS, encoded.length));
    }
  }

  @Test
  public void constantFieldNeedsNoInputTransfer() throws IOException {
    try (TrackingFile file = file(new byte[0], 0, Integer.MAX_VALUE, true)) {
      assertThat(read(file, 8, 0)).isEqualTo(new float[] {-2, -2, -2, -2, -2, -2, -2, -2});
      assertThat(file.readCalls).isEqualTo(0);
      assertThat(file.getFilePointer()).isEqualTo(HEADER_SIZE);
      assertThat(file.read()).isEqualTo(0x42);
    }
  }

  private static byte[] encodedField() throws IOException {
    try (InputStream input = TestGrib2CcsdsInput.class.getResourceAsStream("ccsds-input-blocks.bin")) {
      return ByteStreams.toByteArray(input);
    }
  }

  private static float[] expectedValues() {
    float[] values = new float[GRID_POINTS];
    int state = 42;
    for (int i = 0; i < values.length; i++) {
      // The fixture encodes these unsigned 16-bit samples with libaec 1.1.7 (flags 12, block 8, RSI 16).
      state ^= state << 13;
      state ^= state >>> 17;
      state ^= state << 5;
      values[i] = (-2 + (state & 0xffff) * 2.0f) / 10.0f;
    }
    return values;
  }

  private static TrackingFile file(byte[] payload, int encodedLength, int maximumRead, boolean trailer) {
    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payload.length + (trailer ? 1 : 0));
    buffer.putFloat(-2).putShort((short) 1).putShort((short) 1).put((byte) 16).put((byte) 0);
    buffer.put((byte) 12).put((byte) 8).putShort((short) 16);
    buffer.putInt(6).put((byte) 6).put((byte) 255);
    buffer.putInt(5 + encodedLength).put((byte) 7).put(payload);
    if (trailer) {
      buffer.put((byte) 0x42);
    }
    return new TrackingFile(buffer.array(), maximumRead);
  }

  private static float[] read(TrackingFile file, int points, int encodedLength) throws IOException {
    Grib2Drs.Type42 drs = new Grib2Drs.Type42(file);
    Grib2SectionBitMap bitmap = new Grib2SectionBitMap(file);
    return new Grib2DataReader(42, points, points, 0, 4, HEADER_SIZE - 5, encodedLength + 5).getData(file, bitmap, drs);
  }

  private static final class TrackingFile extends InMemoryRandomAccessFile {
    private final int maximumRead;
    private int maximumBufferSize;
    private int readCalls;

    TrackingFile(byte[] content, int maximumRead) {
      super("ccsds-input", content);
      this.maximumRead = maximumRead;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      maximumBufferSize = Math.max(maximumBufferSize, bytes.length);
      readCalls++;
      return super.read(bytes, offset, Math.min(length, maximumRead));
    }
  }
}
