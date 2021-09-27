/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static ucar.nc2.iosp.IospHelper.convertByteToChar;
import static ucar.nc2.iosp.IospHelper.convertCharToByte;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import ucar.array.ArrayType;
import ucar.gcdm.GcdmNetcdfProto.Data;
import ucar.gcdm.GcdmNetcdfProto.StructureMemberProto;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** Convert between Gcdm Protos and Netcdf objects, using ucar.ma2.Array for data. */
public class GcdmConverterMa2 {
  public static GcdmNetcdfProto.Group.Builder encodeGroup(Group g, int sizeToCache) throws IOException {
    GcdmNetcdfProto.Group.Builder groupBuilder = GcdmNetcdfProto.Group.newBuilder();
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

  public static GcdmNetcdfProto.Attribute.Builder encodeAtt(Attribute att) {
    GcdmNetcdfProto.Attribute.Builder attBuilder = GcdmNetcdfProto.Attribute.newBuilder();
    attBuilder.setName(att.getShortName());
    attBuilder.setDataType(convertDataType(att.getDataType()));
    attBuilder.setLength(att.getLength());

    // values
    if (att.getLength() > 0) {
      if (att.isString()) {
        Data.Builder datab = Data.newBuilder();
        for (int i = 0; i < att.getLength(); i++) {
          datab.addSdata(att.getStringValue(i));
        }
        datab.setDataType(convertDataType(att.getDataType()));
        attBuilder.setData(datab);

      } else {
        attBuilder.setData(encodeData(att.getDataType(), att.getValues()));
      }
    }

    return attBuilder;
  }

  private static GcdmNetcdfProto.Dimension.Builder encodeDim(Dimension dim) {
    GcdmNetcdfProto.Dimension.Builder dimBuilder = GcdmNetcdfProto.Dimension.newBuilder();
    if (dim.getShortName() != null)
      dimBuilder.setName(dim.getShortName());
    if (!dim.isVariableLength())
      dimBuilder.setLength(dim.getLength());
    dimBuilder.setIsPrivate(!dim.isShared());
    dimBuilder.setIsVlen(dim.isVariableLength());
    dimBuilder.setIsUnlimited(dim.isUnlimited());
    return dimBuilder;
  }

  private static GcdmNetcdfProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) {
    GcdmNetcdfProto.EnumTypedef.Builder builder = GcdmNetcdfProto.EnumTypedef.newBuilder();

    builder.setName(enumType.getShortName());
    builder.setBaseType(convertDataType(enumType.getBaseArrayType().getDataType()));
    Map<Integer, String> map = enumType.getMap();
    GcdmNetcdfProto.EnumTypedef.EnumType.Builder b2 = GcdmNetcdfProto.EnumTypedef.EnumType.newBuilder();
    for (int code : map.keySet()) {
      b2.clear();
      b2.setCode(code);
      b2.setValue(map.get(code));
      builder.addMaps(b2);
    }
    return builder;
  }

  private static GcdmNetcdfProto.Variable.Builder encodeVar(Variable var, int sizeToCache) throws IOException {
    GcdmNetcdfProto.Variable.Builder builder = GcdmNetcdfProto.Variable.newBuilder();
    builder.setName(var.getShortName());
    builder.setDataType(convertDataType(var.getDataType()));
    if (var.getDataType().isEnum()) {
      EnumTypedef enumType = var.getEnumTypedef();
      if (enumType != null)
        builder.setEnumType(enumType.getShortName());
    }

    for (Dimension dim : var.getDimensions()) {
      builder.addShapes(encodeDim(dim));
    }

    for (Attribute att : var.attributes()) {
      builder.addAtts(encodeAtt(att));
    }

    // put small amounts of data in header "immediate mode"
    if (var.isCaching() && var.getDataType().isNumeric()) {
      if (var.isCoordinateVariable() || var.getSize() * var.getElementSize() < sizeToCache) {
        Array data = var.read();
        builder.setData(encodeData(var.getDataType(), data));
      }
    }

    return builder;
  }

