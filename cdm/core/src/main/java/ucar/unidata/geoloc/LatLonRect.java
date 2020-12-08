/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.util.Misc;
import ucar.unidata.util.Format;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * Bounding box for latitude/longitude points.
 * This is a rectangle in lat/lon coordinates.
 * This class handles the longitude wrapping problem.
 * The Rectangle always starts from lowerLeft, goes east width degrees until upperRight
 * For latitude, lower < upper.
 * Since the longitude must be in the range +/-180., right may be less or greater than left.
 */
@Immutable
public class LatLonRect {
  public static LatLonRect INVALID = LatLonRect.builder(LatLonPoint.INVALID, LatLonPoint.INVALID).build();

  /** upper right corner */
  private final LatLonPoint upperRight;

  /** lower left corner */
  private final LatLonPoint lowerLeft;

  /** flag for dateline cross */
  private final boolean crossDateline;

  /** All longitudes are included */
  private final boolean allLongitude;

  /** longitude width */
  private final double width;

  /** Create a LatLonRect that covers the whole world. */
  public LatLonRect() {
    this(new LatLonRect.Builder(LatLonPoint.create(-90, -180), 180, 360));
  }

  /**
   * Construct a lat/lon bounding box from a point, and a delta lat, lon.
   * This disambiguates which way the box wraps around the globe.
   *
   * @param p1 one corner of the box
   * @param deltaLat delta lat from p1. (may be positive or negetive)
   * @param deltaLon delta lon from p1. (may be positive or negetive). A negetive value is interepreted as
   *        indicating a wrap, and 360 is added to it.
   */
  public LatLonRect(LatLonPoint p1, double deltaLat, double deltaLon) {
    this(builder(p1, deltaLat, deltaLon));
  }

  /**
   * Construct a lat/lon bounding box from unnormalized longitudes.
   * 
   * @param lat0 lat of starting point
   * @param lon0 lon of starting point
   * @param lat1 lat of ending point
   * @param lon1 lon of ending point
   */
  public LatLonRect(double lat0, double lon0, double lat1, double lon1) {
    this(builder(lat0, lon0, lat1, lon1));
  }

  /** Get the upper right corner of the bounding box. */
  public LatLonPoint getUpperRightPoint() {
    return upperRight;
  }

  /** Get the lower left corner of the bounding box. */
  public LatLonPoint getLowerLeftPoint() {
    return lowerLeft;
  }

  /** Get the upper left corner of the bounding box. */
  public LatLonPoint getUpperLeftPoint() {
    return LatLonPoint.create(upperRight.getLatitude(), lowerLeft.getLongitude());
  }

  /** Get the lower left corner of the bounding box. */
  public LatLonPoint getLowerRightPoint() {
    return LatLonPoint.create(lowerLeft.getLatitude(), upperRight.getLongitude());
  }

  /** Get whether the bounding box crosses the +/- 180 seam */
  public boolean crossDateline() {
    return crossDateline;
  }

  /** Get whether the bounding box contains all longitudes. */
  public boolean isAllLongitude() {
    return allLongitude;
  }

  /** return width of bounding box in degrees longitude, always between 0 and 360 degrees. */
  public double getWidth() {
    return width;
  }

  /** return height of bounding box in degrees latitude, always between 0 and 180 degrees. */
  public double getHeight() {
    return getLatMax() - getLatMin();
  }

  /** return center Longitude, always in the range +/-180 */
  public double getCenterLon() {
    double lon0 = (upperRight.getLongitude() + lowerLeft.getLongitude()) / 2;
    if (crossDateline) {
      lon0 -= 180;
    }
    return lon0;
  }

  /** Get minimum longitude, aka "west" edge. This may be > LonMax when crossDateline is true. */
  public double getLonMin() {
    return lowerLeft.getLongitude();
  }

  /** Get maximum longitude, aka "east" edge. */
  public double getLonMax() {
    return lowerLeft.getLongitude() + width;
  }

  /** Get minimum latitude, aka "south" edge */
  public double getLatMin() {
    return lowerLeft.getLatitude();
  }

  /** Get maximum latitude, aka "north" edge */
  public double getLatMax() {
    return upperRight.getLatitude();
  }

