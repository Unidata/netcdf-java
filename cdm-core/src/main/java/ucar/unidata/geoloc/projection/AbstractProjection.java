/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import javax.annotation.concurrent.Immutable;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.StringUtil2;

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
 * <li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map
 * projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 * </ul>
 */
@Immutable
public abstract class AbstractProjection implements Projection {
  protected static final double EARTH_RADIUS = Earth.WGS84_EARTH_RADIUS_KM;
  protected static final double TOLERANCE = 1.0e-6;
  protected static final double PI = Math.PI;
  protected static final double PI_OVER_2 = Math.PI / 2.0;
  protected static final double PI_OVER_4 = Math.PI / 4.0;

  ///////////////////////////////////////////////////////////////////////

  protected final String name;
  protected final boolean isLatLon;
  protected final AttributeContainerMutable attc;

  /**
   * Copy constructor - avoid clone !!
   * 
   * @deprecated not needed because Immutable
   */
  @Deprecated
  public abstract Projection constructCopy();

  protected AbstractProjection(String name, boolean isLatLon) {
    this.name = name;
    this.isLatLon = isLatLon;
    this.attc = new AttributeContainerMutable(name);
  }

  @Override
  public String getClassName() {
    return StringUtil2.classShortName(getClass());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public AttributeContainer getProjectionAttributes() {
    return attc.toImmutable();
  }

  /**
   * Add an parameter to this projection
   *
   * @param name name of the attribute
   * @param value parameter value as a string
   */
  protected void addParameter(String name, String value) {
    attc.addAttribute(new Attribute(name, value));
  }

  /**
   * Add an parameter to this projection
   *
   * @param name name of the parameter
   * @param value parameter value as a double
   */
  protected void addParameter(String name, double value) {
    attc.addAttribute(new Attribute(name, value));
  }

  /** Add a parameter to this projection */
  protected void addParameter(Attribute att) {
    attc.addAttribute(att);
  }

  /**
   * Is this the lat/lon Projection ?
   *
   * @return true if it is the lat/lon Projection
   */
  public boolean isLatLon() {
    return isLatLon;
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
    ProjectionRect.Builder world = ProjectionRect.builder(w1.getX(), w1.getY(), w2.getX(), w2.getY());

    LatLonPoint la = LatLonPoint.create(ur.getLatitude(), ll.getLongitude());
    LatLonPoint lb = LatLonPoint.create(ll.getLatitude(), ur.getLongitude());

    // now extend if needed to the other two corners
    world.add(latLonToProj(la));
    world.add(latLonToProj(lb));

    return world.build();
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
      LatLonRect.Builder builder = LatLonRect.builder(llpt, LatLonPoint.create(90.0, 0.0)); // ??? lon=???
      builder.extend(lrpt);
      builder.extend(urpt);
      builder.extend(ulpt);
      llbb = builder.build();

    } else if (includesSouthPole && !includesNorthPole) {
      LatLonRect.Builder builder = LatLonRect.builder(llpt, LatLonPoint.create(-90.0, -180.0)); // ??? lon=???
      builder.extend(lrpt);
      builder.extend(urpt);
      builder.extend(ulpt);
      llbb = builder.build();

    } else {
      double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());

      // longitude is a bit tricky as usual
      double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);

      LatLonPoint ll = LatLonPoint.create(latMin, lonMin);
      LatLonPoint ur = LatLonPoint.create(latMax, lonMax);

      llbb = LatLonRect.builder(ll, ur).build();
    }

    return llbb;
  }

  protected static double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
    double midpoint = (lon1 + lon2) / 2;
    lon1 = LatLonPoints.lonNormal(lon1, midpoint);
    lon2 = LatLonPoints.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
  }

}

