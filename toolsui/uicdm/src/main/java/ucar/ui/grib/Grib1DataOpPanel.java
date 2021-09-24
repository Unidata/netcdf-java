/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grib;

import ucar.ui.OpPanel;
import ucar.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

public class Grib1DataOpPanel extends OpPanel {
  private final Grib1DataTable gribTable;

  public Grib1DataOpPanel(PreferencesExt p) {
    super(p, "collection:", true, false);
    gribTable = new Grib1DataTable(prefs);
    add(gribTable, BorderLayout.CENTER);

    gribTable.addPropertyChangeListener(e -> {
      if (e.getPropertyName().equals("openGrib1Collection")) {
        String collectionName = (String) e.getNewValue();
        ToolsUI.getToolsUI().openGrib1Collection(collectionName);
      }
    });

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Check Problems", false);
    infoButton.addActionListener(e -> {
      Formatter f = new Formatter();
      gribTable.checkProblems(f);
      detailTA.setText(f.toString());
      detailTA.gotoTop();
      detailWindow.show();
    });
    buttPanel.add(infoButton);
  }

  public void setCollection(String collection) {
    if (process(collection)) {
      cb.addItem(collection);
    }
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    boolean err = false;

    try {
      gribTable.setCollection(command);
    } catch (FileNotFoundException ioe) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset cannot open " + command + "\n" + ioe.getMessage());
      err = true;
    } catch (Exception e) {
      e.printStackTrace();
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailWindow.show();
      err = true;
    }

    return !err;
  }

  @Override
  public void closeOpenFiles() throws IOException {
    // Nothing to do here.
  }

  @Override
  public void save() {
    gribTable.save();
    super.save();
  }
}
