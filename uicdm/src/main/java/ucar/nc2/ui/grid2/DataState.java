/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid2;

import ucar.nc2.grid.*;

import javax.annotation.Nullable;

/** Holds the current selected state. Shared between Renderer and Viewer. */
class DataState {
  GridDataset gridDataset;
  Grid grid;
  GridCoordinateSystem geocs;

  @Nullable
  GridAxis1DTime rtaxis;
  @Nullable
  GridAxis1DTime taxis;
  @Nullable
  GridAxis1D toaxis;
  @Nullable
  GridAxisOffsetTimeRegular toaxisReg;
  @Nullable
  GridAxis1D zaxis;
  @Nullable
  GridAxis1D ensaxis;

  Object runtimeCoord;
  Object timeCoord; // only one of taxis, toaxis, toaxisReg is used
  Object vertCoord;
  Object ensCoord;
  int horizStride = 1;

  public DataState(GridDataset gridDataset, Grid grid) {
    this.gridDataset = gridDataset;
    this.grid = grid;
    this.geocs = grid.getCoordinateSystem();

    // only one of taxis, toaxis, toaxisReg is used
    if (geocs.getTimeOffsetAxis() != null) {
      if (geocs.getTimeOffsetAxis() instanceof GridAxisOffsetTimeRegular) {
        this.toaxisReg = (GridAxisOffsetTimeRegular) geocs.getTimeOffsetAxis();
      } else if (geocs.getTimeOffsetAxis() instanceof GridAxis1D) {
        this.toaxis = (GridAxis1D) geocs.getTimeOffsetAxis();
      }
    } else {
      this.taxis = geocs.getTimeAxis();
    }

    this.rtaxis = geocs.getRunTimeAxis();
    this.zaxis = geocs.getVerticalAxis();
    this.ensaxis = geocs.getEnsembleAxis();
  }

  boolean setRuntimeCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(runtimeCoord);
    runtimeCoord = coord;
    return changed;
  }

  boolean setTimeCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(timeCoord);
    timeCoord = coord;
    return changed;
  }

  boolean setVertCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(vertCoord);
    vertCoord = coord;
    return changed;
  }

  boolean setEnsCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(ensCoord);
    ensCoord = coord;
    return changed;
  }

  Grid lastGrid;
  Object lastRuntime;
  Object lastTime;
  Object lastVert;
  Object lastEnsemble;
  int lastStride;

  void saveState() {
    lastGrid = grid;
    lastRuntime = runtimeCoord;
    lastTime = timeCoord;
    lastVert = vertCoord;
    lastEnsemble = ensCoord;
    lastStride = horizStride;
  }

  boolean hasChanged() {
    if (grid != null && !grid.equals(lastGrid)) {
      return true;
    }
    if (runtimeCoord != null && !runtimeCoord.equals(lastRuntime)) {
      return true;
    }
    if (timeCoord != null && !timeCoord.equals(lastTime)) {
      return true;
    }
    if (vertCoord != null && !vertCoord.equals(lastVert)) {
      return true;
    }
    if (ensCoord != null && !ensCoord.equals(lastEnsemble)) {
      return true;
    }
    if (horizStride != lastStride) {
      return true;
    }
    return false;
  }
}
