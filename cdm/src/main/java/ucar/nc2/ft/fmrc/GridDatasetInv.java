/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.MCollection;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.nc2.constants._Coordinate;
import java.util.*;
import java.io.*;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import thredds.inventory.MFile;

/**
 * The data inventory of one GridDataset. Track grids, time, vert, ens coordinates. Grids are
 * grouped by the time coordinated that they use. Uses dense time, vert coordinates - just the ones that are in the
 * file.
 *
 * Note: Not sure if the vert coords will ever be different across the time coords.
 *
 * Should be immutable, once the file is finished writing.
 *
 * @author caron
 * @since Jan 11, 2010
 */
public class GridDatasetInv {
  private static final Logger logger = LoggerFactory.getLogger(GridDatasetInv.class);
  private static final int REQ_VERSION = 2; // minimum required version, else regenerate XML
  private static final int CURR_VERSION = 2; // current version

  // Cache the GridDatasetInv directly, not persisted to disk.
  // TODO: Add persistence if thats shown to be needed.
  private static Cache<String, GridDatasetInv> cache = CacheBuilder.newBuilder().maximumSize(100).build();

  public static GridDatasetInv open(MCollection cm, MFile mfile, Element ncml) throws IOException {
    try {
      return cache.get(mfile.getPath() + "#fmrInv.xml", new GenerateInv(cm, mfile, ncml));
    } catch (ExecutionException e) {
      throw new IOException("Cache failed", e);
    }
  }

  private static class GenerateInv implements Callable<GridDatasetInv> {
    private final MCollection cm;
    private final MFile mfile;
    private final Element ncml;

    GenerateInv(MCollection cm, MFile mfile, Element ncml) {
      this.cm = cm;
      this.mfile = mfile;
      this.ncml = ncml;
    }

    @Override
    public GridDatasetInv call() throws Exception {
      GridDataset gds = null;
      try {
        if (ncml == null) {
          gds = GridDataset.open(mfile.getPath());

        } else {
          NetcdfFile nc = NetcdfDataset.acquireFile(new DatasetUrl(null, mfile.getPath()), null);
          NetcdfDataset ncd = NcMLReader.mergeNcML(nc, ncml); // create new dataset
          ncd.enhance(); // now that the ncml is added, enhance "in place", ie modify the NetcdfDataset
          gds = new GridDataset(ncd);
        }

        return new GridDatasetInv(gds, cm.extractDate(mfile));
      } finally {
        if (gds != null) {
          gds.close();
        }
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  private String location;
  private int version;
  private final List<TimeCoord> times = new ArrayList<>(); // list of TimeCoord
  private final List<VertCoord> vaxes = new ArrayList<>(); // list of VertCoord
  private final List<EnsCoord> eaxes = new ArrayList<>(); // list of EnsCoord
  private CalendarDate runDate; // date of the run
  private String runTimeString; // string representation of the date of the run
  private Date lastModified;

  private GridDatasetInv() {}

  public GridDatasetInv(ucar.nc2.dt.grid.GridDataset gds, CalendarDate runDate) {
    this.location = gds.getLocation();
    this.runDate = runDate;

    NetcdfFile ncfile = gds.getNetcdfFile();
    if (ncfile != null && this.runDate == null) {
      runTimeString = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelBaseDate, null);
      if (runTimeString == null) {
        runTimeString = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelRunDate, null);
      }

      if (runTimeString != null) {
        this.runDate = DateUnit.parseCalendarDate(runTimeString);
      }

      if (this.runDate == null) {
        this.runDate = gds.getCalendarDateStart(); // LOOK not really right
        logger.warn("GridDatasetInv using gds.getStartDate() for run date = {} in {}", runTimeString, location);
      }
    }

    if (this.runDate == null) {
      throw new IllegalStateException("No run date");
    }

    this.runTimeString = this.runDate.toString();

    // add each variable, collect unique time and vertical axes
    for (GridDatatype gg : gds.getGrids()) {
      GridCoordSystem gcs = gg.getCoordinateSystem();
      Grid grid = new Grid(gg.getFullName());

      // LOOK: Note this assumes a dense coordinate system
      CoordinateAxis1DTime axis = gcs.getTimeAxis1D();
      if (axis != null) {
        TimeCoord tc = getTimeCoordinate(axis);
        tc.addGridInventory(grid);
        grid.tc = tc;
      }

      CoordinateAxis1D vaxis = gcs.getVerticalAxis();
      if (vaxis != null) {
        grid.vc = getVertCoordinate(vaxis);
      }

    }

    // assign sequence number
    int seqno = 0;
    for (TimeCoord tc : times) {
      tc.setId(seqno++);
    }

  }

