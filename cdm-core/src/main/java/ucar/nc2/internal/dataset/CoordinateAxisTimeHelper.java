/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset;

import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;

/**
 * Helper class for time coordinates
 */
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

}
