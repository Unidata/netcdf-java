/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import ucar.nc2.calendar.CalendarDate;

import javax.annotation.Nullable;

/**
 * Manages the time coordinates of a GridCoordinateSystem.
 * The complexity is due to Forecast Model Run Collections (FMRC), in which the time coordinate
 * depends on the forecast run. The various cases are captured by the subinterfaces. Usage typically
 * is to cast to one of those:
 * 
 * <pre>
 * switch (gtcs.getType()) {
 *   case Observation:
 *     processObservation((GridTimeCoordinateSystem.Observation) gtcs);
 *     break;
 *   case SingleRuntime:
 *     processSingleRuntime((GridTimeCoordinateSystem.SingleRuntime) gtcs);
 *     break;
 *   case Offset:
 *     processOffset((GridTimeCoordinateSystem.Offset) gtcs);
 *     break;
 *   case OffsetRegular:
 *     processOffsetRegular((GridTimeCoordinateSystem.OffsetRegular) gtcs);
 *     break;
 *   case Time2d:
 *     processTime2d((GridTimeCoordinateSystem.Time2d) gtcs);
 *     break;
 * }
 * </pre>
 */
public interface GridTimeCoordinateSystem {
  enum Type {
    None, Observation, // 1D time, runtime = time or null
    SingleRuntime, // scaler runtime, 1D time, offset = null
    Offset, // 1D runtime and offset, time = null
    OffsetRegular, // 1D runtime, 2D offset, time = null
    Time2d // 1D runtime, 2D time, offset = null
  }

  Type getType();

  /** Get the Runtime axis. May be scaler or null. */
  @Nullable
  GridAxisPoint getRunTimeAxis();

  /** Get the Time axis. SRC, OBS = 1D; ORTH, REG = null; TIME2D = 2D */
  @Nullable
  GridAxis getTimeAxis();

  /** Get the Time Offset axis. SRC, OBS, TIME2D = null; ORTH = 1D; REG = 2D */
  @Nullable
  GridAxis getTimeOffsetAxis();

  interface Observation extends GridTimeCoordinateSystem {
    GridAxis getTimeAxis();
  }

  interface SingleRuntime extends GridTimeCoordinateSystem {
    CalendarDate getRunTime();

    GridAxis getTimeAxis();
  }

  interface Offset extends GridTimeCoordinateSystem {
    GridAxisPoint getRunTimeAxis();

    GridAxis getTimeOffsetAxis(); // LOOK important enough special case? UI?

    GridAxis getTimeAxis(int runIdx);
  }

  interface OffsetRegular extends GridTimeCoordinateSystem {
    GridAxisPoint getRunTimeAxis();

    GridAxis getTimeOffsetAxis(int runIdx);

    GridAxis getTimeAxis(int runIdx);
  }

  interface Time2d extends GridTimeCoordinateSystem {
    GridAxisPoint getRunTimeAxis();

    GridAxis getTimeOffsetAxis(int runIdx);

    GridAxis getTimeAxis(int runIdx);
  }
}
