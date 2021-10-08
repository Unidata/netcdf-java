/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.internal.util.Counters;
import ucar.nc2.util.Indent;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Both runtime and time coordinates are tracked here. The time coordinate is dependent on the runtime, at least on the
 * offset.
 * isOrthogonal means all time coordinate offsets are the same for each runtime.
 * isRegular means all time coordinate offsets are the same for each "runtime minute of day".
 *
 * @author caron
 * @since 1/22/14
 */
@Immutable
public class CoordinateTime2D extends CoordinateTimeAbstract implements Coordinate {
  private static final Logger logger = LoggerFactory.getLogger(CoordinateTime2D.class);

  private final CoordinateRuntime runtime;
  private final List<Coordinate> times; // nruns time coordinates - original offsets
  private final CoordinateTimeAbstract otime; // orthogonal time coordinates - only when isOrthogonal
  // only when isRegular: <minute of day, time coordinate>
  private final SortedMap<Integer, CoordinateTimeAbstract> regTimes;
  private final long[] offset; // list all offsets from the base/first runtime, length nruns, units ?

  private final boolean isRegular; // offsets are the same for each "runtime minute of day"
  private final boolean isOrthogonal; // offsets same for all runtimes, so 2d time array is (runtime X otime)
  private final boolean isTimeInterval;
  private final int nruns;
  private final int ntimes;

  private final List<Time2D> vals; // only present when building the GC, otherwise null

