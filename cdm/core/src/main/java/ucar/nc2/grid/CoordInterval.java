/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.auto.value.AutoValue;
import ucar.unidata.util.Format;

import javax.annotation.concurrent.Immutable;

/** Convenience wrapper for interval coordinates. */
@AutoValue
public abstract class CoordInterval {
  public abstract double start();

  public abstract double end();

  public abstract int ndecimals();

  public static CoordInterval create(double start, double end) {
    return new AutoValue_CoordInterval(start, end, 3);

  }

  public static CoordInterval create(double start, double end, int ndec) {
    return new AutoValue_CoordInterval(start, end, ndec);
  }

  @Override
  public String toString() {
    return Format.d(start(), ndecimals()) + "-" + Format.d(end(), ndecimals());
  }
}
