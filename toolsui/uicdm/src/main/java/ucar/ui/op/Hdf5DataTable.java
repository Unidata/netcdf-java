/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.internal.iosp.hdf5.H5header;
import ucar.nc2.internal.iosp.hdf5.H5iosp;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Show HDF5 data objects and their compression
 *
 * @author caron
 * @since 6/26/12
 */
public class Hdf5DataTable extends JPanel {
  protected final PreferencesExt prefs;

  private BeanTable<VarBean> objectTable;
  private JSplitPane splitH;

  private TextHistoryPane infoTA;

  private H5iosp iosp;
  private String location;

  public Hdf5DataTable(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;
    if (buttPanel == null)
      return;

    PopupMenu varPopup;

    objectTable = new BeanTable<>(VarBean.class, (PreferencesExt) prefs.node("Hdf5Object"), false,
        "H5header.DataObject", "Level 2A data object header", null);
    objectTable.addListSelectionListener(e -> {
      VarBean vb = (VarBean) objectTable.getSelectedBean();
      vb.count(true);
      vb.show();
    });

    AbstractAction calcAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        calcStorage();
      }
    };
    BAMutil.setActionProperties(calcAction, "nj22/Dataset", "calc storage", false, 'D', -1);
    BAMutil.addActionToContainer(buttPanel, calcAction);

    varPopup = new PopupMenu(objectTable.getJTable(), "Options");
    varPopup.addAction("deflate", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VarBean mb = (VarBean) objectTable.getSelectedBean();
        if (mb == null) {
          return;
        }
        infoTA.clear();

        Formatter f = new Formatter();
        deflate(f, mb);
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
      }
    });

    varPopup.addAction("show storage", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VarBean mb = (VarBean) objectTable.getSelectedBean();
        if (mb == null) {
          return;
        }
        infoTA.clear();

        Formatter f = new Formatter();
        showStorage(f, mb);
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
      }
    });

    varPopup.addAction("show vlen", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VarBean mb = (VarBean) objectTable.getSelectedBean();
        if (mb == null) {
          return;
        }
        infoTA.clear();
        Formatter f = new Formatter();

        showVlen(f, mb);
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();

    splitH = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, objectTable, infoTA);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 600));

    setLayout(new BorderLayout());
    add(splitH, BorderLayout.CENTER);
  }

  public void save() {
    objectTable.saveState(false);
    prefs.putInt("splitPosH", splitH.getDividerLocation());
  }

  public void closeOpenFiles() throws IOException {
    if (iosp != null) {
      iosp.close();
    }
    iosp = null;
  }

  public void setHdf5File(RandomAccessFile raf) throws IOException {
    closeOpenFiles();

    this.location = raf.getLocation();
    List<VarBean> beanList = new ArrayList<>();

    iosp = new H5iosp();
    NetcdfFile ncfile = NetcdfFiles.build(iosp, raf, raf.getLocation(), null);
    try {
      iosp.open(raf, ncfile, null);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(20000);
      t.printStackTrace(new PrintWriter(sw));
      infoTA.setText(sw.toString());
    }

    for (Variable v : ncfile.getVariables()) {
      beanList.add(new VarBean(v));
    }

    objectTable.setBeans(beanList);
  }

  public void calcStorage() {
    if (iosp == null) {
      return;
    }

    long totalVars = 0;
    long totalStorage = 0;
    long totalCount = 0;

    Formatter f = new Formatter();
    for (Object obean : objectTable.getBeans()) {
      VarBean bean = (VarBean) obean;
      bean.count(false);
      totalStorage += bean.getStorage();
      totalCount += bean.getNchunks();
      totalVars += bean.getSizeBytes();
    }

    f.format("%n");
    f.format(" total uncompressed  = %,d%n", totalVars);
    f.format(" total data storage  = %,d%n", totalStorage);

    File raf = new File(location);
    f.format("        file size    = %,d%n", raf.length());

    float ratio = (totalStorage == 0) ? 0 : ((float) raf.length()) / totalStorage;
    f.format("        overhead     = %f%n", ratio);

    ratio = (totalStorage == 0) ? 0 : ((float) totalVars) / totalStorage;
    f.format("         compression = %f%n", ratio);
    f.format("   # data chunks     = %d%n", totalCount);

    infoTA.setText(f.toString());
  }

  void deflate(Formatter f, VarBean bean) {}

  private void showStorage(Formatter f, VarBean bean) {
    try {
      bean.vinfo.countStorageSize(f);
    } catch (IOException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    }
  }

  private void showVlen(Formatter f, VarBean bean) {
    if (!bean.v.isVariableLength()) {
      f.format("Variable %s must be variable length", bean.v.getFullName());
      return;
    }

    try {
      int countRows = 0;
      long countElems = 0;
      Array result = bean.v.read();
      f.format("class = %s%n", result.getClass().getName());
      while (result.hasNext()) {
        Array line = (Array) result.next();
        countRows++;
        long size = line.getSize();
        countElems += size;
        f.format("  row %d size=%d%n", countRows, size);
      }
      float avg = (countRows == 0) ? 0 : ((float) countElems) / countRows;
      f.format("%n  nrows = %d totalElems=%d avg=%f%n", countRows, countElems, avg);
    } catch (IOException e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    }
  }

  public class VarBean {
    Variable v;
    H5header.Vinfo vinfo;
    long[] countResult;

    public VarBean() {}

    public VarBean(Variable v) {
      this.v = v;
      this.vinfo = (H5header.Vinfo) v.getSPobject();
    }

    public String getName() {
      return v.getShortName();
    }

    public boolean isUseFill() {
      return vinfo.useFillValue();
    }

    public boolean isChunk() {
      return vinfo.isChunked();
    }

    public String getDims() {
      Formatter f = new Formatter();
      for (ucar.nc2.Dimension d : v.getDimensions()) {
        f.format("%d ", d.getLength());
      }
      return f.toString();
    }

    public String getChunks() {
      if (!vinfo.isChunked()) {
        return "";
      }
      int[] chunk = vinfo.getChunking();
      Formatter f = new Formatter();
      for (int i : chunk) {
        f.format("%d ", i);
      }
      return f.toString();
    }

    public int getChunkSize() {
      if (!vinfo.isChunked()) {
        return -1;
      }
      int[] chunks = vinfo.getChunking();
      int total = 1;
      for (int chunk : chunks) {
        total *= chunk;
      }
      return total;
    }

    public long getNelems() {
      return v.getSize();
    }

    public long getSizeBytes() {
      return v.getSize() * v.getElementSize();
    }

    public long getNchunks() {
      return countResult == null ? 0 : countResult[1];
    }

    public long getStorage() {
      return countResult == null ? 0 : countResult[0];
    }

    public float getRatio() {
      if (countResult == null) {
        return 0;
      }
      if (countResult[0] == 0) {
        return 0;
      }
      return ((float) getSizeBytes()) / countResult[0];
    }

    public String getDataType() {
      return v.getDataType().toString();
    }

    public String getCompression() {
      return vinfo.getCompression();
    }

    void count(boolean force) {
      if (!force && countResult != null) {
        return;
      }
      if (vinfo.useFillValue()) {
        countResult = new long[2];
        return;
      }
      if (!vinfo.isChunked()) {
        countResult = new long[2];
        countResult[0] = getSizeBytes();
        countResult[1] = 1;
        return;
      }

      try {
        countResult = vinfo.countStorageSize(null);
      } catch (IOException e) {
        e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
      }
    }

    void show() {
      Formatter f = new Formatter();
      f.format("vinfo = %s%n%n", vinfo.toString());
      f.format("      = %s%n", vinfo.extraInfo());
      infoTA.setText(f.toString());
    }
  }
}