  private static GcdmNetcdfProto.Structure.Builder encodeStructure(Structure s) throws IOException {
    GcdmNetcdfProto.Structure.Builder builder = GcdmNetcdfProto.Structure.newBuilder();
    builder.setName(s.getShortName());
    builder.setDataType(convertDataType(s.getDataType()));

    for (Dimension dim : s.getDimensions())
      builder.addShapes(encodeDim(dim));

    for (Attribute att : s.attributes())
      builder.addAtts(encodeAtt(att));

    for (Variable v : s.getVariables()) {
      if (v instanceof Structure)
        builder.addStructs(GcdmConverterMa2.encodeStructure((Structure) v));
      else
        builder.addVars(encodeVar(v, -1));
    }

    return builder;
  }

  public static GcdmNetcdfProto.Error encodeErrorMessage(String message) {
    GcdmNetcdfProto.Error.Builder builder = GcdmNetcdfProto.Error.newBuilder();
    builder.setMessage(message);
    return builder.build();
  }

  public static Data encodeData(DataType dataType, Array data) {
    Data.Builder builder = Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    IndexIterator iiter = data.getIndexIterator();
    switch (dataType) {
      case CHAR:
        byte[] cdata = convertCharToByte((char[]) data.get1DJavaArray(DataType.CHAR));
        builder.addBdata(ByteString.copyFrom(cdata));
        break;
      case ENUM1:
      case UBYTE:
      case BYTE:
        byte[] bdata = (byte[]) data.get1DJavaArray(DataType.UBYTE);
        builder.addBdata(ByteString.copyFrom(bdata));
        break;
      case SHORT:
      case INT:
        while (iiter.hasNext()) {
          builder.addIdata(iiter.getIntNext());
        }
        break;
      case ENUM2:
      case ENUM4:
      case USHORT:
      case UINT:
        while (iiter.hasNext()) {
          builder.addUidata(iiter.getIntNext());
        }
        break;
      case LONG:
        while (iiter.hasNext()) {
          builder.addLdata(iiter.getLongNext());
        }
        break;
      case ULONG:
        while (iiter.hasNext()) {
          builder.addUldata(iiter.getLongNext());
        }
        break;
      case FLOAT:
        while (iiter.hasNext()) {
          builder.addFdata(iiter.getFloatNext());
        }
        break;
      case DOUBLE:
        while (iiter.hasNext()) {
          builder.addDdata(iiter.getDoubleNext());
        }
        break;
      case STRING:
        while (iiter.hasNext()) {
          builder.addSdata((String) iiter.getObjectNext());
        }
        break;
      case OPAQUE:
        while (iiter.hasNext()) {
          ByteBuffer bb = (ByteBuffer) iiter.getObjectNext();
          builder.addBdata(ByteString.copyFrom(bb.array()));
        }
        break;
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
    return builder.build();
  }

  public static Data encodeVlenData(DataType dataType, ArrayObject data) {
    Data.Builder builder = Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    IndexIterator objectIterator = data.getIndexIterator();
    while (objectIterator.hasNext()) {
      Array array = (Array) objectIterator.next();
      // builder.addVlen((int) array.getSize());
      IndexIterator iiter = array.getIndexIterator();

      switch (dataType) {
        case CHAR:
          byte[] cdata = convertCharToByte((char[]) array.get1DJavaArray(DataType.CHAR));
          builder.addBdata(ByteString.copyFrom(cdata));
          break;
        case ENUM1:
        case UBYTE:
        case BYTE:
          byte[] bdata = (byte[]) array.get1DJavaArray(DataType.UBYTE);
          builder.addBdata(ByteString.copyFrom(bdata));
          break;
        case SHORT:
        case INT:
          while (iiter.hasNext()) {
            builder.addIdata(iiter.getIntNext());
          }
          break;
        case ENUM2:
        case ENUM4:
        case USHORT:
        case UINT:
          while (iiter.hasNext()) {
            builder.addUidata(iiter.getIntNext());
          }
          break;
        case LONG:
          while (iiter.hasNext()) {
            builder.addLdata(iiter.getLongNext());
          }
          break;
        case ULONG:
          while (iiter.hasNext()) {
            builder.addUldata(iiter.getLongNext());
          }
          break;
        case FLOAT:
          while (iiter.hasNext()) {
            builder.addFdata(iiter.getFloatNext());
          }
          break;
        case DOUBLE:
          while (iiter.hasNext()) {
            builder.addDdata(iiter.getDoubleNext());
          }
          break;
        case STRING:
          while (iiter.hasNext()) {
            builder.addSdata((String) iiter.getObjectNext());
          }
          break;
        case OPAQUE:
          while (iiter.hasNext()) {
            ByteBuffer bb = (ByteBuffer) iiter.getObjectNext();
            builder.addBdata(ByteString.copyFrom(bb.array()));
          }
          break;
        default:
          throw new IllegalStateException("Unkown datatype " + dataType);
      }
    }
    return builder.build();
  }

  public static GcdmNetcdfProto.Section encodeSection(Section section) {
    GcdmNetcdfProto.Section.Builder sbuilder = GcdmNetcdfProto.Section.newBuilder();
    for (Range r : section.getRanges()) {
      GcdmNetcdfProto.Range.Builder rbuilder = GcdmNetcdfProto.Range.newBuilder();
      rbuilder.setStart(r.first());
      rbuilder.setSize(r.length());
      rbuilder.setStride(r.stride());
      sbuilder.addRanges(rbuilder);
    }
    return sbuilder.build();
  }

  public static Data encodeArrayStructureData(ArrayStructure arrayStructure) {
    Data.Builder builder = Data.newBuilder();
    builder.setDataType(convertDataType(DataType.STRUCTURE));

    StructureMembers sm = arrayStructure.getStructureMembers();
    for (Member member : sm.getMembers()) {
      StructureMemberProto.Builder smBuilder = StructureMemberProto.newBuilder().setName(member.getName())
          .setDataType(convertDataType(member.getDataType())).addAllShapes(Ints.asList(member.getShape()));
      // builder.addMembers(smBuilder);
    }

    // row oriented
    for (StructureData sdata : arrayStructure) {
      builder.addRows(encodeStructureData(sdata));
    }
    return builder.build();
  }

  public static GcdmNetcdfProto.StructureDataProto encodeStructureData(StructureData structData) {
    GcdmNetcdfProto.StructureDataProto.Builder builder = GcdmNetcdfProto.StructureDataProto.newBuilder();
    for (Member member : structData.getMembers()) {
      Array data = structData.getArray(member);
      builder.addMemberData(encodeData(member.getDataType(), data));
    }
    return builder.build();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static Dimension decodeDim(GcdmNetcdfProto.Dimension dim) {
    String name = (dim.getName().isEmpty() ? null : dim.getName());
    int dimLen = dim.getIsVlen() ? -1 : (int) dim.getLength();
    return Dimension.builder().setName(name).setIsShared(!dim.getIsPrivate()).setIsUnlimited(dim.getIsUnlimited())
        .setIsVariableLength(dim.getIsVlen()).setLength(dimLen).build();
  }

  public static void decodeGroup(GcdmNetcdfProto.Group proto, Group.Builder g) {

    for (GcdmNetcdfProto.Dimension dim : proto.getDimsList())
      g.addDimension(GcdmConverterMa2.decodeDim(dim)); // always added to group? what if private ??

    for (GcdmNetcdfProto.Attribute att : proto.getAttsList())
      g.addAttribute(GcdmConverterMa2.decodeAtt(att));

    for (GcdmNetcdfProto.EnumTypedef enumType : proto.getEnumTypesList())
      g.addEnumTypedef(GcdmConverterMa2.decodeEnumTypedef(enumType));

    for (GcdmNetcdfProto.Variable var : proto.getVarsList())
      g.addVariable(GcdmConverterMa2.decodeVar(var));

    for (GcdmNetcdfProto.Structure s : proto.getStructsList())
      g.addVariable(GcdmConverterMa2.decodeStructure(s));

    for (GcdmNetcdfProto.Group gp : proto.getGroupsList()) {
      Group.Builder ng = Group.builder().setName(gp.getName());
      g.addGroup(ng);
      decodeGroup(gp, ng);
    }
  }

  private static EnumTypedef decodeEnumTypedef(GcdmNetcdfProto.EnumTypedef enumType) {
    List<GcdmNetcdfProto.EnumTypedef.EnumType> list = enumType.getMapsList();
    Map<Integer, String> map = new HashMap<>(2 * list.size());
    for (GcdmNetcdfProto.EnumTypedef.EnumType et : list) {
      map.put(et.getCode(), et.getValue());
    }
    ArrayType basetype = convertDataType(enumType.getBaseType());
    return new EnumTypedef(enumType.getName(), map, basetype);
  }

  public static Attribute decodeAtt(GcdmNetcdfProto.Attribute attp) {
    DataType dtUse = convertDataType(attp.getDataType()).getDataType();
    int len = attp.getLength();
    if (len == 0) { // deal with empty attribute
      return Attribute.builder(attp.getName()).setDataType(dtUse).build();
    }

    Data attData = attp.getData();
    if (dtUse == DataType.STRING) {
      List<String> values = attData.getSdataList();
      if (values.size() != len) {
        throw new IllegalStateException();
      }
      if (values.size() == 1) {
        return new Attribute(attp.getName(), values.get(0));
      } else {
        Array data = Array.factory(dtUse, new int[] {len});
        for (int i = 0; i < len; i++)
          data.setObject(i, values.get(i));
        return Attribute.fromArray(attp.getName(), data);
      }
    } else {
      Array array = decodeData(attp.getData(), Section.builder().appendRange(len).build());
      return Attribute.fromArray(attp.getName(), array);
    }
  }

  private static Variable.Builder<?> decodeVar(GcdmNetcdfProto.Variable var) {
    ArrayType varType = convertDataType(var.getDataType());
    Variable.Builder<?> ncvar = Variable.builder().setName(var.getName()).setArrayType(varType);

    if (varType.isEnum()) {
      ncvar.setEnumTypeName(var.getEnumType());
    }

    // The Dimensions are stored redunantly in the Variable.
    // If shared, they must also exist in a parent Group. However, we dont yet have the Groups wired together,
    // so that has to wait until build().
    List<Dimension> dims = new ArrayList<>(6);
    Section.Builder section = Section.builder();
    for (GcdmNetcdfProto.Dimension dim : var.getShapesList()) {
      dims.add(decodeDim(dim));
      section.appendRange((int) dim.getLength());
    }
    ncvar.addDimensions(dims);

    for (GcdmNetcdfProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    if (var.hasData()) {
      Array data = decodeData(var.getData(), section.build());
      ncvar.setSourceData(data);
    }

    return ncvar;
  }

  private static Structure.Builder<?> decodeStructure(GcdmNetcdfProto.Structure s) {
    Structure.Builder<?> ncvar =
        (s.getDataType() == GcdmNetcdfProto.DataType.DATA_TYPE_SEQUENCE) ? Sequence.builder() : Structure.builder();

    ncvar.setName(s.getName()).setArrayType(convertDataType(s.getDataType()));

    List<Dimension> dims = new ArrayList<>(6);
    for (GcdmNetcdfProto.Dimension dim : s.getShapesList()) {
      dims.add(decodeDim(dim));
    }
    ncvar.addDimensions(dims);

    for (GcdmNetcdfProto.Attribute att : s.getAttsList()) {
      ncvar.addAttribute(decodeAtt(att));
    }

    for (GcdmNetcdfProto.Variable vp : s.getVarsList()) {
      ncvar.addMemberVariable(decodeVar(vp));
    }

    for (GcdmNetcdfProto.Structure sp : s.getStructsList()) {
      ncvar.addMemberVariable(decodeStructure(sp));
    }

    return ncvar;
  }

  @Nonnull
  public static Section decodeSection(GcdmNetcdfProto.Section proto) {
    Section.Builder section = Section.builder();

    for (GcdmNetcdfProto.Range pr : proto.getRangesList()) {
      try {
        long stride = pr.getStride();
        if (stride == 0)
          stride = 1; // default in protobuf2 was 1, but protobuf3 is 0, luckily 0 is illegal
        if (pr.getSize() == 0)
          section.appendRange(Range.EMPTY); // used for scalars LOOK really used ??
        else {
          // this.last = first + (this.length-1) * stride;
          section.appendRange((int) pr.getStart(), (int) (pr.getStart() + (pr.getSize() - 1) * stride), (int) stride);
        }

      } catch (InvalidRangeException e) {
        throw new RuntimeException("Bad Section in Gcdm", e);
      }
    }
    return section.build();
  }

  // Note that this converts to Objects, so not very efficient ??
  public static Array decodeData(Data data, Section section) {
    DataType dataType = convertDataType(data.getDataType()).getDataType();
    int[] shape = section.getShape();
    switch (dataType) {
      case CHAR: {
        byte[] array = data.getBdata(0).toByteArray();
        return Array.factory(dataType, shape, convertByteToChar(array));
      }
      case ENUM1:
      case UBYTE:
      case BYTE: {
        byte[] array = data.getBdata(0).toByteArray();
        return Array.factory(dataType, shape, array);
      }
      case SHORT: {
        int i = 0;
        short[] array = new short[data.getIdataCount()];
        for (int val : data.getIdataList()) {
          array[i++] = (short) val;
        }
        return Array.factory(dataType, shape, array);
      }
      case INT: {
        int i = 0;
        int[] array = new int[data.getIdataCount()];
        for (int val : data.getIdataList()) {
          array[i++] = val;
        }
        return Array.factory(dataType, shape, array);
      }
      case ENUM2:
      case USHORT: {
        int i = 0;
        short[] array = new short[data.getUidataCount()];
        for (int val : data.getUidataList()) {
          array[i++] = (short) val;
        }
        return Array.factory(dataType, shape, array);
      }
      case ENUM4:
      case UINT: {
        int i = 0;
        int[] array = new int[data.getUidataCount()];
        for (int val : data.getUidataList()) {
          array[i++] = val;
        }
        return Array.factory(dataType, shape, array);
      }
      case LONG: {
        int i = 0;
        long[] array = new long[data.getLdataCount()];
        for (long val : data.getLdataList()) {
          array[i++] = val;
        }
        return Array.factory(dataType, shape, array);
      }
      case ULONG: {
        int i = 0;
        long[] array = new long[data.getUldataCount()];
        for (long val : data.getUldataList()) {
          array[i++] = val;
        }
        return Array.factory(dataType, shape, array);
      }
      case FLOAT: {
        int i = 0;
        float[] array = new float[data.getFdataCount()];
        for (float val : data.getFdataList()) {
          array[i++] = val;
        }
        return Array.factory(dataType, shape, array);
      }
      case DOUBLE: {
        int i = 0;
        double[] array = new double[data.getDdataCount()];
        for (double val : data.getDdataList()) {
          array[i++] = val;
        }
        return Array.factory(dataType, shape, array);
      }
      case STRING: {
        int i = 0;
        Object[] array = new Object[data.getSdataCount()];
        for (String val : data.getSdataList()) {
          array[i++] = val;
        }
        return Array.factory(dataType, shape, array);
      }
      case SEQUENCE:
      case STRUCTURE: {
        return decodeArrayStructureData(data, new Section(shape));
      }
      case OPAQUE: {
        int i = 0;
        Object[] array = new Object[data.getBdataCount()];
        for (ByteString val : data.getBdataList()) {
          array[i++] = ByteBuffer.wrap(val.toByteArray());
        }
        return Array.factory(dataType, shape, array);
      }
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
  }

  public static Array decodeVlenData(Data data, Section section) {
    Preconditions.checkArgument(data.getVlenCount() > 0);
    int[] shape = section.toBuilder().removeLast().build().getShape();
    int length = (int) Index.computeSize(shape);
    Preconditions.checkArgument(length == data.getVlenCount());
    Array[] storage = new Array[length];
    DataType dataType = convertDataType(data.getDataType()).getDataType();

    int innerStart = 0;
    for (int index = 0; index < length; index++) {
      int innerLength = 0; // data.getVlen(index);
      int innerEnd = innerStart + innerLength;
      int[] innerShape = new int[] {innerLength};
      switch (dataType) {
        case CHAR: {
          byte[] array = data.getBdata(index).toByteArray();
          storage[index] = Array.factory(dataType, innerShape, convertByteToChar(array));
          break;
        }
        case ENUM1:
        case UBYTE:
        case BYTE: {
          byte[] array = data.getBdata(index).toByteArray();
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case SHORT: {
          int i = 0;
          short[] array = new short[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = (short) data.getIdata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case INT: {
          int i = 0;
          int[] array = new int[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = data.getIdata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case ENUM2:
        case USHORT: {
          int i = 0;
          short[] array = new short[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = (short) data.getUidata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case ENUM4:
        case UINT: {
          int i = 0;
          int[] array = new int[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = data.getUidata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case LONG: {
          int i = 0;
          long[] array = new long[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = data.getLdata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case ULONG: {
          int i = 0;
          long[] array = new long[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = data.getUldata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case FLOAT: {
          int i = 0;
          float[] array = new float[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = data.getFdata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case DOUBLE: {
          int i = 0;
          double[] array = new double[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = data.getDdata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case STRING: {
          int i = 0;
          Object[] array = new Object[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            array[i++] = data.getSdata(innerIndex);
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        case OPAQUE: {
          int i = 0;
          Object[] array = new Object[innerLength];
          for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
            ByteString val = data.getBdata(innerIndex);
            array[i++] = ByteBuffer.wrap(val.toByteArray());
          }
          storage[index] = Array.factory(dataType, innerShape, array);
          break;
        }
        default:
          throw new IllegalStateException("Unkown datatype " + dataType);
      }
      innerStart = innerEnd;
    }
    return Array.makeVlenArray(shape, storage);
  }

  ////////////////////////////////////////////////////////////////

  public static GcdmNetcdfProto.DataType convertDataType(DataType dtype) {
    switch (dtype) {
      case CHAR:
        return GcdmNetcdfProto.DataType.DATA_TYPE_CHAR;
      case BYTE:
        return GcdmNetcdfProto.DataType.DATA_TYPE_BYTE;
      case SHORT:
        return GcdmNetcdfProto.DataType.DATA_TYPE_SHORT;
      case INT:
        return GcdmNetcdfProto.DataType.DATA_TYPE_INT;
      case LONG:
        return GcdmNetcdfProto.DataType.DATA_TYPE_LONG;
      case FLOAT:
        return GcdmNetcdfProto.DataType.DATA_TYPE_FLOAT;
      case DOUBLE:
        return GcdmNetcdfProto.DataType.DATA_TYPE_DOUBLE;
      case STRING:
        return GcdmNetcdfProto.DataType.DATA_TYPE_STRING;
      case STRUCTURE:
        return GcdmNetcdfProto.DataType.DATA_TYPE_STRUCTURE;
      case SEQUENCE:
        return GcdmNetcdfProto.DataType.DATA_TYPE_SEQUENCE;
      case ENUM1:
        return GcdmNetcdfProto.DataType.DATA_TYPE_ENUM1;
      case ENUM2:
        return GcdmNetcdfProto.DataType.DATA_TYPE_ENUM2;
      case ENUM4:
        return GcdmNetcdfProto.DataType.DATA_TYPE_ENUM4;
      case OPAQUE:
        return GcdmNetcdfProto.DataType.DATA_TYPE_OPAQUE;
      case UBYTE:
        return GcdmNetcdfProto.DataType.DATA_TYPE_UBYTE;
      case USHORT:
        return GcdmNetcdfProto.DataType.DATA_TYPE_USHORT;
      case UINT:
        return GcdmNetcdfProto.DataType.DATA_TYPE_UINT;
      case ULONG:
        return GcdmNetcdfProto.DataType.DATA_TYPE_ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  public static ArrayType convertDataType(GcdmNetcdfProto.DataType dtype) {
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

  public static ArrayStructure decodeArrayStructureData(Data arrayStructureProto, Section section) {
    int nrows = arrayStructureProto.getRowsCount();
    Preconditions.checkArgument(nrows > 0);
    Preconditions.checkArgument(section.getSize() == nrows);

    /*
     * StructureMembers.Builder membersb = StructureMembers.builder();
     * for (StructureMemberProto memberProto : arrayStructureProto.getMembersList()) {
     * MemberBuilder memberb = StructureMembers.memberBuilder();
     * memberb.setName(memberProto.getName());
     * memberb.setDataType(convertDataType(memberProto.getDataType()));
     * memberb.setShape(Ints.toArray(memberProto.getShapeList()));
     * membersb.addMember(memberb);
     * }
     * StructureMembers members = membersb.build();
     * 
     * ArrayStructureW result = new ArrayStructureW(members, section.getShape());
     * // row oriented
     * int index = 0;
     * for (GcdmNetcdfProto.StructureDataProto row : arrayStructureProto.getRowsList()) {
     * result.setStructureData(decodeStructureData(row, members), index);
     * index++;
     * }
     * return result;
     */
    return null;
  }

  public static StructureData decodeStructureData(GcdmNetcdfProto.StructureDataProto structDataProto,
      StructureMembers members) {
    StructureDataW sdata = new StructureDataW(members);
    for (int i = 0; i < structDataProto.getMemberDataCount(); i++) {
      Data data = structDataProto.getMemberData(i);
      Member member = members.getMember(i);
      sdata.setMemberData(member, decodeData(data, new Section(member.getShape())));
    }
    return sdata;
  }

}
