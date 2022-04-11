/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import ucar.ma2.*;
import ucar.nc2.VariableSimpleBuilder;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.util.StringUtil2;

/**
 * Write DSG CF-1.6 file from a Coverage Dataset
 *
 * @author caron
 * @since 7/8/2015
 */
public class CoverageAsPoint {
  // TODO: refactor for less duplicate code
  private static final boolean debug = false;

  private List<VarGroup> varGroups;
  private SubsetParams subset;
  private LatLonPoint latLonPoint;
  private LatLonPoint nearestLatLonPoint;

  private class VarGroup {
    private String name;
    private List<VarData> varData;
    private FeatureType fType;

    private CoverageCoordAxis1D timeAxis;
    private CalendarDateUnit dateUnit;
    private CoverageCoordAxis1D zAxis;
    private String zUnit;

    VarGroup(String name, List<Coverage> wantCovs) throws IOException {
      this.name = name;
      this.varData = new ArrayList<>();
      for (Coverage cov : wantCovs) {
        this.varData.add(new VarData(cov));
      }
      CoverageCoordSys subsetSys = this.varData.get(0).array.getCoordSysForData();

      // single point subset, so only one lat/lon to grab, and this will be the lat/lon
      // closest to the one requested in the subset
      if (nearestLatLonPoint == null) {
        nearestLatLonPoint = subsetSys.getHorizCoordSys().getLatLon(0, 0);
      }

      this.timeAxis = (CoverageCoordAxis1D) subsetSys.getTimeAxis();
      if (this.timeAxis != null) {
        this.dateUnit = this.timeAxis.getCalendarDateUnit();
      }
      this.zAxis = (CoverageCoordAxis1D) subsetSys.getZAxis();
      if (this.zAxis != null) {
        this.zUnit = this.zAxis.getUnits();
      }
      this.fType =
          (this.zAxis == null || this.zAxis.getNcoords() <= 1) ? FeatureType.STATION : FeatureType.STATION_PROFILE;
    }
  }

  private class VarData {
    Coverage cov;
    GeoReferencedArray array;

    VarData(Coverage cov) throws IOException {
      this.cov = cov;
      try {
        this.array = cov.readData(subset);
        if (debug) {
          System.out.printf(" Coverage %s data shape = %s%n", cov.getName(),
              Arrays.toString(array.getData().getShape()));
        }
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }
  }

  public CoverageAsPoint(CoverageCollection gcd, List<String> varNames, SubsetParams subset) throws IOException {
    this.subset = subset;

    this.latLonPoint = (LatLonPoint) subset.get(SubsetParams.latlonPoint);
    if (latLonPoint == null) {
      throw new IllegalArgumentException("No latlon point");
    }

    // build var list, grouped by shared axes
    varGroups = new ArrayList<>();
    for (CoordSysSet css : gcd.getCoverageSets()) {
      List<Coverage> wantCovs =
          css.getCoverages().stream().filter(cov -> varNames.contains(cov.getName())).collect(Collectors.toList());
      if (!wantCovs.isEmpty()) {
        varGroups.add(new VarGroup(css.getCoordSys().getName(), wantCovs));
      }
    }
  }

  public FeatureDatasetPoint asFeatureDatasetPoint() {
    return varGroups.isEmpty() ? null : new CoverageAsFeatureDatasetPoint();
  }

  private class CoverageAsFeatureDatasetPoint extends ucar.nc2.ft.point.PointDatasetImpl {
    CoverageAsFeatureDatasetPoint() {
      super(varGroups.size() > 1 ? FeatureType.ANY_POINT : varGroups.get(0).fType);
      this.collectionList = new ArrayList<>();
      List<VariableSimpleIF> dataVars = new ArrayList<>();
      // each group of vars is its own collection
      for (VarGroup vg : varGroups) {
        this.collectionList.add(new CoverageAsStationFeatureCollection(vg));

        for (VarData vd : vg.varData) {
          Coverage cov = vd.cov;
          VariableSimpleIF simple = new VariableSimpleBuilder(cov.getName(), cov.getDescription(), cov.getUnitsString(),
              cov.getDataType(), cov.getDimensions()).build();
          dataVars.add(simple);
        }
      }
      this.dataVariables = dataVars;
    }
  }

  private class CoverageAsStationFeatureCollection extends StationTimeSeriesCollectionImpl {

    private VarGroup varGroup;

    CoverageAsStationFeatureCollection(VarGroup varGroup) {
      super(varGroup.name + " AsStationFeatureCollection", varGroup.dateUnit, varGroup.zUnit);
      this.varGroup = varGroup;
      this.collectionFeatureType = varGroup.fType;
    }

    @Override
    protected StationHelper createStationHelper() {
      StationHelper helper = new StationHelper();
      String name = String.format("GridPointRequestedAt[%s]", LatLonPoints.toString(latLonPoint, 3));
      name = StringUtil2.replace(name.trim(), ' ', "_");
      helper.addStation(createStationFeature(name));
      return helper;
    }

