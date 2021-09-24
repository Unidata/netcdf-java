/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import ucar.array.ArrayType;
import ucar.ma2.ArraySequence;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.write.Ncdump;
import ucar.ui.table.*;
import ucar.ui.widget.*;
import ucar.ui.widget.PopupMenu;
import ucar.nc2.util.Indent;
import ucar.util.prefs.PreferencesExt;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * This puts the data values of a 1D Structure or Sequence into a JTable.
 * The columns are the members of the Structure.
 *
 * @author caron
 */
public class StructureTable extends JPanel {
  // display subtables LOOK WHY STATIC?
  private static final HashMap<String, IndependentWindow> windows = new HashMap<>();

  private final PreferencesExt prefs;
  private StructureTableModel dataModel;

  private JTable jtable;
  private PopupMenu popup;
  private final FileManager fileChooser; // for exporting
  private final TextHistoryPane dumpTA;
  private final IndependentWindow dumpWindow;
  private final EventListenerList listeners = new EventListenerList();

  public StructureTable(PreferencesExt prefs) {
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
    listeners.add(javax.swing.event.ListSelectionListener.class, l);
  }

  /**
   * Remove listener
   *
   * @param l the listener
   */
  public void removeListSelectionListener(ListSelectionListener l) {
    listeners.remove(javax.swing.event.ListSelectionListener.class, l);
  }

  private void fireEvent(javax.swing.event.ListSelectionEvent event) {
    Object[] llist = listeners.getListenerList();
    // Process the listeners last to first
    for (int i = llist.length - 2; i >= 0; i -= 2)
      ((javax.swing.event.ListSelectionListener) llist[i + 1]).valueChanged(event);
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
      dataModel = new SequenceModel(s, true);
    else
      dataModel = new StructureModel(s);

    initTable(dataModel);
  }

  /**
   * Set the data as a collection of StructureData.
   */
  public void setStructureData(List<StructureData> structureData) {
    dataModel = new StructureDataModel(structureData);
    initTable(dataModel);
  }

  public void setStructureData(ArrayStructure as) {
    dataModel = new ArrayStructureModel(as);
    initTable(dataModel);
  }

  public void setSequenceData(Structure s, ArraySequence seq) {
    dataModel = new ArraySequenceModel(s, seq);
    initTable(dataModel);
  }

  private void initTable(StructureTableModel m) {
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
    for (Structure s : m.subtables) {
      addActionToPopupMenu("Data Table for " + s.getShortName(), new SubtableAbstractAction(s));
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

  private class SubtableAbstractAction extends AbstractAction {
    Structure s;
    StructureTable dataTable;
    IndependentWindow dataWindow;

    SubtableAbstractAction(Structure s) {
      this.s = s;
      dataTable = new StructureTable(null);
      dataWindow = windows.get(s.getFullName());
      if (dataWindow == null) {
        dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("nj22/NetcdfUI"), dataTable);
        windows.put(s.getFullName(), dataWindow);
      } else {
        dataWindow.setComponent(dataTable);
      }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
      StructureData sd = getSelectedStructureData();
      if (sd == null)
        return;
      StructureMembers.Member m = sd.findMember(s.getShortName());
      if (m == null)
        throw new IllegalStateException("cant find member = " + s.getShortName());

      if (m.getDataType() == DataType.STRUCTURE) {
        ArrayStructure as = sd.getArrayStructure(m);
        dataTable.setStructureData(as);

      } else if (m.getDataType() == DataType.SEQUENCE) {
        ArraySequence seq = sd.getArraySequence(m);
        dataTable.setSequenceData(s, seq);

      } else
        throw new IllegalStateException("data type = " + m.getDataType());

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

    dumpTA.setText(Ncdump.printStructureData(sd));
    dumpWindow.setVisible(true);
  }

  private void showDataInternal() {
    StructureData sd = getSelectedStructureData();
    if (sd == null)
      return;

    Formatter f = new Formatter();
    sd.showInternalMembers(f, new Indent(2));
    f.format("%n");
    sd.showInternal(f, new Indent(2));
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
    } catch (InvalidRangeException e) {
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
    } catch (InvalidRangeException | IOException e) {
      JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  private abstract static class StructureTableModel extends AbstractTableModel {
    protected StructureMembers members;
    protected boolean wantDate;
    protected List<Structure> subtables = new ArrayList<>();
    private final LoadingCache<Integer, StructureData> cache =
        CacheBuilder.newBuilder().maximumSize(500).build(new CacheLoader<Integer, StructureData>() {
          @Override
          public StructureData load(Integer row) {
            try {
              return getStructureData(row);
            } catch (InvalidRangeException e) {
              throw new IllegalStateException(e.getCause());
            } catch (IOException e) {
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
    public abstract StructureData getStructureData(int row) throws InvalidRangeException, IOException;

    // remove all data
    public abstract void clear();

    // if we know how to extract the date for this data, add two extra columns
    public abstract CalendarDate getObsDate(int row);

    public abstract CalendarDate getNomDate(int row);

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return getStructureData(row);
    }

    public int getColumnCount() {
      if (members == null)
        return 0;
      return members.getMembers().size() + (wantDate ? 2 : 0);
    }

    public String getColumnName(int columnIndex) {
      // if (columnIndex == 0)
      // return "hash";
      if (wantDate && (columnIndex == 0))
        return "obsDate";
      if (wantDate && (columnIndex == 1))
        return "nomDate";
      int memberCol = wantDate ? columnIndex - 2 : columnIndex;
      return members.getMember(memberCol).getName();
    }

    public Object getValueAt(int row, int column) {
      /*
       * if (column == 0) {
       * try {
       * return Long.toHexString( getStructureData(row).hashCode());
       * } catch (Exception e) {
       * return "ERROR";
       * }
       * }
       */
      if (wantDate && (column == 0))
        return getObsDate(row);
      if (wantDate && (column == 1))
        return getNomDate(row);

      StructureData sd = getStructureDataHash(row);
      String colName = getColumnName(column);
      return sd.getArray(colName);
    }

    String enumLookup(StructureMembers.Member m, Number val) {
      return "sorry";
    }

  }

  ////////////////////////////////////////////////////////////////////////
  // handles Structures

  private static class StructureModel extends StructureTableModel {
    private Structure struct;

    StructureModel(Structure s) {
      this.struct = s;
      this.members = s.makeStructureMembers();
      for (Variable v : s.getVariables()) {
        if (v instanceof Structure)
          subtables.add((Structure) v);
      }
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      if (struct == null)
        return 0;
      return (int) struct.getSize();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return struct.readStructure(row);
    }

    public void clear() {
      struct = null;
      fireTableDataChanged();
    }

    String enumLookup(StructureMembers.Member m, Number val) {
      Variable v = struct.findVariable(m.getName());
      return v.lookupEnumString(val.intValue());
    }

  }

  // handles Sequences
  private static class SequenceModel extends StructureModel {
    protected List<StructureData> sdataList;

    SequenceModel(Structure seq, boolean readData) {
      super(seq);

      if (readData) {
        sdataList = new ArrayList<>();
        try {
          try (StructureDataIterator iter = seq.getStructureIterator()) {
            while (iter.hasNext())
              sdataList.add(iter.next());
          }

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return sdataList.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return sdataList.get(row);
    }

    // LOOK does this have to override ?
    public Object getValueAt(int row, int column) {
      StructureData sd = sdataList.get(row);
      return sd.getScalarObject(sd.getStructureMembers().getMember(column));
    }

    public void clear() {
      sdataList = new ArrayList<>();
      fireTableDataChanged();
    }
  }

  private static class ArraySequenceModel extends SequenceModel {

    ArraySequenceModel(Structure s, ArraySequence seq) {
      super(s, false);

      this.members = seq.getStructureMembers();

      sdataList = new ArrayList<>();
      try {
        try (StructureDataIterator iter = seq.getStructureDataIterator()) {
          while (iter.hasNext())
            sdataList.add(iter.next()); // LOOK lame -read at all once
        }

      } catch (IOException e) {
        JOptionPane.showMessageDialog(null, "ERROR: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////

  private static class StructureDataModel extends StructureTableModel {
    private List<StructureData> structureData;

    StructureDataModel(List<StructureData> structureData) {
      this.structureData = structureData;
      if (!structureData.isEmpty()) {
        StructureData sd = structureData.get(0);
        this.members = sd.getStructureMembers();
      }
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return structureData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return structureData.get(row);
    }

    public void clear() {
      structureData = new ArrayList<>(); // empty list
      fireTableDataChanged();
    }

  }

  ////////////////////////////////////////////////////////////////////////

  private static class ArrayStructureModel extends StructureTableModel {
    private ArrayStructure as;

    ArrayStructureModel(ArrayStructure as) {
      this.as = as;
      this.members = as.getStructureMembers();
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return (as == null) ? 0 : (int) as.getSize();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return as.getStructureData(row);
    }

    public void clear() {
      as = null;
      fireTableDataChanged();
    }

  }

  ////////////////////////////////////////////////////////////////////////

  /**
   * Renderer for Date type
   */
  static class DateRenderer extends DefaultTableCellRenderer {
    private final CalendarDateFormatter newForm;
    private final CalendarDateFormatter oldForm;
    private final CalendarDate cutoff;

    DateRenderer() {

      oldForm = new CalendarDateFormatter("yyyy MMM dd HH:mm");
      newForm = new CalendarDateFormatter("MMM dd, HH:mm");

      CalendarDate now = CalendarDate.present();
      cutoff = now.add(-1, CalendarPeriod.Field.Year); // "now" time format within a year
    }

    public void setValue(Object value) {
      if (value == null)
        setText("");
      else {
        CalendarDate date = (CalendarDate) value;
        if (date.isBefore(cutoff))
          setText(oldForm.toString(date));
        else
          setText(newForm.toString(date));
      }
    }
  }
}
