/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static ucar.nc2.iosp.IospHelper.convertByteToChar;
import static ucar.nc2.iosp.IospHelper.convertCharToByte;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ucar.gcdm.GcdmNetcdfProto.Data;
import ucar.gcdm.GcdmNetcdfProto.StructureMemberProto;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArraySequence;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.Member;
import ucar.ma2.StructureMembers.MemberBuilder;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** Convert between Gcdm Protos and Netcdf objects, using Array for data. */
public class GcdmConverter {
  private static final BiMap<DataType, GcdmNetcdfProto.DataType> dataTypeProtoDataTypeMap =
      new ImmutableBiMap.Builder<DataType, GcdmNetcdfProto.DataType>()
          .put(DataType.CHAR, GcdmNetcdfProto.DataType.DATA_TYPE_CHAR)
          .put(DataType.BYTE, GcdmNetcdfProto.DataType.DATA_TYPE_BYTE)
          .put(DataType.SHORT, GcdmNetcdfProto.DataType.DATA_TYPE_SHORT)
          .put(DataType.INT, GcdmNetcdfProto.DataType.DATA_TYPE_INT)
          .put(DataType.LONG, GcdmNetcdfProto.DataType.DATA_TYPE_LONG)
          .put(DataType.FLOAT, GcdmNetcdfProto.DataType.DATA_TYPE_FLOAT)
          .put(DataType.DOUBLE, GcdmNetcdfProto.DataType.DATA_TYPE_DOUBLE)
          .put(DataType.STRING, GcdmNetcdfProto.DataType.DATA_TYPE_STRING)
          .put(DataType.STRUCTURE, GcdmNetcdfProto.DataType.DATA_TYPE_STRUCTURE)
          .put(DataType.SEQUENCE, GcdmNetcdfProto.DataType.DATA_TYPE_SEQUENCE)
          .put(DataType.ENUM1, GcdmNetcdfProto.DataType.DATA_TYPE_ENUM1)
          .put(DataType.ENUM2, GcdmNetcdfProto.DataType.DATA_TYPE_ENUM2)
          .put(DataType.ENUM4, GcdmNetcdfProto.DataType.DATA_TYPE_ENUM4)
          .put(DataType.OPAQUE, GcdmNetcdfProto.DataType.DATA_TYPE_OPAQUE)
          .put(DataType.UBYTE, GcdmNetcdfProto.DataType.DATA_TYPE_UBYTE)
          .put(DataType.USHORT, GcdmNetcdfProto.DataType.DATA_TYPE_USHORT)
          .put(DataType.UINT, GcdmNetcdfProto.DataType.DATA_TYPE_UINT)
          .put(DataType.ULONG, GcdmNetcdfProto.DataType.DATA_TYPE_ULONG).build();

  public static GcdmNetcdfProto.Group.Builder encodeGroup(Group group, int sizeToCache) throws IOException {
    final GcdmNetcdfProto.Group.Builder groupBuilder = GcdmNetcdfProto.Group.newBuilder();
    groupBuilder.setName(group.getShortName());

    for (Dimension dimension : group.getDimensions()) {
      groupBuilder.addDimensions(encodeDimension(dimension));
    }

    for (Attribute attribute : group.attributes()) {
      groupBuilder.addAttributes(encodeAttribute(attribute));
    }

    for (EnumTypedef enumType : group.getEnumTypedefs()) {
      groupBuilder.addEnumTypes(encodeEnumTypedef(enumType));
    }

    for (Variable variable : group.getVariables()) {
      if (variable instanceof Structure) {
        groupBuilder.addStructures(encodeStructure((Structure) variable));
      } else {
        groupBuilder.addVariables(encodeVariable(variable, sizeToCache));
      }
    }

    for (Group nestedGroup : group.getGroups()) {
      groupBuilder.addGroups(encodeGroup(nestedGroup, sizeToCache));
    }

    return groupBuilder;
  }

