/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import static ucar.nc2.internal.util.CompareNetcdf2.IDENTITY_FILTER;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.*;

import ucar.array.Array;
import ucar.array.StructureDataArray;
import ucar.array.StructureMembers;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.ParsedArraySectionSpec;
import ucar.nc2.Sequence;
import ucar.nc2.constants._Coordinate;
import ucar.ui.StructureArrayTable;
import org.jdom2.Element;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.ui.ToolsUI;
import ucar.ui.dialog.CompareDialog;
import ucar.ui.dialog.NetcdfOutputChooser;
import ucar.nc2.internal.util.CompareNetcdf2.ObjFilter;
import ucar.nc2.write.NcdumpArray;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.write.NcmlWriter;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

/** The Viewer component in ToolsUI. */
public class DatasetViewer extends JPanel {
  private final FileManager fileChooser;
  private final PreferencesExt prefs;

  private final List<NestedTable> nestedTableList = new ArrayList<>();

  private final JPanel tablePanel;
  private final JSplitPane mainSplit;

  private final DatasetTreeView datasetTree;
  private final NCdumpPane dumpPane;
  private final VariablePlot dataPlot;
  private final VariableTable variableTable;

  private final TextHistoryPane infoTA;
  private final StructureArrayTable dataArrayTable;
  private final IndependentWindow infoWindow, dataArrayWindow, plotWindow, dumpWindow;

  private BeanTable<AttributeBean> attTable;
  private IndependentWindow attWindow;
  private JComponent currentComponent;
  private NetcdfFile ds;
  private boolean eventsOK = true;

  DatasetViewer(PreferencesExt prefs, FileManager fileChooser) {
    this.prefs = prefs;
    this.fileChooser = fileChooser;

    // create the variable table(s)
    tablePanel = new JPanel(new BorderLayout());
    setNestedTable(0, null);

    // the tree view
    datasetTree = new DatasetTreeView();
    datasetTree.addPropertyChangeListener(e -> {
      if (e.getPropertyName().equals("Selection")) {
        setSelected((Variable) e.getNewValue());
      }
    });

    // layout
    mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, datasetTree, tablePanel);
    mainSplit.setDividerLocation(prefs.getInt("mainSplit", 100));

    setLayout(new BorderLayout());
    add(mainSplit, BorderLayout.CENTER);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // the data Table
    dataArrayTable = new StructureArrayTable((PreferencesExt) prefs.node("structTableArray"));
    variableTable = new VariableTable((PreferencesExt) prefs.node("variableTable"));
    dataArrayWindow = new IndependentWindow("Data Array Table", BAMutil.getImage("nj22/NetcdfUI"), dataArrayTable);
    dataArrayWindow.setBounds((Rectangle) prefs.getBean("dataWindowBounds", new Rectangle(50, 300, 1000, 1200)));

    // the ncdump Pane
    dumpPane = new NCdumpPane((PreferencesExt) prefs.node("dumpPane"));
    dumpWindow = new IndependentWindow("NCDump Variable Data", BAMutil.getImage("nj22/NetcdfUI"), dumpPane);
    dumpWindow.setBounds((Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle(300, 300, 300, 200)));

