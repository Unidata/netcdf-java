/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.geoloc;

import ucar.ui.widget.BAMutil;
import ucar.ui.util.ListenerManager;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Format;
import ucar.ui.prefs.Debug;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;

/**
 * Implements a "navigated" JPanel within which a user can zoom and pan.
 *
 * The mapping of the screen area to world coordinates is called "navigation", and
 * it's NavigatedPanel's job to keep track of the navigation as the user zooms and pans.
 * It throws NewMapAreaEvent to indicate that the user has changed the Map area,
 * and the display needs to be redrawn. It throws PickEvents when the user double clicks
 * on the panel. <br>
 * <br>
 * NavigatedPanel has a standard JToolbar that can be displayed.
 * It also implements a "reference" point and fast updating of the
 * status of the mouse position relative to the reference point. <br>
 * <br>
 * A user typically adds a NavigatedPanel and its toolbar to its frame/applet, and
 * registers itself for NewMapAreaEvent's. See NPController class for an example.
 * When an event occurs, the user obtains
 * a Graphics2D (through the getBufferedImageGraphics() method) to draw into. The
 * AffineTransform of the Graphics2D has been set correctly to map projection coords
 * to screen coords, based on the current zoomed and panned Map area.
 * The renderer can use the AffineTransform if needed, but more typically just works in
 * projection coordinates. The renderer can also get a clipping rectangle by calling
 * g.getClip() and casting the Shape to a Rectangle2D, eg:
 * 
 * <pre>
 * Rectangle2D clipRect = (Rectangle2D) g.getClip();
 * </pre>
 *
 * Our "world coordinates" are the same as java2D's "user coordinates".
 * In general, the world coordinate plane is a projective geometry surface, typically
 * in units of "km on the projection surface".
 * The transformation from lat/lon to the projection plane is handled by a ProjectionImpl object.
 * If a user selects a different projection, NavigatedPanel.setProjection() should be called.
 * The default projection is "Cylindrical Equidistant" or "LatLon" which simply maps lat/lon
 * degrees linearly to screen coordinates. A peculiarity of this projection is that the "seam"
 * of the cylinder shifts as the user pans around. Currently our implementation sends
 * a NewMapAreaEvent whenever this happens. <br>
 * <br>
 * <br>
 * 
 * @author John Caron
 */

public class NavigatedPanel extends JPanel {

  /*
   * Implementation Notes:
   * - NavigatedPanel uses an image to buffer the image.
   * - geoSelection being kept in Projection coords; should probably be LatLon,
   * following reference point implementation. (5/16/04)
   * - bug where mouse event / image are sometimes not correctly registered when first starting up.
   * goes away when you do a manual resize. (5/16/04) may depend on jgoodies widget being
   * in parent layout ?
   */

  // public actions;
  public AbstractAction setReferenceAction;

  // main delegates
  private final Navigation navigate;
  private Projection project;

  // ui stuff
  private BufferedImage bImage;
  private final Color backColor = Color.white;
  private JLabel statusLabel;
  private final myImageObserver imageObs = new myImageObserver();
  private NToolBar toolbar;

  // scheduled redraw
  private javax.swing.Timer redrawTimer;
  private boolean changeable = true; // user allowed to change zoom/pan

  // dragging and zooming state
  private int startx, starty, deltax, deltay;
  private boolean panningMode;
  private boolean zoomingMode;
  private final Rubberband zoomRB;

  // track reference point
  private boolean isReferenceMode, hasReference;
  private ProjectionPoint refWorld = ProjectionPoint.create(0, 0);
  private LatLonPoint refLatLon = LatLonPoint.create(0, 0);
  private Point2D refScreen = new Point2D.Double();
  private Cursor referenceCursor;
  private static final int REFERENCE_CURSOR = -100;

  // selecting geo area state
  private ProjectionRect geoSelection;
  private boolean geoSelectionMode, moveSelectionMode;
  private final RubberbandRectangleHandles selectionRB;
  private ProjectionRect homeMapArea;

  // event management
  private final ListenerManager lmPick;
  private final ListenerManager lmMove;
  private final ListenerManager lmMapArea;
  private final ListenerManager lmProject;
  private final ListenerManager lmGeoSelect;

