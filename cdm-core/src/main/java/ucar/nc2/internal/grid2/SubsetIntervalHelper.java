/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.grid2.CoordInterval;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.grid2.GridAxisInterval;
import ucar.nc2.grid2.GridAxisSpacing;
import ucar.nc2.grid2.Grids;
import ucar.nc2.util.Misc;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.Optional;

/**
 * Helper class for GridAxisInterval for subsetting and searching.
 * Placed in this package so that its not part of the public API.
 */
@Immutable
public class SubsetIntervalHelper {
  private final GridAxisInterval orgGridAxis;

  public SubsetIntervalHelper(GridAxisInterval orgGrid) {
    this.orgGridAxis = orgGrid;
  }

  // TODO incomplete
  @Nullable
  public GridAxisInterval.Builder<?> subsetBuilder(GridSubset params, Formatter errLog) {
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

      case Time:
      case TimeOffset: {
        Double dval = params.getTimeOffset();
        if (dval != null) {
          return subsetClosest(dval); // LOOK what does this mean for an interval?
        }

        CoordInterval intv = params.getTimeOffsetIntv();
        if (intv != null) {
          return subsetClosest(intv);
        }

        if (params.getTimeLatest()) {
          int last = orgGridAxis.getNominalSize() - 1;
          return makeSubsetByIndex(Range.make(last, last));
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

  @Nullable
  public GridAxisInterval.Builder<?> subsetClosest(double want) {
    return makeSubsetValuesClosest(want);
  }

  @Nullable
  public GridAxisInterval.Builder<?> subsetClosest(CoordInterval want) {
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

  // SubsetRange must be contained in this range
  public GridAxisInterval.Builder<?> makeSubsetByIndex(Range subsetRange) {
    int ncoords = subsetRange.length();
    Preconditions.checkArgument(subsetRange.last() < orgGridAxis.getNominalSize());

    double resolution = 0.0;
    if (orgGridAxis.getSpacing().isRegular()) {
      resolution = subsetRange.stride() * orgGridAxis.getResolution();
    }

    GridAxisInterval.Builder<?> builder = orgGridAxis.toBuilder();
    builder.subset(ncoords, orgGridAxis.getCoordMidpoint(subsetRange.first()),
        orgGridAxis.getCoordMidpoint(subsetRange.last()), resolution, subsetRange);
    return builder;
  }

  @Nullable
  private GridAxisInterval.Builder<?> makeSubsetValuesClosest(CoordInterval want) {
    int closest_index = SubsetHelpers.findCoordElement(orgGridAxis, want, true); // bounded, always valid index
    if (closest_index < 0) { // LOOK discontIntv returns -1
      return null;
    }
    Range range = Range.make(closest_index, closest_index);

    GridAxisInterval.Builder<?> builder = orgGridAxis.toBuilder();
    if (orgGridAxis.getSpacing() == GridAxisSpacing.regularInterval) {
      CoordInterval intv = orgGridAxis.getCoordInterval(closest_index);
      double val1 = intv.start();
      double val2 = intv.end();
      builder.subset(1, val1, val2, val2 - val1, range);
    } else {
      builder.subset(1, 0, 0, 0.0, range);
    }
    return builder;
  }

  @Nullable
  private GridAxisInterval.Builder<?> makeSubsetValuesClosest(double want) {
    int closest_index = SubsetHelpers.findCoordElement(orgGridAxis, want, true); // bounded, always valid index
    if (closest_index < 0) { // LOOK discontIntv returns -1
      return null;
    }
    GridAxisInterval.Builder<?> builder = orgGridAxis.toBuilder();

    Range range;
    try {
      range = new Range(closest_index, closest_index);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }

    if (orgGridAxis.getSpacing() == GridAxisSpacing.regularInterval) {
      CoordInterval intv = orgGridAxis.getCoordInterval(closest_index);
      double val1 = intv.start();
      double val2 = intv.end();
      builder.subset(1, val1, val2, val2 - val1, range);

    } else {
      builder.subset(1, 0, 0, 0.0, range);
    }
    return builder;
  }

  /////////////////////////////////////////////////////////////////
  // Not currently used. needed to implement stride ??

  public Optional<GridAxisInterval.Builder<?>> subset(double minValue, double maxValue, int stride, Formatter errLog) {
    return makeSubsetValues(minValue, maxValue, stride, errLog);
  }

  // LOOK could specialize when only one point
  private Optional<GridAxisInterval.Builder<?>> makeSubsetValues(double minValue, double maxValue, int stride,
      Formatter errLog) {
    double lower = Grids.isAscending(orgGridAxis) ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = Grids.isAscending(orgGridAxis) ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = SubsetHelpers.findCoordElement(orgGridAxis, lower, false);
    int maxIndex = SubsetHelpers.findCoordElement(orgGridAxis, upper, false);

    if (minIndex >= orgGridAxis.getNominalSize()) {
      errLog.format("no points in subset: lower %f > end %f", lower, orgGridAxis.getCoordInterval(0).start());
      return Optional.empty();
    }
    if (maxIndex < 0) {
      errLog.format("no points in subset: upper %f < start %f", upper,
          orgGridAxis.getCoordInterval(orgGridAxis.getNominalSize() - 1).end());
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

}
