/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import javax.annotation.Nullable;
import java.util.*;

/** Coordinate value-based subsetting of a Grid. */
public class GridSubset {
  public static final String gridName = "gridName"; // value = String

  public static final String latlonBB = "latlonBB"; // value = LatLonRect
  public static final String projBB = "projBB"; // value = ProjectionRect
  public static final String latlonPoint = "latlonPoint"; // value = LatLonPoint
  public static final String horizStride = "horizStride"; // value = Integer

  public static final String runtime = "runtime"; // value = CalendarDate
  public static final String runtimeLatest = "runtimeLatest"; // value = Boolean
  public static final String runtimeAll = "runtimeAll"; // value = Boolean

  public static final String timeOffset = "timeOffset"; // value = Double
  public static final String timeOffsetIntv = "timeOffsetIntv"; // value = CoordInterval
  public static final String timeOffsetFirst = "timeOffsetFirst"; // value = Boolean
  public static final String timeOffsetAll = "timeOffsetAll"; // value = Boolean

  public static final String time = "time"; // value = CalendarDate
  public static final String timePoint = "timePoint"; // value = Double
  public static final String timeIntv = "timeIntv"; // value = CoordInterval
  public static final String timeRange = "timeRange"; // value = CalendarDateRange
  public static final String timeStride = "timeStride"; // value = Integer
  public static final String timePresent = "timePresent"; // value = Boolean
  public static final String timeAll = "timeAll"; // value = Boolean
  public static final String timeWindow = "timeWindow"; // value = CalendarPeriod

  public static final String vertPoint = "vertPoint"; // value = Double
  public static final String vertIntv = "vertIntv"; // value = CoordInterval
  public static final String ensCoord = "ensCoord"; // value = Double

  // cant use these for selecting, used for validation
  public static final String timeOffsetDate = "timeOffsetDate"; // value = CalendarDate
  public static final String timeOffsetUnit = "timeOffsetUnit"; // value = CalendarDateUnit

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private final Map<String, Object> req = new HashMap<>();

  @Nullable
  private Object get(String key) {
    return req.get(key);
  }

  @Nullable
  private Double getDouble(String key) {
    Object val = req.get(key);
    if (val == null) {
      return null;
    } else if (val instanceof Number) {
      return ((Number) val).doubleValue();
    } else if (val instanceof String) {
      try {
        return Double.parseDouble((String) val);
      } catch (Exception e) {
        throw new RuntimeException(key + " cant parse as Double " + val);
      }
    }
    throw new RuntimeException(key + " not a Double " + val);
  }

  @Nullable
  private CalendarDate getCalendarDate(String key) {
    Object val = req.get(key);
    if (val == null) {
      return null;
    } else if (val instanceof CalendarDate) {
      return (CalendarDate) val;
    } else if (val instanceof String) {
      try {
        // TODO calendar
        return CalendarDate.parseISOformat(null, (String) val);
      } catch (Exception e) {
        throw new RuntimeException(key + " cant parse as Iso CalendarDate " + val);
      }
    }
    throw new RuntimeException(key + " not a CalendarDate " + val);
  }

  @Nullable
  private CalendarDateRange getCalendarDateRange(String key) {
    Object val = req.get(key);
    if (val == null)
      return null;
    if (val instanceof CalendarDateRange) {
      return (CalendarDateRange) val;
    }
    throw new RuntimeException(key + " not a Calendar Date Range " + val);
  }

  @Nullable
  private CoordInterval getCoordInterval(String key) {
    Object val = req.get(key);
    if (val == null) {
      return null;
    } else if (val instanceof CoordInterval) {
      return (CoordInterval) val;
    } else if (val instanceof String) {
      try {
        return CoordInterval.parse((String) val);
      } catch (Exception e) {
        throw new RuntimeException(key + " cant parse as CoordInterval " + val);
      }
    }
    throw new RuntimeException(key + " not a CoordInterval " + val);
  }

  @Nullable
  private Integer getInteger(String key) {
    Object val = req.get(key);
    if (val == null) {
      return null;
    } else if (val instanceof Number) {
      return ((Number) val).intValue();
    } else if (val instanceof String) {
      try {
        return Integer.parseInt((String) val);
      } catch (Exception e) {
        throw new RuntimeException(key + " cant parse as Integer " + val);
      }
    }
    throw new RuntimeException(key + " not a Integer " + val);
  }

  private boolean isTrue(String key) {
    Object val = req.get(key);
    return (val instanceof Boolean) && (Boolean) val;
  }

  public GridSubset setHorizStride(int stride) {
    req.put(horizStride, stride);
    return this;
  }

  @Nullable
  public Integer getHorizStride() {
    return (Integer) req.get(horizStride);
  }

  @Nullable
  public Double getEnsCoord() {
    return getDouble(ensCoord);
  }

  public GridSubset setEnsCoord(Object coord) {
    Preconditions.checkArgument(coord instanceof Double);
    req.put(ensCoord, coord);
    return this;
  }

  @Nullable
  public LatLonRect getLatLonBoundingBox() {
    return (LatLonRect) get(latlonBB);
  }

  public GridSubset setLatLonBoundingBox(LatLonRect llbb) {
    req.put(latlonBB, llbb);
    return this;
  }

