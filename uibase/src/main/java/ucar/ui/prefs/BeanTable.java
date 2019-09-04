/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ui.table.HidableTableColumnModel;
import ucar.ui.table.TableAligner;
import ucar.ui.table.TableAppearanceAction;
import ucar.ui.table.UndoableRowSorter;
import ucar.ui.widget.MultilineTooltip;
import ucar.util.prefs.PreferencesExt;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * A JTable that uses JavaBeans to store the data.
 * <p/>
 * The columns of the JTable are the Properties of the Javabean, found through introspection.
 * <p/>
 * The properties may be editable if they have type primitive or String. and you list
 * the editable properties in a static method editableProperties() in the bean, eg :
 * 
 * <pre>
 * static public String editableProperties() {
 *   return "ID serverName active writeDirectory";
 * }
 * </pre>
 * 
 * or as an instance method with a no parameter constructor
 * 
 * <pre>
 * MyClass() {}
 * 
 * public String editableProperties() {
 *   return "ID serverName active writeDirectory";
 * }
 * </pre>
 * <p/>
 * You may hide properties by listing them in a static method hiddenProperties() in the bean, eg :
 * 
 * <pre>
 * static public String hiddenProperties() {
 *   return "hideThisProperty DDDirectory";
 * }
 * </pre>
 * 
 * * or as an instance method with a no parameter constructor
 * 
 * <pre>
 * MyClass() {}
 * 
 * public String hiddenProperties() {
 *   return "ID serverName active writeDirectory";
 * }
 * </pre>
 * <p/>
 * The data can be made persistent through a PreferencesExt store.
 * The width and order of the columns is persistent.
 * The javabean class may add or delete properties, and the stored data will be reasonably intact.
 *
 * @author John Caron
 */

public class BeanTable<T> extends JPanel {
  private static final Logger logger = LoggerFactory.getLogger(BeanTable.class);

  protected Class<T> beanClass;
  protected T innerbean;
  protected PreferencesExt store;
  protected JTable jtable;
  protected JScrollPane scrollPane;

  protected ArrayList<T> beans;
  protected TableBeanModel model;

  protected boolean debug, debugStore, debugBean, debugSelected;

  public BeanTable(Class<T> bc, PreferencesExt pstore, boolean canAddDelete) {
    this(bc, pstore, canAddDelete, null, null, null);
  }

  public BeanTable(Class<T> bc, PreferencesExt pstore, String header, String tooltip, BeanInfo info) {
    this.beanClass = bc;
    this.store = pstore;

    beans = (store != null) ? (ArrayList<T>) store.getBean("beanList", new ArrayList<>()) : new ArrayList<>();
    model = new TableBeanModelInfo(info);
    init(header, tooltip);
  }

