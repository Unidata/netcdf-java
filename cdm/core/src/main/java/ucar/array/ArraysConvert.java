/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import ucar.array.StructureMembers.Member;

import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.StructureDataW;

/** Static helper classes for converting {@link ucar.array.Array} to/from {@link ucar.ma2.Array}. */
public class ArraysConvert {

  public static Array<?> convertToArray(ucar.ma2.Array from) {
    ArrayType dtype = from.getDataType().getArrayType();
    if (dtype == ArrayType.OPAQUE) {
      return convertOpaque(from);

    } else if (from.isVlen()) {
      return convertVlen(from);

    } else if (dtype == ArrayType.STRING) {
      String[] sarray = new String[(int) from.getSize()];
      for (int idx = 0; idx < sarray.length; idx++) {
        sarray[idx] = (String) from.getObject(idx);
      }
      return Arrays.factory(dtype, from.getShape(), sarray);

    } else if (dtype == ArrayType.STRUCTURE) {
      return convertArrayStructure(from);

    } else {
      return Arrays.factory(dtype, from.getShape(), from.get1DJavaArray(from.getDataType()));
    }
  }

  // Opaque is Vlen of byte
  private static Array<?> convertOpaque(ucar.ma2.Array from) {
    ArrayType dtype = from.getDataType().getArrayType();
    Preconditions.checkArgument(dtype == ArrayType.OPAQUE);
    if (from instanceof ucar.ma2.ArrayObject) {
      ucar.ma2.ArrayObject ma2 = (ucar.ma2.ArrayObject) from;
      byte[][] dataArray = new byte[(int) ma2.getSize()][];
      for (int idx = 0; idx < ma2.getSize(); idx++) {
        ByteBuffer bb = (ByteBuffer) ma2.getObject(idx);
        bb.rewind();
        byte[] raw = new byte[bb.remaining()];
        bb.get(raw);
        dataArray[idx] = raw;
      }
      return ArrayVlen.factory(dtype, ma2.getShape(), dataArray);
    }
    throw new RuntimeException("Unimplemented opaque array class " + from.getClass().getName());
  }

  private static Array<?> convertVlen(ucar.ma2.Array from) {
    ArrayType dtype = from.getDataType().getArrayType();
    if (from instanceof ucar.ma2.ArrayObject) {
      ArrayVlen<?> result = ArrayVlen.factory(dtype, from.getShape());
      int count = 0;
      IndexIterator ii = from.getIndexIterator();
      while (ii.hasNext()) {
        Object parray = ii.getObjectNext();
        result.set(count++, parray);
      }
      return result;
    }
    throw new RuntimeException("Unimplemented vlen array class " + from.getClass().getName());
  }

  private static Array<?> convertArrayStructure(ucar.ma2.Array from) {
    if (from instanceof ArrayStructure) {
      ArrayStructure orgData = (ArrayStructure) from;
      StructureMembers members = convertMembers(orgData.getStructureMembers()).build();
      int length = (int) from.getSize();
      ByteBuffer bb = ByteBuffer.allocate(members.getStorageSizeBytes() * length);
      StructureDataStorageBB storage = new StructureDataStorageBB(members, bb, length);
      for (int row = 0; row < length; row++) {
        ucar.ma2.StructureData sdataMa2 = orgData.getStructureData(row);
        for (ucar.ma2.StructureMembers.Member ma2 : sdataMa2.getMembers()) {
          Member member = members.findMember(ma2.getName());
          int pos = row * members.getStorageSizeBytes();
          ucar.ma2.Array data = sdataMa2.getArray(ma2);
          convertNestedData(pos, member, storage, bb, data);
        }
      }
      return new StructureDataArray(members, orgData.getShape(), storage);
    }
    throw new RuntimeException("Unimplemented ArrayStructure class " + from.getClass().getName());
  }

  private static StructureMembers.Builder convertMembers(ucar.ma2.StructureMembers from) {
    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName(from.getName());
    for (ucar.ma2.StructureMembers.Member ma2 : from.getMembers()) {
      StructureMembers.MemberBuilder mb = StructureMembers.memberBuilder();
      mb.setName(ma2.getName());
      mb.setShape(ma2.getShape());
      mb.setArrayType(ma2.getDataType().getArrayType());
      mb.setUnits(ma2.getUnitsString());
      mb.setDesc(ma2.getDescription());
      if (ma2.getStructureMembers() != null) {
        mb.setStructureMembers(convertMembers(ma2.getStructureMembers()));
      }
      builder.addMember(mb);
    }
    builder.setStandardOffsets(false);
    return builder;
  }

