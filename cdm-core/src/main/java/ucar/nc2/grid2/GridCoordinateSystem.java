/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import com.google.common.collect.Streams;

import javax.annotation.Nullable;
import java.util.Formatter;
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
  Iterable<GridAxis> getGridAxes();

  /** Find the named axis. */
  default Optional<GridAxis> findAxis(String axisName) {
    return Streams.stream(getGridAxes()).filter(g -> g.getName().equals(axisName)).findFirst();
  }

  /** Get the Time CoordinateSystem. LOOK nullable ? empty? */
  GridTimeCoordinateSystem getTimeCoordSystem();

  /** Get the Runtime axis, if any. */
  @Nullable
  default GridAxisPoint getRunTimeAxis() {
    return getTimeCoordSystem().getRunTimeAxis();
  }

  /** Get the Time axis, if any. */
  @Nullable
  default GridAxis getTimeAxis() {
    return getTimeCoordSystem().getTimeAxis();
  }

  /** Get the Time Offset axis, if any. */
  @Nullable
  default GridAxis getTimeOffsetAxis() {
    return getTimeCoordSystem().getTimeOffsetAxis();
  }

  /** Get the ensemble axis, if any. */
  @Nullable
  GridAxis getEnsembleAxis();

  /** Get the Z axis (GeoZ, Height, Pressure), if any. */
  @Nullable
  GridAxis getVerticalAxis();

  /** Get the Horizontal CoordinateSystem. */
  GridHorizCoordinateSystem getHorizCoordSystem();

  /** Get the X axis (either GeoX or Lon). */
  default GridAxis getXHorizAxis() {
    return getHorizCoordSystem().getXHorizAxis();
  }

  /** Get the Y axis (either GeoY or Lat). */
  default GridAxis getYHorizAxis() {
    return getHorizCoordSystem().getYHorizAxis();
  }

  /** Nominal shape, may differ from materialized shape. */
  int[] getNominalShape();

  /** Function description, eg GRID(T,Z,Y,Z):R LOOK needed? */
  String showFnSummary();

  void show(Formatter f, boolean showCoords);
}
