package ucar.nc2.internal.grid;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.Formatter;
import java.util.List;
import java.util.Map;

public class CdmrEncode {

  public static String encodeForCdmrfDataRequest(GridSubset subset, String varname) {
    Formatter f = new Formatter();
    Escaper urlParamEscaper = UrlEscapers.urlFormParameterEscaper();
    f.format("req=data&var=%s", urlParamEscaper.escape(varname));

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
        case GridSubset.runtimeLatest:
          f.format("&runtime=latest");
          break;
        case GridSubset.runtimeAll:
          f.format("&runtime=all");
          break;
        case GridSubset.timeOffsetAll:
          f.format("&timeOffset=all");
          break;
        case GridSubset.timeOffsetFirst:
          f.format("&timeOffset=first");
          break;
        default:
          f.format("&%s=%s", entry.getKey(), entry.getValue());
          break;
      }
    }
    return f.toString();
  }
}
