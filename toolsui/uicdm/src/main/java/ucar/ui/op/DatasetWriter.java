/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import static ucar.nc2.internal.util.CompareNetcdf2.IDENTITY_FILTER;

import com.google.common.base.Stopwatch;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.Variable.Builder;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NcdumpArray;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.write.NcmlWriter;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.ui.StructureArrayTable;
import ucar.ui.dialog.CompareDialog;
import ucar.ui.dialog.NetcdfOutputChooser;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.FileManager;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.ProgressMonitor;
import ucar.ui.widget.ProgressMonitorTask;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import org.jdom2.Element;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

/** UI for writing datasets to netcdf formatted disk files. */
public class DatasetWriter extends JPanel {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final FileManager fileChooser;

  private final PreferencesExt prefs;
  private NetcdfFile ds;

  private final List<NestedTable> nestedTableList = new ArrayList<>();
  private BeanTable<AttributeBean> attTable;
  private final BeanTable<DimensionBean> dimTable;

  private final JPanel tablePanel;
  private final JSplitPane mainSplit;

  private JComponent currentComponent;

  private final TextHistoryPane infoTA;
  private final StructureArrayTable dataTable;
  private final IndependentWindow infoWindow;
  private final IndependentWindow dataWindow;
  private IndependentWindow attWindow;