  public String toString() {
    return location;
  }

  public String getLocation() {
    return location;
  }

  public long getLastModified() {
    return lastModified.getTime();
  }

  /**
   * Get the date of the ForecastModelRun
   *
   * @return the date of the ForecastModelRun
   */
  public CalendarDate getRunDate() {
    return runDate;
  }

  /**
   * Get string representation of the date of the ForecastModelRun
   *
   * @return string representation of the date of the ForecastModelRun
   */
  public String getRunDateString() {
    return runTimeString;
  }

  /**
   * Get a list of unique TimeCoords, which contain the list of variables that all use that
   * TimeCoord.
   *
   * @return list of TimeCoord
   */
  public List<TimeCoord> getTimeCoords() {
    return times;
  }

  /**
   * Get a list of unique VertCoords.
   *
   * @return list of VertCoord
   */
  public List<VertCoord> getVertCoords() {
    return vaxes;
  }

  public Grid findGrid(String name) {
    for (TimeCoord tc : times) {
      List<Grid> grids = tc.getGridInventory();
      for (Grid g : grids) {
        if (g.name.equals(name)) {
          return g;
        }
      }
    }
    return null;
  }

  private TimeCoord getTimeCoordinate(CoordinateAxis1DTime axis) {
    // check for same axis
    for (TimeCoord tc : times) {
      if (tc.getAxisName().equals(axis.getFullName())) {
        return tc;
      }
    }

    // check for same offsets
    TimeCoord want = new TimeCoord(runDate, axis);
    for (TimeCoord tc : times) {
      if ((tc.equalsData(want))) {
        return tc;
      }
    }

    // its a new one
    times.add(want);
    return want;
  }

  private Grid makeGrid(String gridName) {
    return new Grid(gridName);
  }

  /**
   * A Grid variable has a name, timeCoord and optionally a Vertical and Ensemble Coordinate
   */
  public class Grid implements Comparable<Grid> {
    final String name;
    TimeCoord tc = null; // time coordinates reletive to getRunDate()
    EnsCoord ec = null; // optional
    VertCoord vc = null; // optional

    private Grid(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public String getLocation() {
      return location;
    }

    public String getTimeCoordName() {
      return (tc == null) ? "" : tc.getName();
    }

    public String getVertCoordName() {
      return (vc == null) ? "" : vc.getName();
    }

    public int compareTo(Grid o) {
      return name.compareTo(o.name);
    }

    public int countTotal() {
      int ntimes = tc.getNCoords();
      return ntimes * getVertCoordLength();
    }

    public String toString() {
      return name;
    }

    public int getVertCoordLength() {
      return (vc == null) ? 1 : vc.getValues1().length;
    }

    public TimeCoord getTimeCoord() {
      return tc;
    }

    public GridDatasetInv getFile() {
      return GridDatasetInv.this;
    }
  }

  private VertCoord getVertCoordinate(int wantId) {
    if (wantId < 0) {
      return null;
    }
    for (VertCoord vc : vaxes) {
      if (vc.getId() == wantId) {
        return vc;
      }
    }
    return null;
  }

