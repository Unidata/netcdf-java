/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import ucar.array.RangeIterator;
import ucar.nc2.grid2.GridSubset;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** A Coordinate System for materialized gridded data. */
public interface MaterializedCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  String getName();

  /** the GridAxes that constitute this Coordinate System */
  Iterable<GridAxis> getGridAxes();

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

  void show(Formatter f, boolean showCoords);

  /** Subset each axis based on the given parameters. */
  Optional<MaterializedCoordinateSystem> subset(GridSubset params, Formatter errLog);

  /** The index ranges in this array. */
  List<RangeIterator> getRanges();

  /** The shape of this array. */
  int[] getMaterializedShape();
}
