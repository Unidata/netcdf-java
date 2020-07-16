/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.util.stream.Collectors;
import javax.annotation.Nullable;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Station Collections.
 * This assumes that calling getData( Station s) is cheap, ie that theres no cheaper filtering to do.
 * 
 * @author caron
 * @since Feb 5, 2008
 */
public class StationHelper {
  private List<StationFeature> stations;
  private Map<String, StationFeature> stationHash;
  private static final boolean debug = false;

  public StationHelper() {
    stations = new ArrayList<>();
    stationHash = new HashMap<>();
  }

  public void addStation(StationFeature s) {
    stations.add(s);
    stationHash.put(s.getStation().getName(), s);
  }

  public void setStations(List<StationFeature> nstations) {
    stations = new ArrayList<>();
    stationHash = new HashMap<>();
    for (StationFeature s : nstations)
      addStation(s);
  }

  private LatLonRect rect;

  @Nullable
  public LatLonRect getBoundingBox() {
    // lazy evaluation
    if (rect == null) {
      if (stations.isEmpty())
        return null;

      Station s = stations.get(0).getStation();
      LatLonRect.Builder builder = new LatLonRect.Builder(s.getLatitude(), s.getLongitude());
      if (debug)
        System.out.println("start=" + s.getLatitude() + " " + s.getLongitude() + " rect= " + rect.toString2());

      for (int i = 1; i < stations.size(); i++) {
        s = stations.get(i).getStation();
        builder.extend(LatLonPoint.create(s.getLatitude(), s.getLongitude()));
        if (debug)
          System.out.println("add=" + s.getLatitude() + " " + s.getLongitude() + " rect= " + rect.toString2());
      }

      // call it global when width > 350 and crossing the seam
      // slightly expand the bounding box
      builder = builder.extendToAllLongitudes(350.0).expand(.0005);
      rect = builder.build();
    }

    return rect;
  }

  public List<Station> getStations(LatLonRect boundingBox) {
    if (boundingBox == null)
      return getStations();

    List<Station> result = new ArrayList<>();
    for (StationFeature sf : stations) {
      Station s = sf.getStation();
      LatLonPoint latlonPt = LatLonPoint.create(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt))
        result.add(s);
    }
    return result;
  }

  public List<StationFeature> getStationFeatures(LatLonRect boundingBox) {
    if (boundingBox == null)
      return stations;

    List<StationFeature> result = new ArrayList<>();
    for (StationFeature sf : stations) {
      Station s = sf.getStation();
      LatLonPoint latlonPt = LatLonPoint.create(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt))
        result.add(sf);
    }
    return result;
  }

  public StationFeature getStation(String name) {
    return stationHash.get(name);
  }

  public List<StationFeature> getStationFeatures() {
    return stations;
  }

  public List<Station> getStations() {
    return stations.stream().map(sf -> sf.getStation()).collect(Collectors.toList());
  }

  public List<StationFeature> getStationFeaturesFromNames(List<String> stnNames) {
    List<StationFeature> result = new ArrayList<>(stnNames.size());
    for (String ss : stnNames) {
      StationFeature s = stationHash.get(ss);
      if (s != null)
        result.add(s);
    }
    return result;
  }

  public List<StationFeature> getStationFeatures(List<Station> stations) {
    List<StationFeature> result = new ArrayList<>(stations.size());
    for (Station s : stations) {
      StationFeature ss = stationHash.get(s.getName());
      if (ss != null)
        result.add(ss);
    }
    return result;
  }

  public StationFeature getStationFeature(Station stn) {
    return stationHash.get(stn.getName());
  }

  public List<Station> getStations(List<String> stnNames) {
    List<Station> result = new ArrayList<>(stnNames.size());
    for (String ss : stnNames) {
      StationFeature sf = stationHash.get(ss);
      if (sf != null)
        result.add(sf.getStation());
    }
    return result;
  }

  public StationHelper subset(LatLonRect bb) {
    StationHelper result = new StationHelper();
    result.setStations(getStationFeatures(bb));
    return result;
  }

  public StationHelper subset(List<StationFeature> stns) {
    StationHelper result = new StationHelper();
    result.setStations(stns);
    return result;
  }

}

