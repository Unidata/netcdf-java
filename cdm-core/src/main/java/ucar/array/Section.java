/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * A section of multidimensional array indices.
 * Represented as List<Range>.
 * <p/>
 * TODO evaluate use of null in ver7
 */
@Immutable
public class Section {
  public static final Section SCALAR = new Section(Range.SCALAR);

  /**
   * Return a Section guaranteed to be non null, with no null Ranges, and within the bounds set by shape.
   * A section with no nulls is called "filled".
   * If it is already filled, return it, otherwise return a new Section, filled from the shape.
   *
   * @param s the original Section, may be null or not filled
   * @param shape use this as default shape if any of the ranges are null.
   * @return a filled Section
   * @throws InvalidRangeException if shape and s and shape rank dont match, or if s has invalid range compared to shape
   */
  public static Section fill(Section s, int[] shape) throws InvalidRangeException {
    // want all
    if (s == null) {
      return new Section(shape);
    }
    // scalar
    if (shape.length == 0 && s.equals(SCALAR)) {
      return s;
    }

    String errs = s.checkInRange(shape);
    if (errs != null) {
      throw new InvalidRangeException(errs);
    }

    // if s is already filled, use it
    boolean ok = true;
    for (int i = 0; i < shape.length; i++) {
      ok &= (s.getRange(i) != null);
    }
    if (ok) {
      return s;
    }

    // fill in any nulls
    return new Section(s.getRanges(), shape);
  }

  /** Is this a scalar Section? Allows int[], int[1] {0}, int[1] {1} */
  public static boolean isScalar(int[] shape) {
    return (shape.length == 0) || (shape.length == 1 && shape[0] < 2);
  }

  //////////////////////////////////////////////////////////////////////////////
  // Cant use ImmutableList because that doesnt allow nulls.
  private final List<Range> ranges; // unmodifiableList

  /**
   * Create Section from a shape array, assumes 0 origin.
   *
   * @param shape array of lengths for each Range. 0 = EMPTY, &lt; 0 = VLEN
   */
  public Section(int[] shape) {
    ArrayList<Range> builder = new ArrayList<>();
    for (int aShape : shape) {
      if (aShape > 0)
        builder.add(new Range(aShape));
      else if (aShape == 0)
        builder.add(Range.EMPTY);
      else {
        builder.add(Range.VLEN);
      }
    }
    this.ranges = Collections.unmodifiableList(builder);
  }

  /**
   * Create Section from a shape and origin arrays.
   *
   * @param origin array of start for each Range
   * @param shape array of lengths for each Range
   * @throws InvalidRangeException if origin &lt; 0, or shape &lt; 1.
   */
  public Section(int[] origin, int[] shape) throws InvalidRangeException {
    Preconditions.checkArgument(isScalar(origin) == isScalar(shape) || origin.length == shape.length);
    ArrayList<Range> builder = new ArrayList<>();
    for (int i = 0; i < shape.length; i++) {
      if (shape[i] < 0) {
        builder.add(Range.VLEN);
      } else if (shape[i] == 0) {
        builder.add(Range.EMPTY);
      } else if (origin[i] == 0 && shape[i] == 1) {
        builder.add(Range.SCALAR);
      } else {
        builder.add(new Range(origin[i], origin[i] + shape[i] - 1));
      }
    }
    this.ranges = Collections.unmodifiableList(builder);
  }

  /** Create Section from a List<Range>. */
  public Section(List<Range> from) {
    this.ranges = Collections.unmodifiableList(new ArrayList<>(from));
  }

  /** Create Section from a variable length argument list of Ranges */
  public Section(Range... ranges) {
    this.ranges = Collections.unmodifiableList(java.util.Arrays.asList(ranges));
  }

  /**
   * Create Section from a List<Range>, filling in nulls with shape.
   *
   * @param from the list of Range
   * @param shape use this as default shape if any of the ranges are null.
   * @throws InvalidRangeException if shape and range list dont match
   */
  public Section(List<Range> from, int[] shape) throws InvalidRangeException {
    if (shape.length != from.size())
      throw new InvalidRangeException(" shape[] must have same rank as list of ranges");
    ArrayList<Range> builder = new ArrayList<>();

    // check that any individual Range is null
    for (int i = 0; i < shape.length; i++) {
      Range r = from.get(i);
      if (r == null) {
        if (shape[i] > 0)
          builder.add(new Range(shape[i]));
        else if (shape[i] == 0)
          builder.add(Range.EMPTY);
        else {
          builder.add(Range.VLEN);
        }
      } else {
        builder.add(r);
      }
    }
    this.ranges = Collections.unmodifiableList(builder);
  }

