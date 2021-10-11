/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr;

import java.nio.ByteBuffer;
import java.util.HashMap;

import ucar.array.ArrayType;
import ucar.array.StructureMembers;
import ucar.array.StructureMembers.Member;

/** Static utilities for Message Raders into ucar.array.Array */
public class MessageArrayReaderUtils {

  static void associateMessage2Members(StructureMembers members, DataDescriptor parent,
      HashMap<DataDescriptor, Member> map) {
    for (DataDescriptor dkey : parent.getSubKeys()) {
      if (dkey.name == null) {
        if (dkey.getSubKeys() != null) {
          associateMessage2Members(members, dkey, map);
        }
        continue;
      }
      Member m = members.findMember(dkey.name);
      if (m != null) {
        map.put(dkey, m);

        if (m.getArrayType() == ArrayType.STRUCTURE) {
          if (dkey.getSubKeys() != null) {
            associateMessage2Members(m.getStructureMembers(), dkey, map);
          }
        }

      } else {
        if (dkey.getSubKeys() != null) {
          associateMessage2Members(members, dkey, map);
        }
      }
    }
  }

  // The byteBuffer has to be positioned correctly
  // need to use the same algo when setting DataType and offsets
  static void putNumericData(DataDescriptor dkey, ByteBuffer bb, long value) {
    // place into byte buffer
    if (dkey.getByteWidthCDM() == 1) {
      bb.put((byte) value);

    } else if (dkey.getByteWidthCDM() == 2) {
      byte b1 = (byte) (value & 0xff);
      byte b2 = (byte) ((value & 0xff00) >> 8);
      bb.put(b2);
      bb.put(b1);

    } else if (dkey.getByteWidthCDM() == 4) {
      byte b1 = (byte) (value & 0xff);
      byte b2 = (byte) ((value & 0xff00) >> 8);
      byte b3 = (byte) ((value & 0xff0000) >> 16);
      byte b4 = (byte) ((value & 0xff000000) >> 24);
      bb.put(b4);
      bb.put(b3);
      bb.put(b2);
      bb.put(b1);

    } else {
      byte b1 = (byte) (value & 0xff);
      byte b2 = (byte) ((value & 0xff00) >> 8);
      byte b3 = (byte) ((value & 0xff0000) >> 16);
      byte b4 = (byte) ((value & 0xff000000) >> 24);
      byte b5 = (byte) ((value & 0xff00000000L) >> 32);
      byte b6 = (byte) ((value & 0xff0000000000L) >> 40);
      byte b7 = (byte) ((value & 0xff000000000000L) >> 48);
      byte b8 = (byte) ((value & 0xff00000000000000L) >> 56);
      bb.put(b8);
      bb.put(b7);
      bb.put(b6);
      bb.put(b5);
      bb.put(b4);
      bb.put(b3);
      bb.put(b2);
      bb.put(b1);
    }
  }

}
