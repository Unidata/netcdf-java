/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;

// LOOK can remove this, use WmoCommonCodesPanel directly.
public class WmoCCPanel extends OpPanel {
  private final WmoCommonCodesPanel codeTable;

  public WmoCCPanel(PreferencesExt p) {
    super(p, "table:", false, false);
    codeTable = new WmoCommonCodesPanel(prefs, buttPanel);
    add(codeTable, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object command) {
    return true;
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }

  @Override
  public void save() {
    codeTable.save();
    super.save();
  }
}
