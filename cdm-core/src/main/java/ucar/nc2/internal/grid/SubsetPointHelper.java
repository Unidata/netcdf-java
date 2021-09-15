/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.Grids;
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

      // timeOffset, timeOffsetIntv, timeLatest, timeFirst; timeAll is the default
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
        if (params.getTimeFirst()) {
          return makeSubsetByIndex(0);
        }
        if (params.getTimeOffsetRange() != null) {
          CoordInterval range = params.getTimeOffsetRange();
          return subsetRange(range.start(), range.end(), 1, errlog).orElse(null);
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
    double val = orgGridAxis.getCoordDouble(index);
    return builder.subsetWithSingleValue(val, Range.make(index, index));
  }

  ////////////////////////////////////////////////////////////////////////
  // Used by GridHorizCoordinateSystem

  // LOOK bounded ?
  public Optional<GridAxisPoint.Builder<?>> subsetRange(double minValue, double maxValue, int stride,
      Formatter errlog) {
    Preconditions.checkNotNull(errlog);

    double lower;
    double upper;

    // LOOK can we do this in CylindricalCoord instead?
    // longitude wrapping (no seam cross, just look for cylinder that intersects)
    if (orgGridAxis.getAxisType() == AxisType.Lon) {
      if (maxValue < minValue) {
        maxValue = +360;
      }
      double minAxis = orgGridAxis.getCoordDouble(0);
      double maxAxis = orgGridAxis.getCoordDouble(orgGridAxis.getNominalSize() - 1);
      if (maxValue < minAxis) {
        lower = minValue + 360;
        upper = maxValue + 360;
      } else if (minValue > maxAxis) {
        lower = minValue - 360;
        upper = maxValue - 360;
      } else {
        lower = minValue;
        upper = maxValue;
      }
    } else {
      lower = Grids.isAscending(orgGridAxis) ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
      upper = Grids.isAscending(orgGridAxis) ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);
    }

    int minIndex = SubsetHelpers.findCoordElement(orgGridAxis, lower, false);
    int maxIndex = SubsetHelpers.findCoordElement(orgGridAxis, upper, false);

    if (minIndex >= orgGridAxis.getNominalSize()) {
      errlog.format("no points in subset: lower %f > end %f", lower,
          orgGridAxis.getCoordDouble(orgGridAxis.getNominalSize() - 1));
      return Optional.empty();
    }
    if (maxIndex < 0) {
      errlog.format("no points in subset: upper %f < start %f", upper, orgGridAxis.getCoordDouble(0));
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
      errlog.format("%s", e.getMessage());
      return Optional.empty();
    }
  }

}
