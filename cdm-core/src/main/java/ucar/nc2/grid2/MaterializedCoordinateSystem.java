/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;
import ucar.array.RangeIterator;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.List;

/** A Coordinate System for materialized gridded data. */
public interface MaterializedCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  String getName();

  /** the GridAxes that constitute this Coordinate System */
  ImmutableList<GridAxis<?>> getGridAxes();

  /** Get the ensemble axis. */
  @Nullable
  GridAxisPoint getEnsembleAxis();

  /** Get the Z axis (GeoZ, Height, Pressure), if any. */
  @Nullable
  GridAxis<?> getVerticalAxis();

  /** Get the Runtime axis. */
  @Nullable
  GridAxisPoint getRunTimeAxis();

  /** Get the Time Offset axis. */
  @Nullable
  GridAxis<?> getTimeOffsetAxis();

  /** Get the X axis. (either GeoX or Lon) */
  GridAxisPoint getXHorizAxis();

  /** Get the Y axis. (either GeoY or Lat) */
  GridAxisPoint getYHorizAxis();

  /** Get the Horizontal CoordinateSystem. */
  GridHorizCoordinateSystem getHorizCoordSystem();

  void show(Formatter f, boolean showCoords);

  /** The index ranges in this array. */
  List<RangeIterator> getRanges();

  /** The shape of this array. */
  int[] getMaterializedShape();
}
