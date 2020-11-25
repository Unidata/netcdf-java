/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.auto.value.AutoValue;
import com.google.common.math.DoubleMath;
import ucar.unidata.util.Format;

/** A Coordinate represented by an interval [start, end) */
@AutoValue
public abstract class CoordInterval {
  /** The starting value of the coordinate interval */
  public abstract double start();

  /** The ending value of the coordinate interval */
  public abstract double end();

  /** Create an interval. */
  public static CoordInterval create(double start, double end) {
    return new AutoValue_CoordInterval(start, end);
  }

  /** The midpoint between start and end. */
  public double midpoint() {
    return (start() + end()) / 2;
  }

  /** Compare two intervals to within the given tolerence. */
  public boolean fuzzyEquals(CoordInterval other, double tol) {
    return DoubleMath.fuzzyEquals(start(), other.start(), tol) && DoubleMath.fuzzyEquals(end(), other.end(), tol);
  }

  /** Show the interval with given decimal precision. */
  public String toString(int ndecimals) {
    return Format.d(start(), ndecimals) + "-" + Format.d(end(), ndecimals);
  }
}
