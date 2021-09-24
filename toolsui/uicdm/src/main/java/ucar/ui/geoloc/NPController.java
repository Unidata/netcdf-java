/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.geoloc;

import ucar.ui.gis.MapBean;
import ucar.ui.gis.worldmap.WorldMap;
import ucar.ui.util.Renderer;
import ucar.ui.widget.*;
import ucar.unidata.geoloc.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.geom.AffineTransform;
import java.awt.*;
import java.util.ArrayList;

/**
 * A superclass for Navigated Panel controllers
 *
 * @author John Caron
 */
public class NPController extends JPanel {
  private static final boolean debug = false;

  protected NavigatedPanel np;
  protected ArrayList<ucar.ui.util.Renderer> renderers = new ArrayList<>(); // thredds.viewer.ui.Renderer
  protected Projection project;
  protected AffineTransform atI = new AffineTransform(); // identity transform
  protected boolean eventOk = true;

  protected JPanel toolPanel;

  public NPController() {
    // here's where the map will be drawn:
    np = new NavigatedPanel();
    ucar.ui.util.Renderer render = new WorldMap(); // default Renderer
    project = np.getProjectionImpl();
    render.setProjection(project);
    addRenderer(render);

    // get Projection Events from the navigated panel
    np.addNewProjectionListener(e -> {
      Projection p = e.getProjection();
      for (Object renderer : renderers) {
        ucar.ui.util.Renderer r = (ucar.ui.util.Renderer) renderer;
        r.setProjection(p);
      }
      redraw(true);
    });

    // get NewMapAreaEvents from the navigated panel
    np.addNewMapAreaListener(e -> redraw(true));

    ucar.ui.widget.PopupMenu mapBeanMenu = MapBean.getStandardMapSelectButton(e -> {
      if (e.getPropertyName().equals("Renderer")) {
        ucar.ui.util.Renderer mapRender = (ucar.ui.util.Renderer) e.getNewValue();
        mapRender.setProjection(np.getProjectionImpl());
        renderers.set(0, mapRender); // always first
        redraw(true);
      }
    });

    toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    toolPanel.add(mapBeanMenu.getParentComponent());
    toolPanel.add(np.getNavToolBar());
    toolPanel.add(np.getMoveToolBar());
    BAMutil.addActionToContainer(toolPanel, np.setReferenceAction);

    makeUI();
  }

  protected void makeUI() {
    setLayout(new BorderLayout());

    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(new EtchedBorder());
    JLabel positionLabel = new JLabel("position");
    statusPanel.add(positionLabel, BorderLayout.CENTER);

    np.setPositionLabel(positionLabel);
    add(toolPanel, BorderLayout.NORTH);
    add(np, BorderLayout.CENTER);
    add(statusPanel, BorderLayout.SOUTH);
  }

  public NavigatedPanel getNavigatedPanel() {
    return np;
  }

  public void addRenderer(ucar.ui.util.Renderer r) {
    renderers.add(r);
    r.setProjection(project);
  }

  public void setProjection(Projection p) {
    project = p;
    for (Object renderer : renderers) {
      ucar.ui.util.Renderer r = (ucar.ui.util.Renderer) renderer;
      r.setProjection(p);
    }

    eventOk = false;
    np.setProjectionImpl(p);
    eventOk = true;
    redraw(true);
  }

  protected void redraw(boolean complete) {
    if (project == null)
      return;

    long tstart = System.currentTimeMillis();

    java.awt.Graphics2D gNP = np.getBufferedImageGraphics();
    if (gNP == null) // panel not drawn on screen yet
      return;

    // clear it
    gNP.setBackground(np.getBackgroundColor());
    gNP.fill(gNP.getClipBounds());

    for (Object renderer : renderers) {
      ucar.ui.util.Renderer r = (Renderer) renderer;
      r.draw(gNP, atI);
    }
    gNP.dispose();

    if (debug) {
      long tend = System.currentTimeMillis();
      System.out.println("NPController draw time = " + (tend - tstart) / 1000.0 + " secs");
    }
    // copy buffer to the screen
    np.repaint();
  }

}
