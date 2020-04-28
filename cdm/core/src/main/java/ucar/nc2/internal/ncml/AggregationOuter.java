/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import thredds.inventory.MFile;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ProxyReader;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.CancelTask;

/**
 * Superclass for Aggregations on the outer dimension: joinNew, joinExisting, Fmrc, FmrcSingle
 *
 * @author caron
 * @since Aug 10, 2007
 */

abstract class AggregationOuter extends Aggregation implements ProxyReader {
  protected static boolean debugCache, debugInvocation, debugStride;
  public static int invocation; // debugging

  protected List<String> aggVarNames = new ArrayList<>(); // explicitly specified in the NcML
  protected List<VariableDS.Builder> aggVars = new ArrayList<>(); // actual vars that will be aggregated
  private int totalCoords; // the aggregation dimension size

  protected List<CacheVar> cacheList = new ArrayList<>(); // promote global attribute to variable
  protected boolean timeUnitsChange;

  /**
   * Create an Aggregation for the given NetcdfDataset.
   * The following addXXXX methods are called, then finish(), before the object is ready for use.
   *
   * @param ncd Aggregation belongs to this NetcdfDataset
   * @param dimName the aggregation dimension name
   * @param type the Aggregation.Type
   * @param recheckS how often to check if files have changes
   */
  AggregationOuter(NetcdfDataset.Builder ncd, String dimName, Type type, String recheckS) {
    super(ncd, dimName, type, recheckS);
  }

  /**
   * Set if time units can change. Implies isDate
   *
   * @param timeUnitsChange true if time units can change
   */
  void setTimeUnitsChange(boolean timeUnitsChange) {
    this.timeUnitsChange = timeUnitsChange;
    if (timeUnitsChange)
      isDate = true;
  }


  /**
   * Add a name for a variableAgg element
   *
   * @param varName name of agg variable
   */
  public void addVariable(String varName) {
    aggVarNames.add(varName);
  }

  /**
   * Promote a global attribute to a variable
   *
   * @param varName name of agg variable
   * @param orgName name of global attribute, may be different from the variable name
   */
  void addVariableFromGlobalAttribute(String varName, String orgName) {
    cacheList.add(new PromoteVar(varName, orgName));
  }

  /**
   * Promote a global attribute to a variable
   *
   * @param varName name of agg variable
   * @param format java.util.Format string
   * @param gattNames space delimited list of global attribute names
   */
  void addVariableFromGlobalAttributeCompose(String varName, String format, String gattNames) {
    cacheList.add(new PromoteVarCompose(varName, format, gattNames));
  }

  /**
   * Cache a variable (for efficiency).
   * Useful for Variables that are used a lot, and not too large, like coordinate variables.
   *
   * @param varName name of variable to cache. must exist.
   * @param dtype datatype of variable
   */
  void addCacheVariable(String varName, DataType dtype) {
    if (findCacheVariable(varName) != null)
      return; // no duplicates
    cacheList.add(new CacheVar(varName, dtype));
  }

  CacheVar findCacheVariable(String varName) {
    for (CacheVar cv : cacheList)
      if (cv.varName.equals(varName))
        return cv;
    return null;
  }

  /**
   * Get the list of aggregation variable names: variables whose data spans multiple files.
   * For type joinNew only.
   *
   * @return the list of aggregation variable names
   */
  List<String> getAggVariableNames() {
    return aggVarNames;
  }

  protected void buildCoords(CancelTask cancelTask) throws IOException {
    List<AggDataset> nestedDatasets = getDatasets();

    if (type == Type.forecastModelRunCollection) {
      for (AggDataset nested : nestedDatasets) {
        AggDatasetOuter dod = (AggDatasetOuter) nested;
        dod.ncoord = 1;
      }
    }

    totalCoords = 0;
    for (AggDataset nested : nestedDatasets) {
      AggDatasetOuter dod = (AggDatasetOuter) nested;
      totalCoords += dod.setStartEnd(totalCoords, cancelTask);
    }
  }

