/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ui.OpPanel;
import ucar.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public class WmsPanel extends OpPanel {
  private final WmsViewer wmsViewer;
  private final JComboBox<String> types;

  public WmsPanel(PreferencesExt dbPrefs) {
    super(dbPrefs, "dataset:", true, false);
    wmsViewer = new WmsViewer(dbPrefs, ToolsUI.getToolsFrame());
    add(wmsViewer, BorderLayout.CENTER);

    buttPanel.add(new JLabel("version:"));
    types = new JComboBox<>();
    types.addItem("1.3.0");
    types.addItem("1.1.1");
    types.addItem("1.0.0");
    buttPanel.add(types);

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
    infoButton.addActionListener(e -> {
      detailTA.setText(wmsViewer.getDetailInfo());
      detailTA.gotoTop();
      detailWindow.show();
    });
    buttPanel.add(infoButton);
  }

  @Override
  public boolean process(Object o) {
    String location = (String) o;
    return wmsViewer.setDataset((String) types.getSelectedItem(), location);
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }

  @Override
  public void save() {
    super.save();
    wmsViewer.save();
  }
}
