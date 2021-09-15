/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Coordinate value-based subsetting of a Grid.
 * LOOK problem is that the valid combinations for time are not obvious.
 * LOOK maybe should check, eg set vertCoord when there isnt any? or ignore?
 */
public class GridSubset {
  public static final String gridName = "gridName"; // value = String

  public static final String runtime = "runtime"; // value = CalendarDate
  public static final String runtimeLatest = "runtimeLatest"; // value = Boolean
  public static final String runtimeAll = "runtimeAll"; // value = Boolean LOOK unimplemented

  // The value is the offset in the units of the GridAxis
  public static final String timeOffset = "timeOffset"; // value = Double
  public static final String timeOffsetIntv = "timeOffsetIntv"; // value = CoordInterval
  public static final String timeOffsetRange = "timeOffsetRange"; // value = CoordInterval (start, end or range).

  // validtime
  public static final String date = "date"; // value = CalendarDate
  public static final String dateRange = "dateRange"; // value = CalendarDateRange

  public static final String timeAll = "timeAll"; // value = Boolean
  public static final String timeFirst = "timeFirst"; // value = Boolean
  public static final String timeLatest = "timeLatest"; // value = Boolean
  public static final String timePresent = "timePresent"; // value = Boolean
  public static final String timeStride = "timeStride"; // value = Integer LOOK unimplemented

  public static final String vertPoint = "vertPoint"; // value = Double
  public static final String vertIntv = "vertIntv"; // value = CoordInterval
  public static final String ensCoord = "ensCoord"; // value = Number

  public static final String latlonPoint = "latlonPoint"; // value = LatLonPoint
  public static final String latlonBB = "latlonBB"; // value = LatLonRect
  public static final String projBB = "projBB"; // value = ProjectionRect
  public static final String horizStride = "horizStride"; // value = Integer

  // cant use these for selecting, used for validation LOOK not being set
  public static final String timeOffsetDate = "timeOffsetDate"; // value = CalendarDate
  public static final String timeOffsetUnit = "timeOffsetUnit"; // value = CalendarDateUnit

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private final Map<String, Object> req = new HashMap<>();

  public static GridSubset create() {
    return new GridSubset();
  }

  public static GridSubset fromStringMap(Map<String, String> stringMap) {
    Map<String, Object> req = new HashMap<>();
    for (Map.Entry<String, String> entry : stringMap.entrySet()) {
      req.put(entry.getKey(), entry.getValue());
    }
    return new GridSubset(req);
  }

  public GridSubset(Map<String, Object> req) {
    this.req.putAll(req);
  }