  /**
   * Constructor.
   *
   * @param bc JavaBean class
   * @param pstore store data in this PreferencesExt store.
   * @param canAddDelete allow changes to the jtable - adds a New and Delete button to bottom panel
   * @param header optional header label
   * @param tooltip optional tooltip label
   * @param bean needed for inner classes to call reflected methods on
   */
  public BeanTable(Class<T> bc, PreferencesExt pstore, boolean canAddDelete, String header, String tooltip, T bean) {
    this.beanClass = bc;
    this.store = pstore;
    this.innerbean = bean;

    beans = (store != null) ? (ArrayList<T>) store.getBean("beanList", new ArrayList()) : new ArrayList<>();
    model = new TableBeanModel(beanClass);
    init(header, tooltip);

    if (canAddDelete) {
      // button panel
      JPanel buttPanel = new JPanel();
      JButton newButton = new JButton("New");
      buttPanel.add(newButton, null);
      JButton deleteButton = new JButton("Delete");
      buttPanel.add(deleteButton, null);

      add(buttPanel, BorderLayout.SOUTH);

      // button listeners
      newButton.addActionListener(e -> {
        try {
          T newbean = beanClass.newInstance();
          addBean(newbean);
        } catch (Exception exc) {
          exc.printStackTrace();
        }
      });

      deleteButton.addActionListener(e -> {
        if (JOptionPane.showConfirmDialog(null, "Do you want to delete all selected records", "Delete Records",
            JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
          return;

        for (T o : getSelectedBeans()) {
          beans.remove(o);
        }
        model.fireTableDataChanged();
      });
    }
  }

  public void setHeader(String header) {
    headerLabel.setText(header);
  }

  private JLabel headerLabel;

  private void init(String header, String tooltip) {
    TableColumnModel tcm = new HidableTableColumnModel(model);
    jtable = new JTable(model, tcm);
    jtable.setRowSorter(new UndoableRowSorter<>(model));

    // jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); default = multiple
    jtable.setDefaultRenderer(java.util.Date.class, new DateRenderer());

    ToolTipManager.sharedInstance().registerComponent(jtable);

    restoreState();

    // editor/renderers
    jtable.setDefaultEditor(String.class, new DefaultCellEditor(new JTextField()));
    jtable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JCheckBox()));


    // Fixes this bug: http://stackoverflow.com/questions/6601994/jtable-boolean-cell-type-background
    ((JComponent) jtable.getDefaultRenderer(Boolean.class)).setOpaque(true);

    // Left-align every cell, including header cells.
    TableAligner aligner = new TableAligner(jtable, SwingConstants.LEADING);
    jtable.getColumnModel().addColumnModelListener(aligner);

    // Create a button that will popup a menu containing options to configure the appearance of the table.
    JButton cornerButton = new JButton(new TableAppearanceAction(jtable));
    cornerButton.setHideActionText(true);
    cornerButton.setContentAreaFilled(false);

    // Install the button in the upper-right corner of the table's scroll pane.
    scrollPane = new JScrollPane(jtable);
    scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    // This keeps the corner button visible even when the table is empty (or all columns are hidden).
    scrollPane.setColumnHeaderView(new JViewport());
    scrollPane.getColumnHeader().setPreferredSize(jtable.getTableHeader().getPreferredSize());

    // UI
    setLayout(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);

    if (header != null) {
      if (tooltip != null) {
        headerLabel = new JLabel(header, SwingConstants.CENTER) {
          public JToolTip createToolTip() {
            return new MultilineTooltip();
          }
        };
        headerLabel.setToolTipText(tooltip);
      } else {
        headerLabel = new JLabel(header, SwingConstants.CENTER);
      }
      add(headerLabel, BorderLayout.NORTH);
    }

    // event management
    listenerList = new EventListenerList();

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

  }

  // debug
  public String getToolTipText(MouseEvent event) {
    return super.getToolTipText(event);
  }

  public void setProperty(String propertyName, String displayName, String toolTipText) {
    model.setProperty(propertyName, displayName, toolTipText);
  }

  public void setPropertyEditable(String propertyName, boolean isHidden) {

  }

  public void setPropertyHidden(String propertyName, boolean isHidden) {

  }

  /**
   * Add listener: ListSelectionEvent sent when a new row is selected
   */
  public void addListSelectionListener(ListSelectionListener l) {
    listenerList.add(javax.swing.event.ListSelectionListener.class, l);
  }

  /**
   * Remove listener
   */
  public void removeListSelectionListener(ListSelectionListener l) {
    listenerList.remove(javax.swing.event.ListSelectionListener.class, l);
  }

  private void fireEvent(javax.swing.event.ListSelectionEvent event) {
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first
    for (int i = listeners.length - 2; i >= 0; i -= 2)
      ((javax.swing.event.ListSelectionListener) listeners[i + 1]).valueChanged(event);
  }

  /**
   * Get the currently selected bean, or null if none selected.
   *
   * @return the currently selected bean, or null if none selected
   */
  @Nullable
  public T getSelectedBean() {
    int viewRowIndex = jtable.getSelectedRow();
    if (viewRowIndex < 0)
      return null;
    int modelRowIndex = jtable.convertRowIndexToModel(viewRowIndex);
    return (modelRowIndex < 0) || (modelRowIndex >= beans.size()) ? null : beans.get(modelRowIndex);
  }

  /**
   * Get the currently selected beans. Use this for multiple selection
   *
   * @return ArrayList of currently selected beans (wont be null).
   * @see #setSelectionMode
   */
  public List<T> getSelectedBeans() {
    ArrayList<T> list = new ArrayList<>();
    int[] viewRowIndices = jtable.getSelectedRows();
    for (int viewRowIndex : viewRowIndices) {
      int modelRowIndex = jtable.convertRowIndexToModel(viewRowIndex);
      list.add(beans.get(modelRowIndex));
      if (debugSelected)
        System.out.println(" bean selected= " + modelRowIndex + " " + beans.get(modelRowIndex));
    }
    return list;
  }

