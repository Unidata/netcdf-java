/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid3;

import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.ui.util.NamedObject;

import javax.annotation.Nullable;

/** Holds the current selected state. Shared between Renderer and Viewer. */
class DataState {
  GridDataset gridDataset;
  Grid grid;
  GridCoordinateSystem gcs;
  GridTimeCoordinateSystem tcs;

  @Nullable
  GridAxisPoint rtaxis;
  @Nullable
  GridAxis<?> toaxis;
  @Nullable
  GridAxis<?> zaxis;
  @Nullable
  GridAxisPoint ensaxis;

  RuntimeNamedObject runtimeCoord;
  Object timeCoord;
  Object vertCoord;
  Double ensCoord;
  int horizStride = 1;

  public DataState(GridDataset gridDataset, Grid grid) {
    this.gridDataset = gridDataset;
    this.grid = grid;
    this.gcs = grid.getCoordinateSystem();
    this.tcs = grid.getCoordinateSystem().getTimeCoordinateSystem();
    if (tcs != null) {
      this.toaxis = tcs.getTimeOffsetAxis(0);
      this.rtaxis = tcs.getRunTimeAxis();
    }
    this.zaxis = gcs.getVerticalAxis();
    this.ensaxis = gcs.getEnsembleAxis();
  }

  boolean setRuntimeCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(runtimeCoord);
    runtimeCoord = (RuntimeNamedObject) coord;
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
    ensCoord = (Double) coord;
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

  @Override
  public String toString() {
    return "DataState{" + "grid=" + grid + '}';
  }

  static class RuntimeNamedObject implements NamedObject {
    final int runtimeIdx;
    final CalendarDate runtime;

    public RuntimeNamedObject(int runtimeIdx, CalendarDate runtime) {
      this.runtimeIdx = runtimeIdx;
      this.runtime = runtime;
    }

    @Override
    public String getName() {
      return runtime.toString();
    }

    @Override
    public String getDescription() {
      return runtime.toString();
    }

    @Override
    public Object getValue() {
      return this;
    }
  }
}
