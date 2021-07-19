/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;
import ucar.array.MinMax;
import ucar.nc2.grid2.CoordInterval;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisInterval;
import ucar.nc2.grid2.Grids;

public class SubsetHelpers {

  /**
   * Given a coordinate interval, find what grid element matches it.
   *
   * @param target interval in this coordinate system
   * @param bounded if true, always return a valid index. otherwise can return < 0 or > n-1
   * @return index of grid point containing it, or < 0 or > n-1 if outside grid area<?>
   */
  public static int findCoordElement(GridAxis<?> orgGridAxis, CoordInterval target, boolean bounded) {
    switch (orgGridAxis.getSpacing()) {
      case regularInterval:
      case regularPoint:
        // can use midpoint
        return findCoordElementRegular(orgGridAxis, target.midpoint(), bounded);
      case nominalPoint:
      case contiguousInterval:
      case irregularPoint:
        // can use midpoint
        return findCoordElementContiguous(orgGridAxis, target.midpoint(), bounded);
      case discontiguousInterval:
        // cant use midpoint
        return findCoordElementDiscontiguousInterval((GridAxisInterval) orgGridAxis, target, bounded);
    }
    throw new IllegalStateException("unknown spacing" + orgGridAxis.getSpacing());
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
  public static int findCoordElement(GridAxis<?> orgGridAxis, double target, boolean bounded) {
    switch (orgGridAxis.getSpacing()) {
      case regularInterval:
      case regularPoint:
        return findCoordElementRegular(orgGridAxis, target, bounded);
      case nominalPoint:
      case irregularPoint:
      case contiguousInterval:
        return findCoordElementContiguous(orgGridAxis, target, bounded);
      case discontiguousInterval:
        return findCoordElementDiscontiguousInterval((GridAxisInterval) orgGridAxis, target, bounded);
    }
    throw new IllegalStateException("unknown spacing=" + orgGridAxis.getSpacing());
  }

  // same contract as findCoordElement()
  private static int findCoordElementRegular(GridAxis<?> orgGridAxis, double coordValue, boolean bounded) {
    int n = orgGridAxis.getNominalSize();
    if (n == 1 && bounded)
      return 0;

    double distance = coordValue - orgGridAxis.getCoordInterval(0).start();
    double exactNumSteps = distance / orgGridAxis.getResolution();
    // int index = (int) Math.round(exactNumSteps); // ties round to +Inf
    int index = (int) exactNumSteps; // truncate down

    if (bounded && index < 0)
      return 0;
    if (bounded && index >= n)
      return n - 1;

    // check that found point is within interval
    if (index >= 0 && index < n) {
      CoordInterval intv = orgGridAxis.getCoordInterval(index);
      double lower = intv.start();
      double upper = intv.end();
      if (Grids.isAscending(orgGridAxis)) {
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
   * Binary search to find the index whose value is contained in the contiguous intervals.
   * irregularPoint, // irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval, // irregular contiguous spaced intervals (values, npts), values are the edges, and there are
   * npts+1, coord halfway between edges
   * <p>
   * same contract as findCoordElement()
   */
  @VisibleForTesting
  static int findCoordElementContiguous(GridAxis<?> orgGridAxis, double target, boolean bounded) {
    int n = orgGridAxis.getNominalSize();

    // Check that the point is within range
    MinMax minmax = Grids.getCoordEdgeMinMax(orgGridAxis);
    if (target < minmax.min()) {
      return bounded ? 0 : -1;
    } else if (target > minmax.max()) {
      return bounded ? n - 1 : n;
    }

    int low = 0;
    int high = n - 1;
    if (Grids.isAscending(orgGridAxis)) {
      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2; // binary search
        if (intervalContains(orgGridAxis, target, mid, true, false))
          return mid;
        else if (orgGridAxis.getCoordInterval(mid).end() < target)
          low = mid;
        else
          high = mid;
      }
      return intervalContains(orgGridAxis, target, low, true, false) ? low : high;

    } else { // descending
      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2; // binary search
        if (intervalContains(orgGridAxis, target, mid, false, false))
          return mid;
        else if (orgGridAxis.getCoordInterval(mid).end() < target)
          high = mid;
        else
          low = mid;
      }
      return intervalContains(orgGridAxis, target, low, false, false) ? low : high;
    }
  }

  // same contract as findCoordElement(); in addition, -1 is returned when the target is not found
  // will return immediately if an exact match is found (i.e. interval with edges equal to target)
  // If bounded is true, then we will also look for the interval closest to the midpoint of the target
  private static int findCoordElementDiscontiguousInterval(GridAxisInterval orgGridAxis, CoordInterval target,
      boolean bounded) {
    Preconditions.checkNotNull(target);

    // Check that the target is within range
    int n = orgGridAxis.getNominalSize();
    double lowerValue = Math.min(target.start(), target.end());
    double upperValue = Math.max(target.start(), target.end());
    MinMax minmax = Grids.getCoordEdgeMinMax(orgGridAxis);
    if (upperValue < minmax.min()) {
      return bounded ? 0 : -1;
    } else if (lowerValue > minmax.max()) {
      return bounded ? n - 1 : n;
    }

    // see if we can find an exact match
    int index = -1;
    for (int i = 0; i < orgGridAxis.getNominalSize(); i++) {
      if (target.fuzzyEquals(orgGridAxis.getCoordInterval(i), 1.0e-8)) {
        return i;
      }
    }

    // ok, give up on exact match, try to find interval closest to midpoint of the target
    if (bounded) {
      index = findCoordElementDiscontiguousInterval(orgGridAxis, target.midpoint(), bounded);
    }
    return index;
  }

  // same contract as findCoordElement(); in addition, -1 is returned when the target is not contained in any interval
  // LOOK not using bounded
  private static int findCoordElementDiscontiguousInterval(GridAxisInterval orgGridAxis, double target,
      boolean bounded) {
    int idx = findSingleHit(orgGridAxis, target);
    if (idx >= 0)
      return idx;
    if (idx == -1)
      return -1; // no hits

    // multiple hits = choose closest (definition of closest will be based on axis type)
    return findClosestDiscontiguousInterval(orgGridAxis, target);
  }

  private static boolean intervalContains(GridAxisInterval orgGridAxis, double target, int coordIdx) {
    // Target must be in the closed bounds defined by the discontiguous interval at index coordIdx.
    CoordInterval intv = orgGridAxis.getCoordInterval(coordIdx);
    boolean ascending = intv.start() < intv.end();
    return intervalContains(orgGridAxis, target, coordIdx, ascending, true);
  }

  // closed [low, high] or half-closed [low, high)
  private static boolean intervalContains(GridAxis<?> orgGridAxis, double target, int coordIdx, boolean ascending,
      boolean closed) {
    // Target must be in the closed bounds defined by the discontiguous interval at index coordIdx.
    CoordInterval intv = orgGridAxis.getCoordInterval(coordIdx);
    double lowerVal = ascending ? intv.start() : intv.end();
    double upperVal = ascending ? intv.end() : intv.start();

    if (closed) {
      return lowerVal <= target && target <= upperVal;
    } else {
      return lowerVal <= target && target < upperVal;
    }
  }

  // return index if only one match, if no matches return -1, if > 1 match return -nhits
  private static int findSingleHit(GridAxisInterval orgGridAxis, double target) {
    int hits = 0;
    int idxFound = -1;
    int n = orgGridAxis.getNominalSize();
    for (int i = 0; i < n; i++) {
      if (intervalContains(orgGridAxis, target, i)) {
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
  private static int findClosestDiscontiguousInterval(GridAxisInterval orgGridAxis, double target) {
    boolean isTemporal = orgGridAxis.getAxisType().isTime();
    return isTemporal ? findClosestDiscontiguousTimeInterval(orgGridAxis, target)
        : findClosestDiscontiguousNonTimeInterval(orgGridAxis, target);
  }

  private static int findClosestDiscontiguousTimeInterval(GridAxisInterval orgGridAxis, double target) {
    double minDiff = Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;
    int idxFound = -1;

    for (int i = 0; i < orgGridAxis.getNominalSize(); i++) {
      // only check if target is in discontiguous interval i
      if (intervalContains(orgGridAxis, target, i)) {
        // find the end of the time interval
        CoordInterval intv = orgGridAxis.getCoordInterval(i);
        double coord = intv.end();
        // We want to make sure the interval includes our target point, and that means the end of the interval
        // must be greater than or equal to the target.
        if (coord >= target) {
          // compute the width (in time) of the interval
          double width = intv.end() - intv.start();
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

  private static int findClosestDiscontiguousNonTimeInterval(GridAxisInterval orgGridAxis, double target) {
    int idxFound = -1;
    double minDiff = Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;

    for (int i = 0; i < orgGridAxis.getNominalSize(); i++) {
      // only check if target is in discontiguous interval i
      if (intervalContains(orgGridAxis, target, i)) {
        // get midpoint of interval
        double coord = orgGridAxis.getCoordMidpoint(i);
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

  static int search(GridAxis<?> time, double want) {
    if (time.getNominalSize() == 1) {
      return DoubleMath.fuzzyEquals(want, time.getCoordMidpoint(0), 1.0e-8) ? 0 : -1;
    }
    if (time.isRegular()) {
      double fval = (want - time.getCoordMidpoint(0)) / time.getResolution();
      double ival = Math.rint(fval);
      return DoubleMath.fuzzyEquals(fval, ival, 1.0e-8) ? (int) ival : (int) -ival - 1; // LOOK
    }

    // otherwise do a binary search
    return time.binarySearch(want);
  }
}
