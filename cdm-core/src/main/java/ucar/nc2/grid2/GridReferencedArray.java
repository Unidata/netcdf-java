/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import com.google.auto.value.AutoValue;
import ucar.array.Array;
import ucar.array.ArrayType;

/** A data array with Geo referencing. */
@AutoValue
public abstract class GridReferencedArray {
  public abstract String gridName();

  public abstract ArrayType arrayType();

  // LOOK Array<Number> ??
  public abstract Array<Number> data(); // not reduced

  public abstract MaterializedCoordinateSystem getMaterializedCoordinateSystem();

  public static GridReferencedArray create(String gridName, ArrayType arrayType, Array<Number> data,
      MaterializedCoordinateSystem csSubset) {
    return new AutoValue_GridReferencedArray(gridName, arrayType, data, csSubset);
  }

}
