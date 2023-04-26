/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * A Service Provider of {@link thredds.inventory.MFile}.
 */
public interface MFileProvider {

  /** The leading protocol string (without a trailing ":"). */
  String getProtocol();

  /** Determine if this Provider can provide an MFile for a given location. */
  default boolean canProvide(String location) {
    return location != null && location.startsWith(getProtocol() + ":");
  }

  /**
   * Create an {@link thredds.inventory.MFile} from a given location, the file may or may not exist
   *
   * @param location location of a file or .
   * @return {@link thredds.inventory.MFile}
   */
  @Nonnull
  MFile create(String location) throws IOException;
}