  /**
   * Get the currently selected cells.
   * Use this for multiple row selection, when columnSelection is on
   *
   * @return ArrayList of currently selected cells (wont be null).
   * @see BeanTable#setSelectionMode(int).
   */
  public ArrayList<Object> getSelectedCells() {
    ArrayList<Object> list = new ArrayList<>();
    int[] viewRowIndices = jtable.getSelectedRows();
    int[] viewColumnIndices = jtable.getSelectedColumns();
    for (int viewRowIndex : viewRowIndices) {
      for (int viewColumnIndex : viewColumnIndices) {
        int modelRowIndex = jtable.convertRowIndexToModel(viewRowIndex);
        int modelColumnIndex = jtable.convertColumnIndexToModel(viewColumnIndex);
        list.add(model.getValueAt(modelRowIndex, modelColumnIndex));
      }
    }

    return list;
  }

  /**
   * Set the currently selected cells (0, false or null).
   * Use this for multiple row selection, when columnSelection is on
   */
  public void clearSelectedCells() {
    int[] viewRowIndices = jtable.getSelectedRows();
    int[] viewColumnIndices = jtable.getSelectedColumns();
    TableColumnModel tcm = jtable.getColumnModel();

    for (int viewColumnIndex : viewColumnIndices) {
      TableColumn tc = tcm.getColumn(viewColumnIndex);
      int modelColumnIndex = tc.getModelIndex();

      Class colClass = jtable.getColumnClass(viewColumnIndex);
      Object zeroValue = model.zeroValue(colClass);
      for (int viewRowIndex : viewRowIndices) {
        int modelRowIndex = jtable.convertRowIndexToModel(viewRowIndex);
        model.setValueAt(zeroValue, modelRowIndex, modelColumnIndex);
      }
    }
  }

  public void addBean(T bean) {
    beans.add(bean);
    int row = beans.size() - 1;
    model.fireTableRowsInserted(row, row);
  }

  public void addBeans(List<T> newBeans) {
    this.beans.addAll(newBeans);
    int row = beans.size() - 1;
    model.fireTableRowsInserted(row - newBeans.size(), row);
  }

  public void setBeans(List<T> beans) {
    this.beans = (beans == null) ? new ArrayList<>() : new ArrayList<>(beans);
    model.fireTableDataChanged(); // this should make the jtable update
    revalidate(); // LOOK sometimes it doesnt, try this
  }

  public void clearBeans() {
    setBeans(null);
  }

  public List<T> getBeans() {
    return beans;
  }

  public JTable getJTable() {
    return jtable;
  }

  public void setFontSize(int size) {
    jtable.setFont(jtable.getFont().deriveFont((float) size));
  }

  /**
   * Set the selection mode on the JTable
   *
   * @param mode : JTable.setSelectionMode
   * @see JTable#setSelectionMode
   */
  public void setSelectionMode(int mode) {
    jtable.setSelectionMode(mode);
  }

  /**
   * Set which row is selected.
   *
   * @param bean select this one; must be in the list.
   */
  public void setSelectedBean(T bean) {
    if (bean == null)
      return;
    int modelRowIndex = beans.indexOf(bean);
    int viewRowIndex = jtable.convertRowIndexToView(modelRowIndex);

    if (viewRowIndex >= 0)
      jtable.getSelectionModel().setSelectionInterval(viewRowIndex, viewRowIndex);
    makeRowVisible(viewRowIndex);
  }

  public void clearSelection() {
    jtable.getSelectionModel().clearSelection();
  }

  /**
   * Set which rows are selected.
   * must also call setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
   *
   * @param want select these
   */
  public void setSelectedBeans(List<T> want) {
    jtable.getSelectionModel().clearSelection();
    for (T bean : want) {
      int modelRowIndex = beans.indexOf(bean);
      int viewRowIndex = jtable.convertRowIndexToView(modelRowIndex);

      if (viewRowIndex >= 0) {
        jtable.getSelectionModel().addSelectionInterval(viewRowIndex, viewRowIndex);
      }
    }
  }

