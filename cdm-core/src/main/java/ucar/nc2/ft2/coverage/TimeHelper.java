/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import com.google.common.base.Preconditions;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.calendar.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Helper class for Time coordinate axes
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class TimeHelper {

  @Nullable
  public static TimeHelper factory(String units, AttributeContainer atts) {
    if (units == null)
      units = atts.findAttributeString(CDM.UDUNITS, null);
    if (units == null)
      units = atts.findAttributeString(CDM.UNITS, null);
    if (units == null)
      throw new IllegalStateException("No units");

    Calendar cal = getCalendarFromAttribute(atts);
    return CalendarDateUnit.fromUdunitString(cal, units).map(du -> new TimeHelper(du)).orElse(null);
  }

  //////////////////////////////////////////////

  // final Calendar cal;
  final CalendarDateUnit dateUnit;
  // final CalendarDate refDate;
  // final double duration;

  private TimeHelper(CalendarDateUnit dateUnit) {
    // this.cal = cal;
    this.dateUnit = dateUnit;
    // this.refDate = dateUnit.getBaseCalendarDate();
    // this.duration = dateUnit.getTimeUnit().getValueInMillisecs();
  }

  // copy on modify
  public TimeHelper setReferenceDate(CalendarDate refDate) {
    Preconditions.checkArgument(dateUnit.getCalendar() == refDate.getCalendar());
    CalendarDateUnit cdUnit = CalendarDateUnit.of(dateUnit.getCalendarPeriod(), false, refDate);
    return new TimeHelper(cdUnit);
  }

  public String getUdUnit() {
    return dateUnit.toString();
  }

  // get offset from runDate, in units of dateUnit
  public double offsetFromRefDate(CalendarDate date) {
    return dateUnit.makeOffsetFromRefDate(date);
  }

  public CalendarDate getRefDate() {
    return dateUnit.getBaseDateTime();
  }

  public CalendarDate makeDate(int value) {
    return dateUnit.makeCalendarDate(value);
  }

  public CalendarDateRange getDateRange(int startValue, int endValue) {
    CalendarDate start = makeDate(startValue);
    CalendarDate end = makeDate(endValue);
    return CalendarDateRange.of(start, end);
  }

  public long getOffsetInTimeUnits(CalendarDate start, CalendarDate end) {
    return start.since(end, dateUnit.getCalendarPeriod());
  }

  public CalendarDate makeDateInTimeUnits(CalendarDate start, double addTo) {
    return start.add(CalendarPeriod.of((int) addTo, dateUnit.getCalendarField()));
  }

  public static Calendar getCalendarFromAttribute(AttributeContainer atts) {
    String cal = atts.findAttributeString(CF.CALENDAR, null);
    return Calendar.get(cal).orElse(null);
  }

  public Calendar getCalendar() {
    return dateUnit.getCalendar();
  }

  public CalendarDateUnit getCalendarDateUnit() {
    return dateUnit;
  }


}
