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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import thredds.inventory.MFile;
import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ProxyReader;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.internal.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;

/** Superclass for Aggregations on the outer dimension: joinNew, joinExisting */
abstract class AggregationOuter extends Aggregation implements ProxyReader {
  protected static boolean debugCache, debugInvocation, debugStride;
  public static int invocation; // debugging

  protected final List<String> aggVarNames = new ArrayList<>(); // explicitly specified in the NcML
  protected final List<VariableDS.Builder<?>> aggVars = new ArrayList<>(); // actual vars that will be aggregated
  private int totalCoords; // the aggregation dimension size

  protected final List<CacheVar> cacheList = new ArrayList<>(); // promote global attribute to variable
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
  AggregationOuter(NetcdfDataset.Builder<?> ncd, String dimName, Type type, String recheckS) {
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
  void addCacheVariable(String varName, ArrayType dtype) {
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

    totalCoords = 0;
    for (AggDataset nested : nestedDatasets) {
      AggDatasetOuter dod = (AggDatasetOuter) nested;
      totalCoords += dod.setStartEnd(totalCoords, cancelTask);
    }
  }

  // time units change - must read in time coords and convert, cache the results
  // must be able to be made into a CoordinateAxis1DTime
  // calendars must be equivalent
  protected void readTimeCoordinates(Variable.Builder<?> timeAxis, CancelTask cancelTask) throws IOException {
    List<CalendarDate> dateList = new ArrayList<>();
    String timeUnits = null;
    Calendar calendar = null;
    Calendar calendarToCheck;
    CalendarDateUnit calendarDateUnit;

    // make concurrent
    for (AggDataset dataset : getDatasets()) {
      try (NetcdfFile ncfile = dataset.acquireFile(cancelTask)) {
        Variable v = ncfile.findVariable(timeAxis.shortName);
        if (v == null) {
          logger.warn("readTimeCoordinates: variable = {} not found in file {}", timeAxis.shortName,
              dataset.getLocation());
          return;
        }
        VariableDS vds =
            (v instanceof VariableDS) ? (VariableDS) v : VariableDS.fromVar(ncfile.getRootGroup(), v, true);
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
            throw new UnsupportedOperationException(msg);
          }
        }
      }

      if (cancelTask != null && cancelTask.isCancel()) {
        return;
      }
    }

    // Now that using builders, and now that timeAxis isn't built yet, it has no shape.
    // Might not be right
    int[] shape = {dateList.size()};
    // check if its a String or a udunit
    if (timeAxis.dataType == ArrayType.STRING) {
      String[] sdata = new String[dateList.size()];
      int count = 0;
      for (CalendarDate date : dateList) {
        sdata[count++] = date.toString();
      }
      timeAxis.setSourceData(Arrays.factory(ArrayType.STRING, shape, sdata));
    } else {
      timeAxis.setArrayType(ArrayType.DOUBLE); // otherwise fractional values get lost
      calendarDateUnit = CalendarDateUnit.fromUdunitString(calendar, timeUnits).orElseThrow();
      timeAxis.addAttribute(new Attribute(CDM.UNITS, calendarDateUnit.toString()));
      timeAxis.addAttribute(new Attribute(CF.CALENDAR, calendarDateUnit.getCalendar().name()));
      double[] ddata = new double[dateList.size()];
      int count = 0;
      for (CalendarDate date : dateList) {
        ddata[count++] = calendarDateUnit.makeOffsetFromRefDate(date);
      }
      timeAxis.setSourceData(Arrays.factory(ArrayType.DOUBLE, shape, ddata));
    }
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

      Array<?> data = pv.read(typicalDataset);
      if (data == null)
        throw new IOException("cant read " + typicalDataset);

      pv.dtype = data.getArrayType();
      VariableDS.Builder<?> promotedVar = VariableDS.builder().setName(pv.varName).setArrayType(pv.dtype)
          .setParentGroupBuilder(ncDataset.rootGroup).setDimensionsByName(dimName);

