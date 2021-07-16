/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import ucar.array.ArrayType;
import ucar.array.IsMissingEvaluator;
import ucar.nc2.AttributeContainer;
import ucar.nc2.grid2.GridSubset;

import java.io.IOException;

/** A georeferenced Field of data. */
public interface Grid extends IsMissingEvaluator, ucar.ma2.IsMissingEvaluator {

  /** Get Full name of Grid */
  String getName();

  String getDescription();

  String getUnits();

  AttributeContainer attributes();

  ArrayType getArrayType();

  GridCoordinateSystem getCoordinateSystem();

  /** Read the specified subset of data, return result as a Georeferenced Array. */
  GridReferencedArray readData(GridSubset subset) throws IOException, ucar.array.InvalidRangeException;

  default GridReader getReader() {
    return new GridReader(this);
  }

}
