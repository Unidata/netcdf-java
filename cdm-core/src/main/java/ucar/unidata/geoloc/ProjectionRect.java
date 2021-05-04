/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import com.google.common.math.DoubleMath;
import java.util.StringTokenizer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.util.Misc;

/**
 * Bounding box for ProjectionPoints.
 * Note that getX() getY() really means getMinX(), getMinY(), rather than
 * "upper left point" of the rectangle.
 */
@Immutable
public class ProjectionRect {
  /**
   * default constructor, initialized to center (0,0) and width (10000, 10000)
   */
  public ProjectionRect() {
    this(-5000, -5000, 5000, 5000);
  }

  /**
   * Construct a ProjectionRect from any two opposite corner points.
   *
   * @param corner1 a corner.
   * @param corner2 the opposite corner.
   */
  public ProjectionRect(ProjectionPoint corner1, ProjectionPoint corner2) {
    this(corner1.getX(), corner1.getY(), corner2.getX(), corner2.getY());
  }

  /**
   * Construct a ProjectionRect from any two opposite corner points.
   *
   * @param minimum lower left corner, ie the minimum x and y
   * @param width x width.
   * @param height y height
   */
  public ProjectionRect(ProjectionPoint minimum, double width, double height) {
    this.x = minimum.getX();
    this.y = minimum.getY();
    this.width = width;
    this.height = height;
  }

  /**
   * construct a ProjectionRect from any two opposite corner points
   *
   * @param x1 x coord of any corner of the bounding box
   * @param y1 y coord of the same corner as x1
   * @param x2 x coord of opposite corner from x1,y1
   * @param y2 y coord of same corner as x2
   */
  public ProjectionRect(double x1, double y1, double x2, double y2) {
    this.width = Math.abs(x1 - x2);
    this.height = Math.abs(y1 - y2);
    double wx0 = 0.5 * (x1 + x2);
    double wy0 = 0.5 * (y1 + y2);
    this.x = wx0 - width / 2;
    this.y = wy0 - height / 2;
  }

  /**
   * Construct a bounding box from a string, or null if incorrect format.
   *
   * @param spec "startx, starty, width, height"
   */
  @Nullable
  public static ProjectionRect fromSpec(String spec) {
    StringTokenizer stoker = new StringTokenizer(spec, " ,");
    int n = stoker.countTokens();
    if (n != 4) {
      return null;
    }
    try {
      double x = Double.parseDouble(stoker.nextToken());
      double y = Double.parseDouble(stoker.nextToken());
      double width = Double.parseDouble(stoker.nextToken());
      double height = Double.parseDouble(stoker.nextToken());
      return ProjectionRect.builder().setX(x).setY(y).setWidth(width).setHeight(height).build();
    } catch (Exception e) {
      return null;
    }
  }

  /** @deprecated use fromSpec(String spec) */
  @Deprecated
  public ProjectionRect(String spec) {
    StringTokenizer stoker = new StringTokenizer(spec, " ,");
    int n = stoker.countTokens();
    if (n != 4) {
      throw new IllegalArgumentException("Must be 4 numbers = lat, lon, latWidth, lonWidth");
    }
    this.x = Double.parseDouble(stoker.nextToken());
    this.y = Double.parseDouble(stoker.nextToken());
    this.width = Double.parseDouble(stoker.nextToken());
    this.height = Double.parseDouble(stoker.nextToken());
  }

  /** @deprecated use getMinX */
  @Deprecated
  public double getX() {
    return x;
  }

