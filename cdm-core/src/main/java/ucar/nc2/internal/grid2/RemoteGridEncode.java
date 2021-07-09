package ucar.nc2.internal.grid2;

import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.grid.GridSubset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Static uttilities to encode/decode data requests between String and GridSubset. */
// LOOK doesnt belong here, is it used?
public class RemoteGridEncode {

  public static String encodeDataRequest(GridSubset subset, String varname) {
    Formatter f = new Formatter();
    Escaper urlParamEscaper = UrlEscapers.urlFormParameterEscaper();
    f.format("var=%s", urlParamEscaper.escape(varname));

    for (Map.Entry<String, Object> entry : subset.getEntries()) {
      switch (entry.getKey()) {
        case GridSubset.latlonBB:
          LatLonRect llbb = (LatLonRect) entry.getValue();
          f.format("&north=%s&south=%s&west=%s&east=%s", llbb.getLatMax(), llbb.getLatMin(), llbb.getLonMin(),
              llbb.getLonMax());
          break;
        case GridSubset.projBB:
          ProjectionRect projRect = (ProjectionRect) entry.getValue();
          f.format("&minx=%s&miny=%s&maxx=%s&maxy=%s", projRect.getMinX(), projRect.getMinY(), projRect.getMaxX(),
              projRect.getMaxY());
          break;
        case GridSubset.latlonPoint:
          LatLonPoint llPoint = (LatLonPoint) entry.getValue();
          f.format("&lat=%s&lon=%s", llPoint.getLatitude(), llPoint.getLongitude());
          break;
        case GridSubset.horizStride:
          f.format("&%s=%s", GridSubset.horizStride, entry.getValue());
          break;

        case GridSubset.runtime:
          f.format("&runtime=%s", entry.getValue()); // TODO encoded?
          break;
        case GridSubset.runtimeLatest:
          f.format("&runtime=latest");
          break;
        case GridSubset.runtimeAll:
          f.format("&runtime=all");
          break;

        case GridSubset.timeRange:
          CalendarDateRange timeRange = (CalendarDateRange) entry.getValue();
          f.format("&time_start=%s&time_end=%s", timeRange.getStart(), timeRange.getEnd());
          break;
        case GridSubset.timePresent:
          f.format("&time=present");
          break;
        case GridSubset.timeAll:
          f.format("&time=all");
          break;

        case GridSubset.timeOffsetAll:
          f.format("&timeOffset=all");
          break;
        case GridSubset.timeOffsetFirst:
          f.format("&timeOffset=first");
          break;

        case GridSubset.gridName:
          break;

        default:
          f.format("&%s=%s", entry.getKey(), entry.getValue());
          break;
      }
    }
    return f.toString();
  }

  public static GridSubset decodeDataRequest(String request) {
    Map<String, String> keyValueMap = new HashMap<>();
    Splitter splitRequest = Splitter.on('&');
    Splitter splitPair = Splitter.on('=');
    for (String pair : splitRequest.split(request)) {
      List<String> keyvalue = splitPair.splitToList(pair);
      if (keyvalue.size() == 2) {
        keyValueMap.put(keyvalue.get(0), keyvalue.get(1));
      } else if (keyvalue.size() == 1) {
        keyValueMap.put(keyvalue.get(0), null);
      }
    }
    GridSubset subset = GridSubset.create();

    // horiz subsetting
    List<Double> params = getParameters(keyValueMap, "north", "south", "east", "west");
    if (params != null) {
      LatLonRect llbb = LatLonRect.builder(params.get(1), params.get(3), params.get(0), params.get(2)).build();
      subset.setLatLonBoundingBox(llbb);
    }
    params = getParameters(keyValueMap, "minx", "miny", "maxx", "maxy");
    if (params != null) {
      ProjectionRect rect = ProjectionRect.builder(params.get(0), params.get(1), params.get(2), params.get(3)).build();
      subset.setProjectionBoundingBox(rect);
    }
    params = getParameters(keyValueMap, "lat", "lon");
    if (params != null) {
      LatLonPoint llpt = LatLonPoint.create(params.get(0), params.get(1));
      subset.setLatLonPoint(llpt);
    }
    params = getParameters(keyValueMap, GridSubset.horizStride);
    if (params != null) {
      subset.setHorizStride(params.get(0).intValue());
    }

    // runtime
    String svalue = keyValueMap.get("runtime");
    if (svalue != null) {
      if (svalue.equals("latest")) {
        subset.setRunTimeLatest();
      } else if (svalue.equals("all")) {
        subset.setRunTimeAll();
      } else {
        CalendarDate cd;
        try {
          cd = CalendarDate.fromUdunitIsoDate(null, svalue).orElseThrow(); // TODO calendar?
          subset.setRunTime(cd);
        } catch (Exception e) {
          // fall through
        }
      }
    }

    // TODO should we keep this?
    String varname = keyValueMap.get("var");
    if (varname != null) {
      // subset.setVariable(varname);
    }


    return subset;
  }

  private static List<Double> getParameters(Map<String, String> keyValueMap, String... keys) {
    ArrayList<Double> result = new ArrayList<>();
    for (String key : keys) {
      String svalue = keyValueMap.get(key);
      if (svalue == null) {
        return null;
      }
      double value;
      try {
        value = Double.parseDouble(svalue);
      } catch (Exception e) {
        return null;
      }
      result.add(value);
    }
    return result;
  }

  private static CalendarDate getCalendarDate(Map<String, String> keyValueMap, String key) {
    String svalue = keyValueMap.get(key);
    if (svalue == null) {
      return null;
    }
    return CalendarDate.fromUdunitIsoDate(null, svalue).orElse(null); // TODO calendar?
  }
}
