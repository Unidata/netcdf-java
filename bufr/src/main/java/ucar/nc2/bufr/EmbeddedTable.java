/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.StructureData;
import ucar.array.StructureMembers;
import ucar.nc2.*;
import ucar.nc2.bufr.tables.TableB;
import ucar.nc2.bufr.tables.TableD;
import ucar.nc2.bufr.tables.WmoXmlReader;
import ucar.nc2.internal.wmo.Util;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * BUFR allows you to encode a BUFR table in BUFR.
 * if table is embedded, all entries must be from it
 * LOOK: may be NCEP specific ?
 */
public class EmbeddedTable implements EmbeddedTableIF {
  private static final boolean showB = false;
  private static final boolean showD = false;

  private final RandomAccessFile raf;
  private final BufrIdentificationSection ids;

  private final List<Message> messages = new ArrayList<>();
  private boolean tableRead;
  private final TableB tableB;
  private final TableD tableD;
  private Structure seq2, seq3, seq4;
  private TableLookup tlookup;

  EmbeddedTable(Message m, RandomAccessFile raf) {
    this.raf = raf;
    this.ids = m.ids;
    tableB = new TableB("embed", raf.getLocation());
    tableD = new TableD("embed", raf.getLocation());
  }

  @Override
  public void addTable(Message m) {
    messages.add(m);
  }

  private void read2() throws IOException {
    Message proto = messages.get(0);
    BufrConfig config = BufrConfig.openFromMessage(raf, proto, null);
    ConstructNetcdf construct = new ConstructNetcdf(proto, config, raf.getLocation());

    Sequence obs = construct.getObsStructure();
    seq2 = (Structure) obs.findVariable("seq2");
    seq3 = (Structure) obs.findVariable("seq3");
    seq4 = (Structure) seq3.findVariable("seq4");

    // read all the messages
    Array<StructureData> data;
    Formatter f = new Formatter();
    for (Message m : messages) {
      if (m.dds.isCompressed()) {
        MessageArrayCompressedReader comp = new MessageArrayCompressedReader(obs, proto, m, raf, f);
        data = comp.readEntireMessage();
      } else {
        MessageArrayUncompressedReader uncomp = new MessageArrayUncompressedReader(obs, proto, m, raf, f);
        data = uncomp.readEntireMessage();
      }
      for (StructureData sdata : data) {
        add(sdata);
      }
    }
  }

  private void add(StructureData data) throws IOException {
    for (StructureMembers.Member m : data.getStructureMembers()) {
      if (showB)
        System.out.printf("%s%n", m);
      if (m.getArrayType() == ArrayType.SEQUENCE) {
        if (m.getName().equals("seq2")) {
          Array<StructureData> seqData = (Array<StructureData>) data.getMemberData(m);
          for (StructureData sdata : seqData) {
            addTableEntryB(sdata);
          }
        } else if (m.getName().equals("seq3")) {
          Array<StructureData> seqData = (Array<StructureData>) data.getMemberData(m);
          for (StructureData sdata : seqData) {
            addTableEntryD(sdata);
          }
        }
      }
    }
  }

  private void addTableEntryB(StructureData sdata) {
    String name = "", units = "", signScale = null, signRef = null;
    int scale = 0, refVal = 0, width = 0;
    short x1 = 0, y1 = 0;
    StructureMembers members = sdata.getStructureMembers();
    List<Variable> vars = seq2.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = vars.get(i);
      StructureMembers.Member m = members.getMember(i);
      Array<?> data = sdata.getMemberData(m);
      String memberData = Arrays.makeStringFromChar((Array<Byte>) data);
      if (showB) {
        System.out.printf("%s == %s%n", v, memberData);
      }

      Attribute att = v.attributes().findAttribute(BufrIosp.fxyAttName);
      switch (att.getStringValue()) {
        case "0-0-10":
          break;
        case "0-0-11":
          x1 = Short.parseShort(memberData.trim());
          break;
        case "0-0-12":
          y1 = Short.parseShort(memberData.trim());
          break;
        case "0-0-13":
          name = memberData;
          break;
        case "0-0-14":
          name += memberData; // append both lines
          break;
        case "0-0-15":
          units = WmoXmlReader.cleanUnit(memberData.trim());
          break;
        case "0-0-16":
          signScale = memberData.trim();
          break;
        case "0-0-17":
          scale = Integer.parseInt(memberData.trim());
          break;
        case "0-0-18":
          signRef = memberData.trim();
          break;
        case "0-0-19":
          refVal = Integer.parseInt(memberData.trim());
          break;
        case "0-0-20":
          width = Integer.parseInt(memberData.trim());
          break;
      }
    }
    if (showB)
      System.out.printf("%n");

