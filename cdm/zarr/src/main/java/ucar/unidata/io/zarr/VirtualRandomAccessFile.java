package ucar.unidata.io.zarr;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public class VirtualRandomAccessFile implements RandomAccessDirectoryItem {
  private String location;
  private long length;
  private long lastModified;
  private RandomAccessFile raf;

  public VirtualRandomAccessFile(String location, long length, long lastModified) {
    this.location = location;
    this.length = length;
    this.lastModified = lastModified;
    this.raf = null;
  }

  public String getLocation() {
    return this.location;
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

  public void setRaf(RandomAccessFile raf) {
    this.raf = raf;
  }
}
