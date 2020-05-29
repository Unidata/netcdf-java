/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.write.Ncdump;
import ucar.unidata.geoloc.*;
import java.io.IOException;
import java.util.*;

/**
 * fork ucar.nc2.dt.grid.GridCoordSys for adaption of GridCoverage.
 * Minimalist, does not do subsetting, vertical transform.
 *
 * @author caron
 * @since 5/26/2015
 */
public class DtCoverageCS {
  // static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DtCoverageCS.class);
  // static private final boolean warnUnits = false;

  /////////////////////////////////////////////////////////////////////////////
  protected DtCoverageCSBuilder builder;
  private String name;
  private ProjectionImpl proj;
  private GeoGridCoordinate2D g2d;
  private boolean isLatLon;

  /**
   * Create a GeoGridCoordSys from an existing Coordinate System.
   * This will choose which axes are the XHoriz, YHoriz, Vertical, Time, RunTIme, Ensemble.
   * If theres a Projection, it will set its map area
   *
   * @param builder create from this
   */
  public DtCoverageCS(DtCoverageCSBuilder builder) {
    this.builder = builder;

    // make name based on coordinate
    this.name = CoordinateSystem.makeName(builder.allAxes);

    // WRF NMM
    Attribute att = getXHorizAxis().findAttribute(_Coordinate.Stagger);
    if (att != null)
      setHorizStaggerType(att.getStringValue());

    if (builder.orgProj != null) {
      proj = builder.orgProj.constructCopy();
    }
  }

  public String getName() {
    return name;
  }

  public FeatureType getCoverageType() {
    return builder.type;
  }

  public List<CoordinateAxis> getCoordAxes() {
    return builder.allAxes;
  }

  public CoordinateAxis findCoordAxis(String shortName) {
    for (CoordinateAxis axis : builder.allAxes) {
      if (axis.getShortName().equals(shortName))
        return axis;
    }
    return null;
  }

  public List<CoordinateTransform> getCoordTransforms() {
    return builder.coordTransforms;
  }

  /**
   * get the X Horizontal axis (either GeoX or Lon)
   */
  public CoordinateAxis getXHorizAxis() {
    return builder.xaxis;
  }

  /**
   * get the Y Horizontal axis (either GeoY or Lat)
   */
  public CoordinateAxis getYHorizAxis() {
    return builder.yaxis;
  }

  /**
   * get the Vertical axis (either Geoz, Height, or Pressure)
   */
  public CoordinateAxis1D getVerticalAxis() {
    return builder.vertAxis;
  }


  public CoordinateAxis getTimeAxis() {
    return builder.timeAxis;
  }

  public CoordinateAxis1DTime getRunTimeAxis() {
    return builder.rtAxis;
  }

  public CoordinateAxis1D getEnsembleAxis() {
    return builder.ensAxis;
  }

  public ProjectionImpl getProjection() {
    return proj;
  }

  /**
   * is this a Lat/Lon coordinate system?
   */
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Is this a global coverage over longitude ?
   *
   * @return true if isLatLon and longitude wraps
   */
  public boolean isGlobalLon() {
    if (!isLatLon)
      return false;
    if (!(getXHorizAxis() instanceof CoordinateAxis1D))
      return false;
    CoordinateAxis1D lon = (CoordinateAxis1D) getXHorizAxis();
    double first = lon.getCoordEdge(0);
    double last = lon.getCoordEdge((int) lon.getSize());
    double min = Math.min(first, last);
    double max = Math.max(first, last);
    return (max - min) >= 360;
  }

  /**
   * true if x and y axes are CoordinateAxis1D and are regular
   */
  public boolean isRegularSpatial() {
    if (!isRegularSpatial(getXHorizAxis()))
      return false;
    return isRegularSpatial(getYHorizAxis());
  }

  private boolean isRegularSpatial(CoordinateAxis axis) {
    if (axis == null)
      return true;
    if (!(axis instanceof CoordinateAxis1D))
      return false;
    return ((CoordinateAxis1D) axis).isRegular();
  }

  private String horizStaggerType;

  public String getHorizStaggerType() {
    return horizStaggerType;
  }

  public void setHorizStaggerType(String horizStaggerType) {
    this.horizStaggerType = horizStaggerType;
  }

  private ProjectionRect mapArea;

