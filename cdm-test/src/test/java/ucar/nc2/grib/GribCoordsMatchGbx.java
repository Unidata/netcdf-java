/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import javax.annotation.Nullable;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.CollectionUpdateType;
import ucar.array.InvalidRangeException;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.grib.collection.*;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.grid2.CoordInterval;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridDatasetFactory;
import ucar.nc2.grid2.GridReferencedArray;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.internal.util.Counters;
import ucar.nc2.util.Misc;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;

/**
 * Check that coordinate values match Grib Records.
 * Using GridDataset. Using just the gbx
 */
public class GribCoordsMatchGbx {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String KIND_GRID = "grid";
  private static final int MAX_READS = -1;
  private static final boolean showMissing = false;

  // The maximum relative difference for floating-point comparisons.
  private static final double maxRelDiff = 1e-6;

  public static Counters getCounters() {
    Counters countersAll = new Counters();
    countersAll.add(KIND_GRID);
    return countersAll;
  }

  private String filename;
  Counters counters;
  private boolean isGrib1;
  String kind;
  Grid grid;
  int countReadsForVariable;

  public GribCoordsMatchGbx(String filename, Counters counters) {
    this.filename = filename;
    this.counters = counters;
  }

  private CalendarDateRange makeDateBounds(GridSubset coords, CalendarDate runtime) {
    CoordInterval time_bounds = coords.getTimeOffsetIntv();
    CalendarDateUnit dateUnit = coords.getTimeOffsetUnit();
    CalendarDate start = runtime.add(CalendarPeriod.of((int) time_bounds.start(), dateUnit.getCalendarField()));
    CalendarDate end = runtime.add(CalendarPeriod.of((int) time_bounds.end(), dateUnit.getCalendarField()));
    return CalendarDateRange.of(start, end);
  }

  boolean nearlyEquals(CalendarDate date1, CalendarDate date2) {
    return Math.abs(date1.getMillisFromEpoch() - date2.getMillisFromEpoch()) < 5000; // 5 secs
  }

  public int readGridDataset() throws IOException, InvalidRangeException {
    kind = KIND_GRID;
    int countFailures = 0;
    Formatter errlog = new Formatter();
    try (GridDataset fdc = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(fdc).isNotNull();
      String gribVersion = fdc.attributes().findAttributeString("file_format", "GRIB-2");
      isGrib1 = gribVersion.equalsIgnoreCase("GRIB-1");

      for (Grid grid : fdc.getGrids()) {
        if (readGrid(grid))
          counters.count(kind, "success");
        else {
          counters.count(kind, "fail");
          countFailures++;
        }
      }
    }
    return countFailures;
  }

  private boolean readGrid(Grid grid) throws IOException, ucar.array.InvalidRangeException {
    // if (!coverage.getName().startsWith("Total_pre")) return true;
    if (showMissing)
      logger.debug("coverage {}", grid.getName());
    this.grid = grid;
    countReadsForVariable = 0;

    GridCoordinateSystem ccs = grid.getCoordinateSystem();
    List<GridAxis> subsetAxes = new ArrayList<>();

    for (GridAxis axis : ccs.getGridAxes()) {
      switch (axis.getAxisType()) { // LOOK what about 2D ??
        case RunTime:
        case Time:
        case TimeOffset:
        case GeoZ:
        case Height:
        case Pressure:
        case Ensemble:
          subsetAxes.add(axis);
      }
    }

    // LOOK we dont have an equivilent function for GridSubset
    // CoordsSet coordIter = CoordsSet.factory(ccs.isConstantForecast(), subsetAxes);
    List<GridSubset> fake = new ArrayList<>();
    for (GridSubset coords : fake) {
      readGridData(grid, coords);
    }
    return true;
  }

