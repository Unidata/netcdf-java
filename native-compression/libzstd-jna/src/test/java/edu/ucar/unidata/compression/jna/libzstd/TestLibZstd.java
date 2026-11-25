/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libzstd;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.Test;

public class TestLibZstd {
  private static final String DATA_DIR = "src/test/data/";

  @Test
  public void testVersion() {
    assertThat(LibZstd.version()).matches("\\d+\\.\\d+\\.\\d+");
  }

  @Test
  public void testDecodeFrame() throws IOException {
    assertThat(LibZstd.decode(readBytes("zstd_frame.zst"))).isEqualTo(readBytes("zstd_frame.bin"));
  }

  @Test
  public void testDecodeTwoFrames() throws IOException {
    byte[] expected = readBytes("zstd_frame.bin");
    byte[] twice = Arrays.copyOf(expected, 2 * expected.length);
    System.arraycopy(expected, 0, twice, expected.length, expected.length);
    assertThat(LibZstd.decode(readBytes("zstd_two_frames.zst"))).isEqualTo(twice);
  }

  @Test
  public void testRejectsFrameWithoutContentSize() throws IOException {
    byte[] frame = readBytes("zstd_frame_no_size.zst");
    IOException e = assertThrows(IOException.class, () -> LibZstd.decode(frame));
    assertThat(e).hasMessageThat().contains("decompressed size");
  }

  @Test
  public void testRejectsTruncatedFrame() throws IOException {
    byte[] frame = readBytes("zstd_frame.zst");
    for (int keep : new int[] {3, 8, frame.length / 2, frame.length - 1}) {
      byte[] truncated = Arrays.copyOf(frame, keep);
      assertThrows(IOException.class, () -> LibZstd.decode(truncated));
    }
  }

  @Test
  public void testRejectsCorruptFrame() throws IOException {
    byte[] frame = readBytes("zstd_frame.zst");
    byte[] corrupt = frame.clone();
    for (int i = 20; i < corrupt.length - 4; i += 7) {
      corrupt[i] ^= (byte) 0x5a;
    }
    assertThrows(IOException.class, () -> LibZstd.decode(corrupt));
  }

  @Test
  public void testRejectsNonZstdInput() {
    assertThrows(IOException.class, () -> LibZstd.decode(null));
    assertThrows(IOException.class, () -> LibZstd.decode(new byte[0]));
    assertThrows(IOException.class, () -> LibZstd.decode(new byte[] {1, 2, 3}));
    assertThrows(IOException.class, () -> LibZstd.decode(new byte[64]));
  }

  private static byte[] readBytes(String name) throws IOException {
    return Files.readAllBytes(Paths.get(DATA_DIR, name));
  }
}
