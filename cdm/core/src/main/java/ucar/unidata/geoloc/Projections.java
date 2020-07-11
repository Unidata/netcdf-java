/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

/** Static utilities for Projection. */
public class Projections {
  private static final int INDEX_LAT = 0;
  private static final int INDEX_LON = 1;

  ///////////////////////////////////////////////////////////////////////////////////
  // optimizations for doing double and float arrays

  /**
   * Convert projection coordinates to lat/lon coordinates.
   *
   * @param from array of projection coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the x, y coordinate
   *        of the ith point
   * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
   *         is the lat,lon coordinate of the ith point
   */
  public static double[][] projToLatLon(Projection proj, double[][] from) {
    return projToLatLon(proj, from, new double[2][from[0].length]);
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (x, y) coordinate
   *        of the ith point
   * @param to resulting array of lat/lon coordinates: to[2][n] where
   *        (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *        the ith point
   * @return the "to" array
   */
  public static double[][] projToLatLon(Projection proj, double[][] from, double[][] to) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.projToLatLon:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.projToLatLon:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("Projections.projToLatLon:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      LatLonPoint endL = proj.projToLatLon(from[0][i], from[1][i]);
      to[0][i] = endL.getLatitude();
      to[1][i] = endL.getLongitude();
    }

    return to;
  }

  /**
   * Convert projection coordinates to lat/lon coordinates.
   *
   * @param from array of projection coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the x, y coordinate
   *        of the ith point
   * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
   *         is the lat,lon coordinate of the ith point
   */
  public static float[][] projToLatLon(Projection proj, float[][] from) {
    return projToLatLon(proj, from, new float[2][from[0].length]);
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (x, y) coordinate
   *        of the ith point
   * @param to resulting array of lat/lon coordinates: to[2][n] where
   *        (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *        the ith point
   * @return the "to" array
   */
  public static float[][] projToLatLon(Projection proj, float[][] from, float[][] to) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.projToLatLon:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.projToLatLon:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("Projections.projToLatLon:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      ProjectionPoint ppi = ProjectionPoint.create(from[0][i], from[1][i]);
      LatLonPoint llpi = proj.projToLatLon(ppi);
      to[0][i] = (float) llpi.getLatitude();
      to[1][i] = (float) llpi.getLongitude();
    }

    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the (lat,lon)
   *        coordinate of the ith point
   * @return resulting array of projection coordinates, where to[0][i],
   *         to[1][i] is the (x,y) coordinate of the ith point
   */
  public static double[][] latLonToProj(Projection proj, double[][] from) {
    return latLonToProj(proj, from, new double[2][from[0].length]);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (lat,lon) coordinate
   *        of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate
   *        of the ith point
   * @return the "to" array
   */
  public static double[][] latLonToProj(Projection proj, double[][] from, double[][] to) {
    return latLonToProj(proj, from, to, INDEX_LAT, INDEX_LON);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return resulting array of projection coordinates: to[2][n] where
   *         (to[0][i], to[1][i]) is the (x,y) coordinate of the ith point
   */
  public static double[][] latLonToProj(Projection proj, double[][] from, int latIndex, int lonIndex) {
    return latLonToProj(proj, from, new double[2][from[0].length], latIndex, lonIndex);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *        the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   */
  public static double[][] latLonToProj(Projection proj, double[][] from, double[][] to, int latIndex, int lonIndex) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.latLonToProj:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.latLonToProj:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("Projections.latLonToProj:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      LatLonPoint llpi = LatLonPoint.create(from[latIndex][i], from[lonIndex][i]);
      ProjectionPoint ppi = proj.latLonToProj(llpi);
      to[0][i] = ppi.getX();
      to[1][i] = ppi.getY();
    }
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the (lat,lon)
   *        coordinate of the ith point
   * @return resulting array of projection coordinates, where to[0][i],
   *         to[1][i] is the (x,y) coordinate of the ith point
   */
  public static float[][] latLonToProj(Projection proj, float[][] from) {
    return latLonToProj(proj, from, new float[2][from[0].length]);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (lat,lon) coordinate
   *        of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate
   *        of the ith point
   * @return the "to" array
   */
  public static float[][] latLonToProj(Projection proj, float[][] from, float[][] to) {
    return latLonToProj(proj, from, to, INDEX_LAT, INDEX_LON);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return resulting array of projection coordinates: to[2][n] where
   *         (to[0][i], to[1][i]) is the (x,y) coordinate of the ith point
   */
  public static float[][] latLonToProj(Projection proj, float[][] from, int latIndex, int lonIndex) {
    return latLonToProj(proj, from, new float[2][from[0].length], latIndex, lonIndex);
  }


  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *        the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   */
  public static float[][] latLonToProj(Projection proj, float[][] from, float[][] to, int latIndex, int lonIndex) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.latLonToProj:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "Projections.latLonToProj:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("Projections.latLonToProj:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      LatLonPoint llpi = LatLonPoint.create(from[latIndex][i], from[lonIndex][i]);
      ProjectionPoint ppi = proj.latLonToProj(llpi);
      to[0][i] = (float) ppi.getX();
      to[1][i] = (float) ppi.getY();
    }

    return to;
  }

}
