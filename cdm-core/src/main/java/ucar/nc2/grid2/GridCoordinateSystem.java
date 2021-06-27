/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/**
 * A Coordinate System for gridded data, consisting of 1D GridAxes.
 * In some cases, the shape is an approximation, so that when reading data, the returned Array
 * will have a different shape, reflecting the actual data.
 */
public interface GridCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  String getName();

  /** The GridAxes that constitute this Coordinate System. */
  ImmutableList<GridAxis<?>> getGridAxes();

  /** Find the named axis. */
  default Optional<GridAxis<?>> findAxis(String axisName) {
    return getGridAxes().stream().filter(g -> g.getName().equals(axisName)).findFirst();
  }

  /** Get the Time CoordinateSystem. Null if there are no time coordinates. */
  @Nullable
  GridTimeCoordinateSystem getTimeCoordSystem();

  /** Get the ensemble axis, if any. */
  @Nullable
  GridAxisPoint getEnsembleAxis();

  /** Get the Z axis (GeoZ, Height, Pressure), if any. */
  @Nullable
  GridAxis<?> getVerticalAxis();

  /** Get the Horizontal CoordinateSystem. */
  GridHorizCoordinateSystem getHorizCoordSystem();

  /** Get the X axis (either GeoX or Lon). */
  default GridAxisPoint getXHorizAxis() {
    return getHorizCoordSystem().getXHorizAxis();
  }

  /** Get the Y axis (either GeoY or Lat). */
  default GridAxisPoint getYHorizAxis() {
    return getHorizCoordSystem().getYHorizAxis();
  }

  /** Nominal shape, may differ from materialized shape. */
  List<Integer> getNominalShape();

  /** Function description, eg GRID(T,Z,Y,Z):R LOOK needed? */
  String showFnSummary();

  void show(Formatter f, boolean showCoords);
}