  public boolean isEmpty() {
    return req.isEmpty();
  }

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
      // TODO calendar
      return CalendarDate.fromUdunitIsoDate(null, (String) val)
          .orElseThrow(() -> new RuntimeException(key + " cant parse as Iso CalendarDate " + val));
    }
    throw new RuntimeException(key + " not a CalendarDate " + val);
  }

  @Nullable
  private CalendarDateRange getCalendarDateRange(String key) {
    Object val = req.get(key);
    if (val == null) {
      return null;
    } else if (val instanceof CalendarDateRange) {
      return (CalendarDateRange) val;
    } else if (val instanceof String) {
      try {
        return parse((String) val);
      } catch (Exception e) {
        throw new RuntimeException(key + " cant parse as CalendarDateRange " + val);
      }
    }
    throw new RuntimeException(key + " not a CalendarDateRange " + val);
  }

  private static CalendarDateRange parse(String source) {
    StringBuilder sourceb = new StringBuilder(source);
    StringUtil2.removeAll(sourceb, "[]");
    List<String> ss = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(sourceb);
    if (ss.size() != 2) {
      return null;
    }
    CalendarDate start = CalendarDate.fromUdunitIsoDate(null, ss.get(0)).orElse(null);
    CalendarDate end = CalendarDate.fromUdunitIsoDate(null, ss.get(1)).orElse(null);

    try {
      return CalendarDateRange.of(start, end);
    } catch (Exception e) {
      return null;
    }
  }

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

  @Nullable
  private Number getNumber(String key) {
    Object val = req.get(key);
    if (val == null) {
      return null;
    } else if (val instanceof Number) {
      return ((Number) val).intValue();
    } else if (val instanceof String) {
      try {
        return Double.parseDouble((String) val);
      } catch (Exception e) {
        throw new RuntimeException(key + " cant parse as Double " + val);
      }
    }
    throw new RuntimeException(key + " not a Integer " + val);
  }

  private boolean isTrue(String key) {
    Object val = req.get(key);
    if (val instanceof String) {
      return ((String) val).equalsIgnoreCase("true");
    }
    return (val instanceof Boolean) && (Boolean) val;
  }

  public Set<Map.Entry<String, Object>> getEntries() {
    return req.entrySet();
  }

  ////////////////////////////////////////////////////

  @Nullable
  public Integer getHorizStride() {
    return getInteger(horizStride);
  }

  public GridSubset setHorizStride(int stride) {
    req.put(horizStride, stride);
    return this;
  }

  @Nullable
  public Number getEnsCoord() {
    return getNumber(ensCoord);
  }

  public GridSubset setEnsCoord(Number coord) {
    req.put(ensCoord, coord);
    return this;
  }

  @Nullable
  public LatLonRect getLatLonBoundingBox() {
    Object val = req.get(latlonBB);
    if (val == null) {
      return null;
    } else if (val instanceof LatLonRect) {
      return (LatLonRect) val;
    } else if (val instanceof String) {
      try {
        return LatLonRect.fromSpec((String) val);
      } catch (Exception e) {
        throw new RuntimeException(" cant parse as LatLonRect " + val);
      }
    }
    throw new RuntimeException(" not a LatLonRect " + val);
  }

  public GridSubset setLatLonBoundingBox(LatLonRect llbb) {
    req.put(latlonBB, llbb);
    return this;
  }

  @Nullable
  public LatLonPoint getLatLonPoint() {
    Object val = req.get(latlonPoint);
    if (val == null) {
      return null;
    } else if (val instanceof LatLonPoint) {
      return (LatLonPoint) val;
    } else if (val instanceof String) {
      try {
        return LatLonPoints.parseLatLonPoint((String) val);
      } catch (Exception e) {
        throw new RuntimeException(" cant parse as LatLonPoint " + val);
      }
    }
    throw new RuntimeException(" not a LatLonPoint " + val);
  }

  public GridSubset setLatLonPoint(LatLonPoint pt) {
    req.put(latlonPoint, pt);
    return this;
  }

  @Nullable
  public ProjectionRect getProjectionBoundingBox() {
    Object val = req.get(projBB);
    if (val == null) {
      return null;
    } else if (val instanceof ProjectionRect) {
      return (ProjectionRect) val;
    } else if (val instanceof String) {
      try {
        return ProjectionRect.fromSpec((String) val);
      } catch (Exception e) {
        throw new RuntimeException(" cant parse as ProjectionRect " + val);
      }
    }
    throw new RuntimeException(" not a ProjectionRect " + val);
  }

  public GridSubset setProjectionBoundingBox(ProjectionRect projRect) {
    req.put(projBB, projRect);
    return this;
  }

  @Nullable
  public CalendarDate getRunTime() {
    return getCalendarDate(runtime);
  }

  public GridSubset setRunTime(CalendarDate runDate) {
    req.put(runtime, runDate);
    return this;
  }

  public GridSubset setRunTimeCoord(Object coord) {
    Preconditions.checkArgument(coord instanceof CalendarDate);
    req.put(runtime, coord);
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
  public CalendarDate getDate() {
    return getCalendarDate(date);
  }

  public GridSubset setDate(CalendarDate datetime) {
    req.put(GridSubset.date, datetime);
    return this;
  }

  public boolean getTimeAll() {
    return isTrue(timeAll);
  }

  public GridSubset setTimeAll() {
    req.put(timeAll, true);
    return this;
  }

  public boolean getTimeLatest() {
    return isTrue(timeLatest);
  }

  public GridSubset setTimeLatest() {
    req.put(timeLatest, true);
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
  public CalendarDateRange getDateRange() {
    return getCalendarDateRange(dateRange);
  }

  public GridSubset setDateRange(CalendarDateRange dateRange) {
    req.put(GridSubset.dateRange, dateRange);
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
  public Double getTimeOffset() {
    return getDouble(timeOffset);
  }

  // A time offset or time offset interval starts from the rundate of that point, in the units of the coordinate
  // eg "calendar Month since 2004-12-30T00:00:00Z" or "Hours since 2004-12-30T00:00:00Z"
  public GridSubset setTimeOffsetCoord(Object coord) {
    if (coord instanceof Number) {
      req.put(timeOffset, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(timeOffsetIntv, coord);
    } else {
      throw new RuntimeException("setTimeOffsetCoord must be Number or CoordInterval " + coord);
    }
    return this;
  }

  @Nullable
  public CoordInterval getTimeOffsetIntv() {
    return getCoordInterval(timeOffsetIntv);
  }

  @Nullable
  public GridSubset setTimeOffsetRange(CoordInterval range) {
    req.put(timeOffsetRange, range);
    return this;
  }

  @Nullable
  public CoordInterval getTimeOffsetRange() {
    return getCoordInterval(timeOffsetRange);
  }

  public boolean getTimeFirst() {
    return isTrue(timeFirst);
  }

  // LOOK not set - used in grib validation
  public CalendarDate getTimeOffsetDate() {
    return (CalendarDate) get(timeOffsetDate);
  }

  // LOOK not set - used in grib validation
  public CalendarDateUnit getTimeOffsetUnit() {
    return (CalendarDateUnit) get(timeOffsetUnit);
  }

  public GridSubset setTimeFirst() {
    req.put(timeFirst, true);
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
    if (coord instanceof Number) {
      req.put(vertPoint, coord);
    } else if (coord instanceof CoordInterval) {
      req.put(vertIntv, coord);
    } else {
      throw new RuntimeException("setVertCoord must be Number or CoordInterval " + coord);
    }
    return this;
  }

  public String getGridName() {
    return (String) get(gridName);
  }

  public GridSubset setGridName(String name) {
    req.put(gridName, name);
    return this;
  }

  //////////////////////////////////////////////////////////////////////////////////
  GridSubset() {}

  @Override
  public String toString() {
    return req.toString();
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
