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

import java.util.*;

/** Coordinate value-based subsetting of a Grid. */
public class GridSubset {
  public static final String variables = "var"; // value = List<String>

  public static final String latlonBB = "latlonBB"; // value = LatLonRect
  public static final String projBB = "projBB"; // value = ProjectionRect
  public static final String horizStride = "horizStride"; // value = Integer
  public static final String latlonPoint = "latlonPoint"; // value = LatLonPoint
  public static final String stations = "stn"; // value = List<String>

  public static final String runtime = "runtime"; // value = CalendarDate
  public static final String runtimeLatest = "runtimeLatest"; // value = Boolean
  public static final String runtimeAll = "runtimeAll"; // value = Boolean

  public static final String timeOffset = "timeOffset"; // value = Double
  public static final String timeOffsetIntv = "timeOffsetIntv"; // value = CoordInterval
  public static final String timeOffsetFirst = "timeOffsetFirst"; // value = Boolean
  public static final String timeOffsetAll = "timeOffsetAll"; // value = Boolean

  public static final String time = "time"; // value = CalendarDate
  public static final String timeCoord = "timeCoord"; // value = Object
  public static final String timeRange = "timeRange"; // value = CalendarDateRange
  public static final String timeStride = "timeStride"; // value = Integer
  public static final String timePresent = "timePresent"; // value = Boolean
  public static final String timeAll = "timeAll"; // value = Boolean
  public static final String timeWindow = "timeWindow"; // value = CalendarPeriod

  public static final String vertCoord = "vertCoord"; // value = Double or CoordInterval
  public static final String ensCoord = "ensCoord"; // value = Double

  // cant use these for selecting, used for validation
  public static final String timeOffsetDate = "timeOffsetDate"; // value = CalendarDate
  public static final String timeOffsetUnit = "timeOffsetUnit"; // value = CalendarDateUnit

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private final Map<String, Object> req = new HashMap<>();

  public Set<Map.Entry<String, Object>> getEntries() {
    return req.entrySet();
  }

  private Object get(String key) {
    return req.get(key);
  }

  private Double getDouble(String key) {
    Object val = req.get(key);
    if (val == null)
      return null;
    double dval;
    if (val instanceof Number)
      dval = ((Number) val).doubleValue();
    else
      dval = Double.parseDouble((String) val);
    return dval;
  }

  private CalendarDate getCalendarDate(String key) {
    Object val = req.get(key);
    if (val == null)
      return null;
    if (val instanceof CalendarDate) {
      return (CalendarDate) val;
    }
    throw new RuntimeException(key + " not a Calendar Date " + val);
  }

  private CalendarDateRange getCalendarDateRange(String key) {
    Object val = req.get(key);
    if (val == null)
      return null;
    if (val instanceof CalendarDateRange) {
      return (CalendarDateRange) val;
    }
    throw new RuntimeException(key + " not a Calendar Date Range " + val);
  }

  private Integer getInteger(String key) {
    Object val = req.get(key);
    if (val == null)
      return null;
    int dval;
    if (val instanceof Number)
      dval = ((Number) val).intValue();
    else
      dval = Integer.parseInt((String) val);
    return dval;
  }

  private boolean isTrue(String key) {
    Object val = req.get(key);
    return (val instanceof Boolean) && (Boolean) val;
  }

  public boolean hasTimeParam() {
    return get(timeRange) != null || get(time) != null || get(timeStride) != null || get(timePresent) != null
        || get(timeCoord) != null;
  }

  public boolean hasTimeOffsetParam() {
    return get(timeOffset) != null || get(timeOffsetFirst) != null || get(timeOffsetIntv) != null;
  }

  public boolean hasTimeOffsetIntvParam() {
    return get(timeOffsetIntv) != null || get(timeOffsetFirst) != null;
  }

  public GridSubset setHorizStride(int stride) {
    req.put(horizStride, stride);
    return this;
  }

  public Integer getHorizStride() {
    return (Integer) req.get(horizStride);
  }

  public Double getEnsCoord() {
    return getDouble(ensCoord);
  }

