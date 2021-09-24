/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grib;

import ucar.nc2.grib.grib2.table.WmoTemplateTables;
import ucar.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import javax.swing.JComboBox;

public class GribTemplatePanel extends OpPanel {
  private GribWmoTemplatesPanel codeTable;

  public GribTemplatePanel(PreferencesExt p) {
    super(p, "table:", false, false, false);

    JComboBox<WmoTemplateTables.Version> modes = new JComboBox<>(WmoTemplateTables.Version.values());
    modes.setSelectedItem(WmoTemplateTables.standard);
    topPanel.add(modes, BorderLayout.CENTER);
    modes.addActionListener(e -> codeTable.setTable((WmoTemplateTables.Version) modes.getSelectedItem()));

    codeTable = new GribWmoTemplatesPanel(prefs, buttPanel);
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
