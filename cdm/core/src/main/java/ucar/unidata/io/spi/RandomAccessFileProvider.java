/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.io.spi;

import java.io.IOException;
import ucar.unidata.io.RandomAccessFile;

/**
 * A Service Provider of RandomAccessFile.
 */
public interface RandomAccessFileProvider {

  /**
   * Determine if this Provider owns this location.
   */
  boolean isOwnerOf(String location);

  /**
   * Open a location that this Provider is the owner of.
   */
  RandomAccessFile open(String location) throws IOException;
}
