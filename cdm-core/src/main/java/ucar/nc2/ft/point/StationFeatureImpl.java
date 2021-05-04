/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.StructureData;
import ucar.unidata.geoloc.Station;

/**
 * Implement StationFeature
 *
 * @author caron
 * @since 7/8/2014
 */
@Immutable
public class StationFeatureImpl implements StationFeature {
  private final Station station;
  private final StructureData sdata;

  public StationFeatureImpl(String name, String desc, String wmoId, double lat, double lon, double alt, int nobs,
      StructureData sdata) {
    this.station = new Station(name, desc, wmoId, lat, lon, alt, nobs);
    this.sdata = sdata;
  }

  public StationFeatureImpl(StationFeature from) throws IOException {
    this.station = from.getStation();
    this.sdata = from.getFeatureData();
  }

  @Override
  public Station getStation() {
    return station;
  }

  @Override
  public StructureData getFeatureData() {
    return sdata;
  }
}
