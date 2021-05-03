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
  private List<RandomAccessDirectoryItem> children;

  public VirtualRandomAccessFile(String location, List<RandomAccessDirectoryItem> children) {
    this.location = location;
    this.raf = null;
    this.children = children;
    this.length = calcLength(children);
    this.lastModified = calcLastModified(children);
  }

  public VirtualRandomAccessFile(String location, long length, long lastModified) {
    this.location = location;
    this.length = length;
    this.lastModified = lastModified;
    this.raf = null;
    this.children = null;
  }

  public String getLocation() {
    return this.location;
  }

  public boolean isDirectory() { return this.children != null; }

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

  public List<RandomAccessDirectoryItem> getChildren() { return this.children; }

  public RandomAccessFile getRaf() {
    return this.raf;
  }

  public void setRaf(RandomAccessFile raf) {
    this.raf = raf;
  }

  private static long calcLength(List<RandomAccessDirectoryItem> children) {
    return children.stream().mapToLong(RandomAccessDirectoryItem::length).sum();
  }

  private static long calcLastModified(List<RandomAccessDirectoryItem> children) {
    return children.stream().mapToLong(RandomAccessDirectoryItem::getLastModified).max().orElse(-1);
  }
}
