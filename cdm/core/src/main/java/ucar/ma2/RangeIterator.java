/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.util.Iterator;

/**
 * Generalization of Range (which is restricted to (start:stop:stride).
 * RangeIterator is over an arbitrary set of integers from the set {0..fullSize-1}.
 * 
 * @deprecated will move in ver7.
 */
public interface RangeIterator extends Iterable<Integer> {

  @Override
  Iterator<Integer> iterator();

  /** The number of index in this iterator. */
  int length();

  /** The name of this Range iterator. */
  String getName();

  /** Make a copy with a different name. */
  RangeIterator copyWithName(String name);

  /** Make a copy with a different name. */
  RangeIterator copyWithStride(int stride) throws InvalidRangeException;
}
