/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.grid;

import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.ui.event.ActionSourceListener;
import ucar.ui.event.ActionValueEvent;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Slider for Vertical scale */
class VertScaleSlider extends JPanel {
  private static final boolean debugEvent = false;
  private static final boolean debugLevels = false;

  private final JSlider slider;

  // event management
  private final ActionSourceListener actionSource;
  private final String actionName = "level";
  private boolean eventOK = true;
  private int incrY = 1;

  // state
  private int currentIdx = -1;
  private double min, max, scale = 1.0;
  private GridAxis<?> zAxis;

  public VertScaleSlider() {

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    slider = new JSlider(JSlider.VERTICAL, 0, 100, 0);
    // slider.setPaintTrack(false);
    // s.setMajorTickSpacing(20);
    // s.setMinorTickSpacing(5);
    slider.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

    // listen for changes from user manupulation
    slider.addChangeListener(e -> {
      if (eventOK && (zAxis != null) && !slider.getValueIsAdjusting()) {
        int pos = slider.getValue();
        int idx = slider2index(pos);
        if (idx == currentIdx)
          return;
        currentIdx = idx;

        // gotta do this after the dust settles
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setSelectedIndex(currentIdx);
            actionSource.fireActionValueEvent(actionName, zAxis.getCoordDouble(currentIdx));
          } // run
        }); // invokeLater

      } // eventPOk
    }); // add ChangeListener

    // listen for outside changes
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        if (debugEvent)
          System.out.println(" actionSource event " + e);
        setSelectedByName(e.getValue().toString());
      }
    };

    // catch resize events on the slider
    slider.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        setLabels();
      }
    });

    // this.add(Box.createRigidArea(10));
    this.add(slider);
    // this.add(Box.createRigidArea(10));
  }

  /** better way to do event management */
  public ActionSourceListener getActionSourceListener() {
    return actionSource;
  }

  public void setLevels(GridCoordinateSystem gcs, int current) {
    this.zAxis = gcs.getVerticalAxis();
    if (zAxis == null) {
      slider.setEnabled(false);
      return;
    }
    // set up the slider and conversion
    slider.setEnabled(true);
    slider.setInverted(!gcs.isZPositive());
    slider.setToolTipText(zAxis.getUnits());
    setSelectedIndex(current);

    int nz = zAxis.getNominalSize();
    this.min = Math.min(zAxis.getCoordDouble(0), zAxis.getCoordDouble(nz - 1));
    this.max = Math.max(zAxis.getCoordDouble(0), zAxis.getCoordDouble(nz - 1));

    setLabels();
    slider.setPaintLabels(true);
  }

  private void setLabels() {
    Rectangle bounds = slider.getBounds();
    double h = (bounds.getHeight() > 0) ? bounds.getHeight() : 100.0;
    double wh = (max - min) > 0.0 ? (max - min) : 1.0;
    scale = 100.0 / wh;
    double slider2pixel = h / 100.0;

    Font font = slider.getFont();
    FontMetrics fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
    if (fontMetrics != null)
      incrY = fontMetrics.getAscent(); // + fontMetrics.getDescent();

    java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();

    if (zAxis == null)
      return;
    int n = zAxis.getNominalSize();
    int last = world2slider(zAxis.getCoordDouble(n - 1)); // always
    labelTable.put(last, new JLabel(coordName(n - 1))); // always
    int next = world2slider(zAxis.getCoordDouble(0));
    labelTable.put(next, new JLabel(coordName(0))); // always

    for (int i = 1; i < n - 1; i++) {
      int ival = world2slider(zAxis.getCoordDouble(i));
      if ((slider2pixel * Math.abs(ival - last) > incrY) && (slider2pixel * Math.abs(ival - next) > incrY)) {
        labelTable.put(ival, new JLabel(coordName(i)));
        next = ival;
        if (debugLevels)
          System.out.println("  added ");
      }
    }
    slider.setLabelTable(labelTable);
  }

  private int world2slider(double val) {
    return ((int) (scale * (val - min)));
  }

  private String coordName(int index) {
    return Double.toString(zAxis.getCoordDouble(index));
  }


  private double slider2world(int pval) {
    return pval / scale + min;
  }

  private int slider2index(int pval) {
    // optimization
    // return zAxis.findCoordElement(slider2world(pval));
    // TODO wrong
    return pval;
  }

  private void setSelectedByName(String name) {
    if (zAxis == null)
      return;
    for (int i = 0; i < zAxis.getNominalSize(); i++)
      if (name.equals(coordName(i))) {
        setSelectedIndex(i);
        return;
      }
    System.out.println("ERROR VertScaleSlider cant find = " + name);
  }

  // set current value - no event
  private void setSelectedIndex(int idx) {
    if (zAxis == null)
      return;
    eventOK = false;
    currentIdx = idx;
    slider.setValue(world2slider(zAxis.getCoordDouble(currentIdx)));
    eventOK = true;
  }

}
