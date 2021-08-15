/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import ucar.array.MinMax;
import ucar.nc2.constants.AxisType;

import static ucar.nc2.grid.GridAxisSpacing.discontiguousInterval;

public class Grids {

  /** Standard sort on Coordinate Axes */
  public static class AxisComparator implements java.util.Comparator<GridAxis<?>> {
    public int compare(GridAxis c1, GridAxis c2) {
      Preconditions.checkNotNull(c1);
      Preconditions.checkNotNull(c2);
      AxisType t1 = c1.getAxisType();
      AxisType t2 = c2.getAxisType();
      Preconditions.checkNotNull(t1);
      Preconditions.checkNotNull(t2);
      return t1.axisOrder() - t2.axisOrder();
    }
  }

  public static MinMax getCoordEdgeMinMax(GridAxis<?> axis) {
    if (axis.getSpacing() != discontiguousInterval) {
      CoordInterval start = axis.getCoordInterval(0);
      CoordInterval end = axis.getCoordInterval(axis.getNominalSize() - 1);
      double min = Math.min(start.start(), end.end());
      double max = Math.max(start.start(), end.end());
      return MinMax.create(min, max);
    } else {
      double max = -Double.MAX_VALUE;
      double min = Double.MAX_VALUE;
      for (int i = 0; i < axis.getNominalSize(); i++) {
        CoordInterval intv = axis.getCoordInterval(i);
        min = Math.min(min, intv.start());
        min = Math.min(min, intv.end());
        max = Math.max(max, intv.start());
        max = Math.max(max, intv.end());
      }
      return MinMax.create(min, max);
    }
  }

  public static boolean isAscending(GridAxis<?> axis) {
    switch (axis.getSpacing()) {
      case regularInterval:
      case regularPoint:
        return axis.getResolution() > 0;

      case contiguousInterval:
      case irregularPoint:
      case nominalPoint:
        return axis.getCoordMidpoint(0) <= axis.getCoordMidpoint(axis.getNominalSize() - 1);

      case discontiguousInterval: // actually ambiguous
        return axis.getCoordInterval(0).start() <= axis.getCoordInterval(axis.getNominalSize() - 1).end();
    }
    throw new IllegalStateException("unknown spacing=" + axis.getSpacing());
  }
}
