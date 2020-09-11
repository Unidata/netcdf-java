/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr;

import static ucar.nc2.iosp.IospHelper.convertByteToChar;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import ucar.cdmr.CdmRemoteProto.Data;
import ucar.array.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** Convert between CdmRemote Protos and Netcdf objects. */
public class CdmrDataToMa {

  private static Dimension decodeDim(CdmRemoteProto.Dimension dim) {
    String name = (dim.getName().isEmpty() ? null : dim.getName());
    int dimLen = dim.getIsVlen() ? -1 : (int) dim.getLength();
    return Dimension.builder().setName(name).setIsShared(!dim.getIsPrivate()).setIsUnlimited(dim.getIsUnlimited())
        .setIsVariableLength(dim.getIsVlen()).setLength(dimLen).build();
  }

  public static void decodeGroup(CdmRemoteProto.Group proto, Group.Builder g) {

    for (CdmRemoteProto.Dimension dim : proto.getDimsList())
      g.addDimension(CdmrDataToMa.decodeDim(dim)); // always added to group? what if private ??

    for (CdmRemoteProto.Attribute att : proto.getAttsList())
      g.addAttribute(CdmrDataToMa.decodeAtt(att));

    for (CdmRemoteProto.EnumTypedef enumType : proto.getEnumTypesList())
      g.addEnumTypedef(CdmrDataToMa.decodeEnumTypedef(enumType));

    for (CdmRemoteProto.Variable var : proto.getVarsList())
      g.addVariable(CdmrDataToMa.decodeVariable(var));

    for (CdmRemoteProto.Structure s : proto.getStructsList())
      g.addVariable(CdmrDataToMa.decodeStructure(s));

    for (CdmRemoteProto.Group gp : proto.getGroupsList()) {
      Group.Builder ng = Group.builder().setName(gp.getName());
      g.addGroup(ng);
      decodeGroup(gp, ng);
    }
  }

  private static EnumTypedef decodeEnumTypedef(CdmRemoteProto.EnumTypedef enumType) {
    List<CdmRemoteProto.EnumTypedef.EnumType> list = enumType.getMapList();
    Map<Integer, String> map = new HashMap<>(2 * list.size());
    for (CdmRemoteProto.EnumTypedef.EnumType et : list) {
      map.put(et.getCode(), et.getValue());
    }
    DataType basetype = convertDataType(enumType.getBaseType());
    return new EnumTypedef(enumType.getName(), map, basetype);
  }

  public static Attribute decodeAtt(CdmRemoteProto.Attribute attp) {
    return new Attribute("name", "bvalue");
  }

  public static Variable.Builder<?> decodeVariable(CdmRemoteProto.Variable var) {
    DataType varType = convertDataType(var.getDataType());
    Variable.Builder<?> ncvar = Variable.builder().setName(var.getName()).setDataType(varType);

    if (varType.isEnum()) {
      ncvar.setEnumTypeName(var.getEnumType());
    }

    // The Dimensions are stored redunantly in the Variable.
    // If shared, they must also exist in a parent Group. However, we dont yet have the Groups wired together,
    // so that has to wait until build().
    List<Dimension> dims = new ArrayList<>(6);
    ucar.ma2.Section.Builder section = ucar.ma2.Section.builder();
    for (CdmRemoteProto.Dimension dim : var.getShapeList()) {
      dims.add(decodeDim(dim));
      section.appendRange((int) dim.getLength());
    }
    ncvar.addDimensions(dims);

    for (CdmRemoteProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    return ncvar;
  }

  public static ucar.ma2.Section decodeSection(CdmRemoteProto.Variable var) {
    ucar.ma2.Section.Builder section = ucar.ma2.Section.builder();
    for (CdmRemoteProto.Dimension dim : var.getShapeList()) {
      section.appendRange((int) dim.getLength());
    }
    return section.build();
  }

  private static Structure.Builder<?> decodeStructure(CdmRemoteProto.Structure s) {
    Structure.Builder<?> ncvar =
        (s.getDataType() == CdmRemoteProto.DataType.SEQUENCE) ? Sequence.builder() : Structure.builder();

    ncvar.setName(s.getName()).setDataType(convertDataType(s.getDataType()));

    List<Dimension> dims = new ArrayList<>(6);
    for (CdmRemoteProto.Dimension dim : s.getShapeList()) {
      dims.add(decodeDim(dim));
    }
    ncvar.addDimensions(dims);

    for (CdmRemoteProto.Attribute att : s.getAttsList()) {
      ncvar.addAttribute(decodeAtt(att));
    }

    for (CdmRemoteProto.Variable vp : s.getVarsList()) {
      ncvar.addMemberVariable(decodeVariable(vp));
    }

    for (CdmRemoteProto.Structure sp : s.getStructsList()) {
      ncvar.addMemberVariable(decodeStructure(sp));
    }

    return ncvar;
  }

  @Nonnull
  public static Section decodeSection(CdmRemoteProto.Section proto) {
    Section.Builder section = Section.builder();

    for (CdmRemoteProto.Range pr : proto.getRangeList()) {
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
        throw new RuntimeException("Bad Section in CdmRemote", e);
      }
    }
    return section.build();
  }