    // the plot Pane
    dataPlot = new VariablePlot((PreferencesExt) prefs.node("plotPane"));
    plotWindow = new IndependentWindow("Plot Variable Data", BAMutil.getImage("nj22/NetcdfUI"), dataPlot);
    plotWindow.setBounds((Rectangle) prefs.getBean("PlotWindowBounds", new Rectangle(300, 300, 300, 200)));
    plotWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        dataPlot.clear();
      }
    });
  }

  private NetcdfOutputChooser outChooser;

  void addActions(JPanel buttPanel) {
    AbstractAction netcdfAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (ds == null)
          return;
        if (outChooser == null) {
          outChooser = new NetcdfOutputChooser((Frame) null);
          outChooser.addPropertyChangeListener("OK", evt -> writeNetcdf((NetcdfOutputChooser.Data) evt.getNewValue()));
        }
        outChooser.setOutputFilename(ds.getLocation());
        outChooser.setVisible(true);
      }
    };
    BAMutil.setActionProperties(netcdfAction, "nj22/Netcdf", "Write netCDF file", false, 'S', -1);
    BAMutil.addActionToContainer(buttPanel, netcdfAction);

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Compare to another file", false);
    compareButton.addActionListener(e -> compareDataset());
    buttPanel.add(compareButton);

    AbstractAction attAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        showAtts();
      }
    };
    BAMutil.setActionProperties(attAction, "FontDecr", "global attributes", false, 'A', -1);
    BAMutil.addActionToContainer(buttPanel, attAction);
  }

  ///////////////////////////////////////

  private void writeNetcdf(NetcdfOutputChooser.Data data) {
    try {
      NetcdfFormatWriter.Builder builder =
          NetcdfFormatWriter.builder().setFormat(data.format).setLocation(data.outputFilename)
              .setChunker(Nc4ChunkingStrategy.factory(data.chunkerType, data.deflate, data.shuffle));

      try (NetcdfCopier copier = NetcdfCopier.create(ds, builder)) {
        copier.write(null);
        JOptionPane.showMessageDialog(this, "File successfully written");
      }

    } catch (Exception ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  }

  private CompareDialog dialog;

  private void compareDataset() {
    if (ds == null)
      return;
    if (dialog == null) {
      dialog = new CompareDialog(null, fileChooser);
      dialog.pack();
      dialog.addPropertyChangeListener("OK", evt -> {
        CompareDialog.Data data = (CompareDialog.Data) evt.getNewValue();
        compareDataset(data);
      });
    }
    dialog.setVisible(true);
  }

  private void compareBuilder() {
    if (ds == null)
      return;
    String fileLocation = ds.getLocation();
    try (NetcdfFile org = NetcdfFiles.open(fileLocation)) {
      try (NetcdfFile withBuilder = NetcdfFiles.open(fileLocation)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        boolean ok = compare.compare(org, withBuilder, new CoordsObjFilter());
        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.setTitle("Compare Old (file1) with Builder (file 2)");
        infoWindow.show();
      }
    } catch (Throwable ioe) {
      StringWriter sw = new StringWriter(10000);
      ioe.printStackTrace(new PrintWriter(sw));
      infoTA.setText(sw.toString());
      infoTA.gotoTop();
      infoWindow.show();
    }
  }

  public static class CoordsObjFilter implements ObjFilter {
    @Override
    public boolean attCheckOk(Attribute att) {
      return !att.getShortName().equals(_Coordinate._CoordSysBuilder);
    }
  }

  private void compareDataset(CompareDialog.Data data) {
    if (data.name == null)
      return;

    try (NetcdfFile compareFile = ToolsUI.getToolsUI().openFile(data.name, false, null);
        Formatter f = new Formatter()) {
      CompareNetcdf2 cn = new CompareNetcdf2(f, data.showCompare, data.showDetails, data.readData);
      if (data.howMuch == CompareDialog.HowMuch.All)
        cn.compare(ds, compareFile);

      else if (data.howMuch == CompareDialog.HowMuch.varNameOnly) {
        f.format(" First file = %s%n", ds.getLocation());
        f.format(" Second file= %s%n", compareFile.getLocation());
        CompareNetcdf2.compareLists(getVariableNames(ds), getVariableNames(compareFile), f);

      } else {
        NestedTable nested = nestedTableList.get(0);
        Variable org = getSelectedVariable(nested.table);
        if (org == null)
          return;
        Variable ov = compareFile.findVariable(NetcdfFiles.makeFullName(org));
        if (ov != null)
          cn.compareVariable(org, ov, IDENTITY_FILTER);
      }

      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.setTitle("Compare");
      infoWindow.show();

    } catch (Throwable ioe) {
      StringWriter sw = new StringWriter(10000);
      ioe.printStackTrace(new PrintWriter(sw));
      infoTA.setText(sw.toString());
      infoTA.gotoTop();
      infoWindow.show();
    }
  }

  private List<String> getVariableNames(NetcdfFile nc) {
    List<String> result = new ArrayList<>();
    for (Variable v : nc.getVariables())
      result.add(v.getFullName());
    return result;
  }

  ////////////////////////////////////////////////

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

  public NetcdfFile getDataset() {
    return this.ds;
  }

  void clear() {
    this.ds = null;
    if (attTable != null) {
      attTable.clearBeans();
    }
    for (NestedTable nt : nestedTableList) {
      nt.table.clearBeans();
    }
    datasetTree.clear();
  }

  public void setDataset(NetcdfFile ds) {
    this.ds = ds;
    NestedTable nt = nestedTableList.get(0);
    nt.table.setBeans(getVariableBeans(ds));
    hideNestedTable(1);
    datasetTree.setFile(ds);
  }

  private void setSelected(Variable v) {
    eventsOK = false;

    List<Variable> vchain = new ArrayList<>();
    vchain.add(v);

    Variable vp = v;
    while (vp.isMemberOfStructure()) {
      vp = vp.getParentStructure();
      vchain.add(0, vp); // reverse
    }

    for (int i = 0; i < vchain.size(); i++) {
      vp = vchain.get(i);
      NestedTable ntable = setNestedTable(i, vp.getParentStructure());
      ntable.setSelected(vp);
    }

    eventsOK = true;
  }

  private NestedTable setNestedTable(int level, Structure s) {
    NestedTable ntable;
    if (nestedTableList.size() < level + 1) {
      ntable = new NestedTable(level);
      nestedTableList.add(ntable);

    } else {
      ntable = nestedTableList.get(level);
    }

    if (s != null) // variables inside of records
      ntable.table.setBeans(getStructureVariables(s));

    ntable.show();
    return ntable;
  }

  private void hideNestedTable(int level) {
    int n = nestedTableList.size();
    for (int i = n - 1; i >= level; i--) {
      NestedTable ntable = nestedTableList.get(i);
      ntable.hide();
    }
  }

  private class NestedTable {
    int level;
    PreferencesExt myPrefs;

    BeanTable<VariableBean> table; // always the left component
    JSplitPane split; // right component (if exists) is the nested dataset.
    int splitPos = 100;
    boolean isShowing;

    NestedTable(int level) {
      this.level = level;
      myPrefs = (PreferencesExt) prefs.node("NestedTable" + level);

      table = new BeanTable<>(VariableBean.class, myPrefs, false);

      JTable jtable = table.getJTable();
      PopupMenu csPopup = new PopupMenu(jtable, "Options");
      csPopup.addAction("Show Declaration", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, false);
        }
      });
      csPopup.addAction("Show NcML", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, true);
        }
      });
      csPopup.addAction("Show Data", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showData(table);
        }
      });
      csPopup.addAction("NCdump Data", "Dump", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          dumpData(table);
        }
      });
      /*
       * csPopup.addAction("Write binary Data to file", new AbstractAction() {
       * public void actionPerformed(ActionEvent e) {
       * String binaryFilePath = fileChooser.chooseFilenameToSave("data.bin");
       * if (binaryFilePath != null) {
       * writeData(table, new File(binaryFilePath));
       * }
       * }
       * });
       */
      if (level == 0) {
        csPopup.addAction("Data Table", new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            dataArrayTable(table);
          }
        });
        csPopup.addAction("Data Plot", new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            dataPlot(table);
          }
        });
      }

      // get selected variable, see if its a structure
      table.addListSelectionListener(e -> {
        Variable v = getSelectedVariable(table);
        if (v instanceof Structure) {
          hideNestedTable(this.level + 2);
          setNestedTable(this.level + 1, (Structure) v);

        } else {
          hideNestedTable(this.level + 1);
        }
        if (eventsOK) {
          datasetTree.setSelected(v);
        }
      });

      // layout
      if (currentComponent == null) {
        currentComponent = table;
        tablePanel.add(currentComponent, BorderLayout.CENTER);
        isShowing = true;

      } else {
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, currentComponent, table);
        splitPos = myPrefs.getInt("splitPos" + level, 500);
        if (splitPos > 0)
          split.setDividerLocation(splitPos);

        show();
      }
    }

    void show() {
      if (isShowing)
        return;

      tablePanel.remove(currentComponent);
      split.setLeftComponent(currentComponent);
      split.setDividerLocation(splitPos);
      currentComponent = split;
      tablePanel.add(currentComponent, BorderLayout.CENTER);
      tablePanel.revalidate();
      isShowing = true;
    }

    void hide() {
      if (!isShowing)
        return;
      tablePanel.remove(currentComponent);

      if (split != null) {
        splitPos = split.getDividerLocation();
        currentComponent = (JComponent) split.getLeftComponent();
        tablePanel.add(currentComponent, BorderLayout.CENTER);
      }

      tablePanel.revalidate();
      isShowing = false;
    }

    void setSelected(Variable vs) {
      List<VariableBean> beans = table.getBeans();
      for (VariableBean bean : beans) {
        if (bean.vs == vs) {
          table.setSelectedBean(bean);
          return;
        }
      }
    }

    void saveState() {
      table.saveState(false);
      if (split != null)
        myPrefs.putInt("splitPos" + level, split.getDividerLocation());
    }
  }

  private void showDeclaration(BeanTable<VariableBean> from, boolean isNcml) {
    Variable v = getSelectedVariable(from);
    if (v == null)
      return;
    infoTA.clear();

    if (isNcml) {
      NcmlWriter ncmlWriter = new NcmlWriter();
      // LOOK ncmlWriter.setNamespace(null);
      ncmlWriter.getXmlFormat().setOmitDeclaration(true);

      Element varElement = ncmlWriter.makeVariableElement(v, false);
      infoTA.appendLine(ncmlWriter.writeToString(varElement));
    } else {
      infoTA.appendLine(v.toString());
    }

    infoTA.gotoTop();
    infoWindow.setTitle("Variable Info");
    infoWindow.show();
  }

  private void showData(BeanTable<VariableBean> from) {
    Variable v = getSelectedVariable(from);
    if (v == null) {
      return;
    }
    infoTA.clear();

    try {
      if (v.isMemberOfStructure()) {
        Structure parent = v.getParentStructure();
        StructureDataArray alldata = (StructureDataArray) parent.readArray();
        StructureMembers.Member m = alldata.getStructureMembers().findMember(v.getShortName());
        Array<?> memdata = alldata.extractMemberArray(m);
        infoTA.setText(NcdumpArray.printArray(memdata, v.getFullName(), null));

      } else if (v instanceof Sequence) {
        infoTA.setText(NcdumpArray.printSequenceData((Sequence) v, null));

      } else {
        infoTA.setText(NcdumpArray.printArray(v.readArray(), v.getFullName(), null));
      }

    } catch (Exception ex) {
      StringWriter s = new StringWriter();
      ex.printStackTrace(new PrintWriter(s));
      infoTA.setText(s.toString());
    }

    infoTA.gotoTop();
    infoWindow.setTitle("Variable Info");
    infoWindow.show();
  }

  private void dumpData(BeanTable<VariableBean> from) {
    Variable v = getSelectedVariable(from);
    if (v == null)
      return;

    dumpPane.clear();
    String spec;

    try {
      spec = ParsedArraySectionSpec.makeSectionSpecString(v, null);
      dumpPane.setContext(ds, spec);

    } catch (Exception ex) {
      StringWriter s = new StringWriter();
      ex.printStackTrace(new PrintWriter(s));
      dumpPane.setText(s.toString());
    }

    dumpWindow.show();
  }

  private void dataArrayTable(BeanTable<VariableBean> from) {
    Variable v = getSelectedVariable(from);
    if (v instanceof Structure) {
      try {
        dataArrayTable.setStructure((Structure) v);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      dataArrayWindow.setComponent(dataArrayTable);
    } else {
      List<VariableBean> l = from.getSelectedBeans();
      List<Variable> vl = new ArrayList<>();
      for (VariableBean vb1 : l) {
        if (vb1 != null && vb1.vs != null) {
          vl.add(vb1.vs);
        }
      }
      variableTable.setDataset(ds);
      variableTable.setVariableList(vl);
      variableTable.createTable();
      dataArrayWindow.setComponent(variableTable);
    }
    Rectangle r = (Rectangle) prefs.getBean("dataWindowBounds", new Rectangle(50, 300, 1000, 1200));
    dataArrayWindow.setBounds(r);
    dataArrayWindow.show();
  }

  private void dataPlot(BeanTable<VariableBean> from) {
    List<VariableBean> l = from.getSelectedBeans();

    for (VariableBean vb : l) {
      if (vb == null)
        return;
      Variable v = vb.vs;
      if (v != null) {
        try {
          dataPlot.setDataset(ds);
          dataPlot.setVariable(v);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      } else
        return;
    }
    plotWindow.show();
  }

  private Variable getSelectedVariable(BeanTable<VariableBean> from) {
    VariableBean vb = from.getSelectedBean();
    return vb == null ? null : vb.vs;
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    dumpPane.save();
    fileChooser.save();

    for (NestedTable nt : nestedTableList) {
      nt.saveState();
    }
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("dataWindowBounds", dataArrayWindow.getBounds());
    prefs.putBeanObject("DumpWindowBounds", dumpWindow.getBounds());
    prefs.putBeanObject("PlotWindowBounds", plotWindow.getBounds());
    if (attWindow != null)
      prefs.putBeanObject("AttWindowBounds", attWindow.getBounds());

    prefs.putInt("mainSplit", mainSplit.getDividerLocation());
  }

  private List<VariableBean> getVariableBeans(NetcdfFile ds) {
    List<VariableBean> vlist = new ArrayList<>();
    for (Variable v : ds.getVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  private List<VariableBean> getStructureVariables(Structure s) {
    List<VariableBean> vlist = new ArrayList<>();
    for (Variable v : s.getVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  public static class VariableBean {
    // static public String editableProperties() { return "title include logging freq"; }
    private final Variable vs;
    private String name, dimensions, desc, units, dataType, shape;

    // create from a dataset
    public VariableBean(Variable vs) {
      this.vs = vs;

      setName(vs.getShortName());
      setDescription(vs.getDescription());
      setUnits(vs.getUnitsString());
      setDataType(vs.getArrayType().toString());

      // collect dimensions
      StringBuilder lens = new StringBuilder();
      StringBuilder names = new StringBuilder();
      int count = 0;
      for (ucar.nc2.Dimension dim : vs.getDimensions()) {
        if (count++ > 0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getShortName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDimensions(names.toString());
      setShape(lens.toString());
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getGroup() {
      return vs.getParentGroup().getFullName();
    }

    public String getDimensions() {
      return dimensions;
    }

    public void setDimensions(String dimensions) {
      this.dimensions = dimensions;
    }

    /*
     * public boolean isCoordVar() { return isCoordVar; }
     * public void setCoordVar(boolean isCoordVar) { this.isCoordVar = isCoordVar; }
     * 
     * /* public boolean isAxis() { return axis; }
     * public void setAxis(boolean axis) { this.axis = axis; }
     * 
     * public boolean isGeoGrid() { return isGrid; }
     * public void setGeoGrid(boolean isGrid) { this.isGrid = isGrid; }
     * 
     * public String getAxisType() { return axisType; }
     * public void setAxisType(String axisType) { this.axisType = axisType; }
     */

    // public String getCoordSys() { return coordSys; }
    // public void setCoordSys(String coordSys) { this.coordSys = coordSys; }

    /*
     * public String getPositive() { return positive; }
     * public void setPositive(String positive) { this.positive = positive; }
     * 
     */

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
      this.units = units;
    }

    public String getDataType() {
      return dataType;
    }

    public void setDataType(String dataType) {
      this.dataType = dataType;
    }

    public String getShape() {
      return shape;
    }

    public void setShape(String shape) {
      this.shape = shape;
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

  /*
   * public class StructureBean {
   * // static public String editableProperties() { return "title include logging freq"; }
   * 
   * private String name;
   * private int domainRank, rangeRank;
   * private boolean isGeoXY, isLatLon, isProductSet, isZPositive;
   * 
   * // no-arg constructor
   * public StructureBean() {}
   * 
   * // create from a dataset
   * public StructureBean( Structure s) {
   * 
   * 
   * setName( cs.getName());
   * setGeoXY( cs.isGeoXY());
   * setLatLon( cs.isLatLon());
   * setProductSet( cs.isProductSet());
   * setDomainRank( cs.getDomain().size());
   * setRangeRank( cs.getCoordinateAxes().size());
   * //setZPositive( cs.isZPositive());
   * }
   * 
   * public String getName() { return name; }
   * public void setName(String name) { this.name = name; }
   * 
   * public boolean isGeoXY() { return isGeoXY; }
   * public void setGeoXY(boolean isGeoXY) { this.isGeoXY = isGeoXY; }
   * 
   * public boolean getLatLon() { return isLatLon; }
   * public void setLatLon(boolean isLatLon) { this.isLatLon = isLatLon; }
   * 
   * public boolean isProductSet() { return isProductSet; }
   * public void setProductSet(boolean isProductSet) { this.isProductSet = isProductSet; }
   * 
   * public int getDomainRank() { return domainRank; }
   * public void setDomainRank(int domainRank) { this.domainRank = domainRank; }
   * 
   * public int getRangeRank() { return rangeRank; }
   * public void setRangeRank(int rangeRank) { this.rangeRank = rangeRank; }
   * 
   * //public boolean isZPositive() { return isZPositive; }
   * //public void setZPositive(boolean isZPositive) { this.isZPositive = isZPositive; }
   * }
   */

}
