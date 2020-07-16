package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.DirectPositionType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import java.util.Arrays;
import ucar.unidata.geoloc.Station;

/**
 * Created by cwardgar on 2014/02/28.
 */
public abstract class NcDirectPositionType {
  // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/wml2:MonitoringPoint/sams:shape/
  // gml:Point/gml:pos
  public static DirectPositionType initPos(DirectPositionType pos, StationTimeSeriesFeature stationFeat) {
    Station stn = stationFeat.getStation();
    // TEXT
    pos.setListValue(Arrays.asList(stn.getLatitude(), stn.getLongitude(), stn.getAltitude()));

    return pos;
  }

  private NcDirectPositionType() {}
}