  public GridSubset setEnsCoord(Object coord) {
    Preconditions.checkArgument(coord instanceof Double);
    req.put(ensCoord, coord);
    return this;
  }

  public LatLonRect getLatLonBoundingBox() {
    return (LatLonRect) get(latlonBB);
  }

  public GridSubset setLatLonBoundingBox(LatLonRect llbb) {
    req.put(latlonBB, llbb);
    return this;
  }

  public LatLonPoint getLatLonPoint() {
    return (LatLonPoint) get(latlonPoint);
  }

  public GridSubset setLatLonPoint(LatLonPoint pt) {
    req.put(latlonPoint, pt);
    return this;
  }

  public ProjectionRect getProjectionRect() {
    return (ProjectionRect) get(projBB);
  }

  public GridSubset setProjectionRect(ProjectionRect projRect) {
    req.put(projBB, projRect);
    return this;
  }

  public CalendarDate getRunTime() {
    return getCalendarDate(runtime);
  }

  public GridSubset setRunTime(CalendarDate date) {
    req.put(runtime, date);
    return this;
  }

  public GridSubset setRunTimeCoord(Object coord) {
    Preconditions.checkArgument(coord instanceof CalendarDate);
    req.put(runtime, coord);
    return this;
  }

  public Boolean getRunTimeAll() {
    return isTrue(runtimeAll);
  }

  // TODO whats happens for multiple runtimes for timeOffsetRegular?
  public GridSubset setRunTimeAll() {
    req.put(runtimeAll, true);
    return this;
  }

  public Boolean getRunTimeLatest() {
    return isTrue(runtimeLatest);
  }

  public GridSubset setRunTimeLatest() {
    req.put(runtimeLatest, true);
    return this;
  }

  public CalendarDate getTime() {
    return getCalendarDate(time);
  }

  public GridSubset setTime(CalendarDate date) {
    req.put(time, date);
    return this;
  }

  // TODO interval
  public Object getTimeCoord() {
    return get(timeCoord);
  }

  public GridSubset setTimeCoord(Object coord) {
    req.put(timeCoord, coord);
    return this;
  }

  public Boolean getTimePresent() {
    return isTrue(timePresent);
  }

  public GridSubset setTimePresent() {
    req.put(timePresent, true);
    return this;
  }

  public CalendarDateRange getTimeRange() {
    return getCalendarDateRange(timeRange);
  }

  public GridSubset setTimeRange(CalendarDateRange dateRange) {
    req.put(timeRange, dateRange);
    return this;
  }

  public Integer getTimeStride() {
    return getInteger(timeStride);
  }

  public GridSubset setTimeStride(int stride) {
    req.put(timeStride, stride);
    return this;
  }

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
    }
    return this;
  }

  public CoordInterval getTimeOffsetIntv() {
    Object val = req.get(timeOffsetIntv);
    if (val == null)
      return null;
    if (val instanceof CoordInterval) {
      return (CoordInterval) val;
    }
    throw new RuntimeException(timeOffsetIntv + " not a CoordInterval " + val);
  }

  public GridSubset setTimeOffsetIntv(CoordInterval coord) {
    req.put(timeOffsetIntv, coord);
    return this;
  }

  public Boolean getTimeOffsetFirst() {
    return isTrue(timeOffsetFirst);
  }

  public GridSubset setTimeOffsetFirst() {
    req.put(timeOffsetFirst, true);
    return this;
  }

  // TODO interval vs point
  public Object getVertCoord() {
    return get(vertCoord);
  }

  public GridSubset setVertCoord(Object coord) {
    req.put(vertCoord, coord);
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

  //////////////////////////////
  // probably not needed

  public List<String> getStations() {
    return (List<String>) get(stations);
  }

  public GridSubset setStations(List<String> stns) {
    req.put(stations, stns);
    return this;
  }

  public List<String> getVariables() {
    return (List<String>) get(variables);
  }

  public GridSubset setVariables(List<String> vars) {
    req.put(variables, vars);
    return this;
  }
}
