/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ucar.array.ArrayByte;
import ucar.array.ArrayType;
import ucar.array.ArrayVlen;
import ucar.array.Arrays;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.array.StructureDataStorageBB;
import ucar.array.StructureMembers;
import ucar.array.StructureMembers.Member;
import ucar.array.StructureMembers.MemberBuilder;
import ucar.gcdm.CdmrNetcdfProto.Data;
import ucar.gcdm.CdmrNetcdfProto.StructureDataProto;
import ucar.gcdm.CdmrNetcdfProto.StructureMemberProto;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** Convert between CdmRemote Protos and Netcdf objects, using ucar.ma2.Array for data. */
public class CdmrConverter {

  public static CdmrNetcdfProto.DataType convertDataType(ArrayType dtype) {
    switch (dtype) {
      case CHAR:
        return CdmrNetcdfProto.DataType.DATA_TYPE_CHAR;
      case BYTE:
        return CdmrNetcdfProto.DataType.DATA_TYPE_BYTE;
      case SHORT:
        return CdmrNetcdfProto.DataType.DATA_TYPE_SHORT;
      case INT:
        return CdmrNetcdfProto.DataType.DATA_TYPE_INT;
      case LONG:
        return CdmrNetcdfProto.DataType.DATA_TYPE_LONG;
      case FLOAT:
        return CdmrNetcdfProto.DataType.DATA_TYPE_FLOAT;
      case DOUBLE:
        return CdmrNetcdfProto.DataType.DATA_TYPE_DOUBLE;
      case STRING:
        return CdmrNetcdfProto.DataType.DATA_TYPE_STRING;
      case STRUCTURE:
        return CdmrNetcdfProto.DataType.DATA_TYPE_STRUCTURE;
      case SEQUENCE:
        return CdmrNetcdfProto.DataType.DATA_TYPE_SEQUENCE;
      case ENUM1:
        return CdmrNetcdfProto.DataType.DATA_TYPE_ENUM1;
      case ENUM2:
        return CdmrNetcdfProto.DataType.DATA_TYPE_ENUM2;
      case ENUM4:
        return CdmrNetcdfProto.DataType.DATA_TYPE_ENUM4;
      case OPAQUE:
        return CdmrNetcdfProto.DataType.DATA_TYPE_OPAQUE;
      case UBYTE:
        return CdmrNetcdfProto.DataType.DATA_TYPE_UBYTE;
      case USHORT:
        return CdmrNetcdfProto.DataType.DATA_TYPE_USHORT;
      case UINT:
        return CdmrNetcdfProto.DataType.DATA_TYPE_UINT;
      case ULONG:
        return CdmrNetcdfProto.DataType.DATA_TYPE_ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  public static ArrayType convertDataType(CdmrNetcdfProto.DataType dtype) {
    switch (dtype) {
      case DATA_TYPE_CHAR:
        return ArrayType.CHAR;
      case DATA_TYPE_BYTE:
        return ArrayType.BYTE;
      case DATA_TYPE_SHORT:
        return ArrayType.SHORT;
      case DATA_TYPE_INT:
        return ArrayType.INT;
      case DATA_TYPE_LONG:
        return ArrayType.LONG;
      case DATA_TYPE_FLOAT:
        return ArrayType.FLOAT;
      case DATA_TYPE_DOUBLE:
        return ArrayType.DOUBLE;
      case DATA_TYPE_STRING:
        return ArrayType.STRING;
      case DATA_TYPE_STRUCTURE:
        return ArrayType.STRUCTURE;
      case DATA_TYPE_SEQUENCE:
        return ArrayType.SEQUENCE;
      case DATA_TYPE_ENUM1:
        return ArrayType.ENUM1;
      case DATA_TYPE_ENUM2:
        return ArrayType.ENUM2;
      case DATA_TYPE_ENUM4:
        return ArrayType.ENUM4;
      case DATA_TYPE_OPAQUE:
        return ArrayType.OPAQUE;
      case DATA_TYPE_UBYTE:
        return ArrayType.UBYTE;
      case DATA_TYPE_USHORT:
        return ArrayType.USHORT;
      case DATA_TYPE_UINT:
        return ArrayType.UINT;
      case DATA_TYPE_ULONG:
        return ArrayType.ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  public static List<CdmrNetcdfProto.Attribute> encodeAttributes(AttributeContainer atts) {
    List<CdmrNetcdfProto.Attribute> result = new ArrayList<>();
    for (Attribute att : atts) {
      result.add(encodeAtt(att).build());
    }
    return result;
  }

  public static CdmrNetcdfProto.Group.Builder encodeGroup(Group g, int sizeToCache) throws IOException {
    CdmrNetcdfProto.Group.Builder groupBuilder = CdmrNetcdfProto.Group.newBuilder();
    groupBuilder.setName(g.getShortName());

    for (Dimension dim : g.getDimensions())
      groupBuilder.addDims(encodeDim(dim));

    for (Attribute att : g.attributes())
      groupBuilder.addAtts(encodeAtt(att));

    for (EnumTypedef enumType : g.getEnumTypedefs())
      groupBuilder.addEnumTypes(encodeEnumTypedef(enumType));

    for (Variable var : g.getVariables()) {
      if (var instanceof Structure)
        groupBuilder.addStructs(encodeStructure((Structure) var));
      else
        groupBuilder.addVars(encodeVar(var, sizeToCache));
    }

    for (Group ng : g.getGroups())
      groupBuilder.addGroups(encodeGroup(ng, sizeToCache));

    return groupBuilder;
  }

  public static CdmrNetcdfProto.Error encodeErrorMessage(String message) {
    CdmrNetcdfProto.Error.Builder builder = CdmrNetcdfProto.Error.newBuilder();
    builder.setMessage(message);
    return builder.build();
  }

  public static CdmrNetcdfProto.Section encodeSection(Section section) {
    CdmrNetcdfProto.Section.Builder sbuilder = CdmrNetcdfProto.Section.newBuilder();
    for (Range r : section.getRanges()) {
      CdmrNetcdfProto.Range.Builder rbuilder = CdmrNetcdfProto.Range.newBuilder();
      rbuilder.setStart(r.first());
      rbuilder.setSize(r.length());
      rbuilder.setStride(r.stride());
      sbuilder.addRange(rbuilder);
    }
    return sbuilder.build();
  }

  private static CdmrNetcdfProto.Attribute.Builder encodeAtt(Attribute att) {
    CdmrNetcdfProto.Attribute.Builder attBuilder = CdmrNetcdfProto.Attribute.newBuilder();
    attBuilder.setName(att.getShortName());
    attBuilder.setDataType(convertDataType(att.getDataType().getArrayType()));
    attBuilder.setLength(att.getLength());

    // values
    if (att.getLength() > 0) {
      if (att.isString()) {
        CdmrNetcdfProto.Data.Builder datab = CdmrNetcdfProto.Data.newBuilder();
        for (int i = 0; i < att.getLength(); i++) {
          datab.addSdata(att.getStringValue(i));
        }
        datab.setDataType(convertDataType(att.getArrayType()));
        attBuilder.setData(datab);
      } else {
        attBuilder.setData(encodePrimitiveData(att.getArrayType(), att.getArrayValues()));
      }
    }
    return attBuilder;
  }

  private static CdmrNetcdfProto.Dimension.Builder encodeDim(Dimension dim) {
    CdmrNetcdfProto.Dimension.Builder dimBuilder = CdmrNetcdfProto.Dimension.newBuilder();
    if (dim.getShortName() != null)
      dimBuilder.setName(dim.getShortName());
    if (!dim.isVariableLength())
      dimBuilder.setLength(dim.getLength());
    dimBuilder.setIsPrivate(!dim.isShared());
    dimBuilder.setIsVlen(dim.isVariableLength());
    dimBuilder.setIsUnlimited(dim.isUnlimited());
    return dimBuilder;
  }

  private static CdmrNetcdfProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) {
    CdmrNetcdfProto.EnumTypedef.Builder builder = CdmrNetcdfProto.EnumTypedef.newBuilder();

    builder.setName(enumType.getShortName());
    builder.setBaseType(convertDataType(enumType.getBaseType().getArrayType()));
    Map<Integer, String> map = enumType.getMap();
    CdmrNetcdfProto.EnumTypedef.EnumType.Builder b2 = CdmrNetcdfProto.EnumTypedef.EnumType.newBuilder();
    for (int code : map.keySet()) {
      b2.clear();
      b2.setCode(code);
      b2.setValue(map.get(code));
      builder.addMap(b2);
    }
    return builder;
  }

  private static CdmrNetcdfProto.Structure.Builder encodeStructure(Structure s) throws IOException {
    CdmrNetcdfProto.Structure.Builder builder = CdmrNetcdfProto.Structure.newBuilder();
    builder.setName(s.getShortName());
    builder.setDataType(convertDataType(s.getArrayType()));

    for (Dimension dim : s.getDimensions())
      builder.addShape(encodeDim(dim));

    for (Attribute att : s.attributes())
      builder.addAtts(encodeAtt(att));

    for (Variable v : s.getVariables()) {
      if (v instanceof Structure)
        builder.addStructs(CdmrConverter.encodeStructure((Structure) v));
      else
        builder.addVars(encodeVar(v, -1));
    }

    return builder;
  }


  private static CdmrNetcdfProto.Variable.Builder encodeVar(Variable var, int sizeToCache) throws IOException {
    CdmrNetcdfProto.Variable.Builder builder = CdmrNetcdfProto.Variable.newBuilder();
    builder.setName(var.getShortName());
    builder.setDataType(convertDataType(var.getArrayType()));
    if (var.getDataType().isEnum()) {
      EnumTypedef enumType = var.getEnumTypedef();
      if (enumType != null)
        builder.setEnumType(enumType.getShortName());
    }

    for (Dimension dim : var.getDimensions()) {
      builder.addShape(encodeDim(dim));
    }

    for (Attribute att : var.attributes()) {
      builder.addAtts(encodeAtt(att));
    }

    // put small amounts of data in header "immediate mode"
    if (var.isCaching() && var.getArrayType().isNumeric()) {
      if (var.isCoordinateVariable() || var.getSize() * var.getElementSize() < sizeToCache) {
        Array<?> data = var.readArray();
        builder.setData(encodeData(var.getArrayType(), data));
      }
    }
    return builder;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static CdmrNetcdfProto.Data encodeData(ArrayType dataType, Array<?> data) {
    if (data.isVlen()) {
      return encodeVlenData(dataType, (ArrayVlen) data);
    } else if (data instanceof StructureDataArray) {
      return encodeStructureDataArray(dataType, (StructureDataArray) data);
    } else {
      return encodePrimitiveData(dataType, data);
    }
  }

  private static void encodeShape(CdmrNetcdfProto.Data.Builder data, int[] shape) {
    for (int j : shape) {
      data.addShape(j);
    }
  }

  private static CdmrNetcdfProto.Data encodePrimitiveData(ArrayType dataType, Array<?> data) {
    CdmrNetcdfProto.Data.Builder builder = CdmrNetcdfProto.Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    encodeShape(builder, data.getShape());

    switch (dataType) {
      case OPAQUE:
      case ENUM1:
      case CHAR:
      case UBYTE:
      case BYTE: {
        ArrayByte bdata = (ArrayByte) data;
        builder.addBdata(bdata.getByteString());
        break;
      }
      case SHORT: {
        Array<Short> idata = (Array<Short>) data;
        idata.forEach(val -> builder.addIdata(val.intValue()));
        break;
      }
      case INT: {
        Array<Integer> idata = (Array<Integer>) data;
        idata.forEach(val -> builder.addIdata(val));
        break;
      }
      case ENUM2:
      case USHORT: {
        Array<Short> idata = (Array<Short>) data;
        idata.forEach(val -> builder.addUidata(val.intValue()));
        break;
      }
      case ENUM4:
      case UINT: {
        Array<Integer> idata = (Array<Integer>) data;
        idata.forEach(val -> builder.addUidata(val));
        break;
      }
      case LONG: {
        Array<Long> ldata = (Array<Long>) data;
        ldata.forEach(val -> builder.addLdata(val));
        break;
      }
      case ULONG: {
        Array<Long> ldata = (Array<Long>) data;
        ldata.forEach(val -> builder.addUldata(val));
        break;
      }
      case FLOAT: {
        Array<Float> fdata = (Array<Float>) data;
        fdata.forEach(val -> builder.addFdata(val));
        break;
      }
      case DOUBLE: {
        Array<Double> ddata = (Array<Double>) data;
        ddata.forEach(val -> builder.addDdata(val));
        break;
      }
      case STRING: {
        Array<String> sdata = (Array<String>) data;
        sdata.forEach(val -> builder.addSdata(val));
        break;
      }
      /*
       * case OPAQUE: {
       * // location: "cdm/core/src/test/data/hdf5/test_atomic_types.nc" variableSpec: "vo"
       * if (data instanceof ArrayByte) {
       * ArrayByte bdata = (ArrayByte) data;
       * builder.addBdata(bdata.getByteString());
       * } else {
       * Array<ByteBuffer> bdata = (Array<ByteBuffer>) data; // LOOK does this happen?
       * bdata.forEach(val -> builder.addBdata(ByteString.copyFrom(val.array())));
       * }
       * break;
       * }
       */
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
    return builder.build();
  }

  private static CdmrNetcdfProto.Data encodeStructureDataArray(ArrayType dataType, StructureDataArray arrayStructure) {
    CdmrNetcdfProto.Data.Builder builder = CdmrNetcdfProto.Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    encodeShape(builder, arrayStructure.getShape());
    builder.setMembers(encodeStructureMembers(arrayStructure.getStructureMembers()));

    // row oriented
    int count = 0;
    for (StructureData sdata : arrayStructure) {
      builder.addRows(encodeStructureData(sdata));
      count++;
    }
    return builder.build();
  }

  private static CdmrNetcdfProto.StructureDataProto encodeStructureData(StructureData structData) {
    CdmrNetcdfProto.StructureDataProto.Builder builder = CdmrNetcdfProto.StructureDataProto.newBuilder();
    int count = 0;
    for (Member member : structData.getStructureMembers()) {
      Array<?> data = structData.getMemberData(member);
      builder.addMemberData(encodeData(member.getArrayType(), data));
    }
    return builder.build();
  }

  private static CdmrNetcdfProto.StructureMembersProto encodeStructureMembers(StructureMembers members) {
    CdmrNetcdfProto.StructureMembersProto.Builder builder = CdmrNetcdfProto.StructureMembersProto.newBuilder();
    builder.setName(members.getName());
    for (Member member : members.getMembers()) {
      StructureMemberProto.Builder smBuilder = StructureMemberProto.newBuilder().setName(member.getName())
          .setDataType(convertDataType(member.getArrayType())).addAllShape(Ints.asList(member.getShape()));
      if (member.getStructureMembers() != null) {
        smBuilder.setMembers(encodeStructureMembers(member.getStructureMembers()));
      }
      builder.addMembers(smBuilder);
    }
    return builder.build();
  }

  private static CdmrNetcdfProto.Data encodeVlenData(ArrayType dataType, ArrayVlen<?> vlenarray) {
    CdmrNetcdfProto.Data.Builder builder = CdmrNetcdfProto.Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    encodeShape(builder, vlenarray.getShape());
    for (Array<?> one : vlenarray) {
      builder.addVlen(encodeData(vlenarray.getArrayType(), one));
    }
    return builder.build();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static AttributeContainer decodeAttributes(String name, List<CdmrNetcdfProto.Attribute> atts) {
    AttributeContainerMutable builder = new AttributeContainerMutable(name);
    for (CdmrNetcdfProto.Attribute att : atts) {
      builder.addAttribute(decodeAtt(att));
    }
    return builder.toImmutable();
  }

  public static void decodeGroup(CdmrNetcdfProto.Group proto, Group.Builder g) {
    for (CdmrNetcdfProto.Dimension dim : proto.getDimsList())
      g.addDimension(CdmrConverter.decodeDim(dim)); // always added to group? what if private ??

    for (CdmrNetcdfProto.Attribute att : proto.getAttsList())
      g.addAttribute(CdmrConverter.decodeAtt(att));

    for (CdmrNetcdfProto.EnumTypedef enumType : proto.getEnumTypesList())
      g.addEnumTypedef(CdmrConverter.decodeEnumTypedef(enumType));

    for (CdmrNetcdfProto.Variable var : proto.getVarsList())
      g.addVariable(CdmrConverter.decodeVar(var));

    for (CdmrNetcdfProto.Structure s : proto.getStructsList())
      g.addVariable(CdmrConverter.decodeStructure(s));

    for (CdmrNetcdfProto.Group gp : proto.getGroupsList()) {
      Group.Builder ng = Group.builder().setName(gp.getName());
      g.addGroup(ng);
      decodeGroup(gp, ng);
    }
  }

  private static Attribute decodeAtt(CdmrNetcdfProto.Attribute attp) {
    ArrayType dtUse = convertDataType(attp.getDataType());
    int len = attp.getLength();
    if (len == 0) { // deal with empty attribute
      return Attribute.builder(attp.getName()).setDataType(dtUse.getDataType()).build();
    }

    Array<?> attData = decodePrimitiveData(attp.getData());
    return Attribute.fromArray(attp.getName(), attData);
  }

  private static Dimension decodeDim(CdmrNetcdfProto.Dimension dim) {
    String name = (dim.getName().isEmpty() ? null : dim.getName());
    int dimLen = dim.getIsVlen() ? -1 : (int) dim.getLength();
    return Dimension.builder().setName(name).setIsShared(!dim.getIsPrivate()).setIsUnlimited(dim.getIsUnlimited())
        .setIsVariableLength(dim.getIsVlen()).setLength(dimLen).build();
  }

  private static EnumTypedef decodeEnumTypedef(CdmrNetcdfProto.EnumTypedef enumType) {
    List<CdmrNetcdfProto.EnumTypedef.EnumType> list = enumType.getMapList();
    Map<Integer, String> map = new HashMap<>(2 * list.size());
    for (CdmrNetcdfProto.EnumTypedef.EnumType et : list) {
      map.put(et.getCode(), et.getValue());
    }
    ArrayType basetype = convertDataType(enumType.getBaseType());
    return new EnumTypedef(enumType.getName(), map, basetype.getDataType());
  }

  private static Structure.Builder<?> decodeStructure(CdmrNetcdfProto.Structure s) {
    Structure.Builder<?> ncvar =
        (s.getDataType() == CdmrNetcdfProto.DataType.DATA_TYPE_SEQUENCE) ? Sequence.builder() : Structure.builder();

    ncvar.setName(s.getName()).setDataType(convertDataType(s.getDataType()).getDataType());

    List<Dimension> dims = new ArrayList<>(6);
    for (CdmrNetcdfProto.Dimension dim : s.getShapeList()) {
      dims.add(decodeDim(dim));
    }
    ncvar.addDimensions(dims);

    for (CdmrNetcdfProto.Attribute att : s.getAttsList()) {
      ncvar.addAttribute(decodeAtt(att));
    }

    for (CdmrNetcdfProto.Variable vp : s.getVarsList()) {
      ncvar.addMemberVariable(decodeVar(vp));
    }

    for (CdmrNetcdfProto.Structure sp : s.getStructsList()) {
      ncvar.addMemberVariable(decodeStructure(sp));
    }

    return ncvar;
  }

  public static Section decodeSection(CdmrNetcdfProto.Section proto) {
    Section.Builder section = Section.builder();

    for (CdmrNetcdfProto.Range pr : proto.getRangeList()) {
      try {
        long stride = pr.getStride();
        if (stride == 0) {
          stride = 1;
        }
        if (pr.getSize() == 0) {
          section.appendRange(Range.EMPTY); // used for scalars LOOK really used ??
        } else {
          // this.last = first + (this.length-1) * stride;
          section.appendRange((int) pr.getStart(), (int) (pr.getStart() + (pr.getSize() - 1) * stride), (int) stride);
        }

      } catch (InvalidRangeException e) {
        throw new RuntimeException("Bad Section in CdmRemote", e);
      }
    }
    return section.build();
  }

  public static ucar.ma2.Section decodeSection(CdmrNetcdfProto.Variable var) {
    ucar.ma2.Section.Builder section = ucar.ma2.Section.builder();
    for (CdmrNetcdfProto.Dimension dim : var.getShapeList()) {
      section.appendRange((int) dim.getLength());
    }
    return section.build();
  }

  private static int[] decodeShape(CdmrNetcdfProto.Data data) {
    int[] shape = new int[data.getShapeCount()];
    for (int i = 0; i < shape.length; i++) {
      shape[i] = data.getShape(i);
    }
    return shape;
  }


  private static Variable.Builder<?> decodeVar(CdmrNetcdfProto.Variable var) {
    ArrayType varType = convertDataType(var.getDataType());
    Variable.Builder<?> ncvar = Variable.builder().setName(var.getName()).setDataType(varType.getDataType());

    if (varType.isEnum()) {
      ncvar.setEnumTypeName(var.getEnumType());
    }

    // The Dimensions are stored redunantly in the Variable.
    // If shared, they must also exist in a parent Group. However, we dont yet have the Groups wired together,
    // so that has to wait until build().
    List<Dimension> dims = new ArrayList<>(6);
    Section.Builder section = Section.builder();
    for (CdmrNetcdfProto.Dimension dim : var.getShapeList()) {
      dims.add(decodeDim(dim));
      section.appendRange((int) dim.getLength());
    }
    ncvar.addDimensions(dims);

    for (CdmrNetcdfProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    if (var.hasData()) {
      Array<?> data = decodePrimitiveData(var.getData());
      ncvar.setSourceData(data);
    }

    return ncvar;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public static <T> Array<T> decodeData(CdmrNetcdfProto.Data data) {
    if (data.getVlenCount() > 0) {
      return (Array<T>) decodeVlenData(data);
    } else if (data.hasMembers()) {
      return (Array<T>) decodeStructureDataArray(data);
    } else {
      return decodePrimitiveData(data);
    }
  }

  private static <T> Array<T> decodePrimitiveData(CdmrNetcdfProto.Data data) {
    ArrayType dataType = convertDataType(data.getDataType());
    int[] shape = decodeShape(data);
    switch (dataType) {
      case CHAR:
      case OPAQUE:
      case ENUM1:
      case UBYTE:
      case BYTE: {
        byte[] array = data.getBdata(0).toByteArray();
        return Arrays.factory(dataType, shape, array);
      }
      case SHORT: {
        int i = 0;
        short[] array = new short[data.getIdataCount()];
        for (int val : data.getIdataList()) {
          array[i++] = (short) val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case INT: {
        int i = 0;
        int[] array = new int[data.getIdataCount()];
        for (int val : data.getIdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case ENUM2:
      case USHORT: {
        int i = 0;
        short[] array = new short[data.getUidataCount()];
        for (int val : data.getUidataList()) {
          array[i++] = (short) val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case ENUM4:
      case UINT: {
        int i = 0;
        int[] array = new int[data.getUidataCount()];
        for (int val : data.getUidataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case LONG: {
        int i = 0;
        long[] array = new long[data.getLdataCount()];
        for (long val : data.getLdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case ULONG: {
        int i = 0;
        long[] array = new long[data.getUldataCount()];
        for (long val : data.getUldataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case FLOAT: {
        int i = 0;
        float[] array = new float[data.getFdataCount()];
        for (float val : data.getFdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case DOUBLE: {
        int i = 0;
        double[] array = new double[data.getDdataCount()];
        for (double val : data.getDdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      case STRING: {
        int i = 0;
        String[] array = new String[data.getSdataCount()];
        for (String val : data.getSdataList()) {
          array[i++] = val;
        }
        return Arrays.factory(dataType, shape, array);
      }
      /*
       * case OPAQUE: {
       * if (data.getBdataCount() == 1) {
       * byte[] array = data.getBdata(0).toByteArray();
       * return Arrays.factory(dataType, shape, array);
       * }
       * // LOOK PROBABLY WRONG
       * int count = 0;
       * Object[] array = new Object[data.getBdataCount()];
       * for (ByteString val : data.getBdataList()) {
       * array[count++] = ByteBuffer.wrap(val.toByteArray());
       * }
       * return Arrays.factory(dataType, shape, array);
       * }
       */
      default:
        throw new IllegalStateException("Unknown datatype " + dataType);
    }
  }

  private static StructureDataArray decodeStructureDataArray(CdmrNetcdfProto.Data arrayStructureProto) {
    int nrows = arrayStructureProto.getRowsCount();
    int[] shape = decodeShape(arrayStructureProto);

    // ok to have nrows = 0
    Preconditions.checkArgument(Arrays.computeSize(shape) == nrows);

    StructureMembers members = decodeStructureMembers(arrayStructureProto.getMembers()).build();
    ByteBuffer bbuffer = ByteBuffer.allocate(nrows * members.getStorageSizeBytes());
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, nrows);
    int row = 0;
    for (CdmrNetcdfProto.StructureDataProto structProto : arrayStructureProto.getRowsList()) {
      decodeStructureData(structProto, members, storage, bbuffer, row);
      row++;
    }
    return new StructureDataArray(members, shape, storage);
  }

  private static StructureMembers.Builder decodeStructureMembers(CdmrNetcdfProto.StructureMembersProto membersProto) {
    StructureMembers.Builder membersb = StructureMembers.builder();
    membersb.setName(membersProto.getName());
    for (StructureMemberProto memberProto : membersProto.getMembersList()) {
      MemberBuilder mb = StructureMembers.memberBuilder();
      mb.setName(memberProto.getName());
      mb.setArrayType(convertDataType(memberProto.getDataType()));
      mb.setShape(Ints.toArray(memberProto.getShapeList()));
      if (memberProto.hasMembers()) {
        mb.setStructureMembers(decodeStructureMembers(memberProto.getMembers()));
      }
      membersb.addMember(mb);
    }
    membersb.setStandardOffsets(false);
    return membersb;
  }

  private static void decodeStructureData(CdmrNetcdfProto.StructureDataProto structDataProto, StructureMembers members,
      StructureDataStorageBB storage, ByteBuffer bbuffer, int rowidx) {
    for (int i = 0; i < structDataProto.getMemberDataCount(); i++) {
      Data data = structDataProto.getMemberData(i);
      Member member = members.getMember(i);
      int computed = members.getStorageSizeBytes() * rowidx + member.getOffset();
      bbuffer.position(computed);
      decodeNestedData(member, data, storage, bbuffer);
    }
  }

  private static void decodeNestedData(Member member, CdmrNetcdfProto.Data data, StructureDataStorageBB storage,
      ByteBuffer bb) {
    if (data.getVlenCount() > 0) {
      ArrayVlen<?> vlen = decodeVlenData(data);
      int index = storage.putOnHeap(vlen);
      bb.putInt(index);
      return;
    }

    ArrayType dataType = convertDataType(data.getDataType());
    switch (dataType) {
      case ENUM1:
      case UBYTE:
      case BYTE: {
        ByteString bs = data.getBdata(0);
        for (byte val : bs) {
          bb.put(val);
        }
        return;
      }
      case OPAQUE: {
        for (int i = 0; i < data.getBdataCount(); i++) {
          ByteString bs = data.getBdata(i);
          // LOOK cant count on these being the same size
        }
      }
      case CHAR: {
        for (int val : data.getIdataList()) {
          bb.put((byte) val);
        }
        return;
      }
      case SHORT: {
        for (int val : data.getIdataList()) {
          bb.putShort((short) val);
        }
        return;
      }
      case INT: {
        for (int val : data.getIdataList()) {
          bb.putInt(val);
        }
        return;
      }
      case ENUM2:
      case USHORT: {
        for (int val : data.getUidataList()) {
          bb.putShort((short) val);
        }
        return;
      }
      case ENUM4:
      case UINT: {
        for (int val : data.getUidataList()) {
          bb.putInt(val);
        }
        return;
      }
      case LONG: {
        for (long val : data.getLdataList()) {
          bb.putLong(val);
        }
        return;
      }
      case ULONG: {
        for (long val : data.getUldataList()) {
          bb.putLong(val);
        }
        return;
      }
      case FLOAT: {
        for (float val : data.getFdataList()) {
          bb.putFloat(val);
        }
        return;
      }
      case DOUBLE: {
        for (double val : data.getDdataList()) {
          bb.putDouble(val);
        }
        return;
      }
      case STRING: {
        String[] vals = new String[data.getSdataCount()];
        int idx = 0;
        for (String val : data.getSdataList()) {
          vals[idx++] = val;
        }
        int index = storage.putOnHeap(vals);
        bb.putInt(index);
        return;
      }
      case SEQUENCE: {
        StructureDataArray seqData = decodeStructureDataArray(data);
        int index = storage.putOnHeap(seqData);
        bb.putInt(index);
        return;
      }
      case STRUCTURE: {
        Preconditions.checkArgument(member.getStructureMembers() != null);
        decodeNestedStructureDataArray(member.getStructureMembers(), data.getRowsList(), storage, bb);
        return;
      }
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
  }

  private static void decodeNestedStructureDataArray(StructureMembers members, List<StructureDataProto> rows,
      StructureDataStorageBB storage, ByteBuffer bbuffer) {
    int offset = bbuffer.position();
    int rowidx = 0;
    for (CdmrNetcdfProto.StructureDataProto structProto : rows) {
      int memberIdx = 0;
      for (Member nestedMember : members) {
        int computed = offset + members.getStorageSizeBytes() * rowidx + nestedMember.getOffset();
        bbuffer.position(computed);
        decodeNestedData(nestedMember, structProto.getMemberData(memberIdx), storage, bbuffer);
        memberIdx++;
      }
      rowidx++;
    }
  }

  private static <T> ArrayVlen<T> decodeVlenData(CdmrNetcdfProto.Data data) {
    Preconditions.checkArgument(data.getVlenCount() > 0);
    int[] shape = decodeShape(data);
    int length = (int) Arrays.computeSize(shape);
    Preconditions.checkArgument(length == data.getVlenCount());

    ArrayType dataType = convertDataType(data.getDataType());
    ArrayVlen<T> result = ArrayVlen.factory(dataType, shape);

    for (int index = 0; index < length; index++) {
      Data inner = data.getVlen(index);
      result.set(index, decodePrimitiveData(inner));
    }
    return result;
  }
}
