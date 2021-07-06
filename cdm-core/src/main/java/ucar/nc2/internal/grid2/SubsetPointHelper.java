/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.array.InvalidRangeException;
import ucar.array.MinMax;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridAxisSpacing;
import ucar.nc2.grid2.Grids;
import ucar.nc2.util.Misc;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.Optional;

/** Helper class for GridAxisPoint for subsetting and searching. */
@Immutable
public class SubsetPointHelper {
  private final GridAxisPoint orgGridAxis;

  public SubsetPointHelper(GridAxisPoint orgGrid) {
    this.orgGridAxis = orgGrid;
  }

  /**
   * Given a coordinate interval, find what grid element matches it.
   * 
   * @param target interval in this coordinate system
   * @param bounded if true, always return a valid index. otherwise can return < 0 or > n-1
   * @return index of grid point containing it, or < 0 or > n-1 if outside grid area<?>
   */
  public int findCoordElement(CoordInterval target, boolean bounded) {
    switch (orgGridAxis.getSpacing()) {
      case regularInterval:
        // can use midpoint
        return findCoordElementRegular(target.midpoint(), bounded);
      case contiguousInterval:
        // can use midpoint
        return findCoordElementContiguous(target.midpoint(), bounded);
      case discontiguousInterval:
        // cant use midpoint
        return findCoordElementDiscontiguousInterval(target, bounded);
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
  public int findCoordElement(double target, boolean bounded) {
    switch (orgGridAxis.getSpacing()) {
      case regularInterval:
      case regularPoint:
        return findCoordElementRegular(target, bounded);
      case irregularPoint:
      case contiguousInterval:
        return findCoordElementContiguous(target, bounded);
      case discontiguousInterval:
        return findCoordElementDiscontiguousInterval(target, bounded);
    }
    throw new IllegalStateException("unknown spacing" + orgGridAxis.getSpacing());
  }

  // same contract as findCoordElement()
  private int findCoordElementRegular(double coordValue, boolean bounded) {
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
  private int findCoordElementContiguous(double target, boolean bounded) {
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
        if (intervalContains(target, mid, true, false))
          return mid;
        else if (orgGridAxis.getCoordInterval(mid).end() < target)
          low = mid;
        else
          high = mid;
      }
      return intervalContains(target, low, true, false) ? low : high;

    } else { // descending
      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2; // binary search
        if (intervalContains(target, mid, false, false))
          return mid;
        else if (orgGridAxis.getCoordInterval(mid).end() < target)
          high = mid;
        else
          low = mid;
      }
      return intervalContains(target, low, false, false) ? low : high;
    }
  }

