/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
 * Created by JFormDesigner on Thu Jan 28 12:24:51 MST 2010
 */

package ucar.ui.dialog;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author John Caron
 */
public class BufrUsed extends JDialog {
  public BufrUsed(Frame owner) {
    super(owner);
    initComponents();
  }

  public BufrUsed(Dialog owner) {
    super(owner);
    initComponents();
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    buttonBar = new JPanel();
    okButton = new JButton();
    cancelButton = new JButton();

    // ======== this ========
    setTitle("Find BUFR messages that use this table");
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    // ======== dialogPane ========
    {
      dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
      dialogPane.setLayout(new BorderLayout());

      // ======== contentPanel ========
      {

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(contentPanelLayout.createParallelGroup().addGap(0, 364, Short.MAX_VALUE));
        contentPanelLayout.setVerticalGroup(contentPanelLayout.createParallelGroup().addGap(0, 211, Short.MAX_VALUE));
      }
      dialogPane.add(contentPanel, BorderLayout.CENTER);

      // ======== buttonBar ========
      {
        buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
        buttonBar.setLayout(new GridBagLayout());
        ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
        ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

        // ---- okButton ----
        okButton.setText("OK");
        buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(0, 0, 0, 5), 0, 0));

        // ---- cancelButton ----
        cancelButton.setText("Cancel");
        buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
      }
      dialogPane.add(buttonBar, BorderLayout.SOUTH);
    }
    contentPane.add(dialogPane, BorderLayout.CENTER);
    pack();
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JPanel dialogPane;
  private JPanel contentPanel;
  private JPanel buttonBar;
  private JButton okButton;
  private JButton cancelButton;
  // JFormDesigner - End of variables declaration //GEN-END:variables
}
