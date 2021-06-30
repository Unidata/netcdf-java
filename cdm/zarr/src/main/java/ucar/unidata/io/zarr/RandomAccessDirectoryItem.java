/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.zarr;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Interface for items held by a RandomAccessDirectory
 * Provides a wrapper for a RandomAccessFile
 */
public interface RandomAccessDirectoryItem {

  /**
   * @return location of directory item
   */
  String getLocation();

  /**
   * @return start index of item relative to directory
   */
  long startIndex();

  /**
   * @return size of directory item
   */
  long length();

  /**
   * @return last modified time (in ms) of directory item
   */
  long getLastModified();

  /**
   * @return RandomAccessFile for directory item or null if unopened
   */
  RandomAccessFile getRaf();

  /**
   * @return RandomAccessFile for directory item
   */
  RandomAccessFile getOrOpenRaf() throws IOException;
}
