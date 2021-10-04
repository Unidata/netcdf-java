/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.ArrayVlen;
import ucar.array.Arrays;
import ucar.array.ArraysConvert;
import ucar.ma2.DataType;
import ucar.unidata.util.StringUtil2;
import java.util.Formatter;
import java.util.List;

/**
 * An Attribute is a name and a value, used for associating arbitrary metadata with another object.
 * The value can be a one dimensional array of Strings or numeric values.
 */
@Immutable
public class Attribute {

  /** @deprecated use fromArray(String name, Array<?> values) */
  @Deprecated
  public static Attribute fromArray(String name, ucar.ma2.Array values) {
    return builder(name).setValues(values).build();
  }

  /** Create an Attribute from an Array. */
  public static Attribute fromArray(String name, Array<?> values) {
    return builder(name).setArrayValues(values).build();
  }

  /** Create an Attribute with a datatype but no value. */
  public static Attribute emptyValued(String name, ArrayType dtype) {
    return builder(name).setArrayType(dtype).build();
  }

  ///////////////////////////////////////////////////////////////////////////////////

  /**
   * Create a String-valued Attribute.
   *
   * @param name name of Attribute
   * @param val value of Attribute
   */
  public Attribute(String name, @Nullable String val) {
    Preconditions.checkNotNull(Strings.emptyToNull(name), "Attribute name cannot be empty or null");
    this.name = NetcdfFiles.makeValidCdmObjectName(name);
    this.dataType = ArrayType.STRING;
    this.nelems = 1;
    this.svalue = stripZeroes(val);
    this.enumtype = null;
    this.nvalue = null;
    this.values = null;
  }

  /**
   * Create a scalar, signed, numeric-valued Attribute. Use builder for unsigned.
   *
   * @param name name of Attribute
   * @param val value of Attribute, assumed unsigned.
   */
  public Attribute(String name, Number val) {
    Preconditions.checkNotNull(Strings.emptyToNull(name), "Attribute name cannot be empty or null");
    Preconditions.checkNotNull(val, "Attribute value cannot be null");
    this.name = NetcdfFiles.makeValidCdmObjectName(name);
    this.dataType = ArrayType.forPrimitiveClass(val.getClass(), false);
    this.nvalue = val;

    this.enumtype = null;
    this.svalue = null;
    this.values = null;
    this.nelems = 1;
  }

  /** @deprecated use getArrayType() */
  @Deprecated
  public DataType getDataType() {
    return dataType.getDataType();
  }

  /** Get the data type of the Attribute value. */
  public ArrayType getArrayType() {
    return dataType;
  }

  /** Get the EnumTypedef of the Attribute value, if DataType is an ENUM. */
  @Nullable
  public EnumTypedef getEnumType() {
    return this.enumtype;
  }

  /** Get the length of the array of values */
  public int getLength() {
    return nelems;
  }

  /** Get the Attribute name, same as the short name. */
  public String getName() {
    return name;
  }

  /** Retrieve first numeric value. Equivalent to <code>getNumericValue(0)</code> */
  @Nullable
  public Number getNumericValue() {
    return getNumericValue(0);
  }

