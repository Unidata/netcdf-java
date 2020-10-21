/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid2;

import ucar.nc2.grid.*;

/**
 * Describe
 *
 * @author caron
 * @since 7/21/2015
 */
class DataState {
  GridDataset coverageDataset;
  Grid grid;
  GridCoordinateSystem geocs;
  GridAxis1D zaxis;
  GridAxis1DTime taxis;
  GridAxis1DTime rtaxis;
  GridAxis1D ensaxis;

  public DataState(GridDataset coverageDataset, Grid grid) {
    this.coverageDataset = coverageDataset;
    this.grid = grid;
    this.geocs = grid.getCoordinateSystem();
    this.zaxis = geocs.getVerticalAxis();
    this.taxis = geocs.getTimeAxis();
    this.rtaxis = geocs.getRunTimeAxis();
    this.ensaxis = geocs.getEnsembleAxis();
  }
}