  private void readGridData(Grid cover, GridSubset coords) throws IOException, ucar.array.InvalidRangeException {
    countReadsForVariable++;
    if (MAX_READS > 0 && countReadsForVariable > MAX_READS)
      return;

    GribArrayReader.currentDataRecord = null;
    GridReferencedArray geoArray = cover.readData(coords);

    if (isGrib1)
      readAndTestGrib1(cover.getName(), coords);
    else
      readAndTestGrib2(cover.getName(), coords);
  }

  ////////////////////////////////////////////////////////
  // Grib1

  private Map<String, IdxHashGrib1> fileIndexMapGrib1 = new HashMap<>();

  private class IdxHashGrib1 {
    private Map<Long, Grib1Record> map = new HashMap<>();

    IdxHashGrib1(String idxFile) throws IOException {
      Grib1Index index = new Grib1Index();
      index.readIndex(idxFile, -1, CollectionUpdateType.never);
      for (Grib1Record gr : index.getRecords()) {
        Grib1SectionIndicator is = gr.getIs();
        map.put(is.getStartPos(), gr); // start of entire message
      }
    }

    Grib1Record get(long pos) {
      return map.get(pos);
    }
  }

  private void readAndTestGrib1(String name, GridSubset coords) throws IOException {
    GribCollectionImmutable.Record dr = GribArrayReader.currentDataRecord;
    if (dr == null) {
      if (showMissing)
        logger.debug("missing record= {}", coords);
      counters.count(kind, "missing1");
      return;
    }
    if (showMissing)
      logger.debug("found record= {}", coords);
    counters.count(kind, "found1");

    String filename = GribArrayReader.currentDataRafFilename;
    String idxFile = filename.endsWith(".gbx9") ? filename : filename + ".gbx9";
    IdxHashGrib1 idxHash = fileIndexMapGrib1.get(idxFile);
    if (idxHash == null) {
      idxHash = new IdxHashGrib1(idxFile);
      fileIndexMapGrib1.put(idxFile, idxHash);
    }

    Grib1Record gr1 = idxHash.get(dr.pos);
    Grib1SectionProductDefinition pdss = gr1.getPDSsection();
    Grib1Customizer cust1 = Grib1Customizer.factory(gr1, null);

    // check runtime
    CalendarDate rt_val = coords.getRunTime();
    boolean runtimeOk = true;
    if (rt_val != null) {
      CalendarDate gribDate = pdss.getReferenceDate();
      runtimeOk &= rt_val.equals(gribDate);
      if (!runtimeOk) {
        // tryAgain(coords);
        logger.debug("{} {} failed on runtime {} != gbx {}", kind, name, rt_val, gribDate);
      }
      Assert.assertEquals(gribDate, rt_val);
    }

    // check time
    CalendarDate time_val = coords.getTime();
    if (time_val == null) {
      time_val = coords.getTimeOffsetDate();
    }
    Grib1ParamTime ptime = gr1.getParamTime(cust1);
    if (ptime.isInterval()) {
      CalendarDate[] gbxInv = getForecastInterval(pdss, ptime);
      CalendarDateRange date_bounds = coords.getTimeRange();
      if (date_bounds == null) {
        date_bounds = makeDateBounds(coords, rt_val);
      }
      if (!date_bounds.getStart().equals(gbxInv[0]) || !date_bounds.getEnd().equals(gbxInv[1])) {
        // tryAgain(coords);
        logger.debug("{} {} failed on time intv: coord=[{}] gbx =[{},{}]", kind, name, date_bounds, gbxInv[0],
            gbxInv[1]);
      }

    } else if (time_val != null) {
      CalendarDate gbxDate = getForecastDate(pdss, ptime);
      if (!time_val.equals(gbxDate)) {
        // tryAgain(coords);
        logger.debug("{} {} failed on time: coord={} gbx = {}", kind, name, time_val, gbxDate);
      }
      Assert.assertEquals(time_val, gbxDate);
    }

    // check vert
    boolean vertOk = true;
    Double vert_val = coords.getVertPoint();
    Grib1ParamLevel plevel = cust1.getParamLevel(gr1.getPDSsection());
    if (cust1.isLayer(plevel.getLevelType())) {
      CoordInterval edge = coords.getVertIntv();
      if (edge != null) {
        // double low = Math.min(edge[0], edge[1]);
        // double hi = Math.max(edge[0], edge[1]);
        vertOk &= Misc.nearlyEquals(edge.start(), plevel.getValue1(), maxRelDiff);
        vertOk &= Misc.nearlyEquals(edge.end(), plevel.getValue2(), maxRelDiff);
        if (!vertOk) {
          // tryAgain(coords);
          logger.debug("{} {} failed on vert [{},{}] != [{},{}]", kind, name, edge.start(), edge.end(),
              plevel.getValue1(), plevel.getValue2());
        }
      }
    } else if (vert_val != null) {
      vertOk &= Misc.nearlyEquals(vert_val, plevel.getValue1(), maxRelDiff);
      if (!vertOk) {
        // tryAgain(coords);
        logger.debug("{} {} failed on vert {} != {}", kind, name, vert_val, plevel.getValue1());
      }
    }
  }

