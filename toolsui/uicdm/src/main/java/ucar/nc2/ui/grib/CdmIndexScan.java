/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.grib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.ui.prefs.BeanTable;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/** Scan for Feature Datasets */
public class CdmIndexScan extends JPanel {
  private static final Logger logger = LoggerFactory.getLogger(CdmIndexScan.class);

  private final PreferencesExt prefs;

  private final BeanTable<IndexScanBean> indexBeanTable;
  private final JSplitPane split;
  private final TextHistoryPane dumpTA = new TextHistoryPane();
  private final TextHistoryPane infoTA = new TextHistoryPane();
  private final IndependentWindow infoWindow;
  private final AbstractButton filterButt;

  private Formatter scanInfo = new Formatter();
  private boolean filterSRC = true;
  private String topdir;

  public CdmIndexScan(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    indexBeanTable = new BeanTable<>(IndexScanBean.class, (PreferencesExt) prefs.node("IndexScanBeans"), false);
    indexBeanTable.addListSelectionListener(e -> setSelectedBean(indexBeanTable.getSelectedBean()));

    ucar.ui.widget.PopupMenu varPopup = new PopupMenu(indexBeanTable.getJTable(), "Options");
    varPopup.addAction("Open Index File", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        IndexScanBean ftb = indexBeanTable.getSelectedBean();
        if (ftb != null) {
          CdmIndexScan.this.firePropertyChange("openIndexFile", null, ftb.path);
        }
      }
    });

    // the info window
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, indexBeanTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);

    AbstractAction filterAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        filterSRC = (Boolean) getValue(BAMutil.STATE);
        String tooltip = filterSRC ? "dont include SRC/MRUTC" : "include SRC/MRUTC";
        filterButt.setToolTipText(tooltip);
      }
    };
    String tooltip = filterSRC ? "dont include type SRC/MRUTC" : "include SRC/MRUTC";
    BAMutil.setActionProperties(filterAction, "nj22/AddCoords", tooltip, true, 'C', -1);
    filterAction.putValue(BAMutil.STATE, filterSRC);
    filterButt = BAMutil.addActionToContainer(buttPanel, filterAction);
    buttPanel.add(filterButt);

    AbstractAction infoAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        infoTA.setText(scanInfo.toString());
        infoWindow.show();
      }
    };
    BAMutil.setActionProperties(infoAction, "Information", "scan info", false, 'I', -1);
    BAMutil.addActionToContainer(buttPanel, infoAction);

    AbstractAction doitButt = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        infoTA.setText(makeReport());
        infoWindow.show();
      }
    };
    BAMutil.setActionProperties(doitButt, "alien", "make report", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, doitButt);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    indexBeanTable.saveState(false);
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public void clear() {
    indexBeanTable.setBeans(new ArrayList<>()); // clear
  }

  public boolean setScanDirectory(String dirName) {
    clear();
    this.topdir = dirName;

    List<IndexScanBean> beans;
    try {
      beans = scan(dirName);
    } catch (IOException e) {
      e.printStackTrace();
      dumpTA.setText(e.getMessage());
      return false;
    }
    if (beans.isEmpty()) {
      dumpTA.setText("No .ncx4 files were found");
      return false;
    }

    indexBeanTable.setBeans(beans);
    return true;
  }

  private void setSelectedBean(IndexScanBean ftb) {
    if (ftb == null) {
      return;
    }
    dumpTA.setText(ftb.toString());
    dumpTA.gotoTop();
  }

  private java.util.List<IndexScanBean> scan(String topDir) throws IOException {
    List<IndexScanBean> result = new ArrayList<>();
    File topFile = new File(topDir);
    if (!topFile.exists()) {
      dumpTA.appendLine(String.format("File %s does not exist", topDir));
      return result;
    }

    Formatter info = new Formatter();
    if (topFile.isDirectory()) {
      scanDirectory(topFile, result, info);
    } else {
      Accum accum = openCdmIndex(topFile.getPath(), result, info);
      info.format("%s%n", accum);
    }
    dumpTA.setText(info.toString());
    this.scanInfo = info;

    return result;
  }

  private String makeReport() {
    Formatter result = new Formatter();
    result.format("# %s%n", this.topdir);
    for (IndexScanBean bean : indexBeanTable.getBeans()) {
      result.format("%s%n", bean.report);
    }
    return result.toString();
  }

  public void scanDirectory(File dir, List<IndexScanBean> result, Formatter info) throws IOException {
    if ((dir.getName().equals("exclude")) || (dir.getName().equals("problem"))) {
      return;
    }

    // get list of files
    File[] fila = dir.listFiles();
    if (fila == null) {
      return;
    }

    for (File f : fila) {
      if (!f.isDirectory() && f.getName().endsWith(".ncx4")) {
        Accum totalAccum = openCdmIndex(f.getPath(), result, info);
        info.format("%s%n", totalAccum);
      }
    }

    for (File f : fila) {
      if (f.isDirectory() && !f.getName().equals("exclude")) {
        scanDirectory(f, result, info);
      }
    }
  }

  public Accum openCdmIndex(String path, List<IndexScanBean> result, Formatter info) throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig();
    Accum totalAccum = new Accum();
    System.out.printf("%s%n", path);
    String relpath = path.substring(this.topdir.length());

    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(path, config, false, logger)) {
      if (gc == null) {
        info.format("Not a grib collection index file=%s%n", path);
        return totalAccum;
      }
      info.format("%nGribCollection %s%n", path);

      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        info.format(" Dataset %s%n", ds.getType());
        int groupno = 0;
        for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
          if (g.getType() == GribCollectionImmutable.Type.Best) {
            continue; // Best is deprecated.
          }
          if (filterSRC && (g.getType() == GribCollectionImmutable.Type.SRC
              || g.getType() == GribCollectionImmutable.Type.MRUTC)) {
            continue; // Show non-SRC/MRUTC
          }
          Formatter localInfo = new Formatter();
          Formatter report = new Formatter();
          localInfo.format(" %s%n", relpath);
          if (groupno == 0) {
            report.format("### %s%n", relpath);
          }
          Accum groupAccum = new Accum();
          localInfo.format("  Group %s%n", g.getDescription());
          report.format("  **%s** %sGroup %s%n", g.getType(), groupno == 0 ? "" : "***", g.getDescription());
          for (GribCollectionImmutable.VariableIndex vi : g.getVariables()) {
            localInfo.format("   %s%n", vi.toStringFrom());
            groupAccum.add(vi);
            totalAccum.add(vi);
          }
          info.format("  groupTotal %s%n", groupAccum);
          localInfo.format("   groupAccum %s%n", groupAccum);
          report.format("   %s%n", groupAccum);
          result.add(
              new IndexScanBean(path, relpath, ds, g, groupno++, groupAccum, localInfo.toString(), report.toString()));
        }
      }
    }
    return totalAccum;
  }

  public static class IndexScanBean {
    public String path;
    public String name;
    public Accum accum;
    public String info;
    public String report;
    public String time2D;
    public GribCollectionImmutable.Dataset ds;
    public int groupno;

    // no-arg constructor
    public IndexScanBean() {}

    public IndexScanBean(String path, String name, GribCollectionImmutable.Dataset ds,
        GribCollectionImmutable.GroupGC group, int groupno, Accum accum, String info, String report) {
      this.path = path;
      this.name = name;
      this.accum = accum;
      this.ds = ds;
      this.groupno = groupno;
      this.info = info;
      this.report = report;

      // Show the most complicated type of Time2D coordinate
      boolean hasTime2D = false;
      boolean hasOrthogonal = false;
      boolean hasRegular = false;
      for (Coordinate coord : group.getCoordinates()) {
        if (coord.getType() == Coordinate.Type.time2D) {
          hasTime2D = true;
          CoordinateTime2D c2d = (CoordinateTime2D) coord;
          if (c2d.isOrthogonal())
            hasOrthogonal = true;
          if (c2d.isRegular())
            hasRegular = true;
        }
      }
      time2D = hasRegular ? "reg" : hasOrthogonal ? "orth" : hasTime2D ? "time2D" : "";
    }

    public String getName() {
      return name;
    }

    public int getNrecords() {
      return accum.nrecords;
    }

    public int getNdups() {
      return accum.ndups;
    }

    public float getNdupPct() {
      return accum.nrecords == 0 ? 0 : 100.0f * accum.ndups / (float) accum.nrecords;
    }

    public int getNmissing() {
      return accum.nmissing;
    }

    public float getNmissingPct() {
      return accum.nrecords == 0 ? 0 : 100.0f * accum.nmissing / (float) accum.nrecords;
    }

    public int getRectMissing() {
      return accum.rectMiss;
    }

    public float getRectMissingPct() {
      return accum.nrecords == 0 ? 0 : 100.0f * accum.rectMiss / (float) accum.nrecords;
    }

    public String getType() {
      return ds.getType().toString();
    }

    public int getGroupNo() {
      return groupno;
    }

    public String getTime2D() {
      return time2D;
    }

    public String toString() {
      return info;
    }
  }

  public static class Accum {
    int nrecords, ndups, nmissing, rectMiss;

    void add(GribCollectionImmutable.VariableIndex v) {
      this.nrecords += v.getNrecords();
      this.ndups += v.getNdups();
      this.nmissing += v.getNmissing();
      this.rectMiss += (v.getSize() - v.getNrecords());
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("Accum{nrecords=%d, ", nrecords);
      double fn = (float) nrecords / 100.0;
      f.format("ndups=%d (%3.1f %%), ", ndups, ndups / fn);
      f.format("nmissing=%d (%3.1f %%), ", nmissing, nmissing / fn);
      f.format("rectMiss=%d (%3.1f %%)}", rectMiss, rectMiss / fn);
      return f.toString();
    }
  }
}
