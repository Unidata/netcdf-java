package ucar.unidata.io.zarr;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.List;

public interface RandomAccessDirectoryItem {

  String getLocation();

  boolean isDirectory();

  long length();

  long getLastModified();

  List<RandomAccessDirectoryItem> getChildren();

  RandomAccessFile getRaf();

  void setRaf(RandomAccessFile raf);
}
