/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ma2;

/**
 * A StructureData with scalar data.
 * 
 * @deprecated use StructureDataFromMember
 */
@Deprecated
public class StructureDataScalar extends StructureDataW {

  public StructureDataScalar(String name) {
    super(new StructureMembers(name));
  }

  public void addMember(String name, String desc, String units, DataType dtype, Number val) {
    StructureMembers.Member m = members.addMember(name, desc, units, dtype, new int[0]);
    Array data = null;
    switch (dtype) {
      case UBYTE:
      case BYTE:
      case ENUM1:
        data = new ArrayByte.D0(dtype.isUnsigned());
        data.setByte(0, val.byteValue());
        break;
      case SHORT:
      case USHORT:
      case ENUM2:
        data = new ArrayShort.D0(dtype.isUnsigned());
        data.setShort(0, val.shortValue());
        break;
      case INT:
      case UINT:
      case ENUM4:
        data = new ArrayInt.D0(dtype.isUnsigned());
        data.setInt(0, val.intValue());
        break;
      case LONG:
      case ULONG:
        data = new ArrayLong.D0(dtype.isUnsigned());
        data.setDouble(0, val.longValue());
        break;
      case FLOAT:
        data = new ArrayFloat.D0();
        data.setFloat(0, val.floatValue());
        break;
      case DOUBLE:
        data = new ArrayDouble.D0();
        data.setDouble(0, val.doubleValue());
        break;
    }
    setMemberData(m, data);
  }

  public void addMemberString(String name, String desc, String units, String val, int max_len) {
    StructureMembers.Member m = members.addMember(name, desc, units, DataType.CHAR, new int[] {max_len});
    Array data = ArrayChar.makeFromString(val, max_len);
    setMemberData(m, data);
  }
}
