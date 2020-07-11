/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import com.google.common.collect.ImmutableList;
import javax.annotation.concurrent.Immutable;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.*;
import java.util.*;

/**
 * Superclass for our implementations of geoloc.Projection.
 * <p/>
 * <p>
 * All subclasses must:
 * <ul>
 * <li>override equals() and return true when all parameters are equal
 * <li>create "atts" list of parameters as string-valued Attribute pairs
 * <li>implement abstract methods
 * </ul>
 * <p/>
 * If possible, set defaultmapArea to some reasonable world coord bounding box
 * otherwise, provide a way for the user to specify it when a specific projection
 * is created.
 * <p/>
 * <p>
 * Note on "false_easting" and "fale_northing" projection parameters:
 * <ul>
 * <li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 * </ul>
 */
@Immutable
public abstract class AbstractProjection implements Projection {
  protected static final double EARTH_RADIUS = Earth.WGS84_EARTH_RADIUS_KM;
  protected static final int INDEX_LAT = 0;
  protected static final int INDEX_LON = 1;
  protected static final int INDEX_X = 0;
  protected static final int INDEX_Y = 1;
  protected static final double TOLERANCE = 1.0e-6;
  protected static final double PI = Math.PI;
  protected static final double PI_OVER_2 = Math.PI / 2.0;
  protected static final double PI_OVER_4 = Math.PI / 4.0;

  ///////////////////////////////////////////////////////////////////////

  protected final String name;
  protected final boolean isLatLon;
  protected final List<Parameter> atts = new ArrayList<>();

  /**
   * copy constructor - avoid clone !!
   *
   * @return a copy of this Projection.
   */
  public abstract Projection constructCopy();

  protected AbstractProjection(String name, boolean isLatLon) {
    this.name = name;
    this.isLatLon = isLatLon;
  }

  @Override
  public String getClassName() {
    String className = getClass().getName();
    int index = className.lastIndexOf(".");
    if (index >= 0) {
      className = className.substring(index + 1);
    }
    return className;
  }

  @Override
  public abstract String paramsToString();

  /**
   * Get the label to be used in the gui for this type of projection.
   * This defaults to call getClassName
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return getClassName();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ImmutableList<Parameter> getProjectionParameters() {
    return ImmutableList.copyOf(atts);
  }

  /** @deprecated do not use */
  @Deprecated
  public Parameter findProjectionParameter(String want) {
    for (Parameter p : atts) {
      if (p.getName().equals(want))
        return p;
    }
    return null;
  }

  /**
   * Add an attribute to this projection
   *
   * @param name name of the attribute
   * @param value attribute value as a string
   */
  protected void addParameter(String name, String value) {
    atts.add(new Parameter(name, value));
  }

  /**
   * Add an attribute to this projection
   *
   * @param name name of the attribute
   * @param value attribute value as a double
   */
  protected void addParameter(String name, double value) {
    atts.add(new Parameter(name, value));
  }

  /**
   * Add an attribute to this projection
   *
   * @param p specify as a Parameter
   */
  protected void addParameter(Parameter p) {
    atts.add(p);
  }

  /**
   * Is this the lat/lon Projection ?
   *
   * @return true if it is the lat/lon Projection
   */
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Get a header for display.
   *
   * @return human readable header for display
   */
  public static String getHeader() {
    StringBuilder headerB = new StringBuilder(60);
    headerB.append("Name");
    Format.tab(headerB, 20, true);
    headerB.append("Class");
    Format.tab(headerB, 40, true);
    headerB.append("Parameters");
    return headerB.toString();
  }

  /**
   * Get a String representation of this projection.
   *
   * @return the name of the projection. This is what gets
   *         displayed when you add the projection object to
   *         a UI widget (e.g. label, combobox)
   */
  public String toString() {
    return getName();
  }

  //////////////////////////////////////////////////////////////////////
  // Allow subclasses to override.

