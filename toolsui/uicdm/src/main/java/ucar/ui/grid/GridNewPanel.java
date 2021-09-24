/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grid;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.ui.OpPanel;
import ucar.ui.ToolsUI;
import ucar.ui.gis.shapefile.ShapeFileBean;
import ucar.ui.gis.worldmap.WorldMapBean;
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

public class GridNewPanel extends OpPanel {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GRIDNEW_VIEW_FRAME_SIZE = "GridNewViewerWindowSize";

  private final GridNewTable gridNewTable;
  private GridViewer gridViewer;
  private IndependentWindow viewerWindow;

  private GridDataset gridDataset;

  public GridNewPanel(PreferencesExt prefs) {
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
    viewerWindow = new IndependentWindow("Grid2 Viewer", BAMutil.getImage("nj22/NetcdfUI"));

    gridViewer = new GridViewer((PreferencesExt) prefs.node("CoverageDisplay"), viewerWindow, fileChooser, 800);
    gridViewer.addMapBean(new WorldMapBean());
    gridViewer.addMapBean(
        new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "nj22/WorldDetailMap", ToolsUI.WORLD_DETAIL_MAP));
    gridViewer.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "nj22/USMap", ToolsUI.US_MAP));

    viewerWindow.setComponent(gridViewer);
    Rectangle bounds = (Rectangle) prefs.getBean(GRIDNEW_VIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900));
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
      gridDataset = GridDatasetFactory.openGridDataset(command, errLog);
      if (gridDataset == null) {
        JOptionPane.showMessageDialog(null, errLog.toString());
        return false;
      }
      gridNewTable.setGridDataset(gridDataset);
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

    gridNewTable.setGridDataset(fd);
    setSelectedItem(fd.getLocation());
  }

  public void setNetcdfDataset(NetcdfDataset ncd) {
    if (ncd == null) {
      return;
    }
    try {
      closeOpenFiles();
      GridDataset fd = GridDatasetFactory.wrapGridDataset(ncd, new Formatter()).orElse(null);
      gridNewTable.setGridDataset(fd);
      setSelectedItem(fd.getLocation());
    } catch (IOException ioe) {
      logger.warn("close failed");
    }
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
      prefs.putBeanObject(GRIDNEW_VIEW_FRAME_SIZE, viewerWindow.getBounds());
    }
  }
}
