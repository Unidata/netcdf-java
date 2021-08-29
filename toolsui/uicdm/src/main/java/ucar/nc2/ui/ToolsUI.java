/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui;

import java.util.Formatter;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import thredds.ui.catalog.ThreddsUI;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.internal.http.HttpService;
import ucar.nc2.internal.iosp.hdf5.H5iosp;
import ucar.nc2.internal.ncml.Aggregation;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.ui.bufr.BufrCdmIndexOpPanel;
import ucar.nc2.ui.bufr.BufrCodePanel;
import ucar.nc2.ui.bufr.BufrPanel;
import ucar.nc2.ui.bufr.BufrReportPanel;
import ucar.nc2.ui.bufr.BufrTableBPanel;
import ucar.nc2.ui.bufr.BufrTableDPanel;
import ucar.nc2.ui.dialog.DiskCache2Form;
import ucar.nc2.ui.grib.*;
import ucar.nc2.ui.grid.GeoGridPanel;
import ucar.nc2.ui.grid.GridNewPanel;
import ucar.nc2.ui.menu.*;
import ucar.nc2.ui.op.*;
import ucar.nc2.ui.point.PointFeaturePanel;
import ucar.nc2.ui.util.SocketMessage;
import ucar.nc2.ui.widget.URLDumpPane;
import ucar.nc2.ui.widget.UrlAuthenticatorDialog;
import ucar.nc2.write.NetcdfCopier;
import ucar.ui.widget.*;
import ucar.ui.widget.ProgressMonitor;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.internal.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.nc2.internal.cache.FileCache;
import ucar.nc2.internal.util.xml.RuntimeConfigParser;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.http.ReadFromUrl;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import ucar.ui.prefs.Debug;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/** Netcdf Tools user interface. */
public class ToolsUI extends JPanel {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DIALOG_VERSION = "7.0";

  public static final String WORLD_DETAIL_MAP = "/resources/ui/maps/Countries.shp";
  public static final String US_MAP = "/resources/ui/maps/us_state.shp";
  public static final String FRAME_SIZE = "FrameSize";

  private static ToolsUI ui;
  private static JFrame frame;
  private static PreferencesExt prefs;
  private static XMLStore store;
  private static boolean done;

  private static String wantDataset;

  private final JFrame parentFrame; // redundant? will equal static "frame" defined just above

  private final FileManager fileChooser;
  private final FileManager dirChooser;
  private FileManager bufrFileChooser;

  private final JTabbedPane tabbedPane;
  private final JTabbedPane iospTabPane;
  private final JTabbedPane gribTabPane;
  private final JTabbedPane grib2TabPane;
  private final JTabbedPane grib1TabPane;
  private final JTabbedPane ftTabPane;
  private final JTabbedPane ncmlTabPane;

  private final PreferencesExt mainPrefs;

  // Op panels and friends
  private AggPanel aggPanel;
  private BufrPanel bufrPanel;
  private BufrTableBPanel bufrTableBPanel;
  private BufrTableDPanel bufrTableDPanel;
  private ReportOpPanel bufrReportPanel;
  private BufrCdmIndexOpPanel bufrCdmIndexPanel;
  private BufrCodePanel bufrCodePanel;
  private CdmIndexOpPanel cdmIndexPanel;
  private CdmIndexScanOp cdmIndexScanOp;
  private ReportOpPanel cdmIndexReportPanel;
  private CollectionSpecPanel fcPanel;
  private CoordSysPanel coordSysPanel;
  private CoveragePanel coveragePanel;
  private final DatasetViewerPanel viewerPanel;
  private DatasetViewerPanel nc4viewer;
  private DatasetWriterPanel writerPanel;
  private DirectoryPartitionPanel dirPartPanel;
  private FeatureScanOpPanel ftPanel;
  private GeoGridPanel geoGridPanel;
  private GribCodePanel gribCodePanel;
  private GribFilesOpPanel gribFilesPanel;
  private GribIndexOpPanel gribIdxPanel;
  private GribRewriteOpPanel gribRewritePanel;
  private GribTemplatePanel gribTemplatePanel;
  private Grib1CollectionOpPanel grib1CollectionPanel;
  private GridNewPanel gridNewPanel;
  private ReportOpPanel grib1ReportPanel;
  private Grib1TablePanel grib1TablePanel;
  private Grib2CollectionOpPanel grib2CollectionPanel;
  private Grib2TablePanel grib2TablePanel;
  private ReportOpPanel grib2ReportPanel;
  private Grib1DataOpPanel grib1DataPanel;
  private Grib2DataOpPanel grib2DataPanel;
  private Hdf5ObjectPanel hdf5ObjectPanel;
  private Hdf5DataPanel hdf5DataPanel;
  private Hdf4Panel hdf4Panel;
  private NCdumpPanel ncdumpPanel;
  private NcmlEditorPanel ncmlEditorPanel;
  private PointFeaturePanel pointFeaturePanel;
  private SimpleGeomPanel simpleGeomPanel;
  private ThreddsUI threddsUI;
  private UnitsPanel unitsPanel;
  private URLDumpPane urlPanel;
  private WmoCCPanel wmoCommonCodePanel;
  private WmsPanel wmsPanel;

  // data
  private final DataFactory threddsDataFactory = new DataFactory();

  private boolean useRecordStructure;
  private boolean useBuilders = true;
  private DiskCache2Form diskCache2Form;

  // debugging
  private final DebugFlags debugFlags;
  private static final boolean debugListen = false;