  // time units change - must read in time coords and convert, cache the results
  // must be able to be made into a CoordinateAxis1DTime
  // calendars must be equivalent
  protected void readTimeCoordinates(Variable.Builder timeAxis, CancelTask cancelTask) throws IOException {
    List<CalendarDate> dateList = new ArrayList<>();
    String timeUnits = null;
    Calendar calendar = null;
    Calendar calendarToCheck = null;
    CalendarDateUnit calendarDateUnit;

    // make concurrent
    for (AggDataset dataset : getDatasets()) {
      try (NetcdfFile ncfile = dataset.acquireFile(cancelTask)) {
        // LOOK was Variable v = ncfile.findVariable(timeAxis.getFullNameEscaped());
        Variable v = ncfile.findVariable(timeAxis.shortName);
        if (v == null) {
          logger.warn(
              "readTimeCoordinates: variable = " + timeAxis.shortName + " not found in file " + dataset.getLocation());
          return;
        }
        VariableDS vds = (v instanceof VariableDS) ? (VariableDS) v : new VariableDS(null, v, true);
        // LOOK was CoordinateAxis1DTime timeCoordVar = CoordinateAxis1DTime.factory(ncDataset, vds, null);
        CoordinateAxis1DTime timeCoordVar = CoordinateAxis1DTime.factory(null, vds, null);
        dateList.addAll(timeCoordVar.getCalendarDates());

        // if timeUnits is null, then that is our signal in the code that
        // we are on the first file of the aggregation
        if (timeUnits == null) {
          timeUnits = v.getUnitsString();
          // time units might be null. Check before moving on, and, if so, throw runtime error
          if (timeUnits != null) {
            calendar = timeCoordVar.getCalendarFromAttribute();
          } else {
            String msg = String.format("Time coordinate %s must have a non-null unit attribute.", timeAxis.shortName);
            logger.error(msg);
            if (cancelTask != null) {
              cancelTask.setError(msg);
            }
            throw new UnsupportedOperationException(msg);
          }
        } else {
          // Aggregation only makes sense if all files use the same calendar.
          // This block does take into account the same calendar might have
          // different names (i.e. "all_leap" and "366_day" are the same calendar)
          // and we will allow that in the aggregation.
          // If first file in the aggregation was not defined, it also must be
          // not defined in the other files.
          calendarToCheck = timeCoordVar.getCalendarFromAttribute();
          if (!calendarsEquivalent(calendar, calendarToCheck)) {
            String msg = String.format(
                "Inequivalent calendars found across the aggregation: calendar %s is not equivalent to %s.", calendar,
                calendarToCheck);
            logger.error(msg);
            if (cancelTask != null) {
              cancelTask.setError(msg);
            }
            throw new UnsupportedOperationException(msg);
          }
        }
      }

      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }
    }

    // int[] shape = timeAxis.getShapeAsSection().getShape();
    // int ntimes = shape[0];
    // assert (ntimes == dateList.size());
    // LOOK: used to get the shape from the commented code above
    // Now that using builders, and now that timeAxis isn't built yet, it has no shape.
    // Might not be right
    int[] shape = {dateList.size()};

    DataType coordType = (timeAxis.dataType == DataType.STRING) ? DataType.STRING : DataType.DOUBLE;
    Array timeCoordVals = Array.factory(coordType, shape);
    IndexIterator ii = timeCoordVals.getIndexIterator();

    // check if its a String or a udunit
    if (timeAxis.dataType == DataType.STRING) {
      for (CalendarDate date : dateList) {
        ii.setObjectNext(date.toString());
      }
    } else {
      timeAxis.setDataType(DataType.DOUBLE); // otherwise fractional values get lost
      // if calendar is null, maintain the null for the string name, and let
      // CalendarDateUnit handle it.
      String calendarName = calendar != null ? calendar.name() : null;
      calendarDateUnit = CalendarDateUnit.of(calendarName, timeUnits);
      timeAxis.addAttribute(new Attribute(CDM.UNITS, calendarDateUnit.getUdUnit()));
      timeAxis.addAttribute(new Attribute(CF.CALENDAR, calendarDateUnit.getCalendar().name()));
      for (CalendarDate date : dateList) {
        double val = calendarDateUnit.makeOffsetFromRefDate(date);
        ii.setDoubleNext(val);
      }
    }

