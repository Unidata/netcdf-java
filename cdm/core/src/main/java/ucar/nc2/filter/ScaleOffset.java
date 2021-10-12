/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;

import java.nio.*;
import java.util.HashMap;
import java.util.Map;

import static ucar.ma2.DataType.*;

/**
 * Filter implementation of FixedScaleOffset as described by the
 * <a href="https://numcodecs.readthedocs.io/en/stable/fixedscaleoffset.html">NumCodecs</a> project
 */
public class ScaleOffset extends Filter {

  // maps numeric zarr datatypes to CDM datatypes
  private static Map<String, DataType> dTypeMap;

  static {
    dTypeMap = new HashMap<>();
    dTypeMap.put("i1", DataType.BYTE);
    dTypeMap.put("u1", DataType.UBYTE);
    dTypeMap.put("i2", DataType.SHORT);
    dTypeMap.put("u2", DataType.USHORT);
    dTypeMap.put("i4", DataType.INT);
    dTypeMap.put("f4", DataType.FLOAT);
    dTypeMap.put("u4", DataType.UINT);
    dTypeMap.put("i8", DataType.LONG);
    dTypeMap.put("f8", DataType.DOUBLE);
    dTypeMap.put("u8", DataType.ULONG);
  }

  private final double offset;
  private final int scale;

  // type information for original data type
  private final ByteOrder dtypeOrder;
  private final DataType dtype;

  // type information for storage type
  private final DataType astype;
  private final ByteOrder astypeOrder;


  public ScaleOffset(Map<String, Object> properties) {
    // get offset and scale parameters
    offset = ((Number) properties.get("offset")).doubleValue();
    scale = (int) properties.get("scale");

    // input data type
    String type = (String) properties.get("dtype");
    dtype = parseDataType(type);
    if (dtype == null) {
      throw new RuntimeException("ScaleOffset error: could not parse dtype");
    }
    dtypeOrder = parseByteOrder(type, ByteOrder.LITTLE_ENDIAN);

    // get storage type, if exists, or default to dtype
    String aType = (String) properties.getOrDefault("astype", type);
    astype = parseDataType(aType);
    astypeOrder = parseByteOrder(aType, dtypeOrder);
  }


  @Override
  public byte[] encode(byte[] dataIn) {
    Array in =
        Array.factory(dtype, new int[] {dataIn.length / dtype.getSize()}, convertToType(dataIn, dtype, dtypeOrder));
    Array out = applyScaleOffset(in);
    return arrayToBytes(out, astype, astypeOrder);
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    Array in =
        Array.factory(astype, new int[] {dataIn.length / astype.getSize()}, convertToType(dataIn, astype, astypeOrder));
    Array out = removeScaleOffset(in);
    return arrayToBytes(out, dtype, dtypeOrder);
  }

