/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.geoloc.vertical;

import ucar.array.Array;

public interface DataReader {

  Array<Number> read(String varName);

  String getUnits(String varName);
}