  // Note that this converts to Objects, so not very efficient ??
  public static Array<?> decodeData(Data data, CdmRemoteProto.Section proto) {
    Section section = decodeSection(proto);
    DataType dataType = convertDataType(data.getDataType());
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
      /*
       * case SEQUENCE:
       * case STRUCTURE: {
       * return decodeArrayStructureData(data, new Section(shape));
       * }
       */
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

  /*
   * public static Array decodeVlenData(Data data, Section section) {
   * Preconditions.checkArgument(data.getVlenCount() > 0);
   * int[] shape = section.toBuilder().removeLast().build().getShape();
   * int length = (int) Index.computeSize(shape);
   * Preconditions.checkArgument(length == data.getVlenCount());
   * Array[] storage = new Array[length];
   * DataType dataType = convertDataType(data.getDataType());
   * 
   * int innerStart = 0;
   * for (int index = 0; index < length; index++) {
   * int innerLength = data.getVlen(index);
   * int innerEnd = innerStart + innerLength;
   * int[] innerShape = new int[] {innerLength};
   * switch (dataType) {
   * case CHAR: {
   * byte[] array = data.getBdata(index).toByteArray();
   * storage[index] = Array.factory(dataType, innerShape, convertByteToChar(array));
   * break;
   * }
   * case ENUM1:
   * case UBYTE:
   * case BYTE: {
   * byte[] array = data.getBdata(index).toByteArray();
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case SHORT: {
   * int i = 0;
   * short[] array = new short[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = (short) data.getIdata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case INT: {
   * int i = 0;
   * int[] array = new int[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = data.getIdata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case ENUM2:
   * case USHORT: {
   * int i = 0;
   * short[] array = new short[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = (short) data.getUidata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case ENUM4:
   * case UINT: {
   * int i = 0;
   * int[] array = new int[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = data.getUidata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case LONG: {
   * int i = 0;
   * long[] array = new long[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = data.getLdata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case ULONG: {
   * int i = 0;
   * long[] array = new long[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = data.getUldata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case FLOAT: {
   * int i = 0;
   * float[] array = new float[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = data.getFdata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case DOUBLE: {
   * int i = 0;
   * double[] array = new double[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = data.getDdata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case STRING: {
   * int i = 0;
   * Object[] array = new Object[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * array[i++] = data.getSdata(innerIndex);
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * case OPAQUE: {
   * int i = 0;
   * Object[] array = new Object[innerLength];
   * for (int innerIndex = innerStart; innerIndex < innerEnd; innerIndex++) {
   * ByteString val = data.getBdata(innerIndex);
   * array[i++] = ByteBuffer.wrap(val.toByteArray());
   * }
   * storage[index] = Array.factory(dataType, innerShape, array);
   * break;
   * }
   * default:
   * throw new IllegalStateException("Unkown datatype " + dataType);
   * }
   * innerStart = innerEnd;
   * }
   * return Array.makeVlenArray(shape, storage);
   * }
   */

  ////////////////////////////////////////////////////////////////

  public static CdmRemoteProto.DataType convertDataType(DataType dtype) {
    switch (dtype) {
      case CHAR:
        return CdmRemoteProto.DataType.CHAR;
      case BYTE:
        return CdmRemoteProto.DataType.BYTE;
      case SHORT:
        return CdmRemoteProto.DataType.SHORT;
      case INT:
        return CdmRemoteProto.DataType.INT;
      case LONG:
        return CdmRemoteProto.DataType.LONG;
      case FLOAT:
        return CdmRemoteProto.DataType.FLOAT;
      case DOUBLE:
        return CdmRemoteProto.DataType.DOUBLE;
      case STRING:
        return CdmRemoteProto.DataType.STRING;
      case STRUCTURE:
        return CdmRemoteProto.DataType.STRUCTURE;
      case SEQUENCE:
        return CdmRemoteProto.DataType.SEQUENCE;
      case ENUM1:
        return CdmRemoteProto.DataType.ENUM1;
      case ENUM2:
        return CdmRemoteProto.DataType.ENUM2;
      case ENUM4:
        return CdmRemoteProto.DataType.ENUM4;
      case OPAQUE:
        return CdmRemoteProto.DataType.OPAQUE;
      case UBYTE:
        return CdmRemoteProto.DataType.UBYTE;
      case USHORT:
        return CdmRemoteProto.DataType.USHORT;
      case UINT:
        return CdmRemoteProto.DataType.UINT;
      case ULONG:
        return CdmRemoteProto.DataType.ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  public static DataType convertDataType(CdmRemoteProto.DataType dtype) {
    switch (dtype) {
      case CHAR:
        return DataType.CHAR;
      case BYTE:
        return DataType.BYTE;
      case SHORT:
        return DataType.SHORT;
      case INT:
        return DataType.INT;
      case LONG:
        return DataType.LONG;
      case FLOAT:
        return DataType.FLOAT;
      case DOUBLE:
        return DataType.DOUBLE;
      case STRING:
        return DataType.STRING;
      case STRUCTURE:
        return DataType.STRUCTURE;
      case SEQUENCE:
        return DataType.SEQUENCE;
      case ENUM1:
        return DataType.ENUM1;
      case ENUM2:
        return DataType.ENUM2;
      case ENUM4:
        return DataType.ENUM4;
      case OPAQUE:
        return DataType.OPAQUE;
      case UBYTE:
        return DataType.UBYTE;
      case USHORT:
        return DataType.USHORT;
      case UINT:
        return DataType.UINT;
      case ULONG:
        return DataType.ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  /*
   * public static ArrayStructure decodeArrayStructureData(Data arrayStructureProto, Section section) {
   * int nrows = arrayStructureProto.getRowsCount();
   * Preconditions.checkArgument(nrows > 0);
   * Preconditions.checkArgument(section.getSize() == nrows);
   * 
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
   * for (CdmRemoteProto.StructureDataProto row : arrayStructureProto.getRowsList()) {
   * result.setStructureData(decodeStructureData(row, members), index);
   * index++;
   * }
   * return result;
   * }
   * 
   * public static StructureData decodeStructureData(CdmRemoteProto.StructureDataProto structDataProto,
   * StructureMembers members) {
   * StructureDataW sdata = new StructureDataW(members);
   * for (int i = 0; i < structDataProto.getStructDataCount(); i++) {
   * Data data = structDataProto.getStructData(i);
   * Member member = members.getMember(i);
   * sdata.setMemberData(member, decodeData(data, new Section(member.getShape())));
   * }
   * return sdata;
   * }
   */

}
