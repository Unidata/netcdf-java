package ucar.unidata.io.zarr;

import ucar.unidata.io.RandomAccessFile;

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

  public String getLocation() { return this.location; }

  public long length() { return this.length; }

  public long getLastModified() { return this.lastModified; }

  public RandomAccessFile getRaf() { return this.raf; }

  public void setRaf (RandomAccessFile raf) { this.raf = raf; }
}
