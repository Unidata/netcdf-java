/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.grid2;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.ui.geoloc.NavigatedPanel;
import ucar.nc2.ui.geoloc.ProjectionManager;
import ucar.nc2.ui.gis.MapBean;
import ucar.nc2.ui.grid.ColorScale;
import ucar.nc2.ui.util.NamedObjects;
import ucar.ui.util.NamedObject;
import ucar.ui.event.ActionCoordinator;
import ucar.ui.event.ActionSourceListener;
import ucar.ui.event.ActionValueEvent;
import ucar.ui.prefs.Debug;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.*;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Display nc2.grid objects.
 * more or less the controller in MVC
 */
public class GridViewer extends JPanel {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridViewer.class);

  // constants
  private static final int DELAY_DRAW_AFTER_DATA_EVENT = 250; // quarter sec
  private static final String LastMapAreaName = "LastMapArea";
  private static final String LastProjectionName = "LastProjection";
  private static final String ColorScaleName = "ColorScale";

  private final PreferencesExt store;

  // UI components
  private ColorScale colorScale;
  private ColorScale.Panel colorScalePanel;
  private List<Chooser> choosers;
  private SuperComboBox fieldChooser, levelChooser, timeChooser, ensembleChooser, runtimeChooser;
  private JLabel dataValueLabel;

  private NavigatedPanel navPanel;

  // UI components that need global scope
  private TextHistoryPane datasetInfoTA;
  private JPanel drawingPanel;
  private JComboBox csDataMinMax;
  private PopupMenu mapBeanMenu;
  private JSpinner strideSpinner;

  private final JLabel datasetNameLabel = new JLabel();

  // the various managers and dialog boxes
  private ProjectionManager projManager;
  private IndependentWindow infoWindow;

  // toolbars
  private JPanel fieldPanel, toolPanel;
  private JToolBar navToolbar, moveToolbar;
  private AbstractAction navToolbarAction, moveToolbarAction;

  // actions
  private AbstractAction redrawAction;
  private AbstractAction showDatasetInfoAction;
  private AbstractAction minmaxHorizAction, minmaxLogAction, minmaxHoldAction;
  private AbstractAction fieldLoopAction, levelLoopAction, timeLoopAction, runtimeLoopAction;
  private AbstractAction chooseProjectionAction, saveCurrentProjectionAction;

  private AbstractAction dataProjectionAction, drawBBAction, showGridAction, showContoursAction,
      showContourLabelsAction;
  private AbstractAction drawHorizAction, drawVertAction;

  // data components
  private DataState dataState;
  private GridDataset gridDataset;
  private Grid currentField;
  private Projection project;

  // state
  private List<NamedObject> levelNames, timeNames, ensembleNames, runtimeNames;
  private int currentLevelIdx;
  private int currentTimeIdx;
  private int currentEnsembleIdx;
  private int currentRuntimeIdx;
  private boolean drawHorizOn = true;
  private boolean drawVertOn;
  private boolean eventsOK = true;
  private final Color mapColor = Color.black;
  private int mapBeanCount;

  // rendering
  private final AffineTransform atI = new AffineTransform(); // identity transform
  private ucar.nc2.ui.util.Renderer mapRenderer;
  private GridRenderer gridRenderer;
  private javax.swing.Timer redrawTimer;

  public GridViewer(PreferencesExt pstore, RootPaneContainer root, FileManager fileChooser, int defaultHeight) {
    this.store = pstore;

    try {
      // choosers
      choosers = new ArrayList<>();
      fieldChooser = new SuperComboBox(root, "field", true, null);
      choosers.add(new Chooser("field", fieldChooser, true));
      levelChooser = new SuperComboBox(root, "level", false, null);
      choosers.add(new Chooser("level", levelChooser, false));
      timeChooser = new SuperComboBox(root, "time", false, null);
      choosers.add(new Chooser("time", timeChooser, false));
      ensembleChooser = new SuperComboBox(root, "ensemble", false, null);
      choosers.add(new Chooser("ensemble", ensembleChooser, false));
      runtimeChooser = new SuperComboBox(root, "runtime", false, null);
      choosers.add(new Chooser("runtime", runtimeChooser, false));

      // colorscale
      Object bean = store.getBean(ColorScaleName, null);
      if (!(bean instanceof ColorScale))
        colorScale = new ColorScale("default");
      else
        colorScale = (ColorScale) store.getBean(ColorScaleName, null);

      colorScalePanel = new ColorScale.Panel(this, colorScale);
      csDataMinMax = new JComboBox(ColorScale.MinMaxType.values());
      csDataMinMax.setToolTipText("ColorScale Min/Max setting");
      csDataMinMax.addActionListener(e -> {
        gridRenderer.setDataMinMaxType((ColorScale.MinMaxType) csDataMinMax.getSelectedItem());
        redrawLater();
      });

      // renderer
      // set up the renderers; Maps are added by addMapBean()
      gridRenderer = new GridRenderer(store);
      gridRenderer.setColorScale(colorScale);

      strideSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
      strideSpinner.addChangeListener(e -> {
        Integer val = (Integer) strideSpinner.getValue();
        gridRenderer.setHorizStride(val);
      });

      makeActionsDataset();
      makeActionsToolbars();
      makeActions();
      makeEventManagement();

      //// toolPanel
      toolPanel = new JPanel();
      toolPanel.setBorder(new EtchedBorder());
      toolPanel.setLayout(new MFlowLayout(FlowLayout.LEFT, 0, 0));

      // menus
      JMenu dataMenu = new JMenu("Dataset");
      dataMenu.setMnemonic('D');
      JMenu configMenu = new JMenu("Configure");
      configMenu.setMnemonic('C');
      JMenu toolMenu = new JMenu("Controls");
      toolMenu.setMnemonic('T');
      JMenuBar menuBar = new JMenuBar();
      menuBar.add(dataMenu);
      menuBar.add(configMenu);
      menuBar.add(toolMenu);
      toolPanel.add(menuBar);

      // field chooser panel - delay adding the choosers
      fieldPanel = new JPanel();
      fieldPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
      toolPanel.add(fieldPanel);

      // stride
      toolPanel.add(strideSpinner);

      // buttcons
      BAMutil.addActionToContainer(toolPanel, drawHorizAction);
      BAMutil.addActionToContainer(toolPanel, drawVertAction);
      mapBeanMenu = MapBean.makeMapSelectButton();
      toolPanel.add(mapBeanMenu.getParentComponent());

      // the Navigated panel and its toolbars
      navPanel = new NavigatedPanel();
      navPanel.setLayout(new FlowLayout());
      ProjectionRect ma = (ProjectionRect) store.getBean(LastMapAreaName, null);
      if (ma != null)
        navPanel.setMapArea(ma);

      navToolbar = navPanel.getNavToolBar();
      moveToolbar = navPanel.getMoveToolBar();
      if ((Boolean) navToolbarAction.getValue(BAMutil.STATE))
        toolPanel.add(navToolbar);
      if ((Boolean) moveToolbarAction.getValue(BAMutil.STATE))
        toolPanel.add(moveToolbar);
      makeNavPanelWiring();
      addActionsToMenus(dataMenu, configMenu, toolMenu);

      BAMutil.addActionToContainer(toolPanel, navPanel.setReferenceAction);
      BAMutil.addActionToContainer(toolPanel, dataProjectionAction);
      BAMutil.addActionToContainer(toolPanel, showGridAction);
      BAMutil.addActionToContainer(toolPanel, drawBBAction);
      // BAMutil.addActionToContainer(toolPanel, showContourLabelsAction);
      BAMutil.addActionToContainer(toolPanel, redrawAction);

      // vertical split
      // vertPanel = new VertPanel();
      // splitDraw = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panz, vertPanel);
      // int divLoc = store.getInt( "vertSplit", 2*defaultHeight/3);
      // splitDraw.setDividerLocation(divLoc);
      drawingPanel = new JPanel(new BorderLayout()); // filled later

      // status panel
      JPanel statusPanel = new JPanel(new BorderLayout());
      statusPanel.setBorder(new EtchedBorder());
      JLabel positionLabel = new JLabel("position");
      positionLabel.setToolTipText("position at cursor");
      dataValueLabel = new JLabel("data value", SwingConstants.CENTER);
      dataValueLabel.setToolTipText("data value (double click on grid)");
      statusPanel.add(positionLabel, BorderLayout.WEST);
      statusPanel.add(dataValueLabel, BorderLayout.CENTER);
      navPanel.setPositionLabel(positionLabel);

      // assemble
      JPanel westPanel = new JPanel(new BorderLayout());
      westPanel.add(colorScalePanel, BorderLayout.CENTER);
      westPanel.add(csDataMinMax, BorderLayout.NORTH);

      JPanel northPanel = new JPanel();
      // northPanel.setLayout( new BoxLayout(northPanel, BoxLayout.Y_AXIS));
      northPanel.setLayout(new BorderLayout());
      northPanel.add(datasetNameLabel, BorderLayout.NORTH);
      northPanel.add(toolPanel, BorderLayout.SOUTH);

      setLayout(new BorderLayout());
      add(northPanel, BorderLayout.NORTH);
      add(statusPanel, BorderLayout.SOUTH);
      add(westPanel, BorderLayout.WEST);
      add(drawingPanel, BorderLayout.CENTER);

      setDrawHorizAndVert(drawHorizOn, drawVertOn);

      // get last saved Projection
      project = (Projection) store.getBean(LastProjectionName, null);
      if (project != null)
        setProjection(project);

      // redraw timer
      redrawTimer = new javax.swing.Timer(0, e -> {
        // invoke in event thread
        SwingUtilities.invokeLater(() -> draw(false));
        redrawTimer.stop(); // one-shot timer
      });
      redrawTimer.setInitialDelay(DELAY_DRAW_AFTER_DATA_EVENT);
      redrawTimer.setRepeats(false);


    } catch (Exception e) {
      System.out.println("UI creation failed");
      e.printStackTrace();
    }
  }


  // actions that control the dataset
  private void makeActionsDataset() {

    /*
     * choose local dataset
     * AbstractAction chooseLocalDatasetAction = new AbstractAction() {
     * public void actionPerformed(ActionEvent e) {
     * String filename = fileChooser.chooseFilename();
     * if (filename == null) return;
     *
     * Dataset invDs;
     * try { // DatasetNode parent, String name, Map<String, Object> flds, List< AccessBuilder > accessBuilders, List<
     * DatasetBuilder > datasetBuilders
     * Map<String, Object> flds = new HashMap<>();
     * flds.put(Dataset.FeatureType, FeatureType.GRID.toString());
     * flds.put(Dataset.ServiceName, ServiceType.File.toString()); // bogus
     * invDs = new Dataset(null, filename, flds, null, null);
     * setDataset(invDs);
     *
     * } catch (Exception ue) {
     * JOptionPane.showMessageDialog(CoverageDisplay.this, "Invalid filename = <" + filename + ">\n" + ue.getMessage());
     * ue.printStackTrace();
     * }
     * }
     * };
     * BAMutil.setActionProperties(chooseLocalDatasetAction, "FileChooser", "open Local dataset...", false, 'L', -1);
     *
     * /* saveDatasetAction = new AbstractAction() {
     * public void actionPerformed(ActionEvent e) {
     * String fname = controller.getDatasetName();
     * if (fname != null) {
     * savedDatasetList.add( fname);
     * BAMutil.addActionToMenu( savedDatasetMenu, new DatasetAction( fname), 0);
     * }
     * }
     * };
     * BAMutil.setActionProperties( saveDatasetAction, null, "save dataset", false, 'S', 0);
     */

    // Configure
    chooseProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getProjectionManager().setVisible();
      }
    };
    BAMutil.setActionProperties(chooseProjectionAction, null, "Projection Manager...", false, 'P', 0);

    saveCurrentProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getProjectionManager();
        // set the bounding box
        // Projection proj = navPanel.getProjectionImpl().constructCopy();
        // proj.setDefaultMapArea(navPanel.getMapArea());
        // if (debug) System.out.println(" GV save projection "+ proj);

        // projManage.setMap(renderAll.get("Map")); LOOK!
        // projManager.saveProjection( proj);
      }
    };
    BAMutil.setActionProperties(saveCurrentProjectionAction, null, "save Current Projection", false, 'S', 0);

    /*
     * chooseColorScaleAction = new AbstractAction() {
     * public void actionPerformed(ActionEvent e) {
     * if (null == csManager) // lazy instantiation
     * makeColorScaleManager();
     * csManager.show();
     * }
     * };
     * BAMutil.setActionProperties( chooseColorScaleAction, null, "ColorScale Manager...", false, 'C', 0);
     *
     */
    // redraw
    redrawAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        repaint();
        start(true);
        draw(true);
      }
    };
    BAMutil.setActionProperties(redrawAction, "alien", "RedRaw", false, 'W', 0);

    showDatasetInfoAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (infoWindow == null) {
          datasetInfoTA = new TextHistoryPane();
          infoWindow = new IndependentWindow("Dataset Information", BAMutil.getImage("nj22/GDVs"), datasetInfoTA);
          infoWindow.setSize(700, 700);
          infoWindow.setLocation(100, 100);
        }

        datasetInfoTA.clear();
        if (gridDataset != null) {
          Formatter f = new Formatter();
          gridDataset.toString(f);
          datasetInfoTA.appendLine(f.toString());
        } else {
          datasetInfoTA.appendLine("No coverageDataset loaded");
        }
        datasetInfoTA.gotoTop();
        infoWindow.show();
      }
    };
    BAMutil.setActionProperties(showDatasetInfoAction, "Information", "Show info...", false, 'S', -1);

    minmaxHorizAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedItem(ColorScale.MinMaxType.horiz);
        setDataMinMaxType(ColorScale.MinMaxType.horiz);
      }
    };
    BAMutil.setActionProperties(minmaxHorizAction, null, "Horizontal plane", false, 'H', 0);

    minmaxLogAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedItem(ColorScale.MinMaxType.log);
        setDataMinMaxType(ColorScale.MinMaxType.log);
      }
    };
    BAMutil.setActionProperties(minmaxLogAction, null, "log horiz plane", false, 'V', 0);

    minmaxHoldAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        csDataMinMax.setSelectedItem(ColorScale.MinMaxType.hold);
        setDataMinMaxType(ColorScale.MinMaxType.hold);
      }
    };
    BAMutil.setActionProperties(minmaxHoldAction, null, "Hold scale constant", false, 'C', 0);

    fieldLoopAction = new LoopControlAction(fieldChooser);
    levelLoopAction = new LoopControlAction(levelChooser);
    timeLoopAction = new LoopControlAction(timeChooser);
    runtimeLoopAction = new LoopControlAction(runtimeChooser);
  }

  private void makeActionsToolbars() {

    navToolbarAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state)
          toolPanel.add(navToolbar);
        else
          toolPanel.remove(navToolbar);
      }
    };
    BAMutil.setActionProperties(navToolbarAction, "MagnifyPlus", "show Navigate toolbar", true, 'M', 0);
    navToolbarAction.putValue(BAMutil.STATE, store.getBoolean("navToolbarAction", true));

    moveToolbarAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state)
          toolPanel.add(moveToolbar);
        else
          toolPanel.remove(moveToolbar);
      }
    };
    BAMutil.setActionProperties(moveToolbarAction, "Up", "show Move toolbar", true, 'M', 0);
    moveToolbarAction.putValue(BAMutil.STATE, store.getBoolean("moveToolbarAction", true));
  }

  // create all actions here
  // the actions can then be attached to buttcons, menus, etc
  private void makeActions() {
    boolean state;

    dataProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        if (state) {
          Projection dataProjection = gridRenderer.getDataProjection();
          if (null != dataProjection)
            setProjection(dataProjection);
        } else {
          setProjection(new LatLonProjection());
        }
      }
    };
    BAMutil.setActionProperties(dataProjectionAction, "nj22/DataProjection", "use Data Projection", true, 'D', 0);
    dataProjectionAction.putValue(BAMutil.STATE, true);

    // contouring
    drawBBAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        gridRenderer.setDrawBB(state);
        draw(false);
      }
    };
    BAMutil.setActionProperties(drawBBAction, "nj22/Contours", "draw bounding box", true, 'B', 0);
    drawBBAction.putValue(BAMutil.STATE, false);

    // draw horiz
    drawHorizAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        drawHorizOn = (Boolean) getValue(BAMutil.STATE);
        setDrawHorizAndVert(drawHorizOn, drawVertOn);
        draw(false);
      }
    };
    BAMutil.setActionProperties(drawHorizAction, "nj22/DrawHoriz", "draw horizontal", true, 'H', 0);
    state = store.getBoolean("drawHorizAction", true);
    drawHorizAction.putValue(BAMutil.STATE, state);
    drawHorizOn = state;

    // draw Vert
    drawVertAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        drawVertOn = (Boolean) getValue(BAMutil.STATE);
        setDrawHorizAndVert(drawHorizOn, drawVertOn);
        draw(false);
      }
    };
    BAMutil.setActionProperties(drawVertAction, "nj22/DrawVert", "draw vertical", true, 'V', 0);
    state = store.getBoolean("drawVertAction", false);
    drawVertAction.putValue(BAMutil.STATE, state);
    drawVertOn = state;

    // show grid
    showGridAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        gridRenderer.setDrawGridLines(state);
        draw(false);
      }
    };
    BAMutil.setActionProperties(showGridAction, "nj22/Grid", "show grid lines", true, 'G', 0);
    state = store.getBoolean("showGridAction", false);
    showGridAction.putValue(BAMutil.STATE, state);
    gridRenderer.setDrawGridLines(state);

    // contouring
    showContoursAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        gridRenderer.setDrawContours(state);
        draw(false);
      }
    };
    BAMutil.setActionProperties(showContoursAction, "nj22/Contours", "show contours", true, 'C', 0);
    state = store.getBoolean("showContoursAction", false);
    showContoursAction.putValue(BAMutil.STATE, state);
    gridRenderer.setDrawContours(state);

    // contouring labels
    showContourLabelsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        gridRenderer.setDrawContourLabels(state);
        draw(false);
      }
    };
    BAMutil.setActionProperties(showContourLabelsAction, "nj22/ContourLabels", "show contour labels", true, 'L', 0);
    state = store.getBoolean("showContourLabelsAction", false);
    showContourLabelsAction.putValue(BAMutil.STATE, state);
    gridRenderer.setDrawContourLabels(state);
  }

  private void makeEventManagement() {
    //// manage field selection events
    String actionName = "field";
    ActionCoordinator fieldCoordinator = new ActionCoordinator(actionName);
    fieldCoordinator.addActionSourceListener(fieldChooser.getActionSourceListener());
    // heres what to do when the currentField changes
    ActionSourceListener fieldSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        if (setField(e.getValue())) {
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
            // colorScalePanel.paintImmediately(colorScalePanel.getBounds()); // kludgerino
          } else
            redrawLater();
        }
      }
    };
    fieldCoordinator.addActionSourceListener(fieldSource);

    //// manage level selection events
    actionName = "level";
    ActionCoordinator levelCoordinator = new ActionCoordinator(actionName);
    levelCoordinator.addActionSourceListener(levelChooser.getActionSourceListener());
    // connect to the vertPanel
    /*
     * levelCoordinator.addActionSourceListener(vertPanel.getActionSourceListener());
     * // also manage Pick events from the vertPanel
     * vertPanel.getDrawArea().addPickEventListener( new PickEventListener() {
     * public void actionPerformed(PickEvent e) {
     * int level = renderGrid.findLevelCoordElement(e.getLocation().getY());
     * if ((level != -1) && (level != currentLevel)) {
     * currentLevel = level;
     * redrawLater();
     * String selectedName = levelNames.get(currentLevel).getName();
     * if (Debug.isSet("pick/event"))
     * System.out.println("pick.event Vert: "+selectedName);
     * levelSource.fireActionValueEvent(ActionSourceListener.SELECTED, selectedName);
     * }
     * }
     * });
     */
    // heres what to do when a level changes
    ActionSourceListener levelSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int level = findIndexFromName(levelNames, e.getValue().toString());
        if ((level != -1) && (level != currentLevelIdx)) {
          currentLevelIdx = level;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
          } else
            redrawLater();
        }
      }
    };
    levelCoordinator.addActionSourceListener(levelSource);

    //// manage time selection events
    actionName = "time";
    ActionCoordinator timeCoordinator = new ActionCoordinator(actionName);
    timeCoordinator.addActionSourceListener(timeChooser.getActionSourceListener());
    // heres what to do when the time changes
    ActionSourceListener timeSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int time = findIndexFromName(timeNames, e.getValue().toString());
        if ((time != -1) && (time != currentTimeIdx)) {
          currentTimeIdx = time;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
            // colorScalePanel.paintImmediately(colorScalePanel.getBounds()); // kludgerino
          } else
            redrawLater();
        }
      }
    };
    timeCoordinator.addActionSourceListener(timeSource);

    /// manage runtime selection events
    actionName = "runtime";
    ActionCoordinator runtimeCoordinator = new ActionCoordinator(actionName);
    runtimeCoordinator.addActionSourceListener(runtimeChooser.getActionSourceListener());
    // heres what to do when the runtime changes
    ActionSourceListener runtimeSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        // Object dataValue = e.getValue();
        int runtime = findIndexFromName(runtimeNames, e.getValue().toString());
        if ((runtime != -1) && (runtime != currentRuntimeIdx)) {
          currentRuntimeIdx = runtime;

          if (dataState.toaxisReg != null) {
            try {
              GridAxis1DTime taxis = dataState.toaxisReg.getTimeAxisForRun(currentRuntimeIdx);
              timeNames = NamedObjects.getNames(taxis);
              if ((currentTimeIdx < 0) || (currentTimeIdx >= timeNames.size()))
                currentTimeIdx = 0;
              timeChooser.setSelectedByIndex(currentRuntimeIdx);
            } catch (InvalidRangeException exc) {
              exc.printStackTrace();
              throw new RuntimeException(exc);
            }
          }

          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
          } else
            redrawLater();
        }
      }
    };
    runtimeCoordinator.addActionSourceListener(runtimeSource);

    //// manage runtime selection events
    actionName = "ensemble";
    ActionCoordinator ensembleCoordinator = new ActionCoordinator(actionName);
    ensembleCoordinator.addActionSourceListener(ensembleChooser.getActionSourceListener());
    // heres what to do when the time changes
    ActionSourceListener ensembleSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int ensIndex = findIndexFromName(ensembleNames, e.getValue().toString());
        if ((ensIndex != -1) && (ensIndex != currentEnsembleIdx)) {
          currentEnsembleIdx = ensIndex;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
          } else
            redrawLater();
        }
      }
    };
    ensembleCoordinator.addActionSourceListener(ensembleSource);
  }

  private void makeNavPanelWiring() {

    // get Projection Events from the navigated panel
    navPanel.addNewProjectionListener(e -> {
      if (Debug.isSet("event/NewProjection"))
        System.out.println("Controller got NewProjectionEvent " + navPanel.getMapArea());
      if (eventsOK && mapRenderer != null) {
        mapRenderer.setProjection(e.getProjection());
        gridRenderer.setViewProjection(e.getProjection());
        drawH(false);
      }
    });

    // get NewMapAreaEvents from the navigated panel
    navPanel.addNewMapAreaListener(e -> {
      if (Debug.isSet("event/NewMapArea"))
        System.out.println("Controller got NewMapAreaEvent " + navPanel.getMapArea());
      drawH(false);
    });

    // get Move events from the navigated panel
    navPanel.addCursorMoveEventListener(e -> {
      String valueS = gridRenderer.getXYvalueStr(e.getLocation());
      dataValueLabel.setText(valueS);
    });

  }

  private int findIndexFromName(List<NamedObject> list, String name) {
    for (int idx = 0; idx < list.size(); idx++) {
      NamedObject no = list.get(idx);
      if (name.equals(no.getName()))
        return idx;
    }
    log.error("findIndexFromName cant find " + name);
    return -1;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public void save() {
    // store.putInt( "vertSplit", splitDraw.getDividerLocation());

    store.putBoolean("navToolbarAction", (Boolean) navToolbarAction.getValue(BAMutil.STATE));
    store.putBoolean("moveToolbarAction", (Boolean) moveToolbarAction.getValue(BAMutil.STATE));

    if (projManager != null)
      projManager.storePersistentData();

    store.putBeanObject(LastMapAreaName, navPanel.getMapArea());
    store.putBeanObject(LastProjectionName, navPanel.getProjectionImpl());
    // if (gridDataset != null)
    // store.put(LastDatasetName, gridDataset.getTitle());
    store.putBeanObject(ColorScaleName, colorScale);

    store.putBoolean("showGridAction", (Boolean) showGridAction.getValue(BAMutil.STATE));
    store.putBoolean("showContoursAction", (Boolean) showContoursAction.getValue(BAMutil.STATE));
    store.putBoolean("showContourLabelsAction", (Boolean) showContourLabelsAction.getValue(BAMutil.STATE));

  }

  // add a MapBean to the User Interface
  public void addMapBean(MapBean mb) {
    mapBeanMenu.addAction(mb.getActionDesc(), mb.getIcon(), mb.getAction());

    // first one is the "default"
    if (mapBeanCount == 0) {
      setMapRenderer(mb.getRenderer());
    }
    mapBeanCount++;

    mb.addPropertyChangeListener(e -> {
      if (e.getPropertyName().equals("Renderer")) {
        setMapRenderer((ucar.nc2.ui.util.Renderer) e.getNewValue());
      }
    });
  }

  void setMapRenderer(ucar.nc2.ui.util.Renderer mapRenderer) {
    this.mapRenderer = mapRenderer;
    mapRenderer.setProjection(navPanel.getProjectionImpl());
    mapRenderer.setColor(mapColor);
    redrawLater();
  }

  // assume that its done in the event thread
  boolean showDataset() {
    // temp kludge for initialization

    currentField = gridDataset.getGrids().iterator().next(); // first
    currentLevelIdx = 0;
    currentTimeIdx = 0;
    currentEnsembleIdx = 0;
    currentRuntimeIdx = 0;

    eventsOK = false; // dont let this trigger redraw
    this.dataState = gridRenderer.setGrid(gridDataset, currentField);
    gridRenderer.setDataProjection(currentField.getCoordinateSystem().getProjection());
    setField(currentField);

    // LOOK if possible, change the projection and the map area to one that fits this dataset
    Projection dataProjection = currentField.getCoordinateSystem().getProjection();
    if (dataProjection != null) {
      setProjection(dataProjection);
    }
    ProjectionRect fieldBB = currentField.getCoordinateSystem().getBoundingBox();
    if (fieldBB != null) {
      navPanel.setMapArea(fieldBB);
    }

    // events now ok
    eventsOK = true;
    return true;
  }

  public void setDataMinMaxType(ColorScale.MinMaxType type) {
    gridRenderer.setDataMinMaxType(type);
    redrawLater();
  }

  private boolean startOK = true;

  public void setGridCollection(GridDataset gcd) {
    this.gridDataset = gcd;
    setFields(NamedObjects.getNames(gcd.getGrids()));

    startOK = false; // wait till redraw is hit before drawing
    try {
      showDataset();
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
    datasetNameLabel.setText("Dataset:  " + gridDataset.getName());
  }

  void setFieldsFromBeans(List<GridNewTable.GridBean> fields) {
    fieldChooser.setCollection(fields.iterator());
  }

  void setFields(Iterable<NamedObject> fields) {
    fieldChooser.setCollection(fields.iterator());
  }

  private boolean setField(Object fld) {
    Grid gg = null;
    if (fld instanceof Grid)
      gg = (Grid) fld;
    else if (fld instanceof String)
      gg = gridDataset.findGrid((String) fld).orElse(null);
    else if (fld instanceof NamedObject)
      gg = gridDataset.findGrid(((NamedObject) fld).getName()).orElse(null);;
    if (null == gg)
      return false;

    this.dataState = gridRenderer.setGrid(gridDataset, gg);
    gridRenderer.setDataProjection(this.dataState.geocs.getProjection());
    currentField = gg;

    // set runtimes
    if (this.dataState.rtaxis != null) {
      runtimeNames = NamedObjects.getNames(this.dataState.rtaxis);
      currentRuntimeIdx = !runtimeNames.isEmpty() ? 0 : -1;
      if ((currentRuntimeIdx < 0) || (currentRuntimeIdx >= runtimeNames.size()))
        currentRuntimeIdx = 0;

      setChooserWanted("runtime", true);
      runtimeChooser.setCollection(runtimeNames.iterator(), true);
      NamedObject no = runtimeNames.get(currentRuntimeIdx);
      runtimeChooser.setSelectedByName(no.getName());

    } else {
      runtimeNames = new ArrayList<>();
      setChooserWanted("runtime", false);
      gridRenderer.setRunTime(-1);
    }

    // set times
    if (this.dataState.toaxisReg != null) {
      GridAxis1D toaxis1D;
      try {
        toaxis1D = this.dataState.toaxisReg.getTimeAxisForRun(currentRuntimeIdx);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
      timeNames = NamedObjects.getNames(toaxis1D);
      if ((currentTimeIdx < 0) || (currentTimeIdx >= timeNames.size()))
        currentTimeIdx = 0;

      setChooserWanted("time", true);
      timeChooser.setCollection(timeNames.iterator(), true);
      NamedObject no = timeNames.get(currentTimeIdx);
      timeChooser.setSelectedByName(no.getName());

    } else if (this.dataState.taxis != null) {
      timeNames = NamedObjects.getNames(this.dataState.taxis);
      if ((currentTimeIdx < 0) || (currentTimeIdx >= timeNames.size()))
        currentTimeIdx = 0;

      setChooserWanted("time", true);
      timeChooser.setCollection(timeNames.iterator(), true);
      NamedObject no = timeNames.get(currentTimeIdx);
      timeChooser.setSelectedByName(no.getName());
    }

    // set ensembles
    if (this.dataState.ensaxis != null) {
      ensembleNames = NamedObjects.getNames(this.dataState.ensaxis);
      currentEnsembleIdx = !ensembleNames.isEmpty() ? 0 : -1;
      if ((currentEnsembleIdx < 0) || (currentEnsembleIdx >= ensembleNames.size()))
        currentEnsembleIdx = 0;

      setChooserWanted("ensemble", true);
      ensembleChooser.setCollection(ensembleNames.iterator(), true);
      NamedObject no = ensembleNames.get(currentEnsembleIdx);
      ensembleChooser.setSelectedByName(no.getName());

    } else {
      ensembleNames = new ArrayList<>();
      setChooserWanted("ensemble", false);
      gridRenderer.setEnsemble(-1);
    }

    // set levels
    if (this.dataState.zaxis != null) {
      levelNames = NamedObjects.getNames(this.dataState.zaxis);
      if ((currentLevelIdx < 0) || (currentLevelIdx >= levelNames.size()))
        currentLevelIdx = 0;
      // vertPanel.setCoordSys(currentField.getCoordinateSystem(), currentLevel);

      setChooserWanted("level", true);
      levelChooser.setCollection(levelNames.iterator(), true);
      NamedObject no = levelNames.get(currentLevelIdx);
      levelChooser.setSelectedByName(no.getName());

    } else {
      levelNames = new ArrayList<>();
      setChooserWanted("level", false);
      gridRenderer.setLevel(-1);
    }

    addChoosers();

    fieldChooser.setToolTipText(gg.getName());
    colorScalePanel.setUnitString(gg.getUnitsString());
    return true;
  }

  void setDrawHorizAndVert(boolean drawHoriz, boolean drawVert) {
    drawingPanel.removeAll();
    if (drawHoriz && drawVert) {
      // splitDraw.setTopComponent(panz);
      // splitDraw.setBottomComponent(vertPanel);
      drawingPanel.add(navPanel, BorderLayout.CENTER);
    } else if (drawHoriz) {
      drawingPanel.add(navPanel, BorderLayout.CENTER);
    } else if (drawVert) {
      drawingPanel.add(navPanel, BorderLayout.CENTER); // LOOK drawVert not supported
    }
  }

  public void setProjection(Projection p) {
    project = p;
    if (mapRenderer != null) // gridTable.setDataset(controller.getFields());

      mapRenderer.setProjection(p);
    gridRenderer.setViewProjection(p);
    // renderWind.setProjection( p);
    navPanel.setProjectionImpl(p);
    redrawLater();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  void start(boolean ok) {
    startOK = ok;
  }

  synchronized void draw(boolean immediate) {
    if (!startOK)
      return;

    gridRenderer.setLevel(currentLevelIdx);
    gridRenderer.setTime(currentTimeIdx);
    // renderGrid.setSlice(currentSlice);
    gridRenderer.setEnsemble(currentEnsembleIdx);
    gridRenderer.setRunTime(currentRuntimeIdx);

    if (drawHorizOn)
      drawH(immediate);
    // if (drawVertOn)
    // drawV(immediate);
  }

  private void drawH(boolean immediate) {
    if (!startOK)
      return;

    // cancel any redrawLater
    boolean already = redrawTimer.isRunning();
    if (already)
      redrawTimer.stop();

    long tstart = System.currentTimeMillis();
    long startTime, tookTime;

    //// horizontal slice
    // the Navigated Panel's BufferedImage graphics
    Graphics2D gNP = navPanel.getBufferedImageGraphics();
    if (gNP == null) // panel not drawn on screen yet
      return;

    // clear
    gNP.setBackground(navPanel.getBackgroundColor());
    gNP.fill(gNP.getClipBounds());

    // draw grid
    startTime = System.currentTimeMillis();
    try {
      gridRenderer.renderPlanView(gNP, atI);
    } catch (IOException | InvalidRangeException e) {
      e.printStackTrace();
    }
    if (Debug.isSet("timing/GridDraw")) {
      tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.GridDraw: " + tookTime * .001 + " seconds");
    }

    // draw Map
    if (mapRenderer != null) {
      startTime = System.currentTimeMillis();
      mapRenderer.draw(gNP, atI);
      if (Debug.isSet("timing/MapDraw")) {
        tookTime = System.currentTimeMillis() - startTime;
        System.out.println("timing/MapDraw: " + tookTime * .001 + " seconds");
      }
    }

    /*
     * draw Winds
     * if (drawWinds) {
     * startTime = System.currentTimeMillis();
     * renderWind.draw(gNP, currentLevel, currentTime);
     * if (Debug.isSet("timing/WindsDraw")) {
     * tookTime = System.currentTimeMillis() - startTime;
     * System.out.println("timing.WindsDraw: " + tookTime*.001 + " seconds");
     * }
     * }
     */

    // copy buffer to the screen
    if (immediate)
      navPanel.drawG();
    else
      navPanel.repaint();

    // cleanup
    gNP.dispose();

    if (Debug.isSet("timing/total")) {
      tookTime = System.currentTimeMillis() - tstart;
      System.out.println("timing.total: " + tookTime * .001 + " seconds");
    }
  }

  /*
   * private void drawV(boolean immediate) {
   * if (!startOK) return;
   * ScaledPanel drawArea = vertPanel.getDrawArea();
   * Graphics2D gV = drawArea.getBufferedImageGraphics();
   * if (gV == null)
   * return;
   * 
   * long startTime = System.currentTimeMillis();
   * 
   * gV.setBackground(Color.white);
   * gV.fill(gV.getClipBounds());
   * renderGrid.renderVertView(gV, atI);
   * 
   * if (Debug.isSet("timing/GridDrawVert")) {
   * long tookTime = System.currentTimeMillis() - startTime;
   * System.out.println("timing.GridDrawVert: " + tookTime*.001 + " seconds");
   * }
   * gV.dispose();
   * 
   * // copy buffer to the screen
   * if (immediate)
   * drawArea.drawNow();
   * else
   * drawArea.repaint();
   * }
   */

  private synchronized void redrawLater() {
    // redrawComplete |= complete;
    boolean already = redrawTimer.isRunning();
    if (already)
      redrawTimer.restart();
    else
      redrawTimer.start();
  }

  public ProjectionManager getProjectionManager() {
    if (null != projManager)
      return projManager;

    projManager = new ProjectionManager(null, store);
    projManager.addPropertyChangeListener(e -> {
      if (e.getPropertyName().equals("ProjectionImpl")) {
        Projection p = (Projection) e.getNewValue();
        // p = p.constructCopy();
        // System.out.println("UI: new Projection "+p);
        setProjection(p);
      }
    });

    return projManager;
  }

  private void addChoosers() {
    fieldPanel.removeAll();
    for (Chooser c : choosers) {
      if (c.isWanted)
        fieldPanel.add(c.field);
    }
  }

  private static class Chooser {
    Chooser(String name, SuperComboBox field, boolean want) {
      this.name = name;
      this.field = field;
      this.isWanted = want;
    }

    boolean isWanted; // should be displayed
    String name;
    SuperComboBox field;
  }

  private void setChooserWanted(String name, boolean want) {
    for (Chooser chooser : choosers) {
      if (chooser.name.equals(name))
        chooser.isWanted = want;
    }
  }

  private void addActionsToMenus(JMenu datasetMenu, JMenu configMenu, JMenu toolMenu) {
    // Info
    BAMutil.addActionToMenu(datasetMenu, showDatasetInfoAction);

    /// Configure
    JMenu toolbarMenu = new JMenu("Toolbars");
    toolbarMenu.setMnemonic('T');
    configMenu.add(toolbarMenu);
    BAMutil.addActionToMenu(toolbarMenu, navToolbarAction);
    BAMutil.addActionToMenu(toolbarMenu, moveToolbarAction);

    BAMutil.addActionToMenu(configMenu, chooseProjectionAction);
    BAMutil.addActionToMenu(configMenu, saveCurrentProjectionAction);

    //// tools menu
    JMenu displayMenu = new JMenu("Display control");
    displayMenu.setMnemonic('D');

    BAMutil.addActionToMenu(displayMenu, showGridAction);
    BAMutil.addActionToMenu(displayMenu, showContoursAction);
    BAMutil.addActionToMenu(displayMenu, showContourLabelsAction);
    BAMutil.addActionToMenu(displayMenu, redrawAction);
    toolMenu.add(displayMenu);

    // Loop Control
    JMenu loopMenu = new JMenu("Loop control");
    loopMenu.setMnemonic('L');

    BAMutil.addActionToMenu(loopMenu, fieldLoopAction);
    BAMutil.addActionToMenu(loopMenu, levelLoopAction);
    BAMutil.addActionToMenu(loopMenu, timeLoopAction);
    BAMutil.addActionToMenu(loopMenu, runtimeLoopAction);
    toolMenu.add(loopMenu);

    // MinMax Control
    JMenu mmMenu = new JMenu("ColorScale min/max");
    mmMenu.setMnemonic('C');
    BAMutil.addActionToMenu(mmMenu, minmaxHorizAction);
    BAMutil.addActionToMenu(mmMenu, minmaxLogAction);
    BAMutil.addActionToMenu(mmMenu, minmaxHoldAction);
    toolMenu.add(mmMenu);

    // Zoom/Pan
    JMenu zoomMenu = new JMenu("Zoom/Pan");
    zoomMenu.setMnemonic('Z');
    navPanel.addActionsToMenu(zoomMenu); // items are added by NavigatedPanelToolbar
    toolMenu.add(zoomMenu);
  }

  // loop control for SuperComboBox
  private static class LoopControlAction extends AbstractAction {
    SuperComboBox scbox;

    LoopControlAction(SuperComboBox cbox) {
      this.scbox = cbox;
      BAMutil.setActionProperties(this, null, cbox.getName(), false, 0, 0);
    }

    public void actionPerformed(ActionEvent e) {
      scbox.getLoopControl().show();
    }
  }

}