  private static GcdmNetcdfProto.Attribute.Builder encodeAttribute(Attribute attribute) {
    final GcdmNetcdfProto.Attribute.Builder attributeBuilder = GcdmNetcdfProto.Attribute.newBuilder();
    attributeBuilder.setName(attribute.getShortName());
    attributeBuilder.setDataType(convertDataType(attribute.getDataType()));
    attributeBuilder.setLength(attribute.getLength());

    if (attribute.getValues() != null && attribute.getLength() > 0) {
      if (attribute.isString()) {
        final Data.Builder dataBuilder = Data.newBuilder();
        for (int i = 0; i < attribute.getLength(); i++) {
          dataBuilder.addStringData(attribute.getStringValue(i));
        }
        dataBuilder.setDataType(convertDataType(attribute.getDataType()));
        attributeBuilder.setData(dataBuilder);

      } else {
        attributeBuilder.setData(encodeData(attribute.getDataType(), attribute.getValues()));
      }
    }

    return attributeBuilder;
  }

  private static GcdmNetcdfProto.Dimension.Builder encodeDimension(Dimension dimension) {
    final GcdmNetcdfProto.Dimension.Builder dimensionBuilder = GcdmNetcdfProto.Dimension.newBuilder();
    if (dimension.getShortName() != null) {
      dimensionBuilder.setName(dimension.getShortName());
    }
    if (!dimension.isVariableLength()) {
      dimensionBuilder.setLength(dimension.getLength());
    }
    dimensionBuilder.setIsPrivate(!dimension.isShared());
    dimensionBuilder.setIsVlen(dimension.isVariableLength());
    dimensionBuilder.setIsUnlimited(dimension.isUnlimited());
    return dimensionBuilder;
  }