  /**
   * Retrieve a numeric value by index. If it's a String, it will try to parse it as a double.
   *
   * @param index the index into the value array.
   * @return Number <code>value[index]</code>, or null if its a non-parseable String or
   *         the index is out of range.
   */
  @Nullable
  public Number getNumericValue(int index) {
    if ((index < 0) || (index >= nelems))
      return null;

    if (dataType == ArrayType.STRING) {
      String svalue = Preconditions.checkNotNull(getStringValue(index));
      try {
        return Double.parseDouble(svalue);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    if (nvalue != null && index == 0) {
      return nvalue;
    }

    Preconditions.checkNotNull(values);
    switch (dataType) {
      case BYTE:
      case UBYTE:
        return (Byte) values.get(index);
      case SHORT:
      case USHORT:
        return (Short) values.get(index);
      case INT:
      case UINT:
        return (Integer) values.get(index);
      case FLOAT:
        return (Float) values.get(index);
      case DOUBLE:
        return (Double) values.get(index);
      case LONG:
      case ULONG:
        return (Long) values.get(index);
    }
    return null;
  }

  /** Get the Attribute name. */
  public String getShortName() {
    return name;
  }

  /** Retrieve first String value if this is a String valued attribute, else null. */
  @Nullable
  public String getStringValue() {
    if (dataType != ArrayType.STRING)
      return null;
    return (svalue != null) ? svalue : _getStringValue(0);
  }

  /**
   * Retrieve ith String value; only call if isString() is true.
   *
   * @param index which index
   * @return ith String value (if this is a String valued attribute and index in range), else null.
   */
  @Nullable
  public String getStringValue(int index) {
    if (dataType != ArrayType.STRING)
      return null;
    if ((svalue != null) && (index == 0))
      return svalue;
    return _getStringValue(index);
  }

  private String _getStringValue(int index) {
    if ((index < 0) || (index >= nelems) || values == null)
      return null;
    return (String) values.get(index);
  }

  /** Retrieve the ith Opaque value, only call if isOpaque(). */
  @Nullable
  public Array<Byte> getOpaqueValue(int index) {
    Preconditions.checkArgument(dataType == ArrayType.OPAQUE);
    ArrayVlen<Byte> vlen = (ArrayVlen) values;
    return vlen.get(index);
  }

  /**
   * Get the value as an Object (String or Number).
   *
   * @param index index into value Array.2
   * @return ith value as an Object.
   */
  @Nullable
  public Object getValue(int index) {
    if (isString()) {
      return getStringValue(index);
    } else if (dataType == ArrayType.OPAQUE) {
      return getOpaqueValue(index);
    }
    return getNumericValue(index);
  }

  /** Get the values as an ucar.ma2.Array. */
  @Deprecated
  @Nullable
  public ucar.ma2.Array getValues() {
    ucar.array.Array<?> arrayValues = getArrayValues();
    return arrayValues == null ? null : ArraysConvert.convertFromArray(arrayValues);
  }

  /** Get the values as an ucar.array.Array. */
  @Nullable
  public ucar.array.Array<?> getArrayValues() {
    if (svalue != null) {
      return Arrays.factory(ArrayType.STRING, new int[] {1}, new String[] {svalue});
    }
    if (nvalue != null) {
      ucar.ma2.Array values = ucar.ma2.Array.factory(this.dataType.getDataType(), new int[] {1});
      values.setObject(values.getIndex(), nvalue);
      return ArraysConvert.convertToArray(values);
    }
    return values;
  }

  /** True if value is an array (getLength() &gt; 1) */
  public boolean isArray() {
    return (getLength() > 1);
  }

  /** True if value is of type String and not null. */
  public boolean isString() {
    return (dataType == ArrayType.STRING) && null != getStringValue();
  }

  /** True if value is of type Opaque and values instanceof ArrayVlen. */
  public boolean isOpaque() {
    return (dataType == ArrayType.OPAQUE) && (values instanceof ArrayVlen);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    writeCDL(f, false, null);
    return f.toString();
  }

  /**
   * Write CDL representation into a Formatter.
   *
   * @param f write into this
   * @param strict if true, create strict CDL, escaping names
   */
  void writeCDL(Formatter f, boolean strict, String parentname) {
    if (strict && (isString() || this.getEnumType() != null)) {
      // Force type explicitly for string.
      f.format("string ");
    } // note lower case and trailing blank

    if (strict && parentname != null) {
      f.format(NetcdfFiles.makeValidCDLName(parentname));
    }
    f.format(":");
    f.format("%s", strict ? NetcdfFiles.makeValidCDLName(getShortName()) : getShortName());

    if (isString()) {
      f.format(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) {
          f.format(", ");
        }
        String val = getStringValue(i);
        if (val != null) {
          f.format("\"%s\"", encodeString(val));
        }
      }
    } else if (isOpaque()) {
      f.format(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) {
          f.format("; ");
        }
        Array<Byte> oval = getOpaqueValue(i);
        if (oval != null) {
          for (byte b : oval) {
            f.format("%d,", b);
          }
        }
      }
    } else if (getEnumType() != null) {
      f.format(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) {
          f.format(", ");
        }
        EnumTypedef en = getEnumType();
        Integer intValue = (Integer) getValue(i);
        String ecname = intValue == null ? "" : en.lookupEnumString(intValue);
        if (ecname == null) {
          // TODO What does the C library do ?
          ecname = Integer.toString(intValue);
        }
        f.format("\"%s\"", encodeString(ecname));
      }
    } else {
      f.format(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) {
          f.format(", ");
        }

        Number number = getNumericValue(i);
        if (dataType.isUnsigned()) {
          // 'number' is unsigned, but will be treated as signed when we print it below, because Java only has signed
          // types. If it is large enough ( >= 2^(BIT_WIDTH-1) ), its most-significant bit will be interpreted as the
          // sign bit, which will result in an invalid (negative) value being printed. To prevent that, we're going
          // to widen the number before printing it.
          number = ArrayType.widenNumber(number);
        }
        f.format("%s", number);

        if (dataType.isUnsigned()) {
          f.format("U");
        }
        if (dataType == ArrayType.FLOAT)
          f.format("f");
        else if (dataType == ArrayType.SHORT || dataType == ArrayType.USHORT) {
          f.format("S");
        } else if (dataType == ArrayType.BYTE || dataType == ArrayType.UBYTE) {
          f.format("B");
        } else if (dataType == ArrayType.LONG || dataType == ArrayType.ULONG) {
          f.format("L");
        }
      }
    }
  }

  private static final char[] org = {'\b', '\f', '\n', '\r', '\t', '\\', '\'', '\"'};
  private static final String[] replace = {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\'", "\\\""};

  private static String encodeString(String s) {
    return StringUtil2.replace(s, org, replace);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Attribute))
      return false;

    Attribute att = (Attribute) o;

    String name = getShortName();
    if (!name.equals(att.getShortName()))
      return false;
    if (nelems != att.nelems)
      return false;
    if (dataType != att.dataType)
      return false;

    if (isString()) {
      return att.getStringValue().equals(getStringValue());
    }

    if (nvalue != null) {
      return nvalue.equals(att.nvalue);
    }

    if (values != null) {
      if (isOpaque()) {
        for (int i = 0; i < getLength(); i++) {
          if (!checkContents(getOpaqueValue(i), att.getOpaqueValue(i))) {
            return false;
          }
        }
      } else {
        for (int i = 0; i < getLength(); i++) {
          int r1 = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
          int r2 = att.isString() ? att.getStringValue(i).hashCode() : att.getNumericValue(i).hashCode();
          if (r1 != r2) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean checkContents(Array<Byte> r1, Array<Byte> r2) {
    if (!r1.equals(r2)) {
      return false;
    }
    int count = 0;
    for (byte b1 : r1) {
      byte b2 = r2.get(count++);
      if (b1 != b2) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * result + getShortName().hashCode();
    result = 37 * result + nelems;
    result = 37 * result + getDataType().hashCode();
    if (svalue != null) {
      result = 37 * result + svalue.hashCode();
    } else if (nvalue != null) {
      result = 37 * result + nvalue.hashCode();
    } else if (values != null) {
      for (int i = 0; i < getLength(); i++) {
        int h = isString() ? getStringValue(i).hashCode()
            : isOpaque() ? getOpaqueValue(i).hashCode() : getNumericValue(i).hashCode();
        result = 37 * result + h;
      }
    }
    return result;
  }

  // get rid of trailing nul characters
  private static String stripZeroes(String s) {
    if (s == null) {
      return null;
    }
    // get rid of trailing nul characters
    int len = s.length();
    while ((len > 0) && (s.charAt(len - 1) == 0)) {
      len--;
    }
    if (len != s.length()) {
      s = s.substring(0, len);
    }
    return s;
  }

  ///////////////////////////////////////////////////////////////////////////////

  private final String name;
  private final ArrayType dataType;
  private final @Nullable String svalue; // optimization for common case of single String valued attribute
  private final @Nullable Number nvalue; // optimization for common case of scalar Numeric value
  private final @Nullable EnumTypedef enumtype;
  private final @Nullable Array<?> values;
  private final int nelems; // can be 0 or greater

  private Attribute(Builder builder) {
    this.name = builder.name;
    this.svalue = stripZeroes(builder.svalue);
    this.nvalue = builder.nvalue;
    this.dataType = builder.dataType;
    this.enumtype = builder.enumtype;
    this.values = builder.values;
    if (svalue != null || nvalue != null) {
      nelems = 1;
    } else if (this.values != null) {
      nelems = (int) this.values.length();
    } else {
      nelems = builder.nelems;
    }
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    Builder b = builder().setName(this.name).setArrayType(this.dataType).setEnumType(this.enumtype);
    if (this.svalue != null) {
      b.setStringValue(this.svalue);
    } else if (this.nvalue != null) {
      b.setNumericValue(this.nvalue, this.dataType.isUnsigned());
    } else if (this.values != null) {
      b.setArrayValues(this.values);
    }
    return b;
  }

  /** Create an Attribute builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Create an Attribute builder with the given Attribute name. */
  public static Builder builder(String name) {
    return new Builder().setName(name);
  }

  public static class Builder {
    private String name;
    private ArrayType dataType = ArrayType.STRING;
    private String svalue; // optimization for common case of single String valued attribute
    private Number nvalue;
    private Array<?> values;
    private int nelems;
    private EnumTypedef enumtype;
    private boolean built;

    private Builder() {}

    public Builder setName(String name) {
      Preconditions.checkNotNull(Strings.emptyToNull(name), "Attribute name cannot be empty or null");
      this.name = NetcdfFiles.makeValidCdmObjectName(name);
      return this;
    }

    /** @deprecated use setArrayType() */
    @Deprecated
    public Builder setDataType(DataType dataType) {
      this.dataType = dataType.getArrayType();
      return this;
    }

    public Builder setArrayType(ArrayType dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder setEnumType(EnumTypedef enumtype) {
      this.enumtype = enumtype;
      return this;
    }

    public Builder setNumericValue(Number val, boolean isUnsigned) {
      Preconditions.checkNotNull(val, "Attribute value cannot be null");
      this.nvalue = val;
      this.nelems = 1;
      this.dataType = ArrayType.forPrimitiveClass(val.getClass(), isUnsigned);
      return this;
    }

    /**
     * Set the value as a String, trimming trailing zeros
     * 
     * @param svalue value of Attribute, may not be null. If you want a null valued Attribute, dont set a value.
     */
    public Builder setStringValue(String svalue) {
      Preconditions.checkNotNull(svalue, "Attribute value cannot be null");
      this.svalue = svalue;
      this.nelems = 1;
      this.dataType = ArrayType.STRING;
      return this;
    }

    /**
     * Set the values from a list of String or one of the primitives
     * Integer, Float, Double, Short, Long, Integer, Byte.
     */
    public Builder setValues(List<?> values, boolean unsigned) {
      if (values == null || values.isEmpty()) {
        throw new IllegalArgumentException("values may not be null or empty");
      }
      int n = values.size();
      Class<?> c = values.get(0).getClass();
      Object pa;
      if (c == String.class) {
        this.dataType = ArrayType.STRING;
        String[] va = new String[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = stripZeroes((String) values.get(i));
        }
      } else if (c == Integer.class) {
        this.dataType = unsigned ? ArrayType.UINT : ArrayType.INT;
        int[] va = new int[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Integer) values.get(i);
        }
      } else if (c == Double.class) {
        this.dataType = ArrayType.DOUBLE;
        double[] va = new double[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Double) values.get(i);
        }
      } else if (c == Float.class) {
        this.dataType = ArrayType.FLOAT;
        float[] va = new float[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Float) values.get(i);
        }
      } else if (c == Short.class) {
        this.dataType = unsigned ? ArrayType.USHORT : ArrayType.SHORT;
        short[] va = new short[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Short) values.get(i);
        }
      } else if (c == Byte.class) {
        this.dataType = unsigned ? ArrayType.UBYTE : ArrayType.BYTE;
        byte[] va = new byte[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Byte) values.get(i);
        }
      } else if (c == Long.class) {
        this.dataType = unsigned ? ArrayType.ULONG : ArrayType.LONG;
        long[] va = new long[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Long) values.get(i);
        }
      } else {
        throw new IllegalArgumentException("Unknown type for Attribute = " + c.getName());
      }

      return setArrayValues(Arrays.factory(this.dataType, new int[] {n}, pa));
    }

    /**
     * Set the values from an Array, and the DataType from values.getElementType().
     * 
     * @deprecated use Builder.setArrayValues(Array<?> arr)
     */
    @Deprecated
    public Builder setValues(ucar.ma2.Array arr) {
      if (arr == null) {
        dataType = ArrayType.STRING;
        return this;
      }
      setArrayValues(ArraysConvert.convertToArray(arr));
      return this;
    }

    /** Set the values as an Array. */
    public Builder setArrayValues(Array<?> arr) {
      if (arr == null) {
        dataType = ArrayType.STRING;
        return this;
      }

      if (arr.getArrayType() == ArrayType.OPAQUE) {
        this.nelems = (int) arr.getSize();
        int count = 0;
        for (Object val : arr) {
          Preconditions.checkArgument(val != null);
          count++;
        }
        Preconditions.checkArgument(count == nelems);
        this.values = arr;
        this.dataType = ArrayType.OPAQUE;
        return this;
      }

      if (arr.getArrayType() == ArrayType.CHAR) { // turn CHAR into STRING
        if (arr.getRank() < 2) { // common case
          svalue = Arrays.makeStringFromChar((Array<Byte>) arr);
          this.nelems = 1;
          this.dataType = ArrayType.STRING;
          return this;
        }
        // otherwise its an array of Strings
        arr = Arrays.makeStringsFromChar((Array<Byte>) arr);
      }

      if (arr.length() == 1) {
        if (arr.getArrayType().isNumeric()) {
          this.nvalue = (Number) arr.getScalar();
          this.nelems = 1;
          this.dataType = arr.getArrayType();
          return this;
        } else if (arr.getArrayType() == ArrayType.STRING) {
          this.svalue = (String) arr.getScalar();
          this.nelems = 1;
          this.dataType = arr.getArrayType();
          return this;
        }
      }

      if (arr.getRank() > 1) {
        // flatten into 1D array
        arr = Arrays.reshape(arr, new int[] {(int) arr.length()});
      }

      this.values = arr;
      this.nelems = (int) arr.length();
      this.dataType = arr.getArrayType();
      return this;
    }

    public Attribute build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new Attribute(this);
    }
  }


}
