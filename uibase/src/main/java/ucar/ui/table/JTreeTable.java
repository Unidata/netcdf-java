/*
 * Copyright 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package ucar.ui.table;

import ucar.ui.widget.BAMutil;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * This example shows how to create a simple JTreeTable component,
 * by using a JTree as a renderer (and editor) for the cells in a
 * particular column in the JTable.
 *
 * @version 1.2 10/27/98
 *
 * @author Philip Milne
 * @author Scott Violet
 */
public class JTreeTable extends JTable {
  private static Icon openIcon = BAMutil.getIcon("treeOpen", true);
  private static Icon closedIcon = BAMutil.getIcon("treeClosed", true);
  private static Icon leafIcon = BAMutil.getIcon("treeLeaf", true);

  protected TreeTableCellRenderer tree;
  protected TreeTableModelAdapter tableModel;
  protected TreeTableModelAbstract treeModel;
  protected ListToTreeSelectionModelWrapper selectionModel;

  private boolean debug = false, debugSelection = false;

  public JTreeTable(TreeTableModelAbstract treeTableModel) {
    super();
    treeModel = treeTableModel;

    // Create the tree. It will be used as a renderer and editor.
    tree = new TreeTableCellRenderer(treeTableModel);
    /*
     * tree.addPropertyChangeListener( new PropertyChangeListener() {
     * public void propertyChange(PropertyChangeEvent e) {
     * System.out.println("tree PropertyChangeEvent= "+e.getPropertyName());
     * /*if (e.getPropertyName().equals(JTree.TREE_MODEL_PROPERTY))
     * System.out.println("TREE_MODEL_PROPERTY new= "+e.getNewValue()+
     * " old= "+e.getOldValue());
     * }
     * });
     */

    // Install a tableModel representing the visible rows in the tree.
    tableModel = new TreeTableModelAdapter(treeTableModel, tree);
    /*
     * tableModel.addTableModelListener(new TableModelListener() {
     * public void tableChanged(TableModelEvent e) {
     * System.out.println("TableModelEvent= "+e);
     * }
     * });
     */
    super.setModel(tableModel);

    // Force the JTable and JTree to share their row selection models.
    selectionModel = new ListToTreeSelectionModelWrapper();
    tree.setSelectionModel(selectionModel);
    setSelectionModel(selectionModel.getListSelectionModel());
    selectionModel.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);

    // Install the tree editor renderer and editor.
    setDefaultRenderer(TreeTableModel.class, tree);
    setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
    setAutoCreateColumnsFromModel(false);

    // visuals
    setShowGrid(false); // No grid.
    // putClientProperty("JTree.lineStyle", "Angled");
    // putClientProperty("JTree.lineStyle", "Horizontal");
    setIntercellSpacing(new Dimension(0, 0)); // No intercell spacing

