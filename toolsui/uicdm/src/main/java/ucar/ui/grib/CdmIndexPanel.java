/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.grib;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribCollectionImmutable.Record;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.coord.CoordinateTimeIntv;
import ucar.nc2.grib.coord.CoordinateVert;
import ucar.nc2.grib.coord.SparseArray;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.grib.coord.VertCoordValue;
import ucar.nc2.internal.util.Counters;
import ucar.nc2.calendar.*;
import ucar.ui.MFileTable;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.util.Indent;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/** Show info in GRIB ncx index files. */
public class CdmIndexPanel extends JPanel {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmIndexPanel.class);

  private final PreferencesExt prefs;
  private final BeanTable<GroupBean> groupTable;
  private final BeanTable<VarBean> varTable;
  private final BeanTable<CoordBean> coordTable;
  private final JSplitPane split2, split3;

  private final TextHistoryPane infoTA, extraTA;
  private final IndependentWindow infoWindow, extraWindow;
  private final MFileTable fileTable;

  public CdmIndexPanel(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    /////////////////////////////////////////
    // the info windows
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    extraTA = new TextHistoryPane();
    extraWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), extraTA);
    extraWindow.setBounds((Rectangle) prefs.getBean("ExtraWindowBounds", new Rectangle(300, 300, 500, 300)));

    if (buttPanel != null) {
      AbstractButton infoButton = BAMutil.makeButtcon("Information", "Show Info", false);
      infoButton.addActionListener(e -> {
        Formatter f = new Formatter();
        showInfo(f);
        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      });
      buttPanel.add(infoButton);

      AbstractButton filesButton = BAMutil.makeButtcon("nj22/Catalog", "Show Files", false);
      filesButton.addActionListener(e -> {
        if (gc != null)
          showFileTable(gc, null);
      });
      buttPanel.add(filesButton);

      AbstractButton rawButton = BAMutil.makeButtcon("TableAppearence", "Estimate memory use", false);
      rawButton.addActionListener(e -> {
        Formatter f = new Formatter();
        showMemoryEst(f);
        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      });
      buttPanel.add(rawButton);

      AbstractButton checkAllButton = BAMutil.makeButtcon("Select", "Check entire file", false);
      rawButton.addActionListener(e -> {
        Formatter f = new Formatter();
        checkAll(f);
        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      });
      buttPanel.add(checkAllButton);

    }

    PopupMenu varPopup;
    groupTable = new BeanTable<>(GroupBean.class, (PreferencesExt) prefs.node("GroupBean"), false, "GDS group",
        "GribCollectionImmutable.GroupHcs", null);
    groupTable.addListSelectionListener(e -> {
      GroupBean bean = groupTable.getSelectedBean();
      if (bean != null)
        setGroup(bean);
    });

    varPopup = new PopupMenu(groupTable.getJTable(), "Options");
    varPopup.addAction("Show Group Info", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GroupBean bean = groupTable.getSelectedBean();
        if (bean != null && bean.group != null) {
          Formatter f = new Formatter();
          bean.group.show(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });
    varPopup.addAction("Show Files Used", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GroupBean bean = groupTable.getSelectedBean();
        if (bean != null && bean.group != null) {
          showFileTable(gc, bean.group);
        }
      }
    });

    varTable = new BeanTable<>(VarBean.class, (PreferencesExt) prefs.node("Grib2Bean"), false, "Variables in group",
        "GribCollectionImmutable.VariableIndex", null);

    varPopup = new PopupMenu(varTable.getJTable(), "Options");
    varPopup.addAction("Show Variable(s)", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<VarBean> beans = varTable.getSelectedBeans();
        infoTA.clear();
        for (VarBean bean : beans) {
          infoTA.appendLine(bean.name);
          infoTA.appendLine(bean.v.toStringFrom());
        }
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    varPopup.addAction("Show Sparse Array", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VarBean bean = varTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.showSparseArray(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });
    varPopup.addAction("Make Variable(s) GribConfig", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<VarBean> beans = varTable.getSelectedBeans();
        infoTA.clear();
        Formatter f = new Formatter();
        for (VarBean bean : beans)
          bean.makeGribConfig(f);
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });


    coordTable = new BeanTable<>(CoordBean.class, (PreferencesExt) prefs.node("CoordBean"), false,
        "Coordinates in group", "Coordinates", null);
    varPopup = new PopupMenu(coordTable.getJTable(), "Options");

    varPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.coord.showCoords(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("ShowCompact", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.coord.showInfo(f, new Indent(2));
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("Test Time2D isOrthogonal", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          testOrthogonal(f, bean.coord);
          extraTA.setText(f.toString());
          extraTA.gotoTop();
          extraWindow.show();
        }
      }
    });

    varPopup.addAction("Test Time2D isRegular", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          testRegular(f, bean.coord);
          extraTA.setText(f.toString());
          extraTA.gotoTop();
          extraWindow.show();
        }
      }
    });


    varPopup.addAction("Show Resolution distribution", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoordBean bean = coordTable.getSelectedBean();
        if (bean != null) {
          Formatter f = new Formatter();
          bean.showResolution(f);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("Compare", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<CoordBean> beans = coordTable.getSelectedBeans();
        if (beans.size() == 2) {
          Formatter f = new Formatter();
          CoordBean bean1 = beans.get(0);
          CoordBean bean2 = beans.get(1);
          if (bean1.coord.getType() == Coordinate.Type.time2D && bean2.coord.getType() == Coordinate.Type.time2D)
            compareCoords2D(f, (CoordinateTime2D) bean1.coord, (CoordinateTime2D) bean2.coord);
          else
            compareCoords(f, bean1.coord, bean2.coord);
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("Try to Merge", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<CoordBean> beans = coordTable.getSelectedBeans();
        if (beans.size() == 2) {
          Formatter f = new Formatter();
          CoordBean bean1 = beans.get(0);
          CoordBean bean2 = beans.get(1);
          if (bean1.coord.getType() == Coordinate.Type.time2D && bean2.coord.getType() == Coordinate.Type.time2D)
            mergeCoords2D(f, (CoordinateTime2D) bean1.coord, (CoordinateTime2D) bean2.coord);
          else
            f.format("CoordinateTime2D only");
          infoTA.setText(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    // file popup window
    fileTable = new MFileTable((PreferencesExt) prefs.node("MFileTable"), true);
    fileTable.addPropertyChangeListener(
        evt -> firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()));

    setLayout(new BorderLayout());

    split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, groupTable, varTable);
    split3.setDividerLocation(prefs.getInt("splitPos3", 800));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split3, coordTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    // split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, vertCoordTable);
    // split.setDividerLocation(prefs.getInt("splitPos", 500));

    add(split2, BorderLayout.CENTER);
  }

  public void save() {
    groupTable.saveState(false);
    varTable.saveState(false);
    coordTable.saveState(false);
    fileTable.save();
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("ExtraWindowBounds", extraWindow.getBounds());
    if (split2 != null)
      prefs.putInt("splitPos2", split2.getDividerLocation());
    if (split3 != null)
      prefs.putInt("splitPos3", split3.getDividerLocation());
  }

  public void clear() {
    if (gc != null) {
      try {
        gc.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    gc = null;
    groupTable.clearBeans();
    varTable.clearBeans();
    coordTable.clearBeans();
  }

  public void showInfo(Formatter f) {
    if (gc == null)
      return;
    gc.showIndex(f);
    f.format("%n");

    f.format("Groups%n");
    List<GroupBean> groups = groupTable.getBeans();
    for (GroupBean bean : groups) {
      f.format("%-50s %d%n", bean.getGroupId(), bean.getGdsHash());
      bean.group.show(f);
    }

    f.format("%n");
    // showFiles(f);
  }

  private void showFileTable(GribCollectionImmutable gc, GribCollectionImmutable.GroupGC group) {
    File dir = gc.getDirectory();
    Collection<MFile> files = (group == null) ? gc.getFiles() : group.getFiles();
    fileTable.setFiles(dir, files);
  }

  private static class SortBySize implements Comparable<SortBySize> {
    Object obj;
    int size;

    private SortBySize(Object obj, int size) {
      this.obj = obj;
      this.size = size;
    }

    public int compareTo(SortBySize o) {
      return Integer.compare(size, o.size);
    }
  }

  private void checkAll(Formatter f) {
    if (gc == null)
      return;
    for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
      f.format("Dataset %s%n", ds.getType());
      int bytesTotal = 0;
      int bytesSATotal = 0;

      for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
        f.format(" Group %s%n", g.getDescription());

        int count = 0;
        for (GribCollectionImmutable.VariableIndex v : g.getVariables()) {
          VarBean bean = new VarBean(v, g);

          if (v instanceof PartitionCollectionImmutable.VariableIndexPartitioned) {
            if (count == 0)
              f.format(" total   VariablePartitioned%n");

            PartitionCollectionImmutable.VariableIndexPartitioned vip =
                (PartitionCollectionImmutable.VariableIndexPartitioned) v;
            int nparts = vip.getNparts();
            int memEstBytes = 368 + nparts * 4; // very rough
            bytesTotal += memEstBytes;
            f.format("%6d %-50s nparts=%6d%n", memEstBytes, bean.getName(), nparts);

          } else {
            if (count == 0)
              f.format(" total   SA  Variable%n");
            try {
              v.readRecords();
              SparseArray<Record> sa = v.getSparseArray();
              int ntracks = sa.getTotalSize();
              int nrecords = sa.getContent().size();
              int memEstForSA = 276 + nrecords * 40 + ntracks * 4;
              int memEstBytes = 280 + memEstForSA;
              f.format("%6d %6d %-50s nrecords=%6d%n", memEstBytes, memEstForSA, bean.getName(), nrecords);
              bytesTotal += memEstBytes;
              bytesSATotal += memEstForSA;

            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          count++;
        }
      }
      int noSA = bytesTotal - bytesSATotal;
      f.format("%n total KBytes=%d kbSATotal=%d kbNoSA=%d%n", bytesTotal / 1000, bytesSATotal / 1000, noSA / 1000);
      f.format("%n");
    }
  }

  private void showMemoryEst(Formatter f) {
    if (gc == null)
      return;

    for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
      f.format("Dataset %s%n", ds.getType());
      int bytesTotal = 0;
      int bytesSATotal = 0;
      int coordsAllTotal = 0;

      for (GribCollectionImmutable.GroupGC g : ds.getGroups()) {
        f.format(" Group %s%n", g.getDescription());

        List<SortBySize> sortList = new ArrayList<>(g.getCoordinates().size());
        for (Coordinate vc : g.getCoordinates())
          sortList.add(new SortBySize(vc, vc.estMemorySize()));
        Collections.sort(sortList);

        int coordsTotal = 0;
        f.format("  totalKB  type Coordinate%n");
        for (SortBySize ss : sortList) {
          Coordinate vc = (Coordinate) ss.obj;
          f.format("  %6d %-8s %-40s%n", vc.estMemorySize() / 1000, vc.getType(), vc.getName());
          bytesTotal += vc.estMemorySize();
          coordsTotal += vc.estMemorySize();
        }
        f.format(" %7d KBytes%n", coordsTotal / 1000);
        f.format("%n");
        coordsAllTotal += coordsTotal;

        int count = 0;
        for (GribCollectionImmutable.VariableIndex v : g.getVariables()) {
          VarBean bean = new VarBean(v, g);

          if (v instanceof PartitionCollectionImmutable.VariableIndexPartitioned) {
            if (count == 0)
              f.format(" total   VariablePartitioned%n");

            PartitionCollectionImmutable.VariableIndexPartitioned vip =
                (PartitionCollectionImmutable.VariableIndexPartitioned) v;
            int nparts = vip.getNparts();
            int memEstBytes = 368 + nparts * 4; // very rough
            bytesTotal += memEstBytes;
            f.format("%6d %-50s nparts=%6d%n", memEstBytes, bean.getName(), nparts);

          } else {
            if (count == 0)
              f.format(" total   SA  Variable%n");
            try {
              v.readRecords();
              SparseArray<GribCollectionImmutable.Record> sa = v.getSparseArray();
              int ntracks = sa.getTotalSize();
              int nrecords = sa.getContent().size();
              int memEstForSA = 276 + nrecords * 40 + ntracks * 4;
              int memEstBytes = 280 + memEstForSA;
              f.format("%6d %6d %-50s nrecords=%6d%n", memEstBytes, memEstForSA, bean.getName(), nrecords);
              bytesTotal += memEstBytes;
              bytesSATotal += memEstForSA;

            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          count++;
        }
      }
      int noSA = bytesTotal - bytesSATotal;
      f.format("%n total KBytes=%d kbSATotal=%d kbNoSA=%d coordsAllTotal=%d%n", bytesTotal / 1000, bytesSATotal / 1000,
          noSA / 1000, coordsAllTotal / 1000);
      f.format("%n");
    }
  }

  private void compareCoords(Formatter f, Coordinate coord1, Coordinate coord2) {
    List<?> vals1 = coord1.getValues();
    List<?> vals2 = coord2.getValues();
    f.format("Coordinate %s%n", coord1.getName());
    for (Object val1 : vals1) {
      if (!vals2.contains(val1)) {
        f.format(" %s ", val1);
        if (val1 instanceof Long)
          f.format("(%s) ", CalendarDate.of((Long) val1));
        f.format("MISSING IN 2%n");
      }
    }
    f.format("%nCoordinate %s%n", coord2.getName());
    for (Object val2 : vals2) {
      if (!vals1.contains(val2)) {
        f.format(" %s ", val2);
        if (val2 instanceof Long)
          f.format("(%s) ", CalendarDate.of((Long) val2));
        f.format("MISSING IN 1%n");
      }
    }
  }

  private void compareCoords2D(Formatter f, CoordinateTime2D coord1, CoordinateTime2D coord2) {
    CoordinateRuntime runtimes1 = coord1.getRuntimeCoordinate();
    CoordinateRuntime runtimes2 = coord2.getRuntimeCoordinate();
    int n1 = coord1.getNruns();
    int n2 = coord2.getNruns();
    if (n1 != n2) {
      f.format("Coordinate 1 has %d runtimes, Coordinate 2 has %d runtimes, %n", n1, n2);
    }

    int min = Math.min(n1, n2);
    for (int idx = 0; idx < min; idx++) {
      CoordinateTimeAbstract time1 = coord1.getTimeCoordinate(idx);
      CoordinateTimeAbstract time2 = coord2.getTimeCoordinate(idx);
      f.format("Run %d %n", idx);
      if (!runtimes1.getValue(idx).equals(runtimes2.getValue(idx)))
        f.format("Runtime 1 %s != %s runtime 2%n", runtimes1.getValue(idx), runtimes2.getValue(idx));
      compareCoords(f, time1, time2);
    }
  }

  private void mergeCoords2D(Formatter f, CoordinateTime2D coord1, CoordinateTime2D coord2) {
    if (coord1.isTimeInterval() != coord2.isTimeInterval()) {
      f.format("Coordinate 1 isTimeInterval %s != Coordinate 2 isTimeInterval %s %n", coord1.isTimeInterval(),
          coord2.isTimeInterval());
      return;
    }

    CoordinateRuntime runtimes1 = coord1.getRuntimeCoordinate();
    CoordinateRuntime runtimes2 = coord2.getRuntimeCoordinate();
    int n1 = coord1.getNruns();
    int n2 = coord2.getNruns();
    if (n1 != n2) {
      f.format("Coordinate 1 has %d runtimes, Coordinate 2 has %d runtimes, %n", n1, n2);
    }

    int min = Math.min(n1, n2);
    for (int idx = 0; idx < min; idx++) {
      if (!runtimes1.getValue(idx).equals(runtimes2.getValue(idx)))
        f.format("Runtime 1 %s != %s runtime 2%n", runtimes1.getValue(idx), runtimes2.getValue(idx));
    }

    Set<Object> set1 = makeCoordSet(coord1);
    List<?> list1 = coord1.getOffsetsSorted();
    Set<Object> set2 = makeCoordSet(coord2);
    List<?> list2 = coord2.getOffsetsSorted();

    f.format("%nCoordinate %s%n", coord1.getName());
    for (Object val : list1)
      f.format(" %s,", val);
    f.format(" n=(%d)%n", list1.size());
    testMissing(f, list1, set2);

    f.format("%nCoordinate %s%n", coord2.getName());
    for (Object val : list2)
      f.format(" %s,", val);
    f.format(" (n=%d)%n", list2.size());
    testMissing(f, list2, set1);
  }

  private Set<Object> makeCoordSet(CoordinateTime2D time2D) {
    Set<Object> result = new HashSet<>(100);
    for (int runIdx = 0; runIdx < time2D.getNruns(); runIdx++) {
      Coordinate coord = time2D.getTimeCoordinate(runIdx);
      result.addAll(coord.getValues());
    }
    return result;
  }

  private void testMissing(Formatter f, List<?> test, Set<Object> against) {
    int countMissing = 0;
    for (Object val1 : test) {
      if (!against.contains(val1))
        f.format(" %d: %s MISSING%n", countMissing++, val1);
    }
    f.format("TOTAL MISSING %s%n", countMissing);
  }

  // orthogonal means that all the times can be made into a single time coordinate
  private boolean testOrthogonal(Formatter f, Coordinate c) {
    if (!(c instanceof CoordinateTime2D)) {
      f.format("testOrthogonal only on CoordinateTime2D%n");
      return false;
    }
    CoordinateTime2D time2D = (CoordinateTime2D) c;
    List<CoordinateTimeAbstract> coords = new ArrayList<>();
    for (int runIdx = 0; runIdx < time2D.getNruns(); runIdx++) {
      coords.add(time2D.getTimeCoordinate(runIdx));
    }
    return testOrthogonal(f, coords, true);
  }

  private boolean testOrthogonal(Formatter f, List<CoordinateTimeAbstract> times, boolean show) {
    int max = 0;
    Map<Object, Integer> allCoords = new HashMap<>(1000);
    for (CoordinateTimeAbstract coord : times) {
      max = Math.max(max, coord.getSize());
      for (Object val : coord.getValues()) {
        allCoords.merge(val, 1, (a, b) -> a + b);
      }
    }

    // is the set of all values the same as the component times?
    int totalMax = allCoords.size();
    boolean isOrthogonal = (totalMax == max);

    if (show) {
      f.format("isOrthogonal %s : allCoords.size = %d max of coords=%d%nAllCoords=%n", isOrthogonal, totalMax, max);
      List allList = new ArrayList<>(allCoords.keySet());
      Collections.sort(allList);
      for (Object coord : allList) {
        Integer count = allCoords.get(coord);
        f.format("  %4d %s%n", count, coord);
      }
    }

    return isOrthogonal;
  }

  // regular means that all the times for each offset from 0Z can be made into a single time coordinate (FMRC algo)
  private boolean testRegular(Formatter f, Coordinate c) {
    if (!(c instanceof CoordinateTime2D)) {
      f.format("testRegular only on CoordinateTime2D%n");
      return false;
    }
    CoordinateTime2D time2D = (CoordinateTime2D) c;

    // group time coords by offset hour
    Map<Integer, List<CoordinateTimeAbstract>> hourMap = new HashMap<>(); // <hour, all coords for that hour>
    for (int runIdx = 0; runIdx < time2D.getNruns(); runIdx++) {
      CoordinateTimeAbstract coord = time2D.getTimeCoordinate(runIdx);
      CalendarDate runDate = coord.getRefDate();
      int hour = runDate.getHourOfDay();
      List<CoordinateTimeAbstract> hg = hourMap.computeIfAbsent(hour, k -> new ArrayList<>());
      hg.add(coord);
    }

    // see if each offset hour is orthogonal
    boolean ok = true;
    for (int hour : hourMap.keySet()) {
      List<CoordinateTimeAbstract> hg = hourMap.get(hour);
      boolean isOrthogonal = testOrthogonal(f, hg, false); // TODO why orthoginal, why not regular?
      f.format("Hour %d: isOrthogonal=%s%n", hour, isOrthogonal);
      ok &= isOrthogonal;
    }
    f.format("%nAll are orthogonal: %s%n", ok);
    if (ok)
      return true;

    for (int hour : hourMap.keySet()) {
      List<CoordinateTimeAbstract> hg = hourMap.get(hour);
      f.format("Hour %d: %n", hour);
      testOrthogonal(f, hg, true);
      f.format("%n");
    }
    return false;
  }

  ///////////////////////////////////////////////
  Path indexFile;
  GribCollectionImmutable gc;
  Collection<MFile> gcFiles;
  FeatureCollectionConfig config = new FeatureCollectionConfig();

  public void setIndexFile(Path indexFile, FeatureCollectionConfig config) throws IOException {
    if (gc != null)
      gc.close();

    this.indexFile = indexFile;
    this.config = config;
    gc = GribCdmIndex.openCdmIndex(indexFile.toString(), config, false, logger);
    if (gc == null)
      throw new IOException("File not a grib collection index file");

    List<GroupBean> groups = new ArrayList<>();

    for (GribCollectionImmutable.Dataset ds : gc.getDatasets())
      for (GribCollectionImmutable.GroupGC g : ds.getGroups())
        groups.add(new GroupBean(g, ds.getType().toString()));

    if (!groups.isEmpty())
      setGroup(groups.get(0));
    else {
      varTable.clearBeans();
      coordTable.clearBeans();
    }

    groupTable.setBeans(groups);
    groupTable.setHeader(indexFile.toString());
    gcFiles = gc.getFiles();
  }

  private void setGroup(GroupBean bean) {
    bean.clear();
    List<VarBean> vars = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex v : bean.group.getVariables()) {
      VarBean vbean = new VarBean(v, bean.group);
      vars.add(vbean);
      bean.nrecords += vbean.getNrecords();
      bean.ndups += vbean.getNdups();
      bean.nmissing += vbean.getNmissing();
    }
    varTable.setBeans(vars);

    int count = 0;
    List<CoordBean> coords = new ArrayList<>();
    for (Coordinate vc : bean.group.getCoordinates())
      coords.add(new CoordBean(vc, count++));
    coordTable.setBeans(coords);
  }

  ////////////////////////////////////////////////////////////////////////////

  public class GroupBean {
    GribCollectionImmutable.GroupGC group;
    String type;
    int nrecords, nmissing, ndups;

    public GroupBean() {}

    public GroupBean(GribCollectionImmutable.GroupGC g, String type) {
      this.group = g;
      this.type = type;

      for (GribCollectionImmutable.VariableIndex vi : group.getVariables()) {
        nrecords += vi.getNrecords();
        ndups += vi.getNdups();
        nmissing += vi.getNmissing();
      }
    }

    void clear() {
      nmissing = 0;
      ndups = 0;
      nrecords = 0;
    }

    public String getGroupId() {
      return group.getId();
    }

    public int getGdsHash() {
      return group.getGdsHash().hashCode();
    }

    public int getNrecords() {
      return nrecords;
    }

    public String getType() {
      return type;
    }

    public int getNFiles() {
      int n = group.getNFiles();
      if (n == 0) {
        if (gc instanceof PartitionCollectionImmutable)
          n = ((PartitionCollectionImmutable) gc).getPartitionSize();
      }
      return n;
    }

    public int getNruntimes() {
      return group.getNruntimes();
    }

    public int getNCoords() {
      return group.getCoordinates().size();
    }

    public int getNVariables() {
      return group.getVariables().size();
    }

    public int getNmissing() {
      return nmissing;
    }

    public int getNdups() {
      return ndups;
    }
  }


  public class CoordBean implements Comparable<CoordBean> {
    Coordinate coord;
    int idx;
    double start, end, resol;
    Comparable resolMode;
    Counters counters;

    // no-arg constructor

    public CoordBean() {}

    public CoordBean(Coordinate coord, int idx) {
      this.coord = coord;
      this.idx = idx;
      counters = coord.calcDistributions();
      resolMode = counters.get("resol").getMode();

      if (coord instanceof CoordinateRuntime) {
        CoordinateRuntime runtime = (CoordinateRuntime) coord;
        List<Double> offsets = runtime.getOffsetsInTimeUnits();
        long offsetFromMaster = runtime.getOffsetFrom(gc.getMasterFirstDate());

        int n = offsets.size();
        start = offsets.get(0) + offsetFromMaster;
        end = offsets.get(n - 1) + offsetFromMaster;
        resol = (n > 1) ? (end - start) / (n - 1) : 0.0;

      } else if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D time = (CoordinateTime2D) coord;
        List<?> offsets = time.getOffsetsSorted();
        int n = offsets.size();
        // double offsetFromMaster = time.getOffsetInTimeUnits(gc.getMasterFirstDate());

        if (time.isTimeInterval()) {
          start = ((TimeCoordIntvValue) offsets.get(0)).getBounds1(); // + offsetFromMaster;
          end = ((TimeCoordIntvValue) offsets.get(n - 1)).getBounds2(); // + offsetFromMaster;
          resol = (n > 1) ? (end - start) / (n - 1) : 0.0;

        } else {
          start = (Integer) offsets.get(0); // + offsetFromMaster;
          end = (Integer) offsets.get(n - 1); // + offsetFromMaster;
          resol = (n > 1) ? (end - start) / (n - 1) : 0.0;
        }

      } else if (coord instanceof CoordinateTime) {
        CoordinateTime time = (CoordinateTime) coord;
        List<Long> offsets = time.getOffsetSorted();
        int n = offsets.size();
        double offsetFromMaster = time.getOffsetInTimeUnits(gc.getMasterFirstDate());
        start = offsets.get(0) + offsetFromMaster;
        end = offsets.get(n - 1) + offsetFromMaster;
        resol = (n > 1) ? (end - start) / (n - 1) : 0.0;

      } else if (coord instanceof CoordinateTimeIntv) {
        CoordinateTimeIntv time = (CoordinateTimeIntv) coord;
        List<TimeCoordIntvValue> offsets = time.getTimeIntervals();
        double offsetFromMaster = time.getOffsetInTimeUnits(gc.getMasterFirstDate());
        int n = offsets.size();
        start = offsets.get(0).getBounds1() + offsetFromMaster;
        end = offsets.get(n - 1).getBounds2() + offsetFromMaster;

      } else if (coord instanceof CoordinateVert) {
        CoordinateVert vert = (CoordinateVert) coord;
        List<VertCoordValue> offsets = vert.getLevelSorted();
        int n = offsets.size();
        if (vert.isLayer()) {
          start = offsets.get(0).getValue1();
          end = offsets.get(n - 1).getValue2();
          resol = (n > 1) ? (end - start) / (n - 1) : 0.0;
        } else {
          start = offsets.get(0).getValue1();
          end = offsets.get(n - 1).getValue1();
          resol = (n > 1) ? (end - start) / (n - 1) : 0.0;
        }
      }

    }

    private void showResolution(Formatter f) {
      counters.show(f);
    }

    public double getStart() {
      return start;
    }

    public double getEnd() {
      return end;
    }

    public double getResol() {
      return resol;
    }

    public String getResolMode() {
      return (resolMode == null) ? "null" : resolMode.toString();
    }

    public String getValues() {
      Formatter f = new Formatter();
      if (coord instanceof CoordinateRuntime) {
        CoordinateRuntime runtime = (CoordinateRuntime) coord;
        f.format("%s %s", runtime.getFirstDate(), runtime.getLastDate());

      } else if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D coord2D = (CoordinateTime2D) coord;
        CalendarDateRange dr = coord2D.makeCalendarDateRange();
        f.format("%s %s", dr.getStart(), dr.getEnd());

      } else {
        if (coord.getValues() == null)
          return "";
        for (Object val : coord.getValues())
          f.format("%s,", val);
      }
      return f.toString();
    }

    public String getType() {
      if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D c2d = (CoordinateTime2D) coord;
        Formatter f = new Formatter();
        f.format("%s %s", coord.getType(), (c2d.isTimeInterval() ? "intv" : "offs"));
        if (c2d.isOrthogonal())
          f.format(" ort");
        if (c2d.isRegular())
          f.format(" reg");
        return f.toString();
      } else {
        return coord.getType().toString();
      }
    }

    public String getSize() {
      if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D c2d = (CoordinateTime2D) coord;
        Formatter f = new Formatter();
        f.format("%d X %d (%d)", c2d.getRuntimeCoordinate().getSize(), c2d.getNtimes(), coord.getSize());
        return f.toString();
      } else {
        return Integer.toString(coord.getSize());
      }
    }

    public int getCode() {
      return coord.getCode();
    }

    public int getIndex() {
      return idx;
    }

    public String getUnit() {
      return coord.getUnit();
    }

    public String getRefDate() {
      if (coord instanceof CoordinateTimeAbstract)
        return ((CoordinateTimeAbstract) coord).getRefDate().toString();
      else if (coord instanceof CoordinateRuntime)
        return ((CoordinateRuntime) coord).getFirstDate().toString();
      else
        return "";
    }

    public String getName() {
      String intvName = null;
      if (coord instanceof CoordinateTimeIntv) {
        CoordinateTimeIntv timeiCoord = (CoordinateTimeIntv) coord;
        intvName = timeiCoord.getTimeIntervalName();
      }
      if (coord instanceof CoordinateTime2D) {
        CoordinateTime2D timeiCoord = (CoordinateTime2D) coord;
        intvName = timeiCoord.getTimeIntervalName();
      }

      return (intvName == null) ? coord.getName() : coord.getName() + " (" + intvName + ")";
    }

    @Override
    public int compareTo(CoordBean o) {
      return getType().compareTo(o.getType());
    }

    void showCoords(Formatter f) {
      coord.showCoords(f);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////

  public static class VarBean {
    GribCollectionImmutable.VariableIndex v;
    GribCollectionImmutable.GroupGC group;
    String name;

    public VarBean() {}

    public VarBean(GribCollectionImmutable.VariableIndex vindex, GribCollectionImmutable.GroupGC group) {
      this.v = vindex;
      this.group = group;
      this.name = vindex.makeVariableName();
    }

    public String getIndexes() {
      Formatter f = new Formatter();
      for (int idx : v.getCoordinateIndex())
        f.format("%d,", idx);
      return f.toString();
    }

    public String getIntvName() {
      return v.getIntvName();
    }

    public int getCdmHash() {
      return v.hashCode();
    }

    public String getGroupId() {
      return group.getId();
    }

    public String getVariableId() {
      return v.getDiscipline() + "-" + v.getCategory() + "-" + v.getParameter();
    }

    public String getName() {
      return name;
    }

    public int getNdups() {
      return v.getNdups();
    }

    public int getNrecords() {
      return v.getNrecords();
    }

    public int getNmissing() {
      return v.getNmissing();
    }

    public int getRectMissing() {
      int n = v.getSize();
      return n - v.getNrecords();
    }

    public int getSize() {
      return v.getSize();
    }

    public void makeGribConfig(Formatter f) {
      f.format("<variable id='%s'/>%n", getVariableId());
    }

    private void showSparseArray(Formatter f) {
      f.format("%s%n", getName());
      try {
        if (v instanceof PartitionCollectionImmutable.VariableIndexPartitioned) {
          PartitionCollectionImmutable.VariableIndexPartitioned vip =
              (PartitionCollectionImmutable.VariableIndexPartitioned) v;
          vip.showSparseArray(f);
          vip.show(f);

        } else {
          // make sure sparse array has been read in.
          v.readRecords();

          if (v.getSparseArray() != null) {
            SparseArray<GribCollectionImmutable.Record> sa = v.getSparseArray();
            sa.showInfo(f, null);
            f.format("%n");
            sa.showTracks(f);
            f.format("%n");
            sa.showContent(f);
          }
        }
      } catch (IOException e) {
        logger.warn("Failed to showSparseArray for variable " + v, e);
      }
    }
  }
}
