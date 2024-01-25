/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import ucar.nc2.util.IO;

/**
 * Filter implementation of zlib compression.
 */
public class Deflate extends Filter {
  private static final int MAX_ARRAY_LEN = Integer.MAX_VALUE - 8;

  private static final String name = "zlib";

  private static final int id = 1;

  private final int clevel; // compression level

  public Deflate(Map<String, Object> properties) {
    final Object levelObj = properties.get("level");
    if (levelObj == null) {
      this.clevel = 1; // default value
    } else if (levelObj instanceof String) {
      this.clevel = Integer.parseInt((String) levelObj);
    } else {
      this.clevel = ((Number) levelObj).intValue();
    }
    validateLevel();
  }

  private void validateLevel() {
    if (clevel < 0 || clevel > 9) {
      throw new IllegalArgumentException("Invalid compression level: " + clevel);
    }
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
  public byte[] encode(byte[] dataIn) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(dataIn.length);
    try (DeflaterOutputStream dos = new DeflaterOutputStream(os, new Deflater(clevel));) {
      dos.write(dataIn);
      // close everything and return
      dos.finish();
      dos.close();
      return os.toByteArray();
    }
  }

  @Override
  public byte[] decode(byte[] dataIn) throws IOException {
    int len = Math.min(8 * dataIn.length, MAX_ARRAY_LEN);
    try (ByteArrayInputStream in = new ByteArrayInputStream(dataIn);
        InflaterInputStream iis = new InflaterInputStream(in, new Inflater(), dataIn.length);
        ByteArrayOutputStream os = new ByteArrayOutputStream(len)) {

      IO.copyB(iis, os, IO.default_socket_buffersize);

      return os.toByteArray();
    }
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
      return new Deflate(properties);
    }
  }
}
