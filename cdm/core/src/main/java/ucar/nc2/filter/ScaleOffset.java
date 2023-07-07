/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.filter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.VariableDS;

import java.nio.*;
import java.util.HashMap;
import java.util.Map;

import static ucar.ma2.DataType.*;

/**
 * Filter implementation of FixedScaleOffset as described by the
 * <a href="https://numcodecs.readthedocs.io/en/stable/fixedscaleoffset.html">NumCodecs</a> project
 */
public class ScaleOffset extends Filter {

  private static final String name = "fixedscaleoffset";

  private static final int id = 6;

  public static class Keys {
    public static final String OFFSET_KEY = "offset";
    public static final String SCALE_KEY = "scale";
    public static final String DTYPE_KEY = "dtype";
    public static final String ASTYPE_KEY = "astype";
  }

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
  private final double scale;

  private static final double DEFAULT_OFFSET = 0.0;
  private static final double DEFAULT_SCALE = 1.0;

  // type information for original data type
  private final ByteOrder dtypeOrder;
  private final DataType dtype;

  // type information for storage type
  private final DataType astype;
  private final ByteOrder astypeOrder;

  public static ScaleOffset createFromVariable(VariableDS var) {

    DataType scaleType = null, offsetType = null;
    double scale = DEFAULT_SCALE, offset = DEFAULT_OFFSET;

    DataType origDataType = var.getDataType();
    DataType.Signedness signedness = var.getSignedness();

    Attribute scaleAtt = var.findAttribute(CDM.SCALE_FACTOR);
    if (scaleAtt != null && !scaleAtt.isString()) {
      scaleType = FilterHelpers.getAttributeDataType(scaleAtt, signedness);
      scale = 1 / (var.convertUnsigned(scaleAtt.getNumericValue(), scaleType).doubleValue());
    }

    Attribute offsetAtt = var.findAttribute(CDM.ADD_OFFSET);
    if (offsetAtt != null && !offsetAtt.isString()) {
      offsetType = FilterHelpers.getAttributeDataType(offsetAtt, signedness);
      offset = var.convertUnsigned(offsetAtt.getNumericValue(), offsetType).doubleValue();
    }
    if (scale != DEFAULT_SCALE || offset != DEFAULT_OFFSET) {
      DataType scaledOffsetType =
          FilterHelpers.largestOf(var.getUnsignedConversionType(), scaleType, offsetType).withSignedness(signedness);

      Map<String, Object> scaleOffsetProps = new HashMap<>();
      scaleOffsetProps.put(ScaleOffset.Keys.OFFSET_KEY, offset);
      scaleOffsetProps.put(ScaleOffset.Keys.SCALE_KEY, scale);
      scaleOffsetProps.put(ScaleOffset.Keys.DTYPE_KEY, scaledOffsetType);
      scaleOffsetProps.put(ScaleOffset.Keys.ASTYPE_KEY, origDataType);
      return new ScaleOffset(scaleOffsetProps);
    }
    return null;
  }

  public ScaleOffset(Map<String, Object> properties) {
    // get offset and scale parameters
    offset = ((Number) properties.getOrDefault(Keys.OFFSET_KEY, DEFAULT_OFFSET)).doubleValue();
    scale = ((Number) properties.getOrDefault(Keys.SCALE_KEY, DEFAULT_SCALE)).doubleValue();

    // input data type
    Object typeProp = properties.get(Keys.DTYPE_KEY);
    if (typeProp instanceof String) {
      String type = (String) typeProp;
      dtype = parseDataType(type);
      if (dtype == null) {
        throw new RuntimeException("ScaleOffset error: could not parse dtype");
      }
      dtypeOrder = parseByteOrder(type, ByteOrder.LITTLE_ENDIAN);
    } else if (typeProp instanceof DataType) {
      dtype = (DataType) typeProp;
      dtypeOrder = ByteOrder.LITTLE_ENDIAN;
    } else {
      throw new RuntimeException("ScaleOffset error: could not parse dtype");
    }

    // get storage type, if exists, or default to dtype
    Object aTypeProp = properties.getOrDefault(Keys.ASTYPE_KEY, null);
    if (aTypeProp instanceof String) {
      String aType = (String) aTypeProp;
      astype = parseDataType(aType);
      astypeOrder = parseByteOrder(aType, dtypeOrder);
    } else if (aTypeProp instanceof DataType) {
      astype = (DataType) aTypeProp;
      astypeOrder = ByteOrder.LITTLE_ENDIAN;
    } else {
      astype = dtype;
      astypeOrder = dtypeOrder;
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getId() {
    return id;
  }

  public double getScaleFactor() {
    return this.scale;
  }

  public double getOffset() {
    return this.offset;
  }

  public DataType getScaledOffsetType() {
    return this.dtype;
  }

  @Override
  public byte[] encode(byte[] dataIn) {
    if (scale == DEFAULT_SCALE && offset == DEFAULT_OFFSET) {
      return dataIn;
    }
    Array out = applyScaleOffset(FilterHelpers.bytesToArray(dataIn, dtype, dtypeOrder));
    return FilterHelpers.arrayToBytes(out, astype, astypeOrder);
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    if (scale == DEFAULT_SCALE && offset == DEFAULT_OFFSET) {
      return dataIn;
    }
    Array out = removeScaleOffset(FilterHelpers.bytesToArray(dataIn, astype, astypeOrder));
    return FilterHelpers.arrayToBytes(out, dtype, dtypeOrder);
  }

  public Array applyScaleOffset(Array in) {
    if (scale == DEFAULT_SCALE && offset == DEFAULT_OFFSET) {
      return in;
    }
    // use wider datatype if unsigned
    DataType outType = astype;
    if (astype.getSignedness() == Signedness.UNSIGNED) {
      outType = FilterHelpers.nextLarger(astype).withSignedness(DataType.Signedness.UNSIGNED);
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

  public Array removeScaleOffset(Array in) {
    if (scale == DEFAULT_SCALE && offset == DEFAULT_OFFSET) {
      return in;
    }
    // use wider datatype if unsigned
    DataType outType = dtype;
    if (dtype.getSignedness() == Signedness.UNSIGNED) {
      outType = FilterHelpers.nextLarger(dtype).withSignedness(DataType.Signedness.UNSIGNED);
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

  public double applyScaleOffset(Number value) {
    double convertedValue = value.doubleValue();
    if (astype.isIntegral()) {
      return Math.round((convertedValue - offset) * scale);
    }
    return (convertedValue - offset) * scale;
  }

  public double removeScaleOffset(Number value) {
    double convertedValue = value.doubleValue();
    if (dtype.isIntegral()) {
      return Math.round(convertedValue / scale + offset);
    }
    return convertedValue / scale + offset;
  }

  private Number convertUnsigned(Number value, Signedness signedness) {
    if (signedness == Signedness.UNSIGNED) {
      // Handle integral types that should be treated as unsigned by widening them if necessary.
      return DataType.widenNumberIfNegative(value);
    } else {
      return value;
    }
  }


  public static class Provider implements FilterProvider {

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
