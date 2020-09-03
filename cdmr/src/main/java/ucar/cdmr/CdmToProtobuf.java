/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr;

import static ucar.nc2.iosp.IospHelper.convertByteToChar;
import static ucar.nc2.iosp.IospHelper.convertCharToByte;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import ucar.cdmr.CdmRemoteProto.Data;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
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
public class CdmToProtobuf {
  public static CdmRemoteProto.Group.Builder encodeGroup(Group g, int sizeToCache) throws IOException {
    CdmRemoteProto.Group.Builder groupBuilder = CdmRemoteProto.Group.newBuilder();
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

  public static CdmRemoteProto.Attribute.Builder encodeAtt(Attribute att) {
    CdmRemoteProto.Attribute.Builder attBuilder = CdmRemoteProto.Attribute.newBuilder();
    attBuilder.setName(att.getShortName());
    attBuilder.setDataType(convertDataType(att.getDataType()));
    attBuilder.setLength(att.getLength());

    // values
    if (att.getLength() > 0) {
      if (att.isString()) {
        CdmRemoteProto.Data.Builder datab = CdmRemoteProto.Data.newBuilder();
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

  private static CdmRemoteProto.Dimension.Builder encodeDim(Dimension dim) {
    CdmRemoteProto.Dimension.Builder dimBuilder = CdmRemoteProto.Dimension.newBuilder();
    if (dim.getShortName() != null)
      dimBuilder.setName(dim.getShortName());
    if (!dim.isVariableLength())
      dimBuilder.setLength(dim.getLength());
    dimBuilder.setIsPrivate(!dim.isShared());
    dimBuilder.setIsVlen(dim.isVariableLength());
    dimBuilder.setIsUnlimited(dim.isUnlimited());
    return dimBuilder;
  }

  private static CdmRemoteProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) {
    CdmRemoteProto.EnumTypedef.Builder builder = CdmRemoteProto.EnumTypedef.newBuilder();

    builder.setName(enumType.getShortName());
    Map<Integer, String> map = enumType.getMap();
    CdmRemoteProto.EnumTypedef.EnumType.Builder b2 = CdmRemoteProto.EnumTypedef.EnumType.newBuilder();
    for (int code : map.keySet()) {
      b2.clear();
      b2.setCode(code);
      b2.setValue(map.get(code));
      builder.addMap(b2);
    }
    return builder;
  }

  private static CdmRemoteProto.Variable.Builder encodeVar(Variable var, int sizeToCache) throws IOException {
    CdmRemoteProto.Variable.Builder builder = CdmRemoteProto.Variable.newBuilder();
    builder.setName(var.getShortName());
    builder.setDataType(convertDataType(var.getDataType()));
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
    if (var.isCaching() && var.getDataType().isNumeric()) {
      if (var.isCoordinateVariable() || var.getSize() * var.getElementSize() < sizeToCache) {
        Array data = var.read();
        builder.setData(encodeData(var.getDataType(), data));
      }
    }

    return builder;
  }

  private static CdmRemoteProto.Structure.Builder encodeStructure(Structure s) throws IOException {
    CdmRemoteProto.Structure.Builder builder = CdmRemoteProto.Structure.newBuilder();
    builder.setName(s.getShortName());
    builder.setDataType(convertDataType(s.getDataType()));

    for (Dimension dim : s.getDimensions())
      builder.addShape(encodeDim(dim));

    for (Attribute att : s.attributes())
      builder.addAtts(encodeAtt(att));

    for (Variable v : s.getVariables()) {
      if (v instanceof Structure)
        builder.addStructs(CdmToProtobuf.encodeStructure((Structure) v));
      else
        builder.addVars(encodeVar(v, -1));
    }

    return builder;
  }

  public static CdmRemoteProto.Error encodeErrorMessage(String message) {
    CdmRemoteProto.Error.Builder builder = CdmRemoteProto.Error.newBuilder();
    builder.setMessage(message);
    return builder.build();
  }

  public static CdmRemoteProto.Data encodeData(DataType dataType, Array data) {
    CdmRemoteProto.Data.Builder builder = CdmRemoteProto.Data.newBuilder();
    builder.setDataType(convertDataType(dataType));
    IndexIterator iiter = data.getIndexIterator();
    switch (dataType) {
      case CHAR:
        byte[] cdata = convertCharToByte((char[]) data.get1DJavaArray(DataType.CHAR));
        builder.setBdata(ByteString.copyFrom(cdata));
        break;
      case ENUM1:
      case BYTE:
        byte[] bdata = (byte[]) data.get1DJavaArray(DataType.BYTE);
        builder.setBdata(ByteString.copyFrom(bdata));
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
    }
    return builder.build();
  }

  public static CdmRemoteProto.Section encodeSection(Section section) {
    CdmRemoteProto.Section.Builder sbuilder = CdmRemoteProto.Section.newBuilder();
    for (Range r : section.getRanges()) {
      CdmRemoteProto.Range.Builder rbuilder = CdmRemoteProto.Range.newBuilder();
      rbuilder.setStart(r.first());
      rbuilder.setSize(r.length());
      rbuilder.setStride(r.stride());
      sbuilder.addRange(rbuilder);
    }
    return sbuilder.build();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static Dimension decodeDim(CdmRemoteProto.Dimension dim) {
    String name = (dim.getName().isEmpty() ? null : dim.getName());
    int dimLen = dim.getIsVlen() ? -1 : (int) dim.getLength();
    return Dimension.builder().setName(name).setIsShared(!dim.getIsPrivate()).setIsUnlimited(dim.getIsUnlimited())
        .setIsVariableLength(dim.getIsVlen()).setLength(dimLen).build();
  }

  public static void decodeGroup(CdmRemoteProto.Group proto, Group.Builder g) {

    for (CdmRemoteProto.Dimension dim : proto.getDimsList())
      g.addDimension(CdmToProtobuf.decodeDim(dim)); // always added to group? what if private ??

    for (CdmRemoteProto.Attribute att : proto.getAttsList())
      g.addAttribute(CdmToProtobuf.decodeAtt(att));

    for (CdmRemoteProto.EnumTypedef enumType : proto.getEnumTypesList())
      g.addEnumTypedef(CdmToProtobuf.decodeEnumTypedef(enumType));

    for (CdmRemoteProto.Variable var : proto.getVarsList())
      g.addVariable(CdmToProtobuf.decodeVar(var));

    for (CdmRemoteProto.Structure s : proto.getStructsList())
      g.addVariable(CdmToProtobuf.decodeStructure(s));

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
    return new EnumTypedef(enumType.getName(), map);
  }

  public static Attribute decodeAtt(CdmRemoteProto.Attribute attp) {
    DataType dtUse = convertDataType(attp.getDataType());
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
      Array array = decodeData(attp.getData(), new int[] {len});
      return Attribute.fromArray(attp.getName(), array);
    }
  }

  private static Variable.Builder<?> decodeVar(CdmRemoteProto.Variable var) {
    DataType varType = convertDataType(var.getDataType());
    Variable.Builder<?> ncvar = Variable.builder().setName(var.getName()).setDataType(varType);

    if (varType.isEnum()) {
      ncvar.setEnumTypeName(var.getEnumType());
    }

    // The Dimensions are stored redunantly in the Variable.
    // If shared, they must also exist in a parent Group. However, we dont yet have the Groups wired together,
    // so that has to wait until build().
    List<Dimension> dims = new ArrayList<>(6);
    Section.Builder section = Section.builder();
    for (CdmRemoteProto.Dimension dim : var.getShapeList()) {
      dims.add(decodeDim(dim));
      section.appendRange((int) dim.getLength());
    }
    ncvar.addDimensions(dims);

    for (CdmRemoteProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    if (var.hasData()) {
      int[] shape = section.build().getShape();
      Array data = decodeData(var.getData(), shape);
      ncvar.setCachedData(data, true);
    }

    return ncvar;
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
      ncvar.addMemberVariable(decodeVar(vp));
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
        throw new RuntimeException("Bad Section in ncstream", e);
      }
    }
    return section.build();
  }

  // Note that this converts to Objects, so not very efficient ??
  public static Array decodeData(CdmRemoteProto.Data data, int[] shape) {
    DataType dataType = convertDataType(data.getDataType());
    switch (dataType) {
      case CHAR: {
        byte[] array = data.getBdata().toByteArray();
        return Array.factory(dataType, shape, convertByteToChar(array));
      }
      case ENUM1:
      case BYTE: {
        byte[] array = data.getBdata().toByteArray();
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

      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
  }

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

}
