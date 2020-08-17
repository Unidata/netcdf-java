/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.*;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.Dimension;
import ucar.nc2.util.NamedObject;
import ucar.ma2.*;
import java.util.*;
import java.io.IOException;
import ucar.nc2.units.DateRange;

/**
 * A 1-dimensional Coordinate Axis representing Calendar time.
 * Its coordinate values can be represented as Dates.
 * <p/>
 * May use udunit dates, or ISO Strings.
 *
 * @author caron
 */
public class CoordinateAxis1DTime extends CoordinateAxis1D {

  private static final Logger logger = LoggerFactory.getLogger(CoordinateAxis1DTime.class);

  public static CoordinateAxis1DTime factory(@Nullable NetcdfDataset ncd, VariableDS org, Formatter errMessages)
      throws IOException {
    if (org instanceof CoordinateAxis1DTime) {
      return (CoordinateAxis1DTime) org;
    }

    if (org.getDataType() == DataType.CHAR) {
      return fromStringVarDS(ncd, org, ImmutableList.of(org.getDimension(0)));
    }

    if (org.getDataType() == DataType.STRING) {
      return fromStringVarDS(ncd, org, org.getDimensions());
    }

    return fromVarDS(ncd, org, errMessages);
  }

  /**
   * Constructor for CHAR or STRING variables.
   * Must contain ISO dates.
   *
   * @param ncd the containing dataset
   * @param org the underlying Variable
   * @param dims list of dimensions
   * @throws IOException on read error
   * @throws IllegalArgumentException if cant convert coordinate values to a Date
   */
  private static CoordinateAxis1DTime fromStringVarDS(@Nullable NetcdfDataset ncd, VariableDS org,
      List<Dimension> dims) {
    CoordinateAxis1DTime.Builder<?> builder = CoordinateAxis1DTime.builder().setName(org.getShortName())
        .setDataType(DataType.STRING).setUnits(org.getUnitsString()).setDesc(org.getDescription()).setDimensions(dims);
    builder.setOriginalVariable(org).setOriginalName(org.getOriginalName());

    builder.setTimeHelper(new CoordinateAxisTimeHelper(getCalendarFromAttribute(ncd, org.attributes()), null));
    builder.addAttributes(org.attributes());
    if (org instanceof CoordinateAxis) {
      builder.setAxisType(((CoordinateAxis) org).axisType);
    }
    return builder.build(org.getParentGroup());
  }

  /**
   * Constructor for numeric values - must have units
   *
   * @param ncd the containing dataset
   * @param org the underlying Variable
   * @throws IOException on read error
   */
  private static CoordinateAxis1DTime fromVarDS(@Nullable NetcdfDataset ncd, VariableDS org, Formatter errMessages)
      throws IOException {
    CoordinateAxis1DTime.Builder<?> builder = CoordinateAxis1DTime.builder().setName(org.getShortName())
        .setDataType(org.getDataType()).setUnits(org.getUnitsString()).setDesc(org.getDescription());
    builder.setOriginalVariable(org).setOriginalName(org.getOriginalName());

    CoordinateAxisTimeHelper helper =
        new CoordinateAxisTimeHelper(getCalendarFromAttribute(ncd, org.attributes()), org.getUnitsString());
    builder.setTimeHelper(helper);

    // make the coordinates
    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);
    Array data = org.read();

    int count = 0;
    IndexIterator ii = data.getIndexIterator();
    for (int i = 0; i < ncoords; i++) {
      double val = ii.getDoubleNext();
      if (Double.isNaN(val))
        continue; // WTF ??
      result.add(helper.makeCalendarDateFromOffset(val));
      count++;
    }

    // if we encountered NaNs, shorten it up
    ArrayList<Dimension> dims = new ArrayList<>(org.getDimensions());
    if (count != ncoords) {
      Dimension localDim = Dimension.builder(org.getShortName(), count).setIsShared(false).build();
      dims.set(0, localDim);

      // set the shortened values
      Array shortData = Array.factory(data.getDataType(), new int[] {count});
      Index ima = shortData.getIndex();
      int count2 = 0;
      ii = data.getIndexIterator();
      for (int i = 0; i < ncoords; i++) {
        double val = ii.getDoubleNext();
        if (Double.isNaN(val))
          continue;
        shortData.setDouble(ima.set0(count2), val);
        count2++;
      }
    }
    builder.setCalendarDates(result);
    builder.setDimensions(dims);
    builder.addAttributes(org.attributes());

