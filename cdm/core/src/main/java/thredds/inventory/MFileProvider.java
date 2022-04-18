/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import javax.annotation.Nullable;

/**
 * A Service Provider of {@link thredds.inventory.MFile}.
 */
public interface MFileProvider {

  /** The leading protocol string (without a trailing ":"). */
  String getProtocol();

  /** Determine if this Provider can provide an MFile for a given location. */
  default boolean canProvide(String location) {
    return location.startsWith(getProtocol() + ":");
  }

  /**
   * Create an {@link thredds.inventory.MFile} from a given location if it exists and is readable, otherwise return
   * null.
   *
   * @param location location of a file or .
   * @return {@link thredds.inventory.MFile}
   */
  @Nullable
  MFile create(String location) throws IOException;
}
