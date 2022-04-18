/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.util.Map;


// TODO: Still to be implemented
public class Blosc extends Filter {

  public Blosc(Map<String, Object> properties) {}

  @Override
  public byte[] encode(byte[] dataIn) {
    return new byte[0];
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    return new byte[0];
  }

  public static class Provider implements FilterProvider {

    private static final String name = "blosc";

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
      return new Blosc(properties);
    }
  }
}
