/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.MaterializedCoordinateSystem;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.ProjectionRect;

import java.io.IOException;
import java.util.ArrayList;
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

  private final GridAxisPoint xaxis; // original xaxis
  private final double start;
  private final double end;
  private List<Range> lonIntvs;

  public CylindricalCoord(GridHorizCoordinateSystem hcs) {
    Preconditions.checkArgument(isCylindrical(hcs));
    this.xaxis = hcs.getXHorizAxis();
    this.start = xaxis.getCoordInterval(0).start();
    this.end = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();
  }

  public boolean needsSpecialRead() {
    return (this.lonIntvs != null);
  }

  public Optional<GridAxisPoint> subsetLon(ProjectionRect projbb, int horizStride, Formatter errlog) {
    double wantMin = LatLonPoints.lonNormalFrom(projbb.getMinX(), this.start);
    double wantMax = LatLonPoints.lonNormalFrom(projbb.getMaxX(), this.start);

    // may be 0, 1, or 2 CoordIntervals
    List<Range> lonIntvs = subsetLonIntervals(wantMin, wantMax, horizStride);

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
    this.lonIntvs = lonIntvs;
    GridAxisPoint xaxis0 = xaxis.toBuilder().subsetWithRange(lonIntvs.get(0)).build();
    GridAxisPoint xaxis1 = xaxis.toBuilder().subsetWithRange(lonIntvs.get(1)).build();
    GridAxisPoint.Builder<?> builder = xaxis.toBuilder();
    int ncoords = xaxis0.getNominalSize() + xaxis1.getNominalSize();
    int firstIndex = lonIntvs.get(0).first();
    Range rangeSubset = Range.make(firstIndex, firstIndex + ncoords * horizStride, horizStride);

    switch (xaxis.getSpacing()) {
      case regularPoint: // LOOK mistake to assume, probably need to convert to nominal
        builder.setRegular(ncoords, xaxis0.getCoordDouble(0), xaxis0.getResolution()).setIsSubset(true)
            .setRange(rangeSubset);
        break;
    }
    return Optional.of(builder.build());
  }

  private List<Range> subsetLonIntervals(double wantMin, double wantMax, int horizStride) {
    int minIndex = SubsetHelpers.findCoordElement(this.xaxis, wantMin, true);
    int maxIndex = SubsetHelpers.findCoordElement(this.xaxis, wantMax, true);

    if (wantMin < wantMax) {
      return ImmutableList.of(Range.make(minIndex, maxIndex, horizStride));
    } else {
      return ImmutableList.of(Range.make(minIndex, xaxis.getNominalSize() - 1, horizStride),
          Range.make(0, maxIndex, horizStride));
    }
  }

  public Array<Number> readSpecial(MaterializedCoordinateSystem subsetCoordSys, Grid grid)
      throws InvalidRangeException, IOException {
    ArrayList<Range> ranges = new ArrayList(subsetCoordSys.getSubsetRanges());
    int last = ranges.size() - 1;
    ranges.set(last, this.lonIntvs.get(0));
    Array<Number> data0 = grid.readDataSection(new ucar.array.Section(ranges));
    ranges.set(last, this.lonIntvs.get(1));
    Array<Number> data1 = grid.readDataSection(new ucar.array.Section(ranges));

    // LOOK could get clever and just manipulate the indices on two arrays
    int[] shape0 = data0.getShape();
    int[] shape1 = data1.getShape();
    int part0 = (int) data0.length() / shape0[last];
    int part1 = (int) data1.length() / shape1[last];
    int xlen = shape0[last] + shape1[last];

    int[] reshape0 = new int[] {part0, shape0[last]};
    int[] reshape1 = new int[] {part1, shape1[last]};
    Array<Number> redata0 = Arrays.reshape(data0, reshape0);
    Array<Number> redata1 = Arrays.reshape(data1, reshape1);

    double[] values = new double[(int) (data0.length() + data1.length())];
    for (int j = 0; j < part0; j++) {
      for (int i = 0; i < shape0[last]; i++) {
        values[j * xlen + i] = redata0.get(j, i).doubleValue();
      }
    }
    for (int j = 0; j < part1; j++) {
      for (int i = 0; i < shape1[last]; i++) {
        values[j * xlen + shape0[last] + i] = redata1.get(j, i).doubleValue();
      }
    }

    int[] shapeAll = java.util.Arrays.copyOf(shape0, shape0.length);
    shapeAll[last] = xlen;
    return Arrays.factory(ArrayType.DOUBLE, shapeAll, values);
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
