/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import ucar.unidata.geoloc.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Consider this a private inner class of NavigatedPanel.
 * Handle display to world coordinate transformation, always linear.
 * Call np.newMapAreaEvent() when MapArea changes.
 * setMapArea() trigger a NewMapAreaEvent also.
 *
 * @author John Caron
 **/

public class Navigation {
  private static final boolean debug = false;
  private static final boolean debugZoom = false;
  private static final boolean debugRecalc = false;

  private final NavigatedPanel np;

  // fundamental quantities
  private double pwidth, pheight; // current display size (changes when window is resized)
  private double pix_per_world = 1.0; // pixels per world unit (same in both directions)
  private double pix_x0, pix_y0; // offset from world origin, in pixels

  // derived
  private ProjectionRect bb; // current world bounding box
  private final AffineTransform at; // affine transform for graphics2D
  // misc
  private boolean mapAreaIsSet; // cant initialize until screen size is known
  private boolean screenSizeIsSet; // and initial bounding box is known
  private final ZoomStack zoom = new ZoomStack();

  Navigation(NavigatedPanel np) {
    this.np = np;
    bb = new ProjectionRect();
    at = new AffineTransform();
  }

  // screen size
  public double getScreenWidth() {
    return pwidth;
  }

  public double getScreenHeight() {
    return pheight;
  }

  void setScreenSize(double pwidth, double pheight) {
    if ((pwidth == 0) || (pheight == 0))
      return;

    if (mapAreaIsSet && screenSizeIsSet) {
      // make sure bb is current
      bb = ProjectionRect.builder().setRect(getMapArea()).build();
    }

    this.pwidth = pwidth;
    this.pheight = pheight;
    screenSizeIsSet = true;
    if (debugRecalc)
      System.out.println("navigation/setScreenSize " + pwidth + " " + pheight);

    if (mapAreaIsSet) {
      recalcFromBoundingBox();
    }

    fireMapAreaEvent();
  }

  /** Get the affine transform based on screen size and world bounding box */
  public AffineTransform getTransform() {
    at.setTransform(pix_per_world, 0.0, 0.0, -pix_per_world, pix_x0, pix_y0);

    if (debug) {
      System.out.println("Navigation getTransform = " + pix_per_world + " " + pix_x0 + " " + pix_y0);
      System.out.println("  transform = " + at);
    }
    return at;
  }

  // calculate if we want to rotate based on aspect ratio
  boolean wantRotate(double displayWidth, double displayHeight) {
    this.bb = getMapArea(); // current world bounding box
    boolean aspectDisplay = displayHeight < displayWidth;
    boolean aspectWorldBB = bb.getHeight() < bb.getWidth();
    return (aspectDisplay ^ aspectWorldBB); // aspects are different
  }

  /**
   * Calculate an affine transform based on the display size parameters - used for printing.
   * 
   * @param rotate should the page be rotated?
   * @param displayX upper right corner of display area
   * @param displayY upper right corner of display area
   * @param displayWidth display area
   * @param displayHeight display area
   *
   *        see Navigation.calcTransform
   */
  AffineTransform calcTransform(boolean rotate, double displayX, double displayY, double displayWidth,
      double displayHeight) {
    this.bb = getMapArea(); // current world bounding box
    // scale to limiting dimension
    double pxpsx, pypsy;
    if (rotate) {
      pxpsx = displayHeight / bb.getWidth();
      pypsy = displayWidth / bb.getHeight();
    } else {
      pxpsx = displayWidth / bb.getWidth();
      pypsy = displayHeight / bb.getHeight();
    }
    double pps = Math.min(pxpsx, pypsy);

    // calc offset: based on center point staying in center
    double wx0 = bb.getMinX() + bb.getWidth() / 2; // world midpoint
    double wy0 = bb.getMinY() + bb.getHeight() / 2;
    double x0 = displayX + displayWidth / 2 - pps * wx0;
    double y0 = displayY + displayHeight / 2 + pps * wy0;

    AffineTransform cat = new AffineTransform(pps, 0.0, 0.0, -pps, x0, y0);

    // rotate if we need to
    if (rotate)
      cat.rotate(Math.PI / 2, wx0, wy0);

    if (debug) {
      System.out.println(
          "Navigation calcTransform = " + displayX + " " + displayY + " " + displayWidth + " " + displayHeight);
      System.out.println("  world = " + bb);
      System.out.println("  scale/origin = " + pps + " " + x0 + " " + y0);
      System.out.println("  transform = " + cat);
    }
    return cat;
  }

  /** Get current MapArea . */
  public ProjectionRect getMapArea() {
    double width = pwidth / pix_per_world;
    double height = pheight / pix_per_world;

    // center point
    double wx0 = (pwidth / 2 - pix_x0) / pix_per_world;
    double wy0 = (pix_y0 - pheight / 2) / pix_per_world;

    return ProjectionRect.builder().setRect(wx0 - width / 2, wy0 - height / 2, // minx, miny
        width, height).build();
  }

