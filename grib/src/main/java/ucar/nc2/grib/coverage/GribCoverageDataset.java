/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coverage;

import com.google.common.collect.Lists;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateEns;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.coord.CoordinateTimeIntv;
import ucar.nc2.grib.coord.CoordinateVert;
import ucar.nc2.grib.coord.EnsCoordValue;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.grib.coord.VertCoordValue;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time2.Calendar;
import ucar.nc2.time2.CalendarDateRange;
import ucar.nc2.time2.CalendarPeriod;
import ucar.nc2.units.SimpleUnit;
import java.util.Optional;
import ucar.unidata.io.RandomAccessFile;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Create a FeatureDatasetCoverage from a GribCollection file.
 * Called from InvDatasetFcGrib and (by reflection) from CoverageDatasetFactory
 * LOOK might want to switch to Builder pattern
 * 
 * @deprecated FeatureDatasets will move to legacy in ver7, this class will not be available here.
 */
@Deprecated
@Immutable
public class GribCoverageDataset implements CoverageReader, CoordAxisReader {
  private static final Logger logger = LoggerFactory.getLogger(GribCoverageDataset.class);

  /**
   * Open GribCoverageDataset as a FeatureDatasetCoverage.
   * 
   * @param errLog if is grib but error, add error message to this log.
   * @return empty if not a GribCoverageDataset or on error.
   */
  public static Optional<FeatureDatasetCoverage> open(String endpoint, Formatter errLog) {
    GribCollectionImmutable gc;

    if (endpoint.startsWith("file:"))
      endpoint = endpoint.substring("file:".length());

    // try to fail fast
    RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(endpoint, "r");
      gc = GribCdmIndex.openGribCollectionFromRaf(raf, new FeatureCollectionConfig(), CollectionUpdateType.nocheck,
          logger);

      if (gc == null) {
        raf.close();
        return Optional.empty();
      }

      List<CoverageCollection> datasets = new ArrayList<>();
      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        for (GribCollectionImmutable.GroupGC group : ds.getGroups()) {
          GribCoverageDataset gribCov = new GribCoverageDataset(gc, ds, group);
          datasets.add(gribCov.createCoverageCollection());
        }
      }
      FeatureDatasetCoverage result = new FeatureDatasetCoverage(endpoint, gc.getGlobalAttributes(), gc, datasets);
      return Optional.of(result);

    } catch (Throwable t) {
      logger.error("GribCoverageDataset.open failed", t);
      errLog.format("%s", t.getMessage());
      return Optional.empty();
    }
  }

  //////////////////////////////////////////////////////////////////

  private final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.Dataset ds;
  private final GribCollectionImmutable.GroupGC group;
  private final FeatureType coverageType;
  private final boolean isLatLon;
  private final boolean isCurvilinearOrthogonal;

  public GribCoverageDataset(GribCollectionImmutable gribCollection, GribCollectionImmutable.Dataset ds,
      GribCollectionImmutable.GroupGC group) {
    this.gribCollection = gribCollection;
    this.ds = (ds != null) ? ds : gribCollection.getDataset(0);
    this.group = (group != null) ? group : this.ds.getGroup(0);
    boolean isGrib1 = gribCollection.isGrib1;

    GdsHorizCoordSys hcs = this.group.getGdsHorizCoordSys();
    this.isLatLon = hcs.isLatLon(); // isGrib1 ? hcs.isLatLon() : Grib2Utils.isLatLon(hcs.template,
                                    // gribCollection.getCenter());
    this.isCurvilinearOrthogonal =
        !isGrib1 && Grib2Utils.isCurvilinearOrthogonal(hcs.template, gribCollection.getCenter());

    // figure out coverageType
    FeatureType ct;
    switch (this.ds.getType()) {
      case MRC:
        // case MRSTC:
        // case MRSTP:
      case TwoD:
        ct = FeatureType.FMRC;
        break;
      default:
        ct = FeatureType.GRID;
    }
    if (isCurvilinearOrthogonal)
      ct = FeatureType.CURVILINEAR;
    this.coverageType = ct;
  }

  @Override
  public void close() throws IOException {
    gribCollection.close();
  }

  @Override
  public String getLocation() {
    return gribCollection.getLocation() + "#" + group.getId(); // ??
  }

  public CoverageCollection createCoverageCollection() {
    String name = gribCollection.getName() + "#" + ds.getType();
    if (ds.getGroupsSize() > 1)
      name += "-" + group.getId();

    AttributeContainer gatts = gribCollection.getGlobalAttributes();

    // make horiz transform if needed
    List<CoverageTransform> transforms = new ArrayList<>();

    AttributeContainer projAtts = group.getGdsHorizCoordSys().proj.getProjectionAttributes();
    CoverageTransform projTransform = new CoverageTransform(group.horizCoordSys.getId(), projAtts, true);

    transforms.add(projTransform);

    // potential variables - need to remove any 2D LatLon
    List<GribCollectionImmutable.VariableIndex> vars = new ArrayList<>(group.getVariables());

    List<CoverageCoordAxis> axes = new ArrayList<>();
    if (isCurvilinearOrthogonal)
      axes.addAll(makeHorizCoordinates2D(vars));
    else
      axes.addAll(makeHorizCoordinates());

    /*
     * runtime smooshing
     * for (Coordinate axis : group.getCoordinates()) {
     * switch (axis.getType()) {
     * case runtime:
     * runtimes.add(new RuntimeSmoosher((CoverageCoordAxis1D) axis));
     * break;
     * }
     * }
     */

    Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap = new HashMap<>(); // track which coverageAxis is usd by
                                                                              // which GribCoord

    for (Coordinate axis : group.getCoordinates()) {
      switch (axis.getType()) {
        case runtime:
          // runtime coord added by time coord as needed
          break;

        case time2D:
          coord2axisMap.put(axis, makeTime2DCoordinates((CoordinateTime2D) axis));
          break;

        case time:
        case timeIntv:
          coord2axisMap.put(axis, makeTimeCoordinates((CoordinateTimeAbstract) axis));
          break;

        case vert:
          CoverageCoordAxis covAxisVert = makeCoordAxis((CoordinateVert) axis);
          coord2axisMap.put(axis, Lists.newArrayList(covAxisVert));
          break;

        case ens:
          CoverageCoordAxis covAxisEns = makeCoordAxis((CoordinateEns) axis);
          coord2axisMap.put(axis, Lists.newArrayList(covAxisEns));
          break;
      }
    }
    // makeRuntimeCoordAxes(axes);
    // makeTime2DCoordAxis(axes);
    for (Coordinate coord : coord2axisMap.keySet()) {
      for (CoverageCoordAxis covCoord : coord2axisMap.get(coord))
        if (!alreadyHave(axes, covCoord.getName()))
          axes.add(covCoord);
    }

    // make coord systems
    Map<String, CoverageCoordSys> coordSysSet = new HashMap<>();
    for (GribCollectionImmutable.VariableIndex v : vars) {
      CoverageCoordSys sys = makeCoordSys(v, transforms, coord2axisMap);
      coordSysSet.put(sys.getName(), sys); // duplicates get eliminated here
    }
    List<CoverageCoordSys> coordSys = new ArrayList<>(coordSysSet.values());

    // all vars that are left are coverages
    List<Coverage> pgrids = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex v : vars) {
      pgrids.add(makeCoverage(v, coord2axisMap));
    }
    // List<Coverage> pgrids = vars.stream().map(this::makeCoverage).collect(Collectors.toList());

    // put it together
    return new CoverageCollection(name, coverageType, gatts, null, null, // let cc calculate bbox
        getCalendarDateRange(), coordSys, transforms, axes, pgrids, this);
  }

  /////////////
  private CalendarDateRange dateRange;

  private void trackDateRange(CalendarDateRange cdr) {
    if (dateRange == null)
      dateRange = cdr;
    else
      dateRange = dateRange.extend(cdr);
  }

  private CalendarDateRange getCalendarDateRange() {
    return dateRange;
  }
  /////////////////

  private List<CoverageCoordAxis> makeHorizCoordinates() {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();

    List<CoverageCoordAxis> result = new ArrayList<>(2);
    if (isLatLon) {
      AttributeContainerMutable atts = new AttributeContainerMutable(CF.LATITUDE);
      atts.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));

      double[] values = null;
      CoverageCoordAxis.Spacing spacing = CoverageCoordAxis.Spacing.regularPoint;
      Array glats = hcs.getGaussianLats();
      if (glats != null) {
        spacing = CoverageCoordAxis.Spacing.irregularPoint;
        values = (double[]) glats.get1DJavaArray(DataType.DOUBLE);
        atts.addAttribute(new Attribute(CDM.GAUSSIAN, "true"));
      }

      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder(CF.LATITUDE, CDM.LAT_UNITS, null, DataType.FLOAT,
          AxisType.Lat, atts, CoverageCoordAxis.DependenceType.independent, null, spacing, hcs.ny, hcs.getStartY(),
          hcs.getEndY(), hcs.dy, values, this)));

      atts = new AttributeContainerMutable(CF.LONGITUDE);
      atts.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder(CF.LONGITUDE, CDM.LON_UNITS, null, DataType.FLOAT,
          AxisType.Lon, atts, CoverageCoordAxis.DependenceType.independent, null,
          CoverageCoordAxis.Spacing.regularPoint, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this)));

    } else {
      AttributeContainerMutable atts = new AttributeContainerMutable("y");
      atts.addAttribute(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder("y", "km", CF.PROJECTION_Y_COORDINATE,
          DataType.FLOAT, AxisType.GeoY, atts, CoverageCoordAxis.DependenceType.independent, null,
          CoverageCoordAxis.Spacing.regularPoint, hcs.ny, hcs.getStartY(), hcs.getEndY(), hcs.dy, null, this)));

      atts = new AttributeContainerMutable("x");
      atts.addAttribute(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder("x", "km", CF.PROJECTION_X_COORDINATE,
          DataType.FLOAT, AxisType.GeoX, atts, CoverageCoordAxis.DependenceType.independent, null,
          CoverageCoordAxis.Spacing.regularPoint, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this)));
    }
    return result;
  }

  /**
   * identify any variables that are really 2D lat/lon
   *
   * @param vars check this list, but remove lat/lon coordinates from it
   * @return lat/lon coordinates
   */
  private List<CoverageCoordAxis> makeHorizCoordinates2D(List<GribCollectionImmutable.VariableIndex> vars) {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();

    List<GribCollectionImmutable.VariableIndex> remove = new ArrayList<>();
    List<CoverageCoordAxis> result = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex vindex : vars) {
      Grib2Utils.LatLon2DCoord ll2d =
          Grib2Utils.getLatLon2DcoordType(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
      if (ll2d == null)
        continue;

      AxisType axisType = ll2d.getAxisType();
      String name = ll2d.toString();
      AttributeContainerMutable atts = new AttributeContainerMutable(name);
      atts.addAttribute(new Attribute(_Coordinate.Stagger, CDM.CurvilinearOrthogonal));
      atts.addAttribute(new Attribute(CDM.StaggerType, ll2d.toString()));

      int[] shape = {hcs.ny, hcs.nx};
      int npts = hcs.ny * hcs.nx;

      // deffered read
      CoverageCoordAxisBuilder builder;
      if (axisType == AxisType.Lat) {
        atts.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        builder = new CoverageCoordAxisBuilder(name, CDM.LAT_UNITS, vindex.makeVariableDescription(), DataType.FLOAT,
            AxisType.Lat, atts, CoverageCoordAxis.DependenceType.twoD, null, CoverageCoordAxis.Spacing.irregularPoint,
            npts, 0, 0, 0, null, this);
      } else {

        atts.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        builder = new CoverageCoordAxisBuilder(name, CDM.LON_UNITS, vindex.makeVariableDescription(), DataType.FLOAT,
            AxisType.Lon, atts, CoverageCoordAxis.DependenceType.twoD, null, CoverageCoordAxis.Spacing.irregularPoint,
            npts, 0, 0, 0, null, this);
      }

      builder.shape = shape;
      builder.userObject = vindex;
      result.add(new LatLonAxis2D(builder));
      remove.add(vindex);
    }

    // have to do this after the loop is done
    for (GribCollectionImmutable.VariableIndex vindex : remove) {
      vars.remove(vindex);
    }

    return result;
  }

  private List<CoverageCoordAxis> makeTime2DCoordinates(CoordinateTime2D time2D) {
    trackDateRange(time2D.makeCalendarDateRange()); // default calendar

    List<CoverageCoordAxis> result = new ArrayList<>();
    CoverageCoordAxis covTime;

    if (ds.getType() == GribCollectionImmutable.Type.SRC) {
      covTime = makeUniqueTimeAxis(time2D);
      CoordinateRuntime rt = time2D.getRuntimeCoordinate();
      result.add(makeRuntimeCoord(rt));

    } else if (ds.getType().isUniqueTime()) {
      covTime = makeUniqueTimeAxis(time2D);
      result.add(makeRuntimeAuxCoord(time2D, covTime.getNcoords()));

    } else if (time2D.isOrthogonal()) {
      covTime = makeTimeOffsetAxis(time2D);
      CoordinateRuntime rt = time2D.getRuntimeCoordinate();
      result.add(makeRuntimeCoord(rt));

    } else if (time2D.isRegular()) {
      covTime = makeFmrcRegTimeAxis(time2D);
      CoordinateRuntime rt = time2D.getRuntimeCoordinate();
      result.add(makeRuntimeCoord(rt)); // LOOK ?

    } else
      throw new IllegalStateException("Time2D with type= " + ds.getType());

    result.add(covTime);

    return result;
  }

  private boolean alreadyHave(List<CoverageCoordAxis> list, String name) {
    for (CoverageCoordAxis coord : list)
      if (coord.getName().equals(name))
        return true;
    return false;
  }

  // make an independent time 1D coordinate from time2D, knowing values are unique
  private CoverageCoordAxis1D makeUniqueTimeAxis(CoordinateTime2D time2D) {
    int nruns = time2D.getNruns();

    int ntimes = 0;
    for (int run = 0; run < time2D.getNruns(); run++) {
      CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
      ntimes += timeCoord.getSize();
    }
    double[] values;
    CalendarPeriod timeUnit = time2D.getTimeUnit();

    if (time2D.isTimeInterval()) {
      values = new double[2 * ntimes];
      int count = 0;
      for (int runIdx = 0; runIdx < nruns; runIdx++) {
        CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
        for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
          values[count++] = timeUnit.getValue() * tinv.getBounds1() + time2D.getOffset(runIdx);
          values[count++] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
        }
      }

    } else {
      values = new double[ntimes];
      int count = 0;
      for (int runIdx = 0; runIdx < nruns; runIdx++) {
        CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
        for (int val : coordTime.getOffsetSorted()) {
          double b1 = timeUnit.getValue() * val + time2D.getOffset(runIdx);
          values[count++] = b1;
        }
      }
    }

    AttributeContainerMutable atts = new AttributeContainerMutable(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit())); // LOOK why not udunit ??
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    CoverageCoordAxisBuilder builder =
        new CoverageCoordAxisBuilder(time2D.getName(), time2D.getTimeUdUnit(), CF.TIME, DataType.DOUBLE, AxisType.Time,
            atts, CoverageCoordAxis.DependenceType.independent, null, null, ntimes, 0.0, 0.0, 0.0, values, this);
    builder.setSpacingFromValues(time2D.isTimeInterval());

    return new CoverageCoordAxis1D(builder);
  }

  // time(runtime, time)
  private TimeAxis2DFmrc makeFmrcRegTimeAxis(CoordinateTime2D time2D) {
    CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
    String dependsOn = runtime.getName();
    int nruns = time2D.getNruns();
    int ntimes = time2D.getNtimes();

    int nvalues = time2D.isTimeInterval() ? nruns * ntimes * 2 : nruns * ntimes;
    double[] values = new double[nvalues];

    for (int runIdx = 0; runIdx < nruns; runIdx++) {
      int runOffset = time2D.getOffset(runIdx);

      CoordinateTimeAbstract time = time2D.getTimeCoordinate(runIdx);
      if (time2D.isTimeInterval()) {
        CoordinateTimeIntv coordIntv = (CoordinateTimeIntv) time;
        int n = coordIntv.getSize(); // may be different than ntimes
        for (int timeIdx = 0; timeIdx < n; timeIdx++) {
          TimeCoordIntvValue tinv = (TimeCoordIntvValue) coordIntv.getValue(timeIdx);
          values[runIdx * ntimes + timeIdx] = tinv.getBounds1() + runOffset;
          values[runIdx * ntimes + timeIdx + 1] = tinv.getBounds2() + runOffset;
        }
      } else {
        CoordinateTime coord = (CoordinateTime) time;
        int n = coord.getSize(); // may be different than ntimes
        for (int timeIdx = 0; timeIdx < n; timeIdx++) {
          Integer offset = (Integer) coord.getValue(timeIdx);
          values[runIdx * ntimes + timeIdx] = offset + runOffset;
        }
      }
    }

    AttributeContainerMutable atts = new AttributeContainerMutable(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, CDM.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(time2D.getName(), time2D.getUnit(), CDM.TIME_OFFSET,
        DataType.DOUBLE, AxisType.TimeOffset, atts, CoverageCoordAxis.DependenceType.fmrcReg, dependsOn, null,
        nruns * ntimes, 0.0, 0.0, 0.0, values, this);
    builder.setSpacingFromValues(time2D.isTimeInterval());

    return new TimeAxis2DFmrc(builder); // LOOK should be FmrcTimeAxisReg2D to take advantage of regular
  }

  // orthogonal runtime, offset; both independent
  private TimeOffsetAxis makeTimeOffsetAxis(CoordinateTime2D time2D) {
    List<?> offsets = time2D.getOffsetsSorted();
    int n = offsets.size();

    double[] values;

    if (time2D.isTimeInterval()) {
      values = new double[2 * n];
      int count = 0;
      for (Object offset : offsets) {
        TimeCoordIntvValue tinv = (TimeCoordIntvValue) offset;
        values[count++] = tinv.getBounds1();
        values[count++] = tinv.getBounds2();
      }

    } else {
      values = new double[n];
      int count = 0;
      for (Object val : offsets) {
        Integer off = (Integer) val;
        values[count++] = off;
      }
    }

    AttributeContainerMutable atts = new AttributeContainerMutable(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, CDM.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(time2D.getName(), time2D.getUnit(), CDM.TIME_OFFSET,
        DataType.DOUBLE, AxisType.TimeOffset, atts, CoverageCoordAxis.DependenceType.independent, null, null, n, 0.0,
        0.0, 0.0, values, this);
    builder.setSpacingFromValues(time2D.isTimeInterval());

    return new TimeOffsetAxis(builder);
  }

  //////////////////////////////////////////////////////////

  private List<CoverageCoordAxis> makeTimeCoordinates(CoordinateTimeAbstract time) {
    List<CoverageCoordAxis> result = new ArrayList<>();

    if (time instanceof CoordinateTime)
      result.add(makeCoordAxis((CoordinateTime) time));
    else if (time instanceof CoordinateTimeIntv)
      result.add(makeCoordAxis((CoordinateTimeIntv) time));

    CoverageCoordAxis runAux = makeRuntimeAuxCoord(time);
    if (runAux != null)
      result.add(runAux);

    return result;
  }

  // create an independent runtime axis
  private CoverageCoordAxis1D makeRuntimeCoord(CoordinateRuntime runtime) {
    String units = runtime.getPeriodName() + " since " + gribCollection.getMasterFirstDate();

    List<Double> offsets = runtime.getOffsetsInTimeUnits();
    int n = offsets.size();

    // CoordinateRuntime master = gribCollection.getMasterRuntime();
    boolean isScalar = (n == 1); // this is the case of runtime[1]
    CoverageCoordAxis.DependenceType dependence =
        isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

    double[] values = new double[n];
    int count = 0;
    for (Double offset : runtime.getOffsetsInTimeUnits())
      values[count++] = offset;

    AttributeContainerMutable atts = new AttributeContainerMutable(runtime.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, units));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    atts.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(runtime.getName(), units, "GRIB reference time",
        DataType.DOUBLE, AxisType.RunTime, atts, dependence, null, null, n, 0.0, 0.0, 0.0, values, this);

    builder.setSpacingFromValues(false);

    return new CoverageCoordAxis1D(builder);
  }

  // create a dependent runtime axis for this time2d, which has unique times
  private CoverageCoordAxis makeRuntimeAuxCoord(CoordinateTime2D time2D, int ntimes) {
    CoordinateRuntime runtimeU = time2D.getRuntimeCoordinate();
    List<Double> runOffsets = runtimeU.getOffsetsInTimeUnits();

    double[] values = new double[ntimes];
    int count = 0;
    for (int run = 0; run < time2D.getNruns(); run++) {
      CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
      for (int time = 0; time < timeCoord.getNCoords(); time++)
        values[count++] = runOffsets.get(run);
    }

    boolean isScalar = (time2D.getNruns() == 1); // this is the case of runtime[1]
    CoverageCoordAxis.DependenceType dependence =
        isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.dependent;
    String refName = "ref" + time2D.getName();

    AttributeContainerMutable atts = new AttributeContainerMutable(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(refName, time2D.getTimeUdUnit(), Grib.GRIB_RUNTIME,
        DataType.DOUBLE, AxisType.RunTime, atts, dependence, time2D.getName(), null, ntimes, 0, 0, 0, values, this);
    builder.setSpacingFromValues(false);

    return new CoverageCoordAxis1D(builder);
  }

  // create a dependent runtime axis for this time, using the index into the master runtimes array
  @Nullable
  private CoverageCoordAxis makeRuntimeAuxCoord(CoordinateTimeAbstract time) {
    if (time.getTime2runtime() == null)
      return null;
    String refName = "ref" + time.getName();

    int length = time.getSize();
    double[] data = new double[length];
    for (int i = 0; i < length; i++)
      data[i] = Double.NaN;

    int count = 0;
    CoordinateRuntime master = gribCollection.getMasterRuntime();
    List<Double> masterOffsets = master.getOffsetsInTimeUnits();
    for (int masterIdx : time.getTime2runtime()) {
      data[count++] = masterOffsets.get(masterIdx - 1);
    }

    AttributeContainerMutable atts = new AttributeContainerMutable(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getTimeUdUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder =
        new CoverageCoordAxisBuilder(refName, master.getUnit(), Grib.GRIB_RUNTIME, DataType.DOUBLE, AxisType.RunTime,
            atts, CoverageCoordAxis.DependenceType.dependent, time.getName(), null, length, 0, 0, 0, data, this);
    builder.setSpacingFromValues(false);

    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateTime time) {
    trackDateRange(time.makeCalendarDateRange()); // default calendar

    List<Integer> offsets = time.getOffsetSorted();
    int n = offsets.size();
    double[] values = new double[n];

    int count = 0;
    for (int offset : offsets)
      values[count++] = offset;

    AttributeContainerMutable atts = new AttributeContainerMutable(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder =
        new CoverageCoordAxisBuilder(time.getName(), time.getTimeUdUnit(), Grib.GRIB_VALID_TIME, DataType.DOUBLE,
            AxisType.Time, atts, CoverageCoordAxis.DependenceType.independent, null, null, n, 0, 0, 0, values, this);
    builder.setSpacingFromValues(false);
    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateTimeIntv time) {
    trackDateRange(time.makeCalendarDateRange()); // default calendar

    List<TimeCoordIntvValue> offsets = time.getTimeIntervals();
    int n = offsets.size();
    double[] values = new double[2 * n];

    int count = 0;
    for (TimeCoordIntvValue offset : offsets) {
      values[count++] = offset.getBounds1();
      values[count++] = offset.getBounds2();
    }

    AttributeContainerMutable atts = new AttributeContainerMutable(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder =
        new CoverageCoordAxisBuilder(time.getName(), time.getTimeUdUnit(), Grib.GRIB_VALID_TIME, DataType.DOUBLE,
            AxisType.Time, atts, CoverageCoordAxis.DependenceType.independent, null, null, n, 0, 0, 0, values, this);
    builder.setSpacingFromValues(true);
    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateVert vertCoord) {
    List<VertCoordValue> levels = vertCoord.getLevelSorted();

    int n = vertCoord.getSize();
    double[] values;

    if (vertCoord.isLayer()) {
      int count = 0;
      values = new double[2 * n];
      for (int i = 0; i < n; i++) {
        values[count++] = levels.get(i).getValue1();
        values[count++] = levels.get(i).getValue2();
      }

    } else {
      values = new double[n];
      for (int i = 0; i < n; i++)
        values[i] = levels.get(i).getValue1();
    }

    AttributeContainerMutable atts = new AttributeContainerMutable(vertCoord.getName());
    String units = vertCoord.getUnit();
    atts.addAttribute(new Attribute(CDM.UNITS, units));
    AxisType axisType = AxisType.GeoZ;
    if (SimpleUnit.isCompatible("mbar", units))
      axisType = AxisType.Pressure;
    else if (SimpleUnit.isCompatible("m", units))
      axisType = AxisType.Height;

    String desc = vertCoord.getVertUnit().getDesc();
    if (desc != null)
      atts.addAttribute(new Attribute(CDM.LONG_NAME, desc));
    atts.addAttribute(new Attribute(CF.POSITIVE, vertCoord.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));

    CoverageCoordAxisBuilder builder =
        new CoverageCoordAxisBuilder(vertCoord.getName(), vertCoord.getUnit(), null, DataType.DOUBLE, axisType, atts,
            CoverageCoordAxis.DependenceType.independent, null, null, n, 0.0, 0.0, 0.0, values, this);
    builder.setSpacingFromValues(vertCoord.isLayer());
    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateEns ensCoord) {
    int n = ensCoord.getSize();
    double[] values = new double[n];
    for (int i = 0; i < n; i++)
      values[i] = ((EnsCoordValue) ensCoord.getValue(i)).getEnsMember();

    AttributeContainerMutable atts = new AttributeContainerMutable(ensCoord.getName());
    String units = ensCoord.getUnit();
    atts.addAttribute(new Attribute(CDM.UNITS, units));

    CoverageCoordAxisBuilder builder =
        new CoverageCoordAxisBuilder(ensCoord.getName(), units, null, DataType.DOUBLE, AxisType.Ensemble, atts,
            CoverageCoordAxis.DependenceType.independent, null, null, ensCoord.getSize(), 0, 0, 0, values, this);
    builder.setSpacingFromValues(false);
    return new CoverageCoordAxis1D(builder);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  private CoverageCoordSys makeCoordSys(GribCollectionImmutable.VariableIndex gribVar,
      List<CoverageTransform> transforms, Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap) {
    List<String> axisNames = makeAxisNameList(gribVar, coord2axisMap);
    List<String> transformNames = transforms.stream().map(CoverageTransform::getName).collect(Collectors.toList());
    return new CoverageCoordSys(null, axisNames, transformNames, coverageType);
  }

  private static class NameAndType {
    final String name;
    final AxisType type;

    NameAndType(String name, AxisType type) {
      this.name = name;
      this.type = type;
    }
  }

  private List<String> makeAxisNameList(GribCollectionImmutable.VariableIndex gribVar,
      Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap) {
    List<NameAndType> names = new ArrayList<>();
    for (Coordinate axis : gribVar.getCoordinates()) {
      List<CoverageCoordAxis> coordList = coord2axisMap.get(axis);
      if (coordList != null) {
        for (CoverageCoordAxis coord : coordList)
          names.add(new NameAndType(coord.getName(), coord.getAxisType()));
      }
    }

    names.sort(Comparator.comparingInt(o -> o.type.axisOrder()));
    List<String> axisNames = names.stream().map(o -> o.name).collect(Collectors.toList());
    if (isCurvilinearOrthogonal) {
      Grib2Utils.LatLonCoordType type = Grib2Utils.getLatLon2DcoordType(gribVar.makeVariableDescription());
      if (type != null) {
        switch (type) {
          case U:
            axisNames.add(Grib2Utils.LatLon2DCoord.U_Latitude.toString());
            axisNames.add(Grib2Utils.LatLon2DCoord.U_Longitude.toString());
            break;
          case V:
            axisNames.add(Grib2Utils.LatLon2DCoord.V_Latitude.toString());
            axisNames.add(Grib2Utils.LatLon2DCoord.V_Longitude.toString());
            break;
          case P:
            axisNames.add(Grib2Utils.LatLon2DCoord.P_Latitude.toString());
            axisNames.add(Grib2Utils.LatLon2DCoord.P_Longitude.toString());
            break;
        }
      }

    } else if (isLatLon) {
      axisNames.add(CF.LATITUDE);
      axisNames.add(CF.LONGITUDE);

    } else {
      axisNames.add("y");
      axisNames.add("x");
    }

    return axisNames;
  }

  private Coverage makeCoverage(GribCollectionImmutable.VariableIndex gribVar,
      Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap) {

    AttributeContainerMutable atts = new AttributeContainerMutable(gribVar.makeVariableName());
    atts.addAttribute(new Attribute(CDM.LONG_NAME, gribVar.makeVariableDescription()));
    atts.addAttribute(new Attribute(CDM.UNITS, gribVar.makeVariableUnits()));
    gribCollection.addVariableAttributes(atts, gribVar);

    /*
     * GribTables.Parameter gp = gribVar.getGribParameter();
     * if (gp != null) {
     * if (gp.getDescription() != null)
     * atts.addAttribute(new Attribute(CDM.DESCRIPTION, gp.getDescription()));
     * if (gp.getAbbrev() != null)
     * atts.addAttribute(new Attribute(CDM.ABBREV, gp.getAbbrev()));
     * atts.addAttribute(new Attribute(CDM.MISSING_VALUE, gp.getMissing()));
     * if (gp.getFill() != null)
     * atts.addAttribute(new Attribute(CDM.FILL_VALUE, gp.getFill()));
     * } else {
     * atts.addAttribute(new Attribute(CDM.MISSING_VALUE, Float.NaN));
     * }
     * 
     * // statistical interval type
     * if (gribVar.getIntvType() >= 0) {
     * GribStatType statType = gribVar.getStatType();
     * if (statType != null) {
     * atts.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, statType.toString()));
     * CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
     * Coordinate timeCoord = gribVar.getCoordinate(Coordinate.Type.timeIntv);
     * if (cm != null && timeCoord != null)
     * atts.addAttribute(new Attribute(CF.CELL_METHODS, timeCoord.getName() + ": " + cm.toString()));
     * } else {
     * atts.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, gribVar.getIntvType()));
     * }
     * }
     */

    String coordSysName = CoverageCoordSys.makeCoordSysName(makeAxisNameList(gribVar, coord2axisMap));

    return new Coverage(gribVar.makeVariableName(), DataType.FLOAT, atts, coordSysName, gribVar.makeVariableUnits(),
        gribVar.makeVariableDescription(), this, gribVar);
  }

  //////////////////////////////////////////////////////


  @Override
  public double[] readCoordValues(CoverageCoordAxis coordAxis) throws IOException {
    if (coordAxis instanceof LatLonAxis2D)
      return readLatLonAxis2DCoordValues((LatLonAxis2D) coordAxis);

    java.util.Optional<Coordinate> opt = group.findCoordinate(coordAxis.getName());
    if (!opt.isPresent())
      throw new IllegalStateException();
    Coordinate coord = opt.get();

    if (coord instanceof CoordinateTime) {
      List<Integer> offsets = ((CoordinateTime) coord).getOffsetSorted();
      double[] values = new double[offsets.size()];
      int count = 0;
      for (int val : offsets)
        values[count++] = val;
      return values;

    } else if (coord instanceof CoordinateTimeIntv) {
      List<TimeCoordIntvValue> intv = ((CoordinateTimeIntv) coord).getTimeIntervals();
      double[] values;
      if (coordAxis.getSpacing() == CoverageCoordAxis.Spacing.discontiguousInterval) {
        values = new double[2 * intv.size()];
        int count = 0;
        for (TimeCoordIntvValue val : intv) {
          values[count++] = val.getBounds1();
          values[count++] = val.getBounds2();
        }
      } else {
        values = new double[intv.size() + 1];
        int count = 0;
        for (TimeCoordIntvValue val : intv) {
          values[count++] = val.getBounds1();
          values[count] = val.getBounds2(); // gets overritten except for the last
        }
      }
      return values;

    } else if (coord instanceof CoordinateRuntime) {
      List<Double> offsets = ((CoordinateRuntime) coord).getOffsetsInTimeUnits();
      double[] values = new double[offsets.size()];
      int count = 0;
      for (double val : offsets) {
        values[count++] = val;
      }
      return values;
    }

    throw new IllegalStateException();
  }

  private double[] readLatLonAxis2DCoordValues(LatLonAxis2D coordAxis) throws IOException {
    GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) coordAxis.getUserObject();
    int[] shape = coordAxis.getShape();
    List<RangeIterator> ranges = new ArrayList<>();
    List<Integer> fullShape = new ArrayList<>();
    for (Coordinate coord : vindex.getCoordinates()) {
      ranges.add(new Range(1));
      fullShape.add(coord.getNCoords());
    }
    ranges.add(new Range(shape[0]));
    fullShape.add(shape[0]);
    ranges.add(new Range(shape[1]));
    fullShape.add(shape[1]);
    SectionIterable siter = new SectionIterable(ranges, fullShape);

    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
    Array data;
    try {
      data = dataReader.readData(siter); // optimize pass in null ?? LOOK old way
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
    return (double[]) data.get1DJavaArray(DataType.DOUBLE); // LOOK lame conversion
  }


  //////////////////////////////////////////////////////

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams params, boolean canonicalOrder)
      throws IOException, InvalidRangeException {
    GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) coverage.getUserObject();
    CoverageCoordSys orgCoordSys = coverage.getCoordSys();
    Formatter errLog = new Formatter();
    java.util.Optional<CoverageCoordSys> opt = orgCoordSys.subset(params, false, true, errLog);
    if (!opt.isPresent()) {
      throw new InvalidRangeException(errLog.toString());
    }

    CoverageCoordSys subsetCoordSys = opt.get();
    List<CoverageCoordAxis> coordsSetAxes = new ArrayList<>(); // for CoordsSet.factory()

    // this orders the coords based on the grib coords, which also orders the iterator in CoordsSet. could be different
    // i think
    for (Coordinate gribCoord : vindex.getCoordinates()) {

      switch (gribCoord.getType()) {
        case runtime:
          CoverageCoordAxis runAxis = subsetCoordSys.getAxis(AxisType.RunTime);
          coordsSetAxes.addAll(axisAndDependents(runAxis, subsetCoordSys));
          break;

        case time2D:
          // also covers isConstantForecast
          if (ds.getType().isTwoD()) {
            CoverageCoordAxis toAxis = subsetCoordSys.getAxis(AxisType.TimeOffset);
            if (toAxis != null)
              coordsSetAxes.addAll(axisAndDependents(toAxis, subsetCoordSys));
          } else {
            CoverageCoordAxis toAxis = subsetCoordSys.getAxis(AxisType.Time);
            if (toAxis != null)
              coordsSetAxes.addAll(axisAndDependents(toAxis, subsetCoordSys));
          }

          // the OneTime case should be covered by axisAndDependents(runAxis)
          break;

        case vert:
          CoverageCoordAxis1D vertAxis = (CoverageCoordAxis1D) subsetCoordSys.getZAxis();
          coordsSetAxes.add(vertAxis);
          break;

        case time:
        case timeIntv:
        case ens:
          CoverageCoordAxis axis = subsetCoordSys.getAxis(gribCoord.getType().axisType);
          coordsSetAxes.addAll(axisAndDependents(axis, subsetCoordSys));
          break;
      }
    }

    List<CoverageCoordAxis> geoArrayAxes = new ArrayList<>(coordsSetAxes); // for GeoReferencedArray
    geoArrayAxes.add(subsetCoordSys.getYAxis());
    geoArrayAxes.add(subsetCoordSys.getXAxis());
    List<RangeIterator> yxRange = subsetCoordSys.getHorizCoordSys().getRanges(); // may be 2D

    // iterator over all except x, y
    CoordsSet coordIter = CoordsSet.factory(subsetCoordSys.isConstantForecast(), coordsSetAxes);

    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
    Array data = dataReader.readData2(coordIter, yxRange.get(0), yxRange.get(1));

    return new GeoReferencedArray(coverage.getShortName(), coverage.getDataType(), data, subsetCoordSys);
  }

  // LOOK dependent axis could get added multiple times
  private List<CoverageCoordAxis> axisAndDependents(CoverageCoordAxis axis, CoverageCoordSys csys) {
    List<CoverageCoordAxis> result = new ArrayList<>();
    if (axis.getDependenceType() != CoverageCoordAxis.DependenceType.dependent)
      result.add(axis);
    result.addAll(csys.getDependentAxes(axis));
    return result;
  }
}
