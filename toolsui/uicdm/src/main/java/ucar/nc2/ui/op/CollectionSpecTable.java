/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import com.google.common.collect.ImmutableList;
import thredds.inventory.*;
import ucar.nc2.time2.CalendarDate;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Feature Collection Spec parsing
 *
 * @author caron
 * @since 4/9/13
 */
public class CollectionSpecTable extends JPanel {
  private final PreferencesExt prefs;

  private final BeanTable<Bean> ftTable;
  private final JSplitPane split;
  private final TextHistoryPane dumpTA;
  private final IndependentWindow infoWindow;

  private MFileCollectionManager dcm;

  public CollectionSpecTable(PreferencesExt prefs) {
    this.prefs = prefs;

    ftTable = new BeanTable<>(Bean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);

    // the info window
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    dumpTA = new TextHistoryPane();
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ftTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    ftTable.saveState(false);
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public boolean setCollection(String spec) {
    spec = spec.trim();
    Formatter f = new Formatter();

    if (spec.startsWith("<collection ")) {
      dcm = setCollectionElement(spec, f);
    } else {
      CollectionSpecParser sp = new CollectionSpecParser(spec, f);
      f.format("spec='%s'%n", sp);
      dcm = scanCollection(spec, f);
    }
    showCollection(f);
    dumpTA.setText(f.toString());
    return dcm != null;
  }

  private static final String SPEC = "spec='";
  private static final String DFM = "dateFormatMark='";

  private MFileCollectionManager setCollectionElement(String elem, Formatter f) {
    String spec = null;
    int pos1 = elem.indexOf(SPEC);
    if (pos1 > 0) {
      int pos2 = elem.indexOf("'", pos1 + SPEC.length());
      if (pos2 > 0) {
        spec = elem.substring(pos1 + SPEC.length(), pos2);
      }
    }
    if (spec == null) {
      f.format("want <collection spec='spec' [dateFormatMark='dfm'] ... %n");
      return null;
    }
    f.format("spec='%s' %n", spec);

    String dfm = null;
    pos1 = elem.indexOf(DFM);
    if (pos1 > 0) {
      int pos2 = elem.indexOf("'", pos1 + DFM.length());
      if (pos2 > 0) {
        dfm = elem.substring(pos1 + DFM.length(), pos2);
      }
    }

    dcm = scanCollection(spec, f);
    if (dcm != null && dfm != null) {
      dcm.setDateExtractor(new DateExtractorFromName(dfm, false));
      f.format("dateFormatMark='%s' %n", dfm);
    }
    return dcm;
  }

  public void showCollection(Formatter f) {
    if (dcm == null)
      return;

    f.format("dcm = %s%n", dcm);
    for (MFile mfile : dcm.getFilesSorted()) {
      f.format("  %s%n", mfile.getPath());
    }
  }

  private MFileCollectionManager scanCollection(String spec, Formatter f) {
    MFileCollectionManager dc;
    try {
      dc = MFileCollectionManager.open(spec, spec, null, f);
      dc.scan(false);
      List<MFile> fileList = ImmutableList.copyOf(dc.getFilesSorted());

      List<Bean> beans = new ArrayList<>();
      for (MFile mfile : fileList)
        beans.add(new Bean(mfile));
      ftTable.setBeans(beans);
      return dc;

    } catch (Exception e) {
      StringWriter sw = new StringWriter(10000);
      e.printStackTrace(new PrintWriter(sw));
      f.format("Exception %s", sw.toString());
      return null;
    }
  }

  public class Bean {
    MFile mfile;

    Bean(MFile mfile) {
      this.mfile = mfile;
    }

    public String getName() {
      return mfile.getName();
    }

    public String getDate() {
      CalendarDate cd = dcm.extractDate(mfile);
      if (cd != null) {
        return cd.toString();
      } else {
        return "Unknown";
      }
    }
  }
}
