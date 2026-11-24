/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libzstd;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filter;
import ucar.nc2.filter.Filters;
import ucar.nc2.filter.UnknownFilterException;

public class TestZstdFilter {
  private static final String DATA_DIR = "src/test/data/";
  private static final String RAW = DATA_DIR + "zstd_raw.nc";
  private static final String COMPRESSED = DATA_DIR + "zstd_32015.nc";
  private static final String[] VARIABLES = {"time", "temp", "count"};

  @Test
  public void testProviderIsRegistered() throws UnknownFilterException {
    Map<String, Object> props = new HashMap<>();
    props.put(Filters.Keys.ID, (short) 32015);
    props.put(Filters.Keys.DATA, new int[] {3});
    Filter filter = Filters.getFilter(props);
    assertThat(filter).isInstanceOf(ZstdFilter.class);
    assertThat(filter.getId()).isEqualTo(32015);
  }

  @Test
  public void testProviderByName() throws UnknownFilterException {
    Map<String, Object> props = new HashMap<>();
    props.put(Filters.Keys.NAME, "zstd");
    Filter filter = Filters.getFilter(props);
    assertThat(filter).isInstanceOf(ZstdFilter.class);
  }

  @Test
  public void testDecode() throws IOException {
    Filter filter = new ZstdFilter(new HashMap<>());
    assertThat(filter.decode(readBytes("zstd_frame.zst"))).isEqualTo(readBytes("zstd_frame.bin"));
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
    // temp is chunked (5, 8, 16) over (12, 20, 30) with shuffle + zstd: this section touches interior and partial
    // edge chunks
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

  private static byte[] readBytes(String name) throws IOException {
    return Files.readAllBytes(Paths.get(DATA_DIR, name));
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
