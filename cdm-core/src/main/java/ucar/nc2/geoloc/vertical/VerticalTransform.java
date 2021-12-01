/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/**
 * A transformation to a vertical reference coordinate system, such as height or pressure.
 */
public interface VerticalTransform {

  /** The name of the Vertical Transform. */
  String getName();

  /** The name of the Coordinate Variable Transform container. */
  String getCtvName();

  /** The unit string for the vertical coordinate. */
  @Nullable
  String getUnitString();

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

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * A Builder of VerticalTransforms.
   * Note the use of NetcdfDataset and CoordinateSystem. VerticalTransform are only
   * available on Grids built on NetcdfDataset. GRIB does not have these.
   */
  interface Builder {
    Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog);
  }
}

