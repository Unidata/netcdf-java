/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import javax.annotation.Nullable;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.calendar.*;
import ucar.nc2.internal.grid.DatasetClassifier;
import ucar.nc2.write.NcdumpArray;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.dataset.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.unidata.geoloc.Projection;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.*;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/** A Swing widget to examine Coordinate Systems. */
public class CoordSysTable extends JPanel {
  private final PreferencesExt prefs;

  private final BeanTable<VariableBean> varTable;
  private final BeanTable<CoordinateSystemBean> csTable;
  private final BeanTable<AxisBean> axisTable;
  private final JSplitPane split, split2;
  private final TextHistoryPane infoTA;
  private final IndependentWindow infoWindow;
  private BeanTable<AttributeBean> attTable;

  private IndependentWindow attWindow;
  private NetcdfDataset ds;
  private DatasetClassifier dcl;

  public CoordSysTable(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    if (buttPanel != null) {
      AbstractAction attAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showAtts();
        }
      };
      BAMutil.setActionProperties(attAction, "FontDecr", "global attributes", false, 'A', -1);
      BAMutil.addActionToContainer(buttPanel, attAction);
    }

    csTable = new BeanTable<>(CoordinateSystemBean.class, (PreferencesExt) prefs.node("CoordinateSystemBean"), false);
    csTable.addListSelectionListener(e -> {
      CoordinateSystemBean csb = csTable.getSelectedBean();
      if (null != csb) {
        setSelectedCoordinateAxes(csb.coordSys);
      }
    });

    varTable = new BeanTable<>(VariableBean.class, (PreferencesExt) prefs.node("VariableBeans"), false);
    varTable.addListSelectionListener(e -> {
      VariableBean vb = varTable.getSelectedBean();
      if (null != vb) {
        csTable.setSelectedBean(vb.coordSysBean);
      }
    });

    axisTable = new BeanTable<>(AxisBean.class, (PreferencesExt) prefs.node("CoordinateAxisBean"), false);
    PopupMenu varPopup = new PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VariableBean vb = varTable.getSelectedBean();
        if (vb != null) {
          Variable v = ds.findVariable(vb.getName());
          if (v == null)
            return;
          infoTA.clear();
          infoTA.appendLine(((VariableEnhanced) v).toString());
          infoTA.appendLine(showMissing(v));
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    PopupMenu csPopup = new PopupMenu(csTable.getJTable(), "Options");
    csPopup.addAction("Show CoordSys", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordinateSystemBean csb = csTable.getSelectedBean();
        if (csb != null) {
          CoordinateSystem coordSys = csb.coordSys;
          infoTA.clear();
          infoTA.appendLine("Coordinate System = " + coordSys.getName());
          for (CoordinateAxis axis : coordSys.getCoordinateAxes()) {
            infoTA.appendLine("  " + axis.getAxisType() + " " + axis.getNameAndDimensions());
          }
          infoTA.appendLine(" Coordinate Transforms");
          Projection p = coordSys.getProjection();
          if (p != null) {
            infoTA.appendLine("    Projection= " + p);
            infoTA.appendLine("    impl class= " + p.getClass().getName());
          }
        }
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    PopupMenu axisPopup = new PopupMenu(axisTable.getJTable(), "Options");
    axisPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = axisTable.getSelectedBean();
        if (bean == null)
          return;
        VariableDS axis = (VariableDS) ds.findVariable(bean.getName());
        if (axis == null)
          return;
        infoTA.clear();
        infoTA.appendLine(axis.toString());
        infoTA.appendLine(showMissing(axis));
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    axisPopup.addAction("Show Values", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = axisTable.getSelectedBean();
        if (bean == null)
          return;
        infoTA.clear();
        showValues(bean.axis);
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    axisPopup.addAction("Show Value Differences", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = axisTable.getSelectedBean();
        if (bean == null)
          return;
        infoTA.clear();
        showValueDiffs(bean.axis);
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    axisPopup.addAction("Show Values as Date", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = axisTable.getSelectedBean();
        if (bean == null)
          return;
        infoTA.clear();
        showValuesAsDates(bean.axis);
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, csTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, axisTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split2, BorderLayout.CENTER);
  }

  public void summaryInfo(Formatter f) {
    if (ds == null)
      return;
    f.format("%s%n", ds.getLocation());
    int ngrids = 0;

    for (Object varo : varTable.getBeans()) {
      VariableBean varBean = (VariableBean) varo;
      if (varBean.getArrayType().trim().equalsIgnoreCase("grid"))
        ngrids++;
    }
    int ncoordSys = csTable.getBeans().size();
    int ncoords = axisTable.getBeans().size();

    f.format(" ngrids=%d, ncoords=%d, ncoordSys=%d%n", ngrids, ncoords, ncoordSys);
  }

  public void showAtts() {
    if (ds == null)
      return;
    if (attTable == null) {
      // global attributes
      attTable = new BeanTable<>(AttributeBean.class, (PreferencesExt) prefs.node("AttributeBeans"), false);
      PopupMenu varPopup = new PopupMenu(attTable.getJTable(), "Options");
      varPopup.addAction("Show Attribute", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          AttributeBean bean = attTable.getSelectedBean();
          if (bean != null) {
            infoTA.setText(bean.att.toString());
            infoTA.gotoTop();
            infoWindow.show();
          }
        }
      });
      attWindow = new IndependentWindow("Global Attributes", BAMutil.getImage("nj22/NetcdfUI"), attTable);
      attWindow.setBounds((Rectangle) prefs.getBean("AttWindowBounds", new Rectangle(300, 100, 500, 800)));
    }

    List<AttributeBean> attlist = new ArrayList<>();
    addAttributes(attlist, ds.getRootGroup());
    attTable.setBeans(attlist);
    attWindow.show();
  }

  private void addAttributes(List<AttributeBean> attlist, Group g) {
    for (Attribute att : g.attributes()) {
      attlist.add(new AttributeBean(g, att));
    }
    for (Group nested : g.getGroups()) {
      addAttributes(attlist, nested);
    }
  }

  private void showValues(CoordinateAxis axis) {

    try {

      if (axis instanceof CoordinateAxis1D && axis.isNumeric()) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        printArray("midpoints=", axis1D.getCoordValues());

        if (!axis1D.isInterval()) {
          printArray("edges=", axis1D.getCoordEdges());

        } else {
          printArray("bound1=", axis1D.getBound1());
          printArray("bound2=", axis1D.getBound2());

          Formatter f = new Formatter();
          double[] mid = axis1D.getCoordValues();
          double[] b1 = axis1D.getBound1();
          double[] b2 = axis1D.getBound2();
          for (int i = 0; i < b1.length; i++) {
            f.format("%f (%f,%f) = %f%n", mid[i], b1[i], b2[i], b2[i] - b1[i]);
          }
          infoTA.appendLine(f.toString());
        }

      } else if (axis instanceof CoordinateAxis2D && axis.isNumeric()) {
        infoTA.appendLine(NcdumpArray.printVariableData(axis, null));
        showValues2D((CoordinateAxis2D) axis);

      } else {
        infoTA.appendLine(NcdumpArray.printVariableData(axis, null));
      }

    } catch (IOException e1) {
      e1.printStackTrace();
      infoTA.appendLine(e1.getMessage());
    }
  }

  private void showValueDiffs(CoordinateAxis axis) {
    if (!axis.isNumeric())
      return;
    try {
      if (axis instanceof CoordinateAxis1D) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        double[] mids = axis1D.getCoordValues();
        double[] diffs = new double[mids.length];
        for (int i = 0; i < mids.length - 1; i++)
          diffs[i] = mids[i + 1] - mids[i];
        printArrays("midpoint differences", mids, diffs);

      } else if (axis instanceof CoordinateAxis2D) {
        CoordinateAxis2D axis2D = (CoordinateAxis2D) axis;
        Array<Double> mids = axis2D.getCoordValuesArray();
        int[] shape = mids.getShape();
        int nx1 = shape[1] - 1;
        double[] pdiffx = new double[shape[0] * nx1];
        for (int j = 0; j < shape[0]; j++) {
          for (int i = 0; i < shape[1] - 1; i++) {
            double diff = mids.get(j, i + 1) - mids.get(j, i);
            pdiffx[j * nx1 + i] = diff;
          }
        }
        Array diffx = Arrays.factory(ArrayType.DOUBLE, new int[] {shape[0], nx1}, pdiffx);
        infoTA.appendLine(NcdumpArray.printArray(diffx, "diff in x", null));

        int ny1 = shape[0] - 1;
        double[] pdiffy = new double[ny1 * shape[1]];
        for (int j = 0; j < shape[0] - 1; j++) {
          for (int i = 0; i < shape[1]; i++) {
            double diff = mids.get(j + 1, i) - mids.get(j, i);
            pdiffy[j * shape[1] + i] = diff;
          }
        }
        infoTA.appendLine("\n\n\n");
        Array diffy = Arrays.factory(ArrayType.DOUBLE, new int[] {ny1, shape[1]}, pdiffx);
        infoTA.appendLine(NcdumpArray.printArray(diffy, "diff in y", null));
      }

    } catch (Exception e1) {
      e1.printStackTrace();
      infoTA.appendLine(e1.getMessage());
    }
  }

  private void showValuesAsDates(CoordinateAxis axis) {
    String units = axis.getUnitsString();
    String cal = getCalendarAttribute(axis);
    Calendar calt = Calendar.get(cal).orElse(null);
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(calt, units).orElseThrow();

    try {
      infoTA.appendLine(units);
      infoTA.appendLine(NcdumpArray.printVariableData(axis, null));

      if (axis.getArrayType().isNumeric()) {
        if (axis instanceof CoordinateAxis2D) {
          showDates2D((CoordinateAxis2D) axis, cdu);

        } else if (axis instanceof CoordinateAxis1D) { // 1D
          showDates1D((CoordinateAxis1D) axis, cdu);

        } else { // > 2D
          Array<Number> data = (Array<Number>) axis.readArray();
          for (Number val : data) {
            double dval = val.doubleValue();
            infoTA.appendLine(makeCalendarDateStringOrMissing(cdu, dval));
          }
        }

      } else { // must be iso dates
        Array data = axis.readArray();
        Formatter f = new Formatter();
        if (data.getArrayType() == ArrayType.CHAR) {
          Array<Byte> dataS = (Array<Byte>) data;
          Array<String> iter = Arrays.makeStringsFromChar(dataS);
          for (String s : iter) {
            f.format(" %s%n", s);
          }
          infoTA.appendLine(f.toString());

        } else if (data.getArrayType() == ArrayType.STRING) {
          Array<String> iter = (Array<String>) data;
          for (String s : iter) {
            f.format(" %s%n", s);
          }
          infoTA.appendLine(f.toString());
        }

      }
    } catch (Exception ex) {
      ex.printStackTrace();
      infoTA.appendLine(ex.getMessage());
    }
  }

  private void showValues2D(CoordinateAxis2D axis2D) {
    Formatter f = new Formatter();

    if (axis2D.isInterval()) {
      Array<Double> coords = axis2D.getCoordValuesArray();
      Array<Double> bounds = axis2D.getCoordBoundsArray();
      if (bounds == null) {
        infoTA.appendLine("No bounds for interval " + axis2D.getFullName());
        return;
      }

      for (int count = 0; count < coords.getSize(); count++) {
        double coordValue = coords.get(count);
        double bounds1 = bounds.get(count, 0);
        double bounds2 = bounds.get(count, 1);
        f.format("%3d: %f (%f,%f) len=%f mid=%f%n", count, coordValue, bounds1, bounds2, bounds2 - bounds1,
            (bounds2 + bounds1) / 2);
      }

    } else {
      Array<Double> coords = axis2D.getCoordValuesArray();
      for (double coordValue : coords) {
        f.format("%f%n", coordValue);
      }
    }
    infoTA.appendLine(f.toString());
  }

  private void showDates2D(CoordinateAxis2D axis2D, CalendarDateUnit cdu) {
    Formatter f = new Formatter();

    if (axis2D.isInterval()) {
      Array<Double> coords = axis2D.getCoordValuesArray();
      Array<Double> bounds = axis2D.getCoordBoundsArray();
      if (bounds == null) {
        infoTA.appendLine("No bounds for interval " + axis2D.getFullName());
        return;
      }

      for (int idx = 0; idx < coords.getSize(); idx++) {
        double coordValue = coords.get(idx);
        double bounds1 = bounds.get(idx, 0);
        double bounds2 = bounds.get(idx, 1);
        f.format("%3d: %s (%s,%s)%n", idx, makeCalendarDateStringOrMissing(cdu, coordValue),
            makeCalendarDateStringOrMissing(cdu, bounds1), makeCalendarDateStringOrMissing(cdu, bounds2));
      }

    } else {
      Array<Double> coords = axis2D.getCoordValuesArray();
      int count = 0;
      for (double coordValue : coords) {
        f.format("%3d: %s%n", count++, makeCalendarDateStringOrMissing(cdu, coordValue));
      }
    }

    infoTA.appendLine(f.toString());
  }

  private String makeCalendarDateStringOrMissing(CalendarDateUnit cdu, double value) {
    if (Double.isNaN(value))
      return "missing";
    return cdu.makeFractionalCalendarDate(value).toString();
  }

  private String getCalendarAttribute(Variable vds) {
    Attribute cal = vds.findAttribute("calendar");
    return (cal == null) ? null : cal.getStringValue();
  }

  private void showDates1D(CoordinateAxis1D axis1D, CalendarDateUnit cdu) {
    if (!axis1D.isInterval()) {
      for (double val : axis1D.getCoordValues()) {
        if (Double.isNaN(val))
          infoTA.appendLine(" N/A");
        else
          infoTA.appendLine(" " + cdu.makeFractionalCalendarDate(val));
      }
    } else { // is interval
      Formatter f = new Formatter();
      double[] b1 = axis1D.getBound1();
      double[] b2 = axis1D.getBound2();
      for (int i = 0; i < b1.length; i++)
        f.format(" (%f, %f) == (%s, %s)%n", b1[i], b2[i], cdu.makeFractionalCalendarDate(b1[i]),
            cdu.makeFractionalCalendarDate(b2[i]));
      infoTA.appendLine(f.toString());
    }
  }

  private void printArray(String title, double[] vals) {
    Formatter sbuff = new Formatter();
    sbuff.format(" %s=", title);
    for (double val : vals) {
      sbuff.format(" %f", val);
    }
    sbuff.format("%n");
    infoTA.appendLine(sbuff.toString());
  }

  private void printArrays(String title, double[] vals, double[] vals2) {
    Formatter sbuff = new Formatter();
    sbuff.format(" %s%n", title);
    for (int i = 0; i < vals.length; i++) {
      sbuff.format(" %3d: %10.2f  %10.2f%n", i, vals[i], vals2[i]);
    }
    sbuff.format("%n");
    infoTA.appendLine(sbuff.toString());
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    varTable.saveState(false);
    csTable.saveState(false);
    axisTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (attWindow != null)
      prefs.putBeanObject("AttWindowBounds", attWindow.getBounds());
  }

  public void clear() {
    this.ds = null;
    varTable.clearBeans();
    axisTable.clearBeans();
    csTable.clearBeans();
  }

  public void setDataset(NetcdfDataset ds) {
    this.ds = ds;
    Formatter infolog = new Formatter();
    this.dcl = new DatasetClassifier(ds, infolog);

    List<CoordinateSystemBean> csList = getCoordinateSystemBeans(ds);
    List<VariableBean> beanList = new ArrayList<>();
    List<AxisBean> axisList = new ArrayList<>();
    setVariables(ds.getVariables(), axisList, beanList, csList);

    varTable.setBeans(beanList);
    axisTable.setBeans(axisList);
    csTable.setBeans(csList);
  }

  private void setVariables(List<Variable> varList, List<AxisBean> axisList, List<VariableBean> beanList,
      List<CoordinateSystemBean> csList) {
    for (Variable aVar : varList) {
      VariableEnhanced v = (VariableEnhanced) aVar;
      if (v instanceof CoordinateAxis)
        axisList.add(new AxisBean((CoordinateAxis) v));
      else
        beanList.add(new VariableBean(v, csList));

      if (v instanceof Structure) {
        List<Variable> nested = ((Structure) v).getVariables();
        setVariables(nested, axisList, beanList, csList);
      }
    }
  }

  private List<CoordinateSystemBean> getCoordinateSystemBeans(NetcdfDataset ds) {
    List<CoordinateSystemBean> vlist = new ArrayList<>();
    for (CoordinateSystem elem : ds.getCoordinateSystems()) {
      vlist.add(new CoordinateSystemBean(elem));
    }
    return vlist;
  }

  private void setSelectedCoordinateAxes(CoordinateSystem cs) {
    List<CoordinateAxis> axesList = cs.getCoordinateAxes();
    if (axesList.isEmpty())
      return;
    CoordinateAxis axis = axesList.get(0);

    List<AxisBean> beans = axisTable.getBeans();
    for (Object bean1 : beans) {
      AxisBean bean = (AxisBean) bean1;
      if (bean.axis == axis) {
        axisTable.setSelectedBean(bean);
        return;
      }
    }
  }

  private String showMissing(Variable v) {
    if (!(v instanceof VariableDS))
      return "";
    VariableDS ve = (VariableDS) v;
    Formatter buff = new Formatter();
    buff.format("%s:", v.getFullName());
    buff.format("enhanceMode= %s%n", ve.getEnhanceMode());
    ve.scaleMissingUnsignedProxy().showInfo(buff);
    return buff.toString();
  }

  public class VariableBean {
    Variable v;
    VariableEnhanced ve;
    CoordinateSystemBean coordSysBean;
    String name, desc, units;
    String dims, shape;

    // no-arg constructor
    public VariableBean() {}

    // create from a dataset
    public VariableBean(VariableEnhanced ve, List<CoordinateSystemBean> csList) {
      this.v = (Variable) ve;
      this.ve = ve;

      name = v.getFullName();
      desc = v.getDescription();
      units = v.getUnitsString();

      // collect dimensions
      boolean first = true;
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      for (Dimension dim : v.getDimensions()) {
        if (!first) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getShortName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
        first = false;
      }
      dims = names.toString();
      shape = lens.toString();

      // sort by largest size first
      if (!ve.getCoordinateSystems().isEmpty()) {
        List<CoordinateSystem> css = new ArrayList<>(ve.getCoordinateSystems());
        css.sort((o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());
        CoordinateSystem cs = css.get(0);
        for (CoordinateSystemBean csBean : csList)
          if (cs == csBean.coordSys)
            coordSysBean = csBean;
      }
    }

    public String getName() {
      return name;
    }

    public String getDims() {
      return dims;
    }

    public String getShape() {
      return shape;
    }

    public String getDescription() {
      return (desc == null) ? "null" : desc;
    }

    public String getUnits() {
      return (units == null) ? "null" : units;
    }

    @Nullable
    public String getAbbrev() {
      return v.attributes().findAttributeString(CDM.ABBREV, null);
    }

    public String getCoordSys() {
      return (coordSysBean == null) ? "" : coordSysBean.getCoordSys();
    }

    public String getArrayType() {
      return v.getArrayType().toString();
    }

  }

  public class CoordinateSystemBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateSystem coordSys;
    private String name, dataType = "";
    private int domainRank, rangeRank;
    private boolean isGeoXY, isLatLon, isProductSet, isRegular;

    // no-arg constructor

    public CoordinateSystemBean() {}

    public CoordinateSystemBean(CoordinateSystem cs) {
      this.coordSys = cs;

      setCoordSys(cs.getName());
      setGeoXY(cs.isGeoXY());
      setLatLon(cs.isLatLon());
      setProductSet(cs.isProductSet());
      setRegular(cs.isRegular());
      setDomainRank(cs.getDomain().size());
      setRangeRank(cs.getCoordinateAxes().size());

      if (dcl.getCoordinateSystemsUsed().stream().filter(dsc -> dsc.getCoordinateSystem().equals(cs)).findAny()
          .isPresent()) {
        addArrayType("grid");
      }
    }

    public String getCoordSys() {
      return name;
    }

    public void setCoordSys(String name) {
      this.name = name;
    }

    public boolean isGeoXY() {
      return isGeoXY;
    }

    public void setGeoXY(boolean isGeoXY) {
      this.isGeoXY = isGeoXY;
    }

    public boolean getLatLon() {
      return isLatLon;
    }

    public void setLatLon(boolean isLatLon) {
      this.isLatLon = isLatLon;
    }

    public boolean isProductSet() {
      return isProductSet;
    }

    public void setProductSet(boolean isProductSet) {
      this.isProductSet = isProductSet;
    }

    public boolean isRegular() {
      return isRegular;
    }

    public void setRegular(boolean isRegular) {
      this.isRegular = isRegular;
    }

    public int getDomainRank() {
      return domainRank;
    }

    public void setDomainRank(int domainRank) {
      this.domainRank = domainRank;
    }

    public int getRangeRank() {
      return rangeRank;
    }

    public void setRangeRank(int rangeRank) {
      this.rangeRank = rangeRank;
    }

    public String getProjection() {
      return this.coordSys.getProjection() == null ? "" : this.coordSys.getProjection().getName();
    }

    public String getArrayType() {
      return dataType;
    }

    void addArrayType(String dt) {
      dataType = dataType + " " + dt;
    }

    public boolean isImplicit() {
      return coordSys.isImplicit();
    }

  }

  public static class AttributeBean {
    private final Group group;
    private final Attribute att;

    // create from a dataset
    public AttributeBean(Group group, Attribute att) {
      this.group = group;
      this.att = att;
    }

    public String getName() {
      return group.isRoot() ? att.getName() : group.getFullName() + "/" + att.getName();
    }

    public String getValue() {
      ucar.array.Array<?> value = att.getArrayValues();
      return NcdumpArray.printArray(value, null, null);
    }
  }

  public static class AxisBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateAxis axis;
    CoordinateSystem firstCoordSys;
    String name, desc, units, axisType = "", positive = "", incr = "";
    String dims, shape;
    boolean isIndCoord;
    boolean isLayer;
    boolean isInterval;

    // no-arg constructor

    public AxisBean() {}

    // create from a dataset

    public AxisBean(CoordinateAxis v) {
      this.axis = v;

      setName(v.getFullName());
      setCoordVar(v.isIndependentCoordinate());
      setDescription(v.getDescription());
      setUnits(v.getUnitsString());

      // collect dimensions
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      List dims = v.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        Dimension dim = (Dimension) dims.get(j);
        if (j > 0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getShortName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDims(names.toString());
      setShape(lens.toString());

      AxisType at = v.getAxisType();
      if (at != null)
        setAxisType(at.toString());
      String p = v.getPositive();
      if (p != null)
        setPositive(p);

      if (v instanceof CoordinateAxis1D) {
        CoordinateAxis1D v1 = (CoordinateAxis1D) v;
        if (v1.isRegular())
          setRegular(Double.toString(v1.getIncrement()));
      }
      isLayer = (null != axis.findAttribute(_Coordinate.ZisLayer));
      isInterval = axis.isInterval();
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isIndCoord() {
      return isIndCoord;
    }

    public void setCoordVar(boolean isIndCoord) {
      this.isIndCoord = isIndCoord;
    }

    public boolean isContig() {
      return axis.isContiguous();
    }

    /*
     * public boolean isLayer() {
     * return isLayer;
     * }
     */

    public boolean isInterval() {
      return isInterval;
    }

    public String getShape() {
      return shape;
    }

    public void setShape(String shape) {
      this.shape = shape;
    }

    public String getAxisType() {
      return axisType;
    }

    public void setAxisType(String axisType) {
      this.axisType = axisType;
    }

    public String getDims() {
      return dims;
    }

    public void setDims(String dims) {
      this.dims = dims;
    }

    public String getDescription() {
      return desc;
    }

    public void setDescription(String desc) {
      this.desc = desc;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = (units == null) ? "null" : units;
    }

    public String getPositive() {
      return positive;
    }

    public void setPositive(String positive) {
      this.positive = positive;
    }

    public String getRegular() {
      return incr;
    }

    public void setRegular(String incr) {
      this.incr = incr;
    }
  }
}