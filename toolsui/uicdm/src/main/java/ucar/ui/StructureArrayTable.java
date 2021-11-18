/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import ucar.array.*;
import ucar.array.StructureMembers.Member;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.calendar.CalendarDate;
import ucar.ui.StructureTable.DateRenderer;
import ucar.nc2.write.NcdumpArray;
import ucar.ui.table.ColumnWidthsResizer;
import ucar.ui.table.HidableTableColumnModel;
import ucar.ui.table.TableAligner;
import ucar.ui.table.TableAppearanceAction;
import ucar.ui.table.UndoableRowSorter;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;

/** Component "Data Table" in DatasetViewer when you have a Structure selected. */
public class StructureArrayTable extends JPanel {
  // TODO why is this static?
  private static final HashMap<String, IndependentWindow> windows = new HashMap<>(); // display subtables

  private final PreferencesExt prefs;
  private AbstractSATableModel dataModel;

  private JTable jtable;
  private PopupMenu popup;
  private final FileManager fileChooser; // for exporting
  private final TextHistoryPane dumpTA;
  private final IndependentWindow dumpWindow;
  private final EventListenerList listeners = new EventListenerList();

  public StructureArrayTable(PreferencesExt prefs) {
    this.prefs = prefs;

    jtable = new JTable();
    setLayout(new BorderLayout());
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    ToolTipManager.sharedInstance().registerComponent(jtable);
    add(new JScrollPane(jtable), BorderLayout.CENTER);

    // other widgets
    dumpTA = new TextHistoryPane(false);
    dumpWindow = new IndependentWindow("Show Data", BAMutil.getImage("nj22/NetcdfUI"), dumpTA);
    if (prefs != null)
      dumpWindow.setBounds((Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle(300, 300, 600, 600)));
    else
      dumpWindow.setBounds(new Rectangle(300, 300, 600, 600));

    PreferencesExt fcPrefs = (prefs == null) ? null : (PreferencesExt) prefs.node("FileManager");
    fileChooser = new FileManager(null, null, "csv", "comma seperated values", fcPrefs);
  }

  /**
   * Add listener: ListSelectionEvent sent when a new row is selected
   *
   * @param l the listener
   */
  public void addListSelectionListener(ListSelectionListener l) {
    listeners.add(ListSelectionListener.class, l);
  }

  /**
   * Remove listener
   *
   * @param l the listener
   */
  public void removeListSelectionListener(ListSelectionListener l) {
    listeners.remove(ListSelectionListener.class, l);
  }

  private void fireEvent(javax.swing.event.ListSelectionEvent event) {
    Object[] llist = listeners.getListenerList();
    // Process the listeners last to first
    for (int i = llist.length - 2; i >= 0; i -= 2)
      ((ListSelectionListener) llist[i + 1]).valueChanged(event);
  }

  public void addActionToPopupMenu(String title, AbstractAction act) {
    if (popup == null)
      popup = new PopupMenu(jtable, "Options");
    popup.addAction(title, act);
  }

  // clear the table

  public void clear() {
    if (dataModel != null)
      dataModel.clear();
  }

  // save state

  public void saveState() {
    fileChooser.save();
    if (prefs != null)
      prefs.getBean("DumpWindowBounds", dumpWindow.getBounds());
  }

  public void setStructure(Structure s) {
    if (s.getArrayType() == ArrayType.SEQUENCE)
      dataModel = new SequenceModel((Sequence) s, true);
    else
      dataModel = new StructureModel(s);

    initTable(dataModel);
  }

  public void setStructureData(StructureDataArray as) {
    dataModel = new StructureDataArrayModel(as);
    initTable(dataModel);
  }