  void setMapArea(ProjectionRect ma) {
    if (debugRecalc)
      System.out.println("navigation/setMapArea " + ma);

    this.bb = ma;
    zoom.push();

    mapAreaIsSet = true;
    if (screenSizeIsSet) {
      recalcFromBoundingBox();
      fireMapAreaEvent();
    }
  }

  // kludgy thing used to deal with cylindrical seams: package private
  void setWorldCenterX(double wx_center) {
    pix_x0 = pwidth / 2 - pix_per_world * wx_center;
  }

  /** convert a world coordinate to a display point */
  Point2D worldToScreen(ProjectionPoint w) {
    Point2D p = new Point2D.Double();
    p.setLocation(pix_per_world * w.getX() + pix_x0, -pix_per_world * w.getY() + pix_y0);
    return p;
  }

  /** convert a display point to a world coordinate */
  ProjectionPoint screenToWorld(Point2D p) {
    return ProjectionPoint.create((p.getX() - pix_x0) / pix_per_world, (pix_y0 - p.getY()) / pix_per_world);
  }

  public double getPixPerWorld() {
    return pix_per_world;
  }

  /** convert screen Rectangle to a projection (world) rectangle */
  ProjectionRect screenToWorld(Point2D start, Point2D end) {
    ProjectionPoint p1 = screenToWorld(start);
    ProjectionPoint p2 = screenToWorld(end);
    return new ProjectionRect(p1.getX(), p1.getY(), p2.getX(), p2.getY());
  }

