/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.MaterializedCoordinateSystem;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

public class CylindricalCoord {

  public static boolean isCylindrical(GridHorizCoordinateSystem hcs) {
    GridAxisPoint xaxis = hcs.getXHorizAxis();
    int n = xaxis.getNominalSize();
    double width = xaxis.getCoordInterval(n - 1).end() - xaxis.getCoordInterval(0).start();
    return (xaxis.getAxisType() == AxisType.Lon && width >= 360);
  }

  private final GridHorizCoordinateSystem hcs;
  private final GridAxisPoint xaxis;
  private final double start;
  private final double end;
  private List<Range> needsSpecialRead;

  public CylindricalCoord(GridHorizCoordinateSystem hcs) {
    Preconditions.checkArgument(isCylindrical(hcs));
    this.hcs = hcs;
    this.xaxis = hcs.getXHorizAxis();
    this.start = xaxis.getCoordInterval(0).start();
    this.end = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();
  }

  public boolean needsSpecialRead() {
    return (this.needsSpecialRead != null);
  }

  // no strides for now
  public Optional<GridAxisPoint> subsetLon(LatLonRect llbb, Formatter errlog) {
    double wantMin = LatLonPoints.lonNormalFrom(llbb.getLonMin(), this.start);
    double wantMax = LatLonPoints.lonNormalFrom(llbb.getLonMax(), this.start);

    // may be 0, 1, or 2 CoordIntervals
    List<Range> lonIntvs = subsetLonIntervals(wantMin, wantMax);

    if (lonIntvs.isEmpty()) {
      errlog.format("longitude want [%f,%f] does not intersect lon axis [%f,%f]", wantMin, wantMax, start, end);
      return Optional.empty();
    }

    if (lonIntvs.size() == 1) {
      GridAxisPoint.Builder<?> builder = xaxis.toBuilder().subsetWithRange(lonIntvs.get(0));
      return Optional.of(builder.build());
    }

    // this is the seam crossing case.
    // Munge the axis, must call readSpecial()
    this.needsSpecialRead = lonIntvs;
    return null; // xaxis.subsetByIntervals(lonIntvs, stride, errLog);
  }

  // LOOK CoordReturn not right
  private List<Range> subsetLonIntervals(double wantMin, double wantMax) {
    int minIndex = SubsetHelpers.findCoordElement(this.xaxis, wantMin, true);
    int maxIndex = SubsetHelpers.findCoordElement(this.xaxis, wantMax, true);

    if (wantMin < wantMax) {
      return ImmutableList.of(Range.make(minIndex, maxIndex));
    } else {
      return ImmutableList.of(Range.make(minIndex, xaxis.getNominalSize() - 1), Range.make(0, maxIndex));
    }
  }

  public Array<Number> readSpecial(MaterializedCoordinateSystem subsetCoordSys, Grid grid)
      throws InvalidRangeException, IOException {
    List<ucar.array.Range> ranges = subsetCoordSys.getSubsetRanges();
    return grid.readDataSection(new ucar.array.Section(ranges));
  }

  /*
   * This is the more general case, not needed here because we assume that we have a complete longitude axis
   * longitude subset, after normalizing to start
   * draw a circle, representing longitude values from start to start + 360.
   * all values are on this circle and are > start.
   * put start at bottom of circle, end > start, data has values from start, counterclockwise to end.
   * wantMin, wantMax can be anywhere, want goes from wantMin counterclockwise to wantMax.
   * wantMin may be less than or greater than wantMax.
   *
   * cases:
   * A. wantMin < wantMax
   * 1 wantMin, wantMax > end : empty
   * 2 wantMin, wantMax < end : [wantMin, wantMax]
   * 3 wantMin < end, wantMax > end : [wantMin, end]
   *
   * B. wantMin > wantMax
   * 1 wantMin, wantMax > end : all [start, end]
   * 2 wantMin, wantMax < end : 2 pieces: [wantMin, end] + [start, wantMax]
   * 3 wantMin < end, wantMax > end : [wantMin, end]
   *
   * use MinMax to hold a real valued interval, min < max
   */
  private List<CoordInterval> subsetLonIntervals(double wantMin, double wantMax, double start, double end) {
    if (wantMin <= wantMax) {
      if (wantMin > end && wantMax > end) // none A.1
        return ImmutableList.of();

      if (wantMin < end && wantMax < end) // A.2
        return ImmutableList.of(CoordInterval.create(wantMin, wantMax));

      if (wantMin < end && wantMax > end) // A.3
        return ImmutableList.of(CoordInterval.create(wantMin, end));

    } else {
      if (wantMin > end && wantMax > end) { // all B.1
        return ImmutableList.of(CoordInterval.create(start, end));
      } else if (wantMin <= end && wantMax <= end) { // B.2
        return ImmutableList.of(CoordInterval.create(wantMin, end), CoordInterval.create(start, wantMax));
      } else if (wantMin <= end && wantMax > end) { // B.3
        return ImmutableList.of(CoordInterval.create(wantMin, end));
      }
    }

    // otherwise no intersection
    return ImmutableList.of();
  }

}