  // some working objects to minimize excessive garbage collection
  private StringBuffer sbuff = new StringBuffer(100);
  private ProjectionPoint workW = ProjectionPoint.create(0, 0);
  private Point2D workS = new Point2D.Double();
  private Rectangle myBounds = new Rectangle();
  private ProjectionRect boundingBox = new ProjectionRect();

  // DnD
  private final DropTarget dropTarget;

  // debug
  private int repaintCount;
  private static final boolean debugDraw = false, debugEvent = false, debugThread = false;
  private static final boolean debugScreensize = false, debugMaparea = false, debugNewProjection = false;
  private static final boolean debugZoom = false;
  private static final boolean debugSelection = false;

  /** The constructor. */
  public NavigatedPanel() {
    setDoubleBuffered(false); // we do our own double buffer

    // default navigation and projection
    navigate = new Navigation(this);
    project = new LatLonProjection("Cyl.Eq"); // default projection

    // toolbar actions
    makeActions();

    // listen for mouse events
    addMouseListener(new myMouseListener());
    addMouseMotionListener(new myMouseMotionListener());
    addMouseWheelListener(new myMouseWheelListener());

    // catch resize events
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        newScreenSize(getBounds());
      }
    });

    // rubberbanding
    zoomRB = new RubberbandRectangle(this, false);
    selectionRB = new RubberbandRectangleHandles(this, false);

    // DnD
    dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY, new myDropTargetListener());

    // manage Event Listener's
    lmPick = new ListenerManager("ucar.ui.geoloc.PickEventListener", "ucar.ui.geoloc.PickEvent", "actionPerformed");

    lmMove = new ListenerManager("ucar.ui.geoloc.CursorMoveEventListener", "ucar.ui.geoloc.CursorMoveEvent",
        "actionPerformed");

    lmMapArea =
        new ListenerManager("ucar.ui.geoloc.NewMapAreaListener", "ucar.ui.geoloc.NewMapAreaEvent", "actionPerformed");

    lmProject = new ListenerManager("ucar.ui.geoloc.NewProjectionListener", "ucar.ui.geoloc.NewProjectionEvent",
        "actionPerformed");

    lmGeoSelect = new ListenerManager("ucar.ui.geoloc.GeoSelectionListener", "ucar.ui.geoloc.GeoSelectionEvent",
        "actionPerformed");
  }

  public LatLonRect getGeoSelectionLL() {
    return geoSelection == null ? null : project.projToLatLonBB(geoSelection);
  }

  public ProjectionRect getGeoSelection() {
    return geoSelection;
  }

  public void setGeoSelection(ProjectionRect bb) {
    geoSelection = bb;
  }

  public void setGeoSelection(LatLonRect llbb) {
    setGeoSelection(project.latLonToProjBB(llbb));
  }

  public void setGeoSelectionMode(boolean b) {
    geoSelectionMode = b;
  }

  // event management

  /** Register a NewMapAreaListener. */
  public void addNewMapAreaListener(NewMapAreaListener l) {
    lmMapArea.addListener(l);
  }

  /** Remove a NewMapAreaListener. */
  public void removeNewMapAreaListener(NewMapAreaListener l) {
    lmMapArea.removeListener(l);
  }

  /**
   * Register a NewProjectionListener. The only time this is called is when the
   * projection isLatLon, and a zoom or pan crosses the seam.
   */
  public void addNewProjectionListener(NewProjectionListener l) {
    lmProject.addListener(l);
  }

  /** Remove a NewProjectionListener. */
  public void removeNewProjectionListener(NewProjectionListener l) {
    lmProject.removeListener(l);
  }

  /** Register a CursorMoveEventListener. */
  public void addCursorMoveEventListener(CursorMoveEventListener l) {
    lmMove.addListener(l);
  }

  /** Remove a CursorMoveEventListener. */
  public void removeCursorMoveEventListener(CursorMoveEventListener l) {
    lmMove.removeListener(l);
  }

  /** Register a PickEventListener. */
  public void addPickEventListener(PickEventListener l) {
    lmPick.addListener(l);
  }

  /** Remove a PickEventListener. */
  public void removePickEventListener(PickEventListener l) {
    lmPick.removeListener(l);
  }

  /** Register a GeoSelectionListener. */
  public void addGeoSelectionListener(GeoSelectionListener l) {
    lmGeoSelect.addListener(l);
  }

  /** Remove a GeoSelectionListener. */
  public void removeGeoSelectionListener(GeoSelectionListener l) {
    lmGeoSelect.removeListener(l);
  }

  // called by Navigation
  void fireMapAreaEvent() {
    if (debugZoom)
      System.out.println("NP.fireMapAreaEvent ");

    // decide if we need a new Projection: for LatLonProjection only
    if (project.isLatLon()) {
      LatLonProjection llproj = (LatLonProjection) project;
      ProjectionRect box = getMapArea();
      double center = llproj.getCenterLon();
      double lonBeg = LatLonPoints.lonNormal(box.getMinX(), center);
      double lonEnd = lonBeg + box.getMaxX() - box.getMinX();
      boolean showShift = Debug.isSet("projection/LatLonShift") || debugNewProjection;
      if (showShift) {
        System.out.println("projection/LatLonShift: min,max = " + box.getMinX() + " " + box.getMaxX() + " beg,end= "
            + lonBeg + " " + lonEnd + " center = " + center);
      }

      if ((lonBeg < center - 180) || (lonEnd > center + 180)) { // got to do it
        double wx0 = box.getX() + box.getWidth() / 2;
        // llproj.setCenterLon(wx0); // shift cylinder seam
        double newWx0 = llproj.getCenterLon(); // normalize wx0 to [-180,180]
        setWorldCenterX(newWx0); // tell navigation panel to shift
        if (showShift) {
          System.out.println("projection/LatLonShift: shift center to " + wx0 + "->" + newWx0);
        }

        // send projection event instead of map area event
        lmProject.sendEvent(new NewProjectionEvent(this, llproj));
        return;
      }
    }

    // send new map area event
    lmMapArea.sendEvent(new NewMapAreaEvent(this, getMapArea()));
  }


  // accessor methods
  /* Get the Navigation. */
  public Navigation getNavigation() {
    return navigate;
  }

  /* Get the background color of the NavigatedPanel. */
  public Color getBackgroundColor() {
    return backColor;
  }

  /** Set the Map Area. */
  public void setMapArea(ProjectionRect ma) {
    if (debugMaparea)
      System.out.println("NP.setMapArea " + ma);
    navigate.setMapArea(ma);
  }

  /** Set the Map Area. */
  public void setMapAreaHome(ProjectionRect ma) {
    this.homeMapArea = ma;
    navigate.setMapArea(ma);
  }

  /** Set the Map Area by converting LatLonRect to a ProjectionRect. */
  public void setMapArea(LatLonRect llbb) {
    navigate.setMapArea(project.latLonToProjBB(llbb));
  }

  /** Get the current Map Area */
  public ProjectionRect getMapArea() {
    this.boundingBox = navigate.getMapArea();
    return this.boundingBox;
  }

  /** Get the current Map Area as a lat/lon bounding box */
  public LatLonRect getMapAreaLL() {
    return project.projToLatLonBB(getMapArea());
  }

  /** kludgy thing to shift LatLon seam */
  public void setWorldCenterX(double wx_center) {
    navigate.setWorldCenterX(wx_center);
  }

  /** set the center point of the MapArea */
  public void setLatLonCenterMapArea(double lat, double lon) {
    ProjectionPoint center = project.latLonToProj(lat, lon);

    ProjectionRect.Builder ma = getMapArea().toBuilder();
    ma.setX(center.getX() - ma.getWidth() / 2);
    ma.setY(center.getY() - ma.getHeight() / 2);

    setMapArea(ma.build());
  }

  /** Get the current Projection. */
  public Projection getProjectionImpl() {
    return project;
  }

  /** Set the Projection, change the Map Area to the projection's default. */
  public void setProjectionImpl(Projection p) {
    // transfer selection region to new coord system
    if (geoSelection != null) {
      LatLonRect geoLL = project.projToLatLonBB(geoSelection);
      setGeoSelection(p.latLonToProjBB(geoLL));
    }

    // switch projections
    project = p;
    if (Debug.isSet("projection/set") || debugNewProjection)
      System.out.println("projection/set NP=" + project);

    // transfer reference point to new coord system
    if (hasReference) {
      refWorld = project.latLonToProj(refLatLon);
    }
  }

  /**
   * The status label is where the lat/lon position of the mouse is displayed. May be null.
   * 
   * @param statusLabel the Jlabel to write into
   */
  public void setPositionLabel(JLabel statusLabel) {
    this.statusLabel = statusLabel;
  }

  /** @return the "Navigate" toolbar */
  public JToolBar getNavToolBar() {
    return new NToolBar();
  }

  /** @return the "Move" toolbar */
  public JToolBar getMoveToolBar() {
    return new MoveToolBar();
  }

  /**
   * Add all of the toolbar's actions to a menu.
   * 
   * @param menu the menu to add the actions to
   */
  public void addActionsToMenu(JMenu menu) {
    BAMutil.addActionToMenu(menu, zoomIn);
    BAMutil.addActionToMenu(menu, zoomOut);
    BAMutil.addActionToMenu(menu, zoomBack);
    BAMutil.addActionToMenu(menu, zoomDefault);

    menu.addSeparator();

    BAMutil.addActionToMenu(menu, moveUp);
    BAMutil.addActionToMenu(menu, moveDown);
    BAMutil.addActionToMenu(menu, moveRight);
    BAMutil.addActionToMenu(menu, moveLeft);

    menu.addSeparator();

    BAMutil.addActionToMenu(menu, setReferenceAction);
  }

  public void setEnabledActions(boolean b) {
    zoomIn.setEnabled(b);
    zoomOut.setEnabled(b);
    zoomBack.setEnabled(b);
    zoomDefault.setEnabled(b);
    moveUp.setEnabled(b);
    moveDown.setEnabled(b);
    moveRight.setEnabled(b);
    moveLeft.setEnabled(b);
  }

  // Make sure we dont get overwhelmed by redraw calls
  // from panning, so wait delay msecs before doing the redraw.
  private void redrawLater(int delay) {
    boolean already = (redrawTimer != null) && (redrawTimer.isRunning());
    if (debugThread)
      System.out.println("redrawLater isRunning= " + already);
    if (already)
      return;

    // initialize Timer the first time
    if (redrawTimer == null) {
      redrawTimer = new javax.swing.Timer(0, e -> {
        drawG();
        redrawTimer.stop(); // one-shot timer
      });
    }
    // start the timer running
    redrawTimer.setDelay(delay);
    redrawTimer.start();
  }

  /**
   * sets whether the user can zoom/pan on this NavigatedPanel. Default = true.
   * 
   * @param mode set to false if user can't zoom/pan
   */
  public void setChangeable(boolean mode) {
    if (mode == changeable)
      return;
    changeable = mode;
    if (toolbar != null)
      toolbar.setEnabled(mode);
  }

  /** catch repaints - for debugging */
  // note: I believe that the RepaintManager is not used on JPanel subclasses ???
  public void repaint(long tm, int x, int y, int width, int height) {
    if (debugDraw)
      System.out.println("REPAINT " + repaintCount + " x " + x + " y " + y + " width " + width + " heit " + height);
    if (debugThread)
      System.out.println(" thread = " + Thread.currentThread());
    repaintCount++;
    super.repaint(tm, x, y, width, height);
  }

  /** System-triggered redraw. */
  public void paintComponent(Graphics g) {
    if (debugDraw)
      System.out.println("System called paintComponent clip= " + g.getClipBounds());
    draw((Graphics2D) g);
  }

  /** This is used to do some fancy tricks with double buffering */
  public BufferedImage getBufferedImage() {
    return bImage;
  }

  /** User must get this Graphics2D and draw into it when panel needs redrawing */
  public Graphics2D getBufferedImageGraphics() {
    if (bImage == null) {
      return null;
    }
    Graphics2D g2 = bImage.createGraphics();

    // set clipping rectangle into boundingBox
    boundingBox = navigate.getMapArea();
    if (debugMaparea) {
      System.out.println(" getBufferedImageGraphics BB = " + boundingBox);
    }

    // set graphics attributes
    g2.setTransform(navigate.getTransform());
    g2.setStroke(new BasicStroke(0.0f)); // default stroke size is one pixel
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    Rectangle2D hr = new Rectangle2D.Double();
    hr.setRect(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getWidth(), boundingBox.getHeight());
    g2.setClip(hr); // normalized coord system, because transform is applied
    g2.setBackground(backColor);

    return g2;
  }

  //////////////////////// printing ////////////////////////////////
  /**
   * utility routine for printing.
   * 
   * @param pwidth : widtht of the page, units are arbitrary
   * @param pheight : height of the page, units are arbitrary
   * @return true if we want to rotate the page
   */
  public boolean wantRotate(double pwidth, double pheight) {
    return navigate.wantRotate(pwidth, pheight);
  }

  /**
   * This calculates the Affine Transform that maps the current map area (in Projection Coordinates)
   * to a display area (in arbitrary units).
   * 
   * @param rotate should the page be rotated?
   * @param displayX upper right corner of display area
   * @param displayY upper right corner of display area
   * @param displayWidth display area
   * @param displayHeight display area
   *
   *        see Navigation.calcTransform
   */
  public AffineTransform calcTransform(boolean rotate, double displayX, double displayY, double displayWidth,
      double displayHeight) {
    return navigate.calcTransform(rotate, displayX, displayY, displayWidth, displayHeight);
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // private methods

  // when component resizes we need a new buffer
  private void newScreenSize(Rectangle b) {
    boolean sameSize = (b.width == myBounds.width) && (b.height == myBounds.height);
    if (debugScreensize) {
      System.out.println("NavigatedPanel newScreenSize old= " + myBounds);
    }
    if (sameSize && (b.x == myBounds.x) && (b.y == myBounds.y))
      return;

    myBounds.setBounds(b);
    if (sameSize)
      return;

    if (debugScreensize) {
      System.out.println("  newBounds = " + b);
    }

    if ((b.width > 0) && (b.height > 0)) {
      bImage = new BufferedImage(b.width, b.height, BufferedImage.TYPE_INT_RGB); // why RGB ?
    } else { // why not device dependent?
      bImage = null;
    }

    navigate.setScreenSize(b.width, b.height);
  }

  // draw and drawG are like "paintImmediately()"
  public void drawG() {
    Graphics g = getGraphics(); // bypasses double buffering ?
    if (null != g) {
      draw((Graphics2D) g);
      g.dispose();
    }
  }

  private void draw(Graphics2D g) {
    if (bImage == null)
      return;
    boolean redrawReference = true;
    Rectangle bounds = getBounds();

    if (panningMode) {
      if (debugDraw)
        System.out.println("draw draggingMode ");
      // Clear the image.
      g.setBackground(backColor);
      g.clearRect(0, 0, bounds.width, bounds.height);
      g.drawImage(bImage, deltax, deltay, backColor, imageObs);
      redrawReference = false;
    } else {
      if (debugDraw)
        System.out.println("draw copy ");
      g.drawImage(bImage, 0, 0, backColor, imageObs);
    }

    if (hasReference && redrawReference) {
      refWorld = project.latLonToProj(refLatLon);
      refScreen = navigate.worldToScreen(refWorld);
      int px = (int) refScreen.getX();
      int py = (int) refScreen.getY();
      g.setColor(Color.red);
      g.setStroke(new BasicStroke(2.0f));
      // g.drawImage( referenceCursor.getImage(), px, py, Color.red, imageObs);
      int referenceSize = 12;
      g.drawLine(px, py - referenceSize, px, py + referenceSize);
      g.drawLine(px - referenceSize, py, px + referenceSize, py);
    }
  }

  private void setCursor(int what) {
    if (what == REFERENCE_CURSOR) {
      if (null == referenceCursor) {
        referenceCursor = BAMutil.makeCursor("nj22/ReferencePoint");
        if (null == referenceCursor)
          return;
      }
      super.setCursor(referenceCursor);
    } else
      super.setCursor(Cursor.getPredefinedCursor(what));
  }

  private void showStatus(int mousex, int mousey) {
    if ((statusLabel == null) && !lmMove.hasListeners())
      return;

    workS.setLocation(mousex, mousey);
    workW = navigate.screenToWorld(workS);
    LatLonPoint workL = project.projToLatLon(workW);
    if (workL == null) {
      return;
    }
    if (lmMove.hasListeners())
      lmMove.sendEvent(new CursorMoveEvent(this, workW));

    if (statusLabel == null)
      return;

    sbuff.setLength(0);

    if (ucar.ui.prefs.Debug.isSet("projection/showPosition")) {
      sbuff.append(workW).append(" -> ");
    }

    sbuff.append(LatLonPoints.toString(workL, 5));

    if (hasReference) {
      Bearing bearing = Bearing.calculateBearing(refLatLon, workL);
      sbuff.append("  (");
      sbuff.append(Format.dfrac(bearing.getAzimuth(), 0));
      sbuff.append(" deg ");
      sbuff.append(Format.d(bearing.getDistance(), 4, 5));
      sbuff.append(" km)");
    }

    statusLabel.setText(sbuff.toString());
  }

  private void setReferenceMode() {
    if (isReferenceMode) { // toggle
      isReferenceMode = false;
      setCursor(Cursor.DEFAULT_CURSOR);
      statusLabel.setToolTipText("position at cursor");
      drawG();
    } else {
      hasReference = false;
      isReferenceMode = true;
      setCursor(Cursor.CROSSHAIR_CURSOR);
      statusLabel.setToolTipText("position (bearing)");
    }
  }

  //////////////////////////////////////////////////////////////
  // inner classes

  /*
   * myMouseListener and myMouseMotionListener implements the standard mouse behavior.
   * 
   * A: pan:
   * press right : start panning mode
   * drag : pan image
   * release: redraw with new area
   * 
   * B: zoom:
   * press left : start zooming mode
   * drag : draw rubberband
   * release: redraw with new area
   * 
   * C: move:
   * show cursor location / distance from reference point
   * 
   * D. click:
   * (reference mode) : set reference point
   * (not reference mode): throw pick event.
   * 
   * E: selection:
   * press left : find anchor point = diagonal corner
   * drag left : draw rubberband
   * press right : start move selection mode
   * drag right : move selection
   * release: throw new selection event
   */

  private class myMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      // pick event
      if (isReferenceMode) {
        hasReference = true;
        refScreen.setLocation(e.getX(), e.getY());
        refWorld = navigate.screenToWorld(refScreen);
        refLatLon = project.projToLatLon(refWorld);
        setCursor(REFERENCE_CURSOR);
        drawG();
      } else {
        workS.setLocation(e.getX(), e.getY());
        workW = navigate.screenToWorld(workS);
        lmPick.sendEvent(new PickEvent(NavigatedPanel.this, workW));
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (!changeable) {
        return;
      }

      startx = e.getX();
      starty = e.getY();
      if (debugSelection) {
        System.out.printf("NP mousePressed= %s%n", e);
      }

      if (geoSelectionMode && SwingUtilities.isLeftMouseButton(e)) {
        // start geoSelection
        ProjectionPoint pp = navigate.screenToWorld(e.getPoint());
        geoSelection = new ProjectionRect(pp, 0, 0);
        Rectangle sw = navigate.worldToScreen(geoSelection);
        selectionRB.setRectangle(sw);
        if (debugSelection) {
          System.out.printf("NB start selection= %s => %s%n", geoSelection, sw);
        }

        if (selectionRB.anchor2(e.getPoint(), 10, 10)) {
          selectionRB.setActive(true);
          if (debugSelection) {
            System.out.println("  anchor at =" + selectionRB.getAnchor());
          }
        }
        return;
      } // geoSelectionMode

      if (SwingUtilities.isLeftMouseButton(e)) {
        // initiate pan
        panningMode = true;
        setCursor(Cursor.MOVE_CURSOR);
      }

      /*
       * initiate zoom
       * zoomRB.anchor(e.getPoint());
       * zoomRB.setActive(true);
       * zoomingMode = true;
       */


      if (debugEvent) {
        System.out.println("mousePressed " + startx + " " + starty);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (!changeable) {
        return;
      }

      deltax = e.getX() - startx;
      deltay = e.getY() - starty;
      if (debugEvent) {
        System.out.println("mouseReleased " + e.getX() + " " + e.getY() + "=" + deltax + " " + deltay);
      }
      if (debugSelection) {
        System.out.printf("NP mouseReleased= %s%n", e);
      }

      if (geoSelectionMode && selectionRB.isActive()) {
        selectionRB.setActive(false);
        selectionRB.done();
        moveSelectionMode = false;
        setCursor(Cursor.DEFAULT_CURSOR);

        if (!NavigatedPanel.this.contains(e.getPoint())) { // point is off the panel
          if (debugScreensize) {
            System.out.println("NP.select: point " + e.getPoint() + " out of bounds: " + myBounds);
          }
          return;
        }

        geoSelection = navigate.screenToWorld(selectionRB.getAnchor(), selectionRB.getLast());
        if (debugSelection) {
          System.out.printf("NP selection rect= %s %s => %s%n", selectionRB.getAnchor(), selectionRB.getLast(),
              geoSelection);
        }

        // send geoSelection event
        lmGeoSelect.sendEvent(new GeoSelectionEvent(this, geoSelection));
        return;
      }

      if (panningMode) {
        navigate.pan(-deltax, -deltay);
        panningMode = false;
        setCursor(Cursor.DEFAULT_CURSOR);
      }

      /*
       * if (zoomingMode) {
       * zoomRB.setActive(false);
       * zoomRB.end(e.getPoint());
       * zoomingMode = false;
       * if (!NavigatedPanel.this.contains(e.getPoint())) { // point is off the panel
       * if (debugScreensize)
       * System.out.println("NP.zoom: point " + e.getPoint() + " out of bounds: " + myBounds);
       * return;
       * }
       * // "start" must be upper left
       * startx = Math.min(startx, e.getX());
       * starty = Math.min(starty, e.getY());
       * navigate.zoom(startx, starty, Math.abs(deltax), Math.abs(deltay));
       * }
       */
      // drawG();
    }
  } // end myMouseListener

  // mouseMotionListener
  private class myMouseMotionListener implements MouseMotionListener {
    @Override
    public void mouseDragged(MouseEvent e) {
      if (!changeable)
        return;
      deltax = e.getX() - startx;
      deltay = e.getY() - starty;
      if (debugEvent)
        System.out.println("mouseDragged " + e.getX() + " " + e.getY() + "=" + deltax + " " + deltay);

      if (zoomingMode) {
        zoomRB.stretch(e.getPoint());
        return;
      }

      if (moveSelectionMode) {
        selectionRB.move(deltax, deltay);
        return;
      }

      if (geoSelectionMode && !SwingUtilities.isRightMouseButton(e) && selectionRB.isActive()) {
        selectionRB.stretch(e.getPoint());
        return;
      }

      repaint();
      // redrawLater(100); // schedule redraw in 100 msecs
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      showStatus(e.getX(), e.getY());
    }
  }

  private class myMouseWheelListener extends MouseAdapter {
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      if (e.getWheelRotation() < 0) {
        for (int rotation = e.getWheelRotation(); rotation < 0; ++rotation) {
          navigate.zoomIn(e.getX(), e.getY());
          drawG();
        }
      } else {
        for (int rotation = e.getWheelRotation(); rotation > 0; --rotation) {
          navigate.zoomOut(e.getX(), e.getY());
          drawG();
        }
      }
    }
  }

  // necessary for g.drawImage()
  private static class myImageObserver implements ImageObserver {
    public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
      return true;
    }
  }

  // DnD
  private class myDropTargetListener implements DropTargetListener {

    public void dragEnter(DropTargetDragEvent e) {
      System.out.println(" NP dragEnter active = " + dropTarget.isActive());
      e.acceptDrag(DnDConstants.ACTION_COPY);
    }

    public void drop(DropTargetDropEvent e) {
      try {
        if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          Transferable tr = e.getTransferable();
          e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          String s = (String) tr.getTransferData(DataFlavor.stringFlavor);
          // dropList.add(s);
          System.out.println(" NP myDropTargetListener got " + s);
          e.dropComplete(true);
        } else {
          e.rejectDrop();
        }
      } catch (IOException | UnsupportedFlavorException io) {
        io.printStackTrace();
        e.rejectDrop();
      }
    }

    public void dragExit(DropTargetEvent e) {}

    public void dragOver(DropTargetDragEvent e) {}

    public void dropActionChanged(DropTargetDragEvent e) {}
  }

  //////////////////////////////////////////////////////////////////////////////
  // toolbars

  private AbstractAction zoomIn, zoomOut, zoomDefault, zoomBack;
  private AbstractAction moveUp, moveDown, moveLeft, moveRight;

  private void makeActions() {
    // add buttons/actions
    zoomIn = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.zoomIn();
        drawG();
      }
    };
    BAMutil.setActionProperties(zoomIn, "MagnifyPlus", "zoom In", false, 'I', KeyEvent.VK_ADD);

    zoomOut = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.zoomOut();
        drawG();
      }
    };
    BAMutil.setActionProperties(zoomOut, "MagnifyMinus", "zoom Out", false, 'O', KeyEvent.VK_SUBTRACT);

    zoomBack = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.zoomPrevious();
        drawG();
      }
    };
    BAMutil.setActionProperties(zoomBack, "Undo", "Previous map area", false, 'P', KeyEvent.VK_BACK_SPACE);

    zoomDefault = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (homeMapArea != null) {
          navigate.setMapArea(homeMapArea);
          drawG();
        }
      }
    };
    BAMutil.setActionProperties(zoomDefault, "Home", "Home map area", false, 'H', KeyEvent.VK_HOME);

    moveUp = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.moveUp();
        drawG();
      }
    };
    BAMutil.setActionProperties(moveUp, "Up", "move view Up", false, 'U', KeyEvent.VK_UP);

    moveDown = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.moveDown();
        drawG();
      }
    };
    BAMutil.setActionProperties(moveDown, "Down", "move view Down", false, 'D', KeyEvent.VK_DOWN);

    moveLeft = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.moveLeft();
        drawG();
      }
    };
    BAMutil.setActionProperties(moveLeft, "Left", "move view Left", false, 'L', KeyEvent.VK_LEFT);

    moveRight = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate.moveRight();
        drawG();
      }
    };
    BAMutil.setActionProperties(moveRight, "Right", "move view Right", false, 'R', KeyEvent.VK_RIGHT);

    setReferenceAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setReferenceMode();
        drawG();
      }
    };
    BAMutil.setActionProperties(setReferenceAction, "nj22/ReferencePoint", "set reference Point", true, 'P', 0);
  }

  class NToolBar extends JToolBar {
    NToolBar() {
      AbstractButton b = BAMutil.addActionToContainer(this, zoomIn);
      b.setName("zoomIn");

      b = BAMutil.addActionToContainer(this, zoomOut);
      b.setName("zoomOut");

      b = BAMutil.addActionToContainer(this, zoomBack);
      b.setName("zoomBack");

      b = BAMutil.addActionToContainer(this, zoomDefault);
      b.setName("zoomHome");
    }
  }

  class MoveToolBar extends JToolBar {
    MoveToolBar() {
      AbstractButton b = BAMutil.addActionToContainer(this, moveUp);
      b.setName("moveUp");

      b = BAMutil.addActionToContainer(this, moveDown);
      b.setName("moveDown");

      b = BAMutil.addActionToContainer(this, moveLeft);
      b.setName("moveLeft");

      b = BAMutil.addActionToContainer(this, moveRight);
      b.setName("moveRight");
    }
  }

  @Override
  public String toString() {
    return "NavigatedPanel{" + "panningMode=" + panningMode + ", zoomingMode=" + zoomingMode + ", isReferenceMode="
        + isReferenceMode + ", geoSelectionMode=" + geoSelectionMode + ", moveSelectionMode=" + moveSelectionMode + '}';
  }
} // end NavPanel


