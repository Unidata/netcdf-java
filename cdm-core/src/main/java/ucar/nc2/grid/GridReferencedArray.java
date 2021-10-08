/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.auto.value.AutoValue;
import ucar.array.Array;
import ucar.array.ArrayType;

/** A Grid's data array with Geo referencing. */
@AutoValue
public abstract class GridReferencedArray {
  /** The Grid's name. */
  public abstract String gridName();

  /** The underlying data type. Must imply a subclass of Number. */
  public abstract ArrayType arrayType();

  /** The actual data read. */
  public abstract Array<Number> data(); // LOOK not reduced: should it be ??

  /** The MaterializedCoordinateSystem describing the data that was read. */
  public abstract MaterializedCoordinateSystem getMaterializedCoordinateSystem();

  /** Factory method for creating. */
  public static GridReferencedArray create(String gridName, ArrayType arrayType, Array<Number> data,
      MaterializedCoordinateSystem csSubset) {
    return new AutoValue_GridReferencedArray(gridName, arrayType, data, csSubset);
  }

}
