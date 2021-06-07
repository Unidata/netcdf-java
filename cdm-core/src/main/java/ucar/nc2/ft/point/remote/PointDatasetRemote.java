/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.ft.remote.CdmrFeatureDataset;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;
import java.util.ArrayList;
import java.util.List;

/**
 * Client view of a CdmRemote Point Dataset.
 *
 * @author caron
 * @since Feb 16, 2009
 */
public class PointDatasetRemote extends PointDatasetImpl {

  public PointDatasetRemote(FeatureType wantFeatureType, String uri, CalendarDateUnit timeUnit, String altUnits,
      List<VariableSimpleIF> vars, LatLonRect bb, CalendarDateRange dr) {

    super(wantFeatureType);
    setBoundingBox(bb);
    setDateRange(dr);
    setLocationURI(CdmrFeatureDataset.SCHEME + uri);

    dataVariables = new ArrayList<>(vars);

    collectionList = new ArrayList<>(1);
    switch (wantFeatureType) {
      case POINT:
        collectionList.add(new PointCollectionStreamRemote(uri, timeUnit, altUnits, null));
        break;
      case STATION:
        collectionList.add(new StationCollectionStream(uri, timeUnit, altUnits));
        break;
      default:
        throw new UnsupportedOperationException("No implementation for " + wantFeatureType);
    }
  }

  static String makeQuery(String station, LatLonRect boundingBox, CalendarDateRange dateRange) {
    StringBuilder query = new StringBuilder();
    boolean needamp = false;

    if (station != null) {
      query.append(station);
      needamp = true;
    }

    if (boundingBox != null) {
      if (needamp)
        query.append("&");
      query.append("west=");
      query.append(boundingBox.getLonMin());
      query.append("&east=");
      query.append(boundingBox.getLonMax());
      query.append("&south=");
      query.append(boundingBox.getLatMin());
      query.append("&north=");
      query.append(boundingBox.getLatMax());
      needamp = true;
    }

    if (dateRange != null) {
      if (needamp)
        query.append("&");
      query.append("time_start=");
      query.append(dateRange.getStart());
      query.append("&time_end=");
      query.append(dateRange.getEnd());
    }

    if (!needamp)
      query.append("all");
    return query.toString();
  }
}