  private void initTable(AbstractSATableModel m) {
    TableColumnModel tcm = new HidableTableColumnModel(m);
    jtable = new JTable(m, tcm);
    jtable.setRowSorter(new UndoableRowSorter<>(m));

    // Fixes this bug: http://stackoverflow.com/questions/6601994/jtable-boolean-cell-type-background
    ((JComponent) jtable.getDefaultRenderer(Boolean.class)).setOpaque(true);

    // Set the preferred column widths so that they're big enough to display all data without truncation.
    ColumnWidthsResizer resizer = new ColumnWidthsResizer(jtable);
    jtable.getModel().addTableModelListener(resizer);
    jtable.getColumnModel().addColumnModelListener(resizer);

    // Left-align every cell, including header cells.
    TableAligner aligner = new TableAligner(jtable, SwingConstants.LEADING);
    jtable.getColumnModel().addColumnModelListener(aligner);

    // Don't resize the columns to fit the available space. We do this because there may be a LOT of columns, and
    // auto-resize would cause them to be squished together to the point of uselessness. For an example, see
    // Q:/cdmUnitTest/ft/stationProfile/noaa-cap/XmadisXdataXLDADXprofilerXnetCDFX20100501_0200
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    ListSelectionModel rowSM = jtable.getSelectionModel();
    rowSM.addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      } // Ignore extra messages.
      ListSelectionModel lsm = (ListSelectionModel) e.getSource();
      if (!lsm.isSelectionEmpty()) {
        fireEvent(e);
      }
    });

    if (m.wantDate) {
      jtable.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
      jtable.getColumnModel().getColumn(1).setCellRenderer(new DateRenderer());
    }

    // reset popup
    popup = null;
    addActionToPopupMenu("Show", new AbstractAction() {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        showData();
      }
    });

    addActionToPopupMenu("Export", new AbstractAction() {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        export();
      }
    });

    addActionToPopupMenu("Show Internal", new AbstractAction() {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        showDataInternal();
      }
    });

    // add any subtables from inner Structures
    for (String subtable : m.subtables) {
      addActionToPopupMenu("Data Table for " + subtable, new SubtableAction("", subtable));
    }

    removeAll();

    // Create a button that will popup a menu containing options to configure the appearance of the table.
    JButton cornerButton = new JButton(new TableAppearanceAction(jtable));
    cornerButton.setHideActionText(true);
    cornerButton.setContentAreaFilled(false);

    // Install the button in the upper-right corner of the table's scroll pane.
    JScrollPane scrollPane = new JScrollPane(jtable);
    scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    // This keeps the corner button visible even when the table is empty (or all columns are hidden).
    scrollPane.setColumnHeaderView(new JViewport());
    scrollPane.getColumnHeader().setPreferredSize(jtable.getTableHeader().getPreferredSize());

    add(scrollPane, BorderLayout.CENTER);

    revalidate();
  }

  private class SubtableAction extends AbstractAction {
    String memberName;
    StructureArrayTable dataTable;
    IndependentWindow dataWindow;

    SubtableAction(String parentName, String memberName) {
      this.memberName = memberName;
      String fullname = parentName + "/" + memberName;
      dataTable = new StructureArrayTable(null); // LOOK no nested prefs
      dataWindow = windows.get(fullname);
      if (dataWindow == null) {
        dataWindow = new IndependentWindow("Data Array Table", BAMutil.getImage("nj22/NetcdfUI"), dataTable);
        windows.put(fullname, dataWindow);
      } else {
        dataWindow.setComponent(dataTable);
      }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
      StructureData sd = getSelectedStructureData();
      if (sd == null) {
        return;
      }
      StructureMembers members = sd.getStructureMembers();
      StructureMembers.Member m = members.findMember(memberName);
      if (m == null)
        throw new IllegalStateException("cant find member = " + memberName);

      if (m.getArrayType() == ArrayType.STRUCTURE || m.getArrayType() == ArrayType.SEQUENCE) {
        StructureDataArray as = (StructureDataArray) sd.getMemberData(m);
        dataTable.setStructureData(as);

      } else {
        throw new IllegalStateException("data type = " + m.getArrayType());
      }

      dataWindow.show();
    }
  }

  private void export() {
    String filename = fileChooser.chooseFilename();
    if (filename == null)
      return;
    try {
      PrintWriter pw = new PrintWriter(new File(filename), StandardCharsets.UTF_8.name());

      TableModel model = jtable.getModel();
      for (int col = 0; col < model.getColumnCount(); col++) {
        if (col > 0)
          pw.print(",");
        pw.print(model.getColumnName(col));
      }
      pw.println();

      for (int row = 0; row < model.getRowCount(); row++) {
        for (int col = 0; col < model.getColumnCount(); col++) {
          if (col > 0)
            pw.print(",");
          pw.print(model.getValueAt(row, col));
        }
        pw.println();
      }
      pw.close();
      JOptionPane.showMessageDialog(this, "File successfully written");
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }

  }

  private void showData() {
    StructureData sd = getSelectedStructureData();
    if (sd == null)
      return;

    dumpTA.setText(NcdumpArray.printStructureData(sd));
    dumpWindow.setVisible(true);
  }

  private void showDataInternal() {
    StructureData sd = getSelectedStructureData();
    if (sd == null) {
      return;
    }

    Formatter f = new Formatter();
    f.format("Structure Data %s%n", sd.getName());
    StructureMembers members = sd.getStructureMembers();
    f.format(" Members %s%n", members.getName());
    f.format("   Type      Offset Storage ByteOrder Name%n");
    for (Member m : members) {
      f.format("   %9s %3d %6d %12s %s%n", m.getArrayType(), m.getOffset(), m.getStorageSizeBytes(), m.getByteOrder(),
          m.getName());
    }
    dumpTA.setText(f.toString());
    dumpWindow.setVisible(true);
  }

  private StructureData getSelectedStructureData() {
    int viewRowIdx = jtable.getSelectedRow();
    if (viewRowIdx < 0)
      return null;
    int modelRowIndex = jtable.convertRowIndexToModel(viewRowIdx);

    try {
      return dataModel.getStructureData(modelRowIndex);
    } catch (ucar.array.InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  public Object getSelectedRow() {
    int viewRowIdx = jtable.getSelectedRow();
    if (viewRowIdx < 0)
      return null;
    int modelRowIndex = jtable.convertRowIndexToModel(viewRowIdx);

    try {
      return dataModel.getRow(modelRowIndex);
    } catch (ucar.array.InvalidRangeException | IOException e) {
      JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  private abstract static class AbstractSATableModel extends AbstractTableModel {
    protected StructureMembers members;
    protected boolean wantDate;
    protected List<String> subtables = new ArrayList<>();
    private final LoadingCache<Integer, StructureData> cache =
        CacheBuilder.newBuilder().maximumSize(500).build(new CacheLoader<Integer, StructureData>() {
          @Override
          public StructureData load(Integer row) {
            try {
              return getStructureData(row);
            } catch (ucar.array.InvalidRangeException | IOException e) {
              throw new IllegalStateException(e.getCause());
            }
          }
        });

    // get row data if in the cache, otherwise read it
    public StructureData getStructureDataHash(int row) {
      try {
        return cache.get(row);
      } catch (ExecutionException e) {
        throw new IllegalStateException(e.getCause());
      }
    }

    // subclasses implement these
    public abstract StructureData getStructureData(int row) throws ucar.array.InvalidRangeException, IOException;

    // remove all data
    public abstract void clear();

    // if we know how to extract the date for this data, add two extra columns
    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public Object getRow(int row) throws ucar.array.InvalidRangeException, IOException {
      return getStructureData(row);
    }

    @Override
    public int getColumnCount() {
      if (members == null) {
        return 0;
      }
      return members.getMembers().size() + (wantDate ? 2 : 0);
    }

    @Override
    public String getColumnName(int columnIndex) {
      if (wantDate && (columnIndex == 0)) {
        return "obsDate";
      }
      if (wantDate && (columnIndex == 1)) {
        return "nomDate";
      }
      int memberCol = wantDate ? columnIndex - 2 : columnIndex;
      return members.getMember(memberCol).getName();
    }

    @Override
    public Object getValueAt(int row, int column) {
      if (wantDate && (column == 0)) {
        return getObsDate(row);
      }
      if (wantDate && (column == 1)) {
        return getNomDate(row);
      }

      StructureData sd = getStructureDataHash(row);
      String colName = getColumnName(column);
      Array<?> data = sd.getMemberData(colName);
      if (data instanceof StructureDataArray) {
        return "len =" + data.length();
      }
      return data;
    }
  }

  ////////////////////////////////////////////////////////////////////////
  // handles Structures

  private static class StructureModel extends AbstractSATableModel {
    private Structure struct;

    StructureModel(Structure s) {
      this.struct = s;
      this.members = s.makeStructureMembersBuilder().setStandardOffsets().build();
      for (Variable v : s.getVariables()) {
        if (v instanceof Structure) {
          subtables.add(v.getShortName());
        }
      }
    }

    @Override
    public int getRowCount() {
      if (struct == null)
        return 0;
      return (int) struct.getSize();
    }

    @Override
    public StructureData getStructureData(int row) throws ucar.array.InvalidRangeException, IOException {
      ucar.array.Array<?> array = struct.readArray(ucar.array.Section.builder().appendRange(row, row).build());
      return (StructureData) array.getScalar();
    }

    @Override
    public void clear() {
      struct = null;
      fireTableDataChanged();
    }
  }

  private static class SequenceModel extends StructureModel {
    protected List<StructureData> sdataList;

    SequenceModel(Sequence seq, boolean readData) {
      super(seq);

      if (readData) {
        sdataList = new ArrayList<>();
        try {
          for (StructureData sdata : seq) {
            sdataList.add(sdata);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public int getRowCount() {
      return sdataList.size();
    }

    @Override
    public StructureData getStructureData(int row) {
      return sdataList.get(row);
    }

    // LOOK does this have to override ?
    @Override
    public Object getValueAt(int row, int column) {
      StructureData sd = sdataList.get(row);
      Member member = sd.getStructureMembers().getMember(column);
      Array<?> value = sd.getMemberData(member);
      return !(value instanceof StructureDataArray) ? value.getScalar() : "n=" + value.length();
    }

    @Override
    public void clear() {
      sdataList = new ArrayList<>();
      fireTableDataChanged();
    }
  }

  ////////////////////////////////////////////////////////////////////////
  // Handles nested StructureDataArray

  private static class StructureDataArrayModel extends AbstractSATableModel {
    private StructureDataArray as;

    StructureDataArrayModel(StructureDataArray as) {
      this.as = as;
      this.members = as.getStructureMembers();
      for (Member m : as.getStructureMembers()) {
        if (m.getArrayType() == ArrayType.SEQUENCE || m.getArrayType() == ArrayType.STRUCTURE) {
          subtables.add(m.getName());
        }
      }
    }

    @Override
    public int getRowCount() {
      return (as == null) ? 0 : (int) as.length();
    }

    @Override
    public StructureData getStructureData(int row) {
      return as.get(row);
    }

    @Override
    public void clear() {
      as = null;
      fireTableDataChanged();
    }
  }
}
