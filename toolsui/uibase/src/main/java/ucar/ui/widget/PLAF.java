/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.lang.invoke.MethodHandles;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Pluggable Look and Feel management.
 *
 * @author John Caron
 */
public class PLAF {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final JComponent jc;

  /*
   * Constructor.
   *
   * @param tree the top-level JComponent tree: everything in this tree will get switched to
   * the new L&F. Everything not in the tree (eg Dialogs) should listen for changes like:
   * <pre>
   * UIManager.addPropertyChangeListener( new PropertyChangeListener() {
   * public void propertyChange( PropertyChangeEvent e) {
   * if (e.getPropertyName().equals("lookAndFeel"))
   * SwingUtilities.updateComponentTreeUI( <myDialogObject>);
   * }
   * });
   * </pre>
   */
  public PLAF(JComponent jc) {
    this.jc = jc;
  }

  /**
   * Add a set of MenuItems to the given JMenu, one for each possible L&F.
   * if this platform doesnt support the L&F, disable the MenuItem.
   */
  public void addToMenu(JMenu menu) {
    UIManager.LookAndFeelInfo[] plafInfo = UIManager.getInstalledLookAndFeels();
    for (UIManager.LookAndFeelInfo aPlafInfo : plafInfo) {
      addToMenu(aPlafInfo.getName(), aPlafInfo.getClassName(), menu);
    }

    LookAndFeel current = UIManager.getLookAndFeel();
    System.out.printf("current L&F=%s%n", current.getName());
  }

  /**
   *
   */
  private void addToMenu(String name, String className, JMenu menu) {
    logger.debug("PLAF LookAndFeelInfo  {}", className);
    boolean isSupported = true;
    try {
      Class<?> cl = Class.forName(className);
      LookAndFeel lf = (LookAndFeel) cl.newInstance();
      if (!lf.isSupportedLookAndFeel()) {
        isSupported = false;
      }
    } catch (Throwable t) {
      isSupported = false;
    }

    AbstractAction act = new PLAFAction(name, className);
    JMenuItem mi = menu.add(act);
    if (!isSupported) {
      mi.setEnabled(false);
    }
  }

  /**
   *
   */
  private class PLAFAction extends AbstractAction {
    final String plafClassName;

    PLAFAction(String name, String plafClassName) {
      this.plafClassName = plafClassName;
      putValue(Action.NAME, name);
    }

    /** */
    @Override
    public void actionPerformed(ActionEvent evt) {
      try {
        UIManager.setLookAndFeel(plafClassName);
      } catch (Exception ex) {
        ex.printStackTrace();
        return;
      }

      // This sets L&F for top level and its children only
      // Dialog boxes must listen fo L&F PropertyChangeEvents


      JFrame parentFrame = (JFrame) jc.getTopLevelAncestor();

      SwingUtilities.updateComponentTreeUI(parentFrame);
    }
  }
}
