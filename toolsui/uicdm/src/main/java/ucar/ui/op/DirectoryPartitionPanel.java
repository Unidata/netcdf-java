/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ui.OpPanel;
import ucar.ui.ToolsUI;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DirectoryPartitionPanel extends OpPanel {
  private final DirectoryPartitionViewer table;

  public DirectoryPartitionPanel(PreferencesExt dbPrefs) {
    super(dbPrefs, "collection:", false, false, false);
    table = new DirectoryPartitionViewer(prefs, topPanel, buttPanel);
    add(table, BorderLayout.CENTER);

    table.addPropertyChangeListener(e -> {
      if (e.getPropertyName().equals("openGrib2Collection")) {
        String collectionName = (String) e.getNewValue();
        ToolsUI.getToolsUI().openGrib2Collection(collectionName);
      }
    });
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    if (command == null) {
      return false;
    }

    try {
      // table.setCollectionFromConfig(command);
      return true;
    } catch (Exception ioe) {
      ioe.printStackTrace();
      StringWriter sw = new StringWriter(5000);
      ioe.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailTA.gotoTop();
      detailWindow.show();
    }

    return false;
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }

  @Override
  public void save() {
    table.save();
    super.save();
    table.clear();
  }
}
