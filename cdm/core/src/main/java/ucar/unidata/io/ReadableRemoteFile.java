/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io;

import java.io.Closeable;
import java.io.IOException;

/** A remote file that can be read from. */
public interface ReadableRemoteFile extends Closeable {

  /**
   * Read directly from the remote service. For HTTP based access, this is where "Accept-Ranges" HTTP requests are
   * called to do random access
   *
   * @param pos start here in the file
   * @param buff put data into this buffer
   * @param offset buffer offset
   * @param len this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  int readRemote(long pos, byte[] buff, int offset, int len) throws IOException;

  /**
   * Close any resources used to enable remote reading.
   *
   * For example, HTTPSession from httpservices, or S3Client from the AWS SDK
   */
  void closeRemote();
}
