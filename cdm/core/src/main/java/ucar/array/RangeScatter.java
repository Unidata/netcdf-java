package ucar.array;

import com.google.common.collect.ImmutableSortedSet;

import javax.annotation.concurrent.Immutable;
import java.util.Iterator;
import java.util.Set;

/** A Set of indices described by a Set of element values. */
@Immutable
class RangeScatter implements RangeIterator {

  public static RangeIterator intersect(Range one, Range other) {
    ImmutableSortedSet.Builder<Integer> vals = ImmutableSortedSet.naturalOrder();
    Range small, big;
    if (other.length() < one.length()) {
      small = other;
      big = one;
    } else {
      small = one;
      big = other;
    }
    for (int elem : small) {
      if (big.contains(elem)) {
        vals.add(elem);
      }
    }
    return new RangeScatter(String.format("Intersect%sAnd%s", one.name(), other.name()), vals.build());
  }

  /////////////////////////////////////////////////////////////////////////////
  private final ImmutableSortedSet<Integer> vals;
  private final String name;

  public RangeScatter(String name, Set<Integer> vals) {
    this.name = name;
    this.vals = ImmutableSortedSet.copyOf(vals);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public RangeScatter copyWithName(String name) {
    if (name.equals(this.name()))
      return this;
    return new RangeScatter(name, vals);
  }

  @Override
  public int length() {
    return vals.size();
  }

  @Override
  public Iterator<Integer> iterator() {
    return vals.iterator();
  }
}
