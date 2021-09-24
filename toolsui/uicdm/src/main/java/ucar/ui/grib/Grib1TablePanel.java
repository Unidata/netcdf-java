/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grib;

import ucar.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;

public class Grib1TablePanel extends OpPanel {
  private final Grib1TablesViewer codeTable;

  public Grib1TablePanel(PreferencesExt p) {
    super(p, "table:", true, false);
    codeTable = new Grib1TablesViewer(prefs, buttPanel);
    add(codeTable, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object command) {
    codeTable.setTable((String) command);
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
