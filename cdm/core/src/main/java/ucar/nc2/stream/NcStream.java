/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import com.google.protobuf.ByteString;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureBB;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataDeep;
import ucar.ma2.StructureMembers;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.stream.NcStreamProto.Variable.Builder;
import ucar.unidata.io.RandomAccessFile;

/**
 * Defines the ncstream format, along with ncStream.proto.
 *
 * @see "https://www.unidata.ucar.edu/software/netcdf-java/stream/NcStream.html"
 * @see "https://www.unidata.ucar.edu/software/netcdf-java/stream/NcstreamGrammer.html"
 */
public class NcStream {
  // must start with this "CDFS"
  public static final byte[] MAGIC_START = {0x43, 0x44, 0x46, 0x53};

  public static final byte[] MAGIC_HEADER = {(byte) 0xad, (byte) 0xec, (byte) 0xce, (byte) 0xda};
  public static final byte[] MAGIC_DATA = {(byte) 0xab, (byte) 0xec, (byte) 0xce, (byte) 0xba};
  public static final byte[] MAGIC_DATA2 = {(byte) 0xab, (byte) 0xeb, (byte) 0xbe, (byte) 0xba};
  public static final byte[] MAGIC_VDATA = {(byte) 0xab, (byte) 0xef, (byte) 0xfe, (byte) 0xba};
  public static final byte[] MAGIC_VEND = {(byte) 0xed, (byte) 0xef, (byte) 0xfe, (byte) 0xda};

  public static final byte[] MAGIC_HEADERCOV = {(byte) 0xad, (byte) 0xed, (byte) 0xde, (byte) 0xda};
  public static final byte[] MAGIC_DATACOV = {(byte) 0xab, (byte) 0xed, (byte) 0xde, (byte) 0xba};

  public static final byte[] MAGIC_ERR = {(byte) 0xab, (byte) 0xad, (byte) 0xba, (byte) 0xda};
  public static final byte[] MAGIC_END = {(byte) 0xed, (byte) 0xed, (byte) 0xde, (byte) 0xde};

  public static final int ncstream_data_version = 3;

  static NcStreamProto.Group.Builder encodeGroup(Group g, int sizeToCache) throws IOException {
    NcStreamProto.Group.Builder groupBuilder = NcStreamProto.Group.newBuilder();
    groupBuilder.setName(g.getShortName());

    for (Dimension dim : g.getDimensions())
      groupBuilder.addDims(NcStream.encodeDim(dim));

    for (Attribute att : g.attributes())
      groupBuilder.addAtts(NcStream.encodeAtt(att));

    for (EnumTypedef enumType : g.getEnumTypedefs())
      groupBuilder.addEnumTypes(NcStream.encodeEnumTypedef(enumType));

    for (Variable var : g.getVariables()) {
      if (var instanceof Structure)
        groupBuilder.addStructs(NcStream.encodeStructure((Structure) var));
      else
        groupBuilder.addVars(NcStream.encodeVar(var, sizeToCache));
    }

    for (Group ng : g.getGroups())
      groupBuilder.addGroups(encodeGroup(ng, sizeToCache));

    return groupBuilder;
  }


  public static NcStreamProto.Attribute.Builder encodeAtt(Attribute att) {
    NcStreamProto.Attribute.Builder attBuilder = NcStreamProto.Attribute.newBuilder();
    attBuilder.setName(att.getShortName());
    attBuilder.setDataType(convertDataType(att.getDataType()));
    attBuilder.setLen(att.getLength());

    // values
    if (att.getLength() > 0) {
      if (att.isString()) {
        for (int i = 0; i < att.getLength(); i++)
          attBuilder.addSdata(att.getStringValue(i));

      } else {
        Array data = att.getValues();
        ByteBuffer bb = data.getDataAsByteBuffer();
        attBuilder.setData(ByteString.copyFrom(bb.array()));
      }
    }

    return attBuilder;
  }

  private static NcStreamProto.Dimension.Builder encodeDim(Dimension dim) {
    NcStreamProto.Dimension.Builder dimBuilder = NcStreamProto.Dimension.newBuilder();
    if (dim.getShortName() != null)
      dimBuilder.setName(dim.getShortName());
    if (!dim.isVariableLength())
      dimBuilder.setLength(dim.getLength());
    dimBuilder.setIsPrivate(!dim.isShared());
    dimBuilder.setIsVlen(dim.isVariableLength());
    dimBuilder.setIsUnlimited(dim.isUnlimited());
    return dimBuilder;
  }