  private Array applyScaleOffset(Array in) {
    // use wider datatype if unsigned
    DataType outType = astype;
    if (astype.getSignedness() == Signedness.UNSIGNED) {
      outType = nextLarger(astype).withSignedness(DataType.Signedness.UNSIGNED);
    }

    // create conversion array
    Array out = Array.factory(outType, in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();

    // iterate and convert elements
    while (iterIn.hasNext()) {
      Number value = (Number) iterIn.getObjectNext();
      value = convertUnsigned(value, dtype.getSignedness());
      value = applyScaleOffset(value);
      iterOut.setObjectNext(value);
    }

    return out;
  }

  private Array removeScaleOffset(Array in) {
    // use wider datatype if unsigned
    DataType outType = dtype;
    if (dtype.getSignedness() == Signedness.UNSIGNED) {
      outType = nextLarger(dtype).withSignedness(DataType.Signedness.UNSIGNED);
    }

    // create conversion array
    Array out = Array.factory(outType, in.getShape());
    IndexIterator iterIn = in.getIndexIterator();
    IndexIterator iterOut = out.getIndexIterator();

    // iterate and convert elements
    while (iterIn.hasNext()) {
      Number value = (Number) iterIn.getObjectNext();
      value = convertUnsigned(value, astype.getSignedness());
      value = removeScaleOffset(value);
      iterOut.setObjectNext(value);
    }

    return out;
  }

  ////////////////////////////////////////////////////
  // helpers
  ////////////////////////////////////////////////////
  private static DataType parseDataType(String dtype) {
    dtype = dtype.replace(">", "");
    dtype = dtype.replace("<", "");
    dtype = dtype.replace("|", "");
    return dTypeMap.getOrDefault(dtype, null);
  }

  private static ByteOrder parseByteOrder(String dtype, ByteOrder defaultOrder) {
    if (dtype.startsWith(">")) {
      return ByteOrder.BIG_ENDIAN;
    } else if (dtype.startsWith("<")) {
      return ByteOrder.LITTLE_ENDIAN;
    } else if (dtype.startsWith("|")) {
      return ByteOrder.nativeOrder();
    }
    return defaultOrder;
  }

  private DataType nextLarger(DataType dataType) {
    switch (dataType) {
      case BYTE:
        return SHORT;
      case UBYTE:
        return USHORT;
      case SHORT:
        return INT;
      case USHORT:
        return UINT;
      case INT:
        return LONG;
      case UINT:
        return ULONG;
      case LONG:
      case ULONG:
        return DOUBLE;
      default:
        return dataType;
    }
  }

  private Number convertUnsigned(Number value, Signedness signedness) {
    if (signedness == Signedness.UNSIGNED) {
      // Handle integral types that should be treated as unsigned by widening them if necessary.
      return DataType.widenNumberIfNegative(value);
    } else {
      return value;
    }
  }

  private Object convertToType(byte[] dataIn, DataType wantType, ByteOrder bo) {
    if (wantType.getSize() == 1) {
      return dataIn;
    } // no need for conversion

    ByteBuffer bb = ByteBuffer.wrap(dataIn);
    bb.order(bo);
    switch (wantType) {
      case SHORT:
      case USHORT:
        ShortBuffer sb = bb.asShortBuffer();
        short[] shortArray = new short[sb.limit()];
        sb.get(shortArray);
        return shortArray;
      case INT:
      case UINT:
        IntBuffer ib = bb.asIntBuffer();
        int[] intArray = new int[ib.limit()];
        ib.get(intArray);
        return intArray;
      case LONG:
      case ULONG:
        LongBuffer lb = bb.asLongBuffer();
        long[] longArray = new long[lb.limit()];
        lb.get(longArray);
        return longArray;
      case FLOAT:
        FloatBuffer fb = bb.asFloatBuffer();
        float[] floatArray = new float[fb.limit()];
        fb.get(floatArray);
        return floatArray;
      case DOUBLE:
        DoubleBuffer db = bb.asDoubleBuffer();
        double[] doubleArray = new double[db.limit()];
        db.get(doubleArray);
        return doubleArray;
      default:
        return bb.array();
    }
  }

  private int applyScaleOffset(Number value) {
    double convertedValue = value.doubleValue();
    return (int) Math.round((convertedValue - offset) * scale);
  }

  private double removeScaleOffset(Number value) {
    double convertedValue = value.doubleValue();
    return convertedValue / scale + offset;
  }

  private byte[] arrayToBytes(Array arr, DataType type, ByteOrder order) {
    ByteBuffer bb = ByteBuffer.allocate((int) arr.getSize() * type.getSize());
    bb.order(order);

    IndexIterator ii = arr.getIndexIterator();
    while (ii.hasNext()) {
      switch (type) {
        case BYTE:
        case UBYTE:
          bb.put(ii.getByteNext());
          break;
        case SHORT:
        case USHORT:
          bb.putShort(ii.getShortNext());
          break;
        case INT:
        case UINT:
          bb.putInt(ii.getIntNext());
          break;
        case LONG:
        case ULONG:
          bb.putLong(ii.getLongNext());
          break;
        case FLOAT:
          bb.putFloat(ii.getFloatNext());
          break;
        case DOUBLE:
          bb.putDouble(ii.getDoubleNext());
          break;
      }
    }
    return bb.array();
  }

  public static class Provider implements FilterProvider {

    private static final String name = "fixedscaleoffset";

    private static final int id = -1; // not yet implemented by id

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int getId() {
      return id;
    }

    @Override
    public Filter create(Map<String, Object> properties) {
      return new ScaleOffset(properties);
    }
  }
}