  /**
   * Parse an index section String specification, return equivilent Section.
   * A null Range means "all" (i.e.":") indices in that dimension.
   * <p/>
   * The sectionSpec string uses fortran90 array section syntax, namely:
   * 
   * <pre>
   *   sectionSpec := dims
   *   dims := dim | dim, dims
   *   dim := ':' | slice | start ':' end | start ':' end ':' stride
   *   slice := INTEGER
   *   start := INTEGER
   *   stride := INTEGER
   *   end := INTEGER
   *
   * where nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
   *
   * Meaning of index selector :
   *  ':' = all
   *  slice = hold index to that value
   *  start:end = all indices from start to end inclusive
   *  start:end:stride = all indices from start to end inclusive with given stride
   *
   * </pre>
   *
   * @param sectionSpec the token to parse, eg "(1:20,:,3,10:20:2)", parenthesis optional
   * @throws InvalidRangeException when the Range is illegal
   * @throws IllegalArgumentException when sectionSpec is misformed
   */
  public Section(String sectionSpec) throws InvalidRangeException {

    ArrayList<Range> builder = new ArrayList<>();
    Range range;

    StringTokenizer stoke = new StringTokenizer(sectionSpec, "(),"); // LOOK deal with scatterRange {1,2,3}
    while (stoke.hasMoreTokens()) {
      String s = stoke.nextToken().trim();
      if (s.equals(":")) {
        range = null; // all

      } else if (s.indexOf(':') < 0) { // just a number : slice
        try {
          int index = Integer.parseInt(s);
          range = new Range(index, index);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: " + s + " part of <" + sectionSpec + ">");
        }

      } else { // gotta be "start : end" or "start : end : stride"
        StringTokenizer stoke2 = new StringTokenizer(s, ":");
        String s1 = stoke2.nextToken();
        String s2 = stoke2.nextToken();
        String s3 = stoke2.hasMoreTokens() ? stoke2.nextToken() : null;
        try {
          int index1 = Integer.parseInt(s1);
          int index2 = Integer.parseInt(s2);
          int stride = (s3 != null) ? Integer.parseInt(s3) : 1;
          range = new Range(index1, index2, stride);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: " + s + " part of <" + sectionSpec + ">");
        }
      }
      builder.add(range);
    }
    this.ranges = Collections.unmodifiableList(builder);
  }

  /**
   * Create a new Section by compacting each Range.
   * first = first/stride, last=last/stride, stride=1.
   *
   * @return compacted Section
   * @throws InvalidRangeException elements must be nonnegative, 0 &le; first &le; last
   */
  public Section compact() throws InvalidRangeException {
    List<Range> results = new ArrayList<>(getRank());
    for (Range r : ranges) {
      results.add(r == null ? null : r.compact());
    }
    return new Section(results);
  }

  /**
   * Create a new Section by composing with a Section that is reletive to this Section.
   *
   * @param want Section reletive to this one. If null, return this. If individual ranges are null, use corresponding
   *        Range in this.
   * @return new Section, composed
   * @throws InvalidRangeException if want.getRank() not equal to this.getRank(), or invalid component Range
   */
  public Section compose(Section want) throws InvalidRangeException {
    // all nulls
    if (want == null) {
      return this;
    }

    if (want.getRank() != getRank()) {
      throw new InvalidRangeException("Invalid Section rank");
    }

    // check individual nulls
    List<Range> results = new ArrayList<>(getRank());
    for (int j = 0; j < ranges.size(); j++) {
      Range base = ranges.get(j);
      Range r = want.getRange(j);

      if (r == null) {
        results.add(base);
      } else {
        results.add(base.compose(r));
      }
    }

    return new Section(results);
  }

