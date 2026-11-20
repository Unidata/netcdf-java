/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libblosc2;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.filter.Filter;
import ucar.nc2.filter.FilterProvider;
import ucar.nc2.filter.Filters;

/**
 * Decoder for the HDF5 Blosc2 filter (id 32026).
 * <p>
 * The filter parameters ({@code cd_values}) are set by the HDF5 plugin ({@code H5Zblosc2.c}):
 * {@code [0]} filter revision, {@code [1]} Blosc2 version, {@code [2]} type size, {@code [3]} chunk size in
 * bytes, {@code [4]} compression level, {@code [5]} shuffle, {@code [6]} codec, and for chunks of rank 2 or
 * more {@code [7]} rank followed by the chunk shape. Only {@code [3]} is needed for decoding; shuffle and
 * codec are recorded in each frame and handled by C-Blosc2.
 */
public class Blosc2Filter extends Filter {
  private static final Logger logger = LoggerFactory.getLogger(Blosc2Filter.class);
  private static final String name = "blosc2";
  private static final int id = 32026;
  private static final int CD_CHUNK_BYTES = 3;

  private final Map<String, Object> properties;
  private final int chunkBytes;

  public Blosc2Filter(Map<String, Object> properties) {
    this.properties = new HashMap<>(properties);
    Object data = this.properties.get(Filters.Keys.DATA);
    this.chunkBytes =
        (data instanceof int[] && ((int[]) data).length > CD_CHUNK_BYTES) ? ((int[]) data)[CD_CHUNK_BYTES] : -1;
    logger.debug("Blosc2 properties = {}", this.properties);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public byte[] encode(byte[] dataIn) {
    throw new UnsupportedOperationException("Blosc2 encoding is not supported");
  }

  @Override
  public byte[] decode(byte[] dataIn) throws IOException {
    if (chunkBytes <= 0) {
      Object data = properties.get(Filters.Keys.DATA);
      throw new IOException("Blosc2 filter parameters do not contain the chunk size (cd_values[3]): "
          + (data instanceof int[] ? Arrays.toString((int[]) data) : data));
    }
    return LibBlosc2.decodeFrame(dataIn, chunkBytes);
  }

  public Map<String, Object> properties() {
    return new HashMap<>(properties);
  }

  public static class Provider implements FilterProvider {

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
      return new Blosc2Filter(properties);
    }
  }
}
