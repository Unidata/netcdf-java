/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import ucar.array.ArrayType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisDependenceType;
import ucar.nc2.grid2.GridAxisSpacing;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.ui.prefs.BeanTable;
import ucar.ui.util.NamedObject;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.geoloc.Projection;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/** Bean Table for GridNew */
public class GridNewTable extends JPanel {
  private final PreferencesExt prefs;

  private final BeanTable<DatasetBean> dsTable;
  private final BeanTable<GridBean> covTable;
  private final BeanTable<CoordSysBean> csysTable;
  private final BeanTable<AxisBean> axisTable;
  private final JSplitPane split, split2, split3;
  private final TextHistoryPane infoTA;
  private final IndependentWindow infoWindow;

  private GridDataset gridDataset;

  public GridNewTable(PreferencesExt prefs) {
    this.prefs = prefs;

    dsTable = new BeanTable<>(DatasetBean.class, (PreferencesExt) prefs.node("GridDataset"), false, "GridDataset",
        "ucar.nc2.grid.GridDataset", null);
    dsTable.addListSelectionListener(e -> {
      DatasetBean pb = dsTable.getSelectedBean();
      if (pb != null) {
        setDataset(pb.gdataset);
      }
    });

    covTable = new BeanTable<>(GridBean.class, (PreferencesExt) prefs.node("GridBeans"), false, "Grids",
        "ucar.nc2.grid.Grid", new GridBean());

    csysTable = new BeanTable<>(CoordSysBean.class, (PreferencesExt) prefs.node("CoordSysBeans"), false,
        "GridCoordinateSystems", "ucar.nc2.grid.GridCoordinateSystem", null);
    csysTable.addListSelectionListener(e -> {
      CoordSysBean bean = csysTable.getSelectedBean();
      if (null != bean) { // find the coverages
        List<GridBean> result = new ArrayList<>();
        for (GridBean covBean : covTable.getBeans()) {
          if (covBean.getCoordSysName().equals(bean.getName()))
            result.add(covBean);
        }
        covTable.setSelectedBeans(result);
      }
    });

    axisTable = new BeanTable<>(AxisBean.class, (PreferencesExt) prefs.node("AxisBeans"), false, "GridAxes",
        "ucar.nc2.grid.GridAxis", null);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // layout
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, dsTable, covTable);
    split.setDividerLocation(prefs.getInt("splitPos", 300));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, csysTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 200));

    split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, axisTable);
    split3.setDividerLocation(prefs.getInt("splitPos3", 200));

    setLayout(new BorderLayout());
    add(split3, BorderLayout.CENTER);

    // context menu
    JTable jtable;

    jtable = dsTable.getJTable();
    PopupMenu dsPopup = new PopupMenu(jtable, "Options");
    dsPopup.addAction("Show GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean bean = dsTable.getSelectedBean();
        if (bean != null) {
          infoTA.clear();
          infoTA.appendLine(bean.gdataset.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    jtable = covTable.getJTable();
    PopupMenu csPopup = new PopupMenu(jtable, "Options");
    csPopup.addAction("Show Grid", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GridBean vb = covTable.getSelectedBean();
        if (vb != null) {
          infoTA.clear();
          infoTA.appendLine(vb.geogrid.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    jtable = csysTable.getJTable();
    csPopup = new PopupMenu(jtable, "Options");
    csPopup.addAction("Show GridCoordinateSystem", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordSysBean bean = csysTable.getSelectedBean();
        if (bean != null) {
          infoTA.clear();
          infoTA.appendLine(bean.gcs.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    jtable = axisTable.getJTable();
    csPopup = new PopupMenu(jtable, "Options");
    csPopup.addAction("Show GridAxis", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = axisTable.getSelectedBean();
        if (bean != null) {
          infoTA.clear();
          infoTA.appendLine(bean.axis.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });
    /*
     * csPopup.addAction("Show Coord Value differences", new AbstractAction() {
     * public void actionPerformed(ActionEvent e) {
     * AxisBean bean = axisTable.getSelectedBean();
     * if (bean != null) {
     * infoTA.clear();
     * infoTA.appendLine(bean.showCoordValueDiffs());
     * infoTA.gotoTop();
     * infoWindow.show();
     * }
     * }
     * });
     * 
     */
  }

  public void clear() {
    dsTable.clearBeans();
    covTable.clearBeans();
    csysTable.clearBeans();
    axisTable.clearBeans();
  }

  public void save() {
    dsTable.saveState(false);
    covTable.saveState(false);
    csysTable.saveState(false);
    axisTable.saveState(false);

    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
    prefs.putInt("splitPos3", split3.getDividerLocation());
  }

  public void showInfo(Formatter result) {
    gridDataset.toString(result);
  }

  public void setGridDataset(GridDataset gds) {
    clear();
    this.gridDataset = gds;
    List<DatasetBean> dsList = ImmutableList.of(new DatasetBean(gds));
    dsTable.setBeans(dsList);
  }

  public GridDataset getGridCollection() {
    return this.gridDataset;
  }

  public void setDataset(GridDataset gridDataset) {

    List<GridBean> beanList = new ArrayList<>();
    for (Grid g : gridDataset.getGrids()) {
      beanList.add(new GridBean(g));
    }
    covTable.setBeans(beanList);

    List<CoordSysBean> csList = new ArrayList<>();
    for (GridCoordinateSystem gcs : gridDataset.getGridCoordinateSystems()) {
      csList.add(new CoordSysBean(gcs));
    }
    csysTable.setBeans(csList);

    List<AxisBean> axisList = new ArrayList<>();
    for (GridAxis axis : gridDataset.getGridAxes()) {
      axisList.add(new AxisBean(axis));
    }
    axisTable.setBeans(axisList);
  }

  private boolean contains(List<AxisBean> axisList, String name) {
    for (AxisBean axis : axisList)
      if (axis.getName().equals(name))
        return true;
    return false;
  }

  public List<GridBean> getCoverageBeans() {
    return covTable.getBeans();
  }

  public List<String> getSelectedGrids() {
    List<GridBean> grids = covTable.getSelectedBeans();
    List<String> result = new ArrayList<>();
    for (GridBean gbean : grids) {
      result.add(gbean.getName());
    }
    return result;
  }

  public static class DatasetBean {
    GridDataset gdataset;

    public DatasetBean() {}

    public DatasetBean(GridDataset cds) {
      this.gdataset = cds;
    }

    public String getName() {
      return gdataset.getName();
    }

    public int getNGrids() {
      return Iterables.size(gdataset.getGrids());
    }

    public int getNCooordSys() {
      return Iterables.size(gdataset.getGridCoordinateSystems());
    }

    public int getNAxes() {
      return Iterables.size(gdataset.getGridAxes());
    }
  }


  public static class GridBean implements NamedObject {

    public String hiddenProperties() { // for BeanTable
      return "value";
    }

    Grid geogrid;
    String name, desc, units, coordSysName;
    ArrayType dataType;

    // no-arg constructor
    public GridBean() {}

    // create from a dataset
    public GridBean(Grid geogrid) {
      this.geogrid = geogrid;
      name = geogrid.getName();
      desc = geogrid.getDescription();
      units = geogrid.getUnits();
      dataType = geogrid.getArrayType();
      coordSysName = geogrid.getCoordinateSystem().getName();
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return desc;
    }

    @Override
    public Object getValue() {
      return geogrid;
    }

    public String getUnits() {
      return units;
    }

    public String getCoordSysName() {
      return coordSysName;
    }

    public ArrayType getArrayType() {
      return dataType;
    }

    public String getShape() {
      Formatter f = new Formatter();
      geogrid.getCoordinateSystem().getNominalShape().forEach(i -> f.format("%s,", i));
      return f.toString();
    }
  }

  public static class CoordSysBean {
    private GridCoordinateSystem gcs;
    private GridTimeCoordinateSystem tcs;
    private int nIndAxis;

    // no-arg constructor
    public CoordSysBean() {}

    public CoordSysBean(GridCoordinateSystem gcs) {
      this.gcs = gcs;
      this.tcs = gcs.getTimeCoordinateSystem();

      for (GridAxis<?> axis : gcs.getGridAxes()) {
        if (axis.getDependenceType() == GridAxisDependenceType.independent) {
          nIndAxis++;
        }
      }
    }

    public String getName() {
      return gcs.getName();
    }

    public int getNIndCoords() {
      return nIndAxis;
    }

    public String getRuntime() {
      return this.tcs.getRunTimeAxis() == null ? "" : this.tcs.getRunTimeAxis().getName();
    }

    public String getTimeOffset() {
      return this.tcs.getTimeOffsetAxis(0) == null ? "" : this.tcs.getTimeOffsetAxis(0).getName();
    }

    public String getEns() {
      return this.gcs.getEnsembleAxis() == null ? "" : this.gcs.getEnsembleAxis().getName();
    }

    public String getVert() {
      return this.gcs.getVerticalAxis() == null ? "" : this.gcs.getVerticalAxis().getName();
    }

    public String getProjection() {
      Projection p = this.gcs.getHorizCoordinateSystem().getProjection();
      return p == null ? "" : p.getName();
    }

    public String getTimeType() {
      if (this.tcs == null) {
        return "";
      }
      return this.tcs.getType() == null ? "not set" : tcs.getType().toString();
    }
  }

  public static class AxisBean {
    GridAxis<?> axis;
    String name, desc, units;
    AxisType axisType;

    // no-arg constructor
    public AxisBean() {}

    // create from a dataset
    public AxisBean(GridAxis<?> v) {
      this.axis = v;

      name = v.getName();
      axisType = v.getAxisType();
      units = v.getUnits();
      desc = v.getDescription();
    }

    public String getName() {
      return name;
    }

    public String getAxisType() {
      return axisType == null ? "" : axisType.name();
    }

    public String getDescription() {
      return desc;
    }

    public String getUnits() {
      return units;
    }

    public String getSpacing() {
      GridAxisSpacing sp = axis.getSpacing();
      return (sp == null) ? "" : sp.toString();
    }

    public int getSize() {
      return axis.getNominalSize();
    }

    public boolean isRegular() {
      return axis.isRegular();
    }

    public boolean isInterval() {
      return axis.isInterval();
    }

    public double getResolution() {
      return axis.getResolution();
    }

    public String getDependenceType() {
      return axis.getDependenceType().toString();
    }

    /*
     * String showCoordValueDiffs() {
     * Formatter f = new Formatter();
     * switch (axis.getSpacing()) {
     * case regularInterval:
     * case regularPoint:
     * f.format("%n%s resolution=%f%n", axis.getSpacing(), axis.getResolution());
     * break;
     * 
     * case irregularPoint:
     * case contiguousInterval:
     * double[] values = axis.getValues();
     * int n = values.length;
     * f.format("%n%s (npts=%d)%n", axis.getSpacing(), n);
     * for (int i = 0; i < n - 1; i++) {
     * double diff = values[i + 1] - values[i];
     * f.format("%10f %10f == %10f%n", values[i], values[i + 1], diff);
     * }
     * f.format("%n");
     * break;
     * 
     * case discontiguousInterval:
     * values = axis.getValues();
     * n = values.length;
     * f.format("%ndiscontiguous intervals (npts=%d)%n", n);
     * for (int i = 0; i < n; i += 2) {
     * double diff = values[i + 1] - values[i];
     * f.format("(%10f,%10f) = %10f%n", values[i], values[i + 1], diff);
     * }
     * f.format("%n");
     * break;
     * }
     * return f.toString();
     * }
     * 
     */
  }

  /**
   * Wrap this in a JDialog component.
   *
   * @param parent JFrame (application) or JApplet (applet) or null
   * @param title dialog window title
   * @param modal modal dialog or not
   * @return JDialog
   */
  public JDialog makeDialog(RootPaneContainer parent, String title, boolean modal) {
    return new Dialog(parent, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof Frame ? (Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener(e -> {
        if (e.getPropertyName().equals("lookAndFeel"))
          SwingUtilities.updateComponentTreeUI(Dialog.this);
      });

      // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add(GridNewTable.this, BorderLayout.CENTER);
      pack();
    }
  }
}
