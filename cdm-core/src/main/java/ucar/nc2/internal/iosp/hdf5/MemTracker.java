/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/** Track use of space in an HDF5 file */
public class MemTracker {
  private final List<Mem> memList = new ArrayList<>();
  private final long fileSize;

  public MemTracker(long fileSize) {
    this.fileSize = fileSize;
  }

  public void add(String name, long start, long end) {
    memList.add(new Mem(name, start, end));
  }

  public void addByLen(String name, long start, long size) {
    memList.add(new Mem(name, start, start + size));
  }

  public void report(Formatter f) {
    f.format("Memory used file size= %d%n", fileSize);
    f.format("  start    end   size   name");
    Collections.sort(memList);
    Mem prev = null;
    for (Mem m : memList) {
      if ((prev != null) && (m.start > prev.end))
        f.format(" + %6d %6d %6d %6s%n", prev.end, m.start, m.start - prev.end, "HOLE");
      char c = ((prev != null) && (prev.end != m.start)) ? '*' : ' ';
      f.format(" %s %6d %6d %6d %6s%n", c, m.start, m.end, m.end - m.start, m.name);
      prev = m;
    }
    f.format("%n");
  }

  static class Mem implements Comparable<Mem> {
    public final String name;
    public final long start, end;

    Mem(String name, long start, long end) {
      this.name = name;
      this.start = start;
      this.end = end;
    }

    public int compareTo(Mem m) {
      return Long.compare(start, m.start);
    }

  }
}
