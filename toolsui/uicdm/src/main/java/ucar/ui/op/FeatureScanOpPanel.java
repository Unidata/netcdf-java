/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ui.OpPanel;
import ucar.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

public class FeatureScanOpPanel extends OpPanel {
  private final FeatureScanPanel ftTable;
  final FileManager dirChooser;

  public FeatureScanOpPanel(PreferencesExt prefs, FileManager dirChooser) {
    super(prefs, "dir:", false, false);
    this.dirChooser = dirChooser;

    ftTable = new FeatureScanPanel(prefs);
    add(ftTable, BorderLayout.CENTER);

    ftTable.addPropertyChangeListener(e -> {
      if (!(e.getNewValue() instanceof String))
        return;

      String datasetName = (String) e.getNewValue();

      switch (e.getPropertyName()) {
        case "openNetcdfFile":
          ToolsUI.getToolsUI().openNetcdfFile(datasetName);
          break;
        case "openCoordSystems":
          ToolsUI.getToolsUI().openCoordSystems(datasetName);
          break;
        case "openNcML":
          ToolsUI.getToolsUI().openNcML(datasetName);
          break;
        case "openGridDataset":
        case "openNewGrid":
          ToolsUI.getToolsUI().openNewGrid(datasetName);
          break;
      }
    });

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
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    return ftTable.setScanDirectory(command);
  }

  @Override
  public void closeOpenFiles() {
    ftTable.clear();
  }

  @Override
  public void save() {
    dirChooser.save();
    ftTable.save();
    prefs.put("currDir", dirChooser.getCurrentDirectory());
    super.save();
  }
}