  public CalendarDate getReferenceDate(Grib1SectionProductDefinition pds) {
    return pds.getReferenceDate();
  }

  public CalendarDate getForecastDate(Grib1SectionProductDefinition pds, Grib1ParamTime ptime) {
    CalendarPeriod period = GribUtils.getCalendarPeriod(pds.getTimeUnit());
    CalendarDateUnit unit = CalendarDateUnit.of(period.getField(), false, pds.getReferenceDate());
    int timeCoord = ptime.getForecastTime();
    return unit.makeCalendarDate(period.getValue() * timeCoord);
  }

  public CalendarDate[] getForecastInterval(Grib1SectionProductDefinition pds, Grib1ParamTime ptime) {
    CalendarPeriod period = GribUtils.getCalendarPeriod(pds.getTimeUnit());
    CalendarDateUnit unit = CalendarDateUnit.of(period.getField(), false, pds.getReferenceDate());
    int[] intv = ptime.getInterval();
    return new CalendarDate[] {unit.makeCalendarDate(period.getValue() * intv[0]),
        unit.makeCalendarDate(period.getValue() * intv[1])};
  }

  //////////////////////////////////////////////////////////////
  // Grib 2

  private Map<String, IdxHashGrib2> fileIndexMapGrib2 = new HashMap<>();

  private class IdxHashGrib2 {
    private Map<Long, Grib2Record> map = new HashMap<>();

    IdxHashGrib2(String idxFile) throws IOException {
      Grib2Index index = new Grib2Index();
      if (!index.readIndex(idxFile, -1, CollectionUpdateType.never)) {
        logger.debug("idxFile does not exist {}", idxFile);
        throw new FileNotFoundException();
      }
      for (Grib2Record gr : index.getRecords()) {
        long startPos = gr.getIs().getStartPos();
        map.put(startPos, gr); // start of entire message
      }
    }

    Grib2Record get(long pos) {
      return map.get(pos);
    }
  }