  private VertCoord getVertCoordinate(CoordinateAxis1D axis) {
    for (VertCoord vc : vaxes) {
      if (vc.getName().equals(axis.getFullName())) {
        return vc;
      }
    }

    VertCoord want = new VertCoord(axis);
    for (VertCoord vc : vaxes) {
      if ((vc.equalsData(want))) {
        return vc;
      }
    }

    // its a new one
    vaxes.add(want);
    return want;
  }

  private EnsCoord getEnsCoordinate(int ens_id) {
    if (ens_id < 0) {
      return null;
    }
    for (EnsCoord ec : eaxes) {
      if ((ec.getId() == ens_id)) {
        return ec;
      }
    }
    return null;
  }

  //////////////////////////////////////////////////////////////

  /**
   * Write the XML representation to a String.
   *
   * @return the XML representation to a String.
   */
  public String writeXML(Date lastModified) {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(writeDocument(lastModified));
  }

  /**
   * Create the XML representation of the GridDatasetInv
   *
   * @return the XML representation as a Document
   */
  Document writeDocument(Date lastModified) {
    Element rootElem = new Element("gridInventory");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", location);
    rootElem.setAttribute("runTime", runTimeString);
    if (lastModified != null) {
      rootElem.setAttribute("lastModified", CalendarDateFormatter.toDateTimeString(lastModified));
    }
    rootElem.setAttribute("version", Integer.toString(CURR_VERSION));

    // list all the vertical coords
    Collections.sort(vaxes);
    int count = 0;
    for (VertCoord vc : vaxes) {
      vc.setId(count++);
      Element vcElem = new Element("vertCoord");
      rootElem.addContent(vcElem);
      vcElem.setAttribute("id", Integer.toString(vc.getId()));
      vcElem.setAttribute("name", vc.getName());
      if (vc.getUnits() != null) {
        vcElem.setAttribute(CDM.UNITS, vc.getUnits());
      }

      StringBuilder sbuff = new StringBuilder();
      double[] values1 = vc.getValues1();
      double[] values2 = vc.getValues2();
      for (int j = 0; j < values1.length; j++) {
        if (j > 0) {
          sbuff.append(" ");
        }
        sbuff.append(values1[j]);
        if (values2 != null) {
          sbuff.append(",");
          sbuff.append(values2[j]);
        }
      }
      vcElem.addContent(sbuff.toString());
    }

    // list all the time coords
    count = 0;
    for (TimeCoord tc : times) {
      tc.setId(count++);
      Element timeElement = new Element("timeCoord");
      rootElem.addContent(timeElement);
      timeElement.setAttribute("id", Integer.toString(tc.getId()));
      timeElement.setAttribute("name", tc.getName());
      timeElement.setAttribute("isInterval", tc.isInterval() ? "true" : "false");

      Formatter sbuff = new Formatter();
      if (tc.isInterval()) {
        double[] bound1 = tc.getBound1();
        double[] bound2 = tc.getBound2();
        for (int j = 0; j < bound1.length; j++) {
          sbuff.format((Locale) null, "%f %f,", bound1[j], bound2[j]);
        }

      } else {
        for (double offset : tc.getOffsetTimes()) {
          sbuff.format((Locale) null, "%f,", offset);
        }
      }
      timeElement.addContent(sbuff.toString());

      List<GridDatasetInv.Grid> vars = tc.getGridInventory();
      Collections.sort(vars);
      for (Grid grid : vars) {
        Element varElem = new Element("grid");
        timeElement.addContent(varElem);
        varElem.setAttribute("name", grid.name);
        if (grid.ec != null) {
          varElem.setAttribute("ens_id", Integer.toString(grid.ec.getId()));
        }
        if (grid.vc != null) {
          varElem.setAttribute("vert_id", Integer.toString(grid.vc.getId()));
        }
      }
    }

    return doc;
  }

  /**
   * Construct a GridDatasetInv from its XML representation
   *
   * @param xmlString the xml string
   * @return ForecastModelRun
   * @throws IOException on io error
   */
  private static GridDatasetInv readXML(byte[] xmlString) throws IOException {
    InputStream is = new BufferedInputStream(new ByteArrayInputStream(xmlString));
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage() + " reading from XML ");
    }

