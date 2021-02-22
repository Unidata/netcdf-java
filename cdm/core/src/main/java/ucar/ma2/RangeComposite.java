/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * A Composite of other RangeIterators. Used for Lon Axis crossing the seam.
 * Iterate over list in sequence.
 * 
 * @deprecated will move in ver7.
 */
@Immutable
public class RangeComposite implements RangeIterator {
  private final ImmutableList<RangeIterator> ranges;
  private final String name;

  public RangeComposite(String name, List<RangeIterator> ranges) {
    this.name = name;
    this.ranges = ImmutableList.copyOf(ranges);
  }

  @Override
  public String getName() {
    return name;
  }

  public ImmutableList<RangeIterator> getRanges() {
    return ranges;
  }

  @Override
  public RangeIterator copyWithName(String name) {
    if (name.equals(this.getName()))
      return this;
    return new RangeComposite(name, ranges);
  }

  @Override
  public RangeIterator copyWithStride(int stride) throws InvalidRangeException {
    ArrayList<RangeIterator> list = new ArrayList<>();
    for (RangeIterator range : ranges) {
      list.add(range.copyWithStride(stride));
    }
    return new RangeComposite(name, list);
  }

  @Override
  public java.util.Iterator<Integer> iterator() {
    Collection<Iterable<Integer>> iters = new ArrayList<>(ranges);
    return new CompositeIterator<>(iters);
  }

  @Override
  public int length() {
    int result = 0;
    for (RangeIterator r : ranges)
      result += r.length();
    return result;
  }

  // generic could be moved to utils
  private static class CompositeIterator<T> implements Iterator<T> {
    Iterator<Iterable<T>> iters;
    Iterator<T> current;

    CompositeIterator(Collection<Iterable<T>> iters) {
      this.iters = iters.iterator();
      current = this.iters.next().iterator();
    }

    @Override
    public boolean hasNext() {
      if (current.hasNext())
        return true;
      if (!iters.hasNext())
        return false;
      current = iters.next().iterator();
      return hasNext();
    }

    @Override
    public T next() {
      return current.next();
    }
  }

}