  private void readAndTestGrib2(String name, GridSubset coords) throws IOException {
    GribCollectionImmutable.Record dr = GribArrayReader.currentDataRecord;
    String filename = GribArrayReader.currentDataRafFilename;
    if (dr == null) {
      counters.count(kind, "missing2");
      return;
    }
    counters.count(kind, "found2");

    String idxFile = filename.endsWith(".gbx9") ? filename : filename + ".gbx9";
    IdxHashGrib2 idxHash = fileIndexMapGrib2.get(idxFile);
    if (idxHash == null) {
      idxHash = new IdxHashGrib2(idxFile);
      fileIndexMapGrib2.put(idxFile, idxHash);
    }

    Grib2Record grib2 = idxHash.get(dr.pos);
    Grib2Tables cust = Grib2Tables.factory(grib2);

    Grib2RecordBean bean = new Grib2RecordBean(cust, grib2);
    boolean paramOk = true;

    // paramOk &= var_desc.equals(bean.getName());
    // paramOk &= var_param.equals(bean.getParamNo());
    // paramOk &= var_level_type.equals(bean.getLevelName());

    CalendarDate rt_val = coords.getRunTime();
    boolean runtimeOk = true;
    if (rt_val != null) {
      CalendarDate gribDate = bean.getRefDate();
      runtimeOk &= rt_val.equals(gribDate);
      if (!runtimeOk) {
        // tryAgain(coords);
        logger.debug("{} {} failed on runtime {} != {}", kind, name, rt_val, gribDate);
      }
    }

    CalendarDate time_val = coords.getTime();
    if (time_val == null) {
      time_val = coords.getTimeOffsetDate();
    }

    boolean timeOk = true;
    if (bean.isTimeInterval()) {
      TimeCoordIntvDateValue dateFromGribRecord = bean.getTimeIntervalDates();
      CalendarDateRange date_bounds = coords.getTimeRange();
      if (date_bounds == null) {
        date_bounds = makeDateBounds(coords, rt_val);
      }
      if (date_bounds != null) {
        timeOk &= nearlyEquals(date_bounds.getStart(), dateFromGribRecord.getStart());
        timeOk &= nearlyEquals(date_bounds.getEnd(), dateFromGribRecord.getEnd());
      }
      if (!timeOk) {
        // tryAgain(coords);
        logger.debug("{} {} failed on timeIntv [{}] != {}", kind, name, date_bounds, bean.getTimeCoord());
      }

    } else if (time_val != null) {
      // timeOk &= timeCoord == bean.getTimeCoordValue(); // true if GC
      CalendarDate dateFromGribRecord = bean.getForecastDate();
      timeOk &= nearlyEquals(time_val, dateFromGribRecord);
      if (!timeOk) {
        // tryAgain(coords);
        logger.debug("{} {} failed on time {} != {}", kind, name, time_val, bean.getTimeCoord());
      }
    }

    Double vert_val = coords.getVertPoint();
    CoordInterval edge = coords.getVertIntv();
    boolean vertOk = true;
    if (edge != null) {
      vertOk &= bean.isLayer();
      double low = Math.min(edge.start(), edge.end());
      double hi = Math.max(edge.start(), edge.end());
      vertOk &= Misc.nearlyEquals(low, bean.getLevelLowValue(), maxRelDiff);
      vertOk &= Misc.nearlyEquals(hi, bean.getLevelHighValue(), maxRelDiff);
      if (!vertOk) {
        // tryAgain(coords);
        logger.debug("{} {} failed on vert [{},{}] != [{},{}]", kind, name, low, hi, bean.getLevelLowValue(),
            bean.getLevelHighValue());
      }
    } else if (vert_val != null) {
      vertOk &= Misc.nearlyEquals(vert_val, bean.getLevelValue1(), maxRelDiff);
      if (!vertOk) {
        // tryAgain(coords);
        logger.debug("{} {} failed on vert {} != {}", kind, name, vert_val, bean.getLevelValue1());
      }
    }

    boolean ok = paramOk && runtimeOk && timeOk && vertOk;
    Assert.assertTrue(ok);
  }

  /*
   * void tryAgain(GridSubset coords) {
   * try {
   * if (cover != null)
   * cover.readData(coords);
   * 
   * if (grid != null) {
   * int rtIndex = (Integer) dtCoords.get("rtIndex");
   * int tIndex = (Integer) dtCoords.get("tIndex");
   * int zIndex = (Integer) dtCoords.get("zIndex");
   * grid.readDataSlice(rtIndex, -1, tIndex, zIndex, -1, -1);
   * }
   * 
   * } catch (InvalidRangeException e) {
   * e.printStackTrace();
   * } catch (IOException e) {
   * e.printStackTrace();
   * }
   * 
   * }
   */