  private static NcStreamProto.EnumTypedef.Builder encodeEnumTypedef(EnumTypedef enumType) {
    NcStreamProto.EnumTypedef.Builder builder = NcStreamProto.EnumTypedef.newBuilder();

    builder.setName(enumType.getShortName());
    Map<Integer, String> map = enumType.getMap();
    NcStreamProto.EnumTypedef.EnumType.Builder b2 = NcStreamProto.EnumTypedef.EnumType.newBuilder();
    for (int code : map.keySet()) {
      b2.clear();
      b2.setCode(code);
      b2.setValue(map.get(code));
      builder.addMap(b2);
    }
    return builder;
  }

  private static Builder encodeVar(Variable var, int sizeToCache) throws IOException {
    Builder builder = NcStreamProto.Variable.newBuilder();
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
        ByteBuffer bb = data.getDataAsByteBuffer();
        builder.setData(ByteString.copyFrom(bb.array()));
      }
    }

    return builder;
  }

  private static NcStreamProto.Structure.Builder encodeStructure(Structure s) throws IOException {
    NcStreamProto.Structure.Builder builder = NcStreamProto.Structure.newBuilder();
    builder.setName(s.getShortName());
    builder.setDataType(convertDataType(s.getDataType()));

    for (Dimension dim : s.getDimensions())
      builder.addShape(encodeDim(dim));

    for (Attribute att : s.attributes())
      builder.addAtts(encodeAtt(att));

    for (Variable v : s.getVariables()) {
      if (v instanceof Structure)
        builder.addStructs(NcStream.encodeStructure((Structure) v));
      else
        builder.addVars(NcStream.encodeVar(v, -1));
    }

    return builder;
  }

  public static NcStreamProto.Error encodeErrorMessage(String message) {
    NcStreamProto.Error.Builder builder = NcStreamProto.Error.newBuilder();
    builder.setMessage(message);
    return builder.build();
  }

  static NcStreamProto.Data encodeDataProto(Variable var, Section section, NcStreamProto.Compress compressionType,
      ByteOrder bo, int uncompressedLength) {
    NcStreamProto.Data.Builder builder = NcStreamProto.Data.newBuilder();
    builder.setVarName(NetcdfFiles.makeFullName(var));
    builder.setDataType(convertDataType(var.getDataType()));
    builder.setSection(encodeSection(section));
    builder.setCompress(compressionType);
    if (compressionType != NcStreamProto.Compress.NONE) {
      builder.setUncompressedSize(uncompressedLength);
    }
    builder.setVdata(var.isVariableLength());
    builder.setBigend(bo == ByteOrder.BIG_ENDIAN);
    builder.setVersion(ncstream_data_version);
    return builder.build();
  }

  public static NcStreamProto.Data encodeDataProto(String varname, DataType datatype, Section section, boolean deflate,
      int uncompressedLength) {
    NcStreamProto.Data.Builder builder = NcStreamProto.Data.newBuilder();
    builder.setVarName(varname);
    builder.setDataType(convertDataType(datatype));
    builder.setSection(encodeSection(section));
    if (deflate) {
      builder.setCompress(NcStreamProto.Compress.DEFLATE);
      builder.setUncompressedSize(uncompressedLength);
    }
    builder.setVersion(ncstream_data_version);
    return builder.build();
  }

  public static NcStreamProto.Section encodeSection(Section section) {
    NcStreamProto.Section.Builder sbuilder = NcStreamProto.Section.newBuilder();
    for (Range r : section.getRanges()) {
      NcStreamProto.Range.Builder rbuilder = NcStreamProto.Range.newBuilder();
      rbuilder.setStart(r.first());
      rbuilder.setSize(r.length());
      rbuilder.setStride(r.stride());
      sbuilder.addRange(rbuilder);
    }
    return sbuilder.build();
  }

  ////////////////////////////////////////////////////////////

  static int writeByte(OutputStream out, byte b) throws IOException {
    out.write(b);
    return 1;
  }

  static int writeBytes(OutputStream out, byte[] b, int offset, int length) throws IOException {
    out.write(b, offset, length);
    return length;
  }

  public static int writeBytes(OutputStream out, byte[] b) throws IOException {
    return writeBytes(out, b, 0, b.length);
  }

  public static int writeString(OutputStream out, String s) throws IOException {
    int vsize = NcStream.writeVInt(out, s.length());
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    out.write(b);
    return vsize + b.length;
  }

  public static int writeByteBuffer(OutputStream out, ByteBuffer bb) throws IOException {
    int vsize = NcStream.writeVInt(out, bb.limit());
    bb.rewind();
    out.write(bb.array());
    return vsize + bb.limit();
  }

  public static int writeVInt(OutputStream out, int value) throws IOException {
    int count = 0;

    // stolen from protobuf.CodedOutputStream.writeRawVarint32()
    while (true) {
      if ((value & ~0x7F) == 0) {
        writeByte(out, (byte) value);
        break;
      } else {
        writeByte(out, (byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    return count + 1;
  }

  public static int writeVInt(RandomAccessFile out, int value) throws IOException {
    int count = 0;

    while (true) {
      if ((value & ~0x7F) == 0) {
        out.write((byte) value);
        break;
      } else {
        out.write((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    return count + 1;
  }


  public static int writeVInt(WritableByteChannel wbc, int value) throws IOException {
    ByteBuffer bb = ByteBuffer.allocate(8);

    while (true) {
      if ((value & ~0x7F) == 0) {
        bb.put((byte) value);
        break;
      } else {
        bb.put((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }

    bb.flip();
    wbc.write(bb);
    return bb.limit();
  }

  public static int writeVLong(OutputStream out, long i) throws IOException {
    int count = 0;
    while ((i & ~0x7F) != 0) {
      writeByte(out, (byte) ((i & 0x7f) | 0x80));
      i >>>= 7;
      count++;
    }
    writeByte(out, (byte) i);
    return count + 1;
  }

  public static ByteBuffer readByteBuffer(InputStream in) throws IOException {
    int vsize = NcStream.readVInt(in);
    byte[] b = new byte[vsize];
    readFully(in, b);
    return ByteBuffer.wrap(b);
  }

  public static String readString(InputStream in) throws IOException {
    int vsize = NcStream.readVInt(in);
    byte[] b = new byte[vsize];
    readFully(in, b);
    return new String(b, StandardCharsets.UTF_8);
  }

  public static int readVInt(InputStream is) throws IOException {
    int ib = is.read();
    if (ib == -1)
      return -1;

    byte b = (byte) ib;
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      ib = is.read();
      if (ib == -1)
        return -1;
      b = (byte) ib;
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  public static int readVInt(RandomAccessFile raf) throws IOException {
    int ib = raf.read();
    if (ib == -1)
      return -1;

    byte b = (byte) ib;
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      ib = raf.read();
      if (ib == -1)
        return -1;
      b = (byte) ib;
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  public static int readFully(InputStream is, byte[] b) throws IOException {
    int done = 0;
    int want = b.length;
    while (want > 0) {
      int bytesRead = is.read(b, done, want);
      if (bytesRead == -1)
        break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  public static boolean readAndTest(InputStream is, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    readFully(is, b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i])
        return false;
    return true;
  }

  public static boolean readAndTest(RandomAccessFile raf, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    raf.readFully(b);
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i])
        return false;
    return true;
  }

  public static boolean test(byte[] b, byte[] test) {
    if (b.length != test.length)
      return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i])
        return false;
    return true;
  }

  public static String decodeErrorMessage(NcStreamProto.Error err) {
    return err.getMessage();
  }

  private static Dimension decodeDim(NcStreamProto.Dimension dim) {
    String name = (dim.getName().isEmpty() ? null : dim.getName());
    int dimLen = dim.getIsVlen() ? -1 : (int) dim.getLength();
    return Dimension.builder().setName(name).setIsShared(!dim.getIsPrivate()).setIsUnlimited(dim.getIsUnlimited())
        .setIsVariableLength(dim.getIsVlen()).setLength(dimLen).build();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static void readGroup(NcStreamProto.Group proto, Group.Builder g) {

    for (NcStreamProto.Dimension dim : proto.getDimsList())
      g.addDimension(NcStream.decodeDim(dim)); // always added to group? what if private ??

    for (NcStreamProto.Attribute att : proto.getAttsList())
      g.addAttribute(NcStream.decodeAtt(att));

    for (NcStreamProto.EnumTypedef enumType : proto.getEnumTypesList())
      g.addEnumTypedef(NcStream.decodeEnumTypedef(enumType));

    for (NcStreamProto.Variable var : proto.getVarsList())
      g.addVariable(NcStream.decodeVar(var));

    for (NcStreamProto.Structure s : proto.getStructsList())
      g.addVariable(NcStream.decodeStructure(s));

    for (NcStreamProto.Group gp : proto.getGroupsList()) {
      Group.Builder ng = Group.builder().setName(gp.getName());
      g.addGroup(ng);
      readGroup(gp, ng);
    }
  }

  private static EnumTypedef decodeEnumTypedef(NcStreamProto.EnumTypedef enumType) {
    List<NcStreamProto.EnumTypedef.EnumType> list = enumType.getMapList();
    Map<Integer, String> map = new HashMap<>(2 * list.size());
    for (NcStreamProto.EnumTypedef.EnumType et : list) {
      map.put(et.getCode(), et.getValue());
    }
    return new EnumTypedef(enumType.getName(), map);
  }

  public static Attribute decodeAtt(NcStreamProto.Attribute attp) {
    // BARF LOOK
    DataType dtOld = decodeAttributeType(attp.getType());
    DataType dtNew = convertDataType(attp.getDataType());
    DataType dtUse;
    if (dtNew != DataType.CHAR)
      dtUse = dtNew;
    else if (dtOld != DataType.STRING)
      dtUse = dtOld;
    else if (attp.getSdataCount() > 0)
      dtUse = DataType.STRING;
    else
      dtUse = DataType.CHAR;

    int len = attp.getLen();
    if (len == 0) // deal with empty attribute
      return Attribute.builder(attp.getName()).setDataType(dtUse).build();

    if (dtUse == DataType.STRING) {
      int lenp = attp.getSdataCount();
      if (lenp == 1)
        return new Attribute(attp.getName(), attp.getSdata(0));
      else {
        Array data = Array.factory(dtUse, new int[] {lenp});
        for (int i = 0; i < lenp; i++)
          data.setObject(i, attp.getSdata(i));
        return Attribute.fromArray(attp.getName(), data);
      }
    } else {
      ByteString bs = attp.getData();
      ByteBuffer bb = ByteBuffer.wrap(bs.toByteArray());
      // if null, then use int[]{bb.limit()}
      return Attribute.fromArray(attp.getName(), Array.factory(dtUse, (int[]) null, bb));
    }
  }

  private static Variable.Builder decodeVar(NcStreamProto.Variable var) {
    DataType varType = convertDataType(var.getDataType());
    Variable.Builder ncvar = Variable.builder().setName(var.getName()).setDataType(varType);

    if (varType.isEnum()) {
      ncvar.setEnumTypeName(var.getEnumType());
    }

    // The Dimensions are stored redunantly in the Variable.
    // If shared, they must also exist in a parent Group. However, we dont yet have the Groups wired together,
    // so that has to wait until build().
    List<Dimension> dims = new ArrayList<>(6);
    Section.Builder section = Section.builder();
    for (ucar.nc2.stream.NcStreamProto.Dimension dim : var.getShapeList()) {
      dims.add(decodeDim(dim));
      section.appendRange((int) dim.getLength());
    }
    ncvar.addDimensions(dims);

    for (ucar.nc2.stream.NcStreamProto.Attribute att : var.getAttsList())
      ncvar.addAttribute(decodeAtt(att));

    if (!var.getData().isEmpty()) {
      // LOOK may mess with ability to change var size later.
      ByteBuffer bb = ByteBuffer.wrap(var.getData().toByteArray());
      Array data = Array.factory(varType, section.build().getShape(), bb);
      ncvar.setSourceData(data);
    }

    return ncvar;
  }

  private static Structure.Builder decodeStructure(NcStreamProto.Structure s) {
    Structure.Builder ncvar =
        (s.getDataType() == ucar.nc2.stream.NcStreamProto.DataType.SEQUENCE) ? Sequence.builder() : Structure.builder();

    ncvar.setName(s.getName()).setDataType(convertDataType(s.getDataType()));

    List<Dimension> dims = new ArrayList<>(6);
    for (ucar.nc2.stream.NcStreamProto.Dimension dim : s.getShapeList()) {
      dims.add(decodeDim(dim));
    }
    ncvar.addDimensions(dims);

    for (ucar.nc2.stream.NcStreamProto.Attribute att : s.getAttsList()) {
      ncvar.addAttribute(decodeAtt(att));
    }

    for (ucar.nc2.stream.NcStreamProto.Variable vp : s.getVarsList()) {
      ncvar.addMemberVariable(decodeVar(vp));
    }

    for (NcStreamProto.Structure sp : s.getStructsList()) {
      ncvar.addMemberVariable(decodeStructure(sp));
    }

    return ncvar;
  }

  @Nonnull
  public static Section decodeSection(NcStreamProto.Section proto) {
    Section.Builder section = Section.builder();

    for (ucar.nc2.stream.NcStreamProto.Range pr : proto.getRangeList()) {
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

  /*
   * decodeDataByteOrder
   * 
   * proto2:
   * message Data {
   * required string varName = 1; // full escaped name.
   * required DataType dataType = 2;
   * optional Section section = 3; // not required for SEQUENCE
   * optional bool bigend = 4 [default = true];
   * optional uint32 version = 5 [default = 0];
   * optional Compress compress = 6 [default = NONE];
   * optional bool vdata = 7 [default = false];
   * optional uint32 uncompressedSize = 8;
   * }
   * 
   * problem is that bigend default is true, but in proto3 it must be false. so we need to detect if the value is set or
   * not.
   * thanks to Simon (Vsevolod) Ilyushchenko <simonf@google.com>, workaround is:
   * 
   * proto3:
   * message Data {
   * string varName = 1; // full escaped name.
   * DataType dataType = 2;
   * Section section = 3; // not required for SEQUENCE
   * oneof bigend_present {
   * bool bigend = 4; // [default=true] in proto2
   * }
   * uint32 version = 5; // < 3 for proto2, = 3 for proto3 (v5.0+)
   * Compress compress = 6;
   * bool vdata = 7;
   * uint32 uncompressedSize = 8;
   * }
   * 
   * which is wire-compatible and allows us to detect if value is set or not.
   */
  static ByteOrder decodeDataByteOrder(NcStreamProto.Data pData) {
    boolean isMissing = pData.getBigendPresentCase() == NcStreamProto.Data.BigendPresentCase.BIGENDPRESENT_NOT_SET;
    if (isMissing) {
      int version = pData.getVersion();
      return (version < 3) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }
    return pData.getBigend() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
  }

  ////////////////////////////////////////////////////////////////

  public static ucar.nc2.stream.NcStreamProto.DataType convertDataType(DataType dtype) {
    switch (dtype) {
      case CHAR:
        return ucar.nc2.stream.NcStreamProto.DataType.CHAR;
      case BYTE:
        return ucar.nc2.stream.NcStreamProto.DataType.BYTE;
      case SHORT:
        return ucar.nc2.stream.NcStreamProto.DataType.SHORT;
      case INT:
        return ucar.nc2.stream.NcStreamProto.DataType.INT;
      case LONG:
        return ucar.nc2.stream.NcStreamProto.DataType.LONG;
      case FLOAT:
        return ucar.nc2.stream.NcStreamProto.DataType.FLOAT;
      case DOUBLE:
        return ucar.nc2.stream.NcStreamProto.DataType.DOUBLE;
      case STRING:
        return ucar.nc2.stream.NcStreamProto.DataType.STRING;
      case STRUCTURE:
        return ucar.nc2.stream.NcStreamProto.DataType.STRUCTURE;
      case SEQUENCE:
        return ucar.nc2.stream.NcStreamProto.DataType.SEQUENCE;
      case ENUM1:
        return ucar.nc2.stream.NcStreamProto.DataType.ENUM1;
      case ENUM2:
        return ucar.nc2.stream.NcStreamProto.DataType.ENUM2;
      case ENUM4:
        return ucar.nc2.stream.NcStreamProto.DataType.ENUM4;
      case OPAQUE:
        return ucar.nc2.stream.NcStreamProto.DataType.OPAQUE;
      case UBYTE:
        return ucar.nc2.stream.NcStreamProto.DataType.UBYTE;
      case USHORT:
        return ucar.nc2.stream.NcStreamProto.DataType.USHORT;
      case UINT:
        return ucar.nc2.stream.NcStreamProto.DataType.UINT;
      case ULONG:
        return ucar.nc2.stream.NcStreamProto.DataType.ULONG;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  public static DataType convertDataType(ucar.nc2.stream.NcStreamProto.DataType dtype) {
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

  /////////////////////
  // < 5.0

  private static DataType decodeAttributeType(ucar.nc2.stream.NcStreamProto.Attribute.Type dtype) {
    switch (dtype) {
      case STRING:
        return DataType.STRING;
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
    }
    throw new IllegalStateException("illegal att type " + dtype);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  static long encodeArrayStructure(ArrayStructure as, ByteOrder bo, OutputStream os) throws java.io.IOException {
    long size = 0;

    ArrayStructureBB dataBB = StructureDataDeep.copyToArrayBB(as, bo, true); // force canonical packing
    List<String> ss = new ArrayList<>();
    List<Object> heap = dataBB.getHeap();
    List<Integer> count = new ArrayList<>();
    if (heap != null) {
      for (Object ho : heap) {
        if (ho instanceof String) {
          count.add(1);
          ss.add((String) ho);
        } else if (ho instanceof String[]) {
          String[] hos = (String[]) ho;
          count.add(hos.length);
          ss.addAll(Arrays.asList(hos));
        }
      }
    }

    // LOOK optionally compress
    StructureMembers sm = dataBB.getStructureMembers();
    ByteBuffer bb = dataBB.getByteBuffer();
    NcStreamProto.StructureData proto =
        NcStream.encodeStructureDataProto(bb.array(), count, ss, (int) as.getSize(), sm.getStructureSize());
    byte[] datab = proto.toByteArray();
    size += NcStream.writeVInt(os, datab.length); // proto len
    os.write(datab); // proto
    size += datab.length;

    return size;
  }

  private static NcStreamProto.StructureData encodeStructureDataProto(byte[] fixed, List<Integer> count,
      List<String> ss, int nrows, int rowLength) {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.setData(ByteString.copyFrom(fixed));
    builder.setNrows(nrows);
    builder.setRowLength(rowLength);
    for (Integer c : count)
      builder.addHeapCount(c);
    for (String s : ss)
      builder.addSdata(s);
    return builder.build();
  }

  static ArrayStructureBB decodeArrayStructure(StructureMembers sm, int[] shape, byte[] proto)
      throws java.io.IOException {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.mergeFrom(proto);

    ByteBuffer bb = ByteBuffer.wrap(builder.getData().toByteArray());
    ArrayStructureBB dataBB = new ArrayStructureBB(sm, shape, bb, 0);

    List<String> ss = builder.getSdataList();
    List<Integer> count = builder.getHeapCountList();

    int scount = 0;
    for (Integer c : count) {
      if (c == 1) {
        dataBB.addObjectToHeap(ss.get(scount++));
      } else {
        String[] hos = new String[c];
        for (int i = 0; i < c; i++)
          hos[i] = ss.get(scount++);
        dataBB.addObjectToHeap(hos);
      }
    }

    return dataBB;
  }

  public static StructureData decodeStructureData(StructureMembers sm, ByteOrder bo, byte[] proto)
      throws java.io.IOException {
    NcStreamProto.StructureData.Builder builder = NcStreamProto.StructureData.newBuilder();
    builder.mergeFrom(proto);

    ByteBuffer bb = ByteBuffer.wrap(builder.getData().toByteArray());
    bb.order(bo);
    ArrayStructureBB dataBB = new ArrayStructureBB(sm, new int[] {1}, bb, 0);

    List<String> ss = builder.getSdataList();
    List<Integer> count = builder.getHeapCountList();

    int scount = 0;
    for (Integer c : count) {
      if (c == 1) {
        dataBB.addObjectToHeap(ss.get(scount++));
      } else {
        String[] hos = new String[c];
        for (int i = 0; i < c; i++)
          hos[i] = ss.get(scount++);
        dataBB.addObjectToHeap(hos);
      }
    }

    return dataBB.getStructureData(0);
  }

}