  @Nullable
  public LatLonPoint getLatLonPoint() {
    return (LatLonPoint) get(latlonPoint);
  }

  public GridSubset setLatLonPoint(LatLonPoint pt) {
    req.put(latlonPoint, pt);
    return this;
  }

  @Nullable
  public ProjectionRect getProjectionBoundingBox() {
    return (ProjectionRect) get(projBB);
  }

  public GridSubset setProjectionBoundingBox(ProjectionRect projRect) {
    req.put(projBB, projRect);
    return this;
  }

  @Nullable
  public CalendarDate getRunTime() {
    return getCalendarDate(runtime);
  }

  public GridSubset setRunTime(CalendarDate date) {
    req.put(runtime, date);
    return this;
  }

  public boolean getRunTimeAll() {
    return isTrue(runtimeAll);
  }

  // TODO whats happens for multiple runtimes for timeOffsetRegular?
  public GridSubset setRunTimeAll() {
    req.put(runtimeAll, true);
    return this;
  }

  public boolean getRunTimeLatest() {
    return isTrue(runtimeLatest);
  }

  public GridSubset setRunTimeLatest() {
    req.put(runtimeLatest, true);
    return this;
  }

  @Nullable
  public CalendarDate getTime() {
    return getCalendarDate(time);
  }

  public GridSubset setTime(CalendarDate date) {
    req.put(time, date);
    return this;
  }

  @Nullable
  public Double getTimePoint() {
    return getDouble(timePoint);
  }

  @Nullable
  public CoordInterval getTimeIntv() {
    return getCoordInterval(timeIntv);
  }

  public GridSubset setTimeCoord(Object coord) {
    if (coord instanceof Double) {
      req.put(timePoint, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(timeIntv, coord);
    } else {
      throw new RuntimeException("setTimeCoord msut be Double or CoordInterval " + coord);
    }
    return this;
  }

  public boolean getTimePresent() {
    return isTrue(timePresent);
  }

  public GridSubset setTimePresent() {
    req.put(timePresent, true);
    return this;
  }

  @Nullable
  public CalendarDateRange getTimeRange() {
    return getCalendarDateRange(timeRange);
  }

  public GridSubset setTimeRange(CalendarDateRange dateRange) {
    req.put(timeRange, dateRange);
    return this;
  }

  @Nullable
  public Integer getTimeStride() {
    return getInteger(timeStride);
  }

  public GridSubset setTimeStride(int stride) {
    req.put(timeStride, stride);
    return this;
  }

  @Nullable
  public CalendarPeriod getTimeWindow() {
    return (CalendarPeriod) get(timeWindow);
  }

  public Double getTimeOffset() {
    return getDouble(timeOffset);
  }

  public GridSubset setTimeOffset(double offset) {
    req.put(timeOffset, offset);
    return this;
  }

  // A time offset or time offset interval starts from the rundate of that point, in the units of the coordinate
  // eg "calendar Month since 2004-12-30T00:00:00Z" or "Hours since 2004-12-30T00:00:00Z"
  public GridSubset setTimeOffsetCoord(Object coord) {
    if (coord instanceof Double) {
      req.put(timeOffset, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(timeOffsetIntv, coord);
    } else {
      throw new RuntimeException("setTimeOffsetCoord msut be Double or CoordInterval " + coord);
    }
    return this;
  }

  @Nullable
  public CoordInterval getTimeOffsetIntv() {
    return getCoordInterval(timeOffsetIntv);
  }

  public Boolean getTimeOffsetFirst() {
    return isTrue(timeOffsetFirst);
  }

  public GridSubset setTimeOffsetFirst() {
    req.put(timeOffsetFirst, true);
    return this;
  }

  @Nullable
  public Double getVertPoint() {
    return getDouble(vertPoint);
  }

  @Nullable
  public CoordInterval getVertIntv() {
    return getCoordInterval(vertIntv);
  }

  public GridSubset setVertCoord(Object coord) {
    if (coord instanceof Double) {
      req.put(vertPoint, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(vertIntv, coord);
    } else {
      throw new RuntimeException("setVertCoord msut be Double or CoordInterval " + coord);
    }
    return this;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    for (Map.Entry<String, Object> entry : req.entrySet()) {
      f.format(" %s == ", entry.getKey());
      Object val = entry.getValue();
      if (val instanceof CalendarDate[]) {
        CalendarDate[] cd = ((CalendarDate[]) val);
        f.format("[%s,%s]", cd[0], cd[1]);
      } else if (val instanceof double[]) {
        double[] d = ((double[]) val);
        f.format("[%f,%f]", d[0], d[1]);
      } else {
        f.format("%s,", entry.getValue());
      }
    }
    return f.toString();
  }

  public String getGridName() {
    return (String) get(gridName);
  }

  public GridSubset setGridName(String varName) {
    req.put(gridName, varName);
    return this;
  }

  public GridSubset() {}

  public GridSubset(Map<String, String> stringMap) {
    for (Map.Entry<String, String> entry : stringMap.entrySet()) {
      req.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridSubset that = (GridSubset) o;
    return req.equals(that.req);
  }

  @Override
  public int hashCode() {
    return Objects.hash(req);
  }
}
