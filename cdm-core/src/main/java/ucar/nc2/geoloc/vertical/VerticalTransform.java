/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/**
 * A transformation to a vertical reference coordinate system, such as height or pressure.
 * LOOK index space for timeIndex and subset. Could reposition at Grid level and use coordinate space.
 */
public interface VerticalTransform {

  String getName();

  /** The name of the Coordinate Variable Transform container. */
  String getCtvName();

  /**
   * Get the 3D vertical coordinate array for this time step.
   * Must be in "canonical order" : z, y, x.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   *
   * @return 3D vertical coordinate array, for the given t.
   */
  Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException;

  /**
   * Get the 1D vertical coordinate array for this time step and point
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex the x index
   * @param yIndex the y index
   * @return vertical coordinate array
   */
  Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException;

  /** Get the unit string for the vertical coordinate. */
  @Nullable
  String getUnitString();

  /**
   * Create a VerticalTransform as a section of this VerticalTransform.
   * 
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   * @return a new VerticalTransform for the given subset
   */
  VerticalTransform subset(Range t_range, Range z_range, Range y_range, Range x_range);

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** a Builder of VerticalTransforms. */
  interface Builder {
    Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog);
  }
}

