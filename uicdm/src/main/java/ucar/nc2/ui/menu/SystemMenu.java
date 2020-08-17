/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.menu;

import com.google.common.collect.ImmutableList;
import java.util.Set;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.nc2.internal.cache.FileCacheIF;
import ucar.ui.widget.PLAF;
import ucar.unidata.io.RandomAccessFile;
import java.awt.event.ActionEvent;
import java.util.Formatter;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.JMenu;

public class SystemMenu extends JMenu {
  private ToolsUI toolsui;
  private static boolean isCacheInit;

  public SystemMenu(ToolsUI tui) {
    super("System");
    setMnemonic('S');

    this.toolsui = tui;

    AbstractAction clearHttpStateAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        // IGNORE HttpClientManager.clearState();
      }
    };
    BAMutil.setActionProperties(clearHttpStateAction, null, "Clear Http State", false, 'S', -1);
    BAMutil.addActionToMenu(this, clearHttpStateAction);

    AbstractAction showCacheAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        f.format("RandomAccessFileCache contents%n");

        FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
        if (null != rafCache) {
          rafCache.showCache(f);
        }
        f.format("%nNetcdfFileCache contents%n");

        FileCacheIF cache = NetcdfDatasets.getNetcdfFileCache();
        if (null != cache) {
          cache.showCache(f);
        }
        toolsui.getDatasetViewerPanel().setText(f.toString());
        toolsui.getDatasetViewerPanel().getDetailWindow().show();
      }
    };
    BAMutil.setActionProperties(showCacheAction, null, "Show Caches", false, 'S', -1);
    BAMutil.addActionToMenu(this, showCacheAction);

    AbstractAction clearRafCacheAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
        if (rafCache != null) {
          rafCache.clearCache(true);
        }
      }
    };
    BAMutil.setActionProperties(clearRafCacheAction, null, "Clear RandomAccessFileCache", false, 'C', -1);
    BAMutil.addActionToMenu(this, clearRafCacheAction);

    AbstractAction clearCacheAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileCacheIF cache = NetcdfDatasets.getNetcdfFileCache();
        if (cache != null)
          cache.clearCache(true);
      }
    };
    BAMutil.setActionProperties(clearCacheAction, null, "Clear NetcdfDatasetCache", false, 'C', -1);
    BAMutil.addActionToMenu(this, clearCacheAction);

    AbstractAction enableCache = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state == isCacheInit) {
          return;
        }
        isCacheInit = state;
        if (isCacheInit) {
          FileCacheIF cache = NetcdfDatasets.getNetcdfFileCache();
          if (cache != null) {
            cache.enable();
          } else {
            NetcdfDatasets.initNetcdfFileCache(10, 20, 10 * 60);
          }
        } else {
          FileCacheIF cache = NetcdfDatasets.getNetcdfFileCache();
          if (cache != null) {
            cache.disable();
          }
        }
      }
    };
    BAMutil.setActionPropertiesToggle(enableCache, null, "Enable NetcdfDatasetCache", isCacheInit, 'N', -1);
    BAMutil.addActionToMenu(this, enableCache);

    AbstractAction showPropertiesAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        toolsui.getDatasetViewerPanel().setText("System Properties\n");
        Properties sysp = System.getProperties();
        Set<String> eprops = sysp.stringPropertyNames();
        ImmutableList<String> list = ImmutableList.copyOf(eprops);

        for (Object aList : list) {
          String name = (String) aList;
          String value = System.getProperty(name);
          toolsui.getDatasetViewerPanel().appendLine("  " + name + " = " + value);
        }
        toolsui.getDatasetViewerPanel().getDetailWindow().show();
      }
    };
    BAMutil.setActionProperties(showPropertiesAction, null, "System Properties", false, 'P', -1);
    BAMutil.addActionToMenu(this, showPropertiesAction);

    addPlafSubmenu();

    AbstractAction exitAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ToolsUI.exit();
      }
    };
    BAMutil.setActionProperties(exitAction, "Exit", "Exit", false, 'X', -1);
    BAMutil.addActionToMenu(this, exitAction);
  }

  private void addPlafSubmenu() {
    JMenu plafMenu = new JMenu("Look and Feel");
    plafMenu.setMnemonic('L');

    PLAF plaf = new PLAF(toolsui);
    plaf.addToMenu(plafMenu);

    add(plafMenu);
  }
}
