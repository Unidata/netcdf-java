/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grib.*;
import ucar.nc2.grib.collection.GribIosp.Time2Dinfo;
import ucar.nc2.grib.collection.GribIosp.Time2DinfoType;
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
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.coord.VertCoordValue;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.unidata.geoloc.projection.RotatedPole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;

/**
 * Grib Collection IOSP helper, for builders.
 */
class GribIospBuilder {
  final private GribIosp iosp;
  final private boolean isGrib1;
  final private org.slf4j.Logger logger;
  final private GribCollectionImmutable gribCollection;
  final private ucar.nc2.grib.GribTables gribTable;

  GribIospBuilder(GribIosp iosp, boolean isGrib1, Logger logger, GribCollectionImmutable gribCollection,
      GribTables gribTable) {
    this.iosp = iosp;
    this.isGrib1 = isGrib1;
    this.logger = logger;
    this.gribCollection = gribCollection;
    this.gribTable = gribTable;
  }

  void addGroup(Group.Builder parent, GribCollectionImmutable.GroupGC group, GribCollectionImmutable.Type gctype,
      boolean useGroups) {
    Group.Builder g;
    if (useGroups) {
      if (parent.findGroupLocal(group.getId()).isPresent()) {
        logger.warn("Duplicate Group - skipping");
        return;
      }
      g = Group.builder();
      g.setName(group.getId());
      g.addAttribute(new Attribute(CDM.LONG_NAME, group.getDescription()));
      parent.addGroup(g);
    } else {
      g = parent;
    }
    g.addAttribute(new Attribute("GribCollectionType", gctype.toString()));

    makeGroup(g, group, gctype);
  }

