/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid3;

import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.MinMax;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridHorizCurvilinear;
import ucar.nc2.grid.GridReader;

import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.MaterializedCoordinateSystem;
import ucar.nc2.grid.Grids;
import ucar.nc2.internal.grid.CurvilinearCoords;
import ucar.nc2.ui.grid.ColorScale;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Format;

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
  private final boolean drawGrid = true;
  private boolean drawGridLines = true;
  private boolean drawContours;
  private boolean isNewField = true;

  private ColorScale colorScale;
  private ColorScale.MinMaxType dataMinMaxType = ColorScale.MinMaxType.horiz;
  private Projection dataProjection; // current data Projection

  // data stuff
  private DataState dataState;
  private GridReferencedArray geodata;

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

  /** set the Projection of the data */
  void setDataProjection(Projection dataProjection) {
    this.dataProjection = dataProjection;
  }

  /* set the Projection to use for drawing */
  public void setDrawBB(boolean drawBB) {}

  /* set whether grid should be drawn */
  public void setDrawGridLines(boolean drawGrid) {
    this.drawGridLines = drawGrid;
  }

  /* set whether countours should be drawn */
  public void setDrawContours(boolean drawContours) {
    this.drawContours = drawContours;
  }

  /* set whether contour labels should be drawn */
  public void setDrawContourLabels(boolean drawContourLabels) {}

  /**
   * Get the data value at this projection (x,y) point.
   *
   * @param loc : point in display projection coordinates (plan view)
   * @return String representation of value
   */
  public String getXYvalueStr(ProjectionPoint loc) {
    if ((dataState.grid == null) || (geodata == null)) {
      return "";
    }

    // find the grid indexes
    GridHorizCoordinateSystem hcs = dataState.gcs.getHorizCoordinateSystem();
    Optional<GridHorizCoordinateSystem.CoordReturn> opt = hcs.findXYindexFromCoord(loc.getX(), loc.getY());

    // get value, construct the string
    if (opt.isEmpty()) {
      return "outside data grid";
    } else {
      GridHorizCoordinateSystem.CoordReturn cr = opt.get();
      try {
        Array<Number> array = Arrays.reduce(geodata.data());
        double dataValue = array.get(cr.yindex, cr.xindex).doubleValue();
        return makeXYZvalueStr(dataValue, cr);
      } catch (Exception e) {
        e.printStackTrace();
        return e.getMessage() + " error on " + cr;
      }
    }
  }

  private String makeXYZvalueStr(double value, GridHorizCoordinateSystem.CoordReturn cr) {
    String val = dataState.grid.isMissing(value) ? "missing value" : Format.d(value, 6);
    Formatter sbuff = new Formatter();
    sbuff.format("%s %s", val, dataState.grid.getUnits());
    sbuff.format(" @ (%f,%f)", cr.xcoord, cr.ycoord);
    sbuff.format("  [%d,%d]", cr.xindex, cr.yindex);
    return sbuff.toString();
  }

  //////// data routines

  private GridReferencedArray readHSlice() throws IOException, InvalidRangeException {
    System.out.printf("readHSlice %s%n", dataState.grid.getName());
    // make sure we need new one
    if (!dataState.hasChanged()) {
      return geodata;
    }

    // get the data slice
    GridReader reader = dataState.grid.getReader();
    if (dataState.vertCoord != null) {
      reader.setVertCoord(dataState.vertCoord);
    }
    if (dataState.timeCoord != null) {
      reader.setTimeOffsetCoord(dataState.timeCoord);
    }
    if (dataState.runtimeCoord != null) {
      reader.setRunTime(dataState.runtimeCoord.runtime);
    }
    if (dataState.ensCoord != null) {
      reader.setEnsCoord(dataState.ensCoord);
    }
    if (dataState.horizStride != 1) {
      reader.setHorizStride(dataState.horizStride);
    }

    geodata = reader.read();
    dataState.saveState();
    System.out.printf("readHSlice done%n");
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
    if ((dataState.grid == null) || (colorScale == null)) {
      return;
    }

    if (!drawGrid && !drawContours) {
      return;
    }

    // no anitaliasing
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    GridReferencedArray dataArr = readHSlice();
    if (dataArr == null) {
      return;
    }

    setColorScaleParams();

    if (drawGrid) {
      GridHorizCoordinateSystem hcs = dataState.gcs.getHorizCoordinateSystem();
      if (!hcs.isCurvilinear()) {
        drawGridHoriz(g, dataArr);
      } else {
        // LOOK this should be non-regular case, not the 2D case.
        drawGridCurvilinear(g, dataArr);
      }
    }
    if (drawGridLines) {
      GridHorizCoordinateSystem hcs = dataArr.getMaterializedCoordinateSystem().getHorizCoordinateSystem();
      if (hcs.isCurvilinear()) {
        drawGridLinesCurvilinear(g, (GridHorizCurvilinear) hcs);
      } else {
        drawGridLines(g, hcs);
      }
    }
  }

  private boolean drawGridBB(Graphics2D g, LatLonRect latLonRect) {
    g.setColor(Color.BLACK);
    Rectangle2D rect = new Rectangle2D.Double(latLonRect.getLonMin(), latLonRect.getLatMin(), latLonRect.getWidth(),
        latLonRect.getHeight());
    g.draw(rect);
    return true;
  }

  // orthogonal axes (not curvilinear)
  private void drawGridHoriz(Graphics2D g, GridReferencedArray referencedArray) {
    MaterializedCoordinateSystem msys = referencedArray.getMaterializedCoordinateSystem();
    Array<Number> data = referencedArray.data();
    data = Arrays.reduce(data);

    GridAxisPoint xaxis = msys.getXHorizAxis();
    GridAxisPoint yaxis = msys.getYHorizAxis();
    if (data.getRank() != 2) {
      System.out.printf("drawGridHorizRegular Rank equals %d, must be 2%n", data.getRank());
      return;
    }

    int nx = xaxis.getNominalSize();
    int ny = yaxis.getNominalSize();

    //// drawing optimizations
    // find the most common color and fill the entire area with it
    colorScale.resetHist();
    for (Number number : data) {
      colorScale.getIndexFromValue(number.doubleValue()); // accum in histogram
    }
    int modeColor = colorScale.getHistMax();
    MinMax xminmax = Grids.getCoordEdgeMinMax(xaxis);
    MinMax yminmax = Grids.getCoordEdgeMinMax(yaxis);

    // pre color the drawing area with the most used color
    drawRect(g, modeColor, xminmax.min(), xminmax.max(), yminmax.min(), yminmax.max(), dataProjection.isLatLon());

    // draw individual rects with run length
    for (int y = 0; y < ny; y++) {
      CoordInterval yintv = yaxis.getCoordInterval(y);
      double ybeg = yintv.start();
      double yend = yintv.end();

      int thisColor, lastColor = 0;
      int run = 0;
      int xbeg = 0;

      for (int x = 0; x < nx; x++) {
        double val = data.get(y, x).doubleValue();
        thisColor = colorScale.getIndexFromValue(val);

        if ((run == 0) || (lastColor == thisColor)) { // same color - keep running
          run++;
        } else {
          if (lastColor != modeColor) { // dont have to draw these
            drawRect(g, lastColor, xaxis.getCoordInterval(xbeg).start(), xaxis.getCoordInterval(x).end(), ybeg, yend,
                dataProjection.isLatLon());
          }
          xbeg = x;
        }
        lastColor = thisColor;
      }

      // get the ones at the end
      if (lastColor != modeColor) {
        drawRect(g, lastColor, xaxis.getCoordInterval(xbeg).start(),
            xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end(), ybeg, yend, dataProjection.isLatLon());
      }
    }
  }

  private int drawRect(Graphics2D g, int color, double w1, double w2, double h1, double h2, boolean useLatlon) {
    if (useLatlon) {
      return drawRectLatLon(g, color, w1, h1, w2, h2);
    }

    g.setColor(colorScale.getColor(color));
    double wmin = Math.min(w1, w2);
    double hmin = Math.min(h1, h2);
    double width = Math.abs(w1 - w2);
    double height = Math.abs(h1 - h2);
    Rectangle2D rect = new Rectangle2D.Double(wmin, hmin, width, height);
    g.fill(rect);
    return 1;
  }


  //// draw using Rectangle when possible
  private int drawRectLatLon(Graphics2D g, int color, double lon1, double lat1, double lon2, double lat2) {
    g.setColor(colorScale.getColor(color));

    LatLonProjection projectll = (LatLonProjection) dataProjection;

    int count = 0;
    ProjectionRect[] rects = projectll.latLonToProjRect(lat1, lon1, lat2, lon2);
    for (int i = 0; i < 2; i++) {
      if (null != rects[i]) {
        ProjectionRect r2 = rects[i];
        Rectangle2D.Double r = new Rectangle2D.Double(r2.getMinX(), r2.getMinY(), r2.getWidth(), r2.getHeight());
        g.fill(r);
        count++;
      }
    }
    return count;
  }

  // 2D case
  private void drawGridCurvilinear(Graphics2D g, GridReferencedArray referencedArray) {
    GridHorizCoordinateSystem hcsys2D = referencedArray.getMaterializedCoordinateSystem().getHorizCoordinateSystem();
    Array<Number> data = referencedArray.data();
    data = Arrays.reduce(data);

    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);

    for (GridHorizCoordinateSystem.CellBounds edge : hcsys2D.cells()) {
      gp.reset();
      gp.moveTo((float) edge.ll.xcoord, (float) edge.ll.ycoord);
      gp.lineTo((float) edge.lr.xcoord, (float) edge.lr.ycoord);
      gp.lineTo((float) edge.ur.xcoord, (float) edge.ur.ycoord);
      gp.lineTo((float) edge.ul.xcoord, (float) edge.ul.ycoord);

      double val = data.get(edge.yindex, edge.xindex).doubleValue();
      int colorIndex = colorScale.getIndexFromValue(val);
      g.setColor(colorScale.getColor(colorIndex));
      g.fill(gp);
    }
  }

  private void drawGridLinesCurvilinear(java.awt.Graphics2D g, GridHorizCurvilinear hcs) {
    CurvilinearCoords cc = hcs.getCurvilinearCoords();
    Array<Double> latEdge = cc.getLatEdge();
    Array<Double> lonEdge = cc.getLonEdge();

    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 100);
    g.setColor(Color.BLACK);

    int[] shape = latEdge.getShape(); // should both be the same
    int nrows = shape[0];
    int ncols = shape[1];

    for (int y = 0; y < nrows; y += 10) {
      gp.reset();
      for (int x = 0; x < ncols; x++) {
        if (x == 0) {
          gp.moveTo(lonEdge.get(y, x).floatValue(), latEdge.get(y, x).floatValue());
        } else {
          gp.lineTo(lonEdge.get(y, x).floatValue(), latEdge.get(y, x).floatValue());
        }
      }
      g.draw(gp);
    }

    gp.reset();
    for (int x = 0; x < ncols; x++) {
      if (x == 0) {
        gp.moveTo(lonEdge.get(nrows - 1, x).floatValue(), latEdge.get(nrows - 1, x).floatValue());
      } else {
        gp.lineTo(lonEdge.get(nrows - 1, x).floatValue(), latEdge.get(nrows - 1, x).floatValue());
      }
    }
    g.draw(gp);

    for (int x = 0; x < ncols; x += 10) {
      gp.reset();
      for (int y = 0; y < nrows; y++) {
        if (y == 0) {
          gp.moveTo(lonEdge.get(y, x).floatValue(), latEdge.get(y, x).floatValue());
        } else {
          gp.lineTo(lonEdge.get(y, x).floatValue(), latEdge.get(y, x).floatValue());
        }
      }
      g.draw(gp);
    }

    gp.reset();
    for (int y = 0; y < nrows; y++) {
      if (y == 0) {
        gp.moveTo(lonEdge.get(y, ncols - 1).floatValue(), latEdge.get(y, ncols - 1).floatValue());
      } else {
        gp.lineTo(lonEdge.get(y, ncols - 1).floatValue(), latEdge.get(y, ncols - 1).floatValue());
      }
    }
    g.draw(gp);
  }

  private void drawGridLines(java.awt.Graphics2D g, GridHorizCoordinateSystem hcs) {
    GridAxisPoint xaxis = hcs.getXHorizAxis();
    GridAxisPoint yaxis = hcs.getYHorizAxis();

    GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 100);
    g.setColor(Color.BLACK);

    int ny = yaxis.getNominalSize();
    int nx = xaxis.getNominalSize();

    for (int y = 0; y < ny; y += 10) {
      gp.reset();
      CoordInterval yintv = yaxis.getCoordInterval(y);
      for (int x = 0; x < nx; x++) {
        CoordInterval xintv = xaxis.getCoordInterval(x);
        if (x == 0) {
          gp.moveTo(xintv.start(), yintv.start());
        } else {
          gp.lineTo(xintv.start(), yintv.start());
        }
        if (x == nx - 1) {
          gp.lineTo(xintv.end(), yintv.start());
        }
      }
      g.draw(gp);
    }

    gp.reset();
    CoordInterval yintvLast = yaxis.getCoordInterval(ny - 1);
    for (int x = 0; x < nx; x++) {
      CoordInterval xintv = xaxis.getCoordInterval(x);
      if (x == 0) {
        gp.moveTo(xintv.start(), yintvLast.end());
      } else {
        gp.lineTo(xintv.start(), yintvLast.end());
      }
      if (x == nx - 1) {
        gp.lineTo(xintv.end(), yintvLast.end());
      }
    }
    g.draw(gp);

    for (int x = 0; x < nx; x += 10) {
      gp.reset();
      CoordInterval xintv = xaxis.getCoordInterval(x);
      for (int y = 0; y < ny; y++) {
        CoordInterval yintv = yaxis.getCoordInterval(y);
        if (y == 0) {
          gp.moveTo(xintv.start(), yintv.start());
        } else {
          gp.lineTo(xintv.start(), yintv.start());
        }
        if (y == ny - 1) {
          gp.lineTo(xintv.start(), yintv.end());
        }
      }
      g.draw(gp);
    }

    gp.reset();
    CoordInterval xintvLast = xaxis.getCoordInterval(nx - 1);
    for (int y = 0; y < ny; y++) {
      CoordInterval yintv = yaxis.getCoordInterval(y);
      if (y == 0) {
        gp.moveTo(xintvLast.end(), yintv.start());
      } else {
        gp.lineTo(xintvLast.end(), yintv.start());
      }
      if (y == ny - 1) {
        gp.lineTo(xintvLast.end(), yintv.end());
      }
    }
    g.draw(gp);

  }

}

