/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.ft.IOIterator;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * Abstract superclass for implementations of StationProfileFeature.
 * Subclass must implement getPointFeatureCollectionIterator();
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class StationProfileFeatureImpl extends PointFeatureCCImpl implements StationProfileFeature {
  protected int timeSeriesNpts;
  protected StationFeature stationFeature;
  protected PointFeatureCollectionIterator localIterator;

  public StationProfileFeatureImpl(StationFeature s, CalendarDateUnit timeUnit, String altUnits, int npts) {
    super(s.getStation().getName(), timeUnit, altUnits, FeatureType.STATION_PROFILE);
    this.stationFeature = s;
    this.timeSeriesNpts = npts;
  }

  @Override
  public int getNobs() {
    return this.timeSeriesNpts;
  }

  @Override
  public Station getStation() {
    return stationFeature.getStation();
  }

  @Override
  public int size() {
    return timeSeriesNpts;
  }

  // @Override
  public StationProfileFeature subset(LatLonRect boundingBox) {
    return this; // only one station - we could check if its in the bb
  }

  @Override
  public StationProfileFeature subset(CalendarDateRange dateRange) {
    return new StationProfileFeatureSubset(this, dateRange);
  }

  public static class StationProfileFeatureSubset extends StationProfileFeatureImpl {
    private final StationProfileFeature from;
    private final CalendarDateRange dateRange;

    public StationProfileFeatureSubset(StationProfileFeatureImpl from, CalendarDateRange filter_date) {
      super(from.stationFeature, from.getTimeUnit(), from.getAltUnits(), -1);
      this.from = from;
      this.dateRange = filter_date;
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return from.getFeatureData();
    }

    @Override
    public List<CalendarDate> getTimes() {
      List<CalendarDate> result = new ArrayList<>();
      for (ProfileFeature pf : this) {
        if (dateRange.includes(pf.getTime()))
          result.add(pf.getTime());
      }
      return result;
    }

    @Override
    public ProfileFeature getProfileByDate(CalendarDate date) throws IOException {
      return from.getProfileByDate(date);
    }

    @Override // new way
    public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
      return new PointCollectionIteratorFiltered(from.getPointFeatureCollectionIterator(), new DateFilter());
    }

    @Override // old way
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
      return new PointCollectionIteratorFiltered(from.getPointFeatureCollectionIterator(), new DateFilter());
    }

    private class DateFilter implements PointFeatureCollectionIterator.Filter {
      @Override
      public boolean filter(PointFeatureCollection pointFeatureCollection) {
        ProfileFeature profileFeature = (ProfileFeature) pointFeatureCollection;
        return dateRange.includes(profileFeature.getTime());
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<ProfileFeature> iterator() {
    return new ProfileFeatureIterator();
  }

  private class ProfileFeatureIterator implements Iterator<ProfileFeature> {
    PointFeatureCollectionIterator pfIterator;

    public ProfileFeatureIterator() {
      try {
        this.pfIterator = getPointFeatureCollectionIterator();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean hasNext() {
      try {
        return pfIterator.hasNext();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ProfileFeature next() {
      try {
        return (ProfileFeature) pfIterator.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }


  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated


  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null)
      resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public ProfileFeature next() throws IOException {
    return (ProfileFeature) localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getPointFeatureCollectionIterator();
  }

}
