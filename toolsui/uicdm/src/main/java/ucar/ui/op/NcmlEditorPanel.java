/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.IOException;

public class NcmlEditorPanel extends OpPanel {
  private final NcmlEditor editor;

  public NcmlEditorPanel(PreferencesExt p) {
    super(p, "dataset:", true, false);
    editor = new NcmlEditor(buttPanel, prefs);
    add(editor, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object o) {
    return editor.setNcml((String) o);
  }

  @Override
  public void closeOpenFiles() throws IOException {
    editor.closeOpenFiles();
  }

  @Override
  public void save() {
    super.save();
    editor.save();
  }
}