    Element rootElem = doc.getRootElement();
    GridDatasetInv fmr = new GridDatasetInv();
    fmr.runTimeString = rootElem.getAttributeValue("runTime");
    fmr.location = rootElem.getAttributeValue("location");
    if (fmr.location == null) {
      fmr.location = rootElem.getAttributeValue("name"); // old way
    }
    String lastModifiedS = rootElem.getAttributeValue("lastModified");
    if (lastModifiedS != null) {
      fmr.lastModified = CalendarDateFormatter.isoStringToDate(lastModifiedS);
    }
    String version = rootElem.getAttributeValue("version");
    fmr.version = (version == null) ? 0 : Integer.parseInt(version);
    if (fmr.version < REQ_VERSION) {
      return fmr;
    }

    fmr.runDate = DateUnit.parseCalendarDate(fmr.runTimeString);

    java.util.List<Element> vList = rootElem.getChildren("vertCoord");
    for (Element vertElem : vList) {
      VertCoord vc = new VertCoord();
      fmr.vaxes.add(vc);
      vc.setId(Integer.parseInt(vertElem.getAttributeValue("id")));
      vc.setName(vertElem.getAttributeValue("name"));
      vc.setUnits(vertElem.getAttributeValue(CDM.UNITS));

      // parse the values
      String values = vertElem.getTextNormalize();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      double[] values1 = new double[n];
      double[] values2 = null;
      int count = 0;
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        int pos = toke.indexOf(',');
        if (pos < 0) {
          values1[count] = Double.parseDouble(toke);
        } else {
          if (values2 == null) {
            values2 = new double[n];
          }
          String val1 = toke.substring(0, pos);
          String val2 = toke.substring(pos + 1);
          values1[count] = Double.parseDouble(val1);
          values2[count] = Double.parseDouble(val2);
        }
        count++;
      }
      vc.setValues1(values1);
      vc.setValues2(values2);
    }

    java.util.List<Element> tList = rootElem.getChildren("timeCoord");
    for (Element timeElem : tList) {
      TimeCoord tc = new TimeCoord(fmr.runDate);
      fmr.times.add(tc);
      tc.setId(Integer.parseInt(timeElem.getAttributeValue("id")));
      String s = timeElem.getAttributeValue("isInterval");
      boolean isInterval = (s != null) && (s.equals("true"));

      if (isInterval) {
        String boundsAll = timeElem.getTextNormalize();
        String[] bounds = boundsAll.split(",");
        int n = bounds.length;
        double[] bound1 = new double[n];
        double[] bound2 = new double[n];
        int count = 0;
        for (String b : bounds) {
          String[] value = b.split(" ");
          bound1[count] = Double.parseDouble(value[0]);
          bound2[count] = Double.parseDouble(value[1]);
          count++;
        }
        tc.setBounds(bound1, bound2);

      } else {
        String values = timeElem.getTextNormalize();
        String[] value = values.split(",");
        int n = value.length;
        double[] offsets = new double[n];
        int count = 0;
        for (String v : value) {
          offsets[count++] = Double.parseDouble(v);
        }
        tc.setOffsetTimes(offsets);
      }

      // get the variable names
      List<Element> varList = timeElem.getChildren("grid");
      for (Element vElem : varList) {
        Grid grid = fmr.makeGrid(vElem.getAttributeValue("name"));
        if (vElem.getAttributeValue("ens_id") != null) {
          grid.ec = fmr.getEnsCoordinate(Integer.parseInt(vElem.getAttributeValue("ens_id")));
        }
        if (vElem.getAttributeValue("vert_id") != null) {
          grid.vc = fmr.getVertCoordinate(Integer.parseInt(vElem.getAttributeValue("vert_id")));
        }
        tc.addGridInventory(grid);
        grid.tc = tc;
      }
    }

    return fmr;
  }

}
