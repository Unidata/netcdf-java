/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ui.OpPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;

public class CollectionSpecPanel extends OpPanel {
  private final CollectionSpecTable table;

  public CollectionSpecPanel(PreferencesExt dbPrefs) {
    super(dbPrefs, "collection spec:", true, false);
    table = new CollectionSpecTable(prefs);
    add(table, BorderLayout.CENTER);

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
    infoButton.addActionListener(e -> {
      Formatter f = new Formatter();
      try {
        table.showCollection(f);
      } catch (Exception e1) {
        StringWriter sw = new StringWriter(5000);
        e1.printStackTrace(new PrintWriter(sw));
        f.format("%s", sw.toString());
      }
      detailTA.setText(f.toString());
      detailTA.gotoTop();
      detailWindow.show();
    });
    buttPanel.add(infoButton);
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    if (command == null) {
      return false;
    }

    try {
      table.setCollection(command);
      return true;
    } catch (Exception ioe) {
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
  }
}
