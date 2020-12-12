/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.auto.value.AutoValue;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.ma2.DataType;

/** A data array with GeoReferencing. */
@AutoValue
public abstract class GridReferencedArray {
  public abstract String gridName();

  public abstract ArrayType arrayType();

  public abstract Array<Number> data(); // not reduced

  public abstract GridCoordinateSystem csSubset();

  public static GridReferencedArray create(String gridName, ArrayType arrayType, Array<Number> data,
      GridCoordinateSystem csSubset) {
    return new AutoValue_GridReferencedArray(gridName, arrayType, data, csSubset);
  }

}
