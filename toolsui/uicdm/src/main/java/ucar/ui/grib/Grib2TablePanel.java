/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grib;

import ucar.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;

public class Grib2TablePanel extends OpPanel {
  private final Grib2TableViewer2 codeTable;

  public Grib2TablePanel(PreferencesExt p) {
    super(p, "table:", false, false);
    codeTable = new Grib2TableViewer2(prefs, buttPanel);
    add(codeTable, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object command) {
    return true;
  }

  @Override
  public void save() {
    codeTable.save();
    super.save();
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }
}
