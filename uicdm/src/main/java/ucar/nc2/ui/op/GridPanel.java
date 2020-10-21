/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.gis.shapefile.ShapeFileBean;
import ucar.nc2.ui.gis.worldmap.WorldMapBean;
import ucar.nc2.ui.grid2.GridNewTable;
import ucar.nc2.ui.grid2.GridViewer;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.Optional;

public class GridPanel extends OpPanel {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final GridNewTable gridNewTable;
  private GridViewer gridViewer;
  private IndependentWindow viewerWindow;

  private GridDataset gridDataset;

  public GridPanel(PreferencesExt prefs) {
    super(prefs, "dataset:", true, false);
    gridNewTable = new GridNewTable(prefs);
    add(gridNewTable, BorderLayout.CENTER);

    AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
    viewButton.addActionListener(e -> {
      GridDataset gridDataset = gridNewTable.getGridCollection();
      if (gridDataset == null) {
        return;
      }
      if (gridViewer == null) {
        makeDisplay();
      }
      gridViewer.setGridCollection(gridDataset);
      viewerWindow.show();
    });
    buttPanel.add(viewButton);

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
    infoButton.addActionListener(e -> {
      Formatter f = new Formatter();
      gridNewTable.showInfo(f);
      detailTA.setText(f.toString());
      detailTA.gotoTop();
      detailWindow.show();
    });
    buttPanel.add(infoButton);
  }

  private void makeDisplay() {
    viewerWindow = new IndependentWindow("GridNew Viewer", BAMutil.getImage("nj22/NetcdfUI"));

    gridViewer = new GridViewer((PreferencesExt) prefs.node("CoverageDisplay"), viewerWindow, fileChooser, 800);
    gridViewer.addMapBean(new WorldMapBean());
    gridViewer.addMapBean(
        new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "nj22/WorldDetailMap", ToolsUI.WORLD_DETAIL_MAP));
    gridViewer.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "nj22/USMap", ToolsUI.US_MAP));

    viewerWindow.setComponent(gridViewer);
    Rectangle bounds = (Rectangle) ToolsUI.getPrefsBean(ToolsUI.GRIDVIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900));
    if (bounds.x < 0) {
      bounds.x = 0;
    }
    if (bounds.y < 0) {
      bounds.x = 0;
    }
    viewerWindow.setBounds(bounds);
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    boolean err = false;

    // close previous file
    try {
      closeOpenFiles();
    } catch (IOException ioe) {
      logger.warn("close failed");
    }

    try {
      Formatter errLog = new Formatter();
      Optional<GridDataset> opt = GridDatasetFactory.openGridDataset(command, errLog);
      if (!opt.isPresent()) {
        JOptionPane.showMessageDialog(null, errLog.toString());
        return false;
      }
      gridDataset = opt.get();
      gridNewTable.setCollection(gridDataset);
      setSelectedItem(command);
    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, String.format("GridPanel cant open %s err=%s", command, e.getMessage()));
    } catch (Throwable ioe) {
      ioe.printStackTrace();
      StringWriter sw = new StringWriter(5000);
      ioe.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailWindow.show();
      err = true;
    }

    return !err;
  }

  public void setDataset(GridDataset fd) {
    if (fd == null) {
      return;
    }

    try {
      closeOpenFiles();
    } catch (IOException ioe) {
      logger.warn("close failed");
    }

    gridNewTable.setCollection(fd);
    setSelectedItem(fd.getLocation());
  }

  @Override
  public void closeOpenFiles() throws IOException {
    if (gridDataset != null) {
      gridDataset.close();
    }
    gridDataset = null;
    gridNewTable.clear();
  }

  @Override
  public void save() {
    super.save();
    gridNewTable.save();
    if (viewerWindow != null) {
      ToolsUI.putPrefsBeanObject(ToolsUI.GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
    }
  }
}
