package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.StringOrRefType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.unidata.geoloc.Station;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NcStringOrRefType {
  // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/wml2:MonitoringPoint/
  // gml:description
  public static StringOrRefType initDescription(StringOrRefType description, StationTimeSeriesFeature stationFeat) {
    Station stn = stationFeat.getStation();
    // TEXT
    description.setStringValue(stn.getDescription());
    return description;
  }

  private NcStringOrRefType() {}
}