  private ToolsUI(PreferencesExt prefs, JFrame parentFrame) {
    this.mainPrefs = prefs;
    this.parentFrame = parentFrame;

    // FileChoosers are shared
    FileFilter[] filters = {new FileManager.HDF5ExtFilter(), new FileManager.NetcdfExtFilter()};
    fileChooser = new FileManager(parentFrame, null, filters, (PreferencesExt) prefs.node("FileManager"));
    dirChooser = new FileManager(parentFrame, null, null, (PreferencesExt) prefs.node("FeatureScanFileManager"));

    OpPanel.setFileChooser(fileChooser);

    // all the tabbed panes
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    iospTabPane = new JTabbedPane(JTabbedPane.TOP);
    gribTabPane = new JTabbedPane(JTabbedPane.TOP);
    grib2TabPane = new JTabbedPane(JTabbedPane.TOP);
    grib1TabPane = new JTabbedPane(JTabbedPane.TOP);
    JTabbedPane bufrTabPane = new JTabbedPane(JTabbedPane.TOP);
    ftTabPane = new JTabbedPane(JTabbedPane.TOP);
    JTabbedPane fcTabPane = new JTabbedPane(JTabbedPane.TOP);
    JTabbedPane hdf5TabPane = new JTabbedPane(JTabbedPane.TOP);
    ncmlTabPane = new JTabbedPane(JTabbedPane.TOP);

    // Create and attach the initially visible panel in the top level tabbed pane
    viewerPanel = new DatasetViewerPanel((PreferencesExt) mainPrefs.node("varTable"));
    tabbedPane.addTab("Viewer", viewerPanel);

    // All other panels are deferred construction for fast startup
    tabbedPane.addTab("Writer", new JLabel("Writer"));
    tabbedPane.addTab("NCDump", new JLabel("NCDump"));
    tabbedPane.addTab("Iosp", iospTabPane);
    tabbedPane.addTab("CoordSys", new JLabel("CoordSys"));
    tabbedPane.addTab("FeatureTypes", ftTabPane);
    tabbedPane.addTab("THREDDS", new JLabel("THREDDS"));
    tabbedPane.addTab("Units", new JLabel("Units"));
    tabbedPane.addTab("NcML", ncmlTabPane);
    tabbedPane.addTab("URLdump", new JLabel("URLdump"));
    tabbedPane.setSelectedIndex(0);
    tabbedPane.addChangeListener(e -> {
      Component c = tabbedPane.getSelectedComponent();
      if (c instanceof JLabel) {
        int idx = tabbedPane.getSelectedIndex();
        String title = tabbedPane.getTitleAt(idx);
        makeComponent(tabbedPane, title);
      }
    });

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);

    // nested tab - iosp
    iospTabPane.addTab("BUFR", bufrTabPane);
    iospTabPane.addTab("GRIB", gribTabPane);
    iospTabPane.addTab("GRIB2", grib2TabPane);
    iospTabPane.addTab("GRIB1", grib1TabPane);
    iospTabPane.addTab("HDF5", hdf5TabPane);
    iospTabPane.addTab("HDF4", new JLabel("HDF4"));
    addListeners(iospTabPane);

    // nested-2 tab - bufr
    bufrTabPane.addTab("BUFR", new JLabel("BUFR"));
    bufrTabPane.addTab("BufrCdmIndex", new JLabel("BufrCdmIndex"));
    bufrTabPane.addTab("BUFRTableB", new JLabel("BUFRTableB"));
    bufrTabPane.addTab("BUFRTableD", new JLabel("BUFRTableD"));
    bufrTabPane.addTab("BUFR-CODES", new JLabel("BUFR-CODES"));
    bufrTabPane.addTab("BufrReports", new JLabel("BufrReports"));
    addListeners(bufrTabPane);

    // nested-2 tab - grib
    gribTabPane.addTab("CdmIndexScan", new JLabel("CdmIndexScan"));
    gribTabPane.addTab("CdmIndex4", new JLabel("CdmIndex4"));
    gribTabPane.addTab("CdmIndexReport", new JLabel("CdmIndexReport"));
    gribTabPane.addTab("GribIndex", new JLabel("GribIndex"));
    gribTabPane.addTab("WMO-COMMON", new JLabel("WMO-COMMON"));
    gribTabPane.addTab("WMO-CODES", new JLabel("WMO-CODES"));
    gribTabPane.addTab("WMO-TEMPLATES", new JLabel("WMO-TEMPLATES"));
    gribTabPane.addTab("GRIB-Rewrite", new JLabel("GRIB-Rewrite"));
    addListeners(gribTabPane);

    // nested-2 tab - grib-2
    grib2TabPane.addTab("GRIB2collection", new JLabel("GRIB2collection"));
    grib2TabPane.addTab("GRIB2-REPORT", new JLabel("GRIB2-REPORT"));
    grib2TabPane.addTab("GRIB2data", new JLabel("GRIB2data"));
    grib2TabPane.addTab("GRIB2-TABLES", new JLabel("GRIB2-TABLES"));
    addListeners(grib2TabPane);

    // nested-2 tab - grib-1
    grib1TabPane.addTab("GRIB1collection", new JLabel("GRIB1collection"));
    grib1TabPane.addTab("GRIB-FILES", new JLabel("GRIB-FILES"));
    grib1TabPane.addTab("GRIB1-REPORT", new JLabel("GRIB1-REPORT"));
    grib1TabPane.addTab("GRIB1data", new JLabel("GRIB1data"));
    grib1TabPane.addTab("GRIB1-TABLES", new JLabel("GRIB1-TABLES"));
    addListeners(grib1TabPane);

    // nested-2 tab - hdf5
    hdf5TabPane.addTab("HDF5-Objects", new JLabel("HDF5-Objects"));
    hdf5TabPane.addTab("HDF5-Data", new JLabel("HDF5-Data"));
    hdf5TabPane.addTab("Netcdf4-JNI", new JLabel("Netcdf4-JNI"));
    addListeners(hdf5TabPane);

    // nested tab - features
    ftTabPane.addTab("FeatureScan", new JLabel("FeatureScan"));
    ftTabPane.addTab("Grids", new JLabel("Grids"));
    ftTabPane.addTab("Coverages", new JLabel("Coverages"));
    ftTabPane.addTab("GridNew", new JLabel("GridNew"));
    ftTabPane.addTab("SimpleGeometry", new JLabel("SimpleGeometry"));
    ftTabPane.addTab("WMS", new JLabel("WMS"));
    ftTabPane.addTab("PointFeature", new JLabel("PointFeature"));
    ftTabPane.addTab("FeatureCollection", fcTabPane);
    addListeners(ftTabPane);

