/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

/** Abstraction for storing Array data. */
public interface Storage<T> extends Iterable<T> {

  long getLength();

  T get(long elem);

  // Mimic of System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length);
  void arraycopy(int srcPos, Object dest, int destPos, long length);

}
