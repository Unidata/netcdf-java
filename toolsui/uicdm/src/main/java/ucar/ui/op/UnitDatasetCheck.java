/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.ui.OpPanel;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.units.SimpleUnit;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;

public class UnitDatasetCheck extends OpPanel {
  private final TextHistoryPane ta;

  UnitDatasetCheck(PreferencesExt p) {
    super(p, "dataset:");
    ta = new TextHistoryPane(true);
    add(ta, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    boolean err = false;

    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(command, useCoords, null)) {
      ta.setText("Variables for " + command + ":");
      for (Variable vs : ncfile.getVariables()) {
        String units = vs.getUnitsString();
        StringBuilder sb = new StringBuilder();
        sb.append("   ").append(vs.getShortName()).append(" has unit= <").append(units).append(">");
        if (units != null) {
          try {
            SimpleUnit su = SimpleUnit.factoryWithExceptions(units);
            sb.append(" unit convert = ").append(su);
            if (su.isUnknownUnit()) {
              sb.append(" UNKNOWN UNIT");
            }
          } catch (Exception ioe) {
            sb.append(" unit convert failed ");
            sb.insert(0, "**** Fail ");
          }
        }

        ta.appendLine(sb.toString());
      }
    } catch (FileNotFoundException ioe) {
      ta.setText("Failed to open <" + command + ">");
      err = true;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      err = true;
    }

    return !err;
  }

  @Override
  public void closeOpenFiles() throws IOException {
    ta.clear();
  }
}
