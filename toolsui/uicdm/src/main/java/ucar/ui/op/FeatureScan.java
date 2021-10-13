/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.op;

import com.google.common.collect.Iterables;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.geoloc.vertical.VerticalTransform;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.internal.grid.GridNetcdfDataset;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Scan a directory, try to open files in various ways. */
public class FeatureScan {
  private final String top;
  private final boolean subdirs;

  public FeatureScan(String top, boolean subdirs) {
    this.top = top;
    this.subdirs = subdirs;
  }

  public List<Bean> scan(Formatter errlog) {
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

  private void scanDirectory(File dir, List<Bean> result) {
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
    Formatter gribType = new Formatter();
    StringBuilder info = new StringBuilder();
    Throwable problem;
    String newGridFn = null;
    Set<String> ctNames = new HashSet<>();
    Set<String> vctNames = new HashSet<>();

    // no-arg constructor
    public Bean() {}

    public Bean(File f) {
      this.f = f;

      if (debug)
        System.out.printf(" featureScan=%s%n", f.getPath());
      try (NetcdfDataset ds = NetcdfDatasets.openDataset(f.getPath())) {
        fileType = ds.getFileTypeId();

        Formatter errlog = new Formatter();
        findGribType(ds.getRootGroup(), gribType);

        try {
          // new
          errlog = new Formatter();
          Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
          if (grido.isPresent()) {
            GridNetcdfDataset gridDataset = grido.get();
            if (!Iterables.isEmpty(gridDataset.getGrids())) {
              for (GridCoordinateSystem gcs : gridDataset.getGridCoordinateSystems()) {
                VerticalTransform vt = gcs.getVerticalTransform();
                if (vt != null) {
                  vctNames.add((vt.getName()));
                }
              }
              newGridFn = gridDataset.getGridCoordinateSystems().get(0).showFnSummary();
            }
          }
          info.append("\nGridNetcdfDataset errlog = ");
          info.append(errlog);
          info.append("\n\n");

        } catch (Throwable t) {
          info.append(errlog);
          problem = t;
        }

      } catch (Throwable t) {
        fileType = " ERR: " + t.getMessage();
        problem = t;
      }
    }

    private void findGribType(Group group, Formatter f) {
      Attribute att = group.findAttribute("GribCollectionType");
      if (att != null) {
        f.format("%s ", att.getStringValue());
      }
      for (Group nested : group.getGroups()) {
        findGribType(nested, f);
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

    public String getNewGrid() {
      return newGridFn;
    }

    public String getGridVTs() {
      return String.join(",", vctNames);
    }

    public String getGribType() {
      return gribType.toString();
    }

    public void toString(Formatter f, boolean showInfo) {
      f.format("%s%n %s%n%n", getName(), getFileType());
      if (newGridFn != null) {
        f.format("GridCoordinateSystem %s%n", newGridFn);
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
    List<Bean> beans = scanner.scan(new Formatter());
    for (Bean b : beans)
      System.out.printf(" %-60s %-20s %n", b.getName(), b.getFileType());
  }
}