  /**
   * Ctor. Most general, a CoordinateTime for each runtime.
   *
   * @param code pdsFirst.getTimeUnit()
   * @param timeUnit time duration, based on code
   * @param vals complete set of Time2D values, may be null (used only during creation)
   * @param runtime list of runtimes
   * @param times list of times, one for each runtime, offsets reletive to its runtime, may not be null
   */
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime,
      List<Coordinate> times, int[] time2runtime) {
    super(code, timeUnit, runtime.getFirstDate(), time2runtime);

    this.runtime = runtime;
    this.times = Collections.unmodifiableList(times);
    this.otime = null;
    this.regTimes = null;
    this.isRegular = false;
    this.isOrthogonal = false;
    this.isTimeInterval = times.get(0) instanceof CoordinateTimeIntv;

    int nmax = 0;
    for (Coordinate time : times) {
      nmax = Math.max(nmax, time.getSize());
    }
    this.ntimes = nmax;
    this.nruns = runtime.getSize();
    assert nruns == times.size();

    this.offset = makeOffsets(times);
    this.vals = (vals == null) ? null : Collections.unmodifiableList(vals);
  }

  /**
   * Ctor. orthogonal - all offsets are the same for all runtimes, so 2d time array is (runtime X otime)
   *
   * @param code pdsFirst.getTimeUnit()
   * @param timeUnit time duration, based on code
   * @param vals complete set of Time2D values, may be null (used only during creation)
   * @param runtime list of runtimes
   * @param otime list of offsets, all the same for each runtime
   * @param times list of times, one for each runtime, offsets reletive to its runtime, may be null (Only available
   *        during creation, not stored in index)
   */
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime,
      CoordinateTimeAbstract otime, List<Coordinate> times, int[] time2runtime) {
    super(code, timeUnit, runtime.getFirstDate(), time2runtime);

    this.runtime = runtime;
    this.times = (times == null) ? null : Collections.unmodifiableList(times); // need these for makeBest

    this.otime = otime;
    this.isOrthogonal = true;
    this.isRegular = false;
    this.regTimes = null;
    this.isTimeInterval = otime instanceof CoordinateTimeIntv;
    this.ntimes = otime.getSize();

    this.nruns = runtime.getSize();
    this.offset = makeOffsets(timeUnit);
    this.vals = (vals == null) ? null : Collections.unmodifiableList(vals);
  }

  /**
   * Ctor. regular - all offsets are the same for each "runtime hour of day", eg all 0Z runtimes have the same offsets,
   * all 6Z runtimes have the same offsets, etc.
   * 2d time array is (runtime X otime(hour), where hour = runtime hour of day
   *
   * @param code pdsFirst.getTimeUnit()
   * @param timeUnit time duration, based on code
   * @param vals complete set of Time2D values, may be null (used only during creation)
   * @param runtime list of runtimes
   * @param regList list of offsets, one each for each possible runtime hour of day.
   * @param times list of times, one for each runtime, offsets reletive to its runtime, may be null (Only available
   *        during creation, not stored in index)
   */
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime,
      List<Coordinate> regList, List<Coordinate> times, int[] time2runtime) {
    super(code, timeUnit, runtime.getFirstDate(), time2runtime);

    this.runtime = runtime;
    this.nruns = runtime.getSize();

    this.times = (times == null) ? null : Collections.unmodifiableList(times); // need these for makeBest
    this.otime = null;
    this.isOrthogonal = false;
    this.isRegular = true;
    CoordinateTimeAbstract first = (CoordinateTimeAbstract) regList.get(0);
    this.isTimeInterval = first instanceof CoordinateTimeIntv;

    // regList may have different lengths
    int nmax = 0;
    for (Coordinate time : regList) {
      nmax = Math.max(nmax, time.getSize());
    }
    this.ntimes = nmax;

    // make the offset map
    this.regTimes = new TreeMap<>();
    for (Coordinate coord : regList) {
      CoordinateTimeAbstract time = (CoordinateTimeAbstract) coord;
      CalendarDate ref = time.getRefDate();
      int hour = ref.getHourOfDay();
      int min = ref.getMinuteOfHour();
      this.regTimes.put(hour * 60 + min, time);
    }

    this.offset = makeOffsets(timeUnit);
    this.vals = (vals == null) ? null : Collections.unmodifiableList(vals);
  }

  private long[] makeOffsets(List<Coordinate> orgTimes) {
    CalendarDate firstDate = runtime.getFirstDate();
    long[] offsets = new long[nruns];
    for (int idx = 0; idx < nruns; idx++) {
      CoordinateTimeAbstract coordTime = (CoordinateTimeAbstract) orgTimes.get(idx);
      CalendarPeriod period = coordTime.getTimeUnit(); // LOOK are we assuming all have same period ??
      offsets[idx] = (int) runtime.getRuntimeDate(idx).since(firstDate, period); // LOOK possible loss of precision
      // offsets[idx] = period.getOffset(firstDate, runtime.getRuntimeDate(idx)); // LOOK possible loss of precision
    }
    return offsets;
  }

  private long[] makeOffsets(CalendarPeriod period) {
    CalendarDate firstDate = runtime.getFirstDate();
    long[] offsets = new long[nruns];
    for (int idx = 0; idx < nruns; idx++) {
      offsets[idx] = runtime.getRuntimeDate(idx).since(firstDate, period); // LOOK possible loss of precision
      // offsets[idx] = period.getOffset(firstDate, runtime.getRuntimeDate(idx)); // LOOK possible loss of precision
    }
    return offsets;
  }

  @Override
  public CoordinateTimeAbstract setName(String name) {
    super.setName(name);
    if (isOrthogonal()) {
      otime.setName(name);
    } else if (isRegular()) {
      for (CoordinateTimeAbstract time : regTimes.values()) {
        time.setName(name);
      }
    } else {
      for (Coordinate time : times) {
        ((CoordinateTimeAbstract) time).setName(name);
      }
    }
    return this;
  }

  ///////////////////////////////////////////////////////

  public CoordinateRuntime getRuntimeCoordinate() {
    return runtime;
  }

  public boolean isTimeInterval() {
    return isTimeInterval;
  }

  public boolean isOrthogonal() {
    return isOrthogonal;
  }

  public boolean isRegular() {
    return isRegular;
  }

  public int getNtimes() {
    return ntimes;
  }

  public int getNruns() {
    return nruns;
  }

  public long getOffset(int runIndex) {
    return offset[runIndex];
  }

  public Iterable<Integer> getRegularMinuteOffsets() {
    if (!isRegular) {
      return null;
    }
    return regTimes.keySet();
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
    info.format(" %s runtime=%s nruns=%d ntimes=%d isOrthogonal=%s isRegular=%s%n", name, runtime.getName(), nruns,
        ntimes, isOrthogonal, isRegular);
    indent.incr();

    info.format("%sAll time values=", indent);
    List<?> timeValues = getOffsetsSorted();
    for (Object val : timeValues) {
      info.format(" %s,", val);
    }
    info.format(" (n=%d)%n%n", timeValues.size());

    if (isOrthogonal) {
      otime.showInfo(info, indent);
    }

    else if (isRegular)
      for (int minute : regTimes.keySet()) {
        CoordinateTimeAbstract timeCoord = regTimes.get(minute);
        String hourS = String.format("%d:%02d", minute / 60, minute % 60);
        info.format("%shour %s: ", indent, hourS);
        timeCoord.showInfo(info, new Indent(0));
      }

    else
      for (Coordinate time : times) {
        info.format("%s%s:", indent, ((CoordinateTimeAbstract) time).getRefDate());
        time.showInfo(info, new Indent(0));
      }
    indent.decr();
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("%s runtime=%s nruns=%d ntimes=%d isOrthogonal=%s isRegular=%s%n", name, runtime.getName(), nruns,
        ntimes, isOrthogonal, isRegular);

    if (isOrthogonal) {
      otime.showCoords(info);
    } else if (isRegular) {
      for (int minute : regTimes.keySet()) {
        CoordinateTimeAbstract timeCoord = regTimes.get(minute);
        String hourS = String.format("%d:%2d", minute / 60, minute % 60);
        info.format("hour %s: ", hourS);
        timeCoord.showInfo(info, new Indent(0));
      }
    } else {
      for (Coordinate time : times) {
        time.showCoords(info);
      }
    }
  }

  @Override
  public Counters calcDistributions() {
    Counters counters = new Counters();
    counters.add("resol");

    List<?> offsets = getOffsetsSorted();
    if (isTimeInterval()) {
      counters.add("intv");
      for (int i = 0; i < offsets.size(); i++) {
        TimeCoordIntvValue tinv = (TimeCoordIntvValue) offsets.get(i);
        int intv = tinv.getBounds2() - tinv.getBounds1();
        counters.count("intv", intv);
        if (i > 0) {
          int resol = tinv.getBounds1() - ((TimeCoordIntvValue) offsets.get(i - 1)).getBounds1();
          counters.count("resol", resol);
        }
      }

    } else {
      for (int i = 0; i < offsets.size() - 1; i++) {
        int diff = (Integer) offsets.get(i + 1) - (Integer) offsets.get(i);
        counters.count("resol", diff);
      }
    }

    return counters;
  }

  @Override
  public List<?> getValues() {
    return vals;
  }

  @Override
  public Object getValue(int idx) {
    return vals.get(idx);
  }

  @Override
  public int getIndex(Object val) {
    return (vals == null) ? -1 : Collections.binarySearch(vals, (Time2D) val);
  }

  @Override
  public int getSize() {
    return (vals == null) ? 0 : vals.size();
  }

  @Override
  public int getNCoords() {
    return getOffsetsSorted().size();
  }

  @Override
  public int estMemorySize() {
    return 864 + nruns * (48 + 4) + ntimes * 24; // nruns * (calendar date + integer) + ntimes + integer)
  }

  @Override
  public Type getType() {
    return Type.time2D;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CoordinateTime2D that = (CoordinateTime2D) o;
    if (isTimeInterval != that.isTimeInterval) {
      return false;
    }
    if (!runtime.equals(that.runtime)) {
      return false;
    }
    if (isOrthogonal != that.isOrthogonal) {
      return false;
    }
    if (isRegular != that.isRegular) {
      return false;
    }
    if (!Objects.equals(otime, that.otime)) {
      return false;
    }
    if (!Objects.equals(regTimes, that.regTimes)) {
      return false;
    }
    return Objects.equals(times, that.times);
  }

  @Override
  public int hashCode() {
    int result = runtime.hashCode();
    result = 31 * result + (times != null ? times.hashCode() : 0);
    result = 31 * result + (otime != null ? otime.hashCode() : 0);
    result = 31 * result + (regTimes != null ? regTimes.hashCode() : 0);
    result = 31 * result + (isRegular ? 1 : 0);
    result = 31 * result + (isOrthogonal ? 1 : 0);
    result = 31 * result + (isTimeInterval ? 1 : 0);
    return result;
  }

  /**
   * Check if all time intervals have the same length.
   * Only if isTimeInterval.
   * 
   * @return time interval name or MIXED_INTERVALS
   */
  @Nullable
  public String getTimeIntervalName() {
    if (!isTimeInterval) {
      return null;
    }

    if (isOrthogonal) {
      return ((CoordinateTimeIntv) otime).getTimeIntervalName();
    }

    if (isRegular) {
      String firstValue = null;
      for (CoordinateTimeAbstract timeCoord : regTimes.values()) {
        CoordinateTimeIntv timeCoordi = (CoordinateTimeIntv) timeCoord;
        String value = timeCoordi.getTimeIntervalName();
        if (firstValue == null) {
          firstValue = value;
        } else if (!value.equals(firstValue)) {
          return MIXED_INTERVALS;
        } else if (value.equals(MIXED_INTERVALS)) {
          return MIXED_INTERVALS;
        }
      }
      return firstValue;
    }

    // are they the same length ?
    String firstValue = null;
    for (Coordinate timeCoord : times) {
      if (times.isEmpty()) {
        continue; // skip empties
      }
      CoordinateTimeIntv timeCoordi = (CoordinateTimeIntv) timeCoord;
      String value = timeCoordi.getTimeIntervalName();
      if (firstValue == null) {
        firstValue = value;
      } else if (!value.equals(firstValue)) {
        return MIXED_INTERVALS;
      } else if (value.equals(MIXED_INTERVALS)) {
        return MIXED_INTERVALS;
      }
    }
    return firstValue;
  }

  @Override
  public CalendarDateRange makeCalendarDateRange() {
    CoordinateTimeAbstract firstCoord = getTimeCoordinate(0);
    CoordinateTimeAbstract lastCoord = getTimeCoordinate(nruns - 1);

    CalendarDateRange firstRange = firstCoord.makeCalendarDateRange();
    CalendarDateRange lastRange = lastCoord.makeCalendarDateRange();

    return CalendarDateRange.of(firstRange.getStart(), lastRange.getEnd());
  }

  ////////////////////////////////////////////////

  public CoordinateTimeAbstract getTimeCoordinate(int runIdx) {
    if (isOrthogonal) {
      // LOOK problem is cant use time.getRefDate(), must use time2D.getRefDate(runIdx) !!
      return factory(otime, getRefDate(runIdx));
    }

    if (isRegular) {
      CalendarDate ref = getRefDate(runIdx);
      int hour = ref.getHourOfDay();
      int min = ref.getMinuteOfHour();
      return regTimes.get(hour * 60 + min);
    }

    return (CoordinateTimeAbstract) times.get(runIdx);
  }

  public CoordinateTimeAbstract getRegularTimeCoordinateFromMinuteOfDay(int minute) {
    return regTimes.get(minute);
  }

  public CalendarDate getRefDate(int runIdx) {
    return runtime.getRuntimeDate(runIdx);
  }

  public long getRuntime(int runIdx) {
    return runtime.getRuntime(runIdx);
  }

  private CoordinateTimeAbstract factory(CoordinateTimeAbstract org, CalendarDate refDate) {
    CoordinateTimeAbstract result;
    if (isTimeInterval) {
      result = new CoordinateTimeIntv((CoordinateTimeIntv) org, refDate);
    } else {
      result = new CoordinateTime((CoordinateTime) org, refDate);
    }
    result.setName(org.getName());
    return result;
  }

  public CoordinateTimeAbstract getOrthogonalTimes() {
    return otime;
  }

  // For MRUTC, MRUTP
  public CoordinateTimeAbstract getOffsetTimes() {
    // LOOK assumes Point LOOK needs to be seconds, probably cant use this.offset ??
    List<Long> offsets = Arrays.stream(this.offset).boxed().collect(Collectors.toList());
    return new CoordinateTime(this.code, otime.timeUnit, otime.refDate, offsets, otime.time2runtime)
        .setName(this.getName());
  }

  // For TwoD, Regular, maximal time offset ??
  public CoordinateTimeAbstract getMaximalTimes() {
    // Kludge
    if (isRegular()) {
      return this.regTimes.values().stream().max(Comparator.comparingInt(CoordinateTimeAbstract::getNCoords))
          .orElseThrow();
    } else {
      return getTimeCoordinate(0); // LOOK can use this for all?
    }
  }

  /**
   * Get the time coordinate at the given indices, into the 2D time coordinate array
   *
   * @param runIdx run index
   * @param timeIdx time index
   * @return time coordinate
   */
  public Time2D getOrgValue(int runIdx, int timeIdx) {
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    CalendarDate runDate = runtime.getRuntimeDate(runIdx);
    if (isTimeInterval) {
      TimeCoordIntvValue valIntv = (TimeCoordIntvValue) time.getValue(timeIdx);
      if (valIntv == null)
        throw new IllegalArgumentException();
      return new Time2D(runDate, null, valIntv);
    } else {
      Long val = (Long) time.getValue(timeIdx);
      if (val == null) {
        throw new IllegalArgumentException();
      }
      return new Time2D(runDate, val, null);
    }
  }

  /**
   * find the run and time indexes of want
   * the inverse of getOrgValue
   * 
   * @param want find time coordinate that matches
   * @param wholeIndex return index here
   */
  public boolean getIndex(Time2D want, int[] wholeIndex) {
    int runIdx = runtime.getIndex(want.refDate);
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    wholeIndex[0] = runIdx;
    if (isTimeInterval) {
      wholeIndex[1] = time.getIndex(want.tinv);
    } else {
      wholeIndex[1] = time.getIndex(want.time);
    }
    return (wholeIndex[0] >= 0) && (wholeIndex[1] >= 0);
  }

  /**
   * Find the index matching a runtime and time coordinate
   * 
   * @param runIdx which run
   * @param value time coordinate
   * @return index in the time coordinate of the value
   */
  public int matchTimeCoordinate(int runIdx, CoordinateTime2D.Time2D value) {
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    int offset = (int) value.getRefDate().since(getRefDate(runIdx), timeUnit);
    // int offset = timeUnit.getOffset(getRefDate(runIdx), value.getRefDate());

    Object valueWithOffset;
    if (isTimeInterval) {
      valueWithOffset = value.tinv.offset(offset);
    } else {
      valueWithOffset = value.time + offset;
    }
    int result = time.getIndex(valueWithOffset);
    if (Grib.debugRead) {
      logger.debug(String.format("  matchTimeCoordinate value wanted = (%s) valueWithOffset=%s result=%d %n", value,
          valueWithOffset, result));
    }

    return result;
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  protected CoordinateTimeAbstract makeBestFromComplete(int[] best, int n) {
    throw new UnsupportedOperationException();
  }

  public CoordinateTimeAbstract makeTime2Runtime(CoordinateRuntime master) {
    if (isTimeInterval) {
      return makeBestTimeIntv(master);
    } else {
      return makeBestTime(master);
    }
  }

  public CoordinateTimeAbstract makeBestTimeCoordinate(CoordinateRuntime master) {
    if (isTimeInterval) {
      return makeBestTimeIntv(master);
    } else {
      return makeBestTime(master);
    }
  }

  private CoordinateTimeAbstract makeBestTime(CoordinateRuntime master) {
    // make complete, unique set of coordinates
    Set<Long> values = new HashSet<>(); // complete set of values
    // use times array, passed into constructor, with original inventory, if possible
    for (int runIdx = 0; runIdx < nruns; runIdx++) {
      CoordinateTime timeCoord =
          (times == null) ? (CoordinateTime) getTimeCoordinate(runIdx) : (CoordinateTime) times.get(runIdx);
      for (Long offset : timeCoord.getOffsetSorted()) {
        values.add(offset + getOffset(runIdx));
      }
    }
    List<Long> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) {
      offsetSorted.add((Long) val);
    }
    Collections.sort(offsetSorted); // complete set of values

    // fast lookup of offset val in the result CoordinateTime
    Map<Long, Integer> map = new HashMap<>();
    int count = 0;
    for (Long val : offsetSorted) {
      map.put(val, count++);
    }

    // fast lookup of the run time in the master
    int[] run2master = new int[nruns];
    int masterIdx = 0;
    for (int run2Didx = 0; run2Didx < nruns; run2Didx++) {
      while (!master.getRuntimeDate(masterIdx).equals(runtime.getRuntimeDate(run2Didx))) {
        masterIdx++;
      }
      run2master[run2Didx] = masterIdx;
      masterIdx++;
    }
    assert masterIdx >= nruns;

    // now for each coordinate, use the latest runtime available
    int[] time2runtime = new int[offsetSorted.size()];
    for (int runIdx = 0; runIdx < nruns; runIdx++) {
      CoordinateTime timeCoord =
          (times == null) ? (CoordinateTime) getTimeCoordinate(runIdx) : (CoordinateTime) times.get(runIdx);
      assert timeCoord != null;
      for (Long offset : timeCoord.getOffsetSorted()) {
        Integer bestValIdx = map.get(offset + getOffset(runIdx));
        if (bestValIdx == null) {
          throw new IllegalStateException();
        }
        // uses this runtime; later ones override; one based so 0 = missing
        time2runtime[bestValIdx] = run2master[runIdx] + 1;
      }
    }

    return new CoordinateTime(getCode(), getTimeUnit(), getRefDate(), offsetSorted, time2runtime);
  }

  // can we make into a MRUTC ?
  public boolean hasUniqueTimes() {
    if (isTimeInterval) {
      // make unique set of coordinates
      Set<TimeCoordIntvValue> values = new HashSet<>();
      for (int runIdx = 0; runIdx < nruns; runIdx++) { // use times array, passed into constructor, with original
                                                       // inventory, if possible
        CoordinateTimeIntv timeIntv =
            (times == null) ? (CoordinateTimeIntv) getTimeCoordinate(runIdx) : (CoordinateTimeIntv) times.get(runIdx);
        for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
          TimeCoordIntvValue tinvAbs = tinv.offset(getOffset(runIdx)); // convert to absolute offset
          if (values.contains(tinvAbs)) {
            return false;
          }
          values.add(tinvAbs);
        }
      }
    } else {
      Set<Long> values = new HashSet<>(); // complete set of values
      for (int runIdx = 0; runIdx < nruns; runIdx++) { // use times array, passed into constructor, with original
                                                       // inventory, if possible
        CoordinateTime timeCoord =
            (times == null) ? (CoordinateTime) getTimeCoordinate(runIdx) : (CoordinateTime) times.get(runIdx);
        assert timeCoord != null;
        for (Long offset : timeCoord.getOffsetSorted()) {
          long offsetAbs = (offset + getOffset(runIdx)); // convert to absolute offset
          if (values.contains(offsetAbs)) {
            return false;
          }
          values.add(offsetAbs);
        }
      }
    }
    return true;
  }

  public int[] getTimeIndicesFromMrutp(int timeIdx) {
    int[] result = new int[2];
    if (isOrthogonal) {
      int nPerRun = otime.getNCoords();
      result[0] = timeIdx / nPerRun;
      result[1] = timeIdx % nPerRun;
      return result;

    } else if (isRegular) {
      int nPerRun = 0;
      for (Map.Entry<Integer, CoordinateTimeAbstract> entry : regTimes.entrySet()) {
        nPerRun += entry.getValue().getNCoords();
      }
      if (nPerRun > 0) {
        result[0] = timeIdx / nPerRun;
        result[1] = timeIdx % nPerRun;
      }
      return result;

    } else {
      int runtime = 0;
      int count = 0;
      for (Coordinate time : times) {
        if (count + time.getNCoords() > timeIdx) {
          result[0] = runtime;
          result[1] = timeIdx - count;
          return result;
        }
        runtime++;
        count += time.getNCoords();
      }
      throw new IllegalStateException("timeIdx = " + timeIdx); // cant happen?
    }
  }

  private CoordinateTimeAbstract makeBestTimeIntv(CoordinateRuntime master) {
    // make unique set of coordinates
    Set<TimeCoordIntvValue> values = new HashSet<>();
    for (int runIdx = 0; runIdx < nruns; runIdx++) { // use times array, passed into constructor, with original
                                                     // inventory, if possible
      CoordinateTimeIntv timeIntv =
          (times == null) ? (CoordinateTimeIntv) getTimeCoordinate(runIdx) : (CoordinateTimeIntv) times.get(runIdx);
      for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
        values.add(tinv.offset(getOffset(runIdx)));
      }
    }
    List<TimeCoordIntvValue> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) {
      offsetSorted.add((TimeCoordIntvValue) val);
    }
    Collections.sort(offsetSorted);

    // fast lookup of offset tinv in the result CoordinateTimeIntv
    Map<TimeCoordIntvValue, Integer> map = new HashMap<>(); // lookup coord val to index
    int count = 0;
    for (TimeCoordIntvValue val : offsetSorted) {
      map.put(val, count++);
    }

    // fast lookup of the run time in the master
    int[] run2master = new int[nruns];
    int masterIdx = 0;
    for (int run2Didx = 0; run2Didx < nruns; run2Didx++) {
      while (!master.getRuntimeDate(masterIdx).equals(runtime.getRuntimeDate(run2Didx))) {
        masterIdx++;
      }
      run2master[run2Didx] = masterIdx;
      masterIdx++;
    }
    assert masterIdx >= nruns;

    int[] time2runtime = new int[offsetSorted.size()];
    for (int runIdx = 0; runIdx < nruns; runIdx++) {
      CoordinateTimeIntv timeIntv =
          (times == null) ? (CoordinateTimeIntv) getTimeCoordinate(runIdx) : (CoordinateTimeIntv) times.get(runIdx);
      for (TimeCoordIntvValue bestVal : timeIntv.getTimeIntervals()) {
        Integer bestValIdx = map.get(bestVal.offset(getOffset(runIdx)));
        if (bestValIdx == null) {
          throw new IllegalStateException();
        }
        time2runtime[bestValIdx] = run2master[runIdx] + 1; // uses this runtime; later ones override; one based so 0 =
                                                           // missing
      }
    }

    return new CoordinateTimeIntv(getCode(), getTimeUnit(), getRefDate(), offsetSorted, time2runtime);
  }

  ////////////////////////////////////////////////////////

  /**
   * public by accident - do not use
   */
  public List<? extends Coordinate> getTimesForSerialization() {
    if (isOrthogonal) {
      List<Coordinate> list = new ArrayList<>(1);
      list.add(otime);
      return list;
    } else if (isRegular) {
      return new ArrayList<>(regTimes.values());
    } else {
      return times;
    }
  }

  /**
   * Find index in time coordinate from the time value
   * 
   * @param val TimeCoordIntvValue or Integer
   * @return indx in the time coordinate
   */
  public int findTimeIndexFromVal(int runIdx, Object val) {
    if (isOrthogonal) {
      return otime.getIndex(val);
    }

    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    if (time.getNCoords() == 1) {
      return 0;
    }
    return time.getIndex(val); // not sure if its reletive to runtime ??
  }

  /**
   * Get a sorted list of the unique time coordinates
   * 
   * @return List<Integer> or List<TimeCoordIntvValue>
   */
  public List<?> getOffsetsSorted() {
    if (isOrthogonal) {
      return otime.getValues();
    }

    List<? extends Coordinate> coords = isRegular ? new ArrayList<>(regTimes.values()) : times;
    if (isTimeInterval) {
      return getIntervalsSorted(coords);
    } else {
      return getValuesSorted(coords);
    }
  }

  private List<Long> getValuesSorted(List<? extends Coordinate> coords) {
    Set<Long> set = new HashSet<>(100);
    for (Coordinate coord : coords) {
      for (Object val : coord.getValues()) {
        set.add((Long) val);
      }
    }
    List<Long> result = new ArrayList<>(set);
    Collections.sort(result);
    return result;
  }

  private List<TimeCoordIntvValue> getIntervalsSorted(List<? extends Coordinate> coords) {
    Set<TimeCoordIntvValue> set = new HashSet<>(100);
    for (Coordinate coord : coords) {
      for (Object val : coord.getValues()) {
        set.add((TimeCoordIntvValue) val);
      }
    }
    List<TimeCoordIntvValue> result = new ArrayList<>(set);
    Collections.sort(result);
    return result;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class Time2D implements Comparable<Time2D> {
    long refDate;
    Long time;
    TimeCoordIntvValue tinv;

    public Time2D(CalendarDate refDate, Long time, TimeCoordIntvValue tinv) {
      this.refDate = refDate.getMillisFromEpoch();
      this.time = time;
      this.tinv = tinv;
    }

    Time2D(long refDate, Long time, TimeCoordIntvValue tinv) {
      this.refDate = refDate;
      this.time = time;
      this.tinv = tinv;
    }

    public CalendarDate getRefDate() {
      return CalendarDate.of(refDate);
    }

    public long getValue() {
      if (time != null) {
        return time;
      } else {
        return tinv.getBounds2();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      Time2D time2D = (Time2D) o;

      if (refDate != time2D.refDate) {
        return false;
      }
      if (!Objects.equals(time, time2D.time)) {
        return false;
      }
      return Objects.equals(tinv, time2D.tinv);
    }

    @Override
    public int hashCode() {
      int result = (int) (refDate ^ (refDate >>> 32));
      result = 31 * result + (time != null ? time.hashCode() : 0);
      result = 31 * result + (tinv != null ? tinv.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      if (time != null) {
        return time.toString();
      } else {
        return tinv.toString();
      }
    }

    @Override
    public int compareTo(@Nonnull Time2D o) {
      int r = Long.compare(refDate, o.refDate);
      if (r == 0) {
        if (time != null) {
          r = time.compareTo(o.time);
        } else {
          r = tinv.compareTo(o.tinv);
        }
      }
      return r;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record>
      implements CoordinateBuilder.TwoD<Grib2Record> {
    private final boolean isTimeInterval;
    private final Grib2Tables cust;
    private final int code; // pdsFirst.getTimeUnit()
    private CalendarPeriod timeUnit; // time duration, based on code

    private final CoordinateRuntime.Builder2 runBuilder;
    private final Map<Object, CoordinateBuilderImpl<Grib2Record>> timeBuilders; // one for each runtime

    public Builder2(boolean isTimeInterval, Grib2Tables cust, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.cust = cust;
      this.timeUnit = timeUnit;
      this.code = code;

      runBuilder = new CoordinateRuntime.Builder2(timeUnit);
      timeBuilders = new HashMap<>();
    }

    public void addRecord(Grib2Record gr) {
      super.addRecord(gr);
      runBuilder.addRecord(gr);
      Time2D val = (Time2D) extract(gr);
      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(val.refDate);
      timeBuilder.addRecord(gr);
    }

    @Override
    public Object extract(Grib2Record gr) {
      Long run = (Long) runBuilder.extract(gr);
      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(run);
      if (timeBuilder == null) {
        timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder2(cust, code, timeUnit, CalendarDate.of(run))
            : new CoordinateTime.Builder2(code, timeUnit, CalendarDate.of(run));
        timeBuilders.put(run, timeBuilder);
      }
      Object time = timeBuilder.extract(gr);
      if (time instanceof TimeCoordIntvValue) {
        return new Time2D(run, null, (TimeCoordIntvValue) time);
      } else if (time instanceof Number) {
        return new Time2D(run, ((Number) time).longValue(), null);
      } else {
        throw new IllegalArgumentException("unsupported coord type = " + time.getClass().getName());
      }
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();
      if (this.timeUnit != runCoord.timePeriod) {
        this.timeUnit = runCoord.timePeriod; // override time units if need be.
      }
      List<Coordinate> times = new ArrayList<>(runCoord.getSize());
      for (int idx = 0; idx < runCoord.getSize(); idx++) {
        Long runtime = runCoord.getRuntime(idx);
        CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(runtime);
        times.add(timeBuilder.finish());
      }

      List<Time2D> vals = new ArrayList<>(values.size());
      for (Object val : values)
        vals.add((Time2D) val);
      Collections.sort(vals);

      return new CoordinateTime2D(code, timeUnit, vals, runCoord, times, null);
    }

    @Override
    public void addAll(Coordinate coord) {
      super.addAll(coord);
      for (Object val : coord.getValues()) {
        Time2D val2D = (Time2D) val;
        runBuilder.add(val2D.refDate);
        CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(val2D.refDate);
        if (timeBuilder == null) {
          timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder2(cust, code, timeUnit, val2D.getRefDate())
              : new CoordinateTime.Builder2(code, timeUnit, val2D.getRefDate());
          timeBuilders.put(val2D.refDate, timeBuilder);
        }
        timeBuilder.add(isTimeInterval ? val2D.tinv : val2D.time);
      }
    }

    @Override
    public int[] getCoordIndices(Grib2Record gr) {
      CoordinateTime2D coord2D = (CoordinateTime2D) coord;
      Long run = (Long) runBuilder.extract(gr);
      int runIdx = coord2D.runtime.getIndex(run);
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);

      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(run);
      Object time = timeBuilder.extract(gr);
      int timeIdx = timeCoord.getIndex(time);

      return new int[] {runIdx, timeIdx};
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record>
      implements CoordinateBuilder.TwoD<Grib1Record> {
    private final boolean isTimeInterval;
    private final Grib1Customizer cust;
    private final int code; // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;

    private final CoordinateRuntime.Builder1 runBuilder;
    private final Map<Object, CoordinateBuilderImpl<Grib1Record>> timeBuilders;

    public Builder1(boolean isTimeInterval, Grib1Customizer cust, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.cust = cust;
      this.timeUnit = timeUnit;
      this.code = code;

      runBuilder = new CoordinateRuntime.Builder1(timeUnit);
      timeBuilders = new HashMap<>();
    }

    public void addRecord(Grib1Record gr) {
      super.addRecord(gr);
      runBuilder.addRecord(gr);
      Time2D val = (Time2D) extract(gr);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(val.refDate);
      timeBuilder.addRecord(gr);
    }

    @Override
    public Object extract(Grib1Record gr) {
      Long run = (Long) runBuilder.extract(gr);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(run);
      if (timeBuilder == null) {
        timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder1(cust, code, timeUnit, CalendarDate.of(run))
            : new CoordinateTime.Builder1(cust, code, timeUnit, CalendarDate.of(run));
        timeBuilders.put(run, timeBuilder);
      }
      Object time = timeBuilder.extract(gr);
      if (time instanceof TimeCoordIntvValue) {
        return new Time2D(run, null, (TimeCoordIntvValue) time);
      } else if (time instanceof Number) {
        return new Time2D(run, ((Number) time).longValue(), null);
      } else {
        throw new IllegalArgumentException("unsupported coord type = " + time.getClass().getName());
      }
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();

      List<Coordinate> times = new ArrayList<>(runCoord.getSize());
      for (int idx = 0; idx < runCoord.getSize(); idx++) {
        Long runtime = runCoord.getRuntime(idx);
        CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(runtime);
        times.add(timeBuilder.finish());
      }

      List<Time2D> vals = new ArrayList<>(values.size());
      for (Object val : values) {
        vals.add((Time2D) val);
      }
      Collections.sort(vals);

      return new CoordinateTime2D(code, timeUnit, vals, runCoord, times, null);
    }

    @Override
    public void addAll(Coordinate coord) {
      super.addAll(coord);
      for (Object val : coord.getValues()) {
        Time2D val2D = (Time2D) val;
        runBuilder.add(val2D.refDate);
        CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(val2D.refDate);
        if (timeBuilder == null) {
          timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder1(cust, code, timeUnit, val2D.getRefDate())
              : new CoordinateTime.Builder1(cust, code, timeUnit, val2D.getRefDate());
          timeBuilders.put(val2D.refDate, timeBuilder);
        }
        timeBuilder.add(isTimeInterval ? val2D.tinv : val2D.time);
      }
    }

    @Override
    public int[] getCoordIndices(Grib1Record gr) {
      CoordinateTime2D coord2D = (CoordinateTime2D) coord;
      Long run = (Long) runBuilder.extract(gr);
      int runIdx = coord2D.runtime.getIndex(run);
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);

      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(run);
      Object time = timeBuilder.extract(gr);
      int timeIdx = timeCoord.getIndex(time);

      return new int[] {runIdx, timeIdx};
    }
  }

}
