/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.nc2.AttributeContainer;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/** Helper class for GridAxis time coordinates. */
@Immutable
public class CoordTimeHelper {

  public static CoordTimeHelper factory(String units, @Nullable AttributeContainer atts) {
    if (units == null)
      units = atts.findAttributeString(CDM.UDUNITS, null);
    if (units == null)
      units = atts.findAttributeString(CDM.UNITS, null);
    if (units == null)
      throw new IllegalStateException("No units");

    Calendar cal = atts == null ? null : getCalendarFromAttribute(atts);

    CalendarDateUnit dateUnit;
    dateUnit = CalendarDateUnit.fromUdunitString(cal, units).orElseThrow();
    return new CoordTimeHelper(dateUnit);
  }

  //////////////////////////////////////////////

  final CalendarDateUnit dateUnit;

  private CoordTimeHelper(CalendarDateUnit dateUnit) {
    this.dateUnit = dateUnit;
  }

  // copy on modify
  public CoordTimeHelper changeReferenceDate(CalendarDate refDate) {
    Preconditions.checkArgument(dateUnit.getCalendar() == refDate.getCalendar());
    CalendarDateUnit cdUnit = CalendarDateUnit.of(dateUnit.getCalendarPeriod(), false, refDate);
    return new CoordTimeHelper(cdUnit);
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

  public CalendarDate makeDate(long value) {
    return dateUnit.makeCalendarDate(value);
  }

  public CalendarDateRange getDateRange(int startValue, int endValue) {
    CalendarDate start = makeDate(startValue);
    CalendarDate end = makeDate(endValue);
    return CalendarDateRange.of(start, end);
  }

  public double getOffsetInTimeUnits(CalendarDate start, CalendarDate end) {
    return end.since(start, dateUnit.getCalendarPeriod());
    // return dateUnit.getCalendarPeriod().getOffset(start, end);
  }

  public CalendarDate makeDateInTimeUnits(CalendarDate start, double addTo) {
    return start.add(CalendarPeriod.of((int) addTo, dateUnit.getCalendarField()));
  }

  @Nullable
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

  public CalendarDate makeCalendarDateFromOffset(long offset) {
    return dateUnit.makeCalendarDate(offset);
  }

  // LOOK misnamed
  public CalendarDate makeCalendarDateFromOffset(String offset) {
    return CalendarDate.fromUdunitIsoDate(null, offset).orElseThrow();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CoordTimeHelper that = (CoordTimeHelper) o;
    return dateUnit.equals(that.dateUnit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dateUnit);
  }

  @Override
  public String toString() {
    return "TimeHelper{" + "dateUnit=" + dateUnit + '}';
  }
}
