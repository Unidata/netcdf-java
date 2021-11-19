/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.table;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import ucar.ui.event.UIChangeEvent;
import ucar.ui.event.UIChangeListener;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.PopupMenu;
import ucar.ui.util.ListenerManager;
import ucar.ui.util.NamedObject;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * JTableSorted adds sorting functionality to a JTable.
 * It also wraps the JTable in a JScrollPane.
 * Note that JTableSorted is a JPanel, and has-a JTable.
 * It throws ListSelectionEvent events when the selection changes.
 * It throws a UIChangeEvent, property = "sort" just before a sort is going to happen.
 */

public class JTableSorted extends JPanel {
  private static final boolean debug = false;

  // for HeaderRenderer
  static final Icon sortDownIcon = BAMutil.getIcon("SortDown", true);
  static final Icon sortUpIcon = BAMutil.getIcon("SortUp", true);
  static final Icon threadSortIcon = BAMutil.getIcon("ThreadSorted", true);
  static final Icon threadUnSortIcon = BAMutil.getIcon("ThreadUnsorted", true);

  private List<TableRow> rows;
  private String[] colName;

  private final JTable jtable;
  private final JScrollPane scrollPane;
  private final TableRowModel model;

  private boolean sortOK = true;
  private final ThreadSorter threadSorter;
  private int threadCol = -1;

  private final ListenerManager lm;

  /**
   * Constructor.
   * 
   * @param colName list of column names
   * @param listRT list of rows. This must contain objects that implement
   *        the TableRow interface. May be null or empty.
   */
  public JTableSorted(String[] colName, List<TableRow> listRT) {
    this(colName, listRT, false, null);
  }

