/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Fluent Api for creating subset parameters. LOOK incomplete. */
public class GridReader {
  private final Grid grid;
  private final Map<String, Object> req = new HashMap<>();

  public GridReader(Grid grid) {
    this.grid = grid;
  }


  public GridReader setRunTime(CalendarDate date) {
    req.put(GridSubset.runtime, date);
    return this;
  }

  public GridReader setRunTimeLatest() {
    req.put(GridSubset.runtimeLatest, true);
    return this;
  }

  public GridReader setTimeOffsetCoord(Object coord) {
    if (coord instanceof Number) {
      req.put(GridSubset.timeOffset, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(GridSubset.timeOffsetIntv, coord);
    } else {
      throw new RuntimeException("setTimeOffsetCoord must be Number or CoordInterval " + coord);
    }
    return this;
  }

  public GridReader setTimeOffsetRange(CoordInterval range) {
    req.put(GridSubset.timeOffsetRange, range);
    return this;
  }

  public GridReader setTimeFirst() {
    req.put(GridSubset.timeFirst, true);
    return this;
  }

  public GridReader setDate(CalendarDate date) {
    req.put(GridSubset.date, date);
    return this;
  }

  public GridReader setDateRange(CalendarDateRange dateRange) {
    req.put(GridSubset.dateRange, dateRange);
    return this;
  }

  public GridReader setTimePresent() {
    req.put(GridSubset.timePresent, true);
    return this;
  }

  public GridReader setTimeLatest() {
    req.put(GridSubset.timeLatest, true);
    return this;
  }

  public GridReader setTimeAll() {
    req.put(GridSubset.timeAll, true);
    return this;
  }

  public GridReader setVertCoord(Object coord) {
    if (coord instanceof Number) {
      req.put(GridSubset.vertPoint, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(GridSubset.vertIntv, coord);
    } else {
      throw new RuntimeException("setVertCoord must be Number or CoordInterval " + coord);
    }
    return this;
  }

  public GridReader setEnsCoord(Object coord) {
    Preconditions.checkArgument(coord instanceof Double);
    req.put(GridSubset.ensCoord, coord);
    return this;
  }

  public GridReader setLatLonPoint(LatLonPoint pt) {
    req.put(GridSubset.latlonPoint, pt);
    return this;
  }

  public GridReader setLatLonBoundingBox(LatLonRect llbb) {
    req.put(GridSubset.latlonBB, llbb);
    return this;
  }

  public GridReader setProjectionPoint(ProjectionPoint pt) {
    req.put(GridSubset.projectionPoint, pt);
    return this;
  }

  public GridReader setProjectionBoundingBox(ProjectionRect projRect) {
    req.put(GridSubset.projBB, projRect);
    return this;
  }

  public GridReader setHorizStride(int stride) {
    req.put(GridSubset.horizStride, stride);
    return this;
  }

  public GridReferencedArray read() throws IOException, ucar.array.InvalidRangeException {
    return this.grid.readData(new GridSubset(req));
  }

  @Override
  public String toString() {
    return req.toString();
  }
}