      ncDataset.rootGroup.addVariable(promotedVar);
      promotedVar.setProxyReader(this);
      promotedVar.setSPobject(pv);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  private static class ReaderTask implements Callable<Result> {
    final AggDataset ds;
    final Variable mainv;
    final CancelTask cancelTask;
    final int index;

    ReaderTask(AggDataset ds, Variable mainv, CancelTask cancelTask, int index) {
      this.ds = ds;
      this.mainv = mainv;
      this.cancelTask = cancelTask;
      this.index = index;
    }

    public Result call() throws Exception {
      Array<?> data = ds.read(mainv, cancelTask);
      return new Result(data, index);
    }
  }

  private static class Result {
    final Array<?> data;
    final int index;

    Result(Array<?> data, int index) {
      this.data = data;
      this.index = index;
    }
  }

  @Override
  protected AggDataset makeDataset(String cacheName, String location, String id, String ncoordS, String coordValueS,
      String sectionSpec, EnumSet<NetcdfDataset.Enhance> enhance, ucar.nc2.internal.cache.FileFactory reader) {
    return new AggDatasetOuter(this, cacheName, location, id, ncoordS, coordValueS, enhance, reader);
  }

  @Override
  protected AggDataset makeDataset(MFile dset) {
    return new AggDatasetOuter(this, dset);
  }

  /////////////////////////////////////////////
  // vars that should be cached across the agg for efficiency
  class CacheVar {
    final String varName;
    ArrayType dtype;
    private Map<String, Array<?>> dataMap = new HashMap<>();

    CacheVar(String varName, ArrayType dtype) {
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
      Map<String, Array<?>> newMap = new HashMap<>();
      for (AggDataset ds : datasets) {
        String id = ds.getId();
        Array<?> data = dataMap.get(id);
        if (data != null) {
          newMap.put(id, data);
        }
      }
      dataMap = newMap;
    }

    // public access to the data
    Array<?> read(Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      if (debugCache)
        System.out.println("caching " + varName + " section= " + section);

      List<Range> ranges = section.getRanges();
      Range joinRange = section.getRange(0);
      Section innerSection = null;
      if (section.getRank() > 1) {
        innerSection = new Section(ranges.subList(1, ranges.size()));
      }

      List<Array<?>> dataArrays = new ArrayList<>();
      List<AggDataset> nestedDatasets = getDatasets();
      for (AggDataset vnested : nestedDatasets) {
        AggDatasetOuter dod = (AggDatasetOuter) vnested;

        // can we skip ?
        Range nestedJoinRange = dod.getNestedJoinRange(joinRange);
        if (nestedJoinRange == null) {
          continue;
        }
        Array<?> varData = read(dod);
        if (varData == null) {
          throw new IOException("cant read " + dod);
        }

        // which subset do we want?
        // bit tricky - assume returned array's rank depends on type
        if ((type == Type.joinNew) && ((innerSection != null) && (varData.getSize() != innerSection.computeSize()))) {
          varData = Arrays.section(varData, innerSection);

        } else if ((innerSection == null) && (varData.getSize() != nestedJoinRange.length())) {
          List<Range> nestedSection = new ArrayList<>(ranges); // make copy
          nestedSection.set(0, nestedJoinRange);
          varData = Arrays.section(varData, new Section(nestedSection));
        }

        // may not know the data type until now
        if (dtype == null) {
          dtype = varData.getArrayType();
        }

        dataArrays.add(varData);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
      }

      return Arrays.combine(dtype, section.getShape(), dataArrays);
    }

    protected void putCachedData(String id, Array<?> data) {
      dataMap.put(id, data);
    }

    protected Array<?> getCachedData(String id) {
      return dataMap.get(id);
    }

    // get the Array of data for this var in this dataset, use cache else acquire file and read
    protected Array<?> read(AggDatasetOuter dset) throws IOException {

      Array<?> data = getCachedData(dset.getId());
      if (data != null)
        return data;
      if ((type == Type.joinNew) && !(this instanceof PromoteVar)) {
        return null; // ??
      }

      try (NetcdfFile ncfile = dset.acquireFile(null)) {
        return read(dset, ncfile);
      }
    }

