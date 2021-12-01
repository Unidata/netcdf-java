/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import java.util.Iterator;
import javax.annotation.Nullable;
import ucar.array.StructureData;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/** Abstract base class for IOSP implementations. */
public abstract class AbstractIOServiceProvider implements IOServiceProvider {
  protected ucar.unidata.io.RandomAccessFile raf;
  protected String location;
  protected int rafOrder = RandomAccessFile.BIG_ENDIAN;
  protected NetcdfFile ncfile;

  public void setRaf(RandomAccessFile raf) {
    this.raf = raf;
    this.location = raf.getLocation();
  }

  @Override
  public void buildFinish(NetcdfFile ncfile) {
    this.ncfile = ncfile;
  }

  @Override
  public void close() throws java.io.IOException {
    if (raf != null)
      raf.close();
    raf = null;
  }

  // release any resources like file handles
  @Override
  public void release() throws IOException {
    if (raf != null)
      raf.close();
    raf = null;
  }

  // reacquire any resources like file handles
  @Override
  public void reacquire() throws IOException {
    raf = RandomAccessFile.acquire(location);
    this.raf.order(rafOrder);
  }

  @Override
  public Iterator<StructureData> getSequenceIterator(Sequence s, int bufferSize) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public Object sendIospMessage(@Nullable Object message) {
    if (message == NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE) {
      return raf;
    }
    return null;
  }

  /**
   * Returns the time that the underlying file(s) were last modified. If they've changed since they were stored in the
   * cache, they will be closed and reopened with {@link ucar.nc2.internal.cache.FileFactory}.
   *
   * @return a {@code long} value representing the time the file(s) were last modified or {@code 0L} if the
   *         last-modified time couldn't be determined for any reason.
   */
  @Override
  public long getLastModified() {
    if (location != null) {
      File file = new File(location);
      return file.lastModified();
    } else {
      return 0;
    }
  }

  @Override
  public String toStringDebug(Object o) {
    return "";
  }

  @Override
  public String getDetailInfo() {
    if (raf == null)
      return "";
    try {
      Formatter fout = new Formatter();
      double size = raf.length() / (1000.0 * 1000.0);
      fout.format(" raf = %s%n", raf.getLocation());
      fout.format(" size= %d (%s Mb)%n%n", raf.length(), Format.dfrac(size, 3));
      return fout.toString();

    } catch (IOException e) {
      return e.getMessage();
    }
  }

  @Override
  public String getFileTypeVersion() {
    return "N/A";
  }

}
