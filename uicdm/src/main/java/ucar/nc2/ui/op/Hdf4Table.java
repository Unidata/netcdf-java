/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.internal.iosp.hdf4.H4header;
import ucar.nc2.internal.iosp.hdf4.H4iosp;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/** ToolsUI/Iosp/Hdf4 */
public class Hdf4Table extends JPanel {
  protected PreferencesExt prefs;

  private BeanTable<TagBean> tagTable;
  private JSplitPane split;

  private TextHistoryPane dumpTA;

  private H4iosp iosp;
  private H4header header;
  private String location;

  Hdf4Table(PreferencesExt prefs) {
    this.prefs = prefs;

    tagTable = new BeanTable(TagBean.class, (PreferencesExt) prefs.node("Hdf4Object"), false);
    tagTable.addListSelectionListener(e -> {
      TagBean bean = tagTable.getSelectedBean();
      dumpTA.setText("Tag=\n ");
      dumpTA.appendLine(bean.tag.detail());
      dumpTA.appendLine("\nVinfo=");
      dumpTA.appendLine(bean.tag.getVinfo());
    });

    // the info window
    dumpTA = new TextHistoryPane();

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, tagTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void save() {
    tagTable.saveState(false);
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void closeOpenFiles() throws IOException {
    if (iosp != null) {
      iosp.close();
    }
    iosp = null;
  }

  void setHdf4File(RandomAccessFile raf) throws IOException {
    closeOpenFiles();

    this.location = raf.getLocation();
    List<TagBean> beanList = new ArrayList<>();

    iosp = new H4iosp();
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);

    try {
      iosp.open(raf, ncfile, null);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(20000);
      t.printStackTrace(new PrintWriter(sw));
      dumpTA.setText(sw.toString());
    }

    header = (H4header) iosp.sendIospMessage("header");
    for (H4header.Tag tag : header.getTags()) {
      beanList.add(new TagBean(tag));
    }

    tagTable.setBeans(beanList);
  }

  void getEosInfo(Formatter f) throws IOException {
    header.getEosInfo(f);
  }

  public static class TagBean {
    H4header.Tag tag;

    public TagBean() {}

    TagBean(H4header.Tag tag) {
      this.tag = tag;
    }

    public short getCode() {
      return tag.getCode();
    }

    public String getType() {
      return tag.getType();
    }

    public short getRefno() {
      return tag.getRefno();
    }

    public boolean isExtended() {
      return tag.isExtended();
    }

    public String getVClass() {
      return tag.getVClass();
    }

    public int getOffset() {
      return tag.getOffset();
    }

    public int getLength() {
      return tag.getLength();
    }

    public boolean isUsed() {
      return tag.isUsed();
    }

    public String getVinfo() {
      return tag.getVinfo();
    }
  }
}
