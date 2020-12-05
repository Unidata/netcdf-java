/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.IsMissingEvaluator;
import ucar.nc2.AttributeContainer;

import javax.annotation.Nullable;
import java.io.IOException;

/** A georeferenced Field of data. */
public interface Grid extends IsMissingEvaluator {

  String getName();

  @Nullable
  String getDescription();

  String getUnits();

  AttributeContainer attributes();

  DataType getDataType();

  GridCoordinateSystem getCoordinateSystem();

  /** Read the specified subset of data, return result as a Georeferenced Array. */
  GridReferencedArray readData(GridSubset subset) throws IOException, InvalidRangeException;
}
