/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc;

import javax.annotation.concurrent.Immutable;

/** Defines the shape of the earth ellipsoid. */
@Immutable
public class Earth {
  public static final Earth DEFAULT = new Earth();

  /** Get canonical radius of the spherical earth in meters from "WGS 84" */
  public static final double WGS84_EARTH_RADIUS_METERS = 6371229.;

  /** Get canonical radius of the spherical earth in km from "WGS 84" */
  public static final double WGS84_EARTH_RADIUS_KM = 6371.229;

  ///////////////////////////////////////////////////////////////////////////////////
  private final double eccentricity; // eccentricity
  private final double eccentricitySquared; // eccentricity squared
  private final double equatorRadius; // equatorial radius (semimajor axis)
  private final double poleRadius; // polar radius (semiminor axis)
  private final double flattening; // flattening
  private final String name;

  /** Spherical earth with canonical radius. */
  public Earth() {
    this(WGS84_EARTH_RADIUS_METERS);
  }

  /**
   * Assume a spherical cow, I mean earth.
   *
   * @param radius radius of spherical earth.
   */
  public Earth(double radius) {
    this.equatorRadius = radius;
    this.poleRadius = radius;
    this.flattening = 0.0;
    this.eccentricity = 1.0;
    this.eccentricitySquared = 1.0;
    this.name = "spherical_earth";
  }

  /**
   * Create an ellipsoidal earth.
   * The reciprocalFlattening is used if not zero, else the poleRadius is used.
   *
   * @param equatorRadius equatorial radius (semimajor axis) in meters, must be specified
   * @param poleRadius polar radius (semiminor axis) in meters
   * @param reciprocalFlattening inverse flattening = 1/flattening = a / (a-b)
   */
  public Earth(double equatorRadius, double poleRadius, double reciprocalFlattening) {
    this(equatorRadius, poleRadius, reciprocalFlattening, "ellipsoidal_earth");
  }

  /**
   * Create an ellipsoidal earth with a name.
   *
   * @param equatorRadius equatorial radius (semimajor axis) in meters, must be specified
   * @param poleRadius polar radius (semiminor axis) in meters; if reciprocalFlattening != 0 then this is ignored
   * @param reciprocalFlattening inverse flattening = 1/flattening = a / (a-b); if 0 use the poleRadius to calculate
   * @param name name of the Earth
   */
  public Earth(double equatorRadius, double poleRadius, double reciprocalFlattening, String name) {
    this.equatorRadius = equatorRadius;
    this.name = name;
    if (reciprocalFlattening != 0) {
      flattening = 1.0 / reciprocalFlattening;
      eccentricitySquared = 2 * flattening - flattening * flattening;
      this.poleRadius = equatorRadius * Math.sqrt(1.0 - eccentricitySquared);
    } else {
      this.poleRadius = poleRadius;
      eccentricitySquared = 1.0 - (poleRadius * poleRadius) / (equatorRadius * equatorRadius);
      flattening = 1.0 - poleRadius / equatorRadius;
    }
    eccentricity = Math.sqrt(eccentricitySquared);
  }

  /** Is this a spherical earth? */
  public boolean isSpherical() {
    return flattening == 0.0;
  }

  /** Get the equatorial radius (semimajor axis) of the earth, in meters. */
  public double getMajor() {
    return equatorRadius;
  }

  /** Get the polar radius (semiminor axis) of the earth, in meters. */
  public double getMinor() {
    return poleRadius;
  }

  /** Get the Name. */
  public String getName() {
    return name;
  }

  /** Get the Eccentricity . */
  public double getEccentricity() {
    return eccentricity;
  }

  /** Get the Eccentricity squared. */
  public double getEccentricitySquared() {
    return eccentricitySquared;
  }

  /** Get the Equatorial Radius in meters. */
  public double getEquatorRadius() {
    return equatorRadius;
  }

  /** Get the Polar Radius in meters. */
  public double getPoleRadius() {
    return poleRadius;
  }

  /** Get the Flattening. */
  public double getFlattening() {
    return flattening;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Earth earth = (Earth) o;

    if (Double.compare(earth.equatorRadius, equatorRadius) != 0)
      return false;
    return Double.compare(earth.poleRadius, poleRadius) == 0;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = equatorRadius != +0.0d ? Double.doubleToLongBits(equatorRadius) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = poleRadius != +0.0d ? Double.doubleToLongBits(poleRadius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s equatorRadius=%f inverseFlattening=%f", name, equatorRadius, (1.0 / flattening));
  }

}