    // get the Array of data for this var in this dataset and open ncfile
    protected Array<?> read(AggDatasetOuter dset, NetcdfFile ncfile) throws IOException {
      invocation++;

      Array<?> data = getCachedData(dset.getId());
      if (data != null)
        return data;
      if (type == Type.joinNew)
        return null;

      Variable v = ncfile.findVariable(varName);
      data = v.readArray();
      putCachedData(dset.getId(), data);
      if (debugCache)
        System.out.println("caching " + varName + " complete data");
      return data;
    }
  }

  /////////////////////////////////////////////
  // data values might be specified by Dataset.coordValue
  class CoordValueVar extends CacheVar {
    final String units;
    @Nullable
    final CalendarDateUnit du;

    CoordValueVar(String varName, ArrayType dtype, String units) {
      super(varName, dtype);
      this.units = units;
      // TODO we should look for calendar attribute
      this.du = CalendarDateUnit.fromUdunitString(null, units).orElse(null);
    }

    // these deal with possible setting of the coord values in the NcML
    protected Array<?> read(AggDatasetOuter dset) throws IOException {
      Array<?> data = readCached(dset);
      if (data != null) {
        return data;
      }
      return super.read(dset);
    }

    protected Array<?> read(AggDatasetOuter dset, NetcdfFile ncfile) throws IOException {
      Array<?> data = readCached(dset);
      if (data != null) {
        return data;
      }
      return super.read(dset, ncfile);
    }

    // only deals with possible setting of the coord values in the NcML
    private Array<?> readCached(AggDatasetOuter dset) {
      Array<?> cachedData = getCachedData(dset.getId());
      if (cachedData != null) {
        return cachedData;
      }

      // TODO this doesnt make any sense. Maybe the cached data is mutable in old version, and we
      // lazily add the coordinate values? Or ncoord == 1 ?
      Object data = null;
      if (dset.coordValueDate != null) { // its a date, typicallly parsed from the filename
        // we have the coordinates as a Date
        if (dtype == ArrayType.STRING) {
          String[] sdata = new String[1];
          sdata[0] = dset.coordValue; // coordValueDate as a String, see setInfo()
          data = sdata;

        } else if (du != null) {
          double[] ddata = new double[1];
          ddata[0] = du.makeOffsetFromRefDate(dset.coordValueDate);
          data = ddata;
        }

        Array<?> array = Arrays.factory(dtype, new int[] {dset.ncoord}, data);
        putCachedData(dset.getId(), array);
        return array;

      } else if (dset.coordValue != null) {
        // we have the coordinates as a String, typicallly entered in the ncML

        // if theres only one coord
        if (dset.ncoord == 1) {
          if (dtype == ArrayType.STRING) {
            String[] sdata = new String[1];
            sdata[0] = dset.coordValue; // coordValueDate as a String, see setInfo()
            data = sdata;
          } else {
            double[] ddata = new double[1];
            ddata[0] = Double.parseDouble(dset.coordValue);
            data = ddata;
          }

        } else {
          // multiple coords
          List<String> tokens =
              Splitter.on(CharMatcher.anyOf(" ,")).omitEmptyStrings().trimResults().splitToList(dset.coordValue);
          if (tokens.size() != dset.ncoord) {
            logger.error("readAggCoord incorrect number of coordinates dataset=" + dset.getLocation());
            throw new IllegalArgumentException(
                "readAggCoord incorrect number of coordinates dataset=" + dset.getLocation());
          }
          Array<?> array = Aggregations.makeArray(dtype, tokens);
          putCachedData(dset.getId(), array);
          return array;
        }

        Array<?> array = Arrays.factory(dtype, new int[] {dset.ncoord}, data);
        putCachedData(dset.getId(), array);
        return array;
      }

      return null;
    } // readCached
  }


