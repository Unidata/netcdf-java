/*
 * Copyright (c) 2021-2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import ucar.nc2.util.NcServiceLoader;

public class Filters {

  private static final ImmutableList<FilterProvider> spFilters;

  // load filter service providers
  static {
    ImmutableList.Builder<FilterProvider> spFiltersBuilder = ImmutableList.builder();
    for (FilterProvider fp : NcServiceLoader.load(FilterProvider.class)) {
      spFiltersBuilder.add(fp);
    }
    spFilters = spFiltersBuilder.build();
  }

  /**
   * Set of common properties used by Filters
   */
  public class Keys {
    /**
     * key for a String id/name (e.g. Zarr filters)
     */
    public static final String NAME = "id";
    /**
     * key for a numeric id (e.g. HDF5 filters)
     */
    public static final String ID = "nid";
    /**
     * key for property indicating whether filter is optional,
     * i.e. whether it should continue or throw if it fails
     */
    public static final String OPTIONAL = "optional";
    /**
     * key mapping to client data used by filter
     */
    public static final String DATA = "data";
    /**
     * key mapping to element size
     */
    public static final String ELEM_SIZE = "elementsize";
  }

  private static NullFilter nullFilter = new NullFilter();

  /**
   * Create a filter object matching the provided id
   * 
   * @param properties
   * @return A filter object with the given properties
   * @throws UnknownFilterException
   */
  public static Filter getFilter(Map<String, Object> properties) throws UnknownFilterException {
    if (properties == null) {
      return nullFilter;
    }
    // read id properties
    String name = (String) properties.get(Keys.NAME);
    Object oid = properties.get(Keys.ID);
    // if no id or name, return null filter
    final boolean missingName = name == null || name.isEmpty();
    if (missingName && !(oid instanceof Number)) {
      return nullFilter;
    }

    // try by name first
    if (!missingName) {
      for (FilterProvider fp : spFilters) {
        if (fp.canProvide(name)) {
          return fp.create(properties);
        }
      }
    }

    // try by id next
    int id = ((Short) oid).intValue();
    for (FilterProvider fp : spFilters) {
      if (fp.canProvide(id)) {
        return fp.create(properties);
      }
    }
    // final fallback
    throw new UnknownFilterException(id);
  }

  /**
   * A filter which passes data through unchanged
   */
  private static class NullFilter extends Filter {

    private static final String name = "null";

    private static final int id = 0;

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
      return dataIn;
    }

    @Override
    public byte[] decode(byte[] dataIn) {
      return dataIn;
    }
  }
}
