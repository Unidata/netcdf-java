/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.grid;

import ucar.nc2.Dimension;
import ucar.nc2.dt.GridDatatype;
import ucar.ui.event.ActionSourceListener;
import ucar.ui.event.ActionValueEvent;
import ucar.ui.table.JTableSorted;
import ucar.ui.table.TableRow;
import ucar.ui.table.TableRowAbstract;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Formatter;

/**
 * Put the fields of a GridDatatype dataset in a JTable.
 * Uses ActionSourceListener for events.
 *
 * @see ucar.ui.event.ActionSourceListener
 */
public class GridTable {
  private static final boolean debug = false;

  private final JTableSorted table;
  private ArrayList<TableRow> tableRows;
  private final ActionSourceListener actionSource;
  private boolean eventOK = true;

  public GridTable(String actionName) {
    // the main delegate
    table = new JTableSorted(colName, tableRows);

    // event management
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        if (tableRows == null)
          return;
        String want = e.getValue().toString();
        int count = 0;
        for (TableRow row : tableRows) {
          if (want.equals(((Row) row).gg.getFullName())) {
            eventOK = false;
            table.setSelected(count);
            eventOK = true;
            break;
          }
          count++;
        }
      }
    };

    // send event when selected row changes
    table.addListSelectionListener(e -> {
      if (eventOK && !e.getValueIsAdjusting()) {
        // new variable is selected
        Row row = (Row) table.getSelected();
        if (row != null) {
          if (debug)
            System.out.println(" GridTable new gg = " + row.gg.getFullName());
          actionSource.fireActionValueEvent(ActionSourceListener.SELECTED, row.gg.getFullName());
        }
      }
    });
  }

  public void clear() {
    tableRows.clear();
    table.setRows(tableRows);
  }

  public void setDataset(java.util.List<GridDatatype> fields) {
    if (fields == null)
      return;

    tableRows = new ArrayList<>(fields.size());
    for (GridDatatype gg : fields)
      tableRows.add(new Row(gg));

    table.setRows(tableRows);
  }

  public JPanel getPanel() {
    return table;
  }

  /* better way to do event management */
  public ActionSourceListener getActionSourceListener() {
    return actionSource;
  }

  private static final String[] colName = {"Name", "Dimensions", "Units", "Long Name"};

  private static class Row extends TableRowAbstract {
    GridDatatype gg;
    String dims;

    Row(GridDatatype gg) {
      this.gg = gg;
      Formatter f = new Formatter();
      for (Dimension dim : gg.getDimensions())
        f.format("%s ", dim.getShortName());
      dims = f.toString();
    }

    public Object getUserObject() {
      return gg;
    }

    public Object getValueAt(int col) {
      switch (col) {
        case 0:
          return gg.getFullName();
        case 1:
          return dims;
        case 2:
          return gg.getUnitsString();
        case 3:
          return gg.getDescription();
        default:
          return "error";
      }
    }
  }

}
