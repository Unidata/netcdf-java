/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.math.DoubleMath;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Optional;

/** Helper class for GridAxis1D for subsetting and searching. */
class GridAxis1DHelper {
  private final GridAxis1D orgGrid;

  GridAxis1DHelper(GridAxis1D orgGrid) {
    this.orgGrid = orgGrid;
  }

  /**
   * Given a coordinate interval, find what grid element matches it.
   * 
   * @param target interval in this coordinate system
   * @param bounded if true, always return a valid index. otherwise can return < 0 or > n-1
   * @return index of grid point containing it, or < 0 or > n-1 if outside grid area
   */
  int findCoordElement(double[] target, boolean bounded) {
    switch (orgGrid.getSpacing()) {
      case regularInterval:
        // can use midpoint
        return findCoordElementRegular((target[0] + target[1]) / 2, bounded);
      case contiguousInterval:
        // can use midpoint
        return findCoordElementContiguous((target[0] + target[1]) / 2, bounded);
      case discontiguousInterval:
        // cant use midpoint
        return findCoordElementDiscontiguousInterval(target, bounded);
    }
    throw new IllegalStateException("unknown spacing" + orgGrid.getSpacing());
  }

  /**
   * Given a coordinate position, find what grid element contains it.
   * This means that
   * 
   * <pre>
   * edge[i] <= target < edge[i+1] (if values are ascending)
   * edge[i] > target >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param target position in this coordinate system
   * @param bounded if true, always return a valid index. otherwise can return < 0 or > n-1
   * @return index of grid point containing it, or < 0 or > n-1 if outside grid area
   */
  int findCoordElement(double target, boolean bounded) {
    switch (orgGrid.getSpacing()) {
      case regularInterval:
      case regularPoint:
        return findCoordElementRegular(target, bounded);
      case irregularPoint:
      case contiguousInterval:
        return findCoordElementContiguous(target, bounded);
      case discontiguousInterval:
        return findCoordElementDiscontiguousInterval(target, bounded);
    }
    throw new IllegalStateException("unknown spacing" + orgGrid.getSpacing());
  }

  // same contract as findCoordElement()
  private int findCoordElementRegular(double coordValue, boolean bounded) {
    int n = orgGrid.getNcoords();
    if (n == 1 && bounded)
      return 0;

    double distance = coordValue - orgGrid.getCoordEdge1(0);
    double exactNumSteps = distance / orgGrid.getResolution();
    // int index = (int) Math.round(exactNumSteps); // ties round to +Inf
    int index = (int) exactNumSteps; // truncate down

    if (bounded && index < 0)
      return 0;
    if (bounded && index >= n)
      return n - 1;

    // check that found point is within interval
    if (index >= 0 && index < n) {
      double lower = orgGrid.getCoordEdge1(index);
      double upper = orgGrid.getCoordEdge2(index);
      if (orgGrid.isAscending()) {
        assert lower <= coordValue : lower + " should be le " + coordValue;
        assert upper >= coordValue : upper + " should be ge " + coordValue;
      } else {
        assert lower >= coordValue : lower + " should be ge " + coordValue;
        assert upper <= coordValue : upper + " should be le " + coordValue;
      }
    }

    return index;
  }

