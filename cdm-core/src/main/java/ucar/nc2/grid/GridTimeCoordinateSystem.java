/*
 *  Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */

package ucar.nc2.grid;

import ucar.nc2.calendar.CalendarDate;

import javax.annotation.Nullable;

public interface GridTimeCoordinateSystem {
  enum Type {
    Observation, // 1D time, runtime = time or null
    SingleRuntime, // scaler runtime, 1D time, offset = null
    Offset, // 1D runtime and offset, time = null
    OffsetRegular, // 1D runtime, 2D offset, time = null
    Time2d // 1D runtime, 2D time, offset = null
  }

  /** Get the Runtime axis. May be scaler or null. */
  @Nullable
  GridAxis1DTime getRunTimeAxis();

  /** Get the Time axis. SRC, OBS = 1D; ORTH, REG = null; TIME2D = 2D */
  @Nullable
  GridAxis getTimeAxis();

  /** Get the Time Offset axis. SRC, OBS, TIME2D = null; ORTH = 1D; REG = 2D */
  @Nullable
  GridAxis getTimeOffsetAxis();

  interface Observation extends GridTimeCoordinateSystem {
    GridAxis1D getTimeAxis();
  }

  interface SingleRuntime extends GridTimeCoordinateSystem {
    CalendarDate getRunTime();
    GridAxis1D getTimeAxis();
  }

  interface Offset extends GridTimeCoordinateSystem {
    GridAxis1DTime getRunTimeAxis();
    GridAxis1D getTimeOffsetAxis(); // LOOK important enough special case? UI?
    GridAxis1DTime getTimeAxis(int runIdx);
  }

  interface OffsetRegular extends GridTimeCoordinateSystem {
    GridAxis1DTime getRunTimeAxis();
    GridAxis1D getTimeOffsetAxis(int minute);
    GridAxis1DTime getTimeAxis(int runIdx);
  }

  interface Time2d extends GridTimeCoordinateSystem {
    GridAxis1DTime getRunTimeAxis();
    GridAxis1D getTimeOffsetAxis(int runIdx);
    GridAxis1DTime getTimeAxis(int runIdx);
  }
}