  /////////////////////////////////////////////
  // global attributes promoted to variables
  class PromoteVar extends CacheVar {
    String gattName;

    protected PromoteVar(String varName, ArrayType dtype) {
      super(varName, dtype);
    }

    PromoteVar(String varName, String gattName) {
      super(varName, null);
      this.gattName = (gattName != null) ? gattName : varName;
    }

    @Override
    protected Array<?> read(AggDatasetOuter dset, NetcdfFile ncfile) {
      Array<?> data = getCachedData(dset.getId());
      if (data != null) {
        return data;
      }

      Attribute att = ncfile.findAttribute(gattName);
      if (att == null) {
        throw new IllegalArgumentException("Unknown attribute name= " + gattName);
      }
      data = att.getArrayValues();
      if (dtype == null) {
        dtype = data.getArrayType();
      }

      if (dset.ncoord == 1) {
        putCachedData(dset.getId(), data);
      } else {
        List<Array<?>> dataArrays = new ArrayList<>();
        // duplicate the value to each of the coordinates
        for (int i = 0; i < dset.ncoord; i++) {
          dataArrays.add(data);
        }
        int[] shape = data.getSize() == 1 ? new int[] {dset.ncoord} : new int[] {dset.ncoord, (int) data.getSize()};
        Array<?> allData = Arrays.combine(dtype, shape, dataArrays);
        putCachedData(dset.getId(), allData);
        data = allData;
      }
      return data;
    }

  } // PromoteVar

  /////////////////////////////////////////////
  // global attributes promoted to variables
  class PromoteVarCompose extends PromoteVar {
    final String format;
    final Iterable<String> gattNames;

    /**
     * @param varName name of agg variable
     * @param format java.util.Format string
     * @param gattNames space delimited list of global attribute names: each one is promoted
     */
    PromoteVarCompose(String varName, String format, String gattNames) {
      super(varName, ArrayType.STRING);
      this.format = format;
      this.gattNames = StringUtil2.split(gattNames);

      Preconditions.checkNotNull(format, "PromoteVarCompose: Missing format string");
    }

