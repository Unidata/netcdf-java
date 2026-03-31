/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;

/**
 * A DirectoryStream of MFile objects.
 */
public class MFileDirectoryStream implements DirectoryStream<MFile> {
  private final Iterator<MFile> iterator;

  public MFileDirectoryStream(Iterator<MFile> iterator) {
    this.iterator = iterator;
  }

  @Override
  public Iterator<MFile> iterator() {
    return iterator;
  }

  @Override
  public void close() throws IOException {
    // If the iterator itself is closeable, close it.
    if (iterator instanceof AutoCloseable) {
      try {
        ((AutoCloseable) iterator).close();
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }
}
