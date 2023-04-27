/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nullable;

/**
 * An abstraction for java.io.File / java.nio.file.Path
 *
 * @author caron
 * @since Jun 30, 2009
 */
public interface MFile extends Comparable<MFile> {

  /**
   * Get time of last modification at the time the MFile object was created
   * 
   * @return time of last modification in Unix time (msecs since reference), or -1 if unknown
   */
  long getLastModified();

  /**
   * Size of file in bytes
   * 
   * @return Size of file in bytes or -1 if unknown
   */
  long getLength();

  boolean isDirectory();

  default boolean isReadable() {
    return true;
  }

  /**
   * Get full path name, replace \\ with /
   * 
   * @return full path name
   */
  String getPath();

  /**
   * The name is the <em>farthest</em> element from the root in the directory hierarchy.
   * 
   * @return the file name
   */
  String getName();

  /**
   * Get the parent of this
   * 
   * @return the parent or null
   */
  MFile getParent() throws IOException;

  int compareTo(MFile o);

  // does not survive serialization ??
  Object getAuxInfo();

  void setAuxInfo(Object info);

  /**
   * Check if the MFile exists
   *
   * @return true if the MFile exists, else false
   */
  boolean exists();

  /**
   * Get the MFile InputStream
   *
   * @return the MFile InputStream
   */
  InputStream getInputStream() throws FileNotFoundException;

  /**
   * Write the MFile to an OutputStream
   *
   * @param outputStream the OutputStream the MFile contents should be written to
   */
  void writeToStream(OutputStream outputStream) throws IOException;

  /**
   * Write the MFile to an OutputStream
   *
   * @param outputStream the OutputStream the MFile contents should be written to
   * @param offset the index of the first byte to write out
   * @param maxBytes the maximum number of bytes to copy
   */
  void writeToStream(OutputStream outputStream, long offset, long maxBytes) throws IOException;

  /**
   * Get child MFile of this MFile
   *
   * @param newFileName the relative file path of the new MFile
   * @return the new MFile or null if the file can't be resolved
   */
  @Nullable
  MFile getChild(String newFileName);
}
