/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

/** Storage that can be changed. */
public interface StorageMutable<T> extends Storage<T> {
  /** Set the ith element. */
  void setPrimitiveArray(int index, Object value);

  /** Get the ith element. */
  Object getPrimitiveArray(int index);
}
