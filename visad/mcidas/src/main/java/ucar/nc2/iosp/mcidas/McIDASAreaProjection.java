/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.mcidas;


import ucar.mcidas.AREAnav;
import ucar.mcidas.AreaFile;
import ucar.mcidas.McIDASException;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.AbstractProjection;
import ucar.unidata.util.Parameter;

/**
 * McIDASAreaProjection is the ProjectionImpl for McIDAS Area navigation
 * modules.
 *
 * @author dmurray
 */
public class McIDASAreaProjection extends AbstractProjection {

  /**
   * Attribute for the Area Directory
   */
  public static String ATTR_AREADIR = "AreaDirectory";

  /**
   * Attribute for the Navigation Block
   */
  public static String ATTR_NAVBLOCK = "NavBlock";

  /**
   * Attribute for the Navigation Block
   */
  public static String ATTR_AUXBLOCK = "AuxBlock";

  /**
   * Attribute for the Navigation Block
   */
  public static String GRID_MAPPING_NAME = "mcidas_area";

  /**
   * Area navigation
   */
  private AREAnav anav;

  /**
   * number of lines
   */
  private int lines;

  /**
   * number of elements
   */
  private int elements;

  /**
   * directory block
   */
  private int[] dirBlock;

  /**
   * navigation block
   */
  private int[] navBlock;

  /**
   * aux block
   */
  private int[] auxBlock;

  /**
   * copy constructor - avoid clone !!
   *
   * @return construct a copy of this
   */
  public Projection constructCopy() {
    return new McIDASAreaProjection(dirBlock, navBlock, auxBlock);
  }

  /**
   * Default bean constructor
   */
  public McIDASAreaProjection() {
    super("McIDASAreaProjection", false);
  }

  /**
   * create a McIDAS AREA projection from the Area file's
   * directory and navigation blocks.
   * <p/>
   * This routine uses a flipped Y axis (first line of
   * the image file is number 0)
   *
   * @param af is the associated AreaFile
   */
  public McIDASAreaProjection(AreaFile af) {
    this(af.getDir(), af.getNav(), af.getAux());
  }

  /**
   * Create a AREA coordinate system from the Area file's
   * directory and navigation blocks.
   * <p/>
   * This routine uses a flipped Y axis (first line of
   * the image file is number 0)
   *
   * @param dir is the AREA file directory block
   * @param nav is the AREA file navigation block
   * @param aux is the AREA file auxillary block
   */
  public McIDASAreaProjection(int[] dir, int[] nav, int[] aux) {
    super("McIDASAreaProjection", false);

    try {
      anav = AREAnav.makeAreaNav(nav, aux);
    } catch (McIDASException excp) {
      throw new IllegalArgumentException("McIDASAreaProjection: problem creating projection" + excp);
    }
    dirBlock = dir;
    navBlock = nav;
    auxBlock = aux;
    anav.setImageStart(dir[5], dir[6]);
    anav.setRes(dir[11], dir[12]);
    anav.setStart(0, 0);
    anav.setMag(1, 1);
    lines = dir[8];
    elements = dir[9];
    anav.setFlipLineCoordinates(dir[8]); // invert Y axis coordinates

    addParameter(CF.GRID_MAPPING_NAME, GRID_MAPPING_NAME);
    addParameter(new Parameter(ATTR_AREADIR, makeDoubleArray(dir)));
    addParameter(new Parameter(ATTR_NAVBLOCK, makeDoubleArray(nav)));
    if (aux != null) {
      addParameter(new Parameter(ATTR_AUXBLOCK, makeDoubleArray(aux)));
    }
  }


  /**
   * Get the directory block used to initialize this McIDASAreaProjection
   *
   * @return the area directory
   */
  public int[] getDirBlock() {
    return dirBlock;
  }

  /**
   * Get the navigation block used to initialize this McIDASAreaProjection
   *
   * @return the navigation block
   */
  public int[] getNavBlock() {
    return navBlock;
  }

  /**
   * Get the auxilliary block used to initialize this McIDASAreaProjection
   *
   * @return the auxilliary block (may be null)
   */
  public int[] getAuxBlock() {
    return auxBlock;
  }

  /*
   * MACROBODY
   * latLonToProj {} {
   * double[][] xy = anav.toLinEle(new double[][] {{fromLat},{fromLon}});
   * toX = xy[0][0];
   * toY = xy[1][0];
   * }
   * 
   * projToLatLon {} {
   * double[][] latlon = anav.toLatLon(new double[][] {{fromX},{fromY}});
   * toLat = latlon[0][0];
   * toLon = latlon[1][0];
   * }
   * 
   * MACROBODY
   */
  /* BEGINGENERATED */

  /*
   * Note this section has been generated using the convert.tcl script.
   * This script, run as:
   * tcl convert.tcl McIDASAreaProjection.java
   * takes the actual projection conversion code defined in the MACROBODY
   * section above and generates the following 6 methods
   */


  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();


    double[][] xy = anav.toLinEle(new double[][] {{fromLat}, {fromLon}});
    if (xy == null)
      return null;
    toX = xy[0][0];
    toY = xy[1][0];