    // nested tab - feature collection
    fcTabPane.addTab("DirectoryPartition", new JLabel("DirectoryPartition"));
    // fcTabPane.addTab("PartitionReport", new JLabel("PartitionReport"));
    fcTabPane.addTab("CollectionSpec", new JLabel("CollectionSpec"));
    addListeners(fcTabPane);

    // nested tab - ncml
    ncmlTabPane.addTab("NcmlEditor", new JLabel("NcmlEditor"));
    ncmlTabPane.addTab("Aggregation", new JLabel("Aggregation"));
    addListeners(ncmlTabPane);

    // dynamic proxy for DebugFlags
    debugFlags = (DebugFlags) Proxy.newProxyInstance(DebugFlags.class.getClassLoader(), new Class[] {DebugFlags.class},
        new DebugProxyHandler());

    JMenuBar mb = makeMenuBar();
    parentFrame.setJMenuBar(mb);

    setDebugFlags();
  }

  private void addListeners(JTabbedPane tabPane) {
    tabPane.addChangeListener(e -> {
      Component c = tabPane.getSelectedComponent();
      if (c instanceof JLabel) {
        int idx = tabPane.getSelectedIndex();
        String title = tabPane.getTitleAt(idx);
        makeComponent(tabPane, title);
      }
    });
    tabPane.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        Component c = tabPane.getSelectedComponent();
        if (c instanceof JLabel) {
          int idx = tabPane.getSelectedIndex();
          String title = tabPane.getTitleAt(idx);
          makeComponent(tabPane, title);
        }
      }
    });
  }

  /**
   * deferred creation of components to minimize startup
   */
  private void makeComponent(JTabbedPane parent, String title) {
    if (parent == null) {
      parent = tabbedPane;
    }

    // find the correct index
    int n = parent.getTabCount();
    int idx;
    for (idx = 0; idx < n; idx++) {
      String cTitle = parent.getTitleAt(idx);
      if (cTitle.equals(title)) {
        break;
      }
    }
    if (idx >= n) {
      log.debug("Cant find {} in {}", title, parent);
      return;
    }

    Component c;
    switch (title) {
      case "Aggregation":
        aggPanel = new AggPanel((PreferencesExt) mainPrefs.node("NcMLAggregation"));
        c = aggPanel;
        break;

      case "BUFR":
        bufrPanel = new BufrPanel((PreferencesExt) mainPrefs.node("bufr"));
        c = bufrPanel;
        break;

      case "BUFRTableB":
        bufrTableBPanel = new BufrTableBPanel((PreferencesExt) mainPrefs.node("bufr2"));
        c = bufrTableBPanel;
        break;

      case "BUFRTableD":
        bufrTableDPanel = new BufrTableDPanel((PreferencesExt) mainPrefs.node("bufrD"));
        c = bufrTableDPanel;
        break;

      case "BufrReports": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("bufrReports");
        ReportPanel rp = new BufrReportPanel(prefs);
        bufrReportPanel = new ReportOpPanel(prefs, rp);
        c = bufrReportPanel;
        break;
      }
      case "BUFR-CODES":
        bufrCodePanel = new BufrCodePanel((PreferencesExt) mainPrefs.node("bufr-codes"));
        c = bufrCodePanel;
        break;

      case "CollectionSpec":
        fcPanel = new CollectionSpecPanel((PreferencesExt) mainPrefs.node("collSpec"));
        c = fcPanel;
        break;

      case "DirectoryPartition":
        dirPartPanel = new DirectoryPartitionPanel((PreferencesExt) mainPrefs.node("dirPartition"));
        c = dirPartPanel;
        break;

      case "GRIB1collection":
        grib1CollectionPanel = new Grib1CollectionOpPanel((PreferencesExt) mainPrefs.node("grib1raw"));
        c = grib1CollectionPanel;
        break;

      case "GRIB1data":
        grib1DataPanel = new Grib1DataOpPanel((PreferencesExt) mainPrefs.node("grib1Data"));
        c = grib1DataPanel;
        break;

      case "GRIB-FILES":
        gribFilesPanel = new GribFilesOpPanel((PreferencesExt) mainPrefs.node("gribFiles"));
        c = gribFilesPanel;
        break;

      case "GRIB2collection":
        grib2CollectionPanel = new Grib2CollectionOpPanel((PreferencesExt) mainPrefs.node("gribNew"));
        c = grib2CollectionPanel;
        break;

      case "GRIB2data":
        grib2DataPanel = new Grib2DataOpPanel((PreferencesExt) mainPrefs.node("grib2Data"));
        c = grib2DataPanel;
        break;

      case "BufrCdmIndex":
        bufrCdmIndexPanel = new BufrCdmIndexOpPanel((PreferencesExt) mainPrefs.node("bufrCdmIdx"));
        c = bufrCdmIndexPanel;
        break;

      case "CdmIndex4":
        cdmIndexPanel = new CdmIndexOpPanel((PreferencesExt) mainPrefs.node("cdmIdx3"));
        c = cdmIndexPanel;
        break;

      case "CdmIndexScan":
        cdmIndexScanOp = new CdmIndexScanOp((PreferencesExt) mainPrefs.node("cdmIndexScan"), dirChooser);
        c = cdmIndexScanOp;
        break;

      case "CdmIndexReport": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("CdmIndexReport");
        ReportPanel rp = new CdmIndexReportPanel(prefs);
        cdmIndexReportPanel = new ReportOpPanel(prefs, rp);
        c = cdmIndexReportPanel;
        break;
      }

      case "GribIndex":
        gribIdxPanel = new GribIndexOpPanel((PreferencesExt) mainPrefs.node("gribIdx"));
        c = gribIdxPanel;
        break;

      case "GRIB1-REPORT": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("grib1Report");
        ReportPanel rp = new Grib1ReportPanel(prefs);
        grib1ReportPanel = new ReportOpPanel(prefs, rp);
        c = grib1ReportPanel;
        break;
      }

      case "GRIB2-REPORT": {
        PreferencesExt prefs = (PreferencesExt) mainPrefs.node("gribReport");
        ReportPanel rp = new Grib2ReportPanel(prefs);
        grib2ReportPanel = new ReportOpPanel(prefs, rp);
        c = grib2ReportPanel;
        break;
      }

      case "WMO-COMMON":
        wmoCommonCodePanel = new WmoCCPanel((PreferencesExt) mainPrefs.node("wmo-common"));
        c = wmoCommonCodePanel;
        break;

      case "WMO-CODES":
        gribCodePanel = new GribCodePanel((PreferencesExt) mainPrefs.node("wmo-codes"));
        c = gribCodePanel;
        break;

      case "WMO-TEMPLATES":
        gribTemplatePanel = new GribTemplatePanel((PreferencesExt) mainPrefs.node("wmo-templates"));
        c = gribTemplatePanel;

        break;
      case "GRIB1-TABLES":
        grib1TablePanel = new Grib1TablePanel((PreferencesExt) mainPrefs.node("grib1-tables"));
        c = grib1TablePanel;
        break;

      case "GRIB2-TABLES":
        grib2TablePanel = new Grib2TablePanel((PreferencesExt) mainPrefs.node("grib2-tables"));
        c = grib2TablePanel;
        break;

      case "GRIB-Rewrite":
        gribRewritePanel = new GribRewriteOpPanel((PreferencesExt) mainPrefs.node("grib-rewrite"));
        c = gribRewritePanel;
        break;

      case "CoordSys":
        coordSysPanel = new CoordSysPanel((PreferencesExt) mainPrefs.node("CoordSys"));
        c = coordSysPanel;
        break;

      case "FeatureScan":
        ftPanel = new FeatureScanOpPanel((PreferencesExt) mainPrefs.node("ftPanel"), dirChooser);
        c = ftPanel;
        break;

      case "Grids":
        geoGridPanel = new GeoGridPanel((PreferencesExt) mainPrefs.node("grid"));
        c = geoGridPanel;
        break;

      case "SimpleGeometry":
        simpleGeomPanel = new SimpleGeomPanel((PreferencesExt) mainPrefs.node("simpleGeom"));
        c = simpleGeomPanel;
        break;

      case "Coverages":
        coveragePanel = new CoveragePanel((PreferencesExt) mainPrefs.node("coverage2"));
        c = coveragePanel;
        break;

      case "GridNew":
        gridNewPanel = new GridNewPanel((PreferencesExt) mainPrefs.node("grid2Panel"));
        c = gridNewPanel;
        break;

      case "HDF5-Objects":
        hdf5ObjectPanel = new Hdf5ObjectPanel((PreferencesExt) mainPrefs.node("hdf5"), useBuilders);
        c = hdf5ObjectPanel;
        break;

      case "HDF5-Data":
        hdf5DataPanel = new Hdf5DataPanel((PreferencesExt) mainPrefs.node("hdf5data"));
        c = hdf5DataPanel;
        break;

      case "Netcdf4-JNI":
        nc4viewer = new DatasetViewerPanel((PreferencesExt) mainPrefs.node("nc4viewer"));
        c = nc4viewer;
        break;

      case "HDF4":
        hdf4Panel = new Hdf4Panel((PreferencesExt) mainPrefs.node("hdf4"), useBuilders);
        c = hdf4Panel;
        break;

      case "NCDump":
        ncdumpPanel = new NCdumpPanel((PreferencesExt) mainPrefs.node("NCDump"));
        c = ncdumpPanel;
        break;

      case "NcmlEditor":
        ncmlEditorPanel = new NcmlEditorPanel((PreferencesExt) mainPrefs.node("NcmlEditor"));
        c = ncmlEditorPanel;
        break;

      case "PointFeature":
        pointFeaturePanel = new PointFeaturePanel((PreferencesExt) mainPrefs.node("pointFeature"));
        c = pointFeaturePanel;
        break;

      case "THREDDS":
        threddsUI = new ThreddsUI(parentFrame, (PreferencesExt) mainPrefs.node("thredds"));
        threddsUI.addPropertyChangeListener(e -> {
          if (e.getPropertyName().equals("InvAccess")) {
            thredds.client.catalog.Access access = (thredds.client.catalog.Access) e.getNewValue();
            jumptoThreddsDatatype(access);
          }
          if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("CoordSys")
              || e.getPropertyName().equals("File")) {
            thredds.client.catalog.Dataset ds = (thredds.client.catalog.Dataset) e.getNewValue();
            setThreddsDatatype(ds, e.getPropertyName());
          }
        });

        c = threddsUI;
        break;

      case "Units":
        unitsPanel = new UnitsPanel((PreferencesExt) mainPrefs.node("units"));
        c = unitsPanel;
        break;

      case "URLdump":
        urlPanel = new URLDumpPane((PreferencesExt) mainPrefs.node("urlDump"));
        c = urlPanel;
        break;

      case "Viewer":
        c = viewerPanel;
        break;

      case "Writer":
        writerPanel = new DatasetWriterPanel((PreferencesExt) mainPrefs.node("writer"));
        c = writerPanel;
        break;

      case "WMS":
        wmsPanel = new WmsPanel((PreferencesExt) mainPrefs.node("wms"));
        c = wmsPanel;
        break;

      default:
        log.warn("tabbedPane unknown component {}", title);
        return;
    }

    parent.setComponentAt(idx, c);
    log.trace("tabbedPane changed {} added ", title);
  }


  private JMenuBar makeMenuBar() {
    JMenuBar mb = new JMenuBar();

    /// System menu
    JMenu sysMenu = new SystemMenu(this);
    mb.add(sysMenu);

    // Add modes Menu
    JMenu modeMenu = new ModesMenu(this);
    mb.add(modeMenu);

    // Add debug Menu
    JMenu debugMenu = new DebugMenu(this);
    mb.add(debugMenu);

    // Add help/about Menu
    JMenu helpMenu = new HelpMenu(this);
    mb.add(helpMenu);

    return mb;
  }


  public DatasetViewerPanel getDatasetViewerPanel() {
    return viewerPanel;
  }


  public void setDebugFlags() {
    log.debug("setDebugFlags");

    NetcdfFile.setDebugFlags(debugFlags);
    H5iosp.setDebugFlags(debugFlags);
    NcmlReader.setDebugFlags(debugFlags);
    // DODSNetcdfFile.setDebugFlags(debugFlags);
    // Nc4Iosp.setDebugFlags(debugFlags);
    DataFactory.setDebugFlags(debugFlags);

    NetcdfCopier.setDebugFlags(debugFlags);
    ucar.nc2.ft.point.standard.PointDatasetStandardFactory.setDebugFlags(debugFlags);
    ucar.nc2.grib.collection.Grib.setDebugFlags(debugFlags);
  }

  public void setUseRecordStructure(boolean use) {
    useRecordStructure = use;
  }

  public void setUseBuilders(boolean use) {
    useBuilders = use;
  }

  public boolean getUseBuilders() {
    return useBuilders;
  }

  public void setGribDiskCache() {
    if (diskCache2Form == null) {
      diskCache2Form = new DiskCache2Form(parentFrame, GribIndexCache.getDiskCache2());
    }
    diskCache2Form.setVisible(true);
  }


  private void save() {
    fileChooser.save();
    if (aggPanel != null) {
      aggPanel.save();
    }
    if (bufrFileChooser != null) {
      bufrFileChooser.save();
    }
    if (bufrPanel != null) {
      bufrPanel.save();
    }
    if (bufrTableBPanel != null) {
      bufrTableBPanel.save();
    }
    if (bufrTableDPanel != null) {
      bufrTableDPanel.save();
    }
    if (bufrReportPanel != null) {
      bufrReportPanel.save();
    }
    if (bufrCodePanel != null) {
      bufrCodePanel.save();
    }
    if (coordSysPanel != null) {
      coordSysPanel.save();
    }
    if (coveragePanel != null) {
      coveragePanel.save();
    }
    if (cdmIndexPanel != null) {
      cdmIndexPanel.save();
    }
    if (cdmIndexScanOp != null) {
      cdmIndexScanOp.save();
    }
    if (cdmIndexReportPanel != null) {
      cdmIndexReportPanel.save();
    }
    if (dirPartPanel != null) {
      dirPartPanel.save();
    }
    if (bufrCdmIndexPanel != null) {
      bufrCdmIndexPanel.save();
    }
    if (fcPanel != null) {
      fcPanel.save();
    }
    if (ftPanel != null) {
      ftPanel.save();
    }
    if (gribFilesPanel != null) {
      gribFilesPanel.save();
    }
    if (grib2CollectionPanel != null) {
      grib2CollectionPanel.save();
    }
    if (grib2DataPanel != null) {
      grib2DataPanel.save();
    }
    if (grib1DataPanel != null) {
      grib1DataPanel.save();
    }
    if (gribCodePanel != null) {
      gribCodePanel.save();
    }
    if (gribIdxPanel != null) {
      gribIdxPanel.save();
    }
    if (gribTemplatePanel != null) {
      gribTemplatePanel.save();
    }
    if (grib1CollectionPanel != null) {
      grib1CollectionPanel.save();
    }
    if (grib1ReportPanel != null) {
      grib1ReportPanel.save();
    }
    if (grib2ReportPanel != null) {
      grib2ReportPanel.save();
    }
    if (grib1TablePanel != null) {
      grib1TablePanel.save();
    }
    if (grib2TablePanel != null) {
      grib2TablePanel.save();
    }
    if (gribRewritePanel != null) {
      gribRewritePanel.save();
    }
    if (gridNewPanel != null) {
      gridNewPanel.save();
    }
    if (geoGridPanel != null) {
      geoGridPanel.save();
    }
    if (hdf5ObjectPanel != null) {
      hdf5ObjectPanel.save();
    }
    if (hdf5DataPanel != null) {
      hdf5DataPanel.save();
    }
    if (hdf4Panel != null) {
      hdf4Panel.save();
    }
    if (ncdumpPanel != null) {
      ncdumpPanel.save();
    }
    if (nc4viewer != null) {
      nc4viewer.save();
    }
    if (ncmlEditorPanel != null) {
      ncmlEditorPanel.save();
    }
    if (pointFeaturePanel != null) {
      pointFeaturePanel.save();
    }
    if (simpleGeomPanel != null) {
      simpleGeomPanel.save();
    }
    if (threddsUI != null) {
      threddsUI.storePersistentData();
    }
    if (unitsPanel != null) {
      unitsPanel.save();
    }
    if (urlPanel != null) {
      urlPanel.save();
    }
    if (viewerPanel != null) {
      viewerPanel.save();
    }
    if (writerPanel != null) {
      writerPanel.save();
    }
    if (wmoCommonCodePanel != null) {
      wmoCommonCodePanel.save();
    }
    if (wmsPanel != null) {
      wmsPanel.save();
    }
  }

  ///
  /// The following are hooks and shortcuts allowing OpPanel classes to interact with the UI.
  ///


  public static ToolsUI getToolsUI() {
    return ui;
  }

  public static JFrame getToolsFrame() {
    return ui.getFramePriv();
  }

  private JFrame getFramePriv() {
    return parentFrame;
  }

  public static FileManager getBufrFileChooser() {
    return ui.getBufrFileChooserPriv();
  }

  private FileManager getBufrFileChooserPriv() {
    if (bufrFileChooser == null) {
      bufrFileChooser = new FileManager(parentFrame, null, null, (PreferencesExt) mainPrefs.node("bufrFileManager"));
    }
    return bufrFileChooser;
  }


  public static void setNCdumpPanel(NetcdfFile ds) {
    ui.setNCdumpPanelPriv(ds);
  }

  private void setNCdumpPanelPriv(NetcdfFile ds) {
    if (ncdumpPanel == null) {
      log.debug("make ncdumpPanel");
      makeComponent(tabbedPane, "NCDump");
    }

    ncdumpPanel.setNetcdfFile(ds);

    tabbedPane.setSelectedComponent(ncdumpPanel);
  }

  public void openNetcdfFile(String datasetName) {
    makeComponent(tabbedPane, "Viewer");
    viewerPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(viewerPanel);
  }

  public void openNetcdfFile(NetcdfFile ncfile) {
    makeComponent(tabbedPane, "Viewer");
    viewerPanel.setDataset(ncfile);
    tabbedPane.setSelectedComponent(viewerPanel);
  }

  public void openCoordSystems(String datasetName) {
    makeComponent(tabbedPane, "CoordSys");
    coordSysPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(coordSysPanel);
  }

  public void openCoordSystems(NetcdfDataset dataset) {
    makeComponent(tabbedPane, "CoordSys");
    coordSysPanel.setDataset(dataset);
    tabbedPane.setSelectedComponent(coordSysPanel);
  }

  public void openNcML(String datasetName) {
    makeComponent(ncmlTabPane, "NcmlEditor");
    ncmlEditorPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ncmlTabPane);
    ncmlTabPane.setSelectedComponent(ncmlEditorPanel);
  }

  public void openPointFeatureDataset(String datasetName) {
    makeComponent(ftTabPane, "PointFeature");
    pointFeaturePanel.setPointFeatureDataset(FeatureType.ANY_POINT, datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(pointFeaturePanel);
  }

  public void openGrib1Collection(String collection) {
    makeComponent(grib1TabPane, "GRIB1collection"); // LOOK - does this aleays make component ?
    grib1CollectionPanel.setCollection(collection);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib1TabPane);
    grib1TabPane.setSelectedComponent(grib1CollectionPanel);
  }

  public void openGrib2Collection(String collection) {
    makeComponent(grib2TabPane, "GRIB2collection");
    grib2CollectionPanel.setCollection(collection);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib2TabPane);
    grib2TabPane.setSelectedComponent(grib2CollectionPanel);
  }

  public void openGrib2Data(String datasetName) {
    makeComponent(grib2TabPane, "GRIB2data");
    grib2DataPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib2TabPane);
    grib2TabPane.setSelectedComponent(grib2DataPanel);
  }

  public void openGrib1Data(String datasetName) {
    makeComponent(grib1TabPane, "GRIB1data");
    grib1DataPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(grib1TabPane);
    grib1TabPane.setSelectedComponent(grib1DataPanel);
  }

  public void openGridDataset(String datasetName) {
    makeComponent(ftTabPane, "Grids");
    geoGridPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(geoGridPanel);
  }

  public void openCoverageDataset(String datasetName) {
    makeComponent(ftTabPane, "Coverages");
    coveragePanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(coveragePanel);
  }

  public void openNewGrid(String datasetName) {
    makeComponent(ftTabPane, "GridNew");
    gridNewPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(gridNewPanel);
  }

  public void openGridDataset(NetcdfDataset dataset) {
    makeComponent(ftTabPane, "Grids");
    geoGridPanel.setDataset(dataset);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(geoGridPanel);
  }

  public void openGridDataset(GridDataset dataset) {
    makeComponent(ftTabPane, "Grids");
    geoGridPanel.setDataset(dataset);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(geoGridPanel);
  }

  private void openWMSDataset(String datasetName) {
    makeComponent(ftTabPane, "WMS");
    wmsPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(ftTabPane);
    ftTabPane.setSelectedComponent(wmsPanel);
  }

  public void openIndexFile(String datasetName) {
    makeComponent(gribTabPane, "CdmIndex4");
    cdmIndexPanel.doit(datasetName);
    tabbedPane.setSelectedComponent(iospTabPane);
    iospTabPane.setSelectedComponent(gribTabPane);
    gribTabPane.setSelectedComponent(cdmIndexPanel);
  }

  /** Jump to the appropriate tab based on datatype of catalog.Dataset */
  private void setThreddsDatatype(thredds.client.catalog.Dataset invDataset, String wants) {
    if (invDataset == null) {
      return;
    }

    boolean wantsViewer = wants.equals("File");
    boolean wantsCoordSys = wants.equals("CoordSys");

    try {
      // just open as a NetcdfDataset
      if (wantsViewer) {
        openNetcdfFile(threddsDataFactory.openDataset(invDataset, true, null, null));
        return;
      }

      if (wantsCoordSys) {
        NetcdfDataset ncd = threddsDataFactory.openDataset(invDataset, true, null, null);
        // make sure its enhanced
        ncd = NetcdfDatasets.enhance(ncd, NetcdfDataset.getDefaultEnhanceMode(), null);
        openCoordSystems(ncd);
      }

    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      ioe.printStackTrace();
    }

  }

  // jump to the appropriate tab based on datatype of InvAccess
  private void jumptoThreddsDatatype(thredds.client.catalog.Access invAccess) {
    if (invAccess == null) {
      return;
    }

    thredds.client.catalog.Service s = invAccess.getService();
    if (s.getType() == ServiceType.HTTPServer) {
      downloadFile(invAccess.getStandardUrlName());
      return;
    }

    if (s.getType() == ServiceType.WMS) {
      openWMSDataset(invAccess.getStandardUrlName());
      return;
    }

    if (s.getType() == ServiceType.CdmrFeature) {
      openCoverageDataset(invAccess.getWrappedUrlName());
      return;
    }

    thredds.client.catalog.Dataset ds = invAccess.getDataset();
    if (ds.getFeatureType() == null) {
      // if no feature type, just open as a NetcdfDataset
      try {
        openNetcdfFile(threddsDataFactory.openDataset(invAccess, true, null, null));
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
      }
      return;
    }

    DataFactory.Result threddsDataset;
    try {
      threddsDataset = threddsDataFactory.openThreddsDataset(ds, null);
      if (threddsDataset.fatalError) {
        JOptionPane.showMessageDialog(null, "Failed to open err=" + threddsDataset.errLog);
        return;
      }
      jumptoThreddsDatatype(threddsDataset);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error on setThreddsDatatype = " + ioe.getMessage());
    }

  }

  /**
   * Jump to the appropriate tab based on datatype of threddsData
   */
  private void jumptoThreddsDatatype(DataFactory.Result threddsData) {

    if (threddsData.fatalError) {
      JOptionPane.showMessageDialog(this, "Cant open dataset=" + threddsData.errLog);
      try {
        threddsData.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    if (threddsData.featureType.isCoverageFeatureType()) {
      makeComponent(ftTabPane, "Grids");
      gridNewPanel.setDataset(threddsData.featureDataset);
      tabbedPane.setSelectedComponent(ftTabPane);
      ftTabPane.setSelectedComponent(gridNewPanel);
    }
  }

  public NetcdfFile openFile(String location, boolean addCoords, CancelTask task) {

    NetcdfFile ncfile = null;
    Object iospMessage = useRecordStructure ? NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE : null;

    try {
      DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
      if (addCoords) {
        ncfile =
            NetcdfDatasets.acquireDataset(null, durl, NetcdfDataset.getDefaultEnhanceMode(), -1, task, iospMessage);
      } else {
        ncfile = NetcdfDatasets.acquireFile(durl, task);
      }

      if (ncfile == null) {
        JOptionPane.showMessageDialog(null, "NetcdfDatasets.open cannot open " + location);
      } else if (useRecordStructure) {
        ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
      }
    } catch (IOException ioe) {
      String message = ioe.getMessage();
      if ((null == message) && (ioe instanceof EOFException)) {
        message = "Premature End of File";
      }
      JOptionPane.showMessageDialog(null, "NetcdfDatasets.open cannot open " + location + "\n" + message);
      if (!(ioe instanceof FileNotFoundException)) {
        ioe.printStackTrace();
      }
      ncfile = null;
    } catch (Exception e) {
      StringWriter sw = new StringWriter(5000);
      if (e.getCause() != null) {
        e.getCause().printStackTrace(new PrintWriter(sw));
      } else {
        e.printStackTrace(new PrintWriter(sw));
      }
      JOptionPane.showMessageDialog(null, "NetcdfDatasets.open cannot open " + location + "\n" + sw.toString());
      log.error("NetcdfDatasets.open cannot open " + location, e);

      try {
        if (ncfile != null) {
          ncfile.close();
        }
      } catch (IOException ee) {
        System.out.printf("close failed%n");
      }
      ncfile = null;
    }

    return ncfile;
  }

  private String downloadStatus;

  private void downloadFile(String urlString) {
    int pos = urlString.lastIndexOf('/');
    String defFilename = (pos >= 0) ? urlString.substring(pos) : urlString;
    String fileOutName = fileChooser.chooseFilename(defFilename);
    if (fileOutName == null) {
      return;
    }
    String[] values = new String[2];
    values[0] = fileOutName;
    values[1] = urlString;

    // put in background thread with a ProgressMonitor window
    GetDataRunnable runner = new GetDataRunnable() {
      public void run(Object o) {
        String[] values = (String[]) o;
        BufferedOutputStream out;

        try (FileOutputStream fos = new FileOutputStream(values[0])) {
          out = new BufferedOutputStream(fos, 60000);
          ReadFromUrl.copyUrlB(values[1], out, 60000);
          downloadStatus = values[1] + " written to " + values[0];

        } catch (IOException ioe) {
          downloadStatus = "Error opening " + values[0] + " and reading " + values[1] + "%n" + ioe.getMessage();
        }
      }
    };

    GetDataTask task = new GetDataTask(runner, urlString, values);
    ProgressMonitor pm = new ProgressMonitor(task);
    pm.addActionListener(e -> {
      JOptionPane.showMessageDialog(null, e.getActionCommand() + "%n" + downloadStatus);
      downloadStatus = null;
    });
    pm.start(this, "Download", 30);
  }

  ///
  ///////////////////////////////////////////////////////////////////////////////////////
  ///


  public static void exit() {
    doSavePrefsAndUI();
    System.exit(0);
  }


  private static void doSavePrefsAndUI() {
    ui.save();
    Rectangle bounds = frame.getBounds();
    prefs.putBeanObject(FRAME_SIZE, bounds);
    try {
      store.save();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    done = true; // on some systems, still get a window close event
    ucar.nc2.internal.cache.FileCacheIF cache = NetcdfDatasets.getNetcdfFileCache();
    if (cache != null) {
      cache.clearCache(true);
    }
    FileCache.shutdown(); // shutdown threads
    DiskCache2.exit(); // shutdown threads
  }

  /**
   * Handle messages.
   */
  private static void setDataset() {
    // do it in the swing event thread
    SwingUtilities.invokeLater(() -> {
      int pos = wantDataset.indexOf('#');
      if (pos > 0) {
        String catName = wantDataset.substring(0, pos); // {catalog}#{dataset}
        if (catName.endsWith(".xml")) {
          ui.makeComponent(null, "THREDDS");
          ui.threddsUI.setDataset(wantDataset);
          ui.tabbedPane.setSelectedComponent(ui.threddsUI);
        }
        return;
      }

      // default
      ui.openNetcdfFile(wantDataset);
    });
  }

  /**
   * Set look-and-feel.
   */
  private static void prepareGui() {

    String osName = System.getProperty("os.name").toLowerCase();
    boolean isMacOs = osName.startsWith("mac os x");

    if (isMacOs) {
      System.setProperty("apple.laf.useScreenMenuBar", "true");

      // fixes the case on macOS where users use the system menu option to quit rather than
      // closing a window using the 'x' button.
      Runtime.getRuntime().addShutdownHook(new Thread(ToolsUI::doSavePrefsAndUI));
    } else {
      // Not macOS, so try applying Nimbus L&F, if available.
      try {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
      } catch (Exception exc) {
        log.warn("Unable to apply Nimbus look-and-feel due to {}", exc.toString());

        if (log.isTraceEnabled()) {
          exc.printStackTrace();
        }
      }
    }

    // Setting up a font metrics object triggers one of the most time-wasting steps of GUI set up.
    // We do it now before trying to create the splash or tools interface.
    SwingUtilities.invokeLater(() -> {
      Toolkit tk = Toolkit.getDefaultToolkit();
      Font f = new Font("SansSerif", Font.PLAIN, 12);

      @SuppressWarnings("deprecation")
      FontMetrics fm = tk.getFontMetrics(f);
    });
  }

  /**
   * Must call this method on the event thread.
   */
  private static void createToolsFrame() {
    // put UI in a JFrame
    frame = new JFrame("NetCDF (" + DIALOG_VERSION + ") Tools");

    ui = new ToolsUI(prefs, frame);

    frame.setIconImage(BAMutil.getImage("nj22/NetcdfUI"));

    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowActivated(WindowEvent e) {
        ToolsSplashScreen.getSharedInstance().setVisible(false);
      }

      @Override
      public void windowClosing(WindowEvent e) {
        if (!done) {
          exit();
        }
      }
    });

    frame.getContentPane().add(ui);

    Rectangle have = frame.getGraphicsConfiguration().getBounds();
    Rectangle def = new Rectangle(50, 50, 800, 800);

    Rectangle want = (Rectangle) prefs.getBean(FRAME_SIZE, def);

    if (want.getX() > have.getWidth() - 25) {
      // may be off screen when switcing between 2 monitor system
      want = def;
    }

    frame.setBounds(want);

    frame.pack();
    frame.setBounds(want);

    // in case a dataset was on the command line
    if (wantDataset != null) {
      setDataset();
    }
  }

  public static void main(String[] args) {
    /**
     * if (debugListen) {
     * System.out.println("Arguments:");
     * for (String arg : args) {
     * System.out.println(" " + arg);
     * }
     * HTTPSession.setInterceptors(true);
     * }
     */

    // handle multiple versions of ToolsUI, along with passing a dataset name
    // first, if there were command-line arguments
    if (args.length > 0) {
      // munge arguments into a single string
      StringBuilder sbuff = new StringBuilder();
      for (String arg : args) {
        sbuff.append(arg);
        sbuff.append(" ");
      }
      String arguments = sbuff.toString();
      System.out.println("ToolsUI arguments=" + arguments);

      // see if another version is running, if so send it the message then exit
      SocketMessage sm = new SocketMessage(14444, wantDataset);
      if (sm.isAlreadyRunning()) {
        log.error("ToolsUI already running - pass argument='{}' and exit", arguments);
        System.exit(0);
      }
    }

    // otherwise if no command-line arguments were passed
    else {
      SocketMessage sm = new SocketMessage(14444, null);
      if (sm.isAlreadyRunning()) {
        System.out.println("ToolsUI already running - start up another copy");
      } else {
        sm.addEventListener(event -> {
          wantDataset = event.getMessage();
          if (debugListen) {
            System.out.println(" got message= '" + wantDataset);
          }
          setDataset();
        });
      }
    }

    // look for command-line arguments
    boolean configRead = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-nj22Config") && (i < args.length - 1)) {
        String runtimeConfig = args[i + 1];
        i++;
        try (FileInputStream fis = new FileInputStream(runtimeConfig)) {
          Formatter errlog = new Formatter();
          RuntimeConfigParser.read(fis, errlog);
          configRead = true;
          System.out.println(errlog);
        } catch (IOException ioe) {
          log.warn("Error reading {} = {}", runtimeConfig, ioe.getMessage());
        }
      }
    }

    if (!configRead) {
      String filename = XMLStore.makeStandardFilename(".unidata", "nj22Config.xml");
      File f = new File(filename);
      if (f.exists()) {
        try (FileInputStream fis = new FileInputStream(filename)) {
          Formatter errlog = new Formatter();
          RuntimeConfigParser.read(fis, errlog);
          System.out.println(errlog);
        } catch (IOException ioe) {
          log.warn("Error reading {} = {}", filename, ioe.getMessage());
        }
      }
    }

    // prefs storage
    try {
      // 4.4
      String prefStore = XMLStore.makeStandardFilename(".unidata", "ToolsUI.xml");
      File prefs44 = new File(prefStore);

      if (!prefs44.exists()) {
        // if 4.4 doesnt exist, see if 4.3 exists
        String prefStoreBack = XMLStore.makeStandardFilename(".unidata", "NetcdfUI22.xml");
        File prefs43 = new File(prefStoreBack);
        if (prefs43.exists()) {
          // make a copy of it
          IO.copyFile(prefs43, prefs44);
        }
      }

      // open 4.4 version, create it if it doesn't exist
      store = XMLStore.createFromFile(prefStore, null);
      prefs = store.getPreferences();

      Debug.setStore(prefs.node("Debug"));
    } catch (IOException e) {
      log.warn("XMLStore creation failed - {}", e.toString());
    }

    // Prepare UI management.
    prepareGui();

    // Display the splash screen so there's something to look at while we do some more init.
    SwingUtilities.invokeLater(() -> ToolsSplashScreen.getSharedInstance().setVisible(true));

    // LOOK needed? for efficiency, persist aggregations. Every hour, delete stuff older than 30 days
    Aggregation.setPersistenceCache(new DiskCache2("/.unidata/aggCache", true, 60 * 24 * 30, 60));

    // open dap initializations
    // DODSNetcdfFile.setAllowCompression(true);
    // DODSNetcdfFile.setAllowSessions(true);

    // caching
    RandomAccessFile.enableDefaultGlobalFileCache();
    GribCdmIndex.initDefaultCollectionCache(100, 200, -1);

    // Waste a couple secs on the main thread before adding another task to the event dispatch thread.
    // Somehow this let's the EDT "catch up" and in particular gets the splash to finish rendering.
    try {
      Thread.sleep(2500L);
    } catch (Exception ignore) {
      // Nothing to do here.
    }

    // Create and show the UI frame!
    SwingUtilities.invokeLater(() -> {
      createToolsFrame();
      frame.setVisible(true);
    });

    // Name HTTP user agent and set Authentication for accessing password protected services
    SwingUtilities.invokeLater(() -> {
      UrlAuthenticatorDialog provider = new UrlAuthenticatorDialog(frame);
      // HttpService.INSTANCE.setCredentialsProvider(provider);
      HttpService.STANDARD.setUserAgent("ToolsUI v7.0");

      java.net.Authenticator.setDefault(provider);
    });
  }
}
