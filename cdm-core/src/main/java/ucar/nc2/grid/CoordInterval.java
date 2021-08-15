/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.math.DoubleMath;
import ucar.unidata.util.Format;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A Coordinate represented by an interval [start, end)
 * LOOK double vs int ??
 */
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

  public String toString() {
    return toString(3);
  }

  /** Show the interval with given decimal precision. */
  public String toString(int ndecimals) {
    return Format.d(start(), ndecimals) + "-" + Format.d(end(), ndecimals);
  }

  /** The inverse of toString(), or null if cant parse */
  @Nullable
  public static CoordInterval parse(String source) {
    List<String> ss = Splitter.on('-').omitEmptyStrings().trimResults().splitToList(source);
    if (ss.size() != 2) {
      return null;
    }
    try {
      double start = Double.parseDouble(ss.get(0));
      double end = Double.parseDouble(ss.get(1));
      return CoordInterval.create(start, end);
    } catch (Exception e) {
      return null;
    }
  }
}
