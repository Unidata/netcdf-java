/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid2;

import ucar.nc2.grid.*;

import javax.annotation.Nullable;

/** Holds the current selected state. */
class DataState {
  GridDataset coverageDataset;
  Grid grid;
  GridCoordinateSystem geocs;

  @Nullable
  GridAxis1D zaxis;
  @Nullable
  GridAxis1DTime taxis;
  @Nullable
  GridAxis1DTime rtaxis;
  @Nullable
  GridAxis1D ensaxis;
  @Nullable
  GridAxisOffsetTimeRegular toaxisReg;

  public DataState(GridDataset coverageDataset, Grid grid) {
    this.coverageDataset = coverageDataset;
    this.grid = grid;
    this.geocs = grid.getCoordinateSystem();

    if (geocs.getTimeOffsetAxis() instanceof GridAxisOffsetTimeRegular) {
      this.toaxisReg = (GridAxisOffsetTimeRegular) geocs.getTimeOffsetAxis();
    } else if (geocs.getTimeOffsetAxis() instanceof GridAxis1DTime) {
      this.taxis = (GridAxis1DTime) geocs.getTimeOffsetAxis();
    } else {
      this.taxis = geocs.getTimeAxis();
    }

    this.rtaxis = geocs.getRunTimeAxis();
    this.zaxis = geocs.getVerticalAxis();
    this.ensaxis = geocs.getEnsembleAxis();
  }
}
