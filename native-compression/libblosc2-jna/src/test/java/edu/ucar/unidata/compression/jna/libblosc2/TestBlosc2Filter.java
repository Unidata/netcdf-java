/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libblosc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filter;
import ucar.nc2.filter.Filters;
import ucar.nc2.filter.UnknownFilterException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;

public class TestBlosc2Filter {
  private static final String DATA_DIR = "src/test/data/";
  private static final String RAW = DATA_DIR + "blosc2_raw.nc";
  private static final String COMPRESSED = DATA_DIR + "blosc2_32026.nc";
  private static final String[] VARIABLES = {"time", "temp", "count"};
  private static final int[] TEMP_CD_VALUES = {1, 0, 4, 2560, 5, 2, 5, 3, 5, 8, 16};
  private static final int[] TIME_CD_VALUES = {1, 0, 8, 40, 3, 0, 5};

  @Test
  public void testProviderIsRegistered() throws UnknownFilterException {
    Map<String, Object> props = new HashMap<>();
    props.put(Filters.Keys.ID, (short) 32026);
    props.put(Filters.Keys.DATA, TEMP_CD_VALUES);
    Filter filter = Filters.getFilter(props);
    assertThat(filter).isInstanceOf(Blosc2Filter.class);
    assertThat(filter.getId()).isEqualTo(32026);
  }

  @Test
  public void testProviderByName() throws UnknownFilterException {
    Map<String, Object> props = new HashMap<>();
    props.put(Filters.Keys.NAME, "blosc2");
    Filter filter = Filters.getFilter(props);
    assertThat(filter).isInstanceOf(Blosc2Filter.class);
  }

  @Test
  public void testReadMatchesUncompressed() throws IOException {
    try (NetcdfFile raw = NetcdfFiles.open(RAW); NetcdfFile compressed = NetcdfFiles.open(COMPRESSED)) {
      for (String name : VARIABLES) {
        Variable expected = raw.findVariable(name);
        Variable actual = compressed.findVariable(name);
        assertNotNull(expected);
        assertNotNull(actual);
        assertSameValues(name, expected.read(), actual.read());
      }
    }
  }

  @Test
  public void testReadSubsetAcrossEdgeChunks() throws Exception {
    // temp is chunked (5, 8, 16) over (12, 20, 30): this section touches interior and partial edge chunks
    String section = "3:11,6:19,10:29";
    try (NetcdfFile raw = NetcdfFiles.open(RAW); NetcdfFile compressed = NetcdfFiles.open(COMPRESSED)) {
      Array expected = raw.findVariable("temp").read(section);
      Array actual = compressed.findVariable("temp").read(section);
      assertSameValues("temp(" + section + ")", expected, actual);
    }
  }

  @Test
  public void testMultithreadedRead() throws Exception {
    Array expected;
    try (NetcdfFile raw = NetcdfFiles.open(RAW)) {
      expected = raw.findVariable("temp").read();
    }
    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      List<Future<Array>> results = new ArrayList<>();
      for (int i = 0; i < 16; i++) {
        Callable<Array> task = () -> {
          try (NetcdfFile compressed = NetcdfFiles.open(COMPRESSED)) {
            return compressed.findVariable("temp").read();
          }
        };
        results.add(executor.submit(task));
      }
      for (Future<Array> result : results) {
        assertSameValues("temp", expected, result.get());
      }
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void testRejectsNonFrameInput() {
    Blosc2Filter filter = newFilter(TEMP_CD_VALUES);
    assertThrows(IOException.class, () -> filter.decode(new byte[] {1, 2, 3}));
    assertThrows(IOException.class, () -> filter.decode(new byte[64]));
  }

  @Test
  public void testDecodeChunkRank3() throws IOException {
    Blosc2Filter filter = newFilter(TEMP_CD_VALUES);
    assertThat(filter.decode(readBytes("blosc2_temp_chunk0.b2frame")))
        .isEqualTo(readBytes("blosc2_temp_chunk0.expected"));
  }

  @Test
  public void testDecodeChunkRank1() throws IOException {
    Blosc2Filter filter = newFilter(TIME_CD_VALUES);
    assertThat(filter.decode(readBytes("blosc2_time_chunk0.b2frame")))
        .isEqualTo(readBytes("blosc2_time_chunk0.expected"));
  }

  @Test
  public void testRejectsTruncatedFrame() throws IOException {
    Blosc2Filter filter = newFilter(TEMP_CD_VALUES);
    byte[] frame = readBytes("blosc2_temp_chunk0.b2frame");
    for (int keep : new int[] {frame.length / 4, frame.length / 2, frame.length * 3 / 4, frame.length - 1}) {
      byte[] truncated = Arrays.copyOf(frame, keep);
      assertThrows(IOException.class, () -> filter.decode(truncated));
    }
  }

  @Test
  public void testRejectsWrongChunkSize() throws IOException {
    int[] cdValues = TEMP_CD_VALUES.clone();
    cdValues[3] = 1280; // half of the real chunk size
    Blosc2Filter filter = newFilter(cdValues);
    assertThrows(IOException.class, () -> filter.decode(readBytes("blosc2_temp_chunk0.b2frame")));
  }

  private static byte[] readBytes(String name) throws IOException {
    return Files.readAllBytes(Paths.get(DATA_DIR, name));
  }

  @Test
  public void testRequiresChunkSizeParameter() {
    Blosc2Filter filter = newFilter(new int[] {1, 0, 4});
    assertThrows(IOException.class, () -> filter.decode(new byte[64]));
  }

  private static Blosc2Filter newFilter(int[] cdValues) {
    Map<String, Object> props = new HashMap<>();
    props.put(Filters.Keys.ID, (short) 32026);
    props.put(Filters.Keys.DATA, cdValues);
    return new Blosc2Filter(props);
  }

  private static void assertSameValues(String name, Array expected, Array actual) {
    assertThat(actual.getElementType()).isEqualTo(expected.getElementType());
    assertThat(actual.getShape()).isEqualTo(expected.getShape());
    Object expectedValues = expected.copyTo1DJavaArray();
    Object actualValues = actual.copyTo1DJavaArray();
    if (expectedValues instanceof float[]) {
      assertThat((float[]) actualValues).isEqualTo((float[]) expectedValues);
    } else if (expectedValues instanceof double[]) {
      assertThat((double[]) actualValues).isEqualTo((double[]) expectedValues);
    } else if (expectedValues instanceof int[]) {
      assertThat((int[]) actualValues).isEqualTo((int[]) expectedValues);
    } else {
      throw new AssertionError("unexpected type for " + name + ": " + expected.getElementType());
    }
  }
}