    // And update the height of the trees row to match that of
    // the table.
    if (tree.getRowHeight() < 1) {
      // Metal looks better like this.
      setRowHeight(18);
    }
  }

  TreePath[] getSelectionPaths() {
    return selectionModel.getSelectionPaths();
  }

  TreePath getSelectionPath() {
    return selectionModel.getSelectionPath();
  }

  int setSelectionPath(TreePath path) {
    // selectionModel.setSelectionPath( path);
    int rowno = tree.getRowForPath(path);
    if (debugSelection)
      System.out.println("  getRowForPath = " + rowno + " " + path);
    // selectionModel.getListSelectionModel().setSelectionInterval(rowno, rowno);
    if (rowno >= 0)
      setRowSelectionInterval(rowno, rowno);
    if (debugSelection) {
      int[] rows = getSelectedRows();
      for (int row : rows) {
        System.out.println("  rowSelection is = " + row);
      }
    }
    return rowno;
  }

  JTree getTree() {
    return tree;
  }

  /**
   * Increment or decrement the current selection by one row.
   * 
   * @param increment true=increment, false=decrement
   */
  public int incrSelected(boolean increment) {
    int rowno = tree.getRowForPath(getSelectionPath());
    if (rowno < 0)
      return -1;
    if (increment && (rowno < tree.getRowCount() - 1))
      rowno++;
    else if (!increment && (rowno > 0))
      rowno--;
    setRowSelectionInterval(rowno, rowno);
    if (debug)
      System.out.println("  incrSelected to " + rowno);
    return rowno;
  }


  /**
   * Overridden to message super and forward the method to the tree.
   * Since the tree is not actually in the component hieachy it will
   * never receive this unless we forward it in this manner.
   */
  public void updateUI() {
    super.updateUI();
    if (tree != null) {
      tree.updateUI();
    }
    // Use the tree's default foreground and background colors in the
    // table.
    LookAndFeel.installColorsAndFont(this, "Tree.background", "Tree.foreground", "Tree.font");
  }

  /*
   * Workaround for BasicTableUI anomaly. Make sure the UI never tries to
   * paint the editor. The UI currently uses different techniques to
   * paint the renderers and editors and overriding setBounds() below
   * is not the right thing to do for an editor. Returning -1 for the
   * editing row in this case, ensures the editor is never painted.
   */
  public int getEditingRow() {
    return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;
  }

  public TreePath getPathForRow(int row) {
    return tree.getPathForRow(row);
  }


  /**
   * Overridden to pass the new rowHeight to the tree.
   */
  public void setRowHeight(int rowHeight) {
    super.setRowHeight(rowHeight);
    if (tree != null && tree.getRowHeight() != rowHeight) {
      tree.setRowHeight(getRowHeight());
    }
  }

  public void fireDataChanged() {
    // System.out.println("fireDataChanged");
    // tableModel.delayedFireTableDataChanged();
    // tree.setModel(treeModel);
    // tree.firePropertyChange(JTree.TREE_MODEL_PROPERTY, null, null);

    Object[] path = new Object[1];
    path[0] = treeModel.getRoot();
    treeModel.fireTreeStructureChanged(this, path, null, null);

    // tableModel.fireTableChanged(
    // new TableModelEvent(tableModel, 0, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS));
  }

  public void tableChanged(TableModelEvent e) {
    if (debug)
      System.out.println("tableChanged " + autoCreateColumnsFromModel);
    super.tableChanged(null);
  }

  /*
   * Returns the tree that is being shared between the model.
   * 
   * public JTree getTree() {
   * return tree;
   * }
   */

  /**
   * A TreeCellRenderer that displays a JTree.
   */
  public class TreeTableCellRenderer extends JTree implements TableCellRenderer {
    /** Last table/tree row asked to render. */
    protected int visibleRow;

    public TreeTableCellRenderer(TreeModel model) {
      super(model);
      setRootVisible(false);
      setShowsRootHandles(false);
      setToggleClickCount(1);
      DefaultTreeCellRenderer rend = (DefaultTreeCellRenderer) getCellRenderer();
      rend.setOpenIcon(openIcon);
      rend.setClosedIcon(closedIcon);
      rend.setLeafIcon(null);
    }

    /**
     * updateUI is overridden to set the colors of the Tree's renderer
     * to match that of the table.
     */
    public void updateUI() {
      super.updateUI();
      // Make the tree's cell renderer use the table's cell selection
      // colors.
      TreeCellRenderer tcr = getCellRenderer();
      if (tcr instanceof DefaultTreeCellRenderer) {
        DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
        // For 1.1 uncomment this, 1.2 has a bug that will cause an
        // exception to be thrown if the border selection color is
        // null.
        // dtcr.setBorderSelectionColor(null);
        dtcr.setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));
        dtcr.setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));
      }
    }

    /**
     * Sets the row height of the tree, and forwards the row height to
     * the table.
     */
    public void setRowHeight(int rowHeight) {
      if (rowHeight > 0) {
        super.setRowHeight(rowHeight);
        if (JTreeTable.this != null && JTreeTable.this.getRowHeight() != rowHeight) {
          JTreeTable.this.setRowHeight(getRowHeight());
        }
      }
    }

    /**
     * This is overridden to set the height to match that of the JTable.
     */
    public void setBounds(int x, int y, int w, int h) {
      super.setBounds(x, 0, w, JTreeTable.this.getHeight());
    }

    /**
     * Sublcassed to translate the graphics such that the last visible
     * row will be drawn at 0,0.
     */
    public void paint(Graphics g) {
      g.translate(0, -visibleRow * getRowHeight());
      super.paint(g);
    }

    /**
     * TreeCellRenderer method. Overridden to update the visible row.
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
        int row, int column) {
      if (isSelected)
        setBackground(table.getSelectionBackground());
      else
        setBackground(table.getBackground());

      visibleRow = row;
      return this;
    }
  }


  /**
   * TreeTableCellEditor implementation. Component returned is the
   * JTree.
   */
  public class TreeTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int r, int c) {
      return tree;
    }

    /**
     * Overridden to return false, and if the event is a mouse event
     * it is forwarded to the tree.
     * <p>
     * The behavior for this is debatable, and should really be offered
     * as a property. By returning false, all keyboard actions are
     * implemented in terms of the table. By returning true, the
     * tree would get a chance to do something with the keyboard
     * events. For the most part this is ok. But for certain keys,
     * such as left/right, the tree will expand/collapse where as
     * the table focus should really move to a different column. Page
     * up/down should also be implemented in terms of the table.
     * By returning false this also has the added benefit that clicking
     * outside of the bounds of the tree node, but still in the tree
     * column will select the row, whereas if this returned true
     * that wouldn't be the case.
     * <p>
     * By returning false we are also enforcing the policy that
     * the tree will never be editable (at least by a key sequence).
     */
    public boolean isCellEditable(EventObject e) {
      if (e instanceof MouseEvent) {
        for (int counter = getColumnCount() - 1; counter >= 0; counter--) {
          if (getColumnClass(counter) == TreeTableModel.class) {
            MouseEvent me = (MouseEvent) e;
            MouseEvent newME = new MouseEvent(tree, me.getID(), me.getWhen(), me.getModifiers(),
                me.getX() - getCellRect(0, counter, true).x, me.getY(), me.getClickCount(), me.isPopupTrigger());
            tree.dispatchEvent(newME);
            break;
          }
        }
      }
      return false;
    }
  }


  /**
   * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
   * to listen for changes in the ListSelectionModel it maintains. Once
   * a change in the ListSelectionModel happens, the paths are updated
   * in the DefaultTreeSelectionModel.
   */
  class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
    /** Set to true when we are updating the ListSelectionModel. */
    protected boolean updatingListSelectionModel;

    public ListToTreeSelectionModelWrapper() {
      super();
      getListSelectionModel().addListSelectionListener(createListSelectionListener());
    }

    /**
     * Returns the list selection model. ListToTreeSelectionModelWrapper
     * listens for changes to this model and updates the selected paths
     * accordingly.
     */
    ListSelectionModel getListSelectionModel() {
      return listSelectionModel;
    }

    /**
     * This is overridden to set <code>updatingListSelectionModel</code>
     * and message super. This is the only place DefaultTreeSelectionModel
     * alters the ListSelectionModel.
     */
    public void resetRowSelection() {
      if (!updatingListSelectionModel) {
        updatingListSelectionModel = true;
        try {
          super.resetRowSelection();
        } finally {
          updatingListSelectionModel = false;
        }
      }
      // Notice how we don't message super if
      // updatingListSelectionModel is true. If
      // updatingListSelectionModel is true, it implies the
      // ListSelectionModel has already been updated and the
      // paths are the only thing that needs to be updated.
    }

    /**
     * Creates and returns an instance of ListSelectionHandler.
     */
    protected ListSelectionListener createListSelectionListener() {
      return new ListSelectionHandler();
    }

    /**
     * If <code>updatingListSelectionModel</code> is false, this will
     * reset the selected paths from the selected rows in the list
     * selection model.
     */
    protected void updateSelectedPathsFromSelectedRows() {
      if (!updatingListSelectionModel) {
        updatingListSelectionModel = true;
        try {
          // This is way expensive, ListSelectionModel needs an
          // enumerator for iterating.
          int min = listSelectionModel.getMinSelectionIndex();
          int max = listSelectionModel.getMaxSelectionIndex();

          clearSelection();
          if (min != -1 && max != -1) {
            for (int counter = min; counter <= max; counter++) {
              if (listSelectionModel.isSelectedIndex(counter)) {
                TreePath selPath = tree.getPathForRow(counter);

                // System.out.println("JTreeTable selected = "+counter+ " "+selPath);

                if (selPath != null)
                  addSelectionPath(selPath);

                /*
                 * TreePath[] paths = getSelectionPaths();
                 * for (int i=0; i<paths.length; i++)
                 * System.out.println(" "+paths[i]);
                 */

              }
            }
          }
        } finally {
          updatingListSelectionModel = false;
        }
      }
    }

    /**
     * Class responsible for calling updateSelectedPathsFromSelectedRows
     * when the selection of the list changse.
     */
    class ListSelectionHandler implements ListSelectionListener {
      public void valueChanged(ListSelectionEvent e) {
        updateSelectedPathsFromSelectedRows();
      }
    }
  }
}
