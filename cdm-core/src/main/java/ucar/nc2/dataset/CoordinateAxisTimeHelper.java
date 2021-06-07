/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;

/**
 * Helper class for time coordinates
 * 
 * @deprecated use GridAxisTimeHelper
 */
@Deprecated
public class CoordinateAxisTimeHelper {
  private final Calendar calendar;
  private final CalendarDateUnit dateUnit;

  public CoordinateAxisTimeHelper(Calendar calendar, String unitString) {
    this.calendar = calendar;
    if (unitString == null) {
      this.dateUnit = null;
      return;
    }
    this.dateUnit = CalendarDateUnit.fromUdunitString(calendar, unitString).orElseThrow();
  }

  public CalendarDate makeCalendarDateFromOffset(int offset) {
    return dateUnit.makeCalendarDate(offset);
  }

  public CalendarDate makeCalendarDateFromOffset(String offset) {
    return CalendarDate.fromUdunitIsoDate(calendar.toString(), offset).orElseThrow();
  }

  public long offsetFromRefDate(CalendarDate date) {
    return dateUnit.makeOffsetFromRefDate(date);
  }

}
