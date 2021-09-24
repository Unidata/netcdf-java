/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grib;

import ucar.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

public class GribIndexOpPanel extends OpPanel {
  private final GribIndexPanel gribTable;

  public GribIndexOpPanel(PreferencesExt p) {
    super(p, "index file:", true, false);
    gribTable = new GribIndexPanel(prefs, buttPanel);
    add(gribTable, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    boolean err = false;

    try {
      gribTable.setIndexFile(command);
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
    gribTable.closeOpenFiles();
  }

  @Override
  public void save() {
    gribTable.save();
    super.save();
  }
}