  /**
   * Performs a binary search to find the index of the element of the array whose value is contained in the contiguous
   * intervals.
   * irregularPoint, // irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval, // irregular contiguous spaced intervals (values, npts), values are the edges, and there are
   * npts+1, coord halfway between edges
   * <p>
   * same contract as findCoordElement()
   */
  private int findCoordElementContiguous(double target, boolean bounded) {
    int n = orgGrid.getNcoords();
    // double resolution = (values[n-1] - values[0]) / (n - 1);
    // int startGuess = (int) Math.round((target - values[0]) / resolution);

    int low = 0;
    int high = n - 1;
    if (orgGrid.isAscending()) {
      // Check that the point is within range
      if (target < orgGrid.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target > orgGrid.getCoordEdgeLast())
        return bounded ? n - 1 : n;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2; // binary search
        if (intervalContains(target, mid, true))
          return mid;
        else if (orgGrid.getCoordEdge2(mid) < target)
          low = mid;
        else
          high = mid;
      }
      return intervalContains(target, low, true) ? low : high;

    } else { // descending

      // Check that the point is within range
      if (target > orgGrid.getCoordEdge1(0))
        return bounded ? 0 : -1;
      else if (target < orgGrid.getCoordEdgeLast())
        return bounded ? n - 1 : n;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2; // binary search
        if (intervalContains(target, mid, false))
          return mid;
        else if (orgGrid.getCoordEdge2(mid) < target)
          high = mid;
        else
          low = mid;
      }
      return intervalContains(target, low, false) ? low : high;
    }
  }

  // same contract as findCoordElement(); in addition, -1 is returned when the target is not found
  // will return immediately if an exact match is found (i.e. interval with edges equal to target)
  // If bounded is true, then we will also look for the interval closest to the midpoint of the
  // target
  private int findCoordElementDiscontiguousInterval(double[] target, boolean bounded) {
    // Check that the target is within range
    int n = orgGrid.getNcoords();
    double upperIndex = target[1] > target[0] ? target[1] : target[0];
    double lowerIndex = upperIndex == target[1] ? target[0] : target[1];
    if (lowerIndex < orgGrid.getCoordEdge1(0))
      return bounded ? 0 : -1;
    else if (upperIndex > orgGrid.getCoordEdgeLast())
      return bounded ? n - 1 : n;
    // see if we can find an exact match
    int index = -1;
    for (int i = 0; i < orgGrid.getNcoords(); i++) {
      double edge1 = orgGrid.getCoordEdge1(i);
      double edge2 = orgGrid.getCoordEdge2(i);

      if (DoubleMath.fuzzyEquals(edge1, target[0], 1.0e-8) && DoubleMath.fuzzyEquals(edge2, target[1], 1.0e-8))
        return i;
    }
    // ok, give up on exact match, try to find interval closest to midpoint of the target
    if (bounded) {
      index = findCoordElementDiscontiguousInterval(lowerIndex + (upperIndex - lowerIndex) / 2, bounded);
    }

    return index;
  }

  // same contract as findCoordElement(); in addition, -1 is returned when the target is not contained in any interval
  // LOOK not using bounded
  private int findCoordElementDiscontiguousInterval(double target, boolean bounded) {
    int idx = findSingleHit(target);
    if (idx >= 0)
      return idx;
    if (idx == -1)
      return -1; // no hits

    // multiple hits = choose closest (definition of closest will be based on axis type)
    return findClosestDiscontiguousInterval(target);
  }

  boolean intervalContains(double target, int coordIdx) {
    // Target must be in the closed bounds defined by the discontiguous interval at index coordIdx.
    double midVal1 = orgGrid.getCoordEdge1(coordIdx);
    double midVal2 = orgGrid.getCoordEdge2(coordIdx);
    boolean ascending = midVal1 < midVal2;
    return intervalContains(target, coordIdx, ascending);
  }

  private boolean intervalContains(double target, int coordIdx, boolean ascending) {
    // Target must be in the closed bounds defined by the discontiguous interval at index coordIdx.
    double lowerVal = ascending ? orgGrid.getCoordEdge1(coordIdx) : orgGrid.getCoordEdge2(coordIdx);
    double upperVal = ascending ? orgGrid.getCoordEdge2(coordIdx) : orgGrid.getCoordEdge1(coordIdx);

    return lowerVal <= target && target <= upperVal;
  }

  // return index if only one match, if no matches return -1, if > 1 match return -nhits
  private int findSingleHit(double target) {
    int hits = 0;
    int idxFound = -1;
    int n = orgGrid.getNcoords();
    for (int i = 0; i < n; i++) {
      if (intervalContains(target, i)) {
        hits++;
        idxFound = i;
      }
    }
    if (hits == 1)
      return idxFound;
    if (hits == 0)
      return -1;
    return -hits;
  }

  // return index of closest value to target
  // For Discontiguous Intervals, "closest" means:
  // - time coordinate axis? Interval such that the end of the interval is closest to the target value
  // - otherwise, interval midpoint closest to target
  // If there are multiple matches for closest:
  // - time coordinate axis? Use the interval with the smallest non-zero interval width
  // - otherwise, use the one with the largest midpoint value
  private int findClosestDiscontiguousInterval(double target) {
    boolean isTemporal = orgGrid.getAxisType().equals(AxisType.Time);
    return isTemporal ? findClosestDiscontiguousTimeInterval(target) : findClosestDiscontiguousNonTimeInterval(target);
  }

  private int findClosestDiscontiguousTimeInterval(double target) {
    double minDiff = Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;
    int idxFound = -1;

    for (int i = 0; i < orgGrid.getNcoords(); i++) {
      // only check if target is in discontiguous interval i
      if (intervalContains(target, i)) {
        // find the end of the time interval
        double coord = orgGrid.getCoordEdge2(i);
        // We want to make sure the interval includes our target point, and that means the end of the interval
        // must be greater than or equal to the target.
        if (coord >= target) {
          // compute the width (in time) of the interval
          double width = orgGrid.getCoordEdge2(i) - orgGrid.getCoordEdge1(i);
          // we want to identify the interval with the end point closest to our target
          // why? Because a statistic computed over a time window will only have meaning at the end
          // of that interval, so the closer we can get to that the better.
          double diff = Math.abs(coord - target);
          // Here we minimize the difference between the end of an interval and our target value. If multiple
          // intervals result in the same difference value, we will pick the one with the smallest non-zero
          // width interval.
          boolean tiebreaker = (diff == minDiff) && (width != 0) && (width < useValue);
          if (diff < minDiff || tiebreaker) {
            minDiff = diff;
            idxFound = i;
            useValue = width;
          }
        }
      }
    }
    return idxFound;
  }

  private int findClosestDiscontiguousNonTimeInterval(double target) {
    int idxFound = -1;
    double minDiff = Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;

    for (int i = 0; i < orgGrid.getNcoords(); i++) {
      // only check if target is in discontiguous interval i
      if (intervalContains(target, i)) {
        // get midpoint of interval
        double coord = orgGrid.getCoordMidpoint(i);
        // how close is the midpoint of this interval to our target value?
        double diff = Math.abs(coord - target);
        // Look for the interval with the minimum difference. If multiple intervals have the same
        // difference between their midpoint and the target value, pick then one with the larger
        // midpoint value
        // LOOK - is maximum midpoint value the right tie breaker?
        boolean tiebreaker = (diff == minDiff) && (coord > useValue);
        if (diff < minDiff || tiebreaker) {
          minDiff = diff;
          idxFound = i;
          useValue = coord;
        }
      }
    }
    return idxFound;
  }

  //////////////////////////////////////////////////////////////

  public Optional<GridAxis1D.Builder<?>> subset(double minValue, double maxValue, int stride, Formatter errLog) {
    return subsetValues(minValue, maxValue, stride, errLog);
  }

  public GridAxis1D.Builder<?> subsetClosest(double want) {
    return subsetValuesClosest(want);
  }

  public GridAxis1D.Builder<?> subsetLatest() {
    return subsetValuesLatest();
  }

  public GridAxis1D.Builder<?> subsetClosest(CalendarDate date) {
    double want = ((GridAxis1DTime) orgGrid).convert(date);
    return isDiscontiguousInterval() ? subsetClosestDiscontiguousInterval(date) : subsetValuesClosest(want);
  }

  private GridAxis1D.Builder<?> subsetClosestDiscontiguousInterval(CalendarDate date) {
    // this is specific to dates
    double target = ((GridAxis1DTime) orgGrid).convert(date);

    double maxDateToSearch = 0;
    int intervals = 0;
    // if there are multiple intervals that contain the target
    // we want to find the maximum amount of time to subset based
    // on the end of the widest interval
    for (int i = 0; i < orgGrid.getNcoords(); i++) {
      if (intervalContains(target, i)) {
        intervals += 1;
        double bound1 = orgGrid.getCoordEdge1(i);
        double bound2 = orgGrid.getCoordEdge2(i);
        double intervalEnd = bound1 < bound2 ? bound2 : bound1;
        if (intervalEnd > maxDateToSearch) {
          maxDateToSearch = intervalEnd;
        }
      }
    }

    // if we found one or more intervals, try to handle that by subsetting over a range of time
    Optional<GridAxis1D.Builder<?>> multipleIntervalBuilder = Optional.empty();
    if (intervals > 0) {
      multipleIntervalBuilder = subset(target, maxDateToSearch, 1, new Formatter());
    }

    // if multipleIntervalBuilder exists, return it. Otherwise fallback to subsetValuesClosest.
    return multipleIntervalBuilder.orElseGet(() -> subsetValuesClosest(((GridAxis1DTime) orgGrid).convert(date)));
  }

  public GridAxis1D.Builder<?> subsetClosest(CalendarDate[] date) {
    double[] want = new double[2];
    want[0] = ((GridAxis1DTime) orgGrid).convert(date[0]);
    want[1] = ((GridAxis1DTime) orgGrid).convert(date[1]);
    return subsetValuesClosest(want);
  }

  public Optional<GridAxis1D.Builder<?>> subset(CalendarDateRange dateRange, int stride, Formatter errLog) {
    double min = ((GridAxis1DTime) orgGrid).convert(dateRange.getStart());
    double max = ((GridAxis1DTime) orgGrid).convert(dateRange.getEnd());
    return subsetValues(min, max, stride, errLog);
  }

  // LOOK could specialize when only one point
  private Optional<GridAxis1D.Builder<?>> subsetValues(double minValue, double maxValue, int stride, Formatter errLog) {

    double lower = orgGrid.isAscending() ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = orgGrid.isAscending() ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = findCoordElement(lower, false);
    int maxIndex = findCoordElement(upper, false);

    if (minIndex >= orgGrid.getNcoords()) {
      errLog.format("no points in subset: lower %f > end %f", lower, orgGrid.getEndValue());
      return Optional.empty();
    }
    if (maxIndex < 0) {
      errLog.format("no points in subset: upper %f < start %f", upper, orgGrid.getStartValue());
      return Optional.empty();
    }

    if (minIndex < 0)
      minIndex = 0;
    if (maxIndex >= orgGrid.getNcoords())
      maxIndex = orgGrid.getNcoords() - 1;

    int count = maxIndex - minIndex + 1;
    if (count <= 0)
      throw new IllegalArgumentException("no points in subset");

    try {
      return Optional.of(subsetByIndex(new Range(minIndex, maxIndex, stride)));
    } catch (InvalidRangeException e) {
      errLog.format("%s", e.getMessage());
      return Optional.empty();
    }
  }

  public Optional<RangeIterator> makeRange(double minValue, double maxValue, int stride, Formatter errLog) {

    double lower = orgGrid.isAscending() ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = orgGrid.isAscending() ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = findCoordElement(lower, false);
    int maxIndex = findCoordElement(upper, false);

    if (minIndex >= orgGrid.getNcoords()) {
      errLog.format("no points in subset: lower %f > end %f", lower, orgGrid.getEndValue());
      return Optional.empty();
    }
    if (maxIndex < 0) {
      errLog.format("no points in subset: upper %f < start %f", upper, orgGrid.getStartValue());
      return Optional.empty();
    }

    if (minIndex < 0)
      minIndex = 0;
    if (maxIndex >= orgGrid.getNcoords())
      maxIndex = orgGrid.getNcoords() - 1;

    int count = maxIndex - minIndex + 1;
    if (count <= 0) {
      errLog.format("no points in subset");
      return Optional.empty();
    }

    try {
      return Optional.of(new Range(minIndex, maxIndex, stride));
    } catch (InvalidRangeException e) {
      errLog.format("%s", e.getMessage());
      return Optional.empty();
    }
  }

  // Range must be contained in this range
  GridAxis1D.Builder<?> subsetByIndex(Range range) throws InvalidRangeException {
    int ncoords = range.length();
    if (range.last() >= orgGrid.getNcoords())
      throw new InvalidRangeException("range.last() >= axis.getNcoords()");

    double resolution = 0.0;

    int count2 = 0;
    double[] values = orgGrid.getValues(); // will be null for regular
    double[] subsetValues = null;
    switch (orgGrid.getSpacing()) {
      case regularInterval:
      case regularPoint:
        resolution = range.stride() * orgGrid.getResolution();
        break;

      case irregularPoint:
        subsetValues = new double[ncoords];
        for (int i : range)
          subsetValues[count2++] = values[i];
        break;

      case contiguousInterval:
        subsetValues = new double[ncoords + 1]; // need npts+1
        for (int i : range)
          subsetValues[count2++] = values[i];
        subsetValues[count2] = values[range.last() + 1];
        break;

      case discontiguousInterval:
        subsetValues = new double[2 * ncoords]; // need 2*npts
        for (int i : range) {
          subsetValues[count2++] = values[2 * i];
          subsetValues[count2++] = values[2 * i + 1];
        }
        break;
    }

    // subset(int ncoords, double start, double end, double[] values)
    GridAxis1D.Builder<?> builder = orgGrid.toBuilder();
    builder.subset(ncoords, orgGrid.getCoordMidpoint(range.first()), orgGrid.getCoordMidpoint(range.last()), resolution,
        subsetValues);
    builder.setRange(range);
    return builder;
  }

  private GridAxis1D.Builder<?> subsetValuesClosest(double[] want) {
    int closest_index = findCoordElement(want, true); // bounded, always valid index

    GridAxis1D.Builder<?> builder = orgGrid.toBuilder();

    if (orgGrid.spacing == GridAxis.Spacing.regularInterval) {
      double val1 = orgGrid.getCoordEdge1(closest_index);
      double val2 = orgGrid.getCoordEdge2(closest_index);
      builder.subset(1, val1, val2, val2 - val1, null);
    } else {
      builder.subset(1, 0, 0, 0.0, makeValues(closest_index));
    }

    try {
      builder.setRange(new Range(closest_index, closest_index));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return builder;
  }

  private GridAxis1D.Builder<?> subsetValuesClosest(double want) {
    int closest_index = findCoordElement(want, true); // bounded, always valid index
    GridAxis1D.Builder<?> builder = orgGrid.toBuilder();

    if (orgGrid.spacing == GridAxis.Spacing.regularPoint) {
      double val = orgGrid.getCoordMidpoint(closest_index);
      builder.subset(1, val, val, 0.0, null);

    } else if (orgGrid.spacing == GridAxis.Spacing.regularInterval) {
      double val1 = orgGrid.getCoordEdge1(closest_index);
      double val2 = orgGrid.getCoordEdge2(closest_index);
      builder.subset(1, val1, val2, val2 - val1, null);

    } else {
      builder.subset(1, 0, 0, 0.0, makeValues(closest_index));
    }

    try {
      builder.setRange(new Range(closest_index, closest_index));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return builder;
  }

  Optional<GridAxis1D.Builder<?>> subsetContaining(double want, Formatter errlog) {
    int index = findCoordElement(want, false); // not bounded, may not be valid index
    if (index < 0 || index >= orgGrid.getNcoords()) {
      errlog.format("value %f not in axis %s", want, orgGrid.getName());
      return Optional.empty();
    }

    double val = orgGrid.getCoordMidpoint(index);

    GridAxis1D.Builder<?> builder = orgGrid.toBuilder();
    builder.subset(1, val, val, 0.0, makeValues(index));
    try {
      builder.setRange(new Range(index, index));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return Optional.of(builder);
  }

  private GridAxis1D.Builder subsetValuesLatest() {
    int last = orgGrid.getNcoords() - 1;
    double val = orgGrid.getCoordMidpoint(last);

    GridAxis1D.Builder<?> builder = orgGrid.toBuilder();
    builder.subset(1, val, val, 0.0, makeValues(last));
    try {
      builder.setRange(new Range(last, last));
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
    return builder;
  }

  private double[] makeValues(int index) {
    double[] subsetValues = null; // null for regular

    switch (orgGrid.getSpacing()) {
      case irregularPoint:
        subsetValues = new double[1];
        subsetValues[0] = orgGrid.getCoordMidpoint(index);
        break;

      case discontiguousInterval:
      case contiguousInterval:
        subsetValues = new double[2];
        subsetValues[0] = orgGrid.getCoordEdge1(index);
        subsetValues[1] = orgGrid.getCoordEdge2(index);
        break;
    }
    return subsetValues;
  }

  int search(double want) {
    if (orgGrid.getNcoords() == 1) {
      return DoubleMath.fuzzyEquals(want, orgGrid.getStartValue(), 1.0e-8) ? 0 : -1;
    }
    if (orgGrid.isRegular()) {
      double fval = (want - orgGrid.getStartValue()) / orgGrid.getResolution();
      double ival = Math.rint(fval);
      return DoubleMath.fuzzyEquals(fval, ival, 1.0e-8) ? (int) ival : (int) -ival - 1; // LOOK
    }

    // otherwise do a binary search
    return Arrays.binarySearch(orgGrid.getValues(), want);
  }

  private boolean isDiscontiguousInterval() {
    GridAxis.Spacing spacing = orgGrid.getSpacing();
    if (spacing != null) {
      return spacing.equals(GridAxis.Spacing.discontiguousInterval);
    }
    return false;
  }
}
