/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.io.spi;

import java.io.IOException;
import ucar.unidata.io.RandomAccessFile;

/** A Service Provider of RandomAccessFile. */
public interface RandomAccessFileProvider {

  /** Determine if this Provider owns this location. */
  boolean isOwnerOf(String location);

  /** Open a location that this Provider is the owner of. */
  RandomAccessFile open(String location) throws IOException;

  /** Open a location that this Provider is the owner of, with the given buffer size */
  default RandomAccessFile open(String location, int bufferSize) throws IOException {
    return this.open(location); // avoid breaking an existing 3rd party implementations
  }

  /** Acquire a file for a location from a cache, if available **/
  default RandomAccessFile acquire(String location) throws IOException {
    return this.open(location); // avoid breaking an existing 3rd party implementations
  }

  /** Acquire a file for a location, with the given buffer size, from a cache, if available **/
  default RandomAccessFile acquire(String location, int bufferSize) throws IOException {
    return this.open(location, bufferSize); // avoid breaking an existing 3rd party implementations
  }
}
