/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.util.Map;

/**
 * Filter implementation of Shuffle.
 */
public class Shuffle extends Filter {

  private int elemSize;
  private static final int DEFAULT_SIZE = 4;

  public Shuffle(Map<String, Object> properties) {
    try {
      elemSize = (int) properties.get("elementsize");
    } catch (Exception ex) {
      elemSize = DEFAULT_SIZE;
    }
  }

  @Override
  public byte[] encode(byte[] dataIn) {
    if (dataIn.length % elemSize != 0 || elemSize <= 1) {
      return dataIn;
    }

    int nElems = dataIn.length / elemSize;
    int[] start = new int[elemSize];
    for (int k = 0; k < elemSize; k++) {
      start[k] = k * nElems;
    }

    byte[] result = new byte[dataIn.length];
    for (int i = 0; i < nElems; i++) {
      for (int j = 0; j < elemSize; j++) {
        result[i + start[j]] = dataIn[(i * elemSize) + j];
      }
    }

    return result;
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    if (dataIn.length % elemSize != 0 || elemSize <= 1) {
      return dataIn;
    }

    int nElems = dataIn.length / elemSize;
    int[] start = new int[elemSize];
    for (int k = 0; k < elemSize; k++) {
      start[k] = k * nElems;
    }

    byte[] result = new byte[dataIn.length];
    for (int i = 0; i < nElems; i++) {
      for (int j = 0; j < elemSize; j++) {
        result[(i * elemSize) + j] = dataIn[i + start[j]];
      }
    }

    return result;
  }

  public static class Provider implements FilterProvider {

    private static final String name = "shuffle";

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
      return new Shuffle(properties);
    }
  }
}