    timeAxis.setCachedData(timeCoordVals, false);
  }

  // Check if two calendars are equivalent, while allowing one or both to be null.
  // in this case, two null calendars are considered equivalent
  private boolean calendarsEquivalent(Calendar a, Calendar b) {
    boolean equivalent = false;
    if (a != null) {
      // calendar from new file must not be null
      if (b != null) {
        // is calendar from new file the same as the first file in the aggregation?
        equivalent = b.equals(a);
      }
    } else {
      // if calendar attribute is missing from the first file in the aggregation,
      // it must be missing from the new file in order for the calendars to be
      // considered "equivalent"
      equivalent = b == null;
    }
    return equivalent;
  }

  protected int getTotalCoords() {
    return totalCoords;
  }

  protected void promoteGlobalAttributes(AggDatasetOuter typicalDataset) throws IOException {

    for (CacheVar cv : cacheList) {
      if (!(cv instanceof PromoteVar))
        continue;
      PromoteVar pv = (PromoteVar) cv;

      Array data = pv.read(typicalDataset);
      if (data == null)
        throw new IOException("cant read " + typicalDataset);

      pv.dtype = DataType.getType(data);
      VariableDS.Builder promotedVar = VariableDS.builder().setName(pv.varName).setDataType(pv.dtype)
          .setParentGroupBuilder(ncDataset.rootGroup).setDimensionsByName(dimName);
      /*
       * if (data.getSize() > 1) { // LOOK case of non-scalar global attribute not dealt with
       * Dimension outer = ncDataset.getRootGroup().findDimension(dimName);
       * Dimension inner = new Dimension("", (int) data.getSize(), false); //anonymous
       * List<Dimension> dims = new ArrayList<Dimension>(2);
       * dims.add(outer);
       * dims.add(inner);
       * promotedVar.setDimensions(dims);
       * }
       */

      ncDataset.rootGroup.addVariable(promotedVar);
      promotedVar.setProxyReader(this);
      promotedVar.setSPobject(pv);
    }
  }

  /*
   * protected void rebuildDataset() throws IOException {
   * buildCoords(null);
   * Group.Builder rootGroup = ncDataset.rootGroup;
   * 
   * // reset dimension length
   * rootGroup.resetDimensionLength(dimName, getTotalCoords());
   * 
   * // reset coordinate var
   * Variable.Builder joinAggCoord = rootGroup.findVariable(dimName).orElseThrow(IllegalStateException::new);
   * joinAggCoord.setDimensionsByName(dimName); // reset its dimension LOOK is this needed?
   * joinAggCoord.resetCache(); // get rid of any cached data, since its now wrong
   * 
   * // reset agg variables LOOK is this needed?
   * for (Variable.Builder aggVar : aggVars) {
   * // aggVar.setDimensions(dimName); // reset its dimension
   * aggVar.resetDimensions(); // reset its dimensions
   * aggVar.invalidateCache(); // get rid of any cached data, since its now wrong
   * }
   * 
   * // reset the typical dataset, where non-agg variables live
   * AggDataset typicalDataset = getTypicalDataset();
   * for (Variable.Builder var : rootGroup.vbuilders) {
   * if (aggVars.contains(var) || dimName.equals(var.shortName))
   * continue;
   * AggProxyReader proxy = new AggProxyReader(typicalDataset);
   * var.setProxyReader(proxy);
   * }
   * 
   * // reset cacheVars
   * for (CacheVar cv : cacheList) {
   * cv.reset();
   * }
   * 
   * if (timeUnitsChange) {
   * readTimeCoordinates(joinAggCoord, null);
   * }
   * }
   */

  /////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read a section of an aggregation variable.
   *
   * @param section read just this section of the data, array of Range
   * @return the data array section
   */
  @Override
  public Array reallyRead(Variable mainv, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {

    // public Array reallyRead(Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    if (debugConvert && mainv instanceof VariableDS) {
      DataType dtype = ((VariableDS) mainv).getOriginalDataType();
      if ((dtype != null) && (dtype != mainv.getDataType())) {
        logger.warn("Original type = {} mainv type= {}", dtype, mainv.getDataType());
      }
    }

    // If its full sized, then use full read, so that data gets cached.
    long size = section.computeSize();
    if (size == mainv.getSize())
      return reallyRead(mainv, cancelTask);

    // read the original type - if its been promoted to a new type, the conversion happens after this read
    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();

    // check if its cached
    Object spObj = mainv.getSPobject();
    if (spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      Array cacheArray = pv.read(section, cancelTask);
      return MAMath.convert(cacheArray, dtype); // // cache may keep data as different type
    }

    // the case of the agg coordinate var
    // if (mainv.getShortName().equals(dimName))
    // return readAggCoord(mainv, section, cancelTask);

    Array sectionData = Array.factory(dtype, section.getShape());
    int destPos = 0;

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedSection = new ArrayList<>(ranges); // get copy
    List<Range> innerSection = ranges.subList(1, ranges.size());

    if (debug)
      System.out.println("   agg wants range=" + mainv.getFullName() + "(" + joinRange + ")");

    // LOOK: could multithread here
    List<AggDataset> nestedDatasets = getDatasets();
    for (AggDataset nested : nestedDatasets) {
      AggDatasetOuter dod = (AggDatasetOuter) nested;
      Range nestedJoinRange = dod.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;

      Array varData;
      if ((type == Type.joinNew) || (type == Type.forecastModelRunCollection)) {
        varData = dod.read(mainv, cancelTask, innerSection);
      } else {
        nestedSection.set(0, nestedJoinRange);
        varData = dod.read(mainv, cancelTask, nestedSection);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      varData = MAMath.convert(varData, dtype); // just in case it need to be converted

      Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return sectionData;
  }

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   * This is an implementation of ProxyReader, so must fulfill that contract.
   *
   * @param mainv the aggregation variable
   */
  @Override
  public Array reallyRead(Variable mainv, CancelTask cancelTask) throws IOException {

    if (debugConvert && mainv instanceof VariableDS) {
      DataType dtype = ((VariableDS) mainv).getOriginalDataType();
      if ((dtype != null) && (dtype != mainv.getDataType())) {
        logger.warn("Original type = {} mainv type= {}", dtype, mainv.getDataType());
      }
    }

    // read the original type - if its been promoted to a new type, the conversion happens after this read
    DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();

    Object spObj = mainv.getSPobject();
    if (spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      try {
        Array cacheArray = pv.read(mainv.getShapeAsSection(), cancelTask);
        return MAMath.convert(cacheArray, dtype); // // cache may keep data as different type

      } catch (InvalidRangeException e) {
        logger.error("readAgg " + getLocation(), e);
        throw new IllegalArgumentException("readAgg " + getLocation(), e);
      }
    }

    // the case of the agg coordinate var
    // if (mainv.getShortName().equals(dimName))
    // return readAggCoord(mainv, cancelTask);

    Array allData = Array.factory(dtype, mainv.getShape());
    int destPos = 0;

    List<AggDataset> nestedDatasets = getDatasets();
    if (executor != null) {
      CompletionService<Result> completionService = new ExecutorCompletionService<>(executor);

      int count = 0;
      for (AggDataset vnested : nestedDatasets)
        completionService.submit(new ReaderTask(vnested, mainv, cancelTask, count++));

      try {
        int n = nestedDatasets.size();
        for (int i = 0; i < n; ++i) {
          Result r = completionService.take().get();
          if (r != null) {
            r.data = MAMath.convert(r.data, dtype); // just in case it needs to be converted
            int size = (int) r.data.getSize();
            Array.arraycopy(r.data, 0, allData, size * r.index, size);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throw new IOException(e.getMessage());
      }

    } else {

      for (AggDataset vnested : nestedDatasets) {
        Array varData = vnested.read(mainv, cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
        varData = MAMath.convert(varData, dtype); // just in case it need to be converted

        Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
        destPos += varData.getSize();
      }
    }

    return allData;
  }

  private static class ReaderTask implements Callable<Result> {
    AggDataset ds;
    Variable mainv;
    CancelTask cancelTask;
    int index;

    ReaderTask(AggDataset ds, Variable mainv, CancelTask cancelTask, int index) {
      this.ds = ds;
      this.mainv = mainv;
      this.cancelTask = cancelTask;
      this.index = index;
    }

    public Result call() throws Exception {
      Array data = ds.read(mainv, cancelTask);
      return new Result(data, index);
    }
  }

  private static class Result {
    Array data;
    int index;

    Result(Array data, int index) {
      this.data = data;
      this.index = index;
    }
  }

  @Override
  protected AggDataset makeDataset(String cacheName, String location, String id, String ncoordS, String coordValueS,
      String sectionSpec, EnumSet<NetcdfDataset.Enhance> enhance, ucar.nc2.util.cache.FileFactory reader) {
    return new AggDatasetOuter(this, cacheName, location, id, ncoordS, coordValueS, enhance, reader);
  }

  @Override
  protected AggDataset makeDataset(MFile dset) {
    return new AggDatasetOuter(this, dset);
  }

  /////////////////////////////////////////////
  // vars that should be cached across the agg for efficiency
  class CacheVar {
    String varName;
    DataType dtype;
    private Map<String, Array> dataMap = new HashMap<>();

    CacheVar(String varName, DataType dtype) {
      this.varName = varName;
      this.dtype = dtype;

      if (varName == null)
        throw new IllegalArgumentException("Missing variable name on cache var");
    }

    public String toString() {
      return varName + " (" + getClass().getName() + ")";
    }

    // clear out old stuff from the Hash, so it doesnt grow forever
    void reset() {
      Map<String, Array> newMap = new HashMap<>();
      for (AggDataset ds : datasets) {
        String id = ds.getId();
        Array data = dataMap.get(id);
        if (data != null)
          newMap.put(id, data);
      }
      dataMap = newMap;
    }

    // public access to the data
    Array read(Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      if (debugCache)
        System.out.println("caching " + varName + " section= " + section);
      Array allData = null;

      List<Range> ranges = section.getRanges();
      Range joinRange = section.getRange(0);
      Section innerSection = null;
      if (section.getRank() > 1) {
        innerSection = new Section(ranges.subList(1, ranges.size()));
      }

      // LOOK could make concurrent
      int resultPos = 0;
      List<AggDataset> nestedDatasets = getDatasets();
      for (AggDataset vnested : nestedDatasets) {
        AggDatasetOuter dod = (AggDatasetOuter) vnested;

        // can we skip ?
        Range nestedJoinRange = dod.getNestedJoinRange(joinRange);
        if (nestedJoinRange == null) {
          continue;
        }
        if (debugStride)
          System.out.printf("%d: %s [%d,%d) (%d) %f for %s%n", resultPos, nestedJoinRange, dod.aggStart, dod.aggEnd,
              dod.ncoord, dod.aggStart / 8.0, vnested.getLocation());
        Array varData = read(dod);
        if (varData == null)
          throw new IOException("cant read " + dod);

        // which subset do we want?
        // bit tricky - assume returned array's rank depends on type LOOK is this true?
        if (((type == Type.joinNew) || (type == Type.forecastModelRunCollection))
            && ((innerSection != null) && (varData.getSize() != innerSection.computeSize()))) {
          varData = varData.section(innerSection.getRanges());

        } else if ((innerSection == null) && (varData.getSize() != nestedJoinRange.length())) {
          List<Range> nestedSection = new ArrayList<>(ranges); // make copy
          nestedSection.set(0, nestedJoinRange);
          varData = varData.section(nestedSection);
        }

        // may not know the data type until now
        if (dtype == null)
          dtype = DataType.getType(varData);
        if (allData == null) {
          allData = Array.factory(dtype, section.getShape());
          if (debugStride)
            System.out.printf("total result section = %s (%d)%n", section, Index.computeSize(section.getShape()));
        }

        // copy to result array
        int nelems = (int) varData.getSize();
        Array.arraycopy(varData, 0, allData, resultPos, nelems);
        resultPos += nelems;

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
      }

      return allData;
    }

    protected void putData(String id, Array data) {
      dataMap.put(id, data);
    }

    protected Array getData(String id) {
      return dataMap.get(id);
    }

    // get the Array of data for this var in this dataset, use cache else acquire file and read
    protected Array read(AggDatasetOuter dset) throws IOException {

      Array data = getData(dset.getId());
      if (data != null)
        return data;
      if (type == Type.joinNew)
        return null; // ??

      try (NetcdfFile ncfile = dset.acquireFile(null)) {
        return read(dset, ncfile);
      }
    }

    // get the Array of data for this var in this dataset and open ncfile
    protected Array read(AggDatasetOuter dset, NetcdfFile ncfile) throws IOException {
      invocation++;

      Array data = getData(dset.getId());
      if (data != null)
        return data;
      if (type == Type.joinNew)
        return null;

      Variable v = ncfile.findVariable(varName);
      data = v.read();
      putData(dset.getId(), data);
      if (debugCache)
        System.out.println("caching " + varName + " complete data");
      return data;
    }
  }

  /////////////////////////////////////////////
  // data values might be specified by Dataset.coordValue
  class CoordValueVar extends CacheVar {
    String units;
    DateUnit du;

    CoordValueVar(String varName, DataType dtype, String units) {
      super(varName, dtype);
      this.units = units;
      try {
        du = new DateUnit(units);
      } catch (Exception e) {
        // ok to fail - may not be a time coordinate
      }
    }

    // these deal with possible setting of the coord values in the NcML
    protected Array read(AggDatasetOuter dset) throws IOException {
      Array data = readCached(dset);
      if (data != null)
        return data;
      return super.read(dset);
    }

    protected Array read(AggDatasetOuter dset, NetcdfFile ncfile) throws IOException {
      Array data = readCached(dset);
      if (data != null)
        return data;
      return super.read(dset, ncfile);
    }

    // only deals with possible setting of the coord values in the NcML
    private Array readCached(AggDatasetOuter dset) {
      Array data = getData(dset.getId());
      if (data != null)
        return data;

      data = Array.factory(dtype, new int[] {dset.ncoord});
      IndexIterator ii = data.getIndexIterator();

      if (dset.coordValueDate != null) { // its a date, typicallly parsed from the filename
        // we have the coordinates as a Date
        if (dtype == DataType.STRING) {
          ii.setObjectNext(dset.coordValue); // coordValueDate as a String, see setInfo()

        } else if (du != null) {
          double val = du.makeValue(dset.coordValueDate);
          ii.setDoubleNext(val);
        }

        putData(dset.getId(), data);
        return data;

      } else if (dset.coordValue != null) {
        // we have the coordinates as a String, typicallly entered in the ncML

        // if theres only one coord
        if (dset.ncoord == 1) {
          if (dtype == DataType.STRING) {
            ii.setObjectNext(dset.coordValue);
          } else {
            double val = Double.parseDouble(dset.coordValue);
            ii.setDoubleNext(val);
          }

        } else {

          // multiple coords
          int count = 0;
          StringTokenizer stoker = new StringTokenizer(dset.coordValue, " ,");
          while (stoker.hasMoreTokens()) {
            String toke = stoker.nextToken();

            // LOOK how come you dont have to check if this coordinate is contained ?
            // if (!nestedJoinRange.contains(count))

            if (dtype == DataType.STRING) {
              ii.setObjectNext(toke);
            } else {
              double val = Double.parseDouble(toke);
              ii.setDoubleNext(val);
            }
            count++;
          }

          if (count != dset.ncoord) {
            logger.error("readAggCoord incorrect number of coordinates dataset=" + dset.getLocation());
            throw new IllegalArgumentException(
                "readAggCoord incorrect number of coordinates dataset=" + dset.getLocation());
          }
        }

        putData(dset.getId(), data);
        return data;
      }

      return null;
    } // readCached
  }


  /////////////////////////////////////////////
  // global attributes promoted to variables
  class PromoteVar extends CacheVar {
    String gattName;

    protected PromoteVar(String varName, DataType dtype) {
      super(varName, dtype);
    }

    PromoteVar(String varName, String gattName) {
      super(varName, null);
      this.gattName = (gattName != null) ? gattName : varName;
    }

    @Override
    protected Array read(AggDatasetOuter dset, NetcdfFile ncfile) {
      Array data = getData(dset.getId());
      if (data != null)
        return data;

      Attribute att = ncfile.findGlobalAttribute(gattName);
      if (att == null)
        throw new IllegalArgumentException("Unknown attribute name= " + gattName);
      data = att.getValues();
      if (dtype == null)
        dtype = DataType.getType(data);

      if (dset.ncoord == 1) // LOOK ??
        putData(dset.getId(), data);
      else {
        // duplicate the value to each of the coordinates
        Array allData = Array.factory(dtype, new int[] {dset.ncoord});
        for (int i = 0; i < dset.ncoord; i++)
          Array.arraycopy(data, 0, allData, i, 1); // LOOK generalize to vectors ??
        putData(dset.getId(), allData);
        data = allData;
      }
      return data;
    }


  }

  /////////////////////////////////////////////
  // global attributes promoted to variables
  class PromoteVarCompose extends PromoteVar {
    String format;
    String[] gattNames;

    /**
     * @param varName name of agg variable
     * @param format java.util.Format string
     * @param gattNames space delimited list of global attribute names
     */
    PromoteVarCompose(String varName, String format, String gattNames) {
      super(varName, DataType.STRING);
      this.format = format;
      this.gattNames = gattNames.split(" ");

      if (format == null)
        throw new IllegalArgumentException("Missing format string (java.util.Formatter)");
    }

    @Override
    protected Array read(AggDatasetOuter dset, NetcdfFile ncfile) {
      Array data = getData(dset.getId());
      if (data != null)
        return data;

      List<Object> vals = new ArrayList<>();
      for (String gattName : gattNames) {
        Attribute att = ncfile.findGlobalAttribute(gattName);
        if (att == null)
          throw new IllegalArgumentException("Unknown attribute name= " + gattName);
        vals.add(att.getValue(0));
      }

      Formatter f = new Formatter();
      f.format(format, vals.toArray());
      String result = f.toString();

      Array allData = Array.factory(dtype, new int[] {dset.ncoord});
      for (int i = 0; i < dset.ncoord; i++)
        allData.setObject(i, result);
      putData(dset.getId(), allData);
      return allData;
    }

  }

  @Override
  public void getDetailInfo(Formatter f) {
    super.getDetailInfo(f);
    f.format("  timeUnitsChange=%s%n", timeUnitsChange);
    f.format("  totalCoords=%d%n", totalCoords);

    if (!aggVarNames.isEmpty()) {
      f.format("  Aggregation Variables specified in NcML%n");
      for (String vname : aggVarNames)
        f.format("   %s%n", vname);
    }

    f.format("%nAggregation Variables%n");
    for (VariableDS.Builder<?> vds : aggVars) {
      f.format("   %s %s%n", vds.shortName, String.join(",", vds.getDimensionNames()));
    }

    if (!cacheList.isEmpty()) {
      f.format("%nCache Variables%n");
      for (CacheVar cv : cacheList)
        f.format("   %s%n", cv);
    }

    f.format("%nVariable Proxies%n");
    for (Variable.Builder v : ncDataset.rootGroup.vbuilders) {
      f.format("   %20s proxy %s%n", v.shortName, v.proxyReader == null ? "" : v.proxyReader.getClass().getName());
    }


  }
}
