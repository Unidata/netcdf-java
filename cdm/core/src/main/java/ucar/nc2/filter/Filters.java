/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.util.Map;
import java.util.ServiceLoader;

public class Filters {

  private static NullFilter nullFilter = new NullFilter();

  /**
   * Create a filter object for the given property list
   * 
   * @param properties
   * @return A filter object with the provided properties
   * @throws UnknownFilterException
   */
  public static Filter getFilterByName(Map<String, Object> properties) throws UnknownFilterException {
    if (properties == null) {
      return nullFilter;
    }
    String name = (String) properties.get("id");
    if (name == null || name.isEmpty()) {
      return nullFilter;
    }

    // look for dynamically loaded filters
    for (FilterProvider fp : ServiceLoader.load(FilterProvider.class)) {
      if (fp.canProvide(name)) {
        return fp.create(properties);
      }
    }
    throw new UnknownFilterException(name);
  }

  /**
   * A filter which passes data through unchanged
   */
  private static class NullFilter extends Filter {

    @Override
    public byte[] encode(byte[] dataIn) {
      return dataIn;
    }

    @Override
    public byte[] decode(byte[] dataIn) {
      return dataIn;
    }
  }
}
