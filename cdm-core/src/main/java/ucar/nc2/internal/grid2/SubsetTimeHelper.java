/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import com.google.common.math.DoubleMath;
import ucar.array.Range;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import java.util.Formatter;

public class SubsetTimeHelper extends SubsetPointHelper {
  private final GridTimeCoordinateSystem tcs;
  private final GridAxisPoint time;

  public SubsetTimeHelper(GridTimeCoordinateSystem tcs, GridAxisPoint time) {
    super(time);
    this.tcs = tcs;
    this.time = time;
  }

  @Nullable
  public GridAxisPoint.Builder<?> subsetBuilder(GridSubset params, Formatter errlog) {
    if (params.getRunTimeLatest()) {
      return makeSubsetByIndex(new Range(1));
    }

    CalendarDate wantRuntime = params.getRunTime();
    if (wantRuntime != null) {
      double want = tcs.getCalendarDateUnit().makeOffsetFromRefDate(wantRuntime);
      int idx = search(want);
      if (idx >= 0) {
        return makeSubsetByIndex(Range.make(idx, idx));
      } else {
        return null;
      }
    }

    CalendarDate wantTime = params.getTime();
    if (wantTime != null) {
      double want = tcs.getCalendarDateUnit().makeOffsetFromRefDate(wantTime);
      int idx = search(want);
      if (idx >= 0) {
        return makeSubsetByIndex(Range.make(idx, idx));
      } else {
        return null;
      }
    }

    Double dval = params.getTimeOffset();
    if (dval != null) {
      return subsetClosest(dval);
    }

    CoordInterval intv = params.getTimeOffsetIntv();
    if (intv instanceof CoordInterval) {
      return subsetClosest(intv);
    }

    if (params.getTimeOffsetFirst()) {
      return makeSubsetByIndex(new Range(1));
    }

    // otherwise return copy of the original axis
    return time.toBuilder();
  }

  private int search(double want) {
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