    private StationFeature createStationFeature(String name) {
      double stationZ = varGroup.zAxis != null ? varGroup.zAxis.getCoordEdgeFirst() : 0.0;
      return this.collectionFeatureType == FeatureType.STATION_PROFILE
          ? new CoverageAsStationProfile(name, name, null, nearestLatLonPoint.getLatitude(),
              nearestLatLonPoint.getLongitude(), stationZ, this.timeUnit, this.altUnits, -1, varGroup)
          : new CoverageAsStationFeature(name, name, null, nearestLatLonPoint.getLatitude(),
              nearestLatLonPoint.getLongitude(), stationZ, this.timeUnit, null, -1, varGroup);
    }
  }

  /**
   * Begin inner classes for Station Profile feature (w/ z dimension) as point
   */
  private class CoverageAsStationProfile extends StationProfileFeatureImpl {
    private VarGroup varGroup;

    public CoverageAsStationProfile(String name, String desc, String wmoId, double lat, double lon, double alt,
        CalendarDateUnit timeUnit, String altUnits, int npts, VarGroup varGroup) {
      super(name, desc, wmoId, lat, lon, alt, timeUnit, altUnits, npts);
      this.varGroup = varGroup;
    }

    ///////////////
    // NO-OPS
    @Override
    public List<CalendarDate> getTimes() {
      return null;
    }

    @Override
    public ProfileFeature getProfileByDate(CalendarDate date) {
      return null;
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return StructureData.EMPTY;
    }

    // end NO-OPS
    /////////////////

    @Override
    public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
      return new TimeseriesProfileIterator();
    }

    @Override
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator() throws IOException {
      return new TimeseriesProfileIterator();
    }

    private class VarIter {
      Coverage cov;
      GeoReferencedArray geoA;
      IndexIterator dataIter;

      VarIter(Coverage cov, GeoReferencedArray array, IndexIterator dataIter) {
        this.cov = cov;
        this.geoA = array;
        this.dataIter = dataIter;
      }
    }

    private class TimeseriesProfileIterator implements PointFeatureCollectionIterator {

      int curr;
      int nvalues;
      List<VarIter> varIters;
      CoverageCoordAxis1D timeAxis;

      TimeseriesProfileIterator() {
        this.timeAxis = varGroup.timeAxis;
        this.nvalues = this.timeAxis != null ? timeAxis.getNcoords() : 1;

        varIters = new ArrayList<>();
        for (VarData vd : varGroup.varData) {
          Array data = vd.array.getData();
          if (debug) {
            System.out.printf("%s shape=%s%n", vd.cov.getName(), Arrays.toString(data.getShape()));
          }
          varIters.add(new VarIter(vd.cov, vd.array, data.getIndexIterator()));
        }
      }

      @Override
      public boolean hasNext() throws IOException {
        return curr < nvalues;
      }

      @Override
      public PointFeatureCollection next() throws IOException {
        double obsTime = this.timeAxis != null ? this.timeAxis.getCoordMidpoint(curr) : 0.0;
        curr++;
        return new CoverageAsProfileFeature(obsTime, varGroup.dateUnit, varGroup.zUnit, getLatitude(), getLongitude(),
            this.varIters);
      }
    }

    private class CoverageAsProfileFeature extends ProfileFeatureImpl {

      List<VarIter> varIters;

      CoverageAsProfileFeature(double obsTime, CalendarDateUnit timeUnit, String altUnits, double lat, double lon,
          List<VarIter> varIters) {
        super("", timeUnit, altUnits, lat, lon, obsTime, -1);
        this.varIters = varIters;
      }

      @Override
      public PointFeatureIterator getPointFeatureIterator() throws IOException {
        return new ProfileFeatureIterator();
      }

      private class ProfilePoint implements EarthLocation {
        private final double lat;
        private final double lon;
        private final double alt;
        private final LatLonPoint latlon;

        public ProfilePoint(double lat, double lon, double alt) {
          this.lat = lat;
          this.lon = lon;
          this.alt = alt;
          this.latlon = LatLonPoint.create(lat, lon);
        }

        @Override
        public double getLatitude() {
          return lat;
        }

        @Override
        public double getLongitude() {
          return lon;
        }

        @Override
        public double getAltitude() {
          return alt;
        }

        @Override
        public LatLonPoint getLatLon() {
          return latlon;
        }

        @Override
        public boolean isMissing() {
          return false;
        }
      }

      private class ProfileFeatureIterator extends PointIteratorAbstract {
        int curr;
        int nvalues;

        ProfileFeatureIterator() {
          this.nvalues = varGroup.zAxis.getNcoords();
        }

        @Override
        public boolean hasNext() {
          boolean more = curr < nvalues;
          if (!more) {
            close();
          }
          return more;
        }

