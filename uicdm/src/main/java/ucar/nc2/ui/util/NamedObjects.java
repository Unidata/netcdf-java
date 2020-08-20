/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.util;

import java.util.ArrayList;
import java.util.List;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.util.NamedObject;

/** Utililies for creating named objects. */
public class NamedObjects {

  public static List<NamedObject> getNames(CoordinateAxis1DTime axis) {
    if (axis == null) {
      return new ArrayList<>();
    }
    List<CalendarDate> cdates = axis.getCalendarDates();
    List<NamedObject> names = new ArrayList<>(cdates.size());
    for (CalendarDate cd : cdates) {
      names.add(NamedObject.create(CalendarDateFormatter.toDateTimeStringISO(cd), axis.getShortName()));
    }
    return names;
  }

  public static List<NamedObject> getNames(CoordinateAxis1D axis) {
    if (axis == null) {
      return new ArrayList<>();
    }
    List<NamedObject> names = new ArrayList<>();
    for (int i = 0; i < axis.getSize(); i++) {
      names.add(NamedObject.create(axis.getCoordName(i), axis.getShortName() + " " + axis.getUnitsString()));
    }
    return names;
  }

}
