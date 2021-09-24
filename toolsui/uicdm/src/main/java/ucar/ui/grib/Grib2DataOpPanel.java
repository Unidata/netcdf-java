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

public class Grib2DataOpPanel extends OpPanel {
  private final Grib2DataPanel gribTable;

  public Grib2DataOpPanel(PreferencesExt p) {
    super(p, "collection:", true, false);
    gribTable = new Grib2DataPanel(prefs);
    add(gribTable, BorderLayout.CENTER);

    gribTable.addPropertyChangeListener(e -> {
      if (e.getPropertyName().equals("openGrib2Collection")) {
        String collectionName = (String) e.getNewValue();
        ToolsUI.getToolsUI().openGrib2Collection(collectionName);
      }
    });

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
    infoButton.addActionListener(e -> {
      Formatter f = new Formatter();
      gribTable.showInfo(f);
      detailTA.setText(f.toString());
      detailTA.gotoTop();
      detailWindow.show();
    });
    buttPanel.add(infoButton);

    AbstractButton checkButton = BAMutil.makeButtcon("Information", "Check Problems", false);
    checkButton.addActionListener(e -> {
      Formatter f = new Formatter();
      gribTable.checkProblems(f);
      detailTA.setText(f.toString());
      detailTA.gotoTop();
      detailWindow.show();
    });
    buttPanel.add(checkButton);
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
    // Do nothing
  }

  @Override
  public void save() {
    gribTable.save();
    super.save();
  }
}