  private static void convertNestedData(int offset, Member member, StructureDataStorageBB storage, ByteBuffer bbuffer,
      ucar.ma2.Array data) {
    int pos = offset + member.getOffset();
    bbuffer.position(pos);
    if (member.isVlen()) {
      int index = storage.putOnHeap(convertVlen(data));
      bbuffer.putInt(index);
      return;
    }

    DataType dataType = data.getDataType();
    IndexIterator indexIter = data.getIndexIterator();
    switch (dataType) {
      case ENUM1:
      case UBYTE:
      case BYTE: {
        while (indexIter.hasNext()) {
          bbuffer.put(indexIter.getByteNext());
        }
        return;
      }
      case OPAQUE: {
        int index = storage.putOnHeap(convertOpaque(data));
        bbuffer.putInt(index);
        return;
      }
      case CHAR: {
        while (indexIter.hasNext()) {
          bbuffer.put((byte) indexIter.getCharNext());
        }
        return;
      }
      case ENUM2:
      case USHORT:
      case SHORT: {
        while (indexIter.hasNext()) {
          bbuffer.putShort(indexIter.getShortNext());
        }
        return;
      }
      case ENUM4:
      case UINT:
      case INT: {
        while (indexIter.hasNext()) {
          bbuffer.putInt(indexIter.getIntNext());
        }
        return;
      }
      case ULONG:
      case LONG: {
        while (indexIter.hasNext()) {
          bbuffer.putLong(indexIter.getLongNext());
        }
        return;
      }
      case FLOAT: {
        while (indexIter.hasNext()) {
          bbuffer.putFloat(indexIter.getFloatNext());
        }
        return;
      }
      case DOUBLE: {
        while (indexIter.hasNext()) {
          bbuffer.putDouble(indexIter.getDoubleNext());
        }
        return;
      }
      case STRING: {
        String[] svals = new String[(int) data.getSize()];
        int idx = 0;
        while (indexIter.hasNext()) {
          svals[idx++] = (String) indexIter.getObjectNext();
        }
        int index = storage.putOnHeap(svals);
        bbuffer.putInt(index);
        return;
      }
      case STRUCTURE: {
        Preconditions.checkArgument(member.getStructureMembers() != null);
        ArrayStructure orgArray = (ArrayStructure) data;
        StructureMembers nestedMembers = member.getStructureMembers();
        int length = (int) orgArray.getSize();
        for (int nrow = 0; nrow < length; nrow++) {
          ucar.ma2.StructureData orgData = orgArray.getStructureData(nrow);
          for (ucar.ma2.StructureMembers.Member ma2 : orgArray.getStructureMembers().getMembers()) {
            Member nmember = nestedMembers.findMember(ma2.getName());
            int nestedPos = pos + nrow * nestedMembers.getStorageSizeBytes();
            convertNestedData(nestedPos, nmember, storage, bbuffer, orgData.getArray(ma2));
          }
        }
        return;
      }
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
  }

  ////////////////////////////////////////////////////////////////////////////

  public static ucar.ma2.Array convertFromArray(Array<?> from) {
    if (from.getArrayType() == ArrayType.STRUCTURE || from.getArrayType() == ArrayType.SEQUENCE) {
      return convertStructureDataArray(from);
    }
    if (from.isVlen()) {
      return convertVlen(from);
    }

    ucar.ma2.Array values = ucar.ma2.Array.factory(from.getArrayType().getDataType(), from.getShape());
    int count = 0;
    for (Object val : from) {
      values.setObject(count++, val);
    }
    return values;
  }

  private static ucar.ma2.Array convertVlen(Array<?> from) {
    Preconditions.checkArgument(from instanceof ArrayVlen);
    ArrayVlen<Array<?>> vlen = (ArrayVlen<Array<?>>) from;
    if (vlen.getArrayType() == ArrayType.OPAQUE) {
      return convertOpaque(from);
    }

    ArrayObject result = new ArrayObject(vlen.getArrayType().getDataType(), Object.class, true, from.getShape());
    int count = 0;
    for (Array<?> array : vlen) {
      result.setObject(count++, Arrays.copyPrimitiveArray(array));
    }
    return result;
  }

  private static ucar.ma2.Array convertOpaque(Array<?> from) {
    Preconditions.checkArgument(from instanceof ArrayVlen);
    ArrayVlen<Array<?>> vlen = (ArrayVlen<Array<?>>) from;
    Preconditions.checkArgument(vlen.getArrayType() == ArrayType.OPAQUE);

    ucar.ma2.ArrayObject result = new ucar.ma2.ArrayObject(DataType.OPAQUE, ByteBuffer.class, true, from.getShape());
    int count = 0;
    for (Array<?> array : vlen) {
      ArrayByte barray = (ArrayByte) array;
      ByteBuffer bb = barray.getByteBuffer();
      result.setObject(count++, bb);
    }
    return result;
  }

  private static ucar.ma2.Array convertStructureDataArray(Array<?> from) {
    StructureDataArray orgData = (StructureDataArray) from;
    ucar.ma2.StructureMembers members = convertMembers(orgData.getStructureMembers()).build();

    Storage<StructureData> storage = (Storage<StructureData>) from.storage();
    if (storage instanceof StructureDataStorageBB) {
      // TODO go away in version 7
      ByteBuffer bbuffer = ((StructureDataStorageBB) storage).buffer();
      long recSize = ucar.ma2.ArrayStructureBB.setOffsets(members);
      members.setStructureSize((int) recSize);
      return new ucar.ma2.ArrayStructureBB(members, from.getShape(), bbuffer, 0);
    } else {
      ucar.ma2.ArrayStructureW result = new ucar.ma2.ArrayStructureW(members, from.getShape());
      for (int recno = 0; recno < from.length(); recno++) {
        result.setStructureData(convertStructureData(members, storage.get(recno)), recno);
      }
      return result;
    }
  }

  private static ucar.ma2.StructureMembers.Builder convertMembers(StructureMembers from) {
    ucar.ma2.StructureMembers.Builder builder = ucar.ma2.StructureMembers.builder();
    builder.setName(from.getName());
    for (StructureMembers.Member member : from.getMembers()) {
      ucar.ma2.StructureMembers.MemberBuilder ma2 = ucar.ma2.StructureMembers.memberBuilder();
      ma2.setName(member.getName());
      ma2.setShape(member.getShape());
      ma2.setDataType(member.getArrayType().getDataType());
      ma2.setUnits(member.getUnitsString());
      ma2.setDesc(member.getDescription());
      if (member.getStructureMembers() != null) {
        ma2.setStructureMembers(convertMembers(member.getStructureMembers()).build());
      }
      builder.addMember(ma2);
    }
    return builder;
  }

  private static ucar.ma2.StructureData convertStructureData(ucar.ma2.StructureMembers membersMa2, StructureData from) {
    StructureDataW sdata = new StructureDataW(membersMa2);
    StructureMembers members = from.getStructureMembers();
    for (int i = 0; i < members.getMembers().size(); i++) {
      ucar.ma2.StructureMembers.Member memberMa2 = membersMa2.getMember(i);
      StructureMembers.Member member = members.getMember(i);
      Array<?> data = from.getMemberData(member);
      sdata.setMemberData(memberMa2, convertFromArray(data));
    }
    return sdata;
  }

  public static ucar.ma2.Section convertSection(ucar.array.Section section) {
    ucar.ma2.Section.Builder builder = ucar.ma2.Section.builder();
    section.getRanges().forEach(r -> {
      try {
        builder.appendRange(new ucar.ma2.Range(r.getName(), r.first(), r.last(), r.stride()));
      } catch (ucar.ma2.InvalidRangeException e) {
        throw new RuntimeException(e); // not possible haha
      }
    });
    return builder.build();
  }

  public static ucar.array.Section convertSection(ucar.ma2.Section section) {
    ucar.array.Section.Builder builder = ucar.array.Section.builder();
    section.getRanges().forEach(r -> {
      try {
        builder.appendRange(new ucar.array.Range(r.getName(), r.first(), r.last(), r.stride()));
      } catch (InvalidRangeException e) {
        throw new RuntimeException(e); // not possible haha
      }
    });
    return builder.build();
  }

}