  // same contract as findCoordElement(); in addition, -1 is returned when the target is not found
  // will return immediately if an exact match is found (i.e. interval with edges equal to target)
  // If bounded is true, then we will also look for the interval closest to the midpoint of the target
  private int findCoordElementDiscontiguousInterval(CoordInterval target, boolean bounded) {
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
      index = findCoordElementDiscontiguousInterval(target.midpoint(), bounded);
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

  private boolean intervalContains(double target, int coordIdx) {
    // Target must be in the closed bounds defined by the discontiguous interval at index coordIdx.
    CoordInterval intv = orgGridAxis.getCoordInterval(coordIdx);
    boolean ascending = intv.start() < intv.end();
    return intervalContains(target, coordIdx, ascending, true);
  }

  // closed [low, high] or half-closed [low, high)
  private boolean intervalContains(double target, int coordIdx, boolean ascending, boolean closed) {
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
  private int findSingleHit(double target) {
    int hits = 0;
    int idxFound = -1;
    int n = orgGridAxis.getNominalSize();
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
    boolean isTemporal = orgGridAxis.getAxisType().equals(AxisType.Time);
    return isTemporal ? findClosestDiscontiguousTimeInterval(target) : findClosestDiscontiguousNonTimeInterval(target);
  }

  private int findClosestDiscontiguousTimeInterval(double target) {
    double minDiff = Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;
    int idxFound = -1;

    for (int i = 0; i < orgGridAxis.getNominalSize(); i++) {
      // only check if target is in discontiguous interval i
      if (intervalContains(target, i)) {
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

  private int findClosestDiscontiguousNonTimeInterval(double target) {
    int idxFound = -1;
    double minDiff = Double.MAX_VALUE;
    double useValue = Double.MIN_VALUE;

    for (int i = 0; i < orgGridAxis.getNominalSize(); i++) {
      // only check if target is in discontiguous interval i
      if (intervalContains(target, i)) {
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

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  // TODO incomplete handling of subsetting params
  @Nullable
  public GridAxisPoint.Builder<?> subsetBuilder(GridSubset params, Formatter errlog) {
    switch (orgGridAxis.getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height: {
        Double dval = params.getVertPoint();
        if (dval != null) {
          return subsetClosest(dval);
        }
        CoordInterval intv = params.getVertIntv();
        if (intv != null) {
          return subsetClosest(intv);
        }
        // default is all
        break;
      }

      case Ensemble: {
        Double eval = params.getEnsCoord();
        if (eval != null) {
          return subsetClosest(eval);
        }
        // default is all
        break;
      }

      case RunTime:
      case Time:
      case TimeOffset: {
        Double dval = params.getTimeOffset();
        if (dval != null) {
          return subsetClosest(dval);
        }

        CoordInterval intv = params.getTimeOffsetIntv();
        if (intv instanceof CoordInterval) {
          return subsetClosest(intv);
        }

        // TODO do we need this?
        if (params.getTimeOffsetFirst()) {
          return makeSubsetByIndex(new Range(1));
        }

        // default is all
        break;
      }

      // These are subsetted by the HorizCS
      case GeoX:
      case GeoY:
      case Lat:
      case Lon:
        return null;

      default:
        // default is all
        break;
    }

    // otherwise return copy of the original axis
    return orgGridAxis.toBuilder();
  }

  public Optional<GridAxisPoint.Builder<?>> subset(double minValue, double maxValue, int stride, Formatter errLog) {
    return makeSubsetValues(minValue, maxValue, stride, errLog);
  }

  public GridAxisPoint.Builder<?> subsetClosest(double want) {
    return makeSubsetValuesClosest(want);
  }

  public GridAxisPoint.Builder<?> subsetClosest(CoordInterval want) {
    for (int idx = 0; idx < orgGridAxis.getNominalSize(); idx++) {
      CoordInterval intv = orgGridAxis.getCoordInterval(idx);
      double bound1 = intv.start();
      double bound2 = intv.end();
      if (Misc.nearlyEquals(bound1, want.start()) && Misc.nearlyEquals(bound2, want.end())) {
        return makeSubsetByIndex(Range.make(idx, idx));
      }
    }
    return makeSubsetValuesClosest(want);
  }

  // SubsetRange must be contained in the total range
  public GridAxisPoint.Builder<?> makeSubsetByIndex(Range subsetRange) {
    int ncoords = subsetRange.length();
    Preconditions.checkArgument(subsetRange.last() < orgGridAxis.getNominalSize());

    double resolution = 0.0;
    if (orgGridAxis.getSpacing().isRegular()) {
      resolution = subsetRange.stride() * orgGridAxis.getResolution();
    }

    GridAxisPoint.Builder<?> builder = orgGridAxis.toBuilder();
    builder.subset(ncoords, orgGridAxis.getCoordMidpoint(subsetRange.first()),
        orgGridAxis.getCoordMidpoint(subsetRange.last()), resolution, subsetRange);
    return builder;
  }

  private GridAxisPoint.Builder<?> makeSubsetValuesClosest(CoordInterval want) {
    int closest_index = findCoordElement(want, true); // bounded, always valid index
    Range range = Range.make(closest_index, closest_index);

    GridAxisPoint.Builder<?> builder = orgGridAxis.toBuilder();
    builder.subset(1, 0, 0, 0.0, range);
    return builder;
  }

  private GridAxisPoint.Builder<?> makeSubsetValuesClosest(double want) {
    int closest_index = findCoordElement(want, true); // bounded, always valid index
    GridAxisPoint.Builder<?> builder = orgGridAxis.toBuilder();

    Range range;
    try {
      range = new Range(closest_index, closest_index);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }

    if (orgGridAxis.getSpacing() == GridAxisSpacing.regularPoint) {
      double val = orgGridAxis.getCoordMidpoint(closest_index);
      builder.subset(1, val, val, 0.0, range);

    } else {
      builder.subset(1, 0, 0, 0.0, range);
    }
    return builder;
  }

  // LOOK could specialize when only one point
  private Optional<GridAxisPoint.Builder<?>> makeSubsetValues(double minValue, double maxValue, int stride,
      Formatter errLog) {
    double lower = Grids.isAscending(orgGridAxis) ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = Grids.isAscending(orgGridAxis) ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = findCoordElement(lower, false);
    int maxIndex = findCoordElement(upper, false);

    if (minIndex >= orgGridAxis.getNominalSize()) {
      errLog.format("no points in subset: lower %f > end %f", lower,
          orgGridAxis.getCoordMidpoint(orgGridAxis.getNominalSize() - 1));
      return Optional.empty();
    }
    if (maxIndex < 0) {
      errLog.format("no points in subset: upper %f < start %f", upper, orgGridAxis.getCoordMidpoint(0));
      return Optional.empty();
    }

    if (minIndex < 0)
      minIndex = 0;
    if (maxIndex >= orgGridAxis.getNominalSize())
      maxIndex = orgGridAxis.getNominalSize() - 1;

    int count = maxIndex - minIndex + 1;
    if (count <= 0)
      throw new IllegalArgumentException("no points in subset");

    try {
      return Optional.of(makeSubsetByIndex(new Range(minIndex, maxIndex, stride)));
    } catch (InvalidRangeException e) {
      errLog.format("%s", e.getMessage());
      return Optional.empty();
    }
  }

  private GridAxisPoint.Builder<?> makeSubsetValuesLatest() {
    int last = orgGridAxis.getNominalSize() - 1;
    double val = orgGridAxis.getCoordMidpoint(last);

    Range range;
    try {
      range = new Range(last, last);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }

    GridAxisPoint.Builder<?> builder = orgGridAxis.toBuilder();
    builder.subset(1, val, val, 0.0, range);
    return builder;
  }

}
