/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.Streams;

import javax.annotation.Nullable;

import java.util.*;

/** A Coordinate System for gridded data. */
public interface GridCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  String getName();

  /** the GridAxes that constitute this Coordinate System */
  Iterable<GridAxis> getGridAxes();

  /** Find the named axis. */
  default Optional<GridAxis> findAxis(String axisName) {
    return Streams.stream(getGridAxes()).filter(g -> g.getName().equals(axisName)).findFirst();
  }

  /** Get the Time CoordinateSystem. */
  GridTimeCoordinateSystem getTimeCoordSystem();

  /** Get the Runtime axis. */
  @Nullable
  default GridAxis1DTime getRunTimeAxis() {
    return getTimeCoordSystem().getRunTimeAxis();
  }

  /** Get the Time axis. */
  @Nullable
  default GridAxis getTimeAxis() {
    return getTimeCoordSystem().getTimeAxis();
  }

  /** Get the Time Offset axis. */
  @Nullable
  default GridAxis getTimeOffsetAxis() {
    return getTimeCoordSystem().getTimeOffsetAxis();
  }

  /** Get the ensemble axis. */
  @Nullable
  GridAxis1D getEnsembleAxis();

  /** Get the Z axis (GeoZ, Height, Pressure). */
  @Nullable
  GridAxis1D getVerticalAxis();

  /** Get the Horizontal CoordinateSystem. */
  GridHorizCoordinateSystem getHorizCoordSystem();

  /** Get the X axis. (either GeoX or Lon) LOOK nullable? */
  default GridAxis getXHorizAxis() {
    return getHorizCoordSystem().getXHorizAxis();
  }

  /** Get the Y axis. (either GeoY or Lat) LOOK nullable? */
  default GridAxis getYHorizAxis() {
    return getHorizCoordSystem().getYHorizAxis();
  }

  /** Function description, eg GRID(T,Z,Y,Z):R LOOK */
  String showFnSummary();

  void show(Formatter f, boolean showCoords);

  /** Nominal shape, may differ from materialized shape. */
  int[] getNominalShape();
}
