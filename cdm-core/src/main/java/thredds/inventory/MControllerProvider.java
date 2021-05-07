/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

/**
 * A Service Provider of {@link MController}.
 *
 * @since 5.4
 */
public interface MControllerProvider {
  /** The leading protocol string (without a trailing ":"). */
  String getProtocol();

  /** Determine if this Controller can scan for a collection at this location. */
  default boolean canScan(String location) {
    return location.startsWith(getProtocol() + ":");
  }

  /**
   * Creates an instance of
   * 
   * @return An {@link MController} that scans locations to filter and provide a set of {@link MFile}s defining to
   *         be used to define a collection.
   */
  MController create();
}
