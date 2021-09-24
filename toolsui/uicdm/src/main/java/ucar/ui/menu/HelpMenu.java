/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.menu;

import ucar.ui.ToolsAboutWindow;
import ucar.ui.ToolsSplashScreen;
import ucar.ui.ToolsUI;
import ucar.ui.widget.BAMutil;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;

public class HelpMenu extends JMenu {

  private final ToolsUI toolsui;
  private ToolsAboutWindow aboutWindow;

  public HelpMenu(ToolsUI tui) {
    super("Help");
    setMnemonic('H');

    this.toolsui = tui;

    AbstractAction aboutAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        if (aboutWindow == null) {
          JFrame parentFrame = (JFrame) toolsui.getTopLevelAncestor();

          aboutWindow = new ToolsAboutWindow(parentFrame);
        }
        aboutWindow.setVisible(true);
      }
    };
    BAMutil.setActionProperties(aboutAction, null, "About", false, 'A', 0);
    BAMutil.addActionToMenu(this, aboutAction);

    AbstractAction logoAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        ToolsSplashScreen.getSharedInstance().setVisible(true);
      }
    };
    BAMutil.setActionProperties(logoAction, null, "Logo", false, 'L', 0);
    BAMutil.addActionToMenu(this, logoAction);
  }
}
