/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.coverage2;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.ui.widget.*;
import ucar.ui.widget.PopupMenu;
import ucar.nc2.util.NamedObject;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/** Bean Table for Coverage */
public class CoverageTable extends JPanel {
  private final PreferencesExt prefs;

  private final BeanTable<DatasetBean> dsTable;
  private final BeanTable<CoverageBean> covTable;
  private final BeanTable<CoordSysBean> csysTable;
  private final BeanTable<AxisBean> axisTable;
  private final JSplitPane split, split2, split3;
  private final TextHistoryPane infoTA;
  private final IndependentWindow infoWindow;

  private FeatureDatasetCoverage coverageCollection;
  private CoverageCollection currDataset;

  public CoverageTable(JPanel buttPanel, PreferencesExt prefs) {
    this.prefs = prefs;

    dsTable = new BeanTable<>(DatasetBean.class, (PreferencesExt) prefs.node("DatasetBeans"), false, "CoverageDatasets",
        "ucar.nc2.ft2.coverage.CoverageDataset", null);
    dsTable.addListSelectionListener(e -> {
      DatasetBean pb = dsTable.getSelectedBean();
      if (pb != null) {
        currDataset = pb.cds;
        setDataset(pb.cds);
      }
    });

    covTable = new BeanTable<>(CoverageBean.class, (PreferencesExt) prefs.node("CoverageBeans"), false, "Coverages",
        "ucar.nc2.ft2.coverage.Coverage", new CoverageBean());

    csysTable = new BeanTable<>(CoordSysBean.class, (PreferencesExt) prefs.node("CoverageCoordSysBeans"), false,
        "CoverageCoordSys", "ucar.nc2.ft2.coverage.CoverageCoordSys", null);
    csysTable.addListSelectionListener(e -> {
      CoordSysBean bean = csysTable.getSelectedBean();
      if (null != bean) { // find the coverages
        List<CoverageBean> result = new ArrayList<>();
        for (CoverageBean covBean : covTable.getBeans()) {
          if (covBean.getCoordSysName().equals(bean.getName()))
            result.add(covBean);
        }
        covTable.setSelectedBeans(result);
      }
    });

    axisTable = new BeanTable<>(AxisBean.class, (PreferencesExt) prefs.node("CoverageCoordAxisBeans"), false,
        "CoverageCoordAxes", "ucar.nc2.ft2.coverage.CoverageCoordAxis", null);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
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
    PopupMenu dsPopup = new ucar.ui.widget.PopupMenu(jtable, "Options");
    dsPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean bean = dsTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(bean.cds.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    jtable = covTable.getJTable();
    PopupMenu csPopup = new ucar.ui.widget.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoverageBean vb = covTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(vb.geogrid.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    jtable = csysTable.getJTable();
    csPopup = new ucar.ui.widget.PopupMenu(jtable, "Options");
    csPopup.addAction("Show CoordSys", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordSysBean bean = csysTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(bean.gcs.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    csPopup.addAction("Show Transforms", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordSysBean bean = csysTable.getSelectedBean();
        infoTA.clear();
        for (CoverageTransform ct : bean.gcs.getTransforms())
          if (!ct.isHoriz())
            infoTA.appendLine(ct.toString());

        HorizCoordSys hcs = bean.gcs.getHorizCoordSys();
        if (hcs.getTransform() != null)
          infoTA.appendLine(hcs.getTransform().toString());

        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    jtable = axisTable.getJTable();
    csPopup = new ucar.ui.widget.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Axis", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = axisTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(bean.axis.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    csPopup.addAction("Show Coord Value differences", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = axisTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(bean.showCoordValueDiffs());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

  }

  public void clear() {
    dsTable.clearBeans();
    covTable.clearBeans();
    csysTable.clearBeans();
    axisTable.clearBeans();
    currDataset = null;
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
    if (coverageCollection == null)
      return;
    coverageCollection.getDetailInfo(result);
  }

  public void setCollection(FeatureDatasetCoverage gds) {
    this.coverageCollection = gds;
    clear();

    List<DatasetBean> dsList = new ArrayList<>();
    for (CoverageCollection ds : coverageCollection.getCoverageCollections())
      dsList.add(new DatasetBean(ds));
    dsTable.setBeans(dsList);
  }

  public void setDataset(CoverageCollection coverageDataset) {

    List<CoverageBean> beanList = new ArrayList<>();
    for (Coverage g : coverageDataset.getCoverages())
      beanList.add(new CoverageBean(g));
    covTable.setBeans(beanList);

    List<CoordSysBean> csList = new ArrayList<>();
    for (CoverageCoordSys gcs : coverageDataset.getCoordSys())
      csList.add(new CoordSysBean(coverageDataset, gcs));
    csysTable.setBeans(csList);

    List<AxisBean> axisList = new ArrayList<>();
    for (CoverageCoordAxis axis : coverageDataset.getCoordAxes())
      axisList.add(new AxisBean(axis));
    axisTable.setBeans(axisList);
  }

  private boolean contains(List<AxisBean> axisList, String name) {
    for (AxisBean axis : axisList)
      if (axis.getName().equals(name))
        return true;
    return false;
  }

  public CoverageCollection getCoverageDataset() {
    return currDataset;
  }

  public List<CoverageBean> getCoverageBeans() {
    return covTable.getBeans();
  }

  public List<String> getSelectedGrids() {
    List<CoverageBean> grids = covTable.getSelectedBeans();
    List<String> result = new ArrayList<>();
    for (CoverageBean gbean : grids) {
      result.add(gbean.getName());
    }
    return result;
  }

  public static class DatasetBean {
    CoverageCollection cds;

    public DatasetBean() {}

    public DatasetBean(CoverageCollection cds) {
      this.cds = cds;
    }

    public String getName() {
      return cds.getName();
    }

    public String getType() {
      return cds.getCoverageType().toString();
    }

    public String getCalendar() {
      return cds.getCalendar().toString();
    }

    public String getDateRange() {
      return cds.getCalendarDateRange() == null ? "null" : cds.getCalendarDateRange().toString();
    }

    public String getLLBB() {
      return cds.getLatlonBoundingBox() == null ? "null" : cds.getLatlonBoundingBox().toString();
    }

    public int getNCoverages() {
      return cds.getCoverageCount();
    }

    public int getNCooordSys() {
      return cds.getCoordSys().size();
    }

    public int getNAxes() {
      return cds.getCoordAxes().size();
    }
  }


  public static class CoverageBean implements NamedObject {

    public String hiddenProperties() { // for BeanTable
      return "value";
    }

    Coverage geogrid;
    String name, desc, units, coordSysName;
    DataType dataType;

    // no-arg constructor
    public CoverageBean() {}

    // create from a dataset
    public CoverageBean(Coverage geogrid) {
      this.geogrid = geogrid;
      name = geogrid.getShortName();
      desc = (geogrid.getDescription());
      units = (geogrid.getUnitsString());
      dataType = (geogrid.getDataType());
      coordSysName = (geogrid.getCoordSysName());
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

    public DataType getDataType() {
      return dataType;
    }

    public String getShape() {
      return Arrays.toString(geogrid.getCoordSys().getShape());
    }
  }

  public static class CoordSysBean {
    private CoverageCoordSys gcs;
    private String coordTrans, runtimeName, timeName, ensName, vertName;
    private int nIndAxis;
    private int nCov;

    // no-arg constructor
    public CoordSysBean() {}

    public CoordSysBean(CoverageCollection coverageDataset, CoverageCoordSys gcs) {
      this.gcs = gcs;

      Formatter buff = new Formatter();
      for (String ct : gcs.getTransformNames())
        buff.format("%s,", ct);
      coordTrans = buff.toString();

      for (CoverageCoordAxis axis : gcs.getAxes()) {
        if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)
          nIndAxis++;

        AxisType axisType = axis.getAxisType();
        if (axisType == null)
          continue;
        if (axisType == AxisType.RunTime)
          runtimeName = axis.getName();
        else if (axisType.isTime())
          timeName = axis.getName();
        else if (axisType == AxisType.Ensemble)
          ensName = axis.getName();
        else if (axisType.isVert())
          vertName = axis.getName();
      }

      for (Coverage cov : coverageDataset.getCoverages()) {
        if (cov.getCoordSys() == gcs)
          nCov++;
      }
    }

    public String getName() {
      return gcs.getName();
    }

    public String getType() {
      FeatureType type = gcs.getCoverageType();
      return (type == null) ? "" : type.toString();
    }

    public int getNIndCoords() {
      return nIndAxis;
    }

    public int getNCov() {
      return nCov;
    }

    public String getRuntime() {
      return runtimeName;
    }

    public String getTime() {
      return timeName;
    }

    public String getEns() {
      return ensName;
    }

    public String getVert() {
      return vertName;
    }

    public String getCoordTransforms() {
      return coordTrans;
    }
  }

  public static class CoordTransBean {
    private CoverageTransform gcs;
    String params;
    boolean isHoriz;

    // no-arg constructor
    public CoordTransBean() {}

    public CoordTransBean(CoverageTransform gcs) {
      this.gcs = gcs;
      this.isHoriz = gcs.isHoriz();

      Formatter buff = new Formatter();
      for (Attribute att : gcs.attributes()) {
        buff.format("%s, ", att);
      }
      params = buff.toString();
    }

    public String getName() {
      return gcs.getName();
    }

    public String getParams() {
      return params;
    }

    public String getIsHoriz() {
      return Boolean.toString(isHoriz);
    }
  }

  public static class AxisBean {
    CoverageCoordAxis axis;
    String name, desc, units;
    DataType dataType;
    AxisType axisType;
    long nvalues;
    boolean indepenent;

    // no-arg constructor
    public AxisBean() {}

    // create from a dataset
    public AxisBean(CoverageCoordAxis v) {
      this.axis = v;

      name = (v.getName());
      dataType = (v.getDataType());
      axisType = (v.getAxisType());
      units = (v.getUnits());
      desc = (v.getDescription());
      nvalues = (v.getNcoords());
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

    public DataType getDataType() {
      return dataType;
    }

    public String getSpacing() {
      CoverageCoordAxis.Spacing sp = axis.getSpacing();
      return (sp == null) ? "" : sp.toString();
    }

    public long getNvalues() {
      return nvalues;
    }

    public double getStartValue() {
      return axis.getStartValue();
    }

    public double getEndValue() {
      return axis.getEndValue();
    }

    public double getResolution() {
      return axis.getResolution();
    }

    public boolean getHasData() {
      return axis.getHasData();
    }

    public String getDependsOn() {
      if (axis.getDependenceType() != CoverageCoordAxis.DependenceType.independent)
        return axis.getDependenceType() + ": " + axis.getDependsOn();
      else
        return axis.getDependenceType().toString();
    }

    String showCoordValueDiffs() {
      Formatter f = new Formatter();
      switch (axis.getSpacing()) {
        case regularInterval:
        case regularPoint:
          f.format("%n%s resolution=%f%n", axis.getSpacing(), axis.getResolution());
          break;

        case irregularPoint:
        case contiguousInterval:
          double[] values = axis.getValues();
          int n = values.length;
          f.format("%n%s (npts=%d)%n", axis.getSpacing(), n);
          for (int i = 0; i < n - 1; i++) {
            double diff = values[i + 1] - values[i];
            f.format("%10f %10f == %10f%n", values[i], values[i + 1], diff);
          }
          f.format("%n");
          break;

        case discontiguousInterval:
          values = axis.getValues();
          n = values.length;
          f.format("%ndiscontiguous intervals (npts=%d)%n", n);
          for (int i = 0; i < n; i += 2) {
            double diff = values[i + 1] - values[i];
            f.format("(%10f,%10f) = %10f%n", values[i], values[i + 1], diff);
          }
          f.format("%n");
          break;
      }
      return f.toString();
    }
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
      cp.add(CoverageTable.this, BorderLayout.CENTER);
      pack();
    }
  }
}
