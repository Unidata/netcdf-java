/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.iosp.bufr.tables.BufrTables;
import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

public class BufrTableBPanel extends OpPanel {
  private final BufrTableBViewer bufrTable;
  private final JComboBox<BufrTables.Format> modes;
  private final JComboBox<BufrTables.TableConfig> tables;

  public BufrTableBPanel(PreferencesExt p) {
    super(p, "tableB:", false, false);

    AbstractAction fileAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileManager bufrFileChooser = ToolsUI.getBufrFileChooser();

        String filename = bufrFileChooser.chooseFilename();
        if (filename == null) {
          return;
        }
        cb.setSelectedItem(filename);
      }
    };
    BAMutil.setActionProperties(fileAction, "FileChooser", "open Local table...", false, 'L', -1);
    BAMutil.addActionToContainer(buttPanel, fileAction);

    modes = new JComboBox<>(BufrTables.Format.values());
    buttPanel.add(modes);

    JButton acceptButton = new JButton("Accept");
    buttPanel.add(acceptButton);
    acceptButton.addActionListener(e -> accept());

    tables = new JComboBox<>(BufrTables.getTableConfigsAsArray());
    buttPanel.add(tables);
    tables.addActionListener(e -> acceptTable((BufrTables.TableConfig) tables.getSelectedItem()));

    bufrTable = new BufrTableBViewer(prefs, buttPanel);
    add(bufrTable, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object command) {
    return true;
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }

  private void accept() {
    String command = (String) cb.getSelectedItem();

    try {
      Object format = modes.getSelectedItem();
      bufrTable.setBufrTableB(command, (BufrTables.Format) format);
    } catch (FileNotFoundException ioe) {
      JOptionPane.showMessageDialog(null, "BufrTableViewer cannot open " + command + "\n" + ioe.getMessage());
      detailTA.setText("Failed to open <" + command + ">\n" + ioe.getMessage());
      detailTA.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailTA.setVisible(true);
    }
  }

  private void acceptTable(BufrTables.TableConfig tc) {
    try {
      bufrTable.setBufrTableB(tc.getTableBname(), tc.getTableBformat());
    } catch (FileNotFoundException ioe) {
      JOptionPane.showMessageDialog(null, "BufrTableViewer cannot open " + tc + "\n" + ioe.getMessage());
      detailTA.setText("Failed to open <" + tc + ">\n" + ioe.getMessage());
      detailTA.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailTA.setVisible(true);
    }
  }

  @Override
  public void save() {
    bufrTable.save();
    super.save();
  }
}

