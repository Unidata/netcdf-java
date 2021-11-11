/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import com.google.common.base.Preconditions;
import ucar.array.Arrays;
import ucar.array.RangeIterator;
import ucar.array.Section;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Section composed of List&lt;RangeIterator&gt; rather than List&t;Range&gt;.
 * SectionIterable knows the fullShape of which it is a section.
 * The iteration is over the elements in the section, returning 1D index into the full shape.
 */
@Immutable
public class SectionIterable implements Iterable<Integer> {

  private final List<RangeIterator> ranges;
  private final int[] fullShape;

  public SectionIterable(List<RangeIterator> ranges, int[] fullShape) {
    assert ranges.size() == fullShape.length : ranges.size() + " != " + fullShape.length;
    int count = 0;
    for (RangeIterator ri : ranges) {
      assert (ri.length() <= fullShape[count]);
      count++;
    }

    this.ranges = ranges;
    this.fullShape = fullShape;
  }

  public SectionIterable(List<RangeIterator> ranges, List<Integer> fullShapeList) {
    assert ranges.size() == fullShapeList.size() : ranges.size() + " != " + fullShapeList.size();
    int count = 0;
    this.fullShape = new int[fullShapeList.size()];
    for (RangeIterator ri : ranges) {
      Preconditions.checkArgument(ri.length() <= fullShapeList.get(count));
      this.fullShape[count] = fullShapeList.get(count);
      count++;
    }

    this.ranges = ranges;
  }

  public SectionIterable(Section section, int[] fullShape) {
    this.ranges = new ArrayList<>();
    this.ranges.addAll(section.getRanges());
    this.fullShape = fullShape;
  }

  public SectionIterable(Section section, List<Integer> fullShapeList) {
    this.ranges = new ArrayList<>();
    this.ranges.addAll(section.getRanges());

    int count = 0;
    this.fullShape = new int[fullShapeList.size()];
    for (RangeIterator ri : ranges) {
      Preconditions.checkArgument(ri.length() <= fullShapeList.get(count));
      this.fullShape[count] = fullShapeList.get(count);
      count++;
    }
  }

  public int getRank() {
    return ranges.size();
  }

  public SectionIterable subSection(int start, int endExclusive) {
    int n = endExclusive - start;
    int[] subFullRange = new int[n];
    System.arraycopy(fullShape, start, subFullRange, 0, n);

    return new SectionIterable(ranges.subList(start, endExclusive), subFullRange);
  }

  public RangeIterator getRange(int i) {
    return ranges.get(i);
  }

  public int[] getShape() {
    int[] result = new int[getRank()];
    for (int i = 0; i < getRank(); i++)
      result[i] = getRange(i).length();
    return result;
  }

  public long computeSize() {
    long product = 1;
    for (RangeIterator r : ranges) {
      product *= r.length();
    }
    return product;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // use this if you just need the 1D source index, not the equivilent nD index
  @Override
  public Iterator<Integer> iterator() {
    return new SectionIterator();
  }

  /**
   * Iterate over a section, returning the index in an equivalent 1D array of shape[]
   * So this is a section in a (possibly) larger array described by shape[].
   * The index is in the "source" array.
   *
   * @return iterator over this section
   */
  public SectionIterator getIterator() {
    return new SectionIterator();
  }

  public class SectionIterator implements Iterator<Integer> {
    private final int[] odo = new int[getRank()]; // odometer - the current element
                                                  // LOOK could use Index, but must upgrade to using Range
    private final List<Iterator<Integer>> rangeIterList = new ArrayList<>();
    private final int[] stride = new int[getRank()];
    private final long total;
    private long done;

    SectionIterator() {
      int ss = 1;
      for (int i = getRank() - 1; i >= 0; i--) { // fastest varying last
        stride[i] = ss;
        ss *= fullShape[i];
      }

      for (int i = 0; i < getRank(); i++) {
        Iterator<Integer> iter = getRange(i).iterator();
        odo[i] = iter.next();
        rangeIterList.add(iter);
      }

      done = 0;
      total = Arrays.computeSize(getShape()); // total in the section
    }

    public boolean hasNext() {
      return done < total;
    }

    public Integer next() {
      int next = currentElement();
      done++;
      if (done < total)
        incr(); // increment for next call
      return next;
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
        Iterator<Integer> iter = rangeIterList.get(digit);
        if (iter.hasNext()) {
          odo[digit] = iter.next();
          break; // normal exit
        }

        // else, carry to next digit in the odometer
        Iterator<Integer> iterReset = getRange(digit).iterator();
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
  } // SectionIterator

}