    // split name and description from appended line 1 and 2
    String desc = null;
    name = name.trim();
    int pos = name.indexOf(' ');
    if (pos > 0) {
      desc = Util.cleanName(name.substring(pos + 1));
      name = name.substring(0, pos);
      name = Util.cleanName(name);
    }

    if ("-".equals(signScale))
      scale = -1 * scale;
    if ("-".equals(signRef))
      refVal = -1 * refVal;

    tableB.addDescriptor(x1, y1, scale, refVal, width, name, units, desc);
  }

  private void addTableEntryD(StructureData sdata) throws IOException {
    String name = null;
    short x1 = 0, y1 = 0;
    List<Short> dds = null;

    StructureMembers members = sdata.getStructureMembers();
    List<Variable> vars = seq3.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = vars.get(i);
      StructureMembers.Member m = members.getMember(i);
      if (m.getName().equals("seq4")) {
        dds = getDescriptors((Array<StructureData>) sdata.getMemberData(m));
        continue;
      }

      Array<?> data = sdata.getMemberData(m);
      String memberData = null;
      if (data.getArrayType() == ArrayType.CHAR) {
        memberData = Arrays.makeStringFromChar((Array<Byte>) data);
      }

      Attribute att = v.attributes().findAttribute(BufrIosp.fxyAttName);
      if (att != null) {
        if (showD)
          System.out.printf("%s == %s%n", v, memberData);
        switch (att.getStringValue()) {
          case "0-0-10":
            break;
          case "0-0-11":
            x1 = Short.parseShort(memberData.trim());
            break;
          case "0-0-12":
            y1 = Short.parseShort(memberData.trim());
            break;
          case "2-5-64":
            name = memberData;
            break;
        }
      }
    }
    if (showD) {
      System.out.printf("%n");
    }

    name = Util.cleanName(name);
    tableD.addDescriptor(x1, y1, name, dds);
  }

  private List<Short> getDescriptors(Array<StructureData> seqdata) {
    List<Short> list = new ArrayList<>();
    String fxyS = null;
    List<Variable> vars = seq4.getVariables();

    for (StructureData sdata : seqdata) {
      StructureMembers members = sdata.getStructureMembers();
      for (int i = 0; i < vars.size(); i++) {
        Variable v = vars.get(i);
        StructureMembers.Member m = members.getMember(i);
        Array<?> data = sdata.getMemberData(m);
        String memberData = Arrays.makeStringFromChar((Array<Byte>) data);
        if (showD)
          System.out.printf("%s == %s%n", v, memberData);

        Attribute att = v.attributes().findAttribute(BufrIosp.fxyAttName);
        if (att != null && att.getStringValue().equals("0-0-30"))
          fxyS = memberData;
      }
      if (showD)
        System.out.printf("%n");

      if (fxyS != null) {
        short id = Descriptor.getFxy2(fxyS);
        list.add(id);
      }
    }
    return list;
  }

  @Override
  public TableLookup getTableLookup() throws IOException {
    if (!tableRead) {
      read2();
      tableRead = true;
      tlookup = new TableLookup(ids, tableB, tableD);
    }
    return tlookup;
  }

}
