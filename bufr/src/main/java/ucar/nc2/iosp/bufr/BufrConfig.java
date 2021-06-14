/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.bufr;

import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.point.bufr.BufrCdmIndexProto;
import ucar.nc2.ft.point.bufr.BufrField;
import ucar.nc2.ft.point.bufr.StandardFields;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.Station;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;
import java.util.*;

/**
 * Configuration for converting BUFR files to CDM
 * DataDescriptor tree becomes FieldConverter tree with annotations.
 * 
 * @author caron
 * @since 8/8/13
 */
public class BufrConfig {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrConfig.class);

  public static BufrConfig scanEntireFile(RandomAccessFile raf) {
    return new BufrConfig(raf);
  }

  static BufrConfig openFromMessage(RandomAccessFile raf, Message m, Element iospParam) throws IOException {
    BufrConfig config = new BufrConfig(raf, m);
    if (iospParam != null)
      config.merge(iospParam);
    return config;
  }

  private String filename;
  private StandardFields.StandardFieldsFromMessage standardFields;
  private FieldConverter rootConverter;
  private int messHash;
  private FeatureType featureType;
  private Map<String, StationCheck> map;
  private long start = Long.MAX_VALUE;
  private long end = Long.MIN_VALUE;

  private BufrConfig(RandomAccessFile raf) {
    this.filename = raf.getLocation();
    try {
      scanBufrFile(raf);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  private BufrConfig(RandomAccessFile raf, Message m) {
    this.filename = raf.getLocation();
    this.messHash = m.hashCode();
    this.rootConverter = new FieldConverter(m.ids.getCenterId(), m.getRootDataDescriptor());
    standardFields = StandardFields.extract(m);
  }

  public String getFilename() {
    return filename;
  }

  public FieldConverter getRootConverter() {
    return rootConverter;
  }

  public Map<String, StationCheck> getStationMap() {
    return map;
  }

  public int getMessHash() {
    return messHash;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  public FieldConverter getStandardField(BufrCdmIndexProto.FldType want) {
    for (FieldConverter fld : rootConverter.flds)
      if (fld.type == want)
        return fld;
    return null;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public long getNobs() {
    return countObs;
  }

  ////////////////////////////////////////////////////////////////////////////

  private void merge(Element iospParam) {
    assert iospParam.getName().equals("iospParam");
    Element bufr2nc = iospParam.getChild("bufr2nc", Catalog.ncmlNS);
    if (bufr2nc == null)
      return;
    for (Element child : bufr2nc.getChildren("fld", Catalog.ncmlNS))
      merge(child, rootConverter);
  }

  private void merge(Element jdom, FieldConverter parent) {
    if (jdom == null || parent == null)
      return;

    FieldConverter fld = null;

    // find the corresponding field
    String idxName = jdom.getAttributeValue("idx");
    if (idxName != null) {
      try {
        int idx = Integer.parseInt(idxName);
        fld = parent.getChild(idx);
      } catch (NumberFormatException ne) {
        log.info("BufrConfig cant find Child member index={} for file = {}", idxName, filename);
      }
    }

    if (fld == null) {
      String fxyName = jdom.getAttributeValue("fxy");
      if (fxyName != null) {
        fld = parent.findChildByFxyName(fxyName);
        if (fld == null) {
          log.info("BufrConfig cant find Child member fxy={} for file = {}", fxyName, filename);
        }
      }
    }

    if (fld == null) {
      String name = jdom.getAttributeValue("name");
      if (name != null) {
        fld = parent.findChild(name);
        if (fld == null) {
          log.info("BufrConfig cant find Child member name={} for file = {}", name, filename);
        }
      }
    }

    if (fld == null) {
      log.info("BufrConfig must have idx, name or fxy attribute = {} for file = {}", jdom, filename);
      return;
    }

    String action = jdom.getAttributeValue("action");
    if (action != null && !action.isEmpty())
      fld.setAction(action);

    if (jdom.getChildren("fld") != null) {
      for (Element child : jdom.getChildren("fld", Catalog.ncmlNS)) {
        merge(child, fld);
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  private StandardFields.StandardFieldsFromStructure extract;
  private boolean hasStations;
  private boolean hasDate;
  private int countObs;

  private void scanBufrFile(RandomAccessFile raf) throws Exception {
    NetcdfFile ncd = null;
    countObs = 0;

    try {
      MessageScanner scanner = new MessageScanner(raf);
      Message protoMessage = scanner.getFirstDataMessage();
      if (protoMessage == null)
        throw new IOException("No message found!");

      messHash = protoMessage.hashCode();
      standardFields = StandardFields.extract(protoMessage);
      rootConverter = new FieldConverter(protoMessage.ids.getCenterId(), protoMessage.getRootDataDescriptor());

      if (standardFields.hasStation()) {
        hasStations = true;
        map = new HashMap<>(1000);
      }
      featureType = guessFeatureType(standardFields);
      hasDate = standardFields.hasTime();

      ncd = NetcdfFiles.open(raf.getLocation()); // LOOK opening another raf
      Attribute centerAtt = ncd.findAttribute(BufrIosp.centerId);
      int center = (centerAtt == null) ? 0 : centerAtt.getNumericValue().intValue();

      Sequence seq = (Sequence) ncd.getRootGroup().findVariableLocal(BufrIosp.obsRecordName);
      extract = new StandardFields.StandardFieldsFromStructure(center, seq);

      StructureDataIterator iter = seq.getStructureIterator();
      processSeq(iter, rootConverter, true);

      setStandardActions(rootConverter);

    } finally {
      if (ncd != null)
        ncd.close();
    }
  }

  private FeatureType guessFeatureType(StandardFields.StandardFieldsFromMessage standardFields) {
    if (standardFields.hasStation())
      return FeatureType.STATION;
    if (standardFields.hasTime())
      return FeatureType.POINT;
    return FeatureType.ANY;
  }

  private void setStandardActions(FieldConverter fld) {
    fld.setAction(fld.makeAction());
    if (fld.flds == null)
      return;
    for (FieldConverter child : fld.flds)
      setStandardActions(child);
  }

  /////////////////////////////////////////////////////////////////////////////////////

  private CalendarDate today = CalendarDate.present();

  private void processSeq(StructureDataIterator sdataIter, FieldConverter parent, boolean isTop) throws IOException {
    try {
      while (sdataIter.hasNext()) {
        StructureData sdata = sdataIter.next();

        if (isTop) {
          countObs++;

          if (hasStations)
            processStations(parent, sdata);
          if (hasDate) {
            extract.extract(sdata);
            CalendarDate date = extract.makeCalendarDate();
            if (Math.abs(date.getDifferenceInMsecs(today)) > 1000L * 3600 * 24 * 100) {
              extract.makeCalendarDate();
            }
            long msecs = date.getMillisFromEpoch();
            if (this.start > msecs) {
              this.start = msecs;
            }
            if (this.end < msecs) {
              this.end = msecs;
            }
          }
        }

        int count = 0;
        for (StructureMembers.Member m : sdata.getMembers()) {
          if (m.getDataType() == DataType.SEQUENCE) {
            FieldConverter fld = parent.getChild(count);
            ArraySequence data = (ArraySequence) sdata.getArray(m);
            int n = data.getStructureDataCount();
            fld.trackSeqCounts(n);
            processSeq(data.getStructureDataIterator(), fld, false);
          }
          count++;
        }
      }
    } finally {
      sdataIter.close();
    }
  }

  private void processStations(FieldConverter parent, StructureData sdata) {
    Station station = readBufrStation(sdata);

    StationCheck check = map.get(station.getName());
    if (check == null)
      map.put(station.getName(), new StationCheck(station));
    else {
      check.count++;
      if (!station.equals(check))
        log.warn("bad station doesnt equal " + station + " != " + check);
    }
  }

  public class StationCheck implements Comparable<StationCheck> {
    public Station s;
    public int count;

    StationCheck(Station s) {
      this.s = s;
      this.count = 0;
    }

    @Override
    public int compareTo(StationCheck o) {
      return s.getName().compareTo(o.s.getName());
    }
  }

  Station readBufrStation(StructureData sdata) {
    extract.extract(sdata);

    Station.Builder builder = Station.builder(extract.getStationId());
    builder.setLatitude(extract.getFieldValueD(BufrCdmIndexProto.FldType.lat));
    builder.setLongitude(extract.getFieldValueD(BufrCdmIndexProto.FldType.lon));
    if (extract.hasField(BufrCdmIndexProto.FldType.stationDesc))
      builder.setDescription(extract.getFieldValueS(BufrCdmIndexProto.FldType.stationDesc));
    if (extract.hasField(BufrCdmIndexProto.FldType.wmoId))
      builder.setWmoId(extract.getFieldValueS(BufrCdmIndexProto.FldType.wmoId));
    if (extract.hasField(BufrCdmIndexProto.FldType.heightOfStation))
      builder.setAltitude(extract.getFieldValueD(BufrCdmIndexProto.FldType.heightOfStation));

    return builder.build();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class FieldConverter implements BufrField {
    DataDescriptor dds;
    List<FieldConverter> flds;
    BufrCdmIndexProto.FldType type;
    BufrCdmIndexProto.FldAction action;
    int min = Integer.MAX_VALUE;
    int max;
    boolean isSeq;

    private FieldConverter(int center, DataDescriptor dds) {
      this.dds = dds;
      this.type = StandardFields.findField(center, dds.getFxyName());

      if (dds.getSubKeys() != null) {
        this.flds = new ArrayList<>(dds.getSubKeys().size());
        for (DataDescriptor subdds : dds.getSubKeys()) {
          FieldConverter subfld = new FieldConverter(center, subdds);
          flds.add(subfld);
        }
      }
    }

    public String getName() {
      return dds.getName();
    }

    public String getDesc() {
      return dds.getDesc();
    }

    public String getUnits() {
      return dds.getUnits();
    }

    public short getFxy() {
      return dds.getFxy();
    }

    public String getFxyName() {
      return dds.getFxyName();
    }

    public BufrCdmIndexProto.FldAction getAction() {
      return action;
    }

    public BufrCdmIndexProto.FldType getType() {
      return type;
    }

    public List<FieldConverter> getChildren() {
      return flds;
    }

    public boolean isSeq() {
      return isSeq;
    }

    public int getMin() {
      return min;
    }

    public int getMax() {
      return max;
    }

    public int getScale() {
      return dds.getScale();
    }

    public int getReference() {
      return dds.getRefVal();
    }

    public int getBitWidth() {
      return dds.getBitWidth();
    }

    public void setAction(String action) {
      try {
        this.action = BufrCdmIndexProto.FldAction.valueOf(action);
      } catch (Exception e) {
        log.warn("Unknown action {}", action);
      }
    }

    public void setAction(BufrCdmIndexProto.FldAction action) {
      this.action = action;
    }

    FieldConverter findChild(String want) {
      for (FieldConverter child : flds) {
        String name = child.dds.getName();
        if (name != null && name.equals(want))
          return child;
      }
      return null;
    }

    FieldConverter findChildByFxyName(String fxyName) {
      for (FieldConverter child : flds) {
        String name = child.dds.getFxyName();
        if (name != null && name.equals(fxyName))
          return child;
      }
      return null;
    }

    FieldConverter getChild(int i) {
      return flds.get(i);
    }

    void trackSeqCounts(int n) {
      isSeq = true;
      if (n > max)
        max = n;
      if (n < min)
        min = n;
    }

    void showRange(Formatter f) {
      if (!isSeq)
        return;
      if (max == min)
        f.format(" isConstant='%d'", max);
      else if (max < 2)
        f.format(" isBinary='true'");
      else
        f.format(" range='[%d,%d]'", min, max);
    }

    BufrCdmIndexProto.FldAction makeAction() {
      if (!isSeq)
        return null;
      if (max == 0)
        return BufrCdmIndexProto.FldAction.remove;
      if (max < 2)
        return BufrCdmIndexProto.FldAction.asMissing;
      else
        return BufrCdmIndexProto.FldAction.asArray;
    }

    void show(Formatter f, Indent indent, int index) {
      boolean hasContent = false;
      if (isSeq)
        f.format("%s<fld idx='%d' name='%s'", indent, index, dds.getName());
      else
        f.format("%s<fld idx='%d' fxy='%s' name='%s' desc='%s' units='%s' bits='%d'", indent, index, dds.getFxyName(),
            dds.getName(), dds.getDesc(), dds.getUnits(), dds.getBitWidth());

      if (type != null)
        f.format(" type='%s'", type);
      showRange(f);
      f.format(" action='%s'", makeAction());

      /*
       * if (type != null) {
       * f.format(">%n");
       * indent.incr();
       * f.format("%s<type>%s</type>%n", indent, type);
       * indent.decr();
       * hasContent = true;
       * }
       */

      if (flds != null) {
        f.format(">%n");
        indent.incr();
        int subidx = 0;
        for (FieldConverter cc : flds) {
          cc.show(f, indent, subidx++);
        }
        indent.decr();
        hasContent = true;
      }

      if (hasContent)
        f.format("%s</fld>%n", indent);
      else
        f.format(" />%n");
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////


  public void show(Formatter out) {
    if (standardFields != null)
      out.format("Standard Fields%n%s%n%n", standardFields);

    Indent indent = new Indent(2);
    out.format("<bufr2nc location='%s' hash='%s' featureType='%s'>%n", filename, Integer.toHexString(messHash),
        featureType);
    indent.incr();
    int index = 0;
    for (FieldConverter fld : rootConverter.flds) {
      fld.show(out, indent, index++);
    }
    indent.decr();
    out.format("</bufr2nc>%n");
  }
}
