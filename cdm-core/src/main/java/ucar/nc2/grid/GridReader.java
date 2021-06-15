/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GridReader {
  private final Grid grid;
  private final Map<String, Object> req = new HashMap<>();

  public GridReader(Grid grid) {
    this.grid = grid;
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

  public GridReader setTime(CalendarDate date) {
    req.put(GridSubset.time, date);
    return this;
  }

  public GridReader setTimePresent() {
    req.put(GridSubset.timePresent, true);
    return this;
  }

  public GridReader setTimeCoord(Object coord) {
    if (coord instanceof Number) {
      req.put(GridSubset.timePoint, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(GridSubset.timeIntv, coord);
    } else {
      throw new RuntimeException("setTimeCoord must be Number or CoordInterval " + coord);
    }
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

  public GridReader setRunTime(CalendarDate date) {
    req.put(GridSubset.runtime, date);
    return this;
  }

  public GridReader setLatLonBoundingBox(LatLonRect llbb) {
    req.put(GridSubset.latlonBB, llbb);
    return this;
  }

  public GridReader setEnsCoord(Object coord) {
    Preconditions.checkArgument(coord instanceof Double);
    req.put(GridSubset.ensCoord, coord);
    return this;
  }

  public GridReader setHorizStride(int stride) {
    req.put(GridSubset.horizStride, stride);
    return this;
  }

  public GridReferencedArray read() throws IOException, ucar.array.InvalidRangeException {
    return this.grid.readData(new GridSubset(req));
  }

}
