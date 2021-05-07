/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.write.NcmlWriter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/** generate capabilities XML for a FeatureDatasetPoint / StationTimeSeriesFeatureCollection */
public class FeatureDatasetCapabilitiesWriter {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureDatasetCapabilitiesWriter.class);

  private final FeatureDatasetPoint fdp;
  private final String path;

  public FeatureDatasetCapabilitiesWriter(FeatureDatasetPoint fdp, String path) {
    this.fdp = fdp;
    this.path = path;
  }

  public String getCapabilities() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(getCapabilitiesDocument());
  }

  public void getCapabilities(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(getCapabilitiesDocument(), os);
  }

  /**
   * Create an XML document for the stations in this dataset, possible subsetted by bb.
   * Must be a station dataset.
   *
   * @param bb restrict stations to this bounding box, may be null
   * @param names restrict stations to these names, may be null
   * @return XML document for the stations
   */
  public Document makeStationCollectionDocument(LatLonRect bb, String[] names) {
    List<DsgFeatureCollection> list = fdp.getPointFeatureCollectionList();
    DsgFeatureCollection fc = list.get(0); // LOOK maybe should pass in the dsg?

    if (!(fc instanceof StationTimeSeriesFeatureCollection)) {
      throw new UnsupportedOperationException(fc.getClass().getName() + " not a StationTimeSeriesFeatureCollection");
    }
    StationTimeSeriesFeatureCollection sobs = (StationTimeSeriesFeatureCollection) fc;

    Element rootElem = new Element("stationCollection");
    Document doc = new Document(rootElem);

    List<StationFeature> stations;
    if (bb != null)
      stations = sobs.getStationFeatures(bb);
    else if (names != null)
      stations = sobs.getStationFeatures(Arrays.asList(names));
    else
      stations = sobs.getStationFeatures();

    for (StationFeature sf : stations) {
      Station stn = sf.getStation();
      Element sElem = new Element("station");
      sElem.setAttribute("name", stn.getName());
      if (stn.getWmoId() != null)
        sElem.setAttribute("wmo_id", stn.getWmoId());
      if ((stn.getDescription() != null) && (!stn.getDescription().isEmpty()))
        sElem.addContent(new Element("description").addContent(stn.getDescription()));

      sElem.addContent(new Element("longitude").addContent(Double.toString(stn.getLongitude())));
      sElem.addContent(new Element("latitide").addContent(Double.toString(stn.getLatitude())));
      if (!Double.isNaN(stn.getAltitude()))
        sElem.addContent(new Element("altitude").addContent(Double.toString(stn.getAltitude())));
      rootElem.addContent(sElem);
    }

    return doc;
  }

  /**
   * Create the capabilities XML document for this dataset
   *
   * @return capabilities XML document
   */
  public Document getCapabilitiesDocument() {
    Element rootElem = new Element("capabilities");
    Document doc = new Document(rootElem);
    if (null != path) {
      rootElem.setAttribute("location", path);
      Element elem = new Element("featureDataset");
      FeatureType ft = fdp.getFeatureType();
      elem.setAttribute("type", ft.toString().toLowerCase());
      String url = path.replace("dataset.xml", ft.toString().toLowerCase() + ".xml");
      elem.setAttribute("url", url);
      rootElem.addContent(elem);
    }

    List<DsgFeatureCollection> list = fdp.getPointFeatureCollectionList();
    DsgFeatureCollection fc = list.get(0); // LOOK maybe should pass in the dsg?

    rootElem.addContent(writeTimeUnit(fc.getTimeUnit()));
    rootElem.addContent(new Element("AltitudeUnits").addContent(fc.getAltUnits()));

    // data variables
    List<? extends VariableSimpleIF> vars = fdp.getDataVariables();
    Collections.sort(vars);
    for (VariableSimpleIF v : vars) {
      rootElem.addContent(writeVariable(v));
    }

    LatLonRect bb = fc.getBoundingBox();
    if (bb != null)
      rootElem.addContent(writeBoundingBox(bb));

    // add date range
    CalendarDateRange dateRange = fc.getCalendarDateRange();
    if (dateRange != null) {
      Element drElem = new Element("TimeSpan"); // from KML
      drElem.addContent(new Element("begin").addContent(dateRange.getStart().toString()));
      drElem.addContent(new Element("end").addContent(dateRange.getEnd().toString()));
      if (dateRange.getResolution() != null)
        drElem.addContent(new Element("resolution").addContent(dateRange.getResolution().toString()));

      rootElem.addContent(drElem);
    }

    return doc;
  }

  private Element writeTimeUnit(CalendarDateUnit dateUnit) {
    Element elem = new Element("TimeUnit");
    elem.addContent(dateUnit.getUdUnit());
    elem.setAttribute("calendar", dateUnit.getCalendar().toString());
    return elem;
  }

  private Element writeBoundingBox(LatLonRect bb) {
    // extend the bbox to make sure the implicit rounding does not result in a bbox that does not contain
    // any points (can happen when you have a single station with very precise lat/lon values)
    // See https://github.com/Unidata/thredds/issues/470
    // This accounts for the implicit rounding errors that result from the use of
    // ucar.unidata.util.Format.dfrac when writing out the lat/lon box on the NCSS for Points dataset.html
    // page
    int decToKeep = 6;
    double bbExpand = Math.pow(10, -decToKeep);
    LatLonRect bbextend = bb.toBuilder().expand(bbExpand).build();

    Element bbElem = new Element("LatLonBox"); // from KML

    bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(bbextend.getLonMin(), decToKeep)));
    bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(bbextend.getLonMax(), decToKeep)));
    bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(bbextend.getLatMin(), decToKeep)));
    bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(bbextend.getLatMax(), decToKeep)));
    return bbElem;
  }

  private Element writeVariable(VariableSimpleIF v) {
    NcmlWriter ncMLWriter = new NcmlWriter();
    Element varElem = new Element("variable");
    varElem.setAttribute("name", v.getShortName());

    DataType dt = v.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

    // attributes
    for (Attribute att : v.attributes()) {
      varElem.addContent(ncMLWriter.makeAttributeElement(att));
    }

    return varElem;
  }

  /////////////////////////////////////////////
  //

  public Document readCapabilitiesDocument(InputStream in) throws JDOMException, IOException {
    SAXBuilder builder = new SAXBuilder();
    return builder.build(in);
  }

  public static LatLonRect getSpatialExtent(Document doc) {
    Element root = doc.getRootElement();
    Element latlonBox = root.getChild("LatLonBox");
    if (latlonBox == null)
      return null;

    String westS = latlonBox.getChildText("west");
    String eastS = latlonBox.getChildText("east");
    String northS = latlonBox.getChildText("north");
    String southS = latlonBox.getChildText("south");
    if ((westS == null) || (eastS == null) || (northS == null) || (southS == null))
      return null;

    try {
      double west = Double.parseDouble(westS);
      double east = Double.parseDouble(eastS);
      double south = Double.parseDouble(southS);
      double north = Double.parseDouble(northS);
      return new LatLonRect(south, east, north, west);

    } catch (Exception e) {
      return null;
    }
  }

  public static CalendarDateRange getTimeSpan(Document doc) {
    Element root = doc.getRootElement();
    Element timeSpan = root.getChild("TimeSpan");
    if (timeSpan == null)
      return null;

    String beginS = timeSpan.getChildText("begin");
    String endS = timeSpan.getChildText("end");
    // String resS = timeSpan.getChildText("resolution");
    if ((beginS == null) || (endS == null))
      return null;

    try {
      CalendarDate start = CalendarDateFormatter.isoStringToCalendarDate(null, beginS);
      CalendarDate end = CalendarDateFormatter.isoStringToCalendarDate(null, endS);
      if ((start == null) || (end == null)) {
        return null;
      }

      CalendarDateRange dr = CalendarDateRange.of(start, end);

      // LOOK if (resS != null)
      // dr.setResolution(new TimeDuration(resS));

      return dr;

    } catch (Exception e) {
      return null;
    }
  }

  public static CalendarDateUnit getTimeUnit(Document doc) {
    Element root = doc.getRootElement();
    Element timeUnitE = root.getChild("TimeUnit");
    if (timeUnitE == null)
      return null;

    String cal = timeUnitE.getAttributeValue("calendar");
    String timeUnitS = timeUnitE.getTextNormalize();

    try {
      return CalendarDateUnit.of(cal, timeUnitS);
    } catch (Exception e) {
      log.error("Illegal date unit {} in FeatureDatasetCapabilitiesXML", timeUnitS);
      return null;
    }
  }

  public static String getAltUnits(Document doc) {
    Element root = doc.getRootElement();
    String altUnits = root.getChildText("AltitudeUnits");
    if (altUnits == null || altUnits.isEmpty())
      return null;
    return altUnits;
  }

  public static List<VariableSimpleIF> getDataVariables(Document doc) {
    Element root = doc.getRootElement();

    List<VariableSimpleIF> dataVars = new ArrayList<>();
    List<Element> varElems = root.getChildren("variable");
    for (Element varElem : varElems) {
      dataVars.add(new VariableSimpleAdapter(varElem));
    }
    return dataVars;
  }

  private static class VariableSimpleAdapter implements VariableSimpleIF {
    String name, desc, units;
    DataType dt;
    List<Attribute> atts;

    VariableSimpleAdapter(Element velem) {
      name = velem.getAttributeValue("name");
      String type = velem.getAttributeValue("type");
      dt = DataType.getType(type);

      atts = new ArrayList<>();
      List<Element> attElems = velem.getChildren("attribute");
      for (Element attElem : attElems) {
        String attName = attElem.getAttributeValue("name");
        Array<?> values = NcmlReader.readAttributeValues(attElem);
        atts.add(Attribute.fromArray(attName, values));
      }

      for (Attribute att : atts) {
        if (att.getShortName().equals(CDM.UNITS))
          units = att.getStringValue();
        if (att.getShortName().equals(CDM.LONG_NAME))
          desc = att.getStringValue();
        if ((desc == null) && att.getShortName().equals("description"))
          desc = att.getStringValue();
        if ((desc == null) && att.getShortName().equals("standard_name"))
          desc = att.getStringValue();
      }
    }

    public String getName() {
      return name;
    }

    @Override
    public String getFullName() {
      return name;
    }

    @Override
    public String getShortName() {
      return name;
    }

    @Override
    public String getDescription() {
      return desc;
    }

    @Override
    public String getUnitsString() {
      return units;
    }

    @Override
    public int getRank() {
      return 0;
    }

    @Override
    public int[] getShape() {
      return new int[0];
    }

    @Override
    public ImmutableList<Dimension> getDimensions() {
      return null;
    }

    @Override
    public DataType getDataType() {
      return dt;
    }

    @Override
    public ArrayType getArrayType() {
      return dt.getArrayType();
    }

    @Override
    public AttributeContainer attributes() {
      return new AttributeContainerMutable(name, atts).toImmutable();
    }

    @Override
    public int compareTo(VariableSimpleIF o) {
      return name.compareTo(o.getShortName()); // ??
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      VariableSimpleAdapter that = (VariableSimpleAdapter) o;
      return name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }


}