  @Override
  public ProjectionRect latLonToProjBB(LatLonRect latlonRect) {
    LatLonPoint ll = latlonRect.getLowerLeftPoint();
    LatLonPoint ur = latlonRect.getUpperRightPoint();
    ProjectionPoint w1 = latLonToProj(ll);
    ProjectionPoint w2 = latLonToProj(ur);

    // make bounding box out of those two corners
    ProjectionRect world = new ProjectionRect(w1.getX(), w1.getY(), w2.getX(), w2.getY());

    LatLonPoint la = LatLonPoint.create(ur.getLatitude(), ll.getLongitude());
    LatLonPoint lb = LatLonPoint.create(ll.getLatitude(), ur.getLongitude());

    // now extend if needed to the other two corners
    world.add(latLonToProj(la));
    world.add(latLonToProj(lb));

    return world;
  }

  // Allow subclasses to override.
  @Override
  public LatLonRect projToLatLonBB(ProjectionRect bb) {
    // look at all 4 corners of the bounding box
    LatLonPoint llpt = projToLatLon(bb.getLowerLeftPoint());
    LatLonPoint lrpt = projToLatLon(bb.getLowerRightPoint());
    LatLonPoint urpt = projToLatLon(bb.getUpperRightPoint());
    LatLonPoint ulpt = projToLatLon(bb.getUpperLeftPoint());

    // Check if grid contains poles.
    boolean includesNorthPole = false;
    /*
     * int[] resultNP;
     * findXYindexFromLatLon(90.0, 0, resultNP);
     * if (resultNP[0] != -1 && resultNP[1] != -1)
     * includesNorthPole = true;
     */
    boolean includesSouthPole = false;
    /*
     * int[] resultSP = new int[2];
     * findXYindexFromLatLon(-90.0, 0, resultSP);
     * if (resultSP[0] != -1 && resultSP[1] != -1)
     * includesSouthPole = true;
     */

    LatLonRect llbb;

    if (includesNorthPole && !includesSouthPole) {
      llbb = new LatLonRect(llpt, LatLonPoint.create(90.0, 0.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);
      // OR
      // llbb.extend( new LatLonRect( llpt, lrpt ));
      // llbb.extend( new LatLonRect( lrpt, urpt ) );
      // llbb.extend( new LatLonRect( urpt, ulpt ) );
      // llbb.extend( new LatLonRect( ulpt, llpt ) );
    } else if (includesSouthPole && !includesNorthPole) {
      llbb = new LatLonRect(llpt, LatLonPoint.create(-90.0, -180.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);

    } else {
      double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());

      // longitude is a bit tricky as usual
      double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);

      LatLonPoint min = LatLonPoint.create(latMin, lonMin);
      LatLonPoint max = LatLonPoint.create(latMax, lonMax);
      llbb = new LatLonRect(min, max);
    }

    return llbb;
  }

  /**
   * Alternate way to calculate latLonToProjBB, originally in GridCoordSys.
   * Difficult to do this in a general way.
   * TODO evaluate if this is better than latLonToProjBB
   *
   * @param latlonRect desired lat/lon rectangle
   * @return a ProjectionRect
   */
  ProjectionRect latLonToProjBB2(LatLonRect latlonRect) {
    double minx, maxx, miny, maxy;

    LatLonPoint llpt = latlonRect.getLowerLeftPoint();
    LatLonPoint urpt = latlonRect.getUpperRightPoint();
    LatLonPoint lrpt = latlonRect.getLowerRightPoint();
    LatLonPoint ulpt = latlonRect.getUpperLeftPoint();

    if (isLatLon()) {
      minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
      maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

    } else {
      ProjectionPoint ll = latLonToProj(llpt);
      ProjectionPoint ur = latLonToProj(urpt);
      ProjectionPoint lr = latLonToProj(lrpt);
      ProjectionPoint ul = latLonToProj(ulpt);

      minx = Math.min(ll.getX(), ul.getX());
      miny = Math.min(ll.getY(), lr.getY());
      maxx = Math.max(ur.getX(), lr.getX());
      maxy = Math.max(ul.getY(), ur.getY());
    }

    return new ProjectionRect(minx, miny, maxx, maxy);
  }

  protected static double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
    double midpoint = (lon1 + lon2) / 2;
    lon1 = LatLonPoints.lonNormal(lon1, midpoint);
    lon2 = LatLonPoints.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
  }

}

