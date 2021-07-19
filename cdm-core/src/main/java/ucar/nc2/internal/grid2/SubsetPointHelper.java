/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid2;

import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.grid2.CoordInterval;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.Grids;
import ucar.nc2.util.Misc;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.Optional;

/**
 * Helper class for GridAxisPoint for subsetting and searching.
 * Placed in this package so that its not part of the public API.
 */
@Immutable
public class SubsetPointHelper {
  private final GridAxisPoint orgGridAxis;

  public SubsetPointHelper(GridAxisPoint orgGrid) {
    this.orgGridAxis = orgGrid;
  }

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
        Number eval = params.getEnsCoord();
        if (eval != null) {
          return subsetClosest(eval.doubleValue());
        }
        // default is all
        break;
      }

      case Time:
      case TimeOffset: {
        Double dval = params.getTimeOffset();
        if (dval != null) {
          return subsetClosest(dval);
        }

        CoordInterval intv = params.getTimeOffsetIntv();
        if (intv != null) {
          return subsetClosest(intv);
        }

        if (params.getTimeLatest()) {
          int last = orgGridAxis.getNominalSize() - 1;
          return makeSubsetByIndex(last);
        }

        // default is all
        break;
      }

      // These are subsetted by the HorizCS LOOK
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

  private GridAxisPoint.Builder<?> subsetClosest(double want) {
    return makeSubsetValuesClosest(want);
  }

  private GridAxisPoint.Builder<?> subsetClosest(CoordInterval want) {
    // first look for exact match
    for (int idx = 0; idx < orgGridAxis.getNominalSize(); idx++) {
      CoordInterval intv = orgGridAxis.getCoordInterval(idx);
      double bound1 = intv.start();
      double bound2 = intv.end();
      if (Misc.nearlyEquals(bound1, want.start()) && Misc.nearlyEquals(bound2, want.end())) {
        return makeSubsetByIndex(idx);
      }
    }
    return makeSubsetValuesClosest(want);
  }

  private GridAxisPoint.Builder<?> makeSubsetValuesClosest(CoordInterval want) {
    int closest_index = SubsetHelpers.findCoordElement(orgGridAxis, want, true); // bounded, always valid index
    return makeSubsetByIndex(closest_index);
  }

  private GridAxisPoint.Builder<?> makeSubsetValuesClosest(double want) {
    int closest_index = SubsetHelpers.findCoordElement(orgGridAxis, want, true); // bounded, always valid index
    return makeSubsetByIndex(closest_index);
  }

  GridAxisPoint.Builder<?> makeSubsetByIndex(int index) {
    GridAxisPoint.Builder<?> builder = orgGridAxis.toBuilder();
    double val = orgGridAxis.getCoordMidpoint(index);
    return builder.subsetWithSingleValue(val, Range.make(index, index));
  }

  ////////////////////////////////////////////////////////////////////////
  // Used by GridHorizCoordinateSystem

  // LOOK bounded ?
  public Optional<GridAxisPoint.Builder<?>> subsetRange(double minValue, double maxValue, int stride,
      Formatter errLog) {
    double lower = Grids.isAscending(orgGridAxis) ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = Grids.isAscending(orgGridAxis) ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = SubsetHelpers.findCoordElement(orgGridAxis, lower, false);
    int maxIndex = SubsetHelpers.findCoordElement(orgGridAxis, upper, false);

    if (minIndex >= orgGridAxis.getNominalSize()) {
      errLog.format("no points in subset: lower %f > end %f", lower,
          orgGridAxis.getCoordMidpoint(orgGridAxis.getNominalSize() - 1));
      return Optional.empty();
    }
    if (maxIndex < 0) {
      errLog.format("no points in subset: upper %f < start %f", upper, orgGridAxis.getCoordMidpoint(0));
      return Optional.empty();
    }

    if (minIndex < 0) {
      minIndex = 0;
    }
    if (maxIndex >= orgGridAxis.getNominalSize()) {
      maxIndex = orgGridAxis.getNominalSize() - 1;
    }

    int count = maxIndex - minIndex + 1;
    if (count <= 0) {
      throw new IllegalArgumentException("no points in subset");
    }

    try {
      GridAxisPoint.Builder<?> builder = orgGridAxis.toBuilder().subsetWithRange(new Range(minIndex, maxIndex, stride));
      return Optional.of(builder);
    } catch (InvalidRangeException e) {
      errLog.format("%s", e.getMessage());
      return Optional.empty();
    }
  }

}
