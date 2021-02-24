/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import javax.annotation.Nonnull;
import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.Station;

/**
 * Superclass for implementations of StationFeature: time series of data at a point
 * Concrete subclass must implement getFeatureData() and getPointFeatureIterator();
 *
 * @author caron
 */

public abstract class StationTimeSeriesFeatureImpl extends PointCollectionImpl implements StationTimeSeriesFeature {
  protected StationFeature stationFeature;

  public StationTimeSeriesFeatureImpl(String name, String desc, String wmoId, double lat, double lon, double alt,
      CalendarDateUnit timeUnit, String altUnits, int npts, StructureData sdata) {
    super(name, timeUnit, altUnits);
    stationFeature = new StationFeatureImpl(name, desc, wmoId, lat, lon, alt, npts, sdata);
  }

  public StationTimeSeriesFeatureImpl(StationFeature s, CalendarDateUnit timeUnit, String altUnits, int nfeatures) {
    super(s.getStation().getName(), timeUnit, altUnits);
    this.stationFeature = s;
    if (nfeatures >= 0) {
      getInfo(); // create the object
      info.nfeatures = nfeatures;
    }
  }

  public Station getStation() {
    return stationFeature.getStation();
  }

  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.STATION;
  }

  @Override
  public String toString() {
    return "StationFeatureImpl{" + "s=" + stationFeature + '}';
  }

  @Override
  public StationTimeSeriesFeature subset(CalendarDateRange dateRange) {
    if (dateRange == null)
      return this;
    return new StationFeatureSubset(this, dateRange);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StationTimeSeriesFeatureImpl)) {
      return false;
    }

    StationTimeSeriesFeatureImpl that = (StationTimeSeriesFeatureImpl) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public static class StationFeatureSubset extends StationTimeSeriesFeatureImpl {
    private final StationTimeSeriesFeatureImpl from;
    private final CalendarDateRange filter_date;

    public StationFeatureSubset(StationTimeSeriesFeatureImpl from, CalendarDateRange filter_date) {
      super(from.stationFeature, from.getTimeUnit(), from.getAltUnits(), -1);
      this.from = from;
      this.filter_date = filter_date;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      return new PointIteratorFiltered(from.getPointFeatureIterator(), null, filter_date);
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return from.getFeatureData();
    }
  }
}
