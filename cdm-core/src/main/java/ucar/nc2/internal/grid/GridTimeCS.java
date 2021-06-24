/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.grid.GridTimeCoordinateSystem;

import javax.annotation.Nullable;

/** Implementation of GridTimeCoordinateSystem. */
public class GridTimeCS implements GridTimeCoordinateSystem {
  private final GridAxis1DTime runTimeAxis;
  private final GridAxis timeAxis;
  private final GridAxis timeOffsetAxis;

  public GridTimeCS(GridAxis1DTime runTimeAxis, GridAxis timeAxis, GridAxis timeOffsetAxis) {
    this.runTimeAxis = runTimeAxis;
    this.timeAxis = timeAxis;
    this.timeOffsetAxis = timeOffsetAxis;
  }

  @Nullable
  @Override
  public GridAxis1DTime getRunTimeAxis() {
    return runTimeAxis;
  }

  @Nullable
  @Override
  public GridAxis getTimeAxis() {
    return timeAxis;
  }

  @Nullable
  @Override
  public GridAxis getTimeOffsetAxis() {
    return timeOffsetAxis;
  }
}