  private void makeGroup(Group.Builder g, GribCollectionImmutable.GroupGC group, GribCollectionImmutable.Type gctype) {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();
    String grid_mapping = hcs.getName() + "_Projection";

    String horizDims;

    boolean isRotatedLatLon = !isGrib1 && hcs.proj instanceof RotatedPole;
    boolean isLatLon2D = !isGrib1 && Grib2Utils.isCurvilinearOrthogonal(hcs.template, gribCollection.getCenter());
    boolean isLatLon = isGrib1 ? hcs.isLatLon() : Grib2Utils.isLatLon(hcs.template, gribCollection.getCenter());

    if (isRotatedLatLon) {
      Variable.Builder<?> hcsV = Variable.builder().setName(grid_mapping).setArrayType(ArrayType.INT);
      g.addVariable(hcsV);
      hcsV.setSourceData(Arrays.factory(ArrayType.INT, new int[0], new int[] {0}));
      hcsV.addAttributes(hcs.proj.getProjectionAttributes());

      horizDims = "rlat rlon";
      g.addDimension(new Dimension("rlat", hcs.ny));
      g.addDimension(new Dimension("rlon", hcs.nx));
      Variable.Builder<?> rlat = Variable.builder().setName("rlat").setArrayType(ArrayType.FLOAT)
          .setParentGroupBuilder(g).setDimensionsByName("rlat");
      g.addVariable(rlat);
      rlat.addAttribute(new Attribute(CF.STANDARD_NAME, CF.GRID_LATITUDE));
      rlat.addAttribute(new Attribute(CDM.UNITS, CDM.RLATLON_UNITS));
      rlat.setAutoGen(hcs.starty, hcs.dy);
      Variable.Builder<?> rlon = Variable.builder().setName("rlon").setArrayType(ArrayType.FLOAT)
          .setParentGroupBuilder(g).setDimensionsByName("rlon");
      g.addVariable(rlon);
      rlon.addAttribute(new Attribute(CF.STANDARD_NAME, CF.GRID_LONGITUDE));
      rlon.addAttribute(new Attribute(CDM.UNITS, CDM.RLATLON_UNITS));
      rlon.setAutoGen(hcs.startx, hcs.dx);
    } else if (isLatLon2D) { // CurvilinearOrthogonal - lat and lon fields must be present in the file
      horizDims = Grib.LAT_AXIS + " " + Grib.LON_AXIS;

      // Assume same number of points for all grids
      g.addDimension(new Dimension(Grib.LON_AXIS, hcs.nx));
      g.addDimension(new Dimension(Grib.LAT_AXIS, hcs.ny));

    } else if (isLatLon) {
      // make horiz coordsys coordinate variable
      Variable.Builder<?> hcsV = Variable.builder().setName(grid_mapping).setArrayType(ArrayType.INT);
      g.addVariable(hcsV);
      hcsV.setSourceData(Arrays.factory(ArrayType.INT, new int[0], new int[] {0}));
      hcsV.addAttributes(hcs.proj.getProjectionAttributes());

      horizDims = Grib.LAT_AXIS + " " + Grib.LON_AXIS;
      g.addDimension(new Dimension(Grib.LON_AXIS, hcs.nx));
      g.addDimension(new Dimension(Grib.LAT_AXIS, hcs.ny));

      Variable.Builder<?> lat = Variable.builder().setName(Grib.LAT_AXIS).setArrayType(ArrayType.FLOAT)
          .setParentGroupBuilder(g).setDimensionsByName(Grib.LAT_AXIS);
      g.addVariable(lat);
      lat.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      if (hcs.getGaussianLats() != null) {
        lat.setSourceData(hcs.getGaussianLats());
        lat.addAttribute(new Attribute(CDM.GAUSSIAN, "true"));
      } else {
        lat.setAutoGen(hcs.starty, hcs.dy);
      }

      Variable.Builder<?> lon = Variable.builder().setName(Grib.LON_AXIS).setArrayType(ArrayType.FLOAT)
          .setParentGroupBuilder(g).setDimensionsByName(Grib.LON_AXIS);
      g.addVariable(lon);
      lon.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      lon.setAutoGen(hcs.startx, hcs.dx);

    } else {
      // make horiz coordsys coordinate variable
      Variable.Builder<?> hcsV = Variable.builder().setName(grid_mapping).setArrayType(ArrayType.INT);
      g.addVariable(hcsV);
      hcsV.setSourceData(Arrays.factory(ArrayType.INT, new int[0], new int[] {0}));
      hcsV.addAttributes(hcs.proj.getProjectionAttributes());

      horizDims = Grib.YAXIS + " " + Grib.XAXIS;
      g.addDimension(new Dimension(Grib.XAXIS, hcs.nx));
      g.addDimension(new Dimension(Grib.YAXIS, hcs.ny));

      Variable.Builder<?> xcv = Variable.builder().setName(Grib.XAXIS).setArrayType(ArrayType.FLOAT)
          .setParentGroupBuilder(g).setDimensionsByName(Grib.XAXIS);
      g.addVariable(xcv);
      xcv.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
      xcv.addAttribute(new Attribute(CDM.UNITS, "km"));
      xcv.setAutoGen(hcs.startx, hcs.dx);

      Variable.Builder<?> ycv = Variable.builder().setName(Grib.YAXIS).setArrayType(ArrayType.FLOAT)
          .setParentGroupBuilder(g).setDimensionsByName(Grib.YAXIS);
      g.addVariable(ycv);
      ycv.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
      ycv.addAttribute(new Attribute(CDM.UNITS, "km"));
      ycv.setAutoGen(hcs.starty, hcs.dy);
    }

    for (Coordinate coord : group.coords) {
      Coordinate.Type ctype = coord.getType();
      switch (ctype) {
        case runtime:
          if (gctype.isTwoD() || coord.getNCoords() == 1) {
            makeRuntimeCoordinate(g, (CoordinateRuntime) coord);
          }
          break;
        case timeIntv:
          makeTimeCoordinate1D(g, (CoordinateTimeIntv) coord);
          break;
        case time:
          makeTimeCoordinate1D(g, (CoordinateTime) coord);
          break;
        case vert:
          makeVerticalCoordinate(g, (CoordinateVert) coord);
          break;
        case ens:
          makeEnsembleCoordinate(g, (CoordinateEns) coord);
          break;
        case time2D:
          if (gctype.isUniqueTime()) {
            makeUniqueTimeCoordinate2D(g, (CoordinateTime2D) coord);
          } else {
            CoordinateTime2D time2D = (CoordinateTime2D) coord;
            if (time2D.isOrthogonal()) {
              String timeDimName = makeTimeOffsetOrthogonal(g, time2D);
              makeTimeCoordinate2D(g, time2D, timeDimName);
            } else if (time2D.isRegular()) {
              String timeDimName = makeTimeOffsetRegular(g, time2D);
              makeTimeCoordinate2D(g, time2D, timeDimName);
            } else {
              makeTimeCoordinate2D(g, time2D, make2dValidTimeDimensionName(time2D.getName()));
            }
          }
          break;
      }
    }

    List<GribCollectionImmutable.VariableIndex> vlist = new ArrayList<>(group.variList);
    vlist.sort(Comparator.comparing(GribCollectionImmutable.VariableIndex::makeVariableName));
    for (GribCollectionImmutable.VariableIndex vindex : vlist) {
      try (Formatter dimNames = new Formatter(); Formatter coordinateAtt = new Formatter()) {
        // do the times first
        Coordinate run = vindex.getCoordinate(Coordinate.Type.runtime);
        Coordinate time = vindex.getCoordinateTime();
        if (time == null) {
          throw new IllegalStateException("No time coordinate = " + vindex);
        }

        String timeDimName = time.getName();
        String timeCoordName = time.getName();

        if (time instanceof CoordinateTime2D) {
          CoordinateTime2D time2D = (CoordinateTime2D) time;
          if (!gctype.isUniqueTime() && (time2D.isOrthogonal() || time2D.isRegular())) {
            timeDimName = makeTimeOffsetName(time.getName());
          } else {
            timeDimName = make2dValidTimeDimensionName(time.getName());
            timeCoordName = make2dValidTimeCoordName(timeDimName); // TODO backport this to version 5
          }
        }

        boolean isRunScaler = (run != null) && run.getSize() == 1;

        switch (gctype) {
          case SRC: // GC: Single Runtime Collection [ntimes] (run, 2D) scalar runtime
            assert isRunScaler;
            dimNames.format("%s ", timeDimName);
            coordinateAtt.format("%s %s ", run.getName(), timeDimName);
            break;

          case MRUTP: // PC: Multiple Runtime Unique Time Partition [ntimes]
          case MRUTC: // GC: Multiple Runtime Unique Time Collection [ntimes]
            // case MRSTC: // GC: Multiple Runtime Single Time Collection [nruns, 1]
            // case MRSTP: // PC: Multiple Runtime Single Time Partition [nruns, 1] (run, 2D) ignore the run, its
            // generated from the 2D in
            dimNames.format("%s ", timeDimName);
            coordinateAtt.format("ref%s %s ", timeDimName, timeDimName);
            break;

          case MRC: // GC: Multiple Runtime Collection [nruns, ntimes] (run, 2D) use Both
          case TwoD: // PC: TwoD time partition [nruns, ntimes]
            assert run != null : "GRIB MRC or TWOD does not have run coordinate";
            if (isRunScaler) {
              dimNames.format("%s ", timeDimName);
            } else {
              dimNames.format("%s %s ", run.getName(), timeDimName);
            }
            coordinateAtt.format("%s %s ", run.getName(), timeCoordName);
            if (timeDimName != timeCoordName) {
              coordinateAtt.format("%s ", timeDimName);
            }
            break;

          case Best: // PC: Best time partition [ntimes] (time) reftime is generated in makeTimeAuxReference()
          case BestComplete: // PC: Best complete time partition [ntimes]
            dimNames.format("%s ", timeDimName);
            coordinateAtt.format("ref%s %s ", timeDimName, timeDimName);
            break;

          default:
            throw new IllegalStateException("Uknown GribCollection TYpe = " + gctype);
        }

        // do other (vert, ens) coordinates
        for (Coordinate coord : vindex.getCoordinates()) {
          if (coord instanceof CoordinateTimeAbstract || coord instanceof CoordinateRuntime) {
            continue;
          }
          String name = coord.getName().toLowerCase();
          dimNames.format("%s ", name);
          coordinateAtt.format("%s ", name);
        }
        // do horiz coordinates
        dimNames.format("%s", horizDims);
        coordinateAtt.format("%s ", horizDims);

        // heres where the Variable gets made
        String vname = iosp.makeVariableName(vindex);
        Variable.Builder<?> v = Variable.builder().setName(vname).setArrayType(ArrayType.FLOAT).setParentGroupBuilder(g)
            .setDimensionsByName(dimNames.toString());
        g.addVariable(v);

        String desc = iosp.makeVariableLongName(vindex);
        v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
        v.addAttribute(new Attribute(CDM.UNITS, iosp.makeVariableUnits(vindex)));

        GribTables.Parameter gp = iosp.getParameter(vindex);
        if (gp != null) {
          if (gp.getDescription() != null) {
            v.addAttribute(new Attribute(CDM.DESCRIPTION, gp.getDescription()));
          }
          if (gp.getAbbrev() != null) {
            v.addAttribute(new Attribute(CDM.ABBREV, gp.getAbbrev()));
          }
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, gp.getMissing()));
          if (gp.getFill() != null) {
            v.addAttribute(new Attribute(CDM.FILL_VALUE, gp.getFill()));
          }
        } else {
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, Float.NaN));
        }

        // horiz coord system
        if (isLatLon2D) { // special case of "LatLon Orthogononal"
          String s = iosp.searchCoord(Grib2Utils.getLatLon2DcoordType(desc), group.variList);
          if (s == null) { // its a 2D lat/lon coordinate
            v.setDimensionsByName(horizDims);
            String units = desc.contains("Latitude of") ? CDM.LAT_UNITS : CDM.LON_UNITS;
            v.addAttribute(new Attribute(CDM.UNITS, units));

          } else { // its a variable using the coordinates described by s
            coordinateAtt.format("%s ", s);
          }
        } else {
          v.addAttribute(new Attribute(CF.GRID_MAPPING, grid_mapping));
        }
        v.addAttribute(new Attribute(CF.COORDINATES, coordinateAtt.toString()));

        // statistical interval type
        if (vindex.getIntvType() >= 0) {
          GribStatType statType = gribTable.getStatType(vindex.getIntvType());
          if (statType != null) {
            v.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, statType.toString()));
            CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
            Coordinate timeCoord = vindex.getCoordinate(Coordinate.Type.timeIntv);
            if (cm != null && timeCoord != null) {
              v.addAttribute(new Attribute(CF.CELL_METHODS, timeCoord.getName() + ": " + cm));
            }
          } else {
            v.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, vindex.getIntvType()));
          }
        }

        gribCollection.addVariableAttributes(v.getAttributeContainer(), vindex);
        v.setSPobject(vindex);
      }
    }
  }

  private void makeRuntimeCoordinate(Group.Builder g, CoordinateRuntime rtc) {
    int n = rtc.getSize();
    boolean isScalar = (n == 1); // this is the case of runtime[1]
    String tcName = rtc.getName();
    String dims = isScalar ? null : rtc.getName(); // null means scalar
    g.addDimension(new Dimension(tcName, n));


    Variable.Builder<?> v = Variable.builder().setName(tcName).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(g)
        .setDimensionsByName(dims);
    g.addVariable(v);
    v.addAttribute(new Attribute(CDM.UNITS, rtc.getUnit()));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    // lazy eval
    v.setSPobject(new Time2Dinfo(Time2DinfoType.reftime, null, rtc));
  }

  // time coordinates are unique
  // time(nruns, ntimes) -> time(ntimes) with dependent reftime(ntime) coordinate
  private void makeUniqueTimeCoordinate2D(Group.Builder g, CoordinateTime2D time2D) {
    CoordinateRuntime runtime = time2D.getRuntimeCoordinate();

    int countU = 0;
    for (int run = 0; run < time2D.getNruns(); run++) {
      CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
      countU += timeCoord.getSize();
    }
    int ntimes = countU;
    String tcName = time2D.getName();
    String timeDimName = make2dValidTimeDimensionName(tcName);

    g.addDimension(new Dimension(timeDimName, ntimes));
    Variable.Builder<?> v = Variable.builder().setName(tcName).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(g)
        .setDimensionsByName(timeDimName);
    g.addVariable(v);
    String units = runtime.getUnit(); // + " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    // the data is not generated until asked for to save space
    if (!time2D.isTimeInterval()) {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.offU, time2D, null));
    } else {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.intvU, time2D, null));
      // bounds for intervals
      String bounds_name = timeDimName + "_bounds";
      Variable.Builder<?> bounds = Variable.builder().setName(bounds_name).setArrayType(ArrayType.DOUBLE)
          .setParentGroupBuilder(g).setDimensionsByName(timeDimName + " 2");
      g.addVariable(bounds);
      v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
      bounds.addAttribute(new Attribute(CDM.UNITS, units));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));
      bounds.setSPobject(new Time2Dinfo(Time2DinfoType.boundsU, time2D, null));
    }

    if (runtime.getNCoords() != 1) {
      // for this case we have to generate a separate reftime, because have to use the same dimension
      String refName = "ref" + tcName;
      if (!g.findVariableLocal(refName).isPresent()) {
        Variable.Builder<?> vref = Variable.builder().setName(refName).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(g).setDimensionsByName(timeDimName);
        g.addVariable(vref);
        vref.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
        vref.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
        vref.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));
        vref.addAttribute(new Attribute(CDM.UNITS, units));
        vref.setSPobject(new Time2Dinfo(Time2DinfoType.isUniqueRuntime, time2D, null));
      }
    }
  }

  /*
   * For better compatibility with CF recommendations, the 2D time coordinates should not have the same name as
   * a dimension (a recommendation, but not a strict requirement). 2D time coordinates are now called "validtime{n}",
   * where n is empty or a number (starting with 1). In order to maintain shared dimensions with other time variables,
   * we need to transform that name into a dimension name. It's all pretty simple, but we do it enough times that it
   * gets its own method. Basically, the variable "validtime{n}" will use dimension "time{n}".
   * See https://github.com/Unidata/netcdf-java/issues/152
   */
  private String make2dValidTimeDimensionName(String variableName) {
    return variableName.replaceFirst("valid", "");
  }

  private String make2dValidTimeCoordName(String dimName) {
    return "valid" + dimName;
  }

  private String makeTimeOffsetName(String timeName) {
    return timeName.toLowerCase() + "Offset";
  }

  /*
   * non unique time case
   * 3) time(nruns, ntimes) with reftime(nruns)
   */
  private void makeTimeCoordinate2D(Group.Builder g, CoordinateTime2D time2D, String timeDimName) {
    CoordinateRuntime runtime = time2D.getRuntimeCoordinate();

    int ntimes = time2D.getNtimes();
    String tcName = time2D.getName();
    String dims = runtime.getName() + " " + timeDimName;
    int dimLength = ntimes;

    g.addDimensionIfNotExists(new Dimension(timeDimName, dimLength));
    Variable.Builder<?> v = Variable.builder().setName(tcName).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(g)
        .setDimensionsByName(dims);
    g.addVariable(v);
    String units = runtime.getUnit(); // + " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));
    if (!tcName.equalsIgnoreCase(timeDimName)) {
      // explicitly set the axis type as Time
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    }

    // the data is not generated until asked for to save space
    if (!time2D.isTimeInterval()) {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.off, time2D, null));
    } else {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.intv, time2D, null));
      // bounds for intervals
      String bounds_name = tcName + "_bounds";
      Variable.Builder<?> bounds = Variable.builder().setName(bounds_name).setArrayType(ArrayType.DOUBLE)
          .setParentGroupBuilder(g).setDimensionsByName(dims + " 2");
      g.addVariable(bounds);
      v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
      bounds.addAttribute(new Attribute(CDM.UNITS, units));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));
      bounds.setSPobject(new Time2Dinfo(Time2DinfoType.bounds, time2D, null));
    }
  }

  private Array<?> makeLazyCoordinateData(Variable v2, Time2Dinfo info) {
    if (info.time2D != null) {
      return makeLazyTime2Darray(v2, info);
    } else {
      return makeLazyTime1Darray(v2, info);
    }
  }

  // only for the 2d times
  private Array<?> makeLazyTime1Darray(Variable v2, Time2Dinfo info) {
    int length = info.time1D.getSize();
    double[] data = new double[length];
    for (int i = 0; i < length; i++) {
      data[i] = Double.NaN;
    }

    // coordinate values
    switch (info.which) {
      case reftime:
        CoordinateRuntime rtc = (CoordinateRuntime) info.time1D;
        int count = 0;
        for (double val : rtc.getOffsetsInTimeUnits()) {
          data[count++] = val;
        }
        return Arrays.factory(ArrayType.DOUBLE, v2.getShape(), data);

      case timeAuxRef:
        CoordinateTimeAbstract time = (CoordinateTimeAbstract) info.time1D;
        count = 0;
        List<Double> masterOffsets = gribCollection.getMasterRuntime().getOffsetsInTimeUnits();
        for (int masterIdx : time.getTime2runtime()) {
          data[count++] = masterOffsets.get(masterIdx - 1);
        }
        return Arrays.factory(ArrayType.DOUBLE, v2.getShape(), data);

      default:
        throw new IllegalStateException("makeLazyTime1Darray must be reftime or timeAuxRef");
    }
  }

  // only for the 2d times
  private Array<?> makeLazyTime2Darray(Variable coord, Time2Dinfo info) {
    CoordinateTime2D time2D = info.time2D;
    CalendarPeriod timeUnit = time2D.getTimeUnit();

    int nruns = time2D.getNruns();
    int ntimes = time2D.getNtimes();

    int length = (int) coord.getSize();
    if (info.which == Time2DinfoType.bounds) {
      length *= 2;
    }

    double[] data = new double[length];
    for (int i = 0; i < length; i++) {
      data[i] = Double.NaN;
    }
    int count;

    // coordinate values
    switch (info.which) {
      case off:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (long val : coordTime.getOffsetSorted()) {
            data[runIdx * ntimes + timeIdx] = timeUnit.getValue() * val + time2D.getOffset(runIdx);
            timeIdx++;
          }
        }
        break;

      case offU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
          for (long val : coordTime.getOffsetSorted()) {
            data[count++] = timeUnit.getValue() * val + time2D.getOffset(runIdx);
          }
        }
        break;

      case intv:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[runIdx * ntimes + timeIdx] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx); // use
            // upper
            // bounds
            // for
            // coord
            // value
            timeIdx++;
          }
        }
        break;

      case intvU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[count++] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx); // use upper bounds for
            // coord value
          }
        }
        break;

      case is1Dtime:
        CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
        count = 0;
        for (double val : runtime.getOffsetsInTimeUnits()) { // convert to udunits
          data[count++] = val;
        }
        break;

      case isUniqueRuntime: // the aux runtime coordinate
        CoordinateRuntime runtimeU = time2D.getRuntimeCoordinate();
        List<Double> runOffsets = runtimeU.getOffsetsInTimeUnits();
        count = 0;
        for (int run = 0; run < time2D.getNruns(); run++) {
          CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
          for (int time = 0; time < timeCoord.getNCoords(); time++) {
            data[count++] = runOffsets.get(run);
          }
        }
        break;

      case bounds:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[runIdx * ntimes * 2 + timeIdx] = timeUnit.getValue() * tinv.getBounds1() + time2D.getOffset(runIdx);
            data[runIdx * ntimes * 2 + timeIdx + 1] =
                timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
            timeIdx += 2;
          }
        }
        break;

      case boundsU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[count++] = timeUnit.getValue() * tinv.getBounds1() + time2D.getOffset(runIdx);
            data[count++] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
          }
        }
        break;

      default:
        throw new IllegalStateException();
    }

    return Arrays.factory(ArrayType.DOUBLE, coord.getShape(), data);
  }

  private void makeTimeCoordinate1D(Group.Builder g, CoordinateTime coordTime) { // }, CoordinateRuntime
    // runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    g.addDimension(new Dimension(tcName, ntimes));
    Variable.Builder<?> v = Variable.builder().setName(tcName).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(g)
        .setDimensionsByName(dims);
    g.addVariable(v);
    String units = coordTime.getTimeUdUnit();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    double[] data = new double[ntimes];
    int count = 0;

    // coordinate values
    for (long val : coordTime.getOffsetSorted()) {
      data[count++] = val;
    }
    v.setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {ntimes}, data));

    makeTimeAuxReference(g, tcName, units, coordTime);
  }

  private void makeTimeAuxReference(Group.Builder g, String timeName, String units, CoordinateTimeAbstract time) {
    if (time.getTime2runtime() == null) {
      return;
    }
    String tcName = "ref" + timeName;
    Variable.Builder<?> v = Variable.builder().setName(tcName).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(g)
        .setDimensionsByName(timeName);
    g.addVariable(v);
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));
    v.addAttribute(new Attribute(CDM.UNITS, units));

    // lazy evaluation
    v.setSPobject(new Time2Dinfo(Time2DinfoType.timeAuxRef, null, time));
  }

  private void makeTimeCoordinate1D(Group.Builder g, CoordinateTimeIntv coordTime) { // }, CoordinateRuntime
    // runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    g.addDimension(new Dimension(tcName, ntimes));
    Variable.Builder<?> v = Variable.builder().setName(tcName).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(g)
        .setDimensionsByName(dims);
    g.addVariable(v);
    String units = coordTime.getTimeUdUnit();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    double[] data = new double[ntimes];
    int count = 0;

    // use upper bounds for coord value
    for (TimeCoordIntvValue tinv : coordTime.getTimeIntervals()) {
      data[count++] = tinv.getBounds2();
    }
    v.setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {ntimes}, data));

    // bounds
    String bounds_name = tcName + "_bounds";
    Variable.Builder<?> bounds = Variable.builder().setName(bounds_name).setArrayType(ArrayType.DOUBLE)
        .setParentGroupBuilder(g).setDimensionsByName(dims + " 2");
    g.addVariable(bounds);
    v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
    bounds.addAttribute(new Attribute(CDM.UNITS, units));
    bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

    data = new double[ntimes * 2];
    count = 0;
    for (TimeCoordIntvValue tinv : coordTime.getTimeIntervals()) {
      data[count++] = tinv.getBounds1();
      data[count++] = tinv.getBounds2();
    }
    bounds.setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {ntimes, 2}, data));

    makeTimeAuxReference(g, tcName, units, coordTime);
  }

  private void makeVerticalCoordinate(Group.Builder g, CoordinateVert vc) {
    int n = vc.getSize();
    String vcName = vc.getName().toLowerCase();

    g.addDimension(new Dimension(vcName, n));
    Variable.Builder<?> v = Variable.builder().setName(vcName).setArrayType(ArrayType.FLOAT).setParentGroupBuilder(g)
        .setDimensionsByName(vcName);
    g.addVariable(v);
    if (vc.getUnit() != null) {
      v.addAttribute(new Attribute(CDM.UNITS, vc.getUnit()));
      String desc = iosp.getVerticalCoordDesc(vc.getCode());
      if (desc != null) {
        v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      }
      v.addAttribute(new Attribute(CF.POSITIVE, vc.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));
    }

    v.addAttribute(new Attribute("Grib_level_type", vc.getCode()));
    VertCoordType vu = vc.getVertUnit();
    if (vu != null) {
      if (vu.getDatum() != null) {
        v.addAttribute(new Attribute("datum", vu.getDatum()));
      }
    }

    if (vc.isLayer()) {
      float[] data = new float[n];
      int count = 0;
      for (VertCoordValue val : vc.getLevelSorted()) {
        data[count++] = (float) (val.getValue1() + val.getValue2()) / 2;
      }
      v.setSourceData(Arrays.factory(ArrayType.FLOAT, new int[] {n}, data));

      Variable.Builder<?> bounds = Variable.builder().setName(vcName + "_bounds").setArrayType(ArrayType.FLOAT)
          .setParentGroupBuilder(g).setDimensionsByName(vcName + " 2");
      g.addVariable(bounds);
      v.addAttribute(new Attribute(CF.BOUNDS, vcName + "_bounds"));
      String vcUnit = vc.getUnit();
      if (vcUnit != null) {
        bounds.addAttribute(new Attribute(CDM.UNITS, vcUnit));
      }
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + vcName));

      data = new float[2 * n];
      count = 0;
      for (VertCoordValue level : vc.getLevelSorted()) {
        data[count++] = (float) level.getValue1();
        data[count++] = (float) level.getValue2();
      }
      bounds.setSourceData(Arrays.factory(ArrayType.FLOAT, new int[] {n, 2}, data));

    } else {
      float[] data = new float[n];
      int count = 0;
      for (VertCoordValue val : vc.getLevelSorted()) {
        data[count++] = (float) val.getValue1();
      }
      v.setSourceData(Arrays.factory(ArrayType.FLOAT, new int[] {n}, data));
    }
  }

  private void makeEnsembleCoordinate(Group.Builder g, CoordinateEns ec) {
    int n = ec.getSize();
    String ecName = ec.getName().toLowerCase();
    g.addDimension(new Dimension(ecName, n));

    Variable.Builder<?> v = Variable.builder().setName(ecName).setArrayType(ArrayType.INT).setParentGroupBuilder(g)
        .setDimensionsByName(ecName);
    g.addVariable(v);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Ensemble.toString()));

    int[] data = new int[n];
    int count = 0;
    for (EnsCoordValue ecc : ec.getEnsSorted()) {
      data[count++] = ecc.getEnsMember();
    }
    v.setSourceData(Arrays.factory(ArrayType.INT, new int[] {n}, data));
  }

  // orthogonal runtime, offset; both independent
  private String makeTimeOffsetOrthogonal(Group.Builder g, CoordinateTime2D time2D) {
    List<?> offsets = time2D.getOffsetsSorted();
    int n = offsets.size();
    String toName = makeTimeOffsetName(time2D.getName());
    g.addDimension(new Dimension(toName, n));

    Variable.Builder<?> v = Variable.builder().setName(toName).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(g)
        .setDimensionsByName(toName);
    g.addVariable(v);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));
    v.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
    v.addAttribute(new Attribute(CDM.LONG_NAME, CDM.TIME_OFFSET));
    v.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));

    double[] midpoints = new double[n];
    double[] bounds = null;
    if (time2D.isTimeInterval()) {
      bounds = new double[2 * n];
      int count = 0;
      int countb = 0;
      for (Object offset : offsets) {
        TimeCoordIntvValue tinv = (TimeCoordIntvValue) offset;
        midpoints[count++] = (tinv.getBounds1() + tinv.getBounds2()) / 2.0;
        bounds[countb++] = tinv.getBounds1();
        bounds[countb++] = tinv.getBounds2();
      }
    } else {
      int count = 0;
      for (Object val : offsets) {
        midpoints[count++] = (Long) val;
      }
    }
    v.setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {n}, midpoints));

    if (time2D.isTimeInterval()) {
      String boundsName = toName + "_bounds";
      Variable.Builder<?> coordVarBounds = VariableDS.builder().setName(boundsName).setArrayType(ArrayType.DOUBLE)
          .setDesc("TimeOffset coord bounds").setParentGroupBuilder(g).setDimensionsByName(toName + " 2")
          .setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {n, 2}, bounds));
      g.addVariable(coordVarBounds);

      v.addAttribute(new Attribute(CF.BOUNDS, boundsName));
    }

    return toName;
  }

  // regular runtime, offset; offset depends on runtime minute from 0z
  private String makeTimeOffsetRegular(Group.Builder gb, CoordinateTime2D time2D) {
    try {
      List<Integer> minsFrom0z = ImmutableList.copyOf(time2D.getRegularMinuteOffsets());
      int nhours = minsFrom0z.size();
      int noffsets = time2D.getNtimes();
      String toName = makeTimeOffsetName(time2D.getName());
      gb.addDimension(new Dimension(toName, noffsets));
      String dimNames = nhours + " " + toName;

      Variable.Builder<?> v = Variable.builder().setName(toName).setArrayType(ArrayType.DOUBLE)
          .setParentGroupBuilder(gb).setDimensionsByName(dimNames);
      gb.addVariable(v);
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));
      v.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
      v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
      v.addAttribute(new Attribute(CDM.LONG_NAME, CDM.TIME_OFFSET));
      v.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));

      // Special Coordinates. TODO is there a CF equivalent?
      v.addAttribute(
          new Attribute(CDM.RUNTIME_COORDINATE, gb.makeFullName() + time2D.getRuntimeCoordinate().getName()));
      Attribute.Builder attb =
          Attribute.builder(CDM.TIME_OFFSET_MINUTES).setArrayType(ArrayType.INT).setValues(minsFrom0z, false);
      v.addAttribute(attb.build());

      // We use a rectangular array; uneven arrays will have NaNs.
      double[] midpoints = new double[nhours * noffsets];
      java.util.Arrays.fill(midpoints, Double.NaN);
      double[] bounds = null;
      if (time2D.isTimeInterval()) {
        bounds = new double[2 * nhours * noffsets];
        java.util.Arrays.fill(bounds, Double.NaN);
        int offsetidx = 0;
        for (int minutes : time2D.getRegularMinuteOffsets()) {
          CoordinateTimeIntv timeCoord = (CoordinateTimeIntv) time2D.getRegularTimeCoordinateFromMinuteOfDay(minutes);
          // may be uneven number of values for each
          int count = offsetidx * noffsets;
          int countb = 2 * offsetidx * noffsets;
          for (TimeCoordIntvValue tinv : timeCoord.getTimeIntervals()) {
            midpoints[count++] = (tinv.getBounds1() + tinv.getBounds2()) / 2.0;
            bounds[countb++] = tinv.getBounds1();
            bounds[countb++] = tinv.getBounds2();
          }
          offsetidx++;
        }
      } else {
        java.util.Arrays.fill(midpoints, Double.NaN);
        int offsetidx = 0;
        for (int minutes : time2D.getRegularMinuteOffsets()) {
          CoordinateTime timeCoord = (CoordinateTime) time2D.getRegularTimeCoordinateFromMinuteOfDay(minutes);
          // may be uneven number of values for each hour
          int count = offsetidx * noffsets;
          for (Long offset : timeCoord.getOffsetSorted()) {
            midpoints[count++] = offset;
          }
          offsetidx++;
        }
      }
      v.setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {nhours, noffsets}, midpoints));

      if (time2D.isTimeInterval()) {
        String boundsName = toName + "_bounds";
        Variable.Builder<?> coordVarBounds = VariableDS.builder().setName(boundsName).setArrayType(ArrayType.DOUBLE)
            .setDesc("time offset coord bounds").setParentGroupBuilder(gb).setDimensionsByName(dimNames + " 2");
        gb.addVariable(coordVarBounds);
        coordVarBounds.setSourceData(Arrays.factory(ArrayType.DOUBLE, new int[] {nhours, noffsets, 2}, bounds));

        v.addAttribute(new Attribute(CF.BOUNDS, boundsName));
      }
      return toName;
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    }
  }
}