  /**
   * Constructor.
   * 
   * @param columnName list of column names
   * @param listRT list of rows. This must contain objects that implement
   *        the TableRow interface. May be null or empty.
   * @param enableColumnManipulation allow columns to be added, deleted via click-right popup
   * @param threadSorter if not null, add a "thread sorting" column
   * 
   */
  public JTableSorted(String[] columnName, List<TableRow> listRT, boolean enableColumnManipulation,
      ThreadSorter threadSorter) {
    this.colName = columnName;
    this.rows = (listRT == null) ? new ArrayList<>() : listRT;
    this.threadSorter = threadSorter;

    // create the ui
    jtable = new JTable();
    jtable.setDefaultRenderer(Object.class, new MyTableCellRenderer());
    ToolTipManager.sharedInstance().registerComponent(jtable);

    model = new TableRowModel();

    setLayout(new BorderLayout());
    scrollPane = new JScrollPane(jtable);
    add(scrollPane, BorderLayout.CENTER);
    // add(jtable, BorderLayout.CENTER);

    // add a column if it has a ThreadSorter
    boolean hasThreads = (threadSorter != null);
    if (hasThreads) {
      String[] newColName = new String[colName.length + 1];
      System.arraycopy(colName, 0, newColName, 0, colName.length);

      threadCol = colName.length;
      newColName[threadCol] = "Threads";
      colName = newColName;
    }

    jtable.setModel(model);
    jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    setColumnWidths(null);

    // now set the header renderers
    TableColumnModel tcm = jtable.getColumnModel();
    int ncolwt = hasThreads ? jtable.getColumnCount() - 1 : jtable.getColumnCount();
    for (int i = 0; i < ncolwt; i++) {
      TableColumn tc = tcm.getColumn(i);
      tc.setHeaderRenderer(new SortedHeaderRenderer(colName[i], i));
    }
    if (hasThreads) {
      TableColumn tc = tcm.getColumn(ncolwt);
      tc.setHeaderRenderer(new ThreadHeaderRenderer(ncolwt));
    }

    if (enableColumnManipulation) {
      // popupMenu
      PopupMenu popupMenu = new PopupMenu(jtable.getTableHeader(), "Visible");
      int ncols = colName.length;
      PopupAction[] acts = new PopupAction[ncols];
      for (int i = 0; i < ncols; i++) {
        acts[i] = new PopupAction(colName[i]);
        popupMenu.addActionCheckBox(this.colName[i], acts[i], true);
      }
    }

    // set sorting behavior
    JTableHeader hdr = jtable.getTableHeader();
    hdr.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (!sortOK)
          return;
        TableColumnModel tcm2 = jtable.getColumnModel();
        int colIdx = tcm2.getColumnIndexAtX(e.getX());
        int colNo = jtable.convertColumnIndexToModel(colIdx);

        // keep current selection selected
        int selidx = jtable.getSelectedRow();
        Object selected = null;
        if (selidx >= 0)
          selected = rows.get(selidx);

        // notify listsners of impending sort
        if (lm.hasListeners())
          lm.sendEvent(new UIChangeEvent(this, "sort", null, colNo));

        // sort
        model.sort(colNo);

        /* keep current selection selected */
        if (selidx >= 0) {
          int newSelectedRow = rows.indexOf(selected);
          jtable.setRowSelectionInterval(newSelectedRow, newSelectedRow);
          ensureRowIsVisible(newSelectedRow);
        }
        repaint();
      }
    });

    // event manager
    lm = new ListenerManager("ucar.ui.event.UIChangeListener", "ucar.ui.event.UIChangeEvent", "processChange");

  }

  /*
   * Set the state from the last saved in the PersistentStore.
   * 
   * @param String name object name
   * 
   * @param PersistentStore store ok if null or empty
   *
   * public void restoreStateFromStore(String objectName, PersistentStore store) {
   * if (store == null)
   * return;
   * this.objectName = objectName;
   * 
   * int [] modelIndex = (int []) store.get(objectName+"ColumnOrder" );
   * if (modelIndex == null)
   * return;
   * 
   * // make invisible any not stored
   * int ncols = jtable.getColumnCount();
   * boolean [] visible = new boolean[ncols];
   * for (int i=0; i<modelIndex.length; i++)
   * if (modelIndex[i] < ncols)
   * visible[ modelIndex[i]] = true;
   * for (int i=0; i<ncols; i++)
   * if (!visible[i]) {
   * acts[i].hideColumn();
   * acts[i].putValue(BAMutil.STATE, new Boolean(false));
   * }
   * 
   * // now set the header order
   * TableColumnModel tcm = jtable.getColumnModel();
   * int n = Math.min( modelIndex.length, jtable.getColumnCount());
   * for (int i=0; i<n; i++) {
   * TableColumn tc = tcm.getColumn(i);
   * tc.setModelIndex(modelIndex[i]);
   * String name = colName[modelIndex[i]];
   * tc.setHeaderValue(name);
   * tc.setIdentifier(name);
   * if (modelIndex[i] == threadCol)
   * tc.setHeaderRenderer( new ThreadHeaderRenderer(threadCol));
   * else
   * tc.setHeaderRenderer( new SortedHeaderRenderer(name, modelIndex[i]));
   * }
   * 
   * // set the column widths
   * int [] size = (int []) store.get(objectName+"ColumnWidths" );
   * if (size != null)
   * setColumnWidths( size);
   *
   * if ( null != store.get( objectName+"SortOnCol")) {
   * model.sortCol = ((Integer) store.get( objectName+"SortOnCol")).intValue();
   * model.reverse = ((Boolean) store.get( objectName+"SortReverse")).booleanValue();
   * model.sort();
   * }
   * }
   * 
   * public void setFontSize( int size) {
   * jtable.setFont( jtable.getFont().deriveFont( (float) size));
   * }
   */

  /*
   * Save the state in the PersistentStore passed to getState().
   * 
   * public void saveState(PersistentStore store) {
   * if (store == null)
   * return;
   * 
   * int ncols = jtable.getColumnCount();
   * int [] size = new int[ncols];
   * int [] modelIndex = new int[ncols];
   * 
   * TableColumnModel tcm = jtable.getColumnModel();
   * for (int i=0; i<ncols; i++) {
   * TableColumn tc = tcm.getColumn(i);
   * size[i] = tc.getWidth();
   * modelIndex[i] = tc.getModelIndex();
   * }
   * store.put( objectName+"ColumnWidths", size);
   * store.put( objectName+"ColumnOrder", modelIndex);
   * 
   * store.put( objectName+"SortOnCol", new Integer(model.sortCol));
   * store.put( objectName+"SortReverse", new Boolean(model.reverse));
   * 
   * store.put( objectName+"SortReverse", new Boolean(model.reverse));
   * }
   */

  /**
   * Sort the rowList: note rowList changed, not a copy of it.
   * 
   * @param colNo sort on this column
   * @param reverse if true, reverse sort
   */
  public void sort(int colNo, boolean reverse) {
    model.sort(colNo, reverse);
    jtable.setRowSelectionInterval(0, 0);
    ensureRowIsVisible(0);
  }

  /**
   * Replace the rowList with this one.
   * 
   * @param rowList list of rows
   */
  public void setRows(ArrayList<TableRow> rowList) {
    this.rows = rowList;
    if (!rows.isEmpty())
      jtable.setRowSelectionInterval(0, 0);
    else
      jtable.clearSelection();

    model.sort();
    jtable.revalidate();
  }

  /**
   * Remove elem from rowList, update the table.
   * Searches for match using object identity (==)
   * 
   * @param elem which element
   */
  public void removeRow(Object elem) {
    Iterator<TableRow> iter = rows.iterator();
    while (iter.hasNext()) {
      TableRow row = iter.next();
      if (row == elem) {
        iter.remove();
        break;
      }
    }
    jtable.revalidate();
  }

  /** add ListSelectionEvent listener */
  public void addListSelectionListener(ListSelectionListener l) {
    jtable.getSelectionModel().addListSelectionListener(l);
  }

  /** remove ListSelectionEvent listener */
  public void removeListSelectionListener(ListSelectionListener l) {
    jtable.getSelectionModel().removeListSelectionListener(l);
  }

  /** add UIChangeEvent listener */
  public void addUIChangeListener(UIChangeListener l) {
    lm.addListener(l);
  }

  /** remove UIChangeEvent listener */
  public void removeUIChangeListener(UIChangeListener l) {
    lm.removeListener(l);
  }

  // public int getRowCount() { return table.getRowCount(); }
  public int getSelectedRowIndex() {
    return jtable.getSelectedRow();
  } // for SuperComboBox

  public void setSortOK(boolean sortOK) {
    this.sortOK = sortOK;
  } // for SuperComboBox

  /**
   * Get the currently selected row.
   * 
   * @return selected TableRow
   */
  public TableRow getSelected() {
    if (rows.isEmpty())
      return null;
    int sel = jtable.getSelectedRow();
    if (sel >= 0)
      return rows.get(sel);
    else
      return null;
  }

  /**
   * Set the current selection to this row.
   * 
   * @param row index into rowList
   */
  public void setSelected(int row) {
    if ((row < 0) || (row >= rows.size()))
      return;
    if (debug)
      System.out.println("JTableSorted setSelected " + row);
    jtable.setRowSelectionInterval(row, row);
    ensureRowIsVisible(row);
  }

  /**
   * Increment or decrement the current selection by one row.
   * 
   * @param increment true=increment, false=decrement
   */
  public void incrSelected(boolean increment) {
    if (rows.isEmpty())
      return;
    int curr = jtable.getSelectedRow();
    if (increment && (curr < rows.size() - 1))
      setSelected(curr + 1);
    else if (!increment && (curr > 0))
      setSelected(curr - 1);
  }

  /** Get the JTable delegate so you can do nasty things to it */
  public JTable getTable() {
    return jtable;
  }

  /** for each column, get the model index */
  public int[] getModelIndex() {
    int[] modelIndex = new int[colName.length];

    TableColumnModel tcm = jtable.getColumnModel();
    for (int i = 0; i < colName.length; i++) {
      TableColumn tc = tcm.getColumn(i);
      modelIndex[i] = tc.getModelIndex();
    }
    return modelIndex;
  }

  /////////////////////////////////////////////////////////////////////////////////
  private void ensureRowIsVisible(int nRow) {
    Rectangle visibleRect = jtable.getCellRect(nRow, 0, true);
    visibleRect.x = scrollPane.getViewport().getViewPosition().x;
    jtable.scrollRectToVisible(visibleRect);
    jtable.repaint();
  }

  private void setColumnWidths(int[] sizes) {
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i = 0; i < jtable.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      int maxw = ((sizes == null) || (i >= sizes.length)) ? model.getPreferredWidthForColumn(tc) : sizes[i];
      tc.setPreferredWidth(maxw);
    }
    // table.sizeColumnsToFit(0); // must be called due to a JTable bug
  }

  private void setSortCol(int sortCol, boolean reverse) {
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i = 0; i < jtable.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      SortedHeaderRenderer shr = (SortedHeaderRenderer) tc.getHeaderRenderer();
      shr.setSortCol(sortCol, reverse);
    }
  }

  private class PopupAction extends AbstractAction {
    private final String id;
    private TableColumn tc;

    PopupAction(String id) {
      this.id = id;
    }

    public void actionPerformed(ActionEvent e) {
      boolean state = (Boolean) getValue(BAMutil.STATE);
      TableColumnModel tcm = jtable.getColumnModel();

      if (state)
        tcm.addColumn(tc);
      else
        hideColumn();

      JTableSorted.this.revalidate();
    }

    public void hideColumn() {
      TableColumnModel tcm = jtable.getColumnModel();
      int idx = tcm.getColumnIndex(id);
      tc = tcm.getColumn(idx);
      tcm.removeColumn(tc);
    }
  }

  private class TableRowModel extends AbstractTableModel {
    private boolean reverse;
    private int sortCol = -1;

    // AbstractTableModel methods
    public int getRowCount() {
      return rows.size();
    }

    public int getColumnCount() {
      return colName.length;
    }

    public String getColumnName(int col) {
      return colName[col];
    }

    public Object getValueAt(int row, int col) {
      TableRow selectedRow = rows.get(row);

      if (col == threadCol) {
        if (null == threadSorter)
          return "";
        else
          return threadSorter.isTopThread(selectedRow) ? " * " : ""; // ??
      }

      return selectedRow.getValueAt(col);
    }

    // sort using current
    void sort() {
      sort(sortCol, reverse);
    }

    void sort(int sortCol) {
      if (sortCol == this.sortCol)
        reverse = (!reverse);
      else
        reverse = false;
      sort(sortCol, reverse);
    }

    void sort(int sortCol, boolean reverse) {
      this.reverse = reverse;
      if ((sortCol == threadCol) && (threadSorter != null)) {
        rows = threadSorter.sort(sortCol, reverse, rows);
      } else if (sortCol >= 0) {
        rows.sort(new SortList(sortCol, reverse));
      }
      JTableSorted.this.setSortCol(sortCol, reverse);
      this.sortCol = sortCol; // keep track of last sort
    }

    // trying to get the auto - size right: not very successful
    public int getPreferredWidthForColumn(TableColumn col) {
      int hw = columnHeaderWidth(col); // hw = header width
      int cw = widestCellInColumn(col); // cw = column width
      return Math.max(hw, cw);
    }

    private int columnHeaderWidth(TableColumn col) {
      TableCellRenderer renderer = col.getHeaderRenderer();
      if (renderer == null)
        return 10;
      Component comp = renderer.getTableCellRendererComponent(jtable, col.getHeaderValue(), false, false, 0, 0);

      return comp.getPreferredSize().width;
    }

    private int widestCellInColumn(TableColumn col) {
      int c = col.getModelIndex(), width, maxw = 0;

      for (int r = 0; r < getRowCount(); ++r) {
        TableCellRenderer renderer = jtable.getCellRenderer(r, c);
        Component comp = renderer.getTableCellRendererComponent(jtable, getValueAt(r, c), false, false, r, c);
        width = comp.getPreferredSize().width;
        maxw = Math.max(width, maxw);
      }
      return maxw;
    }
  }

  private static class SortList implements Comparator<TableRow>, Serializable {
    private final int col;
    private final boolean reverse;

    SortList(int col, boolean reverse) {
      this.col = col;
      this.reverse = reverse;
    }

    public int compare(TableRow row1, TableRow row2) {
      return reverse ? row2.compare(row1, col) : row1.compare(row2, col);
    }
  }

  // add tooltips
  private static class MyTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
        int row, int column) {

      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      if ((c instanceof JComponent) && (value instanceof NamedObject)) {
        ((JComponent) c).setToolTipText(((NamedObject) value).getDescription());
      } // TODO should turn tip off if there is none
      return c;
    }

    public Point getToolTipLocation(MouseEvent e) {
      return e.getPoint();
    }
  }

  // add tooltips
  private static class MyJTable extends javax.swing.JTable {
    public Point getToolTipLocation(MouseEvent e) {
      return e.getPoint();
    }
  }

  private static class SortedHeaderRenderer implements TableCellRenderer {
    int modelCol;
    Component comp;

    JPanel compPanel;
    JLabel upLabel, downLabel;
    boolean hasSortIndicator;
    boolean reverse;

    protected SortedHeaderRenderer(int modelCol) {
      this.modelCol = modelCol;
    }

    SortedHeaderRenderer(String name, int modelCol) {
      this.modelCol = modelCol;
      upLabel = new JLabel(sortUpIcon);
      downLabel = new JLabel(sortDownIcon);

      compPanel = new JPanel(new BorderLayout());
      compPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
      compPanel.add(new JLabel(name), BorderLayout.CENTER);
      comp = compPanel;
    }

    void setSortCol(int sortCol, boolean reverse) {
      if (sortCol == modelCol) {

        if (!hasSortIndicator)
          compPanel.add(reverse ? upLabel : downLabel, BorderLayout.EAST);
        else if (reverse != this.reverse) {
          compPanel.remove(1);
          compPanel.add(reverse ? upLabel : downLabel, BorderLayout.EAST);
        }

        this.reverse = reverse;
        hasSortIndicator = true;

      } else if (hasSortIndicator) {
        compPanel.remove(1);
        hasSortIndicator = false;
      }
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
        int row, int column) {

      return comp;
    }

  }

  private static class ThreadHeaderRenderer extends SortedHeaderRenderer {
    JPanel sort, unsort;

    ThreadHeaderRenderer(int modelCol) {
      super(modelCol);

      sort = new JPanel(new BorderLayout());
      sort.setBorder(new BevelBorder(BevelBorder.RAISED));
      sort.add(new JLabel(threadSortIcon), BorderLayout.CENTER);

      unsort = new JPanel(new BorderLayout());
      unsort.setBorder(new BevelBorder(BevelBorder.RAISED));
      unsort.add(new JLabel(threadUnSortIcon), BorderLayout.CENTER);

      comp = unsort;
    }

    void setSortCol(int sortCol, boolean reverse) {
      comp = (sortCol == modelCol) ? sort : unsort;
    }

  }

}
