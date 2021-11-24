/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.ui.catalog;

import thredds.client.catalog.*;
import ucar.nc2.constants.DataFormatType;
import ucar.ui.widget.TextGetPutPane;
import ucar.ui.widget.*;
import ucar.unidata.io.ReadFromUrl;
import ucar.util.prefs.PreferencesExt;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.IOException;

/**
 * This is the THREDDS User Interface for ToolsUI.
 * Throws PropertyChangeEvent when a dataset is selected, see addPropertyChangeListener.
 *
 * @author caron
 */
public class ThreddsUI extends JPanel {
  private static final boolean debugSelection = false;

  // store keys
  private static final String VIEWER_SIZE = "ViewerSize";
  private static final String SOURCE_WINDOW_SIZE = "SourceWindowSize";
  private static final String XML_WINDOW_SIZE = "XmlWindowSize";

  // tabs

  private final PreferencesExt store;

  // the main components
  private ThreddsDatasetChooser datasetChooser;

  // UI components that need global scope
  private TextGetPutPane sourcePane;
  private JTabbedPane tabbedPane;

  private TextHistoryPane xmlPane;
  private IndependentDialog xmlWindow;
  private IndependentWindow sourceWindow;

  // the various managers and dialog boxes
  FileManager fileChooser; // shared with component viewers

  // debugging
  // private boolean debugBeans = false, debugChooser = false, debugPrint = false, debugHelp = false;

  public ThreddsUI(JFrame parent, PreferencesExt store) {
    this.store = store;
    // parent = topLevel.getRootPaneContainer().getRootPane();

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    Dimension d = (Dimension) store.getBean(VIEWER_SIZE, null);
    int defaultWidth = 700;
    int defaultHeight = 350;
    setPreferredSize((d != null) ? d : new Dimension(defaultWidth, defaultHeight));

    try {
      makeActionsSystem();
      makeActionsDataset();

      // makeMenu();
      makeUI();

    } catch (Exception e) {
      System.out.println("UI creation Exception");
      e.printStackTrace();
    }

    // other components
    PreferencesExt fcPrefs = (PreferencesExt) store.node("FileManager");
    FileFilter[] filters = {new FileManager.NetcdfExtFilter()};
    fileChooser = new FileManager(parent, null, filters, fcPrefs);
  }

  private void makeUI() {
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);

    /// catalog, DQC, query choosers
    datasetChooser = makeDatasetChooser(); // adds itself to the JTabbedPane

    // panel to show source
    sourcePane = new TextGetPutPane((PreferencesExt) store.node("getputPane"));
    sourceWindow = new IndependentWindow("Source", BAMutil.getImage("nj22/ThreddsIcon.png"), sourcePane);
    sourceWindow.setBounds((Rectangle) store.getBean(SOURCE_WINDOW_SIZE, new Rectangle(50, 50, 725, 450)));

    // panel to show xml data
    xmlPane = new TextHistoryPane(false);
    xmlWindow = new IndependentDialog(null, false, "XML data", xmlPane);
    xmlWindow.setBounds((Rectangle) store.getBean(XML_WINDOW_SIZE, new Rectangle(50, 50, 725, 450)));

    // catIndexer = new thredds.catalog.search.ui.CatalogIndexer((PreferencesExt) store.node("catIndexer"),
    // topLevel.getJFrame());
    // tabbedPane.addTab("Indexer", catIndexer);

    setLayout(new BorderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }

  private ThreddsDatasetChooser makeDatasetChooser() {

    datasetChooser = new ThreddsDatasetChooser((PreferencesExt) store.node("ThreddsDatasetChooser"), tabbedPane);

    // if (Debug.isSet("System/filterDataset"))
    // datasetChooser.setDatasetFilter(new DatasetFilter.ByServiceType(ServiceType.DODS));

    datasetChooser.addPropertyChangeListener(e -> {
      if (e.getPropertyName().equals("InvAccess")) {
        firePropertyChangeEvent(e);
        return;
      }

      if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("CoordSys")
          || e.getPropertyName().equals("File")) {
        // intercept XML, ASCII return types
        Dataset ds = (Dataset) e.getNewValue();
        Access access = ds.getAccess(ServiceType.HTTPServer);
        if (access != null) {
          DataFormatType format = access.getDataFormatType();
          if (format == DataFormatType.PLAIN || format == DataFormatType.XML) {
            String urlString = access.getWrappedUrlName();
            try {
              ReadFromUrl.readURLcontents(urlString);
              xmlPane.setText(ReadFromUrl.readURLcontents(urlString));
            } catch (IOException ioe) {
              xmlPane.setText(ioe.getMessage());
            }
            xmlPane.gotoTop();
            xmlWindow.setVisible(true);
            return;
          }
        }
        firePropertyChangeEvent(e);
      }
    });

    // add a show source button to catalog chooser
    JButton catSource = new JButton("Source");
    catSource.addActionListener(e -> {
      CatalogChooser cc = datasetChooser.getCatalogChooser();
      String catURL = cc.getCurrentURL();
      // InvCatalogImpl cat = (InvCatalogImpl) datasetChooser.getCatalogChooser().getCurrentCatalog();
      // String catURL = cat.getUriString();
      if (debugSelection)
        System.out.println("Catalog Source: url = " + catURL);
      sourcePane.setURL(catURL);
      sourcePane.gotoTop();
      sourceWindow.show();
    });
    datasetChooser.getCatalogChooser().addButton(catSource);

    return datasetChooser;
  }

  /**
   * save all data in the PersistentStore
   */
  public void storePersistentData() {
    store.putBeanObject(VIEWER_SIZE, getSize());
    store.putBeanObject(SOURCE_WINDOW_SIZE, sourceWindow.getBounds());

    if (fileChooser != null)
      fileChooser.save();
    if (datasetChooser != null)
      datasetChooser.save();
    if (sourcePane != null)
      sourcePane.save();
    /*
     * if (catEditor != null) catEditor.save();
     * if (catCrawler != null) catCrawler.save();
     * if (serverConfigure != null) serverConfigure.save();
     * if (catCopier != null) catCopier.save();
     */
  }

  /**
   * Fires a PropertyChangeEvent:
   * <ul>
   * <li>propertyName = "Dataset" or "File", getNewValue() = InvDataset chosen.
   * <li>propertyName = "Datasets", getNewValue() = InvDataset[] chosen. This can only happen if
   * you have set doResolve = true, and the resolved dataset is a list of datasets.
   * </ul>
   */
  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    firePropertyChange(event.getPropertyName(), event.getOldValue(), event.getNewValue());
  }

  public void setDataset(String location) {
    datasetChooser.getCatalogChooser().setCatalog(location);
    tabbedPane.setSelectedComponent(datasetChooser.getCatalogChooser());
  }

  // actions that are system-wide
  private void makeActionsSystem() {
    AbstractAction clearDebugFlagsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) { /* Debug.clear(); */ }
    };
    BAMutil.setActionProperties(clearDebugFlagsAction, null, "Clear Debug Flags", false, 'D', -1);
  }

  // actions that control the dataset
  private void makeActionsDataset() {}
}