    if (org instanceof CoordinateAxis) {
      builder.setAxisType(((CoordinateAxis) org).axisType);
    }
    return builder.build(org.getParentGroup());
  }

  ////////////////////////////////////////////////////////////////

  @Override
  public CoordinateAxis1DTime section(Range r) throws InvalidRangeException {
    CoordinateAxis1DTime s = (CoordinateAxis1DTime) super.section(r);
    List<CalendarDate> cdates = getCalendarDates();

    List<CalendarDate> cdateSection = new ArrayList<>(cdates.size());
    for (int idx : r)
      cdateSection.add(cdates.get(idx));

    s.cdates = cdateSection;
    return s;
  }

  /**
   * Get the the ith CalendarDate.
   * 
   * @param idx index
   * @return the ith CalendarDate
   */
  public CalendarDate getCalendarDate(int idx) {
    List<CalendarDate> cdates = getCalendarDates(); // in case we want to lazily evaluate
    return cdates.get(idx);
  }

  /**
   * Get calendar date range
   * 
   * @return calendar date range
   */
  public CalendarDateRange getCalendarDateRange() {
    List<CalendarDate> cd = getCalendarDates();
    int last = cd.size();
    return (last > 0) ? CalendarDateRange.of(cd.get(0), cd.get(last - 1)) : null;
  }

  @Override
  public List<NamedObject> getNames() {
    List<CalendarDate> cdates = getCalendarDates();
    List<NamedObject> names = new ArrayList<>(cdates.size());
    for (CalendarDate cd : cdates) {
      names.add(NamedObject.create(CalendarDateFormatter.toDateTimeStringISO(cd), getShortName()));
    }
    return names;
  }

  /**
   * only if isRegular() LOOK REDO
   *
   * @return time unit
   * @throws Exception on bad unit string
   */
  public TimeUnit getTimeResolution() throws Exception {
    String tUnits = getUnitsString();
    StringTokenizer stoker = new StringTokenizer(tUnits);
    double tResolution = getIncrement();
    return new TimeUnit(tResolution, stoker.nextToken());
  }

  /**
   * Given a Date, find the corresponding time index on the time coordinate axis.
   * Can only call this is hasDate() is true.
   * This will return
   * <ul>
   * <li>i, if time(i) <= d < time(i+1).
   * <li>0, if d < time(0)
   * <li>n-1, if d > time(n-1), where n is length of time coordinates
   * </ul>
   *
   * @param d date to look for
   * @return corresponding time index on the time coordinate axis
   * @throws UnsupportedOperationException is no time axis or isDate() false
   */
  public int findTimeIndexFromCalendarDate(CalendarDate d) {
    List<CalendarDate> cdates = getCalendarDates(); // LOOK linear search, switch to binary
    int index = 0;
    while (index < cdates.size()) {
      if (d.compareTo(cdates.get(index)) < 0)
        break;
      index++;
    }
    return Math.max(0, index - 1);
  }

  /**
   * See if the given CalendarDate appears as a coordinate
   *
   * @param date test this
   * @return true if equals a coordinate
   */
  public boolean hasCalendarDate(CalendarDate date) {
    List<CalendarDate> cdates = getCalendarDates();
    for (CalendarDate cd : cdates) { // LOOK linear search, switch to binary
      if (date.equals(cd))
        return true;
    }
    return false;
  }

  /**
   * Get the list of datetimes in this coordinate as CalendarDate objects.
   * 
   * @return list of CalendarDates.
   */
  public List<CalendarDate> getCalendarDates() {
    return cdates;
  }

  public CalendarDate[] getCoordBoundsDate(int i) {
    double[] intv = getCoordBounds(i);
    CalendarDate[] e = new CalendarDate[2];
    e[0] = helper.makeCalendarDateFromOffset(intv[0]);
    e[1] = helper.makeCalendarDateFromOffset(intv[1]);
    return e;
  }

  public CalendarDate getCoordBoundsMidpointDate(int i) {
    double[] intv = getCoordBounds(i);
    double midpoint = (intv[0] + intv[1]) / 2;
    return helper.makeCalendarDateFromOffset(midpoint);
  }

  ////////////////////////////////////////////////////////////////////////

  private List<CalendarDate> makeTimesFromChar(VariableDS org, Formatter errMessages) throws IOException {
    int ncoords = (int) org.getSize();
    int rank = org.getRank();
    int strlen = org.getShape(rank - 1);
    ncoords /= strlen;

    List<CalendarDate> result = new ArrayList<>(ncoords);

    ArrayChar data = (ArrayChar) org.read();
    ArrayChar.StringIterator ii = data.getStringIterator();
    ArrayObject.D1 sdata = (ArrayObject.D1) Array.factory(DataType.STRING, new int[] {ncoords});

    for (int i = 0; i < ncoords; i++) {
      String coordValue = ii.next();
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      sdata.set(i, coordValue);
      result.add(cd);
    }
    setCachedData(sdata, true);
    return result;
  }

  private List<CalendarDate> makeTimesFromStrings(VariableDS org, Formatter errMessages) throws IOException {
    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);

    ArrayObject data = (ArrayObject) org.read();
    IndexIterator ii = data.getIndexIterator();
    for (int i = 0; i < ncoords; i++) {
      String coordValue = (String) ii.getObjectNext();
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      result.add(cd);
    }

    return result;
  }

  private CalendarDate makeCalendarDateFromStringCoord(String coordValue, VariableDS org, Formatter errMessages) {
    CalendarDate cd = helper.makeCalendarDateFromOffset(coordValue);
    if (cd == null) {
      if (errMessages != null) {
        errMessages.format("String time coordinate must be ISO formatted= %s%n", coordValue);
        logger.info("Char time coordinate must be ISO formatted= {} file = {}", coordValue, org.getDatasetLocation());
      }
      throw new IllegalArgumentException();
    }
    return cd;
  }

  ///////////////////////////////////////////////////////

  /**
   * Does not handle non-standard Calendars
   * 
   * @deprecated use getCalendarDates() to correctly interpret calendars
   */
  public java.util.Date[] getTimeDates() {
    List<CalendarDate> cdates = getCalendarDates();
    Date[] timeDates = new Date[cdates.size()];
    int index = 0;
    for (CalendarDate cd : cdates)
      timeDates[index++] = cd.toDate();
    return timeDates;
  }

  /**
   * Does not handle non-standard Calendars
   * 
   * @deprecated use getCalendarDate()
   */
  public java.util.Date getTimeDate(int idx) {
    return getCalendarDate(idx).toDate();
  }

  /**
   * Does not handle non-standard Calendars
   * 
   * @deprecated use getCalendarDateRange()
   */
  public DateRange getDateRange() {
    CalendarDateRange cdr = getCalendarDateRange();
    return cdr.toDateRange();
  }

  /**
   * Does not handle non-standard Calendars
   * 
   * @deprecated use findTimeIndexFromCalendarDate
   */
  public int findTimeIndexFromDate(java.util.Date d) {
    return findTimeIndexFromCalendarDate(CalendarDate.of(d));
  }

  /**
   * Does not handle non-standard Calendars
   * 
   * @deprecated use hasCalendarDate
   */
  public boolean hasTime(Date date) {
    List<CalendarDate> cdates = getCalendarDates();
    for (CalendarDate cd : cdates) {
      if (date.equals(cd.toDate()))
        return true;
    }
    return false;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private CoordinateAxisTimeHelper helper;
  private List<CalendarDate> cdates;

  protected CoordinateAxis1DTime(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.helper = builder.helper;

    try {
      Formatter errMessages = new Formatter();
      if (getDataType() == DataType.CHAR) {
        cdates = makeTimesFromChar((VariableDS) builder.orgVar, errMessages);
      } else if (getDataType() == DataType.STRING) {
        cdates = makeTimesFromStrings((VariableDS) builder.orgVar, errMessages);
      } else {
        cdates = builder.cdates;
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /**
   * Get Builder for this class that allows subclassing.
   * 
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends CoordinateAxis1D.Builder<T> {
    private boolean built;
    private CoordinateAxisTimeHelper helper;
    private List<CalendarDate> cdates;

    protected abstract T self();

    public T setTimeHelper(CoordinateAxisTimeHelper helper) {
      this.helper = helper;
      return self();
    }

    public T setCalendarDates(List<CalendarDate> cdates) {
      this.cdates = cdates;
      return self();
    }

    public CoordinateAxis1DTime build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      if (axisType == null) {
        axisType = AxisType.Time;
      }
      return new CoordinateAxis1DTime(this, parentGroup);
    }
  }
}