  /** convert a projection (world) rectangle to a screen Rectangle */
  java.awt.Rectangle worldToScreen(ProjectionRect projRect) {
    Point2D p1 = worldToScreen(projRect.getMaxPoint());
    Point2D p2 = worldToScreen(projRect.getMinPoint());
    return new java.awt.Rectangle((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
  }

  /////////////// domain changing calls

  /**
   * call this to change the center of the screen's world coordinates.
   * deltax, deltay in display coordinates
   */
  void pan(double deltax, double deltay) {
    zoom.push();

    pix_x0 -= deltax;
    pix_y0 -= deltay;
    fireMapAreaEvent();
  }

  /**
   * call this to zoom into a subset of the screen.
   * startx, starty are the upper left corner of the box in display coords
   * width, height the size of the box in display coords
   */
  void zoom(double startx, double starty, double width, double height) {
    if (debugZoom)
      System.out.println("zoom " + startx + " " + starty + " " + width + " " + height + " ");

    if ((width < 5) || (height < 5))
      return;
    zoom.push();

    pix_x0 -= startx + width / 2 - pwidth / 2;
    pix_y0 -= starty + height / 2 - pheight / 2;
    zoom(pwidth / width);
  }

  // keep x, y invariant while zooming in
  public void zoomIn(double x, double y) {
    zoom(1.1, x, y);
  }

  // keep x, y invariant while zooming out
  public void zoomOut(double x, double y) {
    zoom(.9, x, y);
  }

  void zoomIn() {
    zoom(2.0);
  }

  void zoomOut() {
    zoom(.5);
  }

  // see notes below
  // change scale, but leave center point fixed
  // get these equations by solving for pix_x0, pix_y0
  // that leaves center point invariant
  private void zoom(double scale) {
    zoom.push();
    double fac = (1 - scale);
    pix_x0 = scale * pix_x0 + fac * pwidth / 2;
    pix_y0 = scale * pix_y0 + fac * pheight / 2;
    pix_per_world *= scale;
    fireMapAreaEvent();
  }

  //// change scale, but leave point (x, y) fixed
  private void zoom(double scale, double x, double y) {
    zoom.push();
    double fac = (1 - scale);

    pix_x0 = scale * pix_x0 + fac * x;
    pix_y0 = scale * pix_y0 + fac * y;
    pix_per_world *= scale;
    fireMapAreaEvent();
  }

  void moveDown() {
    zoom.push();

    pix_y0 -= pheight / 2;
    fireMapAreaEvent();
  }

  void moveUp() {
    zoom.push();

    pix_y0 += pheight / 2;
    fireMapAreaEvent();
  }

  void moveRight() {
    zoom.push();

    pix_x0 -= pwidth / 2;
    fireMapAreaEvent();
  }

  void moveLeft() {
    zoom.push();

    pix_x0 += pwidth / 2;
    fireMapAreaEvent();
  }

  void zoomPrevious() {
    zoom.pop();
    fireMapAreaEvent();
  }

  /////////////////////////////////////////////////////////////////
  // private methods

  // calculate scale and offset based on the current screen size and bounding box
  // adjust bounding box to fit inside the screen size
  private void recalcFromBoundingBox() {
    if (debugRecalc) {
      System.out.println("Navigation recalcFromBoundingBox= " + bb);
      System.out.println("  " + pwidth + " " + pheight);
    }

    // decide which dimension is limiting
    double pixx_per_wx = (bb.getWidth() == 0.0) ? 1 : pwidth / bb.getWidth();
    double pixy_per_wy = (bb.getHeight() == 0.0) ? 1 : pheight / bb.getHeight();
    pix_per_world = Math.min(pixx_per_wx, pixy_per_wy);

    // calc the center point
    double wx0 = bb.getMinX() + bb.getWidth() / 2;
    double wy0 = bb.getMinY() + bb.getHeight() / 2;

    // calc offset based on center point
    pix_x0 = pwidth / 2 - pix_per_world * wx0;
    pix_y0 = pheight / 2 + pix_per_world * wy0;

    if (debugRecalc) {
      System.out.println("Navigation recalcFromBoundingBox done= " + pix_per_world + " " + pix_x0 + " " + pix_y0);
      System.out.println("  " + pwidth + " " + pheight + " " + bb);
    }
  }

  private synchronized void fireMapAreaEvent() {
    // send out event to Navigated Panel
    np.fireMapAreaEvent();
  }

  // keep stack of previous zooms
  // this should probably be made into a circular buffer
  private class ZoomStack extends java.util.ArrayList<ZoomStack.Zoom> {
    private int current = -1;

    ZoomStack() {
      super(20); // stack size
    }

    void push() {
      current++;
      add(current, new Zoom(pix_per_world, pix_x0, pix_y0));
    }

    void pop() {
      if (current < 0)
        return;
      Zoom zoom = get(current);
      pix_per_world = zoom.pix_per_world;
      pix_x0 = zoom.pix_x0;
      pix_y0 = zoom.pix_y0;
      current--;
    }

    private class Zoom {
      double pix_per_world;
      double pix_x0;
      double pix_y0;

      Zoom(double p1, double p2, double p3) {
        pix_per_world = p1;
        pix_x0 = p2;
        pix_y0 = p3;
      }
    }

  }

}

/*
 * Notes for zoom calculations, because Math is Hard (TM)
 * 
 * linear transformation between pixel space and world space
 * 
 * 1) px = px0 + ppw * wx
 * 2) py = py0 - ppw * wy
 * 3) wx = (px - px0) / ppw
 * 4) wy = (py0 - py) / ppw
 * 
 * where:
 * px, py = pixel in display spae
 * wx, wy = coordinate in world space
 * px0, py0 = offset of pixel coord from from world origin
 * ppw = pixels per unit world coordinate
 * 
 * // map bounding box to pixel box
 * pixel space goes from 0..pwidth-1, 0..pheight-1
 * world space uses a projection rectangle = map area = [minx, miny, width, height]
 * 
 * // calc the center points
 * wx0 = minx + width / 2;
 * wy0 = miny + height / 2;
 * 
 * 1) pcenterx = px0 + ppw * wx0
 * px0 = pcenterx - ppw * wx0
 * pcenterx = pwidth / 2
 * px0 = pwidth / 2 - ppw * wx0
 * 
 * 2) pcentery = py0 - ppw * wy0
 * py0 = pcentery + ppw * wy0
 * pcentery = pheight / 2
 * py0 = pheight / 2 + ppw * wy0
 * 
 * //// change scale, but leave center point fixed
 * // get these equations by solving for pix_x0, pix_y0
 * // that leaves center point invariant
 * 
 * 3) wx0 = (pcenterx - px0) / ppw
 * wx0'= wx0 = (pcenterx - px0') / scale * ppw
 * 
 * ppw*wx0 = pcenterx - px0
 * ppw'*wx0' = pcenterx - px0'
 * (ppw - ppw')*wx0 = px0' - px0
 * px0'= (ppw - ppw')*wx0 + px0
 * 
 * ppw'= ppw * scale
 * px0'= (ppw - ppw * scale)*wx0 + px0
 * px0'= (1 - scale)*wx0*ppw + px0
 * 
 * wx0 = (pcenterx - px0) / ppw
 * wx0*ppw = (pcenterx - px0)
 * 
 * px0'= (1 - scale)*(pcenterx - px0) + px0
 * px0'= (1 - scale)*pcenterx + scale * px0
 * px0'= (1 - scale)*pwidth/2 + scale * px0
 * 
 * //// change scale, but leave point (x, y) fixed
 * 
 * // we know wx:
 * 3) wx = (px - px0) / ppw
 * wx'= wx = (px - px0') / scale * ppw
 * 
 * ppw*wx = px - px0
 * scale*ppw*wx = px - px0'
 * 
 * (1 - scale)*ppw*wx = px0' - px0
 * px0'= (1 - scale)*ppw*wx + px0
 * 
 * wx = (px - px0) / ppw
 * ppw * wx = (px - px0)
 * 
 * px0'= (1 - scale)*(px - px0) + px0
 * px0'= (1 - scale)*px + scale * px0 *****
 * 
 * // we know wy:
 * 4) wy = (py0 - py) / ppw
 * wy'= wy = (py0' - py) / scale * ppw
 * 
 * ppw*wy = py0 - py
 * scale*ppw*wy = py0' - py
 * 
 * (1 - scale)*ppw*wy = py0 - py0'
 * py0'= (1 - scale)*ppw*wy - py0
 * 
 * wy = (py0 - py) / ppw
 * ppw * wx = (py0 - py)
 * 
 * py0'= (1 - scale)*(py0 - py) - py0
 * px0'= (1 - scale)*py + scale * py0
 */

