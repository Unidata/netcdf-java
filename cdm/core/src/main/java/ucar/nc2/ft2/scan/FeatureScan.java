/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.scan;

import com.google.common.collect.Iterables;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCS;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCSBuilder;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.internal.grid.GridDatasetImpl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Scan a directory, try to open files in various ways.
 * Can be used as a standalone program, which is why its here and not in uicdm module.
 */
public class FeatureScan {
  private String top;
  private boolean subdirs;

  public FeatureScan(String top, boolean subdirs) {
    this.top = top;
    this.subdirs = subdirs;
  }

  public java.util.List<FeatureScan.Bean> scan(Formatter errlog) {
    List<Bean> result = new ArrayList<>();
    File topFile = new File(top);
    if (!topFile.exists()) {
      errlog.format("File %s does not exist", top);
      return result;
    }

    if (topFile.isDirectory())
      scanDirectory(topFile, result);
    else {
      Bean fdb = new Bean(topFile);
      result.add(fdb);
    }
    return result;
  }

  private void scanDirectory(File dir, java.util.List<FeatureScan.Bean> result) {
    if ((dir.getName().equals("exclude")) || (dir.getName().equals("problem"))) {
      return;
    }

    // get list of files
    File[] fila = dir.listFiles();
    if (fila == null) {
      return;
    }

    List<File> files = new ArrayList<>();
    for (File f : fila) {
      if (!f.isDirectory()) {
        files.add(f);
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
        if (name.startsWith(".nfs") || name.contains(".gbx") || name.endsWith(".xml") || name.endsWith(".pdf")
            || name.endsWith(".txt") || name.endsWith(".tar") || name.endsWith(".tmp") || name.endsWith(".dump")
            || name.endsWith(".gitignore")) {
          files2.remove(f);

        } else if (prev != null) {
          if (name.endsWith(".ncml")) {
            if (prev.getName().equals(stem) || prev.getName().equals(stem + ".nc"))
              files2.remove(prev);
          } else if (name.endsWith(".bz2")) {
            if (prev.getName().equals(stem))
              files2.remove(f);
          } else if (name.endsWith(".gz")) {
            if (prev.getName().equals(stem))
              files2.remove(f);
          } else if (name.endsWith(".gzip")) {
            if (prev.getName().equals(stem))
              files2.remove(f);
          } else if (name.endsWith(".zip")) {
            if (prev.getName().equals(stem))
              files2.remove(f);
          } else if (name.endsWith(".Z")) {
            if (prev.getName().equals(stem))
              files2.remove(f);
          }
        }
        prev = f;
      }

      // do the remaining
      for (File f : files2) {
        result.add(new Bean(f));
      }
    }

    // do subdirs
    if (subdirs) {
      for (File f : fila) {
        if (f.isDirectory() && !f.getName().equals("exclude"))
          scanDirectory(f, result);
      }
    }

  }

  private String stem(String name) {
    int pos = name.lastIndexOf('.');
    return (pos > 0) ? name.substring(0, pos) : name;
  }

  private static final boolean debug = true;

  public static class Bean {
    public File f;
    String fileType;
    String coordMap;
    FeatureType featureType, ftFromMetadata;
    String ftype;
    StringBuilder info = new StringBuilder();
    String ftImpl;
    Throwable problem;
    DtCoverageCSBuilder builder; // LOOK replace with CoverageDataset
    GridCoordinateSystem gridCoordinateSystem;

    // no-arg constructor
    public Bean() {}

    public Bean(File f) {
      this.f = f;

      if (debug)
        System.out.printf(" featureScan=%s%n", f.getPath());
      try (NetcdfDataset ds = NetcdfDatasets.openDataset(f.getPath())) {
        fileType = ds.getFileTypeId();

        Formatter errlog = new Formatter();
        builder = DtCoverageCSBuilder.classify(ds, errlog);
        info.append(errlog);
        setCoordMap();

        ftFromMetadata = FeatureDatasetFactoryManager.findFeatureType(ds);

        try {
          // new
          errlog = new Formatter();
          Optional<GridDatasetImpl> grido = GridDatasetImpl.create(ds, errlog);
          if (grido.isPresent()) {
            GridDatasetImpl gridDataset = grido.get();
            if (!Iterables.isEmpty(gridDataset.getGrids())) {
              gridCoordinateSystem = gridDataset.getCoordSystems().get(0);
            }
          }
          info.append("\nGridDatasetImpl errlog = ");
          info.append(errlog);
          info.append("\n\n");

          // old
          errlog = new Formatter();
          FeatureDataset featureDataset = FeatureDatasetFactoryManager.wrap(null, ds, null, errlog);
          info.append("FeatureDatasetFactoryManager errlog = ");
          info.append(errlog);
          info.append("\n\n");

          if (featureDataset != null) {
            featureType = featureDataset.getFeatureType();
            if (featureType != null)
              ftype = featureType.toString();
            ftImpl = featureDataset.getImplementationName();
            Formatter infof = new Formatter();
            featureDataset.getDetailInfo(infof);
            // info.append(infof);
          } else {
            ftype = "";
          }

        } catch (Throwable t) {
          ftype = " ERR: " + t.getMessage();
          info.append(errlog);
          problem = t;
        }

      } catch (Throwable t) {
        fileType = " ERR: " + t.getMessage();
        problem = t;
      }
    }


