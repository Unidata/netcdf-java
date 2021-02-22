/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import java.util.List;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * @author mhermida
 */
public final class CFPointWriterUtils {

  private CFPointWriterUtils() {}

  public static LatLonRect getBoundingBox(List<? extends Station> stnList) {
    Station s = stnList.get(0);
    LatLonRect.Builder builder = new LatLonRect.Builder(s.getLatitude(), s.getLongitude());

    for (int i = 1; i < stnList.size(); i++) {
      s = stnList.get(i);
      builder.extend(s.getLatitude(), s.getLongitude());
    }

    // slightly expand the bounding box
    builder = builder.expand(.0005);
    return builder.build();
  }
}
