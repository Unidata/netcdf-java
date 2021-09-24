/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.grid;

import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.ui.event.ActionSourceListener;
import ucar.nc2.ui.widget.ScaledPanel;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.Projection;
import javax.swing.*;
import java.awt.*;

/**
 * 2D Vertical "slice" drawing widget.
 * Integrates a drawing area (ucar.unidata.ui.ScaledPanel), a slider widget
 * (ucar.unidata.view.grid.VertScaleSlider) and a bottom axis.
 */

class VertPanel extends JPanel {
  private static final boolean debugBounds = false;

  private final ScaledPanel drawArea;
  private final VertScaleSlider vslider;
  private final JLabel leftScale, midScale, rightScale, vertUnitsLabel;

  private double yleft, ymid, yright;
  private boolean isLatLon = true;
  private Projection proj;
  private GridAxisPoint xaxis;

  public VertPanel() {
    setLayout(new BorderLayout());

    JPanel botScale = new JPanel(new BorderLayout());
    leftScale = new JLabel("leftScale");
    rightScale = new JLabel("rightScale");
    midScale = new JLabel("midScale", SwingConstants.CENTER);
    botScale.add(leftScale, BorderLayout.WEST);
    botScale.add(midScale, BorderLayout.CENTER);
    botScale.add(rightScale, BorderLayout.EAST);

    drawArea = new ScaledPanel();
    vslider = new VertScaleSlider();

    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(vslider, BorderLayout.CENTER);
    vertUnitsLabel = new JLabel(" ");
    rightPanel.add(vertUnitsLabel, BorderLayout.SOUTH);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(drawArea, BorderLayout.CENTER);
    leftPanel.add(botScale, BorderLayout.SOUTH);

    add(leftPanel, BorderLayout.CENTER);
    add(rightPanel, BorderLayout.EAST);
  }

  /** better way to do event management */
  public ActionSourceListener getActionSourceListener() {
    return vslider.getActionSourceListener();
  }

  /** User must get this Graphics2D and draw into it when panel needs redrawing */
  public ScaledPanel getDrawArea() {
    return drawArea;
  }

  public void setLevels(GridCoordinateSystem gcs, int current) {
    vslider.setLevels(gcs, current);
  }

  public void setCoordSys(GridCoordinateSystem geocs, int currentLevel) {
    GridAxis<?> zaxis = geocs.getVerticalAxis();
    if (zaxis == null)
      return;

    vslider.setLevels(geocs, currentLevel);
    vertUnitsLabel.setText("  " + zaxis.getUnits());

    /*
     * set the bounds of the world coordinates.
     * The point (world.getX(), world.getY()) is mapped to the lower left point of the screen.
     * The point (world.getX() + world.Width(), world.getY()+world.Height()) is mapped
     * to the upper right corner. Therefore if coords decrease as you go up, world.Height()
     * should be negetive.
     */
    GridAxis<?> yaxis = geocs.getYHorizAxis();
    if ((yaxis == null) || (zaxis == null)) {
      return;
    }
    int nz = zaxis.getNominalSize();
    int ny = yaxis.getNominalSize();

    // must determine which is on top: depends on if z is up or down!
    double zmin = Math.min(zaxis.getCoordDouble(0), zaxis.getCoordDouble(nz - 1));
    double zmax = Math.max(zaxis.getCoordDouble(0), zaxis.getCoordDouble(nz - 1));
    double zupper, zlower;
    if (geocs.isZPositive()) {
      zlower = zmin;
      zupper = zmax;
    } else {
      zlower = zmax;
      zupper = zmin;
    }

    // LOOK: actuallly may be non-linear if its a 2D XY coordinate system, so this is just an approximation
    yleft = Math.min(yaxis.getCoordDouble(0), yaxis.getCoordDouble(ny - 1));
    yright = Math.max(yaxis.getCoordDouble(0), yaxis.getCoordDouble(ny - 1));

    if (debugBounds) {
      System.out.println("VertPanel: ascending from " + zlower + " to " + zupper);
      System.out.println("VertPanel: from left " + yleft + " to right " + yright);
    }

    ScaledPanel.Bounds bounds = new ScaledPanel.Bounds(yleft, yright, zupper, zlower);
    drawArea.setWorldBounds(bounds);

    // set bottom scale
    proj = geocs.getHorizCoordinateSystem().getProjection();
    isLatLon = geocs.getHorizCoordinateSystem().isLatLon();
    ymid = (yleft + yright) / 2;

    xaxis = geocs.getHorizCoordinateSystem().getXHorizAxis();

    setSlice(0);
  }

  public void setSlice(int slice) {

    if (isLatLon) {
      leftScale.setText(LatLonPoints.latToString(yleft, 3));
      midScale.setText(LatLonPoints.latToString(ymid, 3));
      rightScale.setText(LatLonPoints.latToString(yright, 3));
      return;
    }

    if (xaxis != null) {
      double xval = xaxis.getCoordDouble(slice);

      // set bottom scale
      leftScale.setText(getYstr(xval, yleft));
      midScale.setText(getYstr(xval, ymid));
      rightScale.setText(getYstr(xval, yright));
    }

    repaint();
  }

  private String getYstr(double xvalue, double yvalue) {
    LatLonPoint lpt = proj.projToLatLon(xvalue, yvalue);
    return LatLonPoints.latToString(lpt.getLatitude(), 3);
  }

}
