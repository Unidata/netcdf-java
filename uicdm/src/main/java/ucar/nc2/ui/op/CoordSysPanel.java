/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.*;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JOptionPane;

public class CoordSysPanel extends OpPanel {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private NetcdfDataset ds;
  private final CoordSysTable coordSysTable;

  public CoordSysPanel(PreferencesExt p) {
    super(p, "dataset:", true, false);
    coordSysTable = new CoordSysTable(prefs, buttPanel);
    add(coordSysTable, BorderLayout.CENTER);

    AbstractButton summaryButton = BAMutil.makeButtcon("Information", "Summary Info", false);
    summaryButton.addActionListener(e -> {
      Formatter f = new Formatter();
      coordSysTable.summaryInfo(f);
      detailTA.setText(f.toString());
      detailWindow.show();
    });
    buttPanel.add(summaryButton);

    JButton dsButton = new JButton("Object dump");
    dsButton.addActionListener(e -> {
      if (ds != null) {
        Formatter f = new Formatter();
        debugDump(ds, f);
        detailTA.setText(f.toString());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(dsButton);
  }

  private void debugDump(NetcdfDataset ncd, Formatter f) {
    ncd.getDetailInfo(f);
    dumpClasses(ncd.getRootGroup(), f);
  }

  private void dumpClasses(Group g, Formatter out) {
    out.format("Dimensions:%n");
    for (Dimension ds : g.getDimensions()) {
      out.format("  %s %s%n", ds.getShortName(), ds.getClass().getName());
    }

    out.format("Attributes:%n");
    for (Attribute a : g.attributes()) {
      out.format("  " + a.getShortName() + " " + a.getClass().getName());
    }

    out.format("Variables:%n");
    dumpVariables(g.getVariables(), out);

    out.format("Groups:%n");
    for (Group nested : g.getGroups()) {
      out.format("  %s %s%n", nested.getFullName(), nested.getClass().getName());
      dumpClasses(nested, out);
    }
  }

  private void dumpVariables(List<Variable> vars, Formatter out) {
    for (Variable v : vars) {
      out.format("  %s %s", v.getFullName(), v.getClass().getName()); // +" "+Integer.toHexString(v.hashCode()));
      if (v instanceof CoordinateAxis) {
        out.format("  %s", ((CoordinateAxis) v).getAxisType());
      }
      out.format("%n");
      if (v instanceof Structure) {
        dumpVariables(((Structure) v).getVariables(), out);
      }
    }
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    boolean err = false;

    // close previous file
    try {
      if (ds != null) {
        ds.close();
      }
    } catch (IOException ioe) {
      logger.warn("close failed");
    }

    try {
      ds = NetcdfDatasets.openDataset(command, true, null);
      if (ds == null) {
        JOptionPane.showMessageDialog(null, "Failed to open <" + command + ">");
      } else {
        coordSysTable.setDataset(ds);
      }
    } catch (FileNotFoundException ioe) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset cannot open " + command + "\n" + ioe.getMessage());
      err = true;
    } catch (Exception e) {
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailWindow.show();
      err = true;
      e.printStackTrace();
    }

    return !err;
  }

  @Override
  public void closeOpenFiles() throws IOException {
    if (ds != null) {
      ds.close();
    }
    ds = null;
    coordSysTable.clear();
  }

  public void setDataset(NetcdfDataset ncd) {
    try {
      if (ds != null)
        ds.close();
      ds = null;
    } catch (IOException ioe) {
      logger.warn("close failed");
    }
    ds = ncd;

    coordSysTable.setDataset(ds);
    setSelectedItem(ds.getLocation());
  }

  @Override
  public void save() {
    coordSysTable.save();
    super.save();
  }
}
