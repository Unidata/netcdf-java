package ucar.ma2;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A Range of indices describes by a list, rather than start:stop:stride.
 * 
 * @deprecated will move in ver7. TODO used?
 */
@Deprecated
@Immutable
public class RangeScatter implements RangeIterator {
  private final int[] vals;
  private final String name;

  /**
   * Ctor
   * 
   * @param name optional name
   * @param val should be sorted
   */
  public RangeScatter(String name, int... val) {
    // super(name, val[0], val[val.length-1], 1, val.length);
    this.name = name;
    this.vals = val;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public RangeIterator copyWithName(String name) {
    if (name.equals(this.getName()))
      return this;
    return new RangeScatter(name, vals);
  }

  /** Make a copy with a different name. */
  @Override
  public RangeIterator copyWithStride(int stride) throws InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int length() {
    return vals.length;
  }

  @Override
  public String toString() {
    return "{" + Arrays.toString(vals) + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    RangeScatter that = (RangeScatter) o;
    return Arrays.equals(vals, that.vals);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(vals);
  }

  @Override
  public Iterator<Integer> iterator() {
    return new ScatterIterator();
  }

  private class ScatterIterator implements Iterator<Integer> {
    private int current;

    @Override
    public boolean hasNext() {
      return current < vals.length;
    }

    @Override
    public Integer next() {
      return vals[current++];
    }
  }
}
