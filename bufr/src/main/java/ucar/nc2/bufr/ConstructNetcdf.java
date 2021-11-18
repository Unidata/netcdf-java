/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr;

import ucar.array.ArrayType;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.bufr.tables.CodeFlagTables;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import java.util.*;

/** Construction of the Netcdf object. */
class ConstructNetcdf {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConstructNetcdf.class);
  private static final boolean warnUnits = false;

  private NetcdfFile ncfile;
  private Sequence obsStructure;

  private Group.Builder rootGroup;
  private Sequence.Builder recordb;
  private int centerId;
  private Formatter coordinates = new Formatter();

  ConstructNetcdf(Message proto, BufrConfig bufrConfig, String location) {
    this.rootGroup = Group.builder();

    // dkeyRoot = dds.getDescriptorRoot();
    // int nbits = dds.getTotalBits();
    // int inputBytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;
    // int outputBytes = dds.getTotalBytes();

    // the category
    // int cat = proto.ids.getCategory();
    // int subcat = proto.ids.getSubCategory();

    // global Attributes
    rootGroup.addAttribute(new Attribute(CDM.HISTORY, "Read using CDM BufrIosp2"));
    if (bufrConfig.getFeatureType() != null) {
      rootGroup.addAttribute(new Attribute(CF.FEATURE_TYPE, bufrConfig.getFeatureType().toString()));
    }
    rootGroup.addAttribute(new Attribute("location", location));

    rootGroup.addAttribute(new Attribute("BUFR:categoryName", proto.getLookup().getCategoryName()));
    rootGroup.addAttribute(new Attribute("BUFR:subCategoryName", proto.getLookup().getSubCategoryName()));
    rootGroup.addAttribute(new Attribute("BUFR:centerName", proto.getLookup().getCenterName()));
    rootGroup.addAttribute(new Attribute("BUFR:category", proto.ids.getCategory()));
    rootGroup.addAttribute(new Attribute("BUFR:subCategory", proto.ids.getSubCategory()));
    rootGroup.addAttribute(new Attribute("BUFR:localSubCategory", proto.ids.getLocalSubCategory()));
    rootGroup.addAttribute(new Attribute(BufrIosp.centerId, proto.ids.getCenterId()));
    rootGroup.addAttribute(new Attribute("BUFR:subCenter", proto.ids.getSubCenterId()));
    // ncfile.addAttribute("BUFR:tableName", proto.ids.getMasterTableFilename()));
    rootGroup.addAttribute(new Attribute("BUFR:table", proto.ids.getMasterTableId()));
    rootGroup.addAttribute(new Attribute("BUFR:tableVersion", proto.ids.getMasterTableVersion()));
    rootGroup.addAttribute(new Attribute("BUFR:localTableVersion", proto.ids.getLocalTableVersion()));
    rootGroup.addAttribute(new Attribute("Conventions", "BUFR/CDM"));
    rootGroup.addAttribute(new Attribute("BUFR:edition", proto.is.getBufrEdition()));

    centerId = proto.ids.getCenterId();

    String header = proto.getHeader();
    if (header != null && !header.isEmpty())
      rootGroup.addAttribute(new Attribute("WMO_Header", header));

    makeObsRecord(bufrConfig);
    String coordS = coordinates.toString();
    if (!coordS.isEmpty())
      recordb.addAttribute(new Attribute("coordinates", coordS));

    this.ncfile = NetcdfFile.builder().setRootGroup(rootGroup).setLocation(location).build();
    this.obsStructure = (Sequence) this.ncfile.findVariable(BufrIosp.obsRecordName);
  }

  NetcdfFile getNetcdfFile() {
    return this.ncfile;
  }

  Sequence getObsStructure() {
    return this.obsStructure;
  }

  private void makeObsRecord(BufrConfig bufrConfig) {
    recordb = Sequence.builder().setName(BufrIosp.obsRecordName);
    rootGroup.addVariable(recordb);

    BufrConfig.FieldConverter root = bufrConfig.getRootConverter();
    for (BufrConfig.FieldConverter fld : root.flds) {
      DataDescriptor dkey = fld.dds;
      if (!dkey.isOkForVariable())
        continue;

      if (dkey.replication == 0) {
        addSequence(recordb, fld);

      } else if (dkey.replication > 1) {

        List<BufrConfig.FieldConverter> subFlds = fld.flds;
        List<DataDescriptor> subKeys = dkey.subKeys;
        if (subKeys.size() == 1) { // only one member
          DataDescriptor subDds = dkey.subKeys.get(0);
          BufrConfig.FieldConverter subFld = subFlds.get(0);
          if (subDds.dpi != null) {
            addDpiStructure(recordb, fld, subFld);

          } else if (subDds.replication == 1) { // one member not a replication
            addVariable(recordb, subFld, dkey.replication).setSPobject(fld); // set the replicating field as SPI object

          } else { // one member is a replication (two replications in a row)
            addStructure(recordb, fld, dkey.replication);
          }
        } else if (subKeys.size() > 1) {
          addStructure(recordb, fld, dkey.replication);
        }

      } else { // replication == 1
        addVariable(recordb, fld, dkey.replication);
      }
    }
  }

  private void addStructure(Structure.Builder<?> parent, BufrConfig.FieldConverter fld, int count) {
    DataDescriptor dkey = fld.dds;
    String uname = findUniqueName(parent, fld.getName(), "struct");
    dkey.name = uname; // name may need to be changed for uniqueness

    // String structName = dataDesc.name != null ? dataDesc.name : "struct" + structNum++;
    Structure.Builder struct = Structure.builder().setName(uname).setDimensionsAnonymous(new int[] {count}); // anon
                                                                                                             // vector

    for (BufrConfig.FieldConverter subKey : fld.flds) {
      addMember(struct, subKey);
    }

    parent.addMemberVariable(struct);
    struct.setSPobject(fld);
  }

  private void addSequence(Structure.Builder<?> parent, BufrConfig.FieldConverter fld) {
    DataDescriptor dkey = fld.dds;
    String uname = findUniqueName(parent, fld.getName(), "seq");
    dkey.name = uname; // name may need to be changed for uniqueness

    // String seqName = ftype == (FeatureType.STATION_PROFILE) ? "profile" : "seq";
    // String seqName = dataDesc.name != null ? dataDesc.name : "seq" + seqNum++;

    Sequence.Builder<?> seq = Sequence.builder().setName(uname);
    for (BufrConfig.FieldConverter subKey : fld.flds) {
      addMember(seq, subKey);
    }
    parent.addMemberVariable(seq);
    seq.setSPobject(fld);

    // LOOK EmbeddedTable needs to be able to read the first message with a built Sequence.
    // SO we create a temporary one as a workaround.
    Sequence.Builder<?> seqTemp = Sequence.builder().setName(uname);
    for (BufrConfig.FieldConverter subKey : fld.flds) {
      addMember(seqTemp, subKey);
    }
    seqTemp.setSPobject(fld);

    dkey.refersTo = seqTemp.build(Group.builder().build());
    dkey.refersToName = seq.shortName;
  }

  private void addMember(Structure.Builder<?> parent, BufrConfig.FieldConverter fld) {
    DataDescriptor dkey = fld.dds;

    if (dkey.replication == 0)
      addSequence(parent, fld);

    else if (dkey.replication > 1) {
      List<DataDescriptor> subKeys = dkey.subKeys;
      if (subKeys.size() == 1) {
        BufrConfig.FieldConverter subFld = fld.flds.get(0);
        Variable.Builder v = addVariable(parent, subFld, dkey.replication);
        v.setSPobject(fld); // set the replicating field as SPI object

      } else {
        addStructure(parent, fld, dkey.replication);
      }

    } else {
      addVariable(parent, fld, dkey.replication);
    }
  }

  private void addDpiStructure(Sequence.Builder<?> parent, BufrConfig.FieldConverter parentFld,
      BufrConfig.FieldConverter dpiField) {
    DataDescriptor dpiKey = dpiField.dds;
    String uname = findUniqueName(parent, dpiField.getName(), "struct");
    dpiKey.name = uname; // name may need to be changed for uniqueness

    // String structName = findUnique(parent, dpiField.name);
    Structure.Builder<?> struct = Structure.builder().setName(uname);
    int n = parentFld.dds.replication;
    struct.setDimensionsAnonymous(new int[] {n}); // anon vector

    Variable.Builder<?> v = Variable.builder().setName("name").setArrayType(ArrayType.STRING); // scalar
    struct.addMemberVariable(v);

    v = Variable.builder().setName("data").setArrayType(ArrayType.FLOAT); // scalar
    struct.addMemberVariable(v);

    parent.addMemberVariable(struct);
    struct.setSPobject(dpiField); // ??

    // add some fake dkeys corresponding to above
    // DataDescriptor nameDD = new DataDescriptor();
  }

  private void addDpiSequence(Sequence.Builder<?> parent, BufrConfig.FieldConverter fld) {
    Structure.Builder<?> struct = Structure.builder().setName("statistics");
    struct.setDimensionsAnonymous(new int[] {fld.dds.replication}); // scalar

    Variable.Builder v = Variable.builder().setName("name").setArrayType(ArrayType.STRING); // scalar
    struct.addMemberVariable(v);

    v = Variable.builder().setName("data").setArrayType(ArrayType.FLOAT);
    struct.addMemberVariable(v);

    parent.addMemberVariable(struct);
  }

  private Variable.Builder<?> addVariable(Structure.Builder<?> struct, BufrConfig.FieldConverter fld, int count) {
    DataDescriptor dkey = fld.dds;
    String uname = findGloballyUniqueName(fld.getName(), "unknown");
    dkey.name = uname; // name may need to be changed for uniqueness

    Variable.Builder v = Variable.builder().setName(uname);
    if (count > 1) {
      v.setDimensionsAnonymous(new int[] {count}); // anon vector
    }

    if (fld.getDesc() != null) {
      v.addAttribute(new Attribute(CDM.LONG_NAME, fld.getDesc()));
    }

    if (fld.getUnits() == null) {
      if (warnUnits)
        log.warn("dataDesc.units == null for " + uname);
    } else {
      String units = fld.getUnits();
      if (DataDescriptor.isCodeTableUnit(units)) {
        v.addAttribute(new Attribute(CDM.UNITS, "CodeTable " + fld.dds.getFxyName()));
      } else if (DataDescriptor.isFlagTableUnit(units)) {
        v.addAttribute(new Attribute(CDM.UNITS, "FlagTable " + fld.dds.getFxyName()));
      } else if (!DataDescriptor.isInternationalAlphabetUnit(units) && !units.startsWith("Numeric")) {
        v.addAttribute(new Attribute(CDM.UNITS, units));
      }
    }

    DataDescriptor dataDesc = fld.dds;
    if (dataDesc.type == 1) {
      v.setArrayType(ArrayType.CHAR);
      int size = dataDesc.bitWidth / 8;
      v.setDimensionsAnonymous(new int[] {size});

    } else if ((dataDesc.type == 2) && CodeFlagTables.hasTable(dataDesc.fxy)) { // enum
      int nbits = dataDesc.bitWidth;
      int nbytes = (nbits % 8 == 0) ? nbits / 8 : nbits / 8 + 1;

      CodeFlagTables ct = CodeFlagTables.getTable(dataDesc.fxy);
      if (nbytes == 1)
        v.setArrayType(ArrayType.ENUM1);
      else if (nbytes == 2)
        v.setArrayType(ArrayType.ENUM2);
      else if (nbytes == 4)
        v.setArrayType(ArrayType.ENUM4);

      // v.removeAttribute(CDM.UNITS);
      v.addAttribute(new Attribute("BUFR:CodeTable", ct.getName() + " (" + dataDesc.getFxyName() + ")"));

      rootGroup.findOrAddEnumTypedef(ct.getName(), ct.getMap());
      v.setEnumTypeName(ct.getName());

    } else {
      int nbits = dataDesc.bitWidth;
      // use of unsigned seems fishy, since only time it uses high bit is for missing
      // not necessarily true, just when they "add one bit" to deal with missing case
      if (nbits < 9) {
        v.setArrayType(ArrayType.BYTE);
        if (nbits == 8) {
          v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, (short) BufrNumbers.missingValue(nbits)));
        } else
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, (byte) BufrNumbers.missingValue(nbits)));

      } else if (nbits < 17) {
        v.setArrayType(ArrayType.SHORT);
        if (nbits == 16) {
          v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, (int) BufrNumbers.missingValue(nbits)));
        } else
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, (short) BufrNumbers.missingValue(nbits)));

      } else if (nbits < 33) {
        v.setArrayType(ArrayType.INT);
        if (nbits == 32) {
          v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, (int) BufrNumbers.missingValue(nbits)));
        } else
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, (int) BufrNumbers.missingValue(nbits)));

      } else {
        v.setArrayType(ArrayType.LONG);
        v.addAttribute(new Attribute(CDM.MISSING_VALUE, BufrNumbers.missingValue(nbits)));
      }

      // value = scale_factor * packed + add_offset
      // bpacked = (value * 10^scale - refVal)
      // (bpacked + refVal) / 10^scale = value
      // value = bpacked * 10^-scale + refVal * 10^-scale
      // scale_factor = 10^-scale
      // add_ofset = refVal * 10^-scale
      int scale10 = dataDesc.scale;
      double scale = (scale10 == 0) ? 1.0 : Math.pow(10.0, -scale10);
      if (scale10 != 0)
        v.addAttribute(new Attribute(CDM.SCALE_FACTOR, (float) scale));
      if (dataDesc.refVal != 0)
        v.addAttribute(new Attribute(CDM.ADD_OFFSET, (float) scale * dataDesc.refVal));

    }

    annotate(v, fld);
    v.addAttribute(new Attribute(BufrIosp.fxyAttName, dataDesc.getFxyName()));
    v.addAttribute(new Attribute("BUFR:bitWidth", dataDesc.bitWidth));
    struct.addMemberVariable(v);

    v.setSPobject(fld);
    return v;
  }


  private int tempNo = 1;

  private String findUniqueName(Structure.Builder<?> struct, String want, String def) {
    if (want == null)
      return def + tempNo++;

    String vwant = NetcdfFiles.makeValidCdmObjectName(want);
    Optional<Variable.Builder<?>> oldV = struct.findMemberVariable(vwant);
    if (!oldV.isPresent())
      return vwant;

    int seq = 2;
    while (true) {
      String wantSeq = vwant + "-" + seq;
      oldV = struct.findMemberVariable(wantSeq);
      if (!oldV.isPresent())
        return wantSeq;
      seq++;
    }
  }

  // force globally unique variable names, even when they are in different Structures.
  // this allows us to promote structure members without worrying about name collisions
  private Map<String, Integer> names = new HashMap<>(100);

  private String findGloballyUniqueName(String want, String def) {
    if (want == null)
      return def + tempNo++;

    String vwant = NetcdfFiles.makeValidCdmObjectName(want);
    Integer have = names.get(vwant);
    if (have == null) {
      names.put(vwant, 1);
      return vwant;
    } else {
      have = have + 1;
      String wantSeq = vwant + "-" + have;
      names.put(vwant, have);
      return wantSeq;
    }
  }


  private void annotate(Variable.Builder v, BufrConfig.FieldConverter fld) {
    if (fld.type == null)
      return;

    switch (fld.type) {
      case lat:
        v.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        coordinates.format("%s ", v.shortName);
        break;

      case lon:
        v.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
        coordinates.format("%s ", v.shortName);
        break;

      case height:
      case heightOfStation:
      case heightAboveStation:
        v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
        coordinates.format("%s ", v.shortName);
        break;

      case stationId:
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_ID));
        break;

      case wmoId:
        v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.STATION_WMOID));
        break;
    }

  }

}
