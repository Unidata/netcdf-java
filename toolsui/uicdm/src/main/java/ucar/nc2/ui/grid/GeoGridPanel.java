/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.gis.shapefile.ShapeFileBean;
import ucar.nc2.ui.gis.worldmap.WorldMapBean;
import ucar.nc2.ui.grid.GeoGridTable;
import ucar.nc2.ui.grid.GridUI;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

public class GeoGridPanel extends OpPanel {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GRIDVIEW_FRAME_SIZE = "GridViewerWindowSize";

  private final GeoGridTable dsTable;
  private IndependentWindow viewerWindow;
  private GridUI gridUI;

  private NetcdfDataset ds;

  public GeoGridPanel(PreferencesExt prefs) {
    super(prefs, "dataset:", true, false);
    dsTable = new GeoGridTable(prefs, true);
    add(dsTable, BorderLayout.CENTER);

    AbstractButton viewButton = BAMutil.makeButtcon("alien", "Grid Viewer", false);
    viewButton.addActionListener(e -> {
      if (ds != null) {
        GridDataset gridDataset = dsTable.getGridDataset();
        if (gridUI == null) {
          makeGridUI();
        }
        gridUI.setDataset(gridDataset);
        viewerWindow.show();
      }
    });
    buttPanel.add(viewButton);

    dsTable.addExtra(buttPanel, fileChooser);
  }

  private void makeGridUI() {
    // a little tricky to get the parent right for GridUI
    viewerWindow = new IndependentWindow("Grid Viewer", BAMutil.getImage("nj22/NetcdfUI"));

    gridUI = new GridUI((PreferencesExt) prefs.node("GridUI"), viewerWindow, fileChooser, 800);
    gridUI.addMapBean(new WorldMapBean());
    gridUI.addMapBean(
        new ShapeFileBean("WorldDetailMap", "Global Detailed Map", "nj22/WorldDetailMap", ToolsUI.WORLD_DETAIL_MAP));
    gridUI.addMapBean(new ShapeFileBean("USDetailMap", "US Detailed Map", "nj22/USMap", ToolsUI.US_MAP));

    viewerWindow.setComponent(gridUI);
    Rectangle bounds = (Rectangle) prefs.getBean(GRIDVIEW_FRAME_SIZE, new Rectangle(77, 22, 700, 900));
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

    NetcdfDataset newds;
    try {
      newds = NetcdfDatasets.openDataset(command, true, null);
      if (newds == null) {
        JOptionPane.showMessageDialog(null, "NetcdfDatasets.open cannot open " + command);
        return false;
      }
      setDataset(newds);
    } catch (FileNotFoundException ioe) {
      JOptionPane.showMessageDialog(null, "NetcdfDatasets.open cannot open " + command + "\n" + ioe.getMessage());
      // ioe.printStackTrace();
      err = true;
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

  @Override
  public void closeOpenFiles() throws IOException {
    if (ds != null) {
      ds.close();
    }
    ds = null;
    dsTable.clear();
    if (gridUI != null) {
      gridUI.clear();
    }
  }

  public void setDataset(NetcdfDataset newds) {
    if (newds == null) {
      return;
    }
    try {
      if (ds != null) {
        ds.close();
      }
    } catch (IOException ioe) {
      logger.warn("close failed");
    }

    Formatter parseInfo = new Formatter();
    this.ds = newds;
    try {
      dsTable.setDataset(newds, parseInfo);
    } catch (IOException e) {
      String info = parseInfo.toString();
      if (!info.isEmpty()) {
        detailTA.setText(info);
        detailWindow.show();
      }
      e.printStackTrace();
      return;
    }
    setSelectedItem(newds.getLocation());
  }

  public void setDataset(GridDataset gds) {
    if (gds == null) {
      return;
    }
    try {
      if (ds != null) {
        ds.close();
      }
    } catch (IOException ioe) {
      logger.warn("close failed");
    }

    this.ds = (NetcdfDataset) gds.getNetcdfFile(); // ??
    dsTable.setDataset(gds);
    setSelectedItem(gds.getLocation());
  }

  @Override
  public void save() {
    super.save();
    dsTable.save();
    if (gridUI != null) {
      gridUI.storePersistentData();
    }
    if (viewerWindow != null) {
      prefs.putBeanObject(GRIDVIEW_FRAME_SIZE, viewerWindow.getBounds());
    }
  }
}
