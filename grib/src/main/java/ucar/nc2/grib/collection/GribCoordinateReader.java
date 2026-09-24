/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.Section;
import ucar.nc2.ProxyReader;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;

/** Generates a regular horizontal coordinate only when it is read. */
final class GribCoordinateReader implements ProxyReader {
  private final double start;
  private final double increment;

  GribCoordinateReader(double start, double increment) {
    this.start = start;
    this.increment = increment;
  }

  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) {
    return Array.makeArray(client.getDataType(), (int) client.getSize(), start, increment);
  }

  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask) {
    Array result = Array.factory(client.getDataType(), section.getShape());
    IndexIterator iterator = result.getIndexIterator();
    for (int index : section.getRange(0)) {
      // Preserve the full coordinate's arithmetic, including rounding, for sections and strides.
      iterator.setDoubleNext(start + index * increment);
    }
    return result;
  }
}
