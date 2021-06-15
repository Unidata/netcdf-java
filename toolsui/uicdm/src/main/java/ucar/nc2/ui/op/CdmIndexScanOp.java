/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.nc2.ui.grib.CdmIndexScan;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CdmIndexScanOp extends OpPanel {
  private final CdmIndexScan cdmIndexScan;
  final FileManager dirChooser;

  public CdmIndexScanOp(PreferencesExt prefs, FileManager dirChooser) {
    super(prefs, "dir:", false, false);
    this.dirChooser = dirChooser;

    cdmIndexScan = new CdmIndexScan(prefs, buttPanel);
    add(cdmIndexScan, BorderLayout.CENTER);

    dirChooser.getFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    dirChooser.setCurrentDirectory(prefs.get("currDir", "."));
    AbstractAction fileAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String filename = dirChooser.chooseFilename();
        if (filename == null) {
          return;
        }
        cb.setSelectedItem(filename);
      }
    };
    BAMutil.setActionProperties(fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
    BAMutil.addActionToContainer(buttPanel, fileAction);

    cdmIndexScan.addPropertyChangeListener(e -> {
      if (!(e.getNewValue() instanceof String))
        return;

      String datasetName = (String) e.getNewValue();

      if ("openIndexFile".equals(e.getPropertyName())) {
        ToolsUI.getToolsUI().openIndexFile(datasetName);
      }
    });
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    return cdmIndexScan.setScanDirectory(command);
  }

  @Override
  public void closeOpenFiles() {
    cdmIndexScan.clear();
  }

  @Override
  public void save() {
    dirChooser.save();
    cdmIndexScan.save();
    prefs.put("currDir", dirChooser.getCurrentDirectory());
    super.save();
  }
}
