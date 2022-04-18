/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import java.util.List;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * @author mhermida
 */
public final class CFPointWriterUtils {

  private CFPointWriterUtils() {}

  public static LatLonRect getBoundingBox(List<? extends Station> stnList) {
    Station s = stnList.get(0);
    LatLonPoint llpt = LatLonPoint.create(s.getLatitude(), s.getLongitude());
    LatLonRect rect = new LatLonRect(llpt, 0, 0);

    for (int i = 1; i < stnList.size(); i++) {
      s = stnList.get(i);
      rect.extend(LatLonPoint.create(s.getLatitude(), s.getLongitude()));
    }

    // To give a little "wiggle room", we're going to slightly expand the bounding box.
    double newLowerLeftLat = rect.getLowerLeftPoint().getLatitude() - .0005;
    double newLowerLeftLon = rect.getLowerLeftPoint().getLongitude() - .0005;
    LatLonPoint newLowerLeftPoint = LatLonPoint.create(newLowerLeftLat, newLowerLeftLon);

    double newUpperRightLat = rect.getUpperRightPoint().getLatitude() + .0005;
    double newUpperRightLon = rect.getUpperRightPoint().getLongitude() + .0005;
    LatLonPoint newUpperRightPoint = LatLonPoint.create(newUpperRightLat, newUpperRightLon);

    rect.extend(newLowerLeftPoint);
    rect.extend(newUpperRightPoint);

    return rect;
  }
}
