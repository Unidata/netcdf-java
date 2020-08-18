/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import com.google.auto.value.AutoValue;
import ucar.nc2.util.Misc;

/** Points on the Projective geometry plane. */
@AutoValue
public abstract class ProjectionPoint {

  /** Create a ProjectionPoint. */
  public static ProjectionPoint create(double x, double y) {
    return new AutoValue_ProjectionPoint(x, y);
  }

  /** Get the X coordinate */
  public abstract double getX();

  /** Get the Y coordinate */
  public abstract double getY();

  /**
   * Returns the result of {@link #nearlyEquals(ProjectionPoint, double)}, with
   * {@link Misc#defaultMaxRelativeDiffDouble}.
   */
  public boolean nearlyEquals(ProjectionPoint other) {
    return nearlyEquals(other, Misc.defaultMaxRelativeDiffDouble);
  }

  /**
   * Returns {@code true} if this point is nearly equal to {@code other}. The "near equality" of points is determined
   * using {@link Misc#nearlyEquals(double, double, double)}, with the specified maxRelDiff.
   *
   * @param other the other point to check.
   * @param maxRelDiff the maximum {@link Misc#relativeDifference relative difference} the two points may have.
   * @return {@code true} if this point is nearly equal to {@code other}.
   */
  public boolean nearlyEquals(ProjectionPoint other, double maxRelDiff) {
    return Misc.nearlyEquals(getX(), other.getX(), maxRelDiff) && Misc.nearlyEquals(getY(), other.getY(), maxRelDiff);
  }
}
