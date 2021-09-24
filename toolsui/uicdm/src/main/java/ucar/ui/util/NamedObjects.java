/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.util;

import java.util.ArrayList;
import java.util.List;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.ui.util.NamedObject;

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

  /////////////////////////////////////////////////////////////////////////////

  public static List<NamedObject> getGridNames(Iterable<ucar.nc2.grid.Grid> grids) {
    if (grids == null) {
      return new ArrayList<>();
    }
    List<NamedObject> result = new ArrayList<>();
    for (ucar.nc2.grid.Grid grid : grids) {
      result.add(NamedObject.create(grid.getName(), grid.getDescription(), grid));
    }
    return result;
  }

  public static List<NamedObject> getCoordNames(ucar.nc2.grid.GridAxis<?> axis) {
    if (axis == null) {
      return new ArrayList<>();
    }
    List<NamedObject> result = new ArrayList<>();
    for (Object coord : axis) {
      result.add(NamedObject.create(coord, axis.getUnits()));
    }
    return result;
  }

  public static List<NamedObject> getTimeNames(GridTimeCoordinateSystem tcs, int runtimeIdx, GridAxisPoint axis) {
    if (tcs == null || axis == null) {
      return new ArrayList<>();
    }
    List<NamedObject> result = new ArrayList<>();
    for (CalendarDate cdate : tcs.getTimesForRuntime(runtimeIdx)) {
      result.add(NamedObject.create(cdate, axis.getAxisType().toString()));
    }
    return result;
  }

}