  // Exact comparison is needed in order to be consistent with hashCode().
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LatLonRect that = (LatLonRect) o;
    return Objects.equals(upperRight, that.upperRight) && Objects.equals(lowerLeft, that.lowerLeft);
  }

  @Override
  public int hashCode() {
    return Objects.hash(upperRight, lowerLeft);
  }

  /**
   * Returns the result of {@link #nearlyEquals(LatLonRect, double)}, with {@link Misc#defaultMaxRelativeDiffFloat}.
   */
  public boolean nearlyEquals(LatLonRect other) {
    return nearlyEquals(other, Misc.defaultMaxRelativeDiffFloat);
  }

  /**
   * Returns {@code true} if this rectangle is nearly equal to {@code other}. The "near equality" of corners is
   * determined using {@link LatLonPoint#nearlyEquals(LatLonPoint, double)}, with the specified maxRelDiff.
   *
   * @param other the other rectangle to check.
   * @param maxRelDiff the maximum {@link Misc#relativeDifference relative difference} that two corners may have.
   * @return {@code true} if this rectangle is nearly equal to {@code other}.
   */
  public boolean nearlyEquals(LatLonRect other, double maxRelDiff) {
    return this.getLowerLeftPoint().nearlyEquals(other.getLowerLeftPoint(), maxRelDiff)
        && this.getUpperRightPoint().nearlyEquals(other.getUpperRightPoint(), maxRelDiff);
  }

  /**
   * Determine if a specified LatLonPoint is contained in this bounding box.
   *
   * @param p the specified point to be tested
   * @return true if point is contained in this bounding box
   */
  public boolean contains(LatLonPoint p) {
    return contains(p.getLatitude(), p.getLongitude());
  }

  /**
   * Determine if the given lat/lon point is contined inside this rectangle.
   *
   * @param lat lat of point
   * @param lon lon of point
   * @return true if the given lat/lon point is contained inside this rectangle
   */
  public boolean contains(double lat, double lon) {
    // check lat first
    double eps = 1.0e-9;
    if ((lat + eps < lowerLeft.getLatitude()) || (lat - eps > upperRight.getLatitude())) {
      return false;
    }

    if (allLongitude)
      return true;

    if (crossDateline) {
      // bounding box crosses the +/- 180 seam
      return ((lon >= lowerLeft.getLongitude()) || (lon <= upperRight.getLongitude()));
    } else {
      // check "normal" lon case
      return ((lon >= lowerLeft.getLongitude()) && (lon <= upperRight.getLongitude()));
    }
  }

  /**
   * Determine if this bounding box is contained in another LatLonRect.
   *
   * @param b the other box to see if it contains this one
   * @return true if b contained in this bounding box
   */
  public boolean containedIn(LatLonRect b) {
    return (b.getWidth() >= width) && b.contains(upperRight) && b.contains(lowerLeft);
  }

  /**
   * Create the intersection of this LatLonRect with another LatLonRect.
   *
   * @param clip intersect with this
   * @return intersection, or null if there is no intersection
   */
  @Nullable
  public LatLonRect intersect(LatLonRect clip) {
    double latMin = Math.max(getLatMin(), clip.getLatMin());
    double latMax = Math.min(getLatMax(), clip.getLatMax());
    double deltaLat = latMax - latMin;
    if (deltaLat < 0)
      return null;

    // lon as always is a pain : if not intersection, try +/- 360
    double lon1min = getLonMin();
    double lon1max = getLonMax();
    double lon2min = clip.getLonMin();
    double lon2max = clip.getLonMax();
    if (!intersect(lon1min, lon1max, lon2min, lon2max)) {
      lon2min = clip.getLonMin() + 360;
      lon2max = clip.getLonMax() + 360;
      if (!intersect(lon1min, lon1max, lon2min, lon2max)) {
        lon2min = clip.getLonMin() - 360;
        lon2max = clip.getLonMax() - 360;
      }
    }

    // we did our best to find an intersection
    double lonMin = Math.max(lon1min, lon2min);
    double lonMax = Math.min(lon1max, lon2max);
    double deltaLon = lonMax - lonMin;
    if (deltaLon < 0)
      return null;

    return new LatLonRect.Builder(LatLonPoint.create(latMin, lonMin), deltaLat, deltaLon).build();
  }

  private boolean intersect(double min1, double max1, double min2, double max2) {
    double min = Math.max(min1, min2);
    double max = Math.min(max1, max2);
    return min < max;
  }

  /**
   * Return a String representation of this object.
   * 
   * <pre>
   * eg: ll: 90.0S .0E+ ur: 90.0N .0E
   * </pre>
   *
   * @return a String representation of this object.
   */
  public String toString1() {
    return " ll: " + lowerLeft + " ur: " + upperRight + " width: " + width + " cross: " + crossDateline + " all: "
        + allLongitude;
  }

  /**
   * Return a String representation of this object.
   * 
   * <pre>
   * lat= [-90.00,90.00] lon= [0.00,360.00
   * </pre>
   *
   * @return a String representation of this object.
   */
  public String toString2() {
    return " lat= [" + Format.dfrac(getLatMin(), 2) + "," + Format.dfrac(getLatMax(), 2) + "] lon= ["
        + Format.dfrac(getLonMin(), 2) + "," + Format.dfrac(getLonMax(), 2) + "]";
  }

  /**
   * Return a String representation of this LatLonRect that can be used in new LatLonRect.Builder(String):
   * "lat, lon, deltaLat, deltaLon"
   */
  @Override
  public String toString() {
    return String.format("%f, %f, %f, %f", lowerLeft.getLatitude(), lowerLeft.getLongitude(), getHeight(), getWidth());
  }

  /** Convert to a mutable Builder */
  public Builder toBuilder() {
    return new Builder(this.lowerLeft, this.upperRight.getLatitude() - this.lowerLeft.getLatitude(), this.width);
  }

  /**
   * Construct a lat/lon bounding box from two points. The order of longitude coord of the two points matters:
   * pt1.lon is always the "left" point, then points contained within the box increase (unless crossing the Dateline,
   * in which case they jump to -180, but then start increasing again) until pt2.lon.
   * The order of lat doesnt matter: smaller will go to "lower" point (further south).
   *
   * There is an ambiguity when left = right, since LatLonPoint is normalized. Assume this is the full width = 360 case.
   * 
   * @deprecated use builder(LatLonPoint p1, double deltaLat, double deltaLon).
   *
   * @param left left corner
   * @param right right corner
   */
  @Deprecated
  public static Builder builder(LatLonPoint left, LatLonPoint right) {
    double width = right.getLongitude() - left.getLongitude();
    while (width < 0.0) {
      width += 360;
    }
    if (width == 0.0) {
      width = 360;
    }
    return new Builder(left, right.getLatitude() - left.getLatitude(), width);
  }

  /**
   * Construct a lat/lon bounding box from a point, and a delta lat, lon. This disambiguates which way the box wraps
   * around the globe.
   *
   * @param p1 one corner of the box
   * @param deltaLat delta lat from p1. (may be positive or negetive)
   * @param deltaLon delta lon from p1. (may be positive or negetive)
   */
  public static Builder builder(LatLonPoint p1, double deltaLat, double deltaLon) {
    return new Builder().init(p1, deltaLat, deltaLon);
  }

  /**
   * Construct a lat/lon bounding box from unnormalized longitude.
   * 
   * @param lat0 lat of starting point
   * @param lon0 lon of starting point
   * @param lat1 lat of ending point
   * @param lon1 lon of ending point
   */
  public static Builder builder(double lat0, double lon0, double lat1, double lon1) {
    double width = lon1 - lon0;
    if (Math.abs(width) < 1.0e-8) {
      width = 360.0; // assume its the whole thing
    }
    return new Builder().init(LatLonPoint.create(lat0, lon0), lat1 - lat0, width);
  }

  /**
   * Construct a lat/lon bounding box from a string, or null if format is wrong.
   *
   * @param spec "lat, lon, deltaLat, deltaLon"
   * @see {@link LatLonRect.Builder(LatLonPoint, double, double)}
   */
  @Nullable
  public static LatLonRect fromSpec(String spec) {
    StringTokenizer stoker = new StringTokenizer(spec, " ,");
    int n = stoker.countTokens();
    if (n != 4)
      throw new IllegalArgumentException("Must be 4 numbers = lat, lon, latWidth, lonWidth");
    double lat = Double.parseDouble(stoker.nextToken());
    double lon = Double.parseDouble(stoker.nextToken());
    double deltaLat = Double.parseDouble(stoker.nextToken());
    double deltaLon = Double.parseDouble(stoker.nextToken());

    return new Builder().init(LatLonPoint.create(lat, lon), deltaLat, deltaLon).build();
  }

  /** @deprecated use fromSpec(String spec) */
  @Deprecated
  public static Builder builder(String spec) {
    StringTokenizer stoker = new StringTokenizer(spec, " ,");
    int n = stoker.countTokens();
    if (n != 4)
      throw new IllegalArgumentException("Must be 4 numbers = lat, lon, latWidth, lonWidth");
    double lat = Double.parseDouble(stoker.nextToken());
    double lon = Double.parseDouble(stoker.nextToken());
    double deltaLat = Double.parseDouble(stoker.nextToken());
    double deltaLon = Double.parseDouble(stoker.nextToken());

    return new Builder().init(LatLonPoint.create(lat, lon), deltaLat, deltaLon);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private LatLonRect(Builder builder) {
    this.lowerLeft = LatLonPoint.create(builder.llLat, builder.llLon);
    this.upperRight = LatLonPoint.create(builder.urLat, builder.urLon);
    this.crossDateline = builder.crossDateline;
    this.allLongitude = builder.allLongitudes;
    this.width = builder.width;
  }

  /** A builder of LatLonRect. */
  public static class Builder {
    double llLat;
    double llLon;
    double urLat;
    double urLon;
    double width;
    boolean allLongitudes;
    boolean crossDateline;

    private Builder() {}

    /**
     * @deprecated use LatLonRect.builder(LatLonPoint p1, double deltaLat, double deltaLon), or
     *             new LatLonRect(LatLonPoint p1, double deltaLat, double deltaLon)
     */
    @Deprecated
    public Builder(LatLonPoint left, LatLonPoint right) {
      this(left, right.getLatitude() - left.getLatitude(),
          LatLonPoints.lonNormal360(right.getLongitude() - left.getLongitude()));
    }

    /** @deprecated use LatLonRect.builder(String spec) */
    @Deprecated
    public Builder(String spec) {
      StringTokenizer stoker = new StringTokenizer(spec, " ,");
      int n = stoker.countTokens();
      if (n != 4)
        throw new IllegalArgumentException("Must be 4 numbers = lat, lon, latWidth, lonWidth");
      double lat = Double.parseDouble(stoker.nextToken());
      double lon = Double.parseDouble(stoker.nextToken());
      double deltaLat = Double.parseDouble(stoker.nextToken());
      double deltaLon = Double.parseDouble(stoker.nextToken());

      init(LatLonPoint.create(lat, lon), deltaLat, deltaLon);
    }

    /**
     * @deprecated use LatLonRect.builder(LatLonPoint p1, double deltaLat, double deltaLon), or
     *             new LatLonRect(LatLonPoint p1, double deltaLat, double deltaLon)
     */
    @Deprecated
    public Builder(LatLonPoint p1, double deltaLat, double deltaLon) {
      init(p1, deltaLat, deltaLon);
    }

    private Builder init(LatLonPoint p1, double deltaLat, double deltaLon) {
      double lonmin, lonmax;
      double latmin = Math.min(p1.getLatitude(), p1.getLatitude() + deltaLat);
      double latmax = Math.max(p1.getLatitude(), p1.getLatitude() + deltaLat);
      while (deltaLon < 0) {
        deltaLon += 360;
      }
      while (deltaLon > 360) {
        deltaLon -= 360;
      }

      double lonpt = p1.getLongitude();
      lonmin = lonpt;
      lonmax = lonpt + deltaLon;
      crossDateline = (lonmax > 180.0);

      this.width = deltaLon;
      this.allLongitudes = (this.width >= 360.0);

      this.llLat = latmin;
      this.llLon = LatLonPoints.lonNormal(lonmin);
      this.urLat = latmax;
      this.urLon = LatLonPoints.lonNormal(lonmax);

      return this;
    }

    /** Start with a point, use extend() to add points. */
    public Builder(double startLat, double startLon) {
      this.llLat = startLat;
      this.urLat = startLat;
      this.llLon = LatLonPoints.lonNormal(startLon);
      this.urLon = this.llLon;
      this.allLongitudes = false;
      this.crossDateline = false;
      this.width = 0.0;
    }

    /** Extend the bounding box to contain the given rectangle */
    public Builder extend(LatLonRect r) {
      Preconditions.checkNotNull(r);
      Preconditions.checkNotNull(r);

      // lat is easy
      double latMin = r.getLatMin();
      double latMax = r.getLatMax();

      if (latMax > urLat) {
        urLat = latMax;
      }
      if (latMin < llLat) {
        llLat = latMin;
      }

      // lon is uglier
      if (allLongitudes)
        return this;

      // everything is reletive to current LonMin
      double lonMin = llLon;
      double lonMax = llLon + width;

      double nlonMin = LatLonPoints.lonNormal(r.getLonMin(), lonMin);
      double nlonMax = nlonMin + r.getWidth();
      lonMin = Math.min(lonMin, nlonMin);
      lonMax = Math.max(lonMax, nlonMax);

      width = lonMax - lonMin;
      allLongitudes = width >= 360.0;
      if (allLongitudes) {
        width = 360.0;
        lonMin = -180.0;
      } else {
        lonMin = LatLonPoints.lonNormal(lonMin);
      }

      llLon = LatLonPoints.lonNormal(lonMin);
      urLon = LatLonPoints.lonNormal(lonMin + width);
      crossDateline = llLon > urLon;
      return this;
    }

    /** Extend the bounding box to contain this point */
    public Builder extend(LatLonPoint latlon) {
      return extend(latlon.getLatitude(), latlon.getLongitude());
    }

    /** Extend the bounding box to contain this point */
    public Builder extend(double lat, double lon) {
      lon = LatLonPoints.lonNormal(lon);
      if (contains(lat, lon))
        return this;

      // lat is easy to deal with
      if (lat > urLat) {
        urLat = lat;
      }
      if (lat < llLat) {
        llLat = lat;
      }

      // lon is uglier
      if (allLongitudes) {
        // do nothing

      } else if (crossDateline) {

        // bounding box crosses the +/- 180 seam
        double d1 = lon - urLon;
        double d2 = llLon - lon;
        if ((d1 > 0.0) && (d2 > 0.0)) { // needed ?
          if (d1 > d2) {
            llLon = lon;
          } else {
            urLon = lon;
          }
        }

      } else {

        // normal case
        if (lon > urLon) {
          if (lon - urLon > llLon - lon + 360) {
            crossDateline = true;
            llLon = lon;
          } else {
            urLon = lon;
          }
        } else if (lon < llLon) {
          if (llLon - lon > lon + 360.0 - urLon) {
            crossDateline = true;
            urLon = lon;
          } else {
            llLon = lon;
          }
        }
      }

      // recalc allLongitudes
      this.width = urLon - llLon;
      if (crossDateline) {
        width += 360;
      }
      this.allLongitudes = this.allLongitudes || (width >= 360.0);
      return this;
    }

    private boolean contains(double lat, double lon) {
      // check lat first
      double eps = 1.0e-9;
      if ((lat + eps < llLat) || (lat - eps > urLat)) {
        return false;
      }

      if (allLongitudes) {
        return true;
      }

      if (crossDateline) {
        // bounding box crosses the +/- 180 seam
        return ((lon >= llLon) || (lon <= urLon));
      } else {
        // check "normal" lon case
        return ((lon >= llLon) && (lon <= urLon));
      }
    }

    /** Extend to allLongitudes if width > minWidth */
    public Builder extendToAllLongitudes(double minWidth) {
      if (crossDateline && this.width > minWidth) {
        this.llLon = -180;
        this.urLon = 180;
        this.allLongitudes = true;
      }
      return this;
    }

    /** Expand rect in all directions by delta amount. */
    public Builder expand(double delta) {
      this.llLat -= delta;
      this.urLat += delta;

      this.llLon -= delta;
      this.urLon += delta;

      return this;
    }

    public LatLonRect build() {
      return new LatLonRect(this);
    }

  }

}
