/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.auto.value.AutoValue;

/** A value class holding a minimum and a maximum double value. */
@AutoValue
public abstract class MinMax {
  public abstract double min();

  public abstract double max();

  public static MinMax create(double min, double max) {
    return new AutoValue_MinMax(min, max);
  }

  @Override
  public String toString() {
    return "MinMax{" + "min=" + min() + ", max=" + max() + '}';
  }
}
