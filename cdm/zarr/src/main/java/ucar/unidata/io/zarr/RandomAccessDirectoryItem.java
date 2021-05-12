package ucar.unidata.io.zarr;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.List;

public interface RandomAccessDirectoryItem {

  String getLocation();

  long length();

  long getLastModified();

  RandomAccessFile getRaf();

  void setRaf(RandomAccessFile raf);
}
