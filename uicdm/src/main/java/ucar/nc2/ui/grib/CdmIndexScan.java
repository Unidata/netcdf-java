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
  private final TextHistoryPane dumpTA;
  private final IndependentWindow infoWindow;
  private boolean subdirs = true;

  public CdmIndexScan(PreferencesExt prefs) {
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
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    dumpTA = new TextHistoryPane();
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, indexBeanTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
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

  public java.util.List<IndexScanBean> scan(String topDir) throws IOException {
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
      Accum accum = new Accum();
      openCdmIndex(topFile.getPath(), accum, result, info);
      info.format("%n%s%n", accum);
    }
    dumpTA.setText(info.toString());

    return result;
  }

  private void scanDirectory(File dir, List<IndexScanBean> result, Formatter info) throws IOException {
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
        Accum totalAccum = new Accum();
        openCdmIndex(f.getPath(), totalAccum, result, info);
        info.format("%n%s%n", totalAccum);
      }
    }

    if (subdirs) {
      for (File f : fila) {
        if (f.isDirectory() && !f.getName().equals("exclude")) {
          scanDirectory(f, result, info);
        }
      }
    }
  }

  private void openCdmIndex(String path, Accum accum, List<IndexScanBean> result, Formatter info) throws IOException {

    info.format("%nFile %s%n", path);
    FeatureCollectionConfig config = new FeatureCollectionConfig();

    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(path, config, false, logger)) {
      if (gc == null) {
        info.format("Not a grib collection index file=%s%n" + path);
        return;
      }

      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        info.format("%nDataset %s%n", ds.getType());
        int groupno = 0;
        for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
          if (g.getType() == GribCollectionImmutable.Type.Best) {
            continue; // Best is deprecated.
          }
          Formatter localInfo = new Formatter();
          localInfo.format("File %s%n", path);
          Accum groupAccum = new Accum();
          localInfo.format(" Group %s%n", g.getDescription());
          for (GribCollectionImmutable.VariableIndex v : g.getVariables()) {
            localInfo.format("  %s%n", v.toStringFrom());
            showIfNonZero(info, v, path);

            groupAccum.add(v);
            accum.add(v);
          }
          info.format(" total %s%n", groupAccum);
          localInfo.format(" total %s%n", groupAccum);
          result.add(new IndexScanBean(path, ds, g, groupno++, groupAccum, localInfo.toString()));
        }
      }

    }
  }

  String lastFilename;

  void showIfNonZero(Formatter info, GribCollectionImmutable.VariableIndex v, String filename) {
    if ((v.getNdups() != 0) || (v.getNmissing() != 0)) {
      if (!filename.equals(lastFilename))
        info.format(" %s%n  %s%n", filename, v.toStringFrom());
      else
        info.format("  %s%n", v.toStringFrom());
      lastFilename = filename;
    }
  }

  private static class Accum {
    int nrecords, ndups, nmissing;

    Accum add(GribCollectionImmutable.VariableIndex v) {
      this.nrecords += v.getNrecords();
      this.ndups += v.getNdups();
      this.nmissing += (v.getSize() - v.getNrecords());
      return this;
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("Accum{nrecords=%d, ", nrecords);
      double fn = (float) nrecords / 100.0;
      f.format("ndups=%d (%3.1f %%), ", ndups, ndups / fn);
      f.format("nmissing=%d (%3.1f %%)}", nmissing, nmissing / fn);
      return f.toString();
    }
  }

  public static class IndexScanBean {
    public String path;
    public String name;
    public Accum accum;
    public String info;
    public String time2D;
    public GribCollectionImmutable.Dataset ds;
    public int groupno;

    // no-arg constructor
    public IndexScanBean() {}

    public IndexScanBean(String path, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group,
        int groupno, Accum accum, String info) {
      this.path = path;
      int pos = path.lastIndexOf('/');
      this.name = ".." + path.substring(pos);
      this.accum = accum;
      this.ds = ds;
      this.groupno = groupno;
      this.info = info;

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

    public float getNmissPct() {
      return accum.nrecords == 0 ? 0 : 100.0f * accum.nmissing / (float) accum.nrecords;
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
}