  private static GcdmNetcdfProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) {
    final GcdmNetcdfProto.EnumTypedef.Builder enumTypdefBuilder = GcdmNetcdfProto.EnumTypedef.newBuilder();

    enumTypdefBuilder.setName(enumType.getShortName());
    enumTypdefBuilder.setBaseType(convertDataType(enumType.getBaseType()));
    final Map<Integer, String> map = enumType.getMap();
    final GcdmNetcdfProto.EnumTypedef.EnumType.Builder enumTypeBuilder =
        GcdmNetcdfProto.EnumTypedef.EnumType.newBuilder();
    for (int code : map.keySet()) {
      enumTypeBuilder.clear();
      enumTypeBuilder.setCode(code);
      enumTypeBuilder.setValue(map.get(code));
      enumTypdefBuilder.addMaps(enumTypeBuilder);
    }
    return enumTypdefBuilder;
  }

  private static GcdmNetcdfProto.Variable.Builder encodeVariable(Variable variable, int sizeToCache)
      throws IOException {
    final GcdmNetcdfProto.Variable.Builder builder = GcdmNetcdfProto.Variable.newBuilder();
    builder.setName(variable.getShortName());
    builder.setDataType(convertDataType(variable.getDataType()));
    if (variable.getDataType().isEnum()) {
      final EnumTypedef enumType = variable.getEnumTypedef();
      if (enumType != null) {
        builder.setEnumType(enumType.getShortName());
      }
    }

    for (Dimension dimension : variable.getDimensions()) {
      builder.addShapes(encodeDimension(dimension));
    }

    for (Attribute attribute : variable.attributes()) {
      builder.addAttributes(encodeAttribute(attribute));
    }

    // put small amounts of data in header "immediate mode"
    if (variable.isCaching() && variable.getDataType().isNumeric()) {
      if (variable.isCoordinateVariable() || variable.getSize() * variable.getElementSize() < sizeToCache) {
        final Array data = variable.read();
        builder.setData(encodeData(variable.getDataType(), data));
      }
    }

    return builder;
  }

  private static GcdmNetcdfProto.Structure.Builder encodeStructure(Structure structure) throws IOException {
    final GcdmNetcdfProto.Structure.Builder builder = GcdmNetcdfProto.Structure.newBuilder();
    builder.setName(structure.getShortName());
    builder.setDataType(convertDataType(structure.getDataType()));

    for (Dimension dimension : structure.getDimensions()) {
      builder.addShapes(encodeDimension(dimension));
    }

    for (Attribute attribute : structure.attributes()) {
      builder.addAttributes(encodeAttribute(attribute));
    }

    for (Variable variable : structure.getVariables()) {
      if (variable instanceof Structure) {
        builder.addStructs(GcdmConverter.encodeStructure((Structure) variable));
      } else {
        builder.addVariables(encodeVariable(variable, -1));
      }
    }

    return builder;
  }

  public static GcdmNetcdfProto.Data encodeData(DataType dataType, Array data) {
    if (data.isVlen()) {
      return encodeVlenData(dataType, (ArrayObject) data);
    } else if (data instanceof ArrayStructure) {
      return encodeArrayStructureData(dataType, (ArrayStructure) data);
    } else {
      return encodePrimitiveData(dataType, data);
    }
  }

  private static void encodeShape(GcdmNetcdfProto.Data.Builder data, int[] shape) {
    for (int i : shape) {
      data.addShapes(i);
    }
  }

  private static Data encodePrimitiveData(DataType dataType, Array data) {
    final Data.Builder builder = Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    encodeShape(builder, data.getShape());
    final IndexIterator indexIterator = data.getIndexIterator();

    switch (dataType) {
      case CHAR: {
        final byte[] array = convertCharToByte((char[]) data.get1DJavaArray(DataType.CHAR));
        builder.addByteData(ByteString.copyFrom(array));
        break;
      }
      case ENUM1:
      case UBYTE:
      case BYTE: {
        final byte[] array = (byte[]) data.get1DJavaArray(DataType.UBYTE);
        builder.addByteData(ByteString.copyFrom(array));
        break;
      }
      case SHORT:
      case INT:
        while (indexIterator.hasNext()) {
          builder.addIntData(indexIterator.getIntNext());
        }
        break;
      case ENUM2:
      case ENUM4:
      case USHORT:
      case UINT:
        while (indexIterator.hasNext()) {
          builder.addUintData(indexIterator.getIntNext());
        }
        break;
      case LONG:
        while (indexIterator.hasNext()) {
          builder.addLongData(indexIterator.getLongNext());
        }
        break;
      case ULONG:
        while (indexIterator.hasNext()) {
          builder.addUlongData(indexIterator.getLongNext());
        }
        break;
      case FLOAT:
        while (indexIterator.hasNext()) {
          builder.addFloatData(indexIterator.getFloatNext());
        }
        break;
      case DOUBLE:
        while (indexIterator.hasNext()) {
          builder.addDoubleData(indexIterator.getDoubleNext());
        }
        break;
      case STRING:
        while (indexIterator.hasNext()) {
          builder.addStringData((String) indexIterator.getObjectNext());
        }
        break;
      case OPAQUE:
        while (indexIterator.hasNext()) {
          final ByteBuffer bb = (ByteBuffer) indexIterator.getObjectNext();
          builder.addByteData(ByteString.copyFrom(bb.array()));
        }
        break;
      default:
        throw new IllegalStateException("Unknown datatype " + dataType);
    }
    return builder.build();
  }

  private static Data encodeVlenData(DataType dataType, ArrayObject data) {
    final Data.Builder builder = Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    encodeShape(builder, data.getShape());
    final IndexIterator objectIterator = data.getIndexIterator();
    while (objectIterator.hasNext()) {
      final Array array = (Array) objectIterator.next();
      builder.addVlenData(encodeData(dataType, array));
    }
    return builder.build();
  }

  private static Data encodeArrayStructureData(DataType dataType, ArrayStructure arrayStructure) {
    final Data.Builder builder = Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    encodeShape(builder, arrayStructure.getShape());
    builder.setMembers(encodeStructureMembers(arrayStructure.getStructureMembers()));

    // row oriented
    for (StructureData structureData : arrayStructure) {
      builder.addRows(encodeStructureData(structureData));
    }
    return builder.build();
  }

  private static GcdmNetcdfProto.StructureMembersProto encodeStructureMembers(StructureMembers members) {
    final GcdmNetcdfProto.StructureMembersProto.Builder builder = GcdmNetcdfProto.StructureMembersProto.newBuilder();
    builder.setName(members.getName());
    for (Member member : members.getMembers()) {
      final StructureMemberProto.Builder structureMemberBuilder =
          StructureMemberProto.newBuilder().setName(member.getName()).setDataType(convertDataType(member.getDataType()))
              .addAllShapes(Ints.asList(member.getShape()));
      if (member.getStructureMembers() != null) {
        structureMemberBuilder.setMembers(encodeStructureMembers(member.getStructureMembers()));
      }
      builder.addMembers(structureMemberBuilder);
    }
    return builder.build();
  }

  private static GcdmNetcdfProto.StructureDataProto encodeStructureData(StructureData structureData) {
    final GcdmNetcdfProto.StructureDataProto.Builder builder = GcdmNetcdfProto.StructureDataProto.newBuilder();
    for (Member member : structureData.getMembers()) {
      final Array data = structureData.getArray(member);
      builder.addMemberData(encodeData(member.getDataType(), data));
    }
    return builder.build();
  }

  private static Dimension decodeDimension(GcdmNetcdfProto.Dimension dimension) {
    final String name = (dimension.getName().isEmpty() ? null : dimension.getName());
    final int dimLen = dimension.getIsVlen() ? -1 : (int) dimension.getLength();
    return Dimension.builder().setName(name).setIsShared(!dimension.getIsPrivate())
        .setIsUnlimited(dimension.getIsUnlimited()).setIsVariableLength(dimension.getIsVlen()).setLength(dimLen)
        .build();
  }

  public static void decodeGroup(GcdmNetcdfProto.Group protoGroup, Group.Builder groupBuilder) {
    for (GcdmNetcdfProto.Dimension dim : protoGroup.getDimensionsList()) {
      groupBuilder.addDimension(GcdmConverter.decodeDimension(dim)); // always added to group? what if private ??
    }

    for (GcdmNetcdfProto.Attribute att : protoGroup.getAttributesList()) {
      groupBuilder.addAttribute(GcdmConverter.decodeAttribute(att));
    }

    for (GcdmNetcdfProto.EnumTypedef enumType : protoGroup.getEnumTypesList()) {
      groupBuilder.addEnumTypedef(GcdmConverter.decodeEnumTypedef(enumType));
    }

    for (GcdmNetcdfProto.Variable var : protoGroup.getVariablesList()) {
      groupBuilder.addVariable(GcdmConverter.decodeVariable(var));
    }

    for (GcdmNetcdfProto.Structure s : protoGroup.getStructuresList()) {
      groupBuilder.addVariable(GcdmConverter.decodeStructure(s));
    }

    for (GcdmNetcdfProto.Group nestedProtoGroup : protoGroup.getGroupsList()) {
      final Group.Builder nestedGroup = Group.builder().setName(nestedProtoGroup.getName());
      groupBuilder.addGroup(nestedGroup);
      decodeGroup(nestedProtoGroup, nestedGroup);
    }
  }

  private static EnumTypedef decodeEnumTypedef(GcdmNetcdfProto.EnumTypedef enumTypedef) {
    final List<GcdmNetcdfProto.EnumTypedef.EnumType> enumTypes = enumTypedef.getMapsList();
    final Map<Integer, String> map = new HashMap<>(2 * enumTypes.size());
    for (GcdmNetcdfProto.EnumTypedef.EnumType enumType : enumTypes) {
      map.put(enumType.getCode(), enumType.getValue());
    }
    final DataType baseType = convertDataType(enumTypedef.getBaseType());
    return new EnumTypedef(enumTypedef.getName(), map, baseType);
  }

  private static Attribute decodeAttribute(GcdmNetcdfProto.Attribute protoAttribute) {
    final DataType dataType = convertDataType(protoAttribute.getDataType());
    final int length = protoAttribute.getLength();
    if (length == 0) { // deal with empty attribute
      return Attribute.builder(protoAttribute.getName()).setDataType(dataType).build();
    }

    final Data attributeData = protoAttribute.getData();
    if (dataType == DataType.STRING) {
      final List<String> values = attributeData.getStringDataList();
      if (values.size() != length) {
        throw new IllegalStateException();
      }
      if (values.size() == 1) {
        return new Attribute(protoAttribute.getName(), values.get(0));
      } else {
        final Array data = Array.factory(dataType, new int[] {length});
        for (int i = 0; i < length; i++) {
          data.setObject(i, values.get(i));
        }
        return Attribute.builder(protoAttribute.getName()).setValues(data).build();
      }
    } else {
      final Array array = decodeData(protoAttribute.getData());
      return Attribute.builder(protoAttribute.getName()).setValues(array).build();
    }
  }

  private static Variable.Builder<?> decodeVariable(GcdmNetcdfProto.Variable protoVariable) {
    final DataType dataType = convertDataType(protoVariable.getDataType());
    final Variable.Builder<?> variableBuilder =
        Variable.builder().setName(protoVariable.getName()).setDataType(dataType);

    if (dataType.isEnum()) {
      variableBuilder.setEnumTypeName(protoVariable.getEnumType());
    }

    // The Dimensions are stored redundantly in the Variable.
    // If shared, they must also exist in a parent Group. However, we don't yet have the Groups wired together,
    // so that has to wait until build().
    final List<Dimension> dimensions = new ArrayList<>(6);
    final Section.Builder section = Section.builder();
    for (GcdmNetcdfProto.Dimension dimension : protoVariable.getShapesList()) {
      dimensions.add(decodeDimension(dimension));
      section.appendRange((int) dimension.getLength());
    }
    variableBuilder.addDimensions(dimensions);

    for (GcdmNetcdfProto.Attribute attribute : protoVariable.getAttributesList()) {
      variableBuilder.addAttribute(decodeAttribute(attribute));
    }

    if (protoVariable.hasData()) {
      final Array data = decodeData(protoVariable.getData());
      variableBuilder.setCachedData(data, false);
    }

    return variableBuilder;
  }

  private static Structure.Builder<?> decodeStructure(GcdmNetcdfProto.Structure protoStructure) {
    final Structure.Builder<?> structureBuilder =
        (protoStructure.getDataType() == GcdmNetcdfProto.DataType.DATA_TYPE_SEQUENCE) ? Sequence.builder()
            : Structure.builder();

    structureBuilder.setName(protoStructure.getName()).setDataType(convertDataType(protoStructure.getDataType()));

    final List<Dimension> dimensions = new ArrayList<>(6);
    for (GcdmNetcdfProto.Dimension dimension : protoStructure.getShapesList()) {
      dimensions.add(decodeDimension(dimension));
    }
    structureBuilder.addDimensions(dimensions);

    for (GcdmNetcdfProto.Attribute attribute : protoStructure.getAttributesList()) {
      structureBuilder.addAttribute(decodeAttribute(attribute));
    }

    for (GcdmNetcdfProto.Variable protoVariable : protoStructure.getVariablesList()) {
      structureBuilder.addMemberVariable(decodeVariable(protoVariable));
    }

    for (GcdmNetcdfProto.Structure nestedProtoStructure : protoStructure.getStructsList()) {
      structureBuilder.addMemberVariable(decodeStructure(nestedProtoStructure));
    }

    return structureBuilder;
  }

  public static Array decodeData(GcdmNetcdfProto.Data protoData) {
    if (protoData.getVlenDataCount() > 0) {
      return decodeVlenData(protoData);
    } else if (protoData.hasMembers()) {
      return decodeArrayStructureData(protoData);
    } else {
      return decodePrimitiveData(protoData);
    }
  }

  private static int[] decodeShape(GcdmNetcdfProto.Data data) {
    final int[] shape = new int[data.getShapesCount()];
    for (int i = 0; i < shape.length; i++) {
      shape[i] = data.getShapes(i);
    }
    return shape;
  }

  // Note that this converts to Objects, so not very efficient ??
  private static Array decodePrimitiveData(Data data) {
    final DataType dataType = convertDataType(data.getDataType());
    final int[] shape = decodeShape(data);

    final Object storage = decodePrimitiveData(data, dataType);
    return Array.factory(dataType, shape, storage);
  }

  private static Object decodePrimitiveData(Data data, DataType dataType) {
    switch (dataType) {
      case CHAR: {
        return convertByteToChar(data.getByteData(0).toByteArray());
      }
      case ENUM1:
      case UBYTE:
      case BYTE: {
        return data.getByteData(0).toByteArray();
      }
      case SHORT: {
        int i = 0;
        final short[] array = new short[data.getIntDataCount()];
        for (int val : data.getIntDataList()) {
          array[i++] = (short) val;
        }
        return array;
      }
      case INT: {
        int i = 0;
        final int[] array = new int[data.getIntDataCount()];
        for (int val : data.getIntDataList()) {
          array[i++] = val;
        }
        return array;
      }
      case ENUM2:
      case USHORT: {
        int i = 0;
        final short[] array = new short[data.getUintDataCount()];
        for (int val : data.getUintDataList()) {
          array[i++] = (short) val;
        }
        return array;
      }
      case ENUM4:
      case UINT: {
        int i = 0;
        final int[] array = new int[data.getUintDataCount()];
        for (int val : data.getUintDataList()) {
          array[i++] = val;
        }
        return array;
      }
      case LONG: {
        int i = 0;
        final long[] array = new long[data.getLongDataCount()];
        for (long val : data.getLongDataList()) {
          array[i++] = val;
        }
        return array;
      }
      case ULONG: {
        int i = 0;
        final long[] array = new long[data.getUlongDataCount()];
        for (long val : data.getUlongDataList()) {
          array[i++] = val;
        }
        return array;
      }
      case FLOAT: {
        int i = 0;
        final float[] array = new float[data.getFloatDataCount()];
        for (float val : data.getFloatDataList()) {
          array[i++] = val;
        }
        return array;
      }
      case DOUBLE: {
        int i = 0;
        final double[] array = new double[data.getDoubleDataCount()];
        for (double val : data.getDoubleDataList()) {
          array[i++] = val;
        }
        return array;
      }
      case STRING: {
        int i = 0;
        final Object[] array = new Object[data.getStringDataCount()];
        for (String val : data.getStringDataList()) {
          array[i++] = val;
        }
        return array;
      }
      case OPAQUE: {
        int i = 0;
        final Object[] array = new Object[data.getByteDataCount()];
        for (ByteString val : data.getByteDataList()) {
          array[i++] = ByteBuffer.wrap(val.toByteArray());
        }
        return array;
      }
      default:
        throw new IllegalStateException("Unknown datatype " + dataType);
    }
  }

  private static Array decodeVlenData(Data vlenData) {
    Preconditions.checkArgument(vlenData.getVlenDataCount() > 0);
    final int[] shape = decodeShape(vlenData);
    final int length = (int) Index.computeSize(shape);
    Preconditions.checkArgument(length == vlenData.getVlenDataCount());
    final Array[] storage = new Array[length];

    for (int i = 0; i < length; i++) {
      final Data inner = vlenData.getVlenData(i);
      storage[i] = decodeData(inner);
    }

    return Array.makeVlenArray(shape, storage);
  }

  public static GcdmNetcdfProto.DataType convertDataType(DataType dataType) {
    final GcdmNetcdfProto.DataType protoDataType = dataTypeProtoDataTypeMap.get(dataType);

    if (protoDataType == null) {
      throw new IllegalStateException("illegal data type " + dataType);
    }
    return protoDataType;
  }

  public static DataType convertDataType(GcdmNetcdfProto.DataType protoDataType) {
    final DataType dataType = dataTypeProtoDataTypeMap.inverse().get(protoDataType);

    if (protoDataType == null) {
      throw new IllegalStateException("illegal data type " + dataType);
    }
    return dataType;
  }

  private static ArrayStructure decodeArrayStructureData(Data arrayStructureProto) {
    final DataType dataType = convertDataType(arrayStructureProto.getDataType());
    final int numberOfRows = arrayStructureProto.getRowsCount();
    final int[] shape = decodeShape(arrayStructureProto);

    // ok to have numberOfRows = 0
    Preconditions.checkArgument(Index.computeSize(shape) == numberOfRows);

    StructureMembers members = decodeStructureMembers(arrayStructureProto.getMembers());

    final ArrayStructureW result = new ArrayStructureW(members, shape);

    // row oriented
    int index = 0;
    for (GcdmNetcdfProto.StructureDataProto row : arrayStructureProto.getRowsList()) {
      result.setStructureData(decodeStructureData(row, members), index);
      index++;
    }

    if (dataType == DataType.SEQUENCE) {
      return new ArraySequence(members, result.getStructureDataIterator(), -1);
    }

    return result;
  }

  private static StructureMembers decodeStructureMembers(GcdmNetcdfProto.StructureMembersProto membersProto) {
    final StructureMembers.Builder structureMembersBuilder = StructureMembers.builder();
    structureMembersBuilder.setName(membersProto.getName());

    for (StructureMemberProto memberProto : membersProto.getMembersList()) {
      final MemberBuilder memberBuilder = StructureMembers.memberBuilder().setName(memberProto.getName())
          .setDataType(convertDataType(memberProto.getDataType())).setShape(Ints.toArray(memberProto.getShapesList()));
      if (memberProto.hasMembers()) {
        memberBuilder.setStructureMembers(decodeStructureMembers(memberProto.getMembers()));
      }
      structureMembersBuilder.addMember(memberBuilder);
    }
    return structureMembersBuilder.build();
  }

  private static StructureData decodeStructureData(GcdmNetcdfProto.StructureDataProto structDataProto,
      StructureMembers members) {
    final StructureDataW structureData = new StructureDataW(members);

    for (int i = 0; i < structDataProto.getMemberDataCount(); i++) {
      final Data data = structDataProto.getMemberData(i);
      final Member member = members.getMember(i);
      structureData.setMemberData(member, decodeData(data));
    }
    return structureData;
  }
}
