/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grib;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.util.StringUtil2;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Rewrite GRIB files - track stats etc
 *
 * @author caron
 * @since 8/11/2014
 */
public class GribRewritePanel extends JPanel {
  private static final Logger logger = LoggerFactory.getLogger(GribRewritePanel.class);

  private final PreferencesExt prefs;

  private final BeanTable<FileBean> ftTable;
  private final JSplitPane split;
  private final TextHistoryPane dumpTA;
  private final IndependentWindow infoWindow;

  public GribRewritePanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;
    ftTable = new BeanTable<>(FileBean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);

    AbstractAction calcAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        calcAverage();
      }
    };
    BAMutil.setActionProperties(calcAction, "nj22/Dataset", "calc storage", false, 'C', -1);
    BAMutil.addActionToContainer(buttPanel, calcAction);
    PopupMenu varPopup = new PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        GribRewritePanel.this.firePropertyChange("openNetcdfFile", null, ftb.getPath());
      }
    });
    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        GribRewritePanel.this.firePropertyChange("openGridDataset", null, ftb.getPath());
      }
    });
    varPopup.addAction("Open in Grib2Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        GribRewritePanel.this.firePropertyChange("openGrib2Data", null, ftb.getPath());
      }
    });
    varPopup.addAction("Open in Grib1Data", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FileBean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        GribRewritePanel.this.firePropertyChange("openGrib1Data", null, ftb.getPath());
      }
    });

    /*
     * varPopup.addAction("Show Report on selected rows", new AbstractAction() {
     * public void actionPerformed(ActionEvent e) {
     * List<FileBean> selected = ftTable.getSelectedBeans();
     * Formatter f = new Formatter();
     * for (FileBean bean : selected) {
     * bean.toString(f, false);
     * }
     * dumpTA.setText(f.toString());
     * }
     * });
     * 
     * varPopup.addAction("Run Coverage Classifier", new AbstractAction() {
     * public void actionPerformed(ActionEvent e) {
     * FileBean ftb = (FileBean) ftTable.getSelectedBean();
     * if (ftb == null) return;
     * dumpTA.setText(ftb.runClassifier());
     * }
     * });
     */

    // the info window
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    dumpTA = new TextHistoryPane();
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ftTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    ftTable.saveState(false);
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public void clear() {
    ftTable.setBeans(new ArrayList<>()); // clear
  }

  public boolean setScanDirectory(String dirName) {
    clear();

    Formatter errlog = new Formatter();
    List<FileBean> beans = scan(dirName, errlog);
    if (beans.isEmpty()) {
      dumpTA.setText(errlog.toString());
      return false;
    }

    ftTable.setBeans(beans);
    return true;
  }

  private void setSelectedFeatureDataset(FileBean ftb) {
    dumpTA.setText(ftb.toString());
    dumpTA.gotoTop();
  }

  private void calcAverage() {
    Formatter f = new Formatter();
    calcAverage(null, f);
    calcAverage("grib1", f);
    calcAverage("grib2", f);

    dumpTA.setText(f.toString());
    dumpTA.gotoTop();
  }

  private void calcAverage(String what, Formatter f) {

    double totalRecords = 0.0;
    List<FileBean> beans = ftTable.getBeans();
    for (FileBean bean : beans) {
      if (bean.getNdups() > 0)
        continue;
      if (what != null && (!bean.getPath().contains(what)))
        continue;
      totalRecords += bean.getNrecords();
    }

    if (what != null)
      f.format("%n%s%n", what);
    f.format("Total # grib records = %f%n", totalRecords);
  }

  ///////////////

  public java.util.List<FileBean> scan(String top, Formatter errlog) {

    List<FileBean> result = new ArrayList<>();

    File topFile = new File(top);
    if (!topFile.exists()) {
      errlog.format("File %s does not exist", top);
      return result;
    }

    if (topFile.isDirectory())
      scanDirectory(topFile, false, result, errlog);
    else {
      FileBean fdb = null;
      try {
        fdb = new FileBean(topFile);
      } catch (IOException e) {
        System.out.printf("FAIL, skip %s%n", topFile.getPath());
      }
      result.add(fdb);
    }

    return result;
  }

  public static class FileFilterFromSuffixes implements FileFilter {
    String[] suffixes;

    public FileFilterFromSuffixes(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s : suffixes)
        if (file.getPath().endsWith(s))
          return true;
      return false;
    }
  }


  private void scanDirectory(File dir, boolean subdirs, java.util.List<FileBean> result, Formatter errlog) {
    if ((dir.getName().equals("exclude")) || (dir.getName().equals("problem")))
      return;

    File[] gribFilesInDir = dir.listFiles(new FileFilterFromSuffixes("grib1 grib2"));
    if (gribFilesInDir == null) {
      // File.listFiles() returns null instead of throwing an exception. Dumb.
      throw new RuntimeException(String.format("Either an I/O error occurred, or \"%s\" is not a directory.", dir));
    }

    List<File> files = new ArrayList<>();
    for (File gribFile : gribFilesInDir) {
      if (!gribFile.isDirectory()) {
        files.add(gribFile);
      }
    }

    // eliminate redundant files
    // ".Z", ".zip", ".gzip", ".gz", or ".bz2"
    if (!files.isEmpty()) {
      Collections.sort(files);
      List<File> files2 = new ArrayList<>(files);

      File prev = null;
      for (File f : files) {
        String name = f.getName();
        String stem = stem(name);
        if (prev != null) {
          if (name.endsWith(".ncml")) {
            if (prev.getName().equals(stem) || prev.getName().equals(stem + ".grib2"))
              files2.remove(prev);
          }
        }
        prev = f;
      }

      // do the remaining
      for (File f : files2) {
        try {
          result.add(new FileBean(f));
        } catch (IOException e) {
          System.out.printf("FAIL, skip %s%n", f.getPath());
        }
      }
    }

    // do subdirs
    if (subdirs) {
      for (File f : dir.listFiles()) {
        if (f.isDirectory() && !f.getName().equals("exclude"))
          scanDirectory(f, subdirs, result, errlog);
      }
    }

  }

  private String stem(String name) {
    int pos = name.lastIndexOf('.');
    return (pos > 0) ? name.substring(0, pos) : name;
  }

  private static final boolean debug = true;

  public static class FileBean {
    private File f;
    String fileType;
    int nrecords, nvars, ndups;

    // no-arg constructor
    public FileBean() {}

    public FileBean(File f) throws IOException {
      this.f = f;

      if (debug)
        System.out.printf(" fileScan=%s%n", getPath());
      try (NetcdfDataset ncd = NetcdfDatasets.openDataset(getPath())) {
        fileType = ncd.getFileTypeId();
        countGribData2D(f);
      }
    }

    public String getPath() {
      String p = f.getPath();
      return StringUtil2.replace(p, "\\", "/");
    }

    public String getFileType() {
      return fileType;
    }

    public double getGribSizeM() {
      return ((double) f.length()) / 1000 / 1000;
    }

    public int getNrecords() {
      return nrecords;
    }

    public int getNvars() {
      return nvars;
    }

    public int getNdups() {
      return ndups;
    }

    public void countGribData2D(File f) throws IOException {
      String indexFilename = f.getPath() + GribCdmIndex.NCX_SUFFIX;

      FeatureCollectionConfig config = new FeatureCollectionConfig();

      try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(indexFilename, config, false, logger)) {
        if (gc == null)
          throw new IOException("Not a grib collection index file");

        for (GribCollectionImmutable.Dataset ds : gc.getDatasets())
          for (GribCollectionImmutable.GroupGC group : ds.getGroups())
            for (GribCollectionImmutable.VariableIndex vi : group.getVariables()) {
              nvars++;
            }
      }
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("f", f).add("fileType", fileType).add("nrecords", nrecords)
          .add("nvars", nvars).add("ndups", ndups).toString();
    }
  }
}
