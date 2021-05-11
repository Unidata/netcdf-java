/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import java.nio.charset.StandardCharsets;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.internal.iosp.hdf5.H5header;
import ucar.nc2.internal.iosp.hdf5.H5iosp;
import ucar.nc2.internal.iosp.hdf5.H5objects;
import ucar.nc2.util.DebugFlags;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JSplitPane;

/** ToolsUI/Iosp/Hdf5 raw file objects */
public class Hdf5NewObjectTable extends Hdf5ObjectTable {
  private final BeanTable<ObjectBean> objectTable;
  private BeanTable<MessageBean> messTable;
  private BeanTable<AttributeBean> attTable;
  private final JSplitPane splitH;
  private final JSplitPane split;
  private final JSplitPane split2;

  private final TextHistoryPane dumpTA;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private H5iosp iosp;

  public Hdf5NewObjectTable(PreferencesExt prefs) {
    super(prefs);
    dumpTA = new TextHistoryPane();

    objectTable = new BeanTable<>(ObjectBean.class, (PreferencesExt) prefs.node("Hdf5Object"), false,
        "H5headerNew.DataObject", "Level 2A data object header", null);
    objectTable.addListSelectionListener(e -> {
      messTable.setBeans(new ArrayList<>());

      ArrayList<MessageBean> beans = new ArrayList<>();
      ObjectBean ob = objectTable.getSelectedBean();
      if (ob != null) {
        for (H5objects.HeaderMessage m : ob.m.getMessages()) {
          beans.add(new MessageBean(m));
        }
        messTable.setBeans(beans);

        ArrayList<AttributeBean> attBeans = new ArrayList<>();
        for (H5objects.MessageAttribute m : ob.m.getAttributes()) {
          attBeans.add(new AttributeBean(m));
        }
        attTable.setBeans(attBeans);
      }
    });

    PopupMenu varPopup = new PopupMenu(objectTable.getJTable(), "Options");
    varPopup.addAction("show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ObjectBean mb = objectTable.getSelectedBean();
        if (mb == null) {
          return;
        }
        dumpTA.clear();
        Formatter f = new Formatter();

        try {
          mb.show(f);
        } catch (IOException exc) {
          exc.printStackTrace();
        }
        dumpTA.appendLine(f.toString());
        dumpTA.gotoTop();
      }
    });


    messTable = new BeanTable<>(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false,
        "H5headerNew.HeaderMessage", "Level 2A1 and 2A2 (part of Data Object)", null);
    messTable.addListSelectionListener(e -> {
      MessageBean mb = messTable.getSelectedBean();
      if (mb != null) {
        dumpTA.setText(mb.m.toString());
      }
    });

    varPopup = new PopupMenu(messTable.getJTable(), "Options");
    varPopup.addAction("Show FractalHeap", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = messTable.getSelectedBean();
        if (mb == null)
          return;
        if (infoTA == null)
          makeInfoWindow();
        infoTA.clear();
        Formatter f = new Formatter();

        mb.m.showFractalHeap(f);
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    attTable = new BeanTable<>(AttributeBean.class, (PreferencesExt) prefs.node("AttBean"), false,
        "H5headerNew.HeaderAttribute", "Message Type 12/0xC : define an Atribute", null);
    attTable.addListSelectionListener(e -> {
      AttributeBean mb = attTable.getSelectedBean();
      if (mb != null) {
        Formatter f = new Formatter();
        mb.show(f);
        dumpTA.setText(f.toString());
      }
    });

    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, objectTable, dumpTA);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 600));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, messTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, attTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split2, BorderLayout.CENTER);
  }

  public void save() {
    objectTable.saveState(false);
    messTable.saveState(false);
    attTable.saveState(false);
    // prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
    prefs.putInt("splitPosH", splitH.getDividerLocation());
  }

  private void makeInfoWindow() {
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds(new Rectangle(300, 300, 500, 800));
  }

  public void getEosInfo(Formatter f) throws IOException {
    H5header header = iosp.getHeader();
    header.getEosInfo(f);
  }

  public void closeOpenFiles() throws IOException {
    if (iosp != null) {
      iosp.close();
    }
    iosp = null;
    attTable.clearBeans();
    messTable.clearBeans();
    objectTable.clearBeans();
    dumpTA.clear();
  }

  public void setHdf5File(RandomAccessFile raf) throws IOException {
    closeOpenFiles();

    iosp = new H5iosp();
    try {
      NetcdfFile ncfile = NetcdfFiles.build(iosp, raf, raf.getLocation(), null);
      ncfile.sendIospMessage(H5iosp.IOSP_MESSAGE_INCLUDE_ORIGINAL_ATTRIBUTES);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(20000);
      PrintWriter s = new PrintWriter(sw);
      t.printStackTrace(s);
      dumpTA.setText(sw.toString());
    }

    List<ObjectBean> beanList = new ArrayList<>();
    H5header header = iosp.getHeader();
    if (header != null) {
      for (H5objects.DataObject dataObj : header.getDataObjects()) {
        beanList.add(new ObjectBean(dataObj));
      }
    }
    objectTable.setBeans(beanList);
  }

  public void showInfo(Formatter f) {
    if (iosp == null) {
      return;
    }
    for (ObjectBean bean : objectTable.getBeans()) {
      bean.m.show(f);
    }
  }

  public void showInfo2(Formatter f) throws IOException {
    if (iosp == null)
      return;

    ByteArrayOutputStream os = new ByteArrayOutputStream(100 * 1000);
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    H5header.setDebugFlags(DebugFlags
        .create("H5headerNew/header H5headerNew/headerDetails H5headerNew/symbolTable H5headerNew/memTracker"));
    H5header headerEmpty = (H5header) iosp.sendIospMessage("headerEmpty");
    headerEmpty.read(pw);
    H5header.setDebugFlags(DebugFlags.create(""));
    pw.flush();
    f.format("%s", os.toString(StandardCharsets.UTF_8.name()));
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  public static class ObjectBean {
    H5objects.DataObject m;

    public ObjectBean() {}

    ObjectBean(H5objects.DataObject m) {
      this.m = m;
    }

    public long getAddress() {
      return m.getAddress();
    }

    public String getName() {
      return m.getName();
    }

    void show(Formatter f) throws IOException {
      f.format("HDF5 object name '%s'%n", m.getName());
      for (H5objects.MessageAttribute mess : m.getAttributes()) {
        f.format("  %s%n", mess);
      }
    }
  }

  public static class MessageBean {
    H5objects.HeaderMessage m;

    public MessageBean() {}

    MessageBean(H5objects.HeaderMessage m) {
      this.m = m;
    }

    public String getMessageType() {
      return m.getMtype().toString();
    }

    public String getName() {
      return m.getName();
    }

    public int getSize() {
      return m.getSize();
    }

    public byte getFlags() {
      return m.getFlags();
    }

    public long getStart() {
      return m.getStart();
    }
  }

  public static class AttributeBean {
    H5objects.MessageAttribute att;

    public AttributeBean() {}

    AttributeBean(H5objects.MessageAttribute att) {
      this.att = att;
    }

    public byte getVersion() {
      return att.getVersion();
    }

    public String getAttributeName() {
      return att.getName();
    }

    public String getMdt() {
      return att.getMdt().toString();
    }

    public String getMds() {
      return att.getMds().toString();
    }

    public long getDataPos() {
      return att.getDataPosAbsolute();
    }

    void show(Formatter f) {
      f.format("hdf5 att = %s%n%n", att);
    }
  }
}
