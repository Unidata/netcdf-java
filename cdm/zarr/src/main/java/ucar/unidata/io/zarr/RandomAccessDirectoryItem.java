package ucar.unidata.io.zarr;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

public interface RandomAccessDirectoryItem {

  String getLocation();

  long length() throws IOException;

  long getLastModified();

  RandomAccessFile getRaf();

  void setRaf(RandomAccessFile raf);
}
