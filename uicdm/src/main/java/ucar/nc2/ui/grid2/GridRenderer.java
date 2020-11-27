/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid2;

import ucar.array.Array;
import ucar.array.Arrays;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.grid.*;
import ucar.nc2.internal.grid.GridLatLon2D;
import ucar.nc2.ui.grid.ColorScale;
import ucar.nc2.util.MinMax;
import ucar.ui.prefs.Debug;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Format;
import ucar.util.prefs.PreferencesExt;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/**
 * Display nc2.grid objects.
 * more or less the view in MVC
 */
public class GridRenderer {
  // draw state
  private boolean drawGrid = true;
  private boolean drawGridLines = true;
  private boolean drawContours;
  private boolean drawContourLabels;
  private boolean drawBB;
  private boolean isNewField = true;

  private ColorScale colorScale;
  private ColorScale.MinMaxType dataMinMaxType = ColorScale.MinMaxType.horiz;
  private Projection drawProjection; // current drawing Projection
  private Projection dataProjection; // current data Projection

  // data stuff
  private DataState dataState;
  private GridReferencedArray geodata;

  // drawing optimization
  private boolean useModeForProjections; // use colorMode optimization for different projections
  private boolean sameProjection = true;
  private LatLonProjection projectll; // special handling for LatLonProjection

  private static final boolean debugHorizDraw = false, debugMiss = false;
  private boolean debugPts, debugPathShape = true;

  /**
   * constructor
   */
  public GridRenderer(PreferencesExt store) {
    // rects[0] = new ProjectionRect();
  }

  ///// bean properties

  /* get the current ColorScale */
  public ColorScale getColorScale() {
    return colorScale;
  }

  /* set the ColorScale to use */
  public void setColorScale(ColorScale cs) {
    this.colorScale = cs;
  }

  /* set the ColorScale data min/max type */
  public void setDataMinMaxType(ColorScale.MinMaxType type) {
    if (type != dataMinMaxType) {
      dataMinMaxType = type;
    }
  }

  /* set the Grid */
  public DataState setGrid(GridDataset gridDataset, Grid grid) {
    this.dataState = new DataState(gridDataset, grid);
    isNewField = true;
    return this.dataState;
  }

  /* get the current data projection */
  public Projection getDataProjection() {
    return dataProjection;
  }

  public void setDataProjection(Projection dataProjection) {
    this.dataProjection = dataProjection;
  }

  /* get the current display projection */
  public Projection getDisplayProjection() {
    return drawProjection;
  }

  /* set the Projection to use for drawing */
  public void setViewProjection(Projection project) {
    drawProjection = project;
  }

  /* set the Projection to use for drawing */
  public void setDrawBB(boolean drawBB) {
    this.drawBB = drawBB;
  }

  /* set whether grid should be drawn */
  public void setDrawGridLines(boolean drawGrid) {
    this.drawGridLines = drawGrid;
  }

  /* set whether countours should be drawn */
  public void setDrawContours(boolean drawContours) {
    this.drawContours = drawContours;
  }

  /* set whether contour labels should be drawn */
  public void setDrawContourLabels(boolean drawContourLabels) {
    this.drawContourLabels = drawContourLabels;
  }

  /**
   * Get the data value at this projection (x,y) point.
   * 
   * @param loc : point in display projection coordinates (plan view)
   * @return String representation of value
   */
  public String getXYvalueStr(ProjectionPoint loc) {
    if ((dataState.grid == null) || (geodata == null))
      return "";

    // convert to dataProjection, where x and y are orthogonal
    if (!sameProjection) {
      LatLonPoint llpt = drawProjection.projToLatLon(loc);
      loc = dataProjection.latLonToProj(llpt);
    }

    // find the grid indexes
    GridHorizCoordinateSystem hcs = dataState.geocs.getHorizCoordSystem();
    Optional<GridHorizCoordinateSystem.CoordReturn> opt = hcs.findXYindexFromCoord(loc.getX(), loc.getY());

    // get value, construct the string
    if (!opt.isPresent())
      return "hcs.findXYindexFromCoord failed";
    else {
      GridHorizCoordinateSystem.CoordReturn cr = opt.get();
      try {
        Array<?> array = geodata.data();
        double dataValue = ((Number) array.get(cr.yindex, cr.xindex)).doubleValue();
        return makeXYZvalueStr(dataValue, cr);
      } catch (Exception e) {
        return "error on " + cr.toString();
      }
    }
  }

  private String makeXYZvalueStr(double value, GridHorizCoordinateSystem.CoordReturn cr) {
    String val = dataState.grid.isMissing(value) ? "missing value" : Format.d(value, 6);
    Formatter sbuff = new Formatter();
    sbuff.format("%s %s", val, dataState.grid.getUnitsString());
    sbuff.format(" @ (%f,%f)", cr.xcoord, cr.ycoord);
    sbuff.format("  [%d,%d]", cr.xindex, cr.yindex);
    return sbuff.toString();
  }