    public String getName() {
      String path = f.getPath();
      if (path.contains("/cdmUnitTest/")) {
        int pos = path.indexOf("/cdmUnitTest/");
        path = ".." + path.substring(pos);
      }
      if (path.contains("/core/src/test/data/")) {
        int pos = path.indexOf("/core/src/test/data/");
        path = ".." + path.substring(pos);
      }
      return path;
    }

    public String getFileType() {
      return fileType;
    }

    public double getSizeM() {
      return f.length() / 1000.0 / 1000.0;
    }

    public String getCoordMap() {
      return coordMap;
    }

    public String getNewGrid() {
      return gridCoordinateSystem == null ? "" : gridCoordinateSystem.showFnSummary();
    }

    public void setCoordMap() {
      if (builder == null)
        return;
      DtCoverageCS cs = builder.makeCoordSys();
      if (cs == null || cs.getCoverageType() == null)
        return;
      coordMap = "f:D(" + cs.getDomainRank() + ")->R(" + cs.getRangeRank() + ")";
    }

    public String getFtMetadata() {
      return (ftFromMetadata == null) ? "" : ftFromMetadata.toString();
    }

    public String getFeatureType() {
      return ftype;
    }

    /*
     * public String getFeatureImpl() {
     * return ftImpl;
     * }
     */

    public String getCoverage() {
      return builder == null ? "" : builder.showSummary();
    }

    public void toString(Formatter f, boolean showInfo) {
      f.format("%s%n %s%n map = '%s'%n", getName(), getFileType(), getCoordMap());
      if (gridCoordinateSystem != null) {
        f.format("GridCoordinateSystem %s%n", gridCoordinateSystem.showFnSummary());
      }

      if (builder != null) {
        f.format("%nDtCoverageCSBuilder %s%n", builder.toString());
      }

      if (showInfo && info != null) {
        f.format("%n%s", info);
      }

      if (problem != null) {
        StringWriter sw = new StringWriter(5000);
        problem.printStackTrace(new PrintWriter(sw));
        f.format(sw.toString());
      }
    }

    public String toString() {
      Formatter f = new Formatter();
      toString(f, true);
      return f.toString();
    }

    public String runClassifier() {
      Formatter ff = new Formatter();
      String type = null;
      try (NetcdfDataset ds = NetcdfDatasets.openDataset(f.getPath())) {
        type = DtCoverageCSBuilder.describe(ds, ff);

      } catch (IOException e) {
        StringWriter sw = new StringWriter(10000);
        e.printStackTrace(new PrintWriter(sw));
        ff.format("%n%s", sw.toString());
      }
      ff.format("CoverageCS.Type = %s", type);
      return ff.toString();
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  public static void main(String[] arg) {
    String usage = "usage: ucar.nc2.ft.scan.FeatureScan directory [-subdirs]";
    if (arg.length < 1) {
      System.out.println(usage);
      System.exit(0);
    }

    boolean subdirs = false;

    for (int i = 1; i < arg.length; i++) {
      String s = arg[i];
      if (s.equalsIgnoreCase("-subdirs"))
        subdirs = true;
    }

    FeatureScan scanner = new FeatureScan(arg[0], subdirs);

    System.out.printf(" %-60s %-20s %-10s %-10s%n", "name", "fileType", "featureType", "featureImpl");
    List<FeatureScan.Bean> beans = scanner.scan(new Formatter());
    for (Bean b : beans)
      System.out.printf(" %-60s %-20s %n", b.getName(), b.getFileType());
  }
}