    @Override
    protected Array<?> read(AggDatasetOuter dset, NetcdfFile ncfile) {
      Array<?> cachedData = getCachedData(dset.getId());
      if (cachedData != null) {
        return cachedData;
      }

      List<Object> vals = new ArrayList<>();
      for (String gattName : gattNames) {
        Attribute att = ncfile.findAttribute(gattName);
        if (att == null) {
          throw new IllegalArgumentException("Unknown attribute name= " + gattName);
        }
        vals.add(att.getValue(0));
      }

      Formatter f = new Formatter();
      f.format(format, vals.toArray());
      String result = f.toString();

      // TODO we seem to be concatenating all the attribute values into a String.
      // then replicating that across all datasets. Why?
      String[] sarray = new String[dset.ncoord];
      // duplicate the value to each of the coordinates
      for (int i = 0; i < dset.ncoord; i++) {
        sarray[i] = result;
      }
      Array<?> allData = Arrays.factory(ArrayType.STRING, new int[] {dset.ncoord}, sarray);
      putCachedData(dset.getId(), allData);
      return allData;
    }

  } // PromoteVarCompose

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
    for (Variable.Builder<?> v : ncDataset.rootGroup.vbuilders) {
      f.format("   %20s proxy %s%n", v.shortName, v.proxyReader == null ? "" : v.proxyReader.getClass().getName());
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Array<?> proxyReadArray(Variable mainv, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {

    // public Array reallyRead(Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    if (debugConvert && mainv instanceof VariableDS) {
      ArrayType dtype = ((VariableDS) mainv).getOriginalArrayType();
      if ((dtype != null) && (dtype != mainv.getArrayType())) {
        logger.warn("Original type = {} mainv type= {}", dtype, mainv.getArrayType());
      }
    }

    // If its full sized, then use full read, so that data gets cached.
    long size = section.computeSize();
    if (size == mainv.getSize()) {
      return proxyReadArray(mainv, cancelTask);
    }

    // read the original type - if its been promoted to a new type, the conversion happens after this read
    ArrayType dtype =
        (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalArrayType() : mainv.getArrayType();

    // check if its cached
    Object spObj = mainv.getSPobject();
    if (spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      Array<?> cacheArray = pv.read(section, cancelTask);
      return Aggregations.convert(cacheArray, dtype); // // cache may keep data as different type
    }

    // the case of the agg coordinate var
    // if (mainv.getShortName().equals(dimName))
    // return readAggCoord(mainv, section, cancelTask);

    List<Range> ranges = section.getRanges();
    Range joinRange = section.getRange(0);
    List<Range> nestedRanges = new ArrayList<>(ranges);
    Section innerSection = new Section(ranges.subList(1, ranges.size()));

    List<Array<?>> arrayData = new ArrayList<>();
    List<AggDataset> nestedDatasets = getDatasets();
    for (AggDataset nested : nestedDatasets) {
      AggDatasetOuter dod = (AggDatasetOuter) nested;
      Range nestedJoinRange = dod.getNestedJoinRange(joinRange);
      if (nestedJoinRange == null)
        continue;

      Array<?> varData;
      if (type == Type.joinNew) {
        varData = dod.read(mainv, cancelTask, innerSection);
      } else {
        nestedRanges.set(0, nestedJoinRange);
        varData = dod.read(mainv, cancelTask, new Section(nestedRanges));
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      varData = Aggregations.convert(varData, dtype); // just in case it need to be converted
      arrayData.add(varData);
    }

    return Arrays.combine(dtype, section.getShape(), arrayData);
  }

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   * This is an implementation of ProxyReader, so must fulfill that contract.
   *
   * @param mainv the aggregation variable
   */
  @Override
  public Array<?> proxyReadArray(Variable mainv, CancelTask cancelTask) throws IOException {
    // read the original type - if its been promoted to a new type, the conversion happens after this read
    ArrayType dtype =
        (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalArrayType() : mainv.getArrayType();

    Object spObj = mainv.getSPobject();
    if (spObj instanceof CacheVar) {
      CacheVar pv = (CacheVar) spObj;
      try {
        Array<?> cacheArray = pv.read(mainv.getSection(), cancelTask);
        return Aggregations.convert(cacheArray, dtype); // // cache may keep data as different type

      } catch (InvalidRangeException e) {
        logger.error("readAgg " + getLocation(), e);
        throw new IllegalArgumentException("readAgg " + getLocation(), e);
      }
    }

    // the case of the agg coordinate var
    // if (mainv.getShortName().equals(dimName))
    // return readAggCoord(mainv, cancelTask);

    List<Array<?>> dataArrays = new ArrayList<>();
    List<AggDataset> nestedDatasets = getDatasets();
    if (executor != null) {
      CompletionService<Result> completionService = new ExecutorCompletionService<>(executor);

      int count = 0;
      for (AggDataset vnested : nestedDatasets) {
        completionService.submit(new ReaderTask(vnested, mainv, cancelTask, count++));
      }

      try {
        int n = nestedDatasets.size();
        for (int i = 0; i < n; ++i) {
          Result r = completionService.take().get();
          if (r != null) {
            dataArrays.set(r.index, Aggregations.convert(r.data, dtype)); // just in case it needs to be converted
          }
        }
        return Arrays.combine(dtype, mainv.getShape(), dataArrays);

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throw new IOException(e.getMessage());
      }
    }

    for (AggDataset vnested : nestedDatasets) {
      Array<?> varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel()) {
        return null;
      }
      dataArrays.add(Aggregations.convert(varData, dtype)); // just in case it need to be converted
    }
    // ArrayType dataType, int[] shape, List<Array<T>> dataArrays
    return Arrays.combine(dtype, mainv.getShape(), dataArrays);
  }
}