  //////// data routines

  private GridReferencedArray readHSlice()
      throws IOException, InvalidRangeException {

    // make sure we need new one
    if (!dataState.hasChanged()) {
      return geodata;
    }

    // get the data slice
    GridSubset subset = new GridSubset();
    if (dataState.vertCoord != null) {
      subset.setVertCoord(dataState.vertCoord);
    }
    if (dataState.timeCoord != null) {
      if (dataState.taxis != null) {
        subset.setTimeCoord(dataState.timeCoord);
      } else {
        subset.setTimeOffsetCoord(dataState.timeCoord);
      }
    }
    if (dataState.runtimeCoord != null) {
      subset.setRunTimeCoord(dataState.runtimeCoord);
    }
    if (dataState.ensCoord != null) {
      subset.setEnsCoord(dataState.ensCoord);
    }
    if (dataState.horizStride != 1) {
      subset.setHorizStride(dataState.horizStride);
    }

    geodata = dataState.grid.readData(subset);
    dataState.saveState();
    return geodata;
  }

  //////////// Renderer stuff

  // set colorscale limits, missing data
  private void setColorScaleParams() throws IOException, InvalidRangeException {
    if (dataMinMaxType == ColorScale.MinMaxType.hold && !isNewField)
      return;
    isNewField = false;

    GridReferencedArray dataArr = readHSlice();
    if (dataArr != null) {
      MinMax minmax = Arrays.getMinMaxSkipMissingData(dataArr.data(), dataState.grid);
      colorScale.setMinMax(minmax.min(), minmax.max());
      colorScale.setGeoGrid(dataState.grid);
    }
  }

  /**
   * Do the rendering to the given Graphics2D object.
   *
   * @param g Graphics2D object: has clipRect and AffineTransform set.
   * @param dFromN transforms "Normalized Device" to Device coordinates
   */
  public void renderPlanView(Graphics2D g, AffineTransform dFromN) throws IOException, InvalidRangeException {
    if ((dataState.grid == null) || (colorScale == null) || (drawProjection == null))
      return;

    if (!drawGrid && !drawContours)
      return;

    // no anitaliasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    GridReferencedArray dataArr = readHSlice();
    if (dataArr == null) {
      return;
    }

    setColorScaleParams();

    if (drawGrid) {
      GridHorizCoordinateSystem hcs = dataState.geocs.getHorizCoordSystem();
      if (hcs.isRegular()) {
        drawGridHorizRegular(g, dataArr);
      } else {
        drawGridHoriz(g, dataArr);
      }
    }

    // if (drawContours)
    // drawContours(g, dataH.transpose(0, 1), dFromN);
    // if (drawGridLines)
    // drawGridLines(g, dataH);
    // if (drawBB)
    // drawGridBB(g, this.dataState.GridDataset.getLatlonBoundingBox());
  }

  private boolean drawGridBB(Graphics2D g, LatLonRect latLonRect) {
    g.setColor(Color.BLACK);
    Rectangle2D rect = new Rectangle2D.Double(latLonRect.getLonMin(), latLonRect.getLatMin(), latLonRect.getWidth(),
        latLonRect.getHeight());
    g.draw(rect);
    return true;
  }

