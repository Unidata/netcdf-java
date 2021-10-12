/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import java.util.Map;

/**
 * A Service Provider of {@link ucar.nc2.filter.Filter}.
 */
public interface FilterProvider {

  String getName();

  int getId();

  /** Determine if this Provider can provide an Filter with the given name */
  default boolean canProvide(String name) {
    return name.equals(getName());
  }

  /** Determine if this Provider can provide an Filter with the given numeric ID */
  default boolean canProvide(int id) {
    return id == getId();
  }


  /**
   * Create a {@link ucar.nc2.filter.Filter} of the correct type
   * 
   * @return {@link ucar.nc2.filter.Filter}
   */
  Filter create(Map<String, Object> properties);
}