        @Override
        public PointFeature next() {
          double alt = varGroup.zAxis.getCoordMidpoint(curr);

          StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
          for (VarIter vi : varIters) {
            smb.addMemberScalar(vi.cov.getName(), null, null, vi.cov.getDataType(),
                (Number) vi.dataIter.getObjectNext());
          }
          StructureData coords = new StructureDataFromMember(smb.build());
          curr++;
          EarthLocation location = new ProfilePoint(CoverageAsStationProfile.this.getLatitude(),
              CoverageAsStationProfile.this.getLongitude(), alt);
          PointFeature pf = new CoverageAsStationProfile.CoverageAsPointFeature(CoverageAsStationProfile.this, time,
              0.0, timeUnit, location, coords);
          calcBounds(pf);
          return pf;
        }

        @Override
        public void close() {
          finishCalcBounds();
        }
      }

      @Nonnull
      @Override
      public CalendarDate getTime() {
        return timeUnit != null ? timeUnit.makeCalendarDate(time) : CalendarDate.UNKNOWN;
      }

      @Nonnull
      @Override
      public StructureData getFeatureData() throws IOException {
        return StructureData.EMPTY;
      }
    }

    private class CoverageAsPointFeature extends PointFeatureImpl implements StationPointFeature {
      StationFeature stn;
      StructureData sdata;

      CoverageAsPointFeature(StationFeature stn, double obsTime, double nomTime, CalendarDateUnit timeUnit,
          EarthLocation location, StructureData sdata) {
        super(CoverageAsStationProfile.this, stn, obsTime, nomTime, timeUnit);
        this.stn = stn;
        this.sdata = sdata;
        this.location = location;
      }

      @Override
      @Nonnull
      public StationFeature getStation() {
        return stn;
      }

      @Override
      @Nonnull
      public StructureData getFeatureData() {
        return sdata;
      }

      @Override
      @Nonnull
      public StructureData getDataAll() {
        return sdata;
      }
    }
  }

  /**
   * Begin inner classes for Station feature (no z) as point
   */

  private class CoverageAsStationFeature extends StationTimeSeriesFeatureImpl {

    private VarGroup varGroup;

    CoverageAsStationFeature(String name, String desc, String wmoId, double lat, double lon, double alt,
        CalendarDateUnit timeUnit, String altUnits, int npts, VarGroup varGroup) {
      // String name, String desc, String wmoId, double lat, double lon, double alt, DateUnit timeUnit, String altUnits,
      // int npts
      super(name, desc, wmoId, lat, lon, alt, timeUnit, altUnits, npts, StructureData.EMPTY);
      this.varGroup = varGroup;
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() {
      return StructureData.EMPTY;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator() {
      return new TimeseriesIterator();
    }

    private class VarIter {
      Coverage cov;
      GeoReferencedArray geoA;
      IndexIterator dataIter;

      VarIter(Coverage cov, GeoReferencedArray array, IndexIterator dataIter) {
        this.cov = cov;
        this.geoA = array;
        this.dataIter = dataIter;
      }
    }
    private class TimeseriesIterator extends PointIteratorAbstract {
      int curr;
      int nvalues;
      List<VarIter> varIters;
      CoverageCoordAxis1D timeAxis;

      TimeseriesIterator() {
        this.timeAxis = varGroup.timeAxis;
        this.nvalues = timeAxis != null ? timeAxis.getNcoords() : 1;

        varIters = new ArrayList<>();
        for (VarData vd : varGroup.varData) {
          Array data = vd.array.getData();
          if (debug) {
            System.out.printf("%s shape=%s%n", vd.cov.getName(), Arrays.toString(data.getShape()));
          }
          varIters.add(new VarIter(vd.cov, vd.array, data.getIndexIterator()));
        }
      }

      @Override
      public boolean hasNext() {
        boolean more = curr < nvalues;
        if (!more) {
          close();
        }
        return more;
      }

      @Override
      public PointFeature next() {
        double obsTime = this.timeAxis != null ? this.timeAxis.getCoordMidpoint(curr) : 0.0;

        StructureMembers.Builder smb = StructureMembers.builder().setName("Coords");
        for (VarIter vi : varIters) {
          smb.addMemberScalar(vi.cov.getName(), null, null, vi.cov.getDataType(), (Number) vi.dataIter.getObjectNext());
        }
        StructureData coords = new StructureDataFromMember(smb.build());
        curr++;
        PointFeature pf = new CoverageAsPointFeature(CoverageAsStationFeature.this, obsTime, 0.0, timeUnit, coords);
        calcBounds(pf);
        return pf;
      }

      @Override
      public void close() {
        finishCalcBounds();
      }
    }

    private class CoverageAsPointFeature extends PointFeatureImpl implements StationPointFeature {
      StationFeature stn;
      StructureData sdata;

      CoverageAsPointFeature(StationFeature stn, double obsTime, double nomTime, CalendarDateUnit timeUnit,
          StructureData sdata) {
        super(CoverageAsStationFeature.this, stn, obsTime, nomTime, timeUnit);
        this.stn = stn;
        this.sdata = sdata;
      }

      @Override
      @Nonnull
      public StationFeature getStation() {
        return stn;
      }

      @Override
      @Nonnull
      public StructureData getFeatureData() {
        return sdata;
      }

      @Override
      @Nonnull
      public StructureData getDataAll() {
        return sdata;
      }
    }
  }
}