  private void drawGridHorizRegular(Graphics2D g, GridReferencedArray referencedArray) {
    GridCoordinateSystem gsys = referencedArray.csSubset();
    Array<Number> data = referencedArray.data();
    data = Arrays.reduce(data);

    int count = 0;
    GridAxis1D xaxis = (GridAxis1D) gsys.getXHorizAxis();
    GridAxis1D yaxis = (GridAxis1D) gsys.getYHorizAxis();
    if (data.getRank() != 2) {
      System.out.printf("drawGridHorizRegular Rank equals %d, must be 2%n", data.getRank());
      return;
    }

    int nx = xaxis.getNcoords();
    int ny = yaxis.getNcoords();

    //// drawing optimizations
    sameProjection = drawProjection.equals(dataProjection);
    if (drawProjection.isLatLon()) {
      projectll = (LatLonProjection) drawProjection;
      double centerLon = projectll.getCenterLon();
      if (Debug.isSet("projection/LatLonShift"))
        System.out.println("projection/LatLonShift: gridDraw = " + centerLon);
    }

    // find the most common color and fill the entire area with it
    colorScale.resetHist();
    for (Number number : data) {
      colorScale.getIndexFromValue(number.doubleValue()); // accum in histogram
    }
    int modeColor = colorScale.getHistMax();
    if (debugMiss) {
      System.out.println("mode = " + modeColor + " sameProj= " + sameProjection);
    }
    MinMax xminmax = xaxis.getCoordEdgeMinMax();
    MinMax yminmax = yaxis.getCoordEdgeMinMax();

    if (sameProjection) {
      // pre color the drawing area with the most used color
      count +=
          drawRect(g, modeColor, xminmax.min(), yminmax.min(), xminmax.max(), yminmax.max(), drawProjection.isLatLon());

    } else if (useModeForProjections) {
      drawPathShape(g, modeColor, xaxis, yaxis);
    }

    debugPts = Debug.isSet("GridRenderer/showPts");

    // draw individual rects with run length
    for (int y = 0; y < ny; y++) {
      double ybeg = yaxis.getCoordEdge1(y);
      double yend = yaxis.getCoordEdge2(y);

      int thisColor, lastColor = 0;
      int run = 0;
      int xbeg = 0;

      for (int x = 0; x < nx; x++) {
        double val = data.get(y, x).doubleValue();
        thisColor = colorScale.getIndexFromValue(val);

        if ((run == 0) || (lastColor == thisColor)) { // same color - keep running
          run++;
        } else {
          if (sameProjection) {
            if (lastColor != modeColor) // dont have to draw these
              count += drawRect(g, lastColor, xaxis.getCoordEdge1(xbeg), ybeg, xaxis.getCoordEdge2(x), yend,
                  drawProjection.isLatLon());
          } else {
            if (!useModeForProjections || (lastColor != modeColor)) // dont have to draw mode
              count += drawPathRun(g, lastColor, ybeg, yend, xaxis, xbeg, x - 1, debugPts);
          }
          xbeg = x;
        }
        lastColor = thisColor;
      }

      // get the ones at the end
      if (sameProjection) {
        if (lastColor != modeColor)
          count += drawRect(g, lastColor, xaxis.getCoordEdge1(xbeg), ybeg, xaxis.getCoordEdge2(xaxis.getNcoords() - 1),
              yend, drawProjection.isLatLon());
      } else {
        if (!useModeForProjections || (lastColor != modeColor))
          count += drawPathRun(g, lastColor, ybeg, yend, xaxis, xbeg, nx - 1, false); // needed ?
      }
    }
    if (debugHorizDraw)
      System.out.println("debugHorizDraw = " + count);
  }

  //// draw using Rectangle when possible

  private int drawRectLatLon(Graphics2D g, int color, double lon1, double lat1, double lon2, double lat2) {
    g.setColor(colorScale.getColor(color));

    int count = 0;
    ProjectionRect[] rects = projectll.latLonToProjRect(lat1, lon1, lat2, lon2);
    for (int i = 0; i < 2; i++)
      if (null != rects[i]) {
        ProjectionRect r2 = rects[i];
        Rectangle2D.Double r = new Rectangle2D.Double(r2.getX(), r2.getY(), r2.getWidth(), r2.getHeight());
        g.fill(r);
        count++;
      }
    return count;
  }

  private int drawRect(Graphics2D g, int color, double w1, double h1, double w2, double h2, boolean useLatlon) {
    if (useLatlon)
      return drawRectLatLon(g, color, w1, h1, w2, h2);

    g.setColor(colorScale.getColor(color));
    double wmin = Math.min(w1, w2);
    double hmin = Math.min(h1, h2);
    double width = Math.abs(w1 - w2);
    double height = Math.abs(h1 - h2);
    Rectangle2D rect = new Rectangle2D.Double(wmin, hmin, width, height);
    g.fill(rect);
    return 1;
  }

  private int drawPathShape(Graphics2D g, int color, GridAxis1D xaxis, GridAxis1D yaxis) {
    int count = 0;
    for (int y = 0; y < yaxis.getNcoords() - 1; y++) {
      double y1 = yaxis.getCoordEdge1(y);
      double y2 = yaxis.getCoordEdge2(y);
      count += drawPathRun(g, color, y1, y2, xaxis, 0, xaxis.getNcoords() - 1, false);
    }

    return count;
  }