  /**
   * Get the x,y bounding box in projection coordinates.
   */
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {

      CoordinateAxis horizXaxis = getXHorizAxis();
      CoordinateAxis horizYaxis = getYHorizAxis();
      if ((horizXaxis == null) || !horizXaxis.isNumeric() || (horizYaxis == null) || !horizYaxis.isNumeric())
        return null; // impossible

      // x,y may be 2D
      if ((horizXaxis instanceof CoordinateAxis2D) && (horizYaxis instanceof CoordinateAxis2D)) {
        // could try to optimize this - just get corners or something
        CoordinateAxis2D xaxis2 = (CoordinateAxis2D) horizXaxis;
        CoordinateAxis2D yaxis2 = (CoordinateAxis2D) horizYaxis;

        mapArea = null; // getBBfromCorners(xaxis2, yaxis2); LOOK LOOK

        // mapArea = new ProjectionRect(horizXaxis.getMinValue(), horizYaxis.getMinValue(),
        // horizXaxis.getMaxValue(), horizYaxis.getMaxValue());

      } else {

        CoordinateAxis1D xaxis1 = (CoordinateAxis1D) horizXaxis;
        CoordinateAxis1D yaxis1 = (CoordinateAxis1D) horizYaxis;

        /*
         * add one percent on each side if its a projection. WHY?
         * double dx = 0.0, dy = 0.0;
         * if (!isLatLon()) {
         * dx = .01 * (xaxis1.getCoordEdge((int) xaxis1.getSize()) - xaxis1.getCoordEdge(0));
         * dy = .01 * (yaxis1.getCoordEdge((int) yaxis1.getSize()) - yaxis1.getCoordEdge(0));
         * }
         * 
         * mapArea = new ProjectionRect(xaxis1.getCoordEdge(0) - dx, yaxis1.getCoordEdge(0) - dy,
         * xaxis1.getCoordEdge((int) xaxis1.getSize()) + dx,
         * yaxis1.getCoordEdge((int) yaxis1.getSize()) + dy);
         */

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0), yaxis1.getCoordEdge(0),
            xaxis1.getCoordEdge((int) xaxis1.getSize()), yaxis1.getCoordEdge((int) yaxis1.getSize()));
      }
    }

    return mapArea;
  }

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the x,y indices
   *
   * @param xindex x index
   * @param yindex y index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int xindex, int yindex) {
    double x, y;

    CoordinateAxis horizXaxis = getXHorizAxis();
    CoordinateAxis horizYaxis = getYHorizAxis();
    if (horizXaxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizXaxis;
      x = horiz1D.getCoordValue(xindex);
    } else {
      CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizXaxis;
      x = horiz2D.getCoordValue(yindex, xindex);
    }

    if (horizYaxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizYaxis;
      y = horiz1D.getCoordValue(yindex);
    } else {
      CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizYaxis;
      y = horiz2D.getCoordValue(yindex, xindex);
    }

    return isLatLon() ? LatLonPoint.create(y, x) : getLatLon(x, y);
  }

  public LatLonPoint getLatLon(double xcoord, double ycoord) {
    Projection dataProjection = getProjection();
    return dataProjection.projToLatLon(ProjectionPoint.create(xcoord, ycoord));
  }

  private LatLonRect llbb;

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   *
   * @return lat, lon bounding box.
   */
  public LatLonRect getLatLonBoundingBox() {

    if (llbb == null) {

      if ((getXHorizAxis() instanceof CoordinateAxis2D) && (getYHorizAxis() instanceof CoordinateAxis2D)) {
        return null;
      }

      CoordinateAxis horizXaxis = getXHorizAxis();
      CoordinateAxis horizYaxis = getYHorizAxis();
      if (isLatLon()) {
        double startLat = horizYaxis.getMinValue();
        double startLon = horizXaxis.getMinValue();

        double deltaLat = horizYaxis.getMaxValue() - startLat;
        double deltaLon = horizXaxis.getMaxValue() - startLon;

        LatLonPoint llpt = LatLonPoint.create(startLat, startLon);
        llbb = new LatLonRect(llpt, deltaLat, deltaLon);

      } else {
        ProjectionImpl dataProjection = getProjection();
        ProjectionRect bb = getBoundingBox();
        if (bb != null)
          llbb = dataProjection.projToLatLonBB(bb);
      }
    }

    return llbb;

    /*
     * // look at all 4 corners of the bounding box
     * LatLonPointImpl llpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerLeftPoint(), new
     * LatLonPointImpl());
     * LatLonPointImpl lrpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerRightPoint(), new
     * LatLonPointImpl());
     * LatLonPointImpl urpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperRightPoint(), new
     * LatLonPointImpl());
     * LatLonPointImpl ulpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperLeftPoint(), new
     * LatLonPointImpl());
     * 
     * // Check if grid contains poles.
     * boolean includesNorthPole = false;
     * int[] resultNP;
     * resultNP = findXYindexFromLatLon(90.0, 0, null);
     * if (resultNP[0] != -1 && resultNP[1] != -1)
     * includesNorthPole = true;
     * boolean includesSouthPole = false;
     * int[] resultSP;
     * resultSP = findXYindexFromLatLon(-90.0, 0, null);
     * if (resultSP[0] != -1 && resultSP[1] != -1)
     * includesSouthPole = true;
     * 
     * if (includesNorthPole && !includesSouthPole) {
     * llbb = new LatLonRect(llpt, LatLonPoint.create(90.0, 0.0)); // ??? lon=???
     * llbb.extend(lrpt);
     * llbb.extend(urpt);
     * llbb.extend(ulpt);
     * // OR
     * //llbb.extend( new LatLonRect( llpt, lrpt ));
     * //llbb.extend( new LatLonRect( lrpt, urpt ) );
     * //llbb.extend( new LatLonRect( urpt, ulpt ) );
     * //llbb.extend( new LatLonRect( ulpt, llpt ) );
     * } else if (includesSouthPole && !includesNorthPole) {
     * llbb = new LatLonRect(llpt, LatLonPoint.create(-90.0, -180.0)); // ??? lon=???
     * llbb.extend(lrpt);
     * llbb.extend(urpt);
     * llbb.extend(ulpt);
     * } else {
     * double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
     * double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());
     * 
     * // longitude is a bit tricky as usual
     * double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
     * double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);
     * 
     * llpt.set(latMin, lonMin);
     * urpt.set(latMax, lonMax);
     * 
     * llbb = new LatLonRect(llpt, urpt);
     * }
     * }
     * }
     */

  }

  @Override
  public String toString() {
    Formatter buff = new Formatter();
    show(buff, false);
    return buff.toString();
  }

  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    showCoordinateAxis(getRunTimeAxis(), f, showCoords);
    showCoordinateAxis(getEnsembleAxis(), f, showCoords);
    showCoordinateAxis(getTimeAxis(), f, showCoords);
    showCoordinateAxis(getVerticalAxis(), f, showCoords);
    showCoordinateAxis(getYHorizAxis(), f, showCoords);
    showCoordinateAxis(getXHorizAxis(), f, showCoords);

    if (proj != null)
      f.format(" Projection: %s %s%n", proj.getName(), proj.paramsToString());
  }

  private void showCoordinateAxis(CoordinateAxis axis, Formatter f, boolean showCoords) {
    if (axis == null)
      return;
    f.format(" rt=%s (%s)", axis.getNameAndDimensions(), axis.getClass().getName());
    if (showCoords)
      showCoords(axis, f);
    f.format("%n");
  }

  private void showCoords(CoordinateAxis axis, Formatter f) {
    try {
      if (axis instanceof CoordinateAxis1D && axis.isNumeric()) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        if (!axis1D.isInterval()) {
          double[] e = axis1D.getCoordEdges();
          for (double anE : e) {
            f.format("%f,", anE);
          }
        } else {
          double[] b1 = axis1D.getBound1();
          double[] b2 = axis1D.getBound2();
          for (int i = 0; i < b1.length; i++) {
            f.format("(%f,%f) = %f%n", b1[i], b2[i], b2[i] - b1[i]);
          }
        }
      } else {
        f.format("%s", Ncdump.printVariableData(axis, null));
      }
    } catch (IOException ioe) {
      f.format(ioe.getMessage());
    }
    f.format(" %s%n", axis.getUnitsString());
  }

  public CalendarDateRange getCalendarDateRange() {
    CoordinateAxis timeTaxis = getTimeAxis();
    if (timeTaxis instanceof CoordinateAxis1DTime)
      return ((CoordinateAxis1DTime) timeTaxis).getCalendarDateRange();

    CoordinateAxis1DTime rtaxis = getRunTimeAxis();
    if (rtaxis != null) {
      return rtaxis.getCalendarDateRange();
    }

    return null;
  }

  public int getDomainRank() {
    return CoordinateSystem.makeDomain(builder.independentAxes).size();
  }

  public int getRangeRank() {
    return builder.allAxes.size(); // not right
  }
}
