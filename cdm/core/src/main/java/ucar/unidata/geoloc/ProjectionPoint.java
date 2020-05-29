/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import ucar.nc2.util.Misc;

/**
 * Points on the Projective geometry plane.
 *
 * TODO will be an Immutable class in ver6
 */
public interface ProjectionPoint {

  /** Standard way to create a ProjectionPoint. */
  static ProjectionPoint create(double x, double y) {
    return new ProjectionPointImpl(x, y);
  }

  /** Standard way to create a "default" ProjectionPoint. */
  static ProjectionPoint create() {
    return new ProjectionPointImpl();
  }

  /**
   * Get the X coordinate
   *
   * @return the X coordinate
   */
  double getX();

  /**
   * Get the Y coordinate
   *
   * @return the Y coordinate
   */
  double getY();

  /**
   * Returns the result of {@link #nearlyEquals(ProjectionPoint, double)}, with
   * {@link Misc#defaultMaxRelativeDiffDouble}.
   */
  default boolean nearlyEquals(ProjectionPoint other) {
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
  boolean nearlyEquals(ProjectionPoint other, double maxRelDiff);
}
