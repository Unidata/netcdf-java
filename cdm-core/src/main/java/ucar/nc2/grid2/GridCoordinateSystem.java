/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;
import ucar.nc2.constants.AxisType;

import javax.annotation.Nullable;
import java.util.ArrayList;
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
  GridTimeCoordinateSystem getTimeCoordinateSystem();

  /** Get the ensemble axis, if any. */
  @Nullable
  default GridAxisPoint getEnsembleAxis() {
    return (GridAxisPoint) getGridAxes().stream().filter(a -> a.getAxisType() == AxisType.Ensemble).findFirst()
        .orElse(null);
  }

  /** Get the Z axis (GeoZ, Height, Pressure), if any. */
  @Nullable
  default GridAxis<?> getVerticalAxis() {
    return getGridAxes().stream().filter(a -> a.getAxisType().isVert()).findFirst().orElse(null);
  }

  /** Get the Horizontal CoordinateSystem. */
  GridHorizCoordinateSystem getHorizCoordinateSystem();

  /** Get the X axis (either GeoX or Lon). */
  default GridAxisPoint getXHorizAxis() {
    return getHorizCoordinateSystem().getXHorizAxis();
  }

  /** Get the Y axis (either GeoY or Lat). */
  default GridAxisPoint getYHorizAxis() {
    return getHorizCoordinateSystem().getYHorizAxis();
  }

  /** Nominal shape, may differ from materialized shape. */
  default List<Integer> getNominalShape() {
    List<Integer> result = new ArrayList<>();
    if (getTimeCoordinateSystem() != null) {
      result.addAll(getTimeCoordinateSystem().getNominalShape());
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis().getNominalSize());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis().getNominalSize());
    }
    result.addAll(getHorizCoordinateSystem().getShape());
    return result;
  }

  /** Function description, eg GRID(T,Z,Y,Z):R LOOK needed? */
  String showFnSummary();
}