  public class Grib2RecordBean {
    Grib2Tables cust;
    Grib2Record gr;
    Grib2Pds pds;
    int discipline;

    public Grib2RecordBean() {}

    public Grib2RecordBean(Grib2Tables cust, Grib2Record gr) throws IOException {
      this.cust = cust;
      this.gr = gr;
      this.pds = gr.getPDS();
      discipline = gr.getDiscipline();
    }

    public String getParamNo() {
      return discipline + "-" + pds.getParameterCategory() + "-" + pds.getParameterNumber();
    }

    public String getName() {
      return cust.getVariableName(gr);
    }

    public String getLevelName() {
      return cust.getLevelName(pds.getLevelType1());
    }

    public final CalendarDate getRefDate() {
      return gr.getReferenceDate();
    }

    public boolean isLayer() {
      return cust.isLayer(pds);
    }

    public final CalendarDate getForecastDate() {
      return cust.getForecastDate(gr);
    }

    public String getTimeCoord() {
      if (isTimeInterval())
        return getTimeIntervalDates().toString();
      else
        return getForecastDate().toString();
    }

    public final String getTimeUnit() {
      int unit = pds.getTimeUnit();
      return cust.getCodeTableValue("4.4", unit);
    }

    public final int getForecastTime() {
      return pds.getForecastTime();
    }

    public String getLevel() {
      int v1 = pds.getLevelType1();
      int v2 = pds.getLevelType2();
      if (v1 == 255)
        return "";
      if (v2 == 255)
        return "" + pds.getLevelValue1();
      if (v1 != v2)
        return pds.getLevelValue1() + "-" + pds.getLevelValue2() + " level2 type= " + v2;
      return pds.getLevelValue1() + "-" + pds.getLevelValue2();
    }

    public double getLevelValue1() {
      return pds.getLevelValue1();
    }

    public double getLevelLowValue() {
      return Math.min(pds.getLevelValue1(), pds.getLevelValue2());
    }

    public double getLevelHighValue() {
      return Math.max(pds.getLevelValue1(), pds.getLevelValue2());
    }

    /////////////////////////////////////////////////////////////
    /// time intervals

    public boolean isTimeInterval() {
      return pds instanceof Grib2Pds.PdsInterval;
    }

    @Nullable
    public TimeCoordIntvDateValue getTimeIntervalDates() {
      if (cust != null && isTimeInterval()) {
        return cust.getForecastTimeInterval(gr);
      }
      return null;
    }

    @Override
    public String toString() {
      final Formatter sb = new Formatter();
      sb.format("Record dataStart=%s%n", gr.getDataSection().getStartingPosition());
      sb.format(" %s (%s)%n", getName(), getParamNo());
      sb.format(" reftime=%s%n", getRefDate());
      sb.format(" time=%s%n", getTimeCoord());
      sb.format(" level=%s type=%s (%d)%n", getLevel(), getLevelName(), pds.getLevelType1());
      return sb.toString();
    }

    ///////////////////////////////
    // Ensembles
    public int getPertN() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationNumber();
      if (v == GribNumbers.UNDEFINED)
        v = -1;
      return v;
    }

    public int getNForecastsInEns() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getNumberEnsembleForecasts();
      if (v == GribNumbers.UNDEFINED)
        v = -1;
      return v;
    }

    public int getPertType() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationType();
      return (v == GribNumbers.UNDEFINED) ? -1 : v;
    }

    /////////////////////////////////
    // Probability

    public String getProbLimits() {
      Grib2Pds.PdsProbability pdsi = (Grib2Pds.PdsProbability) pds;
      double v = pdsi.getProbabilityLowerLimit();
      if (v == GribNumbers.UNDEFINEDD)
        return "";
      else
        return pdsi.getProbabilityLowerLimit() + "-" + pdsi.getProbabilityUpperLimit();
    }

  }

}