  private void makeRowVisible(int viewRowIndex) {
    Rectangle visibleRect = jtable.getCellRect(viewRowIndex, 0, true);
    if (debugSelected)
      System.out.println("----ensureRowIsVisible = " + visibleRect);
    visibleRect.x = scrollPane.getViewport().getViewPosition().x;
    jtable.scrollRectToVisible(visibleRect);
    jtable.repaint();
  }

  public void refresh() {
    jtable.repaint();
  }

  /**
   * Set the ColumnSelection is allowed (default false)
   *
   * @param b allowed or not
   */
  public void setColumnSelectionAllowed(boolean b) {
    jtable.setColumnSelectionAllowed(b);
  }

  /**
   * Save state to the PreferencesExt.
   */
  public void saveState(boolean saveData) {
    if (store == null)
      return;

    try {
      // save data
      if (saveData) {
        store.putBeanCollection("beanList", beans);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    List<PropertyCol> propCols = new ArrayList<>();
    HidableTableColumnModel tableColumnModel = (HidableTableColumnModel) jtable.getColumnModel();
    Enumeration<TableColumn> columns = tableColumnModel.getColumns(false);

    while (columns.hasMoreElements()) {
      PropertyCol propCol = new PropertyCol();
      TableColumn column = columns.nextElement();
      propCol.setName(column.getIdentifier().toString());
      propCol.setWidth(column.getWidth());
      propCol.setVisible(tableColumnModel.isColumnVisible(column));
      propCols.add(propCol);
    }

    store.putBeanCollection("propertyCol", propCols);
  }

  /**
   * Notifies the TableModel that the data in the specified bean has changed.
   * The TableModel will then fire an event of its own, which its listeners will hear (usually a JTable).
   *
   * @param bean a bean that has changed.
   */
  public void fireBeanDataChanged(T bean) {
    int row = beans.indexOf(bean);
    if (row >= 0) {
      model.fireTableRowsUpdated(row, row);
    }
  }

  /**
   * Restore state from PreferencesExt
   */
  protected void restoreState() {
    if (store == null) {
      return;
    }

    ArrayList propColObjs = (ArrayList) store.getBean("propertyCol", new ArrayList());
    HidableTableColumnModel tableColumnModel = (HidableTableColumnModel) jtable.getColumnModel();
    int newViewIndex = 0;

    for (Object propColObj : propColObjs) {
      PropertyCol propCol = (PropertyCol) propColObj;
      try {
        int currentViewIndex = tableColumnModel.getColumnIndex(propCol.getName()); // May throw IAE.

        TableColumn column = tableColumnModel.getColumn(currentViewIndex);
        column.setPreferredWidth(propCol.getWidth());

        tableColumnModel.moveColumn(currentViewIndex, newViewIndex);
        assert tableColumnModel.getColumn(newViewIndex) == column : "tableColumn wasn't successfully moved.";

        // We must do this last, since moveColumn() only works on visible columns.
        tableColumnModel.setColumnVisible(column, propCol.isVisible());
        if (propCol.isVisible()) {
          ++newViewIndex; // Don't increment for hidden columns.
        }
      } catch (IllegalArgumentException e) {
        logger.debug(String.format("Column named \"%s\" was present in the preferences file but not the dataset.",
            propCol.getName()), e);
      }
    }
  }

  /**
   * Should be private. This is to store the width and visibility for each table column.
   */
  public static class PropertyCol {
    private String name;
    private int width;
    private boolean visible = true;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    public boolean isVisible() {
      return visible;
    }

    public void setVisible(boolean visible) {
      this.visible = visible;
    }
  }

  /**
   * Renderer for Date type
   */
  static class DateRenderer extends DefaultTableCellRenderer {
    private java.text.SimpleDateFormat newForm, oldForm;
    private Date cutoff;

    DateRenderer() {
      oldForm = new java.text.SimpleDateFormat("yyyy MMM dd HH:mm z");
      oldForm.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
      newForm = new java.text.SimpleDateFormat("MMM dd, HH:mm z");
      newForm.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
      Calendar cal = Calendar.getInstance();
      cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
      cal.add(Calendar.YEAR, -1); // "now" time format within a year
      cutoff = cal.getTime();
    }

    public void setValue(Object value) {
      if (value == null)
        setText("");
      else {
        Date date = (Date) value;
        if (date.before(cutoff))
          setText(oldForm.format(date));
        else
          setText(newForm.format(date));
      }
    }
  }

  /**
   * Does the reflection on the bean objects
   */
  protected class TableBeanModel extends AbstractTableModel {
    protected List<PropertyDescriptor> properties = new ArrayList<>();

    protected TableBeanModel() {}

    protected TableBeanModel(Class beanClass) {

      // get bean info
      BeanInfo info;
      try {
        if (!beanClass.isInterface())
          info = Introspector.getBeanInfo(beanClass, Object.class);
        else
          info = Introspector.getBeanInfo(beanClass); // allows interfaces to be beans
      } catch (IntrospectionException e) {
        e.printStackTrace();
        return;
      }

      if (debugBean)
        System.out.println("Bean " + beanClass.getName());

      // see if editableProperties method exists
      String editableProperties = "";
      MethodDescriptor[] mds = info.getMethodDescriptors();
      if (mds == null)
        throw new IllegalStateException("no public methods");

      for (MethodDescriptor md : mds) {
        Method m = md.getMethod();
        if (m != null && m.getName().equals("editableProperties")) {
          try {
            editableProperties = (String) m.invoke(null, (Object[]) null); // try static
            if (debugBean)
              System.out.println(" static editableProperties: " + editableProperties);
          } catch (Exception ee) {

            if (innerbean != null) {
              try {
                editableProperties = (String) m.invoke(innerbean, (Object[]) null); // try non static
                if (debugBean)
                  System.out.println(" editableProperties: " + editableProperties);
              } catch (Exception e2) {
                e2.printStackTrace();
              }

            } else {
              ee.printStackTrace();
            }
          }
        }
      }

      // see if hiddenProperties method exists
      String hiddenProperties = "";
      for (MethodDescriptor md : mds) {
        Method m = md.getMethod();
        assert m != null;

        if (m.getName().equals("hiddenProperties")) {
          try {
            hiddenProperties = (String) m.invoke(null, (Object[]) null);
            if (debugBean)
              System.out.println(" hiddenProperties: " + hiddenProperties);

          } catch (Exception ee) {
            if (innerbean != null) {
              try {
                hiddenProperties = (String) m.invoke(innerbean, (Object[]) null); // try non static
                if (debugBean)
                  System.out.println(" hiddenProperties: " + hiddenProperties);
              } catch (Exception e2) {
                e2.printStackTrace();
              }

            } else {
              System.out.println("BeanTable: Bad hiddenProperties ");
              ee.printStackTrace();
            }
          }
        }
      }

      // properties must have read method, not be hidden
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (PropertyDescriptor pd : pds) {
        if ((pd.getReadMethod() != null) && !isHidden(pd, hiddenProperties)) {
          properties.add(pd);
          // preferred == editable
          setEditable(pd, editableProperties);
        }
      }

      if (debugBean) {
        System.out.println("Properties:");
        System.out.println("  display name  type   read()       write()         editable");
        for (PropertyDescriptor pd : pds) {
          String displayName = pd.getDisplayName();
          String name = pd.getName();
          Class type = pd.getPropertyType();
          Method rm = pd.getReadMethod();
          Method wm = pd.getWriteMethod();
          System.out.println(
              "  " + displayName + " " + name + " " + type.getName() + " " + rm + " " + wm + " " + pd.isPreferred());
        }
      }
    }

    public void setProperty(String propertyName, String displayName, String toolTipText) {
      PropertyDescriptor pd = getProperty(propertyName);
      if (pd != null) {
        if (displayName != null) {
          pd.setDisplayName(displayName);
          JLabel hl = (JLabel) pd.getValue("Header");
          if (hl != null)
            hl.setText(displayName);
        }
        if (toolTipText != null) {
          pd.setShortDescription(toolTipText);
          JComponent jc = (JComponent) pd.getValue("ToolTipComp");
          if (jc != null)
            jc.setToolTipText(toolTipText);
        }
      } else
        System.out.println("BeanTable.setProperty " + beanClass.getName() + " no property named " + propertyName);
    }

    // AbstractTableModel methods
    public int getRowCount() {
      return beans.size();
    }

    public int getColumnCount() {
      return properties.size();
    }

    public String getColumnName(int col) {
      return properties.get(col).getDisplayName();
    }

    public Object getValueAt(int row, int col) {
      Object bean = beans.get(row);
      Object value = "N/A";
      PropertyDescriptor pd = properties.get(col);
      try {
        Method m = pd.getReadMethod();
        value = m.invoke(bean, (Object[]) null);
      } catch (Exception ee) {
        System.out.println(
            "BeanTable: Bad getReadMethod " + row + " " + col + " " + beanClass.getName() + " " + pd.getDisplayName());
        ee.printStackTrace();
      }

      return value;
    }

    // for BeanTable
    public Object getValueAt(Object bean, int col) {
      Object value = "N/A";
      try {
        Method m = properties.get(col).getReadMethod();
        value = m.invoke(bean, (Object[]) null);
      } catch (Exception ee) {
        System.out.println("BeanTable: Bad Bean " + bean + " " + col + " " + beanClass.getName());
        ee.printStackTrace();
      }
      return value;
    }

    // editing

    public Class getColumnClass(int col) {
      return wrapPrimitives(properties.get(col).getPropertyType());
    }

    public boolean isCellEditable(int row, int col) {
      PropertyDescriptor pd = properties.get(col);
      if (!pd.isPreferred())
        return false;
      Class type = pd.getPropertyType();
      return type.isPrimitive() || (type == String.class);
    }

    public void setValueAt(Object value, int row, int col) {
      Object bean = beans.get(row);
      try {
        Object[] params = new Object[1];
        params[0] = value;
        Method m = properties.get(col).getWriteMethod();
        if (m != null)
          m.invoke(bean, params);
      } catch (Exception ee) {
        ee.printStackTrace();
      }

      fireTableCellUpdated(row, col);
    }

    // extra stuff

    protected Class wrapPrimitives(Class c) {
      if (c == boolean.class)
        return Boolean.class;
      else if (c == int.class)
        return Integer.class;
      else if (c == float.class)
        return Float.class;
      else if (c == double.class)
        return Double.class;
      else if (c == short.class)
        return Short.class;
      else if (c == long.class)
        return Long.class;
      else if (c == byte.class)
        return Byte.class;
      else
        return c;
    }

    @Nullable
    protected Object zeroValue(Class c) {
      if (c == Boolean.class)
        return Boolean.FALSE;
      else if (c == Integer.class)
        return 0;
      else if (c == Float.class)
        return (float) 0.0;
      else if (c == Double.class)
        return 0.0;
      else if (c == Short.class)
        return (short) 0;
      else if (c == Long.class)
        return 0L;
      else if (c == Byte.class)
        return (byte) 0;
      else
        return null;
    }

    // return PropertyDescriptor with this property name, return null if not exists
    @Nullable
    protected PropertyDescriptor getProperty(String wantName) {
      for (PropertyDescriptor property : properties) {
        if (property.getName().equals(wantName))
          return property;
      }
      return null;
    }

    // return PropertyDescriptor
    protected PropertyDescriptor getProperty(int idx) {
      return properties.get(idx);
    }

    private ArrayList<String> editP;

    private void setEditable(PropertyDescriptor pd, String editableProperties) {
      if (editP == null) {
        editP = new ArrayList<>();
        StringTokenizer toke = new StringTokenizer(editableProperties);
        while (toke.hasMoreTokens())
          editP.add(toke.nextToken());
      }

      pd.setPreferred(editP.contains(pd.getName()));
    }

    private ArrayList<String> hiddenP;

    private boolean isHidden(PropertyDescriptor pd, String hiddenProperties) {
      if (hiddenP == null) {
        hiddenP = new ArrayList<>();
        StringTokenizer toke = new StringTokenizer(hiddenProperties);
        while (toke.hasMoreTokens())
          hiddenP.add(toke.nextToken());
      }

      return hiddenP.contains(pd.getName());
    }

  }

  protected class TableBeanModelInfo extends TableBeanModel {

    protected TableBeanModelInfo(BeanInfo info) {
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (PropertyDescriptor pd : pds) {
        if (pd.getReadMethod() != null) {
          properties.add(pd);
        }
      }
    }
  }
}
