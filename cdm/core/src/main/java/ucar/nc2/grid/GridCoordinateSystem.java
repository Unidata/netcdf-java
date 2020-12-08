/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import javax.annotation.Nullable;

import ucar.ma2.RangeIterator;

import java.util.*;

/** A Coordinate System for gridded data. */
public interface GridCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  String getName();

  /** the GridAxes that constitute this Coordinate System */
  Iterable<GridAxis> getGridAxes();

  /** Find the named axis. */
  Optional<GridAxis> findAxis(String axisName);

  /** Get the ensemble axis. */
  @Nullable
  GridAxis1D getEnsembleAxis();

  /** Get the Runtime axis. */
  @Nullable
  GridAxis1DTime getRunTimeAxis();

  /** Get the Time axis. */
  @Nullable
  GridAxis1DTime getTimeAxis();

  /** Get the Time Offset axis. */
  @Nullable
  GridAxis getTimeOffsetAxis();

  /** Get the Z axis (GeoZ, Height, Pressure). */
  @Nullable
  GridAxis1D getVerticalAxis();

  /** Get the X axis. (either GeoX or Lon) */
  GridAxis getXHorizAxis();

  /** Get the Y axis. (either GeoY or Lat) */
  GridAxis getYHorizAxis();

  /** Get the Horizontal CoordinateSystem. */
  GridHorizCoordinateSystem getHorizCoordSystem();

  String showFnSummary();

  void show(Formatter f, boolean showCoords);

  /** Subset each axis based on the given parameters. */
  Optional<GridCoordinateSystem> subset(GridSubset params, Formatter errLog);

  // TODO what is this?
  List<RangeIterator> getRanges();
}
