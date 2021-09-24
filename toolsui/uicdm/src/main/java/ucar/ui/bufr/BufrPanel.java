/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.bufr;

import ucar.ui.OpPanel;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

public class BufrPanel extends OpPanel {
  private RandomAccessFile raf;
  private final BufrMessageViewer bufrTable;

  public BufrPanel(PreferencesExt p) {
    super(p, "file:", true, false);
    bufrTable = new BufrMessageViewer(prefs, buttPanel);
    add(bufrTable, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object o) {
    String command = (String) o;
    boolean err = false;

    try {
      if (raf != null) {
        raf.close();
      }
      raf = new RandomAccessFile(command, "r");

      bufrTable.setBufrFile(raf);
    } catch (FileNotFoundException ioe) {
      JOptionPane.showMessageDialog(null, "NetcdfDataset cannot open " + command + "\n" + ioe.getMessage());
      err = true;
    } catch (Exception e) {
      e.printStackTrace();
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailWindow.show();
      err = true;
    }

    return !err;
  }

  @Override
  public void closeOpenFiles() throws IOException {
    if (raf != null) {
      raf.close();
    }
    raf = null;
  }

  @Override
  public void save() {
    bufrTable.save();
    super.save();
  }
}
