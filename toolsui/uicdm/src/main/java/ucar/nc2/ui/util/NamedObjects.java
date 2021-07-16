/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.util;

import java.util.ArrayList;
import java.util.List;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.ui.util.NamedObject;
import ucar.unidata.util.Format;

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

  public static List<NamedObject> getNames(CoverageCoordAxis1D axis) {
    if (axis == null) {
      return new ArrayList<>();
    }
    if (axis.getAxisType() == AxisType.Time || axis.getAxisType() == AxisType.RunTime) {
      return getCoverageCoordTimeNames(axis);
    }

    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < axis.getNcoords(); i++) {
      Object value = null;
      switch (axis.getSpacing()) {
        case regularPoint:
        case irregularPoint:
          value = Format.d(axis.getCoordMidpoint(i), 3);
          break;

        case regularInterval:
        case contiguousInterval:
        case discontiguousInterval:
          value = new ucar.nc2.ft2.coverage.CoordInterval(axis.getCoordEdge1(i), axis.getCoordEdge2(i), 3);
          break;
      }
      result.add(NamedObject.create(value, value + " " + axis.getUnits()));
    }

    return result;
  }

  private static List<NamedObject> getCoverageCoordTimeNames(CoverageCoordAxis1D axis) {
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < axis.getNcoords(); i++) {
      double value;
      switch (axis.getSpacing()) {
        case regularPoint:
        case irregularPoint:
          value = axis.getCoordMidpoint(i);
          result.add(NamedObject.create(axis.makeDate(value), axis.getAxisType().toString()));
          break;

        case regularInterval:
        case contiguousInterval:
        case discontiguousInterval:
          ucar.nc2.ft2.coverage.CoordInterval coord =
              new ucar.nc2.ft2.coverage.CoordInterval(axis.getCoordEdge1(i), axis.getCoordEdge2(i), 3);
          result.add(NamedObject.create(coord, coord + " " + axis.getUnits()));
          break;
      }
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////

  public static List<NamedObject> getGridNames(Iterable<ucar.nc2.grid2.Grid> grids) {
    if (grids == null) {
      return new ArrayList<>();
    }
    List<NamedObject> result = new ArrayList<>();
    for (ucar.nc2.grid2.Grid grid : grids) {
      result.add(NamedObject.create(grid.getName(), grid.getDescription(), grid));
    }
    return result;
  }

  public static List<NamedObject> getCoordNames(ucar.nc2.grid2.GridAxis<?> axis) {
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
