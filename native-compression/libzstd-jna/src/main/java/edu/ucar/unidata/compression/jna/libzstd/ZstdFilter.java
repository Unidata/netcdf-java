/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package edu.ucar.unidata.compression.jna.libzstd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.filter.Filter;
import ucar.nc2.filter.FilterProvider;

/**
 * Decoder for the HDF5 Zstandard filter (id 32015).
 * <p>
 * Each HDF5 chunk is stored as one zstd frame. The only filter parameter ({@code cd_values[0]}) is the compression
 * level, which is not needed for decoding. Byte shuffling, if used, is a separate filter in the pipeline.
 */
public class ZstdFilter extends Filter {
  private static final Logger logger = LoggerFactory.getLogger(ZstdFilter.class);
  private static final String name = "zstd";
  private static final int id = 32015;

  private final Map<String, Object> properties;

  public ZstdFilter(Map<String, Object> properties) {
    this.properties = new HashMap<>(properties);
    logger.debug("zstd properties = {}", this.properties);
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
    throw new UnsupportedOperationException("zstd encoding is not supported");
  }

  @Override
  public byte[] decode(byte[] dataIn) throws IOException {
    return LibZstd.decode(dataIn);
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
      return new ZstdFilter(properties);
    }
  }
}
