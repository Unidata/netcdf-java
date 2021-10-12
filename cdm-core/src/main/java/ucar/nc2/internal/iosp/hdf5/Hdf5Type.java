/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import ucar.array.ArrayType;

import javax.annotation.Nullable;
import java.nio.ByteOrder;

class Hdf5Type {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Hdf5Type.class);
  private static final boolean warnings = true;

  int hdfType;
  int byteSize;
  byte[] flags;
  ArrayType dataType;
  ByteOrder endian;
  boolean unsigned;
  boolean isVString; // is it a vlen string
  boolean isVlen; // vlen but not string
  int vpad; // string padding
  Hdf5Type base; // vlen, enum

  Hdf5Type(H5objects.MessageDatatype mdt) {
    this.hdfType = mdt.type;
    this.byteSize = mdt.byteSize;
    this.flags = mdt.flags;

    if (hdfType == 0) { // int, long, short, byte
      this.dataType = getNCtype(hdfType, byteSize, mdt.unsigned);
      this.endian = mdt.endian;
      this.unsigned = ((flags[0] & 8) == 0);

    } else if (hdfType == 1) { // floats, doubles
      this.dataType = getNCtype(hdfType, byteSize, mdt.unsigned);
      this.endian = mdt.endian;

    } else if (hdfType == 2) { // time
      this.dataType = ArrayType.STRING;
      this.endian = mdt.endian;

    } else if (hdfType == 3) { // fixed length strings map to CHAR. String is used for Vlen type = 1.
      this.dataType = ArrayType.CHAR;
      this.vpad = (flags[0] & 0xf);
      // when elem length = 1, there is a problem with dimensionality.
      // eg char cr(2); has a storage_size of [1,1].

    } else if (hdfType == 4) { // bit field
      this.dataType = getNCtype(hdfType, byteSize, mdt.unsigned);

    } else if (hdfType == 5) { // opaque
      this.dataType = ArrayType.OPAQUE;

    } else if (hdfType == 6) { // structure
      this.dataType = ArrayType.STRUCTURE;

    } else if (hdfType == 7) { // reference
      this.endian = ByteOrder.LITTLE_ENDIAN;
      this.dataType = ArrayType.LONG; // file offset of the referenced object
      // LOOK - should get the object, and change type to whatever it is (?)

    } else if (hdfType == 8) { // enums
      if (this.byteSize == 1)
        this.dataType = ArrayType.ENUM1;
      else if (this.byteSize == 2)
        this.dataType = ArrayType.ENUM2;
      else if (this.byteSize == 4)
        this.dataType = ArrayType.ENUM4;
      else {
        log.warn("Illegal byte size for enum type = {}", this.byteSize);
        throw new IllegalStateException("Illegal byte size for enum type = " + this.byteSize);
      }
      this.endian = mdt.base.endian;

    } else if (hdfType == 9) { // variable length array
      this.isVString = mdt.isVString;
      this.isVlen = mdt.isVlen;
      if (mdt.isVString) {
        this.vpad = ((flags[0] >> 4) & 0xf);
        this.dataType = ArrayType.STRING;
      } else {
        this.dataType = getNCtype(mdt.getBaseType(), mdt.getBaseSize(), mdt.base.unsigned);
        this.endian = mdt.base.endian;
        this.unsigned = mdt.base.unsigned;
      }
    } else if (hdfType == 10) { // array : used for structure members
      // mdt.getFlags uses the base type if it exists
      this.endian = (mdt.getFlags()[0] & 1) == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
      if (mdt.isVString()) {
        this.dataType = ArrayType.STRING;
      } else {
        int basetype = mdt.getBaseType();
        this.dataType = getNCtype(basetype, mdt.getBaseSize(), mdt.unsigned);
      }
    } else if (warnings) {
      log.debug("WARNING not handling hdf dataType = " + hdfType + " size= " + byteSize);
    }

    if (mdt.base != null) {
      this.base = new Hdf5Type(mdt.base);
    }
  }

  /*
   * Value Description
   * 0 Fixed-Point
   * 1 Floating-point
   * 2 Time
   * 3 String
   * 4 Bit field
   * 5 Opaque
   * 6 Compound
   * 7 Reference
   * 8 Enumerated
   * 9 Variable-Length
   * 10 Array
   */
  @Nullable
  private ArrayType getNCtype(int hdfType, int size, boolean unsigned) {
    // LOOK not translating all of them !
    if ((hdfType == 0) || (hdfType == 4)) { // integer, bit field
      ArrayType.Signedness signedness = unsigned ? ArrayType.Signedness.UNSIGNED : ArrayType.Signedness.SIGNED;

      if (size == 1) {
        return ArrayType.BYTE.withSignedness(signedness);
      } else if (size == 2) {
        return ArrayType.SHORT.withSignedness(signedness);
      } else if (size == 4) {
        return ArrayType.INT.withSignedness(signedness);
      } else if (size == 8) {
        return ArrayType.LONG.withSignedness(signedness);
      } else {
        log.warn("HDF5 file  not handling hdf integer type (" + hdfType + ") with size= " + size);
        return null;
      }

    } else if (hdfType == 1) {
      if (size == 4) {
        return ArrayType.FLOAT;
      } else if (size == 8) {
        return ArrayType.DOUBLE;
      } else {
        log.warn("HDF5 file  not handling hdf float type with size= " + size);
        return null;
      }

    } else if (hdfType == 3) { // fixed length strings. String is used for Vlen type = 1
      return ArrayType.CHAR;

    } else if (hdfType == 6) {
      return ArrayType.STRUCTURE;

    } else if (hdfType == 7) { // reference
      return ArrayType.ULONG;

    } else if (hdfType == 9) {
      return null; // dunno

    } else if (warnings) {
      log.warn("HDF5 file not handling hdf type = " + hdfType + " size= " + size);
    }
    return null;
  }

  public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append("hdfType=").append(hdfType).append(" byteSize=").append(byteSize).append(" dataType=").append(dataType);
    buff.append(" unsigned=").append(unsigned).append(" isVString=").append(isVString).append(" vpad=").append(vpad)
        .append(" endian=").append(endian);
    if (base != null)
      buff.append("\n   base=").append(base);
    return buff.toString();
  }
}