  /**
   * Create a new Section by intersection with another Section
   *
   * @param other Section other section
   * @return new Section, composed
   * @throws InvalidRangeException if want.getRank() not equal to this.getRank(), or invalid component Range
   */
  public Section intersect(Section other) throws InvalidRangeException {
    if (!compatibleRank(other)) {
      throw new InvalidRangeException("Invalid Section rank");
    }

    // check individual nulls
    List<Range> results = new ArrayList<>(getRank());
    for (int j = 0; j < ranges.size(); j++) {
      Range base = ranges.get(j);
      Range r = other.getRange(j);
      results.add(base.intersect(r));
    }

    return new Section(results);
  }

  /**
   * Compute the element offset of an intersecting subrange of this.
   * 
   * @param intersect the subrange
   * @return element offset
   */
  public int offset(Section intersect) throws InvalidRangeException {
    if (!compatibleRank(intersect)) {
      throw new InvalidRangeException("Incompatible Section rank");
    }

    int result = 0;
    int stride = 1;
    for (int j = ranges.size() - 1; j >= 0; j--) {
      Range base = ranges.get(j);
      Range r = intersect.getRange(j);
      int offset = base.index(r.first());
      result += offset * stride;
      stride *= base.length();
    }

    return result;
  }

  /**
   * See if this Section intersects with another Section. ignores strides, vlen
   *
   * @param other another section
   * @return true if intersection is non-empty
   * @throws InvalidRangeException if want.getRank() not equal to this.getRank(),
   *
   */
  public boolean intersects(Section other) throws InvalidRangeException {
    if (!compatibleRank(other)) {
      throw new InvalidRangeException("Invalid Section rank");
    }

    for (int j = 0; j < ranges.size(); j++) {
      Range base = ranges.get(j);
      Range r = other.getRange(j);
      if (base == Range.VLEN || r == Range.VLEN) {
        continue;
      }
      if (!base.intersects(r)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Convert List of Ranges to String sectionSpec.
   * Inverse of new Section(String sectionSpec)
   *
   * @return index section String specification
   */
  @Override
  public String toString() {
    Formatter sbuff = new Formatter();
    for (int i = 0; i < ranges.size(); i++) {
      Range r = ranges.get(i);
      if (i > 0)
        sbuff.format(",");
      if (r == null)
        sbuff.format(":");
      else {
        sbuff.format("%s", r.toString());
      }
    }
    return sbuff.toString();
  }

  /** Does this contain a VLEN range? */
  public boolean isVariableLength() {
    for (Range aFrom : ranges) {
      if (aFrom == Range.VLEN) {
        return true;
      }
    }
    return false;
  }

  public boolean isStrided() {
    for (Range r : ranges) {
      if (r.stride() != 1) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get shape array using the Range.length() values.
   *
   * @return int[] shape
   */
  public int[] getShape() {
    int[] result = new int[ranges.size()];
    for (int i = 0; i < ranges.size(); i++) {
      result[i] = ranges.get(i).length();
    }
    return result;
  }

  /**
   * Get origin array using the Range.first() values.
   *
   * @return int[] origin
   */
  public int[] getOrigin() {
    int[] result = new int[ranges.size()];
    for (int i = 0; i < ranges.size(); i++) {
      result[i] = ranges.get(i).first();
    }
    return result;
  }

  /**
   * Get stride array using the Range.stride() values.
   *
   * @return int[] stride
   */
  public int[] getStride() {
    int[] result = new int[ranges.size()];
    for (int i = 0; i < ranges.size(); i++) {
      result[i] = ranges.get(i).stride();
    }
    return result;
  }

  /** Get origin of the ith Range */
  public int getOrigin(int i) {
    return ranges.get(i).first();
  }

  /** Get length of the ith Range */
  public int getShape(int i) {
    return ranges.get(i).length();
  }

  /** Get stride of the ith Range */
  public int getStride(int i) {
    return ranges.get(i).stride();
  }

  /** Get rank = number of Ranges. */
  public int getRank() {
    return ranges.size();
  }

  private boolean compatibleRank(Section other) {
    return (getRank() == other.getRank());
  }

  /**
   * Compute total number of elements represented by the section.
   * Any null or VLEN Ranges are skipped.
   *
   * @return total number of elements
   */
  public long computeSize() {
    long product = 1;
    for (Range r : ranges) {
      if (r == null || r.length() < 0) {
        continue;
      }
      product *= r.length();
    }
    return product;
  }

  /** Get an unmodifyable list of Ranges. */
  public List<Range> getRanges() {
    return ranges;
  }

  /**
   * Get the ith Range
   *
   * @param i index into the list of Ranges
   * @return ith Range
   */
  public Range getRange(int i) {
    return ranges.get(i);
  }

  /** Fil any null sections with the ccorresponding value from shape. */
  public Section fill(int[] shape) throws InvalidRangeException {
    return fill(this, shape);
  }

  /**
   * Find a Range by its name.
   *
   * @param rangeName find this Range
   * @return named Range or null
   */
  @Nullable
  public Range find(String rangeName) {
    for (Range r : ranges) {
      if (rangeName.equals(r.name())) {
        return r;
      }
    }
    return null;
  }

  /**
   * Check if this Section is legal for the given shape.
   * [Note: modified by dmh to address the case of unlimited
   * where the size is zero]
   *
   * @param shape range must fit within this shape, rank must match.
   * @return error message if illegal, null if all ok
   */
  public String checkInRange(int[] shape) {
    if (ranges.size() != shape.length) {
      return "Number of ranges in section (" + ranges.size() + ") must be = " + shape.length;
    }

    for (int i = 0; i < ranges.size(); i++) {
      Range r = ranges.get(i);
      if (r == null)
        continue;
      if (r == Range.VLEN)
        continue;
      if (r == Range.EMPTY) {
        if (shape[i] != 0)
          return "Illegal Range for dimension " + i + ": empty range only for unlimited dimension len = 0";
        else
          continue;
      }
      if (r.last() >= shape[i])
        return "Illegal Range for dimension " + i + ": last requested " + r.last() + " > max " + (shape[i] - 1);
    }

    return null;
  }

  /**
   * Is this section equivilent to the given shape.
   * All non-null ranges must have origin 0 and length = shape[i]
   */
  public boolean equivalent(int[] shape) throws InvalidRangeException {
    if (isScalar(shape) && isScalar(getShape())) {
      return true;
    }
    if (getRank() != shape.length) {
      throw new InvalidRangeException("Invalid Section rank");
    }

    for (int i = 0; i < ranges.size(); i++) {
      Range r = ranges.get(i);
      if (r == null)
        continue;
      if (r.first() != 0)
        return false;
      if (r.length() != shape[i])
        return false;
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Section section = (Section) o;
    return Objects.equals(ranges, section.ranges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ranges);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Iterate over a section, returning the index in an equivalent 1D array of shape[], and optionally the corresponding
   * index[n]
   * So this is a section in a (possibly) larger array described by shape[].
   * The index is in the "source" array.
   *
   * @param shape total array shape
   * @return iterator over this section
   */
  public Iterator getIterator(int[] shape) {
    return new Iterator(shape);
  }

  public class Iterator {
    private final int[] odo = new int[getRank()]; // odometer - the current element LOOK could use Index, but must
                                                  // upgrade to using Range
    private final List<java.util.Iterator<Integer>> rangeIterList = new ArrayList<>();
    private final int[] stride = new int[getRank()];
    private final long total;
    private long done;

    Iterator(int[] shape) {
      int ss = 1;
      for (int i = getRank() - 1; i >= 0; i--) { // fastest varying last
        stride[i] = ss;
        ss *= shape[i];
      }

      for (int i = 0; i < getRank(); i++) {
        java.util.Iterator<Integer> iter = getRange(i).iterator();
        odo[i] = iter.next();
        rangeIterList.add(iter);
      }

      done = 0;
      total = Arrays.computeSize(getShape()); // total in the section
    }

    /** Return true if there are more elements */
    public boolean hasNext() {
      return done < total;
    }

    /**
     * Get the position in the equivalant 1D array of shape[]
     *
     * @param index if not null, return the current nD index
     * @return the current position in a 1D array
     */
    public int next(int[] index) {
      int next = currentElement();
      if (index != null)
        System.arraycopy(odo, 0, index, 0, odo.length);

      done++;
      if (done < total)
        incr(); // increment for next call
      return next;
    }

    private void incr() {
      int digit = getRank() - 1;
      while (digit >= 0) {
        java.util.Iterator<Integer> iter = rangeIterList.get(digit);
        if (iter.hasNext()) {
          odo[digit] = iter.next();
          break; // normal exit
        }

        // else, carry to next digit in the odometer
        java.util.Iterator<Integer> iterReset = getRange(digit).iterator();
        odo[digit] = iterReset.next();
        rangeIterList.set(digit, iterReset);
        digit--;
        assert digit >= 0; // catch screw-ups
      }
    }

    private int currentElement() {
      int value = 0;
      for (int ii = 0; ii < getRank(); ii++)
        value += odo[ii] * stride[ii];
      return value;
    }
  } // Section.Iterator

  public Builder toBuilder() {
    return new Builder().appendRanges(this.getRanges());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    List<Range> ranges = new ArrayList<>();

    /** Append a Range to the Section, LOOK may be null. */
    public Builder appendRange(@Nullable Range range) {
      ranges.add(range);
      return this;
    }

    /** Append a new Range(0,size-1) */
    public Builder appendRange(int size) {
      if (size > 1)
        ranges.add(new Range(size));
      else if (size == 0)
        ranges.add(Range.EMPTY);
      else if (size == 1)
        ranges.add(Range.SCALAR);
      else
        ranges.add(Range.VLEN);
      return this;
    }

    /**
     * Append a new Range(first, last) to the Section
     *
     * @param first starting index
     * @param last last index, inclusive. If last &lt; 0, then append a VLEN Range.
     */
    public Builder appendRange(int first, int last) throws InvalidRangeException {
      if (last < 0)
        ranges.add(Range.VLEN);
      else
        ranges.add(new Range(first, last));
      return this;
    }

    /**
     * Append a new Range(first,last,stride) to the Section.
     *
     * @param first starting index
     * @param last last index, inclusive
     * @param stride stride
     */
    public Builder appendRange(int first, int last, int stride) throws InvalidRangeException {
      if (last < 0)
        ranges.add(Range.VLEN);
      else
        ranges.add(new Range(first, last, stride));
      return this;
    }

    /**
     * Append a new Range(name,first,last,stride) to the Section
     *
     * @param name name of Range
     * @param first starting index
     * @param last last index, inclusive
     * @param stride stride
     */
    public Builder appendRange(String name, int first, int last, int stride) throws InvalidRangeException {
      if (last < 0)
        ranges.add(Range.VLEN);
      else
        ranges.add(new Range(name, first, last, stride));
      return this;
    }

    /** Append Ranges to the Section */
    public Builder appendRanges(List<Range> ranges) {
      this.ranges.addAll(ranges);
      return this;
    }

    /** Append Ranges to the Section, Range(shape[i]) for each i. */
    public Builder appendRanges(int[] shape) {
      for (int aShape : shape) {
        appendRange(aShape);
      }
      return this;
    }

    /**
     * Insert a range at the specified index in the list.
     * 
     * @param index insert here in the list, existing ranges at or after this index get shifted by one
     * @param r insert this Range
     */
    public Builder insertRange(int index, Range r) {
      ranges.add(index, r);
      return this;
    }

    /**
     * Remove a range at the specified index in the list.
     * 
     * @param index remove here in the list, existing ranges after this index get shifted by one
     */
    public Builder removeRange(int index) {
      ranges.remove(index);
      return this;
    }

    /**
     * Replace a range at the specified index in the list.
     *
     * @param index replace here in the list.
     * @param r use this Range
     * @return this
     * @throws IndexOutOfBoundsException if bad index
     */
    public Builder replaceRange(int index, Range r) {
      ranges.set(index, r);
      return this;
    }

    /**
     * Set the range at the specified index in the list, previous Range is discarded
     * 
     * @param index list index, must be in interval [0,size).
     * @param r insert this Range
     */
    public Builder setRange(int index, Range r) {
      ranges.set(index, r);
      return this;
    }

    /** Remove the last range, if it exists. */
    public Builder removeLast() {
      int size = ranges.size();
      if (size > 0) {
        ranges.remove(size - 1);
      }
      return this;
    }

    /** Remove the first n Ranges, n &le; number of ranges. */
    public Builder removeFirst(int n) {
      assert n <= ranges.size();
      ranges = ranges.subList(n, ranges.size());
      return this;
    }

    /** Remove the last range, if it exists and is a Vlen. */
    public Builder removeVlen() {
      int size = ranges.size();
      if (ranges.get(size - 1) == Range.VLEN) {
        ranges.remove(size - 1);
      }
      return this;
    }

    public Section build() {
      return new Section(ranges);
    }
  }

}