  private Nc4Chunking chunker = Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 0, false);

  private CompareDialog dialog;

  public DatasetWriter(PreferencesExt prefs, FileManager fileChooser) {
    this.prefs = prefs;
    this.fileChooser = fileChooser;

    // create the variable table(s)
    dimTable = new BeanTable<>(DimensionBean.class, (PreferencesExt) prefs.node("DimensionBeanTable"), false,
        "Dimensions", null, new DimensionBean());

    tablePanel = new JPanel(new BorderLayout());
    setNestedTable(0, null);

    NetcdfOutputChooser outputChooser = new NetcdfOutputChooser((Frame) null);
    outputChooser.addPropertyChangeListener("OK", evt -> writeFile((NetcdfOutputChooser.Data) evt.getNewValue()));
    outputChooser.addEventListener(e -> {
      Nc4Chunking.Strategy strategy = (Nc4Chunking.Strategy) e.getItem();
      chunker = Nc4ChunkingStrategy.factory(strategy, 0, false);
      showChunking();
    });

    // layout
    mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, dimTable, tablePanel);
    mainSplit.setDividerLocation(prefs.getInt("mainSplit", 300));

    setLayout(new BorderLayout());
    add(outputChooser.getContentPane(), BorderLayout.NORTH);
    add(mainSplit, BorderLayout.CENTER);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // the data Table
    dataTable = new StructureArrayTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("nj22/NetcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));

    /*
     * the ncdump Pane
     * dumpPane = new NCdumpPane((PreferencesExt) prefs.node("dumpPane"));
     * dumpWindow = new IndependentWindow("NCDump Variable Data", BAMutil.getImage( "netcdfUI"), dumpPane);
     * dumpWindow.setBounds( (Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle( 300, 300, 300, 200)));
     */
  }

  public void addActions(JPanel buttPanel) {
    AbstractAction attAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showAtts();
      }
    };
    BAMutil.setActionProperties(attAction, "FontDecr", "global attributes", false, 'A', -1);
    BAMutil.addActionToContainer(buttPanel, attAction);
  }

  public void save() {
    dimTable.saveState(false);

    for (NestedTable nt : nestedTableList) {
      nt.saveState();
    }

    // prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    // prefs.putBeanObject("DumpWindowBounds", dumpWindow.getBounds());
    // if (attWindow != null) prefs.putBeanObject("AttWindowBounds", attWindow.getBounds());

    prefs.putInt("mainSplit", mainSplit.getDividerLocation());
  }

  private void showChunking() {
    if (nestedTableList.isEmpty()) {
      return;
    }

    NestedTable t = nestedTableList.get(0);
    for (VariableBean bean : t.table.getBeans()) {
      boolean isChunked = chunker.isChunked(bean.vs);
      bean.setChunked(isChunked);
      if (isChunked) {
        bean.setChunkArray(chunker.getChunking(bean.vs));
      } else {
        bean.setChunkArray(null);
      }
    }
    t.table.refresh();
  }

  void writeFile(NetcdfOutputChooser.Data data) {
    if (ds == null) {
      return;
    }

    String filename = data.outputFilename.trim();
    if (filename.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Filename has not been set");
      return;
    }

    if (data.format.isNetdf4format()) {
      return;
      /*
       * if (!NetcdfClibrary.isLibraryPresent()) {
       * JOptionPane.showMessageDialog(this, "NetCDF-4 C library is not loaded");
       * return;
       * }
       */
    }

    WriterTask task = new WriterTask(data);
    ProgressMonitor pm = new ProgressMonitor(task, (e) -> logger.debug("success}"));
    pm.start(null, "Writing " + filename, ds.getAllVariables().size());
  }

  class WriterTask extends ProgressMonitorTask implements CancelTask {

    NetcdfOutputChooser.Data data;

    WriterTask(NetcdfOutputChooser.Data data) {
      this.data = data;
    }

    public void run() {
      try {
        List<VariableBean> beans = nestedTableList.get(0).table.getBeans();
        BeanChunker bc = new BeanChunker(beans, data.deflate, data.shuffle);
        NetcdfFormatWriter.Builder<?> builder =
            NetcdfFormatWriter.builder().setFormat(data.format).setLocation(data.outputFilename).setChunker(bc);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (NetcdfCopier copier = NetcdfCopier.create(ds, builder)) {
          // write returns the open file that was just written, so we just need to close it.
          copier.write(this);
        }

        File oldFile = new File(ds.getLocation());
        File newFile = new File(data.outputFilename);

        double r = (double) newFile.length() / oldFile.length();

        logger.debug("Rewrite from {} {} to {} {} format = {} ratio = {} took= {} secs", ds.getLocation(),
            oldFile.length(), data.outputFilename, newFile.length(), data.format, r, stopwatch);
        JOptionPane.showMessageDialog(DatasetWriter.this,
            "File successfully written took=" + stopwatch.stop() + " secs ratio=" + r);
      } catch (Exception ioe) {
        JOptionPane.showMessageDialog(DatasetWriter.this, "ERROR: " + ioe.getMessage());
        ioe.printStackTrace();
      } finally {
        success = !cancel && !isError();
        done = true; // do last!
      }
    }
  }

  public void compareDataset() {
    if (ds == null) {
      return;
    }

    if (dialog == null) {
      dialog = new CompareDialog(null, fileChooser);
      dialog.pack();
      dialog.addPropertyChangeListener("OK", evt -> {
        CompareDialog.Data data = (CompareDialog.Data) evt.getNewValue();
        // logger.debug("name={} {}", evt.getPropertyName(), data);
        compareDataset(data);
      });
    }
    dialog.setVisible(true);
  }

  private void compareDataset(CompareDialog.Data data) {
    if (data.name == null) {
      return;
    }

    try (NetcdfFile compareFile = NetcdfDatasets.openFile(data.name, null)) {
      Formatter f = new Formatter();
      CompareNetcdf2 cn = new CompareNetcdf2(f, data.showCompare, data.showDetails, data.readData);

      if (data.howMuch == CompareDialog.HowMuch.All) {
        cn.compare(ds, compareFile);
      } else {
        NestedTable nested = nestedTableList.get(0);
        Variable org = getCurrentVariable(nested.table);
        if (org == null) {
          return;
        }
        Variable ov = compareFile.findVariable(NetcdfFiles.makeFullName(org));
        if (ov != null) {
          cn.compareVariable(org, ov, IDENTITY_FILTER);
        }
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

  public void showAtts() {
    if (ds == null) {
      return;
    }

    if (attTable == null) {
      // global attributes
      attTable = new BeanTable<>(AttributeBean.class, (PreferencesExt) prefs.node("AttributeBeans"), false);
      PopupMenu varPopup = new PopupMenu(attTable.getJTable(), "Options");
      varPopup.addAction("Show Attribute", new AbstractAction() {
        @Override
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

  public void setDataset(NetcdfFile ds) {
    this.ds = ds;
    dimTable.setBeans(makeDimensionBeans(ds));
    NestedTable nt = nestedTableList.get(0);
    nt.table.setBeans(makeVariableBeans(ds));
    hideNestedTable(1);
    showChunking();
  }

  private void setSelected(Variable v) {
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
  }

  private void showDeclaration(BeanTable<VariableBean> from, boolean isNcml) {
    Variable v = getCurrentVariable(from);
    if (v == null) {
      return;
    }

    infoTA.clear();

    if (isNcml) {
      NcmlWriter ncmlWriter = new NcmlWriter();
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

  private void dataTable(BeanTable<VariableBean> from) {
    VariableBean vb = from.getSelectedBean();
    if (vb == null) {
      return;
    }

    Variable v = vb.vs;

    if (v instanceof Structure) {
      try {
        dataTable.setStructure((Structure) v);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      return;
    }

    dataWindow.show();
  }

  private Variable getCurrentVariable(BeanTable<VariableBean> from) {
    VariableBean vb = from.getSelectedBean();
    if (vb == null) {
      return null;
    }

    return vb.vs;
  }

  private List<VariableBean> makeVariableBeans(NetcdfFile ds) {
    List<VariableBean> vlist = new ArrayList<>();
    for (Variable v : ds.getAllVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  private List<DimensionBean> makeDimensionBeans(NetcdfFile ds) {
    List<DimensionBean> dlist = new ArrayList<>();
    for (Dimension d : ds.getAllDimensions()) {
      dlist.add(new DimensionBean(d));
    }
    return dlist;
  }

  private List<VariableBean> getStructureVariables(Structure s) {
    List<VariableBean> vlist = new ArrayList<>();
    for (Variable v : s.getVariables()) {
      vlist.add(new VariableBean(v));
    }
    return vlist;
  }

  private NestedTable setNestedTable(int level, Structure s) {
    NestedTable ntable;
    if (nestedTableList.size() < level + 1) {
      ntable = new NestedTable(level);
      nestedTableList.add(ntable);
    } else {
      ntable = nestedTableList.get(level);
    }

    if (s != null) {
      // variables inside of records
      ntable.table.setBeans(getStructureVariables(s));
    }
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

      table = new BeanTable<>(VariableBean.class, myPrefs, false, "Variables", null, new VariableBean());

      JTable jtable = table.getJTable();
      PopupMenu csPopup = new PopupMenu(jtable, "Options");
      csPopup.addAction("Show Declaration", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, false);
        }
      });
      csPopup.addAction("Show NcML", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table, true);
        }
      });

      if (level == 0) {
        csPopup.addAction("Data Table", new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            dataTable(table);
          }
        });
      }

      // get selected variable, see if its a structure
      table.addListSelectionListener(e -> {
        Variable v = getCurrentVariable(table);
        if (v instanceof Structure) {
          hideNestedTable(this.level + 2);
          setNestedTable(this.level + 1, (Structure) v);
        } else {
          hideNestedTable(this.level + 1);
        }
        // if (eventsOK) datasetTree.setSelected( v);
      });

      // layout
      if (currentComponent == null) {
        currentComponent = table;
        tablePanel.add(currentComponent, BorderLayout.CENTER);
        isShowing = true;

      } else {
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, currentComponent, table);
        splitPos = myPrefs.getInt("splitPos" + level, 500);
        if (splitPos > 0) {
          split.setDividerLocation(splitPos);
        }
        show();
      }
    }

    void show() {
      if (isShowing) {
        return;
      }

      tablePanel.remove(currentComponent);
      split.setLeftComponent(currentComponent);
      split.setDividerLocation(splitPos);
      currentComponent = split;
      tablePanel.add(currentComponent, BorderLayout.CENTER);
      tablePanel.revalidate();
      isShowing = true;
    }

    void hide() {
      if (!isShowing) {
        return;
      }

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
      for (VariableBean bean : table.getBeans()) {
        if (bean.vs == vs) {
          table.setSelectedBean(bean);
          return;
        }
      }
    }

    void saveState() {
      table.saveState(false);
      if (split != null) {
        myPrefs.putInt("splitPos" + level, split.getDividerLocation());
      }
    }
  }

  public static class BeanChunker implements Nc4Chunking {
    Map<String, VariableBean> map;
    int deflate;
    boolean shuffle;

    BeanChunker(List<VariableBean> beans, int deflate, boolean shuffle) {
      this.map = new HashMap<>(2 * beans.size());
      for (VariableBean bean : beans) {
        map.put(bean.vs.getFullName(), bean);
      }
      this.deflate = deflate;
      this.shuffle = shuffle;
    }

    @Override
    public boolean isChunked(Variable v) {
      VariableBean bean = map.get(v.getFullName());
      return (bean != null) && bean.isChunked();
    }

    @Override
    public boolean isChunked(Builder<?> vb) {
      return false;
    }

    @Override
    public long[] getChunking(Variable v) {
      VariableBean bean = map.get(v.getFullName());
      return (bean == null) ? new long[0] : bean.chunked;
    }

    @Override
    public long[] computeChunking(Builder<?> vb) {
      return new long[0];
    }

    @Override
    public int getDeflateLevel(Builder<?> vb) {
      return deflate;
    }

    @Override
    public boolean isShuffle(Builder<?> vb) {
      return shuffle;
    }
  }

  public static class DimensionBean {

    Dimension ds;

    /**
     * no-arg constructor
     */
    public DimensionBean() {}

    /**
     * create from a dimension
     */
    public DimensionBean(Dimension ds) {
      this.ds = ds;
    }

    public String editableProperties() {
      return "unlimited";
    }

    public String getName() {
      return ds.getShortName();
    }

    public int getLength() {
      return ds.getLength();
    }

    public boolean isUnlimited() {
      return ds.isUnlimited();
    }

  }

  public class VariableBean {

    private Variable vs;
    private String name, dimensions, desc, units, dataType, shape;
    private boolean isChunked;
    private long[] chunked;

    /**
     * no-arg constructor
     */
    public VariableBean() {}

    // create from a dataset
    public VariableBean(Variable vs) {
      this.vs = vs;

      setName(vs.getShortName());
      setDescription(vs.getDescription());
      setUnits(vs.getUnitsString());
      setDataType(vs.getArrayType().toString());

      // collect dimensions
      Formatter lens = new Formatter();
      Formatter names = new Formatter();
      lens.format("(");
      List<Dimension> dims = vs.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        Dimension dim = dims.get(j);
        if (j > 0) {
          lens.format(",");
          names.format(",");
        }
        String name = dim.isShared() ? dim.getShortName() : "anon";
        names.format("%s", name);
        lens.format("%d", dim.getLength());
      }
      lens.format(")");
      setDimensions(names.toString());
      setShape(lens.toString());
    }

    public String editableProperties() {
      return "chunked chunkSize";
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

    public boolean isUnlimited() {
      return vs.isUnlimited();
    }

    public String getSize() {
      Formatter f = new Formatter();
      f.format("%,d", vs.getSize());
      return f.toString();
    }

    public boolean isChunked() {
      return isChunked;
    }

    public void setChunked(boolean chunked) {
      isChunked = chunked;
      if (chunked) {
        setChunkArray(chunker.getChunking(vs));
      } else {
        setChunkArray(null);
      }
    }

    public long getNChunks() {
      if (!isChunked) {
        return 1;
      }
      if (chunked == null) {
        return 1;
      }

      long elementsPerChunk = 1;
      for (long c : chunked) {
        elementsPerChunk *= c;
      }
      return vs.getSize() / elementsPerChunk;
    }

    public long getOverHang() {
      if (!isChunked) {
        return 0;
      }
      if (chunked == null) {
        return 0;
      }

      int[] shape = vs.getShape();

      long total = 1;

      for (int i = 0; i < chunked.length; i++) {
        int overhang = (int) (shape[i] % chunked[i]);
        total *= overhang;
      }
      return total;
    }

    public String getOHPercent() {
      if (!isChunked) {
        return "";
      }
      if (chunked == null) {
        return "";
      }

      long total = getOverHang();
      float p = 100.0f * total / vs.getSize();

      Formatter f = new Formatter();
      f.format("%6.3f", p);

      return f.toString();
    }

    public String getChunkSize() {
      if (chunked == null) {
        return "";
      }

      Formatter f = new Formatter();
      f.format("(");

      for (int i = 0; i < chunked.length; i++) {
        f.format("%d", chunked[i]);
        if (i < chunked.length - 1) {
          f.format(",");
        }
      }
      f.format(")");
      return f.toString();
    }

    public void setChunkSize(String chunkSize) {
      StringTokenizer stoke = new StringTokenizer(chunkSize, "(), ");
      this.chunked = new long[stoke.countTokens()];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        this.chunked[count++] = Long.parseLong(stoke.nextToken());
      }
    }

    public void setChunkArray(long[] chunked) {
      this.chunked = chunked;
      this.isChunked = (chunked != null);
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
}
