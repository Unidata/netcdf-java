package ucar.nc2.ui;

import thredds.inventory.*;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/** Superclass for report panels */
public abstract class ReportPanel extends JPanel {
  protected PreferencesExt prefs;
  protected TextHistoryPane reportPane;
  protected String spec;

  protected ReportPanel(PreferencesExt prefs) {
    this.prefs = prefs;
    this.reportPane = new TextHistoryPane();
    setLayout(new BorderLayout());
    add(reportPane, BorderLayout.CENTER);
  }

  public void addOptions(JPanel buttPanel) {}

  public void save() {}

  protected abstract void doReport(Formatter f, Object option, MCollection dcm, boolean useIndex, boolean eachFile,
      boolean extra) throws IOException;

  public abstract Object[] getOptions();

  public void doReport(String spec, boolean useIndex, boolean eachFile, boolean extra, Object option)
      throws IOException {
    Formatter f = new Formatter();
    f.format("%s on %s useIndex=%s eachFile=%s extra=%s%n", option, spec, useIndex, eachFile, extra);
    this.spec = spec;

    try (MCollection dcm = getCollectionUnfiltered(spec, f)) {
      if (dcm == null) {
        return;
      }

      f.format("top dir = %s%n", dcm.getRoot());
      reportPane.setText(f.toString());

      File top = new File(dcm.getRoot());
      if (!top.exists()) {
        f.format("top dir = %s does not exist%n", dcm.getRoot());
      } else {
        doReport(f, option, dcm, useIndex, eachFile, extra);
      }

      reportPane.setText(f.toString());
      reportPane.gotoTop();
    }
  }

  public boolean showCollection(String spec) {
    Formatter f = new Formatter();
    f.format("collection = %s%n", spec);
    boolean hasFiles = false;

    try (MCollection dcm = getCollectionUnfiltered(spec, f)) {
      if (dcm == null)
        return false;

      try {
        for (MFile mfile : dcm.getFilesSorted()) {
          f.format(" %s%n", mfile.getPath());
          hasFiles = true;
        }
      } catch (IOException e) {
        e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
      }

      reportPane.setText(f.toString());
      reportPane.gotoTop();
      return hasFiles;
    }
  }

  MCollection getCollection(String spec, Formatter f) {
    try {
      MCollection org = CollectionAbstract.open(spec, spec, null, f);
      return new CollectionFiltered("GribReportPanel", org, mfile -> {
        String suffix = mfile.getName();
        // if (suffix.contains(".ncx")) TODO how to control this??
        // return false;
        return !suffix.contains(".gbx");
      });

    } catch (IOException e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace(new PrintWriter(sw));
      reportPane.setText(sw.toString());
      return null;
    }
  }

  protected MCollection getCollectionUnfiltered(String spec, Formatter f) {
    try {
      return CollectionAbstract.open(spec, spec, null, f);
    } catch (IOException e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace(new PrintWriter(sw));
      reportPane.setText(sw.toString());
      return null;
    }
  }

}
