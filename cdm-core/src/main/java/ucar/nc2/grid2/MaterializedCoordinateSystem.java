/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import ucar.array.RangeIterator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** A Coordinate System for materialized gridded data. */
public interface MaterializedCoordinateSystem {

  @Nullable
  GridTimeCoordinateSystem getTimeCoordSystem();

  /** Get the ensemble axis. */
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

  /** The shape of this array. */
  default List<Integer> getMaterializedShape() {
    List<Integer> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      result.addAll(getTimeCoordSystem().getNominalShape());
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis().getNominalSize());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis().getNominalSize());
    }
    result.addAll(getHorizCoordSystem().getShape());
    return result;
  }

  default List<ucar.array.Range> getSubsetRanges() {
    List<ucar.array.Range> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      result.addAll(getTimeCoordSystem().getSubsetRanges());
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis().getSubsetRange());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis().getSubsetRange());
    }
    result.addAll(getHorizCoordSystem().getSubsetRanges());
    return result;
  }

}