    return ProjectionPoint.create(toX, toY);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = world.getX();
    double fromY = world.getY();


    double[][] latlon = anav.toLatLon(new double[][] {{fromX}, {fromY}});
    toLat = latlon[0][0];
    toLon = latlon[1][0];

    return LatLonPoint.create(toLat, toLon);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the (lat,lon)
   *        coordinate of the ith point
   * @param to resulting array of projection coordinates,
   *        where to[0][i], to[1][i] is the (x,y) coordinate
   *        of the ith point
   * @param latIndex index of latitude in "from"
   * @param lonIndex index of longitude in "from"
   * @return the "to" array.
   */
  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex, int lonIndex) {
    float[] fromLatA = from[latIndex];
    float[] fromLonA = from[lonIndex];

    float[][] xy = anav.toLinEle(new float[][] {fromLatA, fromLonA});
    if (xy == null)
      return null;
    to[INDEX_X] = xy[0];
    to[INDEX_Y] = xy[1];
    return to;
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
  public float[][] projToLatLon(float[][] from, float[][] to) {
    float[] fromXA = from[INDEX_X];
    float[] fromYA = from[INDEX_Y];
    float[][] latlon = anav.toLatLon(new float[][] {fromXA, fromYA});
    to[INDEX_LAT] = latlon[0];
    to[INDEX_LON] = latlon[1];
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the (lat,lon)
   *        coordinate of the ith point
   * @param to resulting array of projection coordinates,
   *        where to[0][i], to[1][i] is the (x,y) coordinate
   *        of the ith point
   * @param latIndex index of latitude in "from"
   * @param lonIndex index of longitude in "from"
   * @return the "to" array.
   */
  public double[][] latLonToProj(double[][] from, double[][] to, int latIndex, int lonIndex) {
    double[] fromLatA = from[latIndex];
    double[] fromLonA = from[lonIndex];

    double[][] xy = anav.toLinEle(new double[][] {fromLatA, fromLonA});
    if (xy == null)
      return null;
    to[INDEX_X] = xy[0];
    to[INDEX_Y] = xy[1];
    return to;
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
  public double[][] projToLatLon(double[][] from, double[][] to) {
    double[] fromXA = from[INDEX_X];
    double[] fromYA = from[INDEX_Y];
    double[][] latlon = anav.toLatLon(new double[][] {fromXA, fromYA});
    to[INDEX_LAT] = latlon[0];
    to[INDEX_LON] = latlon[1];
    return to;
  }

  /* ENDGENERATED */

  /**
   * Get the bounds for this image
   *
   * @return the projection bounds
   */
  public ProjectionRect getDefaultMapArea() {
    return new ProjectionRect(new ProjectionRect(0, 0, elements, lines));
  }

  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   *
   * @param pt1 point 1
   * @param pt2 point 2
   * @return true when the line between pt1 and pt2 crosses the seam.
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2)) {
      return true;
    }
    if (Double.isNaN(pt1.getX()) || Double.isNaN(pt1.getY()) || Double.isNaN(pt2.getX()) || Double.isNaN(pt2.getY())) {
      return true;
    }
    // opposite signed X values, larger then 5000 km
    return (pt1.getX() * pt2.getX() < 0) && (Math.abs(pt1.getX() - pt2.getX()) > 5000.0);
  }

  /*
   * Determines whether or not the <code>Object</code> in question is
   * the same as this <code>McIDASAreaProjection</code>. The specified
   * <code>Object</code> is equal to this <CODE>McIDASAreaProjection</CODE>
   * if it is an instance of <CODE>McIDASAreaProjection</CODE> and it has
   * the same navigation module and default map area as this one.
   *
   * @param obj the Object in question
   * 
   * @return true if they are equal
   *
   * public boolean equals(Object obj) {
   * if (!(obj instanceof McIDASAreaProjection)) {
   * return false;
   * }
   * McIDASAreaProjection that = (McIDASAreaProjection) obj;
   * if ((defaultMapArea == null) != (that.defaultMapArea == null)) return false; // common case is that these are null
   * if (defaultMapArea != null && !that.defaultMapArea.equals(defaultMapArea)) return false;
   * 
   * return (this == that)
   * || (anav.equals(that.anav) && (this.lines == that.lines)
   * && (this.elements == that.elements));
   * }
   */

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    McIDASAreaProjection that = (McIDASAreaProjection) o;

    if (elements != that.elements)
      return false;
    if (lines != that.lines)
      return false;
    return anav.equals(that.anav);

  }

  @Override
  public int hashCode() {
    int result = anav.hashCode();
    result = 31 * result + lines;
    result = 31 * result + elements;
    return result;
  }

  /**
   * Return a String which tells some info about this navigation
   *
   * @return wordy String
   */
  public String toString() {
    return "Image (" + anav + ") Projection";
  }

  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return " nav " + anav;
  }

  /**
   * make a double array out of an int array
   *
   * @param ints array of ints
   * @return array of doubles
   */
  private double[] makeDoubleArray(int[] ints) {
    double[] newArray = new double[ints.length];
    for (int i = 0; i < ints.length; i++) {
      newArray[i] = ints[i];
    }
    return newArray;
  }
}

