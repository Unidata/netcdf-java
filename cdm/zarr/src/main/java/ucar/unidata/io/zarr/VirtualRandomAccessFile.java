/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.zarr;

import ucar.nc2.NetcdfFiles;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * A wrapper for a RandomAccessFile that allows lazy loading
 */
public class VirtualRandomAccessFile implements RandomAccessDirectoryItem {
  private final String location;
  private long startIndex;
  private long length;
  private long lastModified;
  private RandomAccessFile raf;
  private final int bufferSize;

  public VirtualRandomAccessFile(String location, long startIndex, long length, long lastModified, int bufferSize) {
    this.location = location;
    this.startIndex = startIndex;
    this.length = length;
    this.lastModified = lastModified;
    this.raf = null;
    this.bufferSize = bufferSize;
  }

  public String getLocation() {
    return this.location;
  }

  public long startIndex() {
    return this.startIndex;
  }

  public long length() {
    try {
      return this.raf == null ? this.length : raf.length();
    } catch (IOException ioe) {
      return this.length;
    }
  }

  public long getLastModified() {
    return this.raf == null ? this.lastModified : this.raf.getLastModified();
  }

  public RandomAccessFile getRaf() {
    return this.raf;
  }

  public RandomAccessFile getOrOpenRaf() throws IOException {
    if (this.raf == null) {
      this.raf = NetcdfFiles.getRaf(this.location, this.bufferSize);
    }
    return this.raf;
  }
}
