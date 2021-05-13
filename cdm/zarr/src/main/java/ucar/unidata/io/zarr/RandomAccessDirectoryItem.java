/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.zarr;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.List;

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
   * @return size of directory item
   */
  long length();

  /**
   * @return last modified time (in ms) of directory item
   */
  long getLastModified();

  /**
   * @return RandomAccessFile for directory item
   */
  RandomAccessFile getRaf();

  /**
   * Set RandomAccessFile for directory item
   * 
   * @param raf
   */
  void setRaf(RandomAccessFile raf);
}