  /** @deprecated use getMinY */
  @Deprecated
  public double getY() {
    return y;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  ////////////////////////////////////////////////////
  // taken from java.awt.geom.Rectangle2D, removed because awt missing on android

  /**
   * Returns the smallest X coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the smallest X coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMinX() {
    return getX();
  }

  /**
   * Returns the smallest Y coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the smallest Y coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMinY() {
    return getY();
  }

  /**
   * Returns the largest X coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the largest X coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMaxX() {
    return getX() + getWidth();
  }

  /**
   * Returns the largest Y coordinate of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   *
   * @return the largest Y coordinate of the framing
   *         rectangle of the <code>Shape</code>.
   * @since 1.2
   */
  public double getMaxY() {
    return getY() + getHeight();
  }

  /**
   * Returns the X coordinate of the center of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   * 
   * @return the X coordinate of the center of the framing rectangle
   *         of the <code>Shape</code>.
   * @since 1.2
   */
  public double getCenterX() {
    return getX() + getWidth() / 2.0;
  }

  /**
   * Returns the Y coordinate of the center of the framing
   * rectangle of the <code>Shape</code> in <code>double</code>
   * precision.
   * 
   * @return the Y coordinate of the center of the framing rectangle
   *         of the <code>Shape</code>.
   * @since 1.2
   */
  public double getCenterY() {
    return getY() + getHeight() / 2.0;
  }

  public boolean isEmpty() {
    return (width <= 0.0) || (height <= 0.0);
  }

  public boolean intersects(ProjectionRect r) {
    return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
  }

  public boolean intersects(double x, double y, double w, double h) {
    if (isEmpty() || w <= 0 || h <= 0) {
      return false;
    }
    double x0 = getX();
    double y0 = getY();
    return (x + w > x0 && y + h > y0 && x < x0 + getWidth() && y < y0 + getHeight());
  }

  /**
   * Intersects the pair of specified source <code>Rectangle2D</code>
   * objects and puts the result into the specified destination
   * <code>Rectangle2D</code> object. One of the source rectangles
   * can also be the destination to avoid creating a third Rectangle2D
   * object, but in this case the original points of this source
   * rectangle will be overwritten by this method.
   * 
   * @param src1 the first of a pair of <code>Rectangle2D</code>
   *        objects to be intersected with each other
   * @param src2 the second of a pair of <code>Rectangle2D</code>
   *        objects to be intersected with each other
   * @return the <code>ProjectionRect</code> that holds the
   *         results of the intersection of <code>src1</code> and
   *         <code>src2</code>
   * @since 1.2
   */
  public static ProjectionRect intersect(ProjectionRect src1, ProjectionRect src2) {
    double x1 = Math.max(src1.getMinX(), src2.getMinX());
    double y1 = Math.max(src1.getMinY(), src2.getMinY());
    double x2 = Math.min(src1.getMaxX(), src2.getMaxX());
    double y2 = Math.min(src1.getMaxY(), src2.getMaxY());
    return builder().setX(x1).setY(y1).setWidth(x2 - x1).setHeight(y2 - y1).build();
  }

  /**
   * Returns {@code true} if this bounding box contains {@code point}.
   *
   * @param point a point in projection coordinates.
   * @return {@code true} if this bounding box contains {@code point}.
   */
  public boolean contains(ProjectionPoint point) {
    return DoubleMath.fuzzyCompare(point.getX(), getMinX(), 1e-6) >= 0
        && DoubleMath.fuzzyCompare(point.getX(), getMaxX(), 1e-6) <= 0
        && DoubleMath.fuzzyCompare(point.getY(), getMinY(), 1e-6) >= 0
        && DoubleMath.fuzzyCompare(point.getY(), getMaxY(), 1e-6) <= 0;
  }

  /**
   *
   * Returns {@code true} if this bounding box contains {@code rect}.
   *
   * @param rect a bounding box in projection coordinates
   * @return {@code true} if this bounding box contains {@code rect}.
   */
  public boolean contains(ProjectionRect rect) {
    boolean contained;
    // The old ProjectionRect class was based off of java.awt.Rectangle.
    // If the rectangles were the same, .contains(rect) returned true.
    // This check makes sure the old behavior is preserved.
    if (this.equals(rect)) {
      contained = true;
    } else {
      // Just check to see if corners of rect contained within this
      contained = (this.contains(rect.getMinPoint()) && this.contains(rect.getMaxPoint()));
    }
    return contained;
  }

  /////////////////////////////////////////////////////

  /**
   * Get the Lower Right Point
   *
   * @return the Lower Right Point
   */
  public ProjectionPoint getLowerRightPoint() {
    return ProjectionPoint.create(getMaxPoint().getX(), getMinPoint().getY());
  }

  /**
   * Get the Upper Left Point (same as getMaxPoint)
   *
   * @return the Upper Left Point
   */
  public ProjectionPoint getUpperRightPoint() {
    return getMaxPoint();
  }

  /**
   * Get the Lower Right Point (same as getMinPoint)
   *
   * @return the Lower Right Point
   */
  public ProjectionPoint getLowerLeftPoint() {
    return getMinPoint();
  }

  /**
   * Get the Upper Left Point
   *
   * @return the Upper Left Point
   */
  public ProjectionPoint getUpperLeftPoint() {
    return ProjectionPoint.create(getMinPoint().getX(), getMaxPoint().getY());
  }

  /**
   * Get the minimum corner of the bounding box.
   *
   * @return minimum corner of the bounding box
   */
  public ProjectionPoint getMinPoint() {
    return ProjectionPoint.create(getX(), getY());
  }

  /**
   * Get the maximum corner of the bounding box.
   *
   * @return maximum corner of the bounding box
   */
  public ProjectionPoint getMaxPoint() {
    return ProjectionPoint.create(getX() + getWidth(), getY() + getHeight());
  }

  /**
   * Get a String representation of this object.
   *
   * @return a String representation of this object.
   */
  public String toString1() {
    return String.format("min: %.3f %.3f size: %.3f %.3f", getX(), getY(), getWidth(), getHeight());
  }

  public String toString2(int ndec) {
    String f = " %." + ndec + "f";
    return String.format("min:" + f + f + " max:" + f + f, getX(), getY(), getMaxX(), getMaxY());
  }

  /**
   * Return a String representation of this ProjectionRect that can be used in new ProjectionRect(String):
   * "x, y, width, height"
   */
  public String toString() {
    return String.format("%f, %f, %f, %f", x, y, getWidth(), getHeight());
  }

  // Exact comparison is needed in order to be consistent with hashCode().
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProjectionRect that = (ProjectionRect) o;

    if (Double.compare(that.height, height) != 0)
      return false;
    if (Double.compare(that.width, width) != 0)
      return false;
    if (Double.compare(that.x, x) != 0)
      return false;
    return Double.compare(that.y, y) == 0;

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(width);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(height);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Returns the result of {@link #nearlyEquals(ProjectionRect, double)}, with {@link Misc#defaultMaxRelativeDiffFloat}.
   */
  public boolean nearlyEquals(ProjectionRect other) {
    return nearlyEquals(other, Misc.defaultMaxRelativeDiffFloat);
  }

  /**
   * Returns {@code true} if this rectangle is nearly equal to {@code other}. The "near equality" of corners is
   * determined using {@link ProjectionPoint#nearlyEquals(ProjectionPoint, double)}, with the specified maxRelDiff.
   *
   * @param other the other rectangle to check.
   * @param maxRelDiff the maximum {@link Misc#relativeDifference relative difference} that two corners may have.
   * @return {@code true} if this rectangle is nearly equal to {@code other}.
   */
  public boolean nearlyEquals(ProjectionRect other, double maxRelDiff) {
    return this.getLowerLeftPoint().nearlyEquals(other.getLowerLeftPoint(), maxRelDiff)
        && this.getUpperRightPoint().nearlyEquals(other.getUpperRightPoint(), maxRelDiff);
  }

  //////////////////////////////////////////////////////////////////////////
  private final double x;
  private final double y;
  private final double width;
  private final double height;

  private ProjectionRect(ProjectionRect.Builder builder) {
    this.x = builder.x;
    this.y = builder.y;
    this.width = builder.width;
    this.height = builder.height;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public ProjectionRect.Builder toBuilder() {
    return builder().setX(this.x).setY(this.y).setWidth(this.width).setHeight(this.height);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(double x1, double y1, double x2, double y2) {
    double wx0 = 0.5 * (x1 + x2);
    double wy0 = 0.5 * (y1 + y2);
    double width = Math.abs(x1 - x2);
    double height = Math.abs(y1 - y2);
    return new Builder().setWidth(width).setHeight(height).setX(wx0 - width / 2).setY(wy0 - height / 2);
  }

  public static class Builder {
    private double x;
    private double y;
    private double width;
    private double height;

    public double getWidth() {
      return width;
    }

    public double getHeight() {
      return height;
    }

    public Builder setX(double x) {
      this.x = x;
      return this;
    }

    public Builder setY(double y) {
      this.y = y;
      return this;
    }

    public Builder setWidth(double width) {
      this.width = width;
      return this;
    }

    public Builder setHeight(double height) {
      this.height = height;
      return this;
    }

    public Builder setRect(double x, double y, double w, double h) {
      this.x = x;
      this.y = y;
      this.width = w;
      this.height = h;
      return this;
    }

    public Builder setRect(ProjectionRect r) {
      return setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /** Extend the rectangle by the given rectangle. */
    public Builder add(ProjectionRect r) {
      double x1 = Math.min(this.x, r.getMinX());
      double x2 = Math.max(this.x + this.height, r.getMaxX());
      double y1 = Math.min(this.y, r.getMinY());
      double y2 = Math.max(this.y + this.height, r.getMaxY());
      this.x = x1;
      this.y = y1;
      this.width = x2 - x1;
      this.height = y2 - y1;
      return this;
    }

    /** Extend the rectangle by the given point. */
    public Builder add(double newx, double newy) {
      double x1 = Math.min(this.x, newx);
      double x2 = Math.max(this.x + this.height, newx);
      double y1 = Math.min(this.y, newy);
      double y2 = Math.max(this.y + this.height, newy);
      this.x = x1;
      this.y = y1;
      this.width = x2 - x1;
      this.height = y2 - y1;
      return this;
    }

    /** Extend the rectangle by the given point. */
    public Builder add(ProjectionPoint pt) {
      add(pt.getX(), pt.getY());
      return this;
    }

    public ProjectionRect build() {
      return new ProjectionRect(this);
    }
  }
}
