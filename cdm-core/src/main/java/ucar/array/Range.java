/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Iterator;
import java.util.Objects;

/**
 * A strided subset of the interval of positive integers [first(), last()] inclusive.
 * For example Range(1:11:3) represents the set of integers {1,4,7,10}
 * <p>
 * Elements must be nonnegative and unique.
 * EMPTY is the empty Range.
 * SCALAR is the set {0}.
 * VLEN is for variable length dimensions.
 * <p>
 * Standard iteration is
 * 
 * <pre>
 *  for (int idx : range) {
 *    ...
 *  }
 * </pre>
 */
@Immutable
public class Range implements RangeIterator {
  public static final Range EMPTY = new Range(); // used for unlimited dimension = 0
  public static final Range SCALAR = new Range("SCALAR", 1);
  public static final Range VLEN = new Range("VLEN", -1);

  /** Make a named Range from 0 to len-1. RuntimeException on error. */
  public static Range make(String name, int len) {
    try {
      return new Range(name, 0, len - 1, 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen if len > 0
    }
  }

  /** Make an unnamed Range from first to last. RuntimeException on error. */
  public static Range make(int first, int last) {
    try {
      return new Range(first, last);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen if last >= first
    }
  }

  /** Make an unnamed Range from first to last with stride. RuntimeException on error. */
  public static Range make(int first, int last, int stride) {
    try {
      return new Range(first, last, stride);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen if last >= first
    }
  }

  ////////////////////////////////////////////////////////

  private final int length; // number of elements
  private final int first; // first value in range
  private final int last; // last value in range, inclusive
  private final int stride; // stride, must be >= 1
  private final String name; // optional name

  /** Used for EMPTY */
  private Range() {
    this.length = 0;
    this.first = 0;
    this.last = -1;
    this.stride = 1;
    this.name = "EMPTY";
  }

  /** Used for ONE, VLEN */
  private Range(String name, int length) {
    assert (length != 0);
    this.name = name;
    this.first = 0;
    this.last = length - 1;
    this.stride = 1;
    this.length = length;
  }

  /**
   * Create a range with unit stride.
   *
   * @param first first value in range
   * @param last last value in range, inclusive
   * @throws InvalidRangeException elements must be nonnegative, 0 &le; first &le; last
   */
  public Range(int first, int last) throws InvalidRangeException {
    this(null, first, last, 1);
  }

  /**
   * Create a range starting at zero, with unit stride.
   *
   * @param length number of elements in the Range
   */
  public Range(int length) {
    assert (length != 0);
    this.name = null;
    this.first = 0;
    this.last = length - 1;
    this.stride = 1;
    this.length = length;
  }

  /**
   * Create a named range with unit stride.
   *
   * @param name name of Range
   * @param first first value in range
   * @param last last value in range, inclusive
   * @throws InvalidRangeException elements must be nonnegative, 0 &le; first &le; last
   */
  public Range(String name, int first, int last) throws InvalidRangeException {
    this(name, first, last, 1);
  }

  /**
   * Create a range with a specified first, last, stride.
   *
   * @param first first value in range
   * @param last last value in range, inclusive
   * @param stride stride between consecutive elements, must be &gt; 0
   * @throws InvalidRangeException elements must be nonnegative: 0 &le; first &le; last, stride &gt; 0
   */
  public Range(int first, int last, int stride) throws InvalidRangeException {
    this(null, first, last, stride);
  }

  /**
   * Create a named range with a specified name, first, last, stride.
   *
   * @param name name of Range
   * @param first first value in range
   * @param last last value in range, inclusive
   * @param stride stride between consecutive elements, must be &gt; 0
   * @throws InvalidRangeException elements must be nonnegative: 0 &le; first &le; last, stride &gt; 0
   */
  public Range(String name, int first, int last, int stride) throws InvalidRangeException {
    if (first < 0)
      throw new InvalidRangeException("first (" + first + ") must be >= 0");
    if (last < first)
      throw new InvalidRangeException("last (" + last + ") must be >= first (" + first + ")");
    if (stride < 1)
      throw new InvalidRangeException("stride (" + stride + ") must be > 0");

    this.name = name;
    this.first = first;
    this.stride = stride;
    this.length = 1 + (last - first) / stride;
    this.last = first + (this.length - 1) * stride;
    assert stride != 1 || this.last == last;
  }

  private Range(String name, int first, int last, int stride, int length) throws InvalidRangeException {
    if (first < 0)
      throw new InvalidRangeException("first (" + first + ") must be >= 0");
    if (last < first)
      throw new InvalidRangeException("last (" + last + ") must be >= first (" + first + ")");
    if (stride < 1)
      throw new InvalidRangeException("stride (" + stride + ") must be > 0");
    if (length < (1 + last - first) / stride)
      throw new InvalidRangeException("length (" + length + ") must be > (1 + last - first) / stride");

    this.name = name;
    this.first = first;
    this.last = last;
    this.stride = stride;
    this.length = length;
  }

  /** Make a copy with a different stride. */
  public Range copyWithStride(int stride) {
    if (stride == this.stride) {
      return this;
    }
    return Range.make(this.first(), this.last(), stride);
  }

  /** Make a copy with a different name. */
  @Override
  public Range copyWithName(String name) {
    if (name.equals(this.name())) {
      return this;
    }
    try {
      return new Range(name, first, last, stride, length);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // can happen if you call this on EMPTY or VLEN
    }
  }

  /** @return name, or null if none */
  @Nullable
  @Override
  public String name() {
    return name;
  }

  /** @return first value in range */
  public int first() {
    return first;
  }

  /** @return last value in range, inclusive */
  public int last() {
    return last;
  }

  /** @return the number of elements in the range. */
  @Override
  public int length() {
    return length;
  }

  /** @return stride, must be &ge; 1. */
  public int stride() {
    return stride;
  }

  /////////////////////////////////////////////

  /**
   * Is want contained in this Range?
   *
   * @param want index in the original Range
   * @return true if the ith element would be returned by the Range iterator
   */
  public boolean contains(int want) {
    if (want < first())
      return false;
    if (want > last())
      return false;
    if (stride == 1)
      return true;
    return (want - first) % stride == 0;
  }

  /**
   * Create a new Range by composing a Range that is reletive to this Range.
   * 
   * @param r range elements are reletive to this
   * @return combined Range, may be EMPTY
   * @throws InvalidRangeException elements must be nonnegative, 0 &le; first &le; last
   */
  public Range compose(Range r) throws InvalidRangeException {
    if ((length() == 0) || (r.length() == 0)) {
      return EMPTY;
    }
    if (this == VLEN || r == VLEN) {
      return VLEN;
    }
    /*
     * if(false) {// Original version
     * // Note that this version assumes that range r is
     * // correct with respect to this.
     * int first = element(r.first());
     * int stride = stride() * r.stride();
     * int last = element(r.last());
     * return new Range(name, first, last, stride);
     * } else {//new version: handles versions all values of r.
     */
    int sr_stride = this.stride * r.stride;
    int sr_first = element(r.first()); // MAP(this,i) == element(i)
    int lastx = element(r.last());
    int sr_last = Math.min(last(), lastx);
    return new Range(name, sr_first, sr_last, sr_stride);
  }

  /**
   * Create a new Range by compacting this Range by removing the stride.
   * first = first/stride, last=last/stride, stride=1.
   *
   * @return compacted Range
   * @throws InvalidRangeException elements must be nonnegative, 0 &le; first &le; last
   */
  public Range compact() throws InvalidRangeException {
    if (stride == 1)
      return this;
    int first = first() / stride; // LOOK WTF ?
    int last = first + length() - 1;
    return new Range(name, first, last, 1);
  }

  /**
   * Get ith element
   *
   * @param i index of the element
   * @return the i-th element of a range.
   * @throws InvalidRangeException i must be: 0 &le; i &lt; length
   */
  public int element(int i) throws InvalidRangeException {
    if (i < 0) {
      throw new InvalidRangeException("element idx (" + i + ") must be >= 0");
    }
    if (i >= length) {
      throw new InvalidRangeException("element idx (" + i + ") must be < length");
    }

    return first + i * stride;
  }

  /**
   * Given an element in the Range, find its index in the array of elements.
   *
   * @param want the element of the range
   * @return index
   * @throws InvalidRangeException if illegal elem
   */
  public int index(int want) throws InvalidRangeException {
    if (want < first)
      throw new InvalidRangeException("elem must be >= first");
    int result = (want - first) / stride;
    if (result > length)
      throw new InvalidRangeException("elem must be &le; first = n * stride");
    return result;
  }

  /**
   * Create a new Range by intersecting with another Range that must have stride 1.
   *
   * @param other range to intersect, must have stride 1.
   * @return intersected Range, may be EMPTY
   * @throws InvalidRangeException elements must be nonnegative
   */
  public Range intersect(Range other) throws InvalidRangeException {
    if ((length() == 0) || (other.length() == 0)) {
      return EMPTY;
    }
    if (this == VLEN || other == VLEN) {
      return VLEN;
    }
    Preconditions.checkArgument(other.stride == 1, "other stride must be 1");

    int first = Math.max(this.first(), other.first());
    int last = Math.min(this.last(), other.last());

    if (first > last) {
      return EMPTY;
    }

    if (first() < other.first()) {
      int incr = (other.first() - first()) / this.stride;
      first = first() + incr * this.stride;
      if (first < other.first()) {
        first += this.stride;
      }
    }

    return new Range(name, first, last, this.stride);
  }

  /**
   * Determine if a given Range interval intersects this one. Strides are ignored, only first() and last() matter.
   *
   * @param other range to intersect
   * @return true if their intervals intersect, ignoring stride.
   */
  public boolean intersects(Range other) {
    if ((length() == 0) || (other.length() == 0)) {
      return false;
    }
    if (this == VLEN || other == VLEN) {
      return true;
    }

    int first = Math.max(this.first(), other.first());
    int last = Math.min(this.last(), other.last());

    return (first <= last);
  }

  /**
   * Find the first element in a strided array after some index start.
   * Return the smallest element k in the Range, such that
   * <ul>
   * <li>k &ge; first
   * <li>k &ge; start
   * <li>k &le; last
   * <li>k = element of this Range
   * </ul>
   *
   * @param start starting index
   * @return first in interval, else -1 if there is no such element.
   */
  public int getFirstInInterval(int start) {
    if (start > last()) {
      return -1;
    }
    if (start <= first) {
      return first;
    }
    if (stride == 1) {
      return start;
    }
    int offset = start - first;
    int i = offset / stride;
    i = (offset % stride == 0) ? i : i + 1; // round up
    return first + i * stride;
  }

  @Override
  public String toString() {
    if (this.length == 0)
      return ":"; // EMPTY
    else if (this.length < 0)
      return ":"; // VLEN
    else
      return first + ":" + last() + (stride > 1 ? ":" + stride : "");
  }

  /** Does not include the name. */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Range integers = (Range) o;
    return length == integers.length && first == integers.first && last == integers.last && stride == integers.stride;
  }

  @Override
  public int hashCode() {
    return Objects.hash(length, first, last, stride);
  }

  /////////////////////////////////////////////////////////

  @Override
  public Iterator<Integer> iterator() {
    return new InternalIterator();
  }

  private class InternalIterator implements Iterator<Integer> {
    private int current;

    @Override
    public boolean hasNext() {
      return current < length;
    }

    @Override
    public Integer next() {
      return elementNC(current++);
    }
  }

  /** Get ith element; skip checking, for speed. */
  private int elementNC(int i) {
    return first + i * stride;
  }

}
