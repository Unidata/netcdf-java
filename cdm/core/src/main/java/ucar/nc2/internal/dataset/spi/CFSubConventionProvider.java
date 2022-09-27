/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.spi;

import java.util.List;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;

/**
 * A special SPI to register CF sub-convention or incubating convention CoordSystemBuilderFactory classes.
 *
 * FOR INTERNAL USE ONLY
 *
 * This interface is the same as CoordSystemBuilderFactory, except these are dynamically loaded and placed at the top of
 * the list of conventions, as opposed to being appended to the bottom of the list of conventions. The reason we must
 * do this is because the current CF CoordSystemBuilderFactory looks for a global attribute named "Convention" that
 * starts with the string "CF-1.". That means we'd either need to handle all sub-conventions or incubating conventions
 * within the main CF CoordSystemBuilder, or somehow make sure these conventions are loaded before the main CF
 * CoordSystemBuilder (which is what this interface is intended to facilitate).
 */
public interface CFSubConventionProvider extends CoordSystemBuilderFactory {

  // check a list of convention names to see if the CFSubConventionProvider is a provider
  default boolean isMine(List<String> convList) {
    return false;
  }
}
