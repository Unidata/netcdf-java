package ucar.nc2.ft;

import ucar.nc2.ft.point.StationFeature;

import java.util.List;

public interface StationFeatureCollection {

  List<StationFeature> getStationFeatures();

  List<StationFeature> getStationFeatures(List<String> stnNames);

  List<StationFeature> getStationFeatures(ucar.unidata.geoloc.LatLonRect boundingBox);

  StationFeature findStationFeature(String name);
}