  private GeneralPath gpRun = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 25);

  private int drawPathRun(Graphics2D g, int color, double y1, double y2, GridAxis1D xaxis, int x1, int x2,
      boolean debugPts) {
    int nx = xaxis.getNcoords();
    if ((x1 < 0) || (x2 < 0) || (x2 > nx) || (x1 > x2)) // from the recursion
      return 0;

    int count = 0;
    gpRun.reset();

    // first point
    LatLonPoint llp = dataProjection.projToLatLon(xaxis.getCoordEdge1(x1), y1);
    ProjectionPoint pt = drawProjection.latLonToProj(llp);
    if (debugPts)
      System.out.printf("** moveTo = x1=%d (%f, %f)%n", x1, pt.getX(), pt.getY());
    gpRun.moveTo((float) pt.getX(), (float) pt.getY());

    for (int e = x1; e <= x2; e++) {
      llp = dataProjection.projToLatLon(xaxis.getCoordEdge2(e), y1);
      pt = drawProjection.latLonToProj(llp);
      if (debugPts)
        System.out.printf("%d x2=%d lineTo = (%f, %f)%n", count++, e, pt.getX(), pt.getY());
      gpRun.lineTo((float) pt.getX(), (float) pt.getY());
    }

    for (int e = x2; e >= x1; e--) {
      llp = dataProjection.projToLatLon(xaxis.getCoordEdge2(e), y2);
      pt = drawProjection.latLonToProj(llp);
      if (debugPts)
        System.out.printf("%d x2=%d lineTo = (%f, %f)%n", count++, e, pt.getX(), pt.getY());
      gpRun.lineTo((float) pt.getX(), (float) pt.getY());
    }

    // finish
    llp = dataProjection.projToLatLon(xaxis.getCoordEdge1(x1), y2);
    pt = drawProjection.latLonToProj(llp);
    if (debugPts)
      System.out.printf("%d (%d,y2) lineTo = [%f, %f]%n", count, x1, pt.getX(), pt.getY());
    gpRun.lineTo((float) pt.getX(), (float) pt.getY());

    g.setColor(colorScale.getColor(color));
    try {
      g.fill(gpRun);
    } catch (Throwable e) {
      System.out.println("Exception in drawPathRun = " + e);
      return 0;
    }
    return 1;
  }

  //// 2D case
  private void drawGridHoriz(java.awt.Graphics2D g, GridReferencedArray referencedArray) {
    GridLatLon2D hcsys2D = (GridLatLon2D) referencedArray.csSubset().getHorizCoordSystem();
    Array<Number> data = referencedArray.data();
    data = Arrays.reduce(data);

    GridAxis2D lat2D = hcsys2D.getLatAxis();
    GridAxis2D lon2D = hcsys2D.getLonAxis();

    /*
     * not implemented
     * String stag = hcsys2D.getHorizStaggerType();
     * if (CDM.ARAKAWA_E.equals(stag)) {
     * drawGridHorizRotated(g, data, lon2D, lat2D);
     * return;
     * }
     */

    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);
    int[] shape = hcsys2D.getShape(); // should both be the same
    int ny = shape[0];
    int nx = shape[1];

    for (int y = 0; y < ny; y++) {
      for (int x = 0; x < nx; x++) {
        gp.reset();
        gp.moveTo((float) lon2D.getCoordValue(y, x), (float) lat2D.getCoordValue(y, x));
        gp.lineTo((float) lon2D.getCoordValue(y, x + 1), (float) lat2D.getCoordValue(y, x + 1));
        gp.lineTo((float) lon2D.getCoordValue(y + 1, x + 1), (float) lat2D.getCoordValue(y + 1, x + 1));
        gp.lineTo((float) lon2D.getCoordValue(y + 1, x), (float) lat2D.getCoordValue(y + 1, x));

        double val = data.get(y, x).doubleValue();
        int colorIndex = colorScale.getIndexFromValue(val);
        g.setColor(colorScale.getColor(colorIndex));
        g.fill(gp);
      }
    }
  }

}

/*
 * private void drawGridLines(Graphics2D g, GridCoordSystem geocs) {
 * LatLonAxis2D lataxis = geocs.getHorizCoordSys().getLatAxis2D();
 * LatLonAxis2D lonaxis = geocs.getHorizCoordSys().getLonAxis2D();
 *
 * if (lataxis == null || lonaxis == null)
 * return;
 *
 * Array<Double> edgex = (Array<Double>) lonaxis.getCoordBoundsAsArray();
 * Array<Double> edgey = (Array<Double>) lataxis.getCoordBoundsAsArray();
 *
 * GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);
 * g.setColor(Color.BLACK);
 *
 * int[] shape = lataxis.getShape(); // should both be the same
 * int ny = shape[0];
 * int nx = shape[1];
 *
 * for (int y = 0; y < ny + 1; y += 10) {
 * gp.reset();
 * for (int x = 0; x < nx + 1; x++) {
 * if (x == 0) {
 * gp.moveTo(edgex.get(y, x), edgey.get(y, x));
 * } else {
 * gp.lineTo(edgex.get(y, x), edgey.get(y, x));
 * }
 * }
 * g.draw(gp);
 * }
 *
 * for (int x = 0; x < nx + 1; x += 10) {
 * gp.reset();
 * for (int y = 0; y < ny + 1; y++) {
 * if (y == 0) {
 * gp.moveTo(edgex.get(y, x), edgey.get(y, x));
 * } else {
 * gp.lineTo(edgex.get(y, x), edgey.get(y, x));
 * }
 * }
 * g.draw(gp);
 * }
 *
 * }
 */

