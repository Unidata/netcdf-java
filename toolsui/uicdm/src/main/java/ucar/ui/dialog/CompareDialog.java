/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
 * Created by JFormDesigner on Tue Jan 26 17:07:19 MST 2010
 */

package ucar.ui.dialog;

import ucar.ui.widget.FileManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author John Caron
 */
public class CompareDialog extends JDialog {
  public enum HowMuch {
    All, varOnly, varNameOnly
  }

  public static class Data {
    public HowMuch howMuch;
    public String name;
    public boolean showCompare, showDetails, readData;

    private Data(String name, HowMuch howMuch, boolean showCompare, boolean showDetails, boolean readData) {
      this.name = name;
      this.howMuch = howMuch;
      this.showCompare = showCompare;
      this.showDetails = showDetails;
      this.readData = readData;
    }


    @Override
    public String toString() {
      return "Data{" + "howMuch=" + howMuch + ", name='" + name + '\'' + ", showCompare=" + showCompare
          + ", showDetails=" + showDetails + ", readData=" + readData + '}';
    }
  }

  //////////////////////
  private FileManager fileChooser;

  public CompareDialog(Frame owner, FileManager fileChooser) {
    super(owner);
    this.fileChooser = fileChooser;
    initComponents();
  }

  private void okButtonActionPerformed(ActionEvent e) {
    HowMuch howMuch = all.isSelected() ? HowMuch.All : (varOnly.isSelected() ? HowMuch.varOnly : HowMuch.varNameOnly);
    Data data = new Data((String) datasetCombo.getSelectedItem(), howMuch, showCompare.isSelected(),
        showDetails.isSelected(), readData.isSelected());
    firePropertyChange("OK", null, data);
    setVisible(false);
  }

  private void cancelButtonActionPerformed(ActionEvent e) {
    setVisible(false);
  }

  private void fileBrowserActionPerformed(ActionEvent e) {
    String filename = fileChooser.chooseFilename();
    if (filename == null)
      return;
    datasetCombo.insertItemAt(filename, 0);
    datasetCombo.setSelectedIndex(0);
  }

  private void datasetComboActionPerformed(ActionEvent e) {
    // System.out.printf("%s%n", e);
    if (!e.getActionCommand().equals("comboBoxChanged"))
      return;
    datasetCombo.insertItemAt(datasetCombo.getSelectedItem(), 0);
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    fileBrowser = new JButton();
    label1 = new JLabel();
    all = new JRadioButton();
    varOnly = new JRadioButton();
    buttonBar = new JPanel();
    readData = new JCheckBox();
    showDetails = new JCheckBox();
    showCompare = new JCheckBox();
    datasetCombo = new JComboBox();
    cancelButton = new JButton();
    okButton = new JButton();
    varNameOnly = new JRadioButton();

    // ======== this ========
    setTitle("Compare current dataset with another dataset");
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    // ======== dialogPane ========
    {
      dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
      dialogPane.setLayout(new BorderLayout());

      // ======== contentPanel ========
      {

        // ---- fileBrowser ----
        fileBrowser.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        fileBrowser.setToolTipText("open Local dataset");
        fileBrowser.addActionListener(this::fileBrowserActionPerformed);

        // ---- label1 ----
        label1.setText("dataset:");
        label1.setFont(new Font("Dialog", Font.BOLD, 12));

        // ---- all ----
        all.setText("All");
        all.setSelected(true);

        // ---- varOnly ----
        varOnly.setText("selected Variable");

        // ======== buttonBar ========
        {
          buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
          buttonBar.setLayout(new GridBagLayout());
          ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
          ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};
        }

        // ---- readData ----
        readData.setText("compare Data");

        // ---- showDetails ----
        showDetails.setText("show Details");

        // ---- showCompare ----
        showCompare.setText("show Compare");
        showCompare.setSelected(true);

        // ---- datasetCombo ----
        datasetCombo.setEditable(true);
        datasetCombo.addActionListener(this::datasetComboActionPerformed);

        // ---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(this::cancelButtonActionPerformed);

        // ---- okButton ----
        okButton.setText("OK");
        okButton.addActionListener(this::okButtonActionPerformed);

        // ---- varNameOnly ----
        varNameOnly.setText("Variable names only");

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(contentPanelLayout.createParallelGroup().addGroup(contentPanelLayout
            .createSequentialGroup().addContainerGap()
            .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                .addGroup(contentPanelLayout.createSequentialGroup().addComponent(label1)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(contentPanelLayout.createParallelGroup()
                        .addGroup(contentPanelLayout.createSequentialGroup()
                            .addComponent(datasetCombo, GroupLayout.PREFERRED_SIZE, 676, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(fileBrowser))
                        .addGroup(contentPanelLayout.createSequentialGroup().addComponent(showCompare)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(showDetails)
                            .addGap(18, 18, 18).addComponent(readData))
                        .addGroup(contentPanelLayout.createSequentialGroup().addComponent(all).addGap(18, 18, 18)
                            .addComponent(varOnly).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(varNameOnly))))
                .addComponent(buttonBar, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 779,
                    GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
            .addGroup(GroupLayout.Alignment.TRAILING,
                contentPanelLayout.createSequentialGroup().addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                    .addGap(32, 32, 32)));
        contentPanelLayout
            .setVerticalGroup(
                contentPanelLayout.createParallelGroup()
                    .addGroup(contentPanelLayout.createSequentialGroup().addContainerGap()
                        .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label1).addComponent(datasetCombo, GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addComponent(fileBrowser))
                        .addGap(20, 20, 20)
                        .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(all).addComponent(varOnly).addComponent(varNameOnly))
                        .addGap(18, 18, 18)
                        .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(showCompare).addComponent(showDetails).addComponent(readData))
                        .addGap(22, 22, 22)
                        .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(okButton).addComponent(cancelButton))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                        .addComponent(buttonBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE)
                        .addContainerGap()));
      }
      dialogPane.add(contentPanel, BorderLayout.NORTH);
    }
    contentPane.add(dialogPane, BorderLayout.CENTER);
    pack();
    setLocationRelativeTo(getOwner());

    // ---- buttonGroup1 ----
    ButtonGroup buttonGroup1 = new ButtonGroup();
    buttonGroup1.add(all);
    buttonGroup1.add(varOnly);
    buttonGroup1.add(varNameOnly);
    // JFormDesigner - End of component initialization //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JPanel dialogPane;
  private JPanel contentPanel;
  private JButton fileBrowser;
  private JLabel label1;
  private JRadioButton all;
  private JRadioButton varOnly;
  private JPanel buttonBar;
  private JCheckBox readData;
  private JCheckBox showDetails;
  private JCheckBox showCompare;
  private JComboBox datasetCombo;
  private JButton cancelButton;
  private JButton okButton;
  private JRadioButton varNameOnly;
  // JFormDesigner - End of variables declaration //GEN-END:variables
}
