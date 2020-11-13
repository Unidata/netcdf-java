/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.auto.value.AutoValue;
import ucar.unidata.util.Format;

/** A Coordinate represented by an interval [start, end) */
@AutoValue
public abstract class CoordInterval {
  /** The starting value of the coordinate interval */
  public abstract double start();

  /** The ending value of the coordinate interval */
  public abstract double end();

  /** Number of digits to right of decimal place in toString(). Default is 3. */
  public abstract int ndecimals();

  /** Create an interval with default decimals. */
  public static CoordInterval create(double start, double end) {
    return new AutoValue_CoordInterval(start, end, 3);
  }

  /** Create an interval with specified decimals. */
  public static CoordInterval create(double start, double end, int ndec) {
    return new AutoValue_CoordInterval(start, end, ndec);
  }

  @Override
  public String toString() {
    return Format.d(start(), ndecimals()) + "-" + Format.d(end(), ndecimals());
  }

  /** Convert to a double[2] primitive array. */
  public double[] toPrimitiveArray() {
    return new double[] {start(), end()};
  }
}
