/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm.client;

import com.google.common.base.Preconditions;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

public class GcdmTimeCS extends GridTimeCoordinateSystem {

  @Nullable
  @Override
  public CalendarDate getRuntimeDate(int runIdx) {
    if (this.runTimeAxis == null) {
      return null;
    } else {
      return calendarDateUnit.makeCalendarDate(this.runTimeAxis.getCoordinate(runIdx).longValue());
    }
  }

  @Override
  public GridAxis<?> getTimeOffsetAxis(int runIdx) {
    return timeOffsetAxis; // So we are only supporting Orthogonal times right now
  }

  @Override
  public List<CalendarDate> getTimesForRuntime(int runIdx) {
    if (this.type == Type.Observation) {
      return getTimesForObservation();
    } else {
      return getTimesForNonObservation(runIdx);
    }
  }

  private List<CalendarDate> getTimesForObservation() {
    List<CalendarDate> result = new ArrayList<>();
    for (int timeIdx = 0; timeIdx < timeOffsetAxis.getNominalSize(); timeIdx++) {
      result.add(this.calendarDateUnit.makeCalendarDate((long) timeOffsetAxis.getCoordMidpoint(timeIdx)));
    }
    return result;
  }

  private List<CalendarDate> getTimesForNonObservation(int runIdx) {
    Preconditions.checkArgument(runTimeAxis != null && runIdx >= 0 && runIdx < runTimeAxis.getNominalSize());
    CalendarDate baseForRun = getRuntimeDate(runIdx);
    GridAxis<?> timeAxis = getTimeOffsetAxis(runIdx);
    List<CalendarDate> result = new ArrayList<>();
    for (int offsetIdx = 0; offsetIdx < timeAxis.getNominalSize(); offsetIdx++) {
      result.add(baseForRun.add((long) timeAxis.getCoordMidpoint(offsetIdx), this.offsetPeriod));
    }
    return result;
  }

  @Override
  public Optional<? extends GridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    return Optional.empty();
  }

  public GcdmTimeCS(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit calendarDateUnit) {
    super(type, runTimeAxis, timeOffsetAxis, calendarDateUnit);
  }
}
