/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import java.util.Iterator;

/**
 * Generalization of Range (which is restricted to (start:stop:stride).
 * RangeIterator is over an arbitrary set of integers from the set {0..fullSize-1}.
 */
public interface RangeIterator extends Iterable<Integer> {

  @Override
  Iterator<Integer> iterator();

  /** The number of elements in this iterator. */
  int length();

  /** The name of this Range iterator. */
  String name();

  /** Make a copy with a different name. */
  RangeIterator copyWithName(String name);
}
