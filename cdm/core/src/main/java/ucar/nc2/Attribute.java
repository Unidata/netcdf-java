/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.ArraysConvert;
import ucar.ma2.DataType;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;
import java.util.Formatter;
import java.util.List;

/**
 * An Attribute is a name and a value, used for associating arbitrary metadata with another object.
 * The value can be a one dimensional array of Strings or numeric values.
 */
@Immutable
public class Attribute {

  /** Create an Attribute from a ucar.unidata.util.Parameter. */
  public static Attribute fromParameter(Parameter param) {
    Preconditions.checkNotNull(param);
    Builder b = builder(param.getName());

    if (param.isString()) {
      b.setStringValue(param.getStringValue());
    } else {
      double[] values = param.getNumericValues();
      assert values != null;
      int n = values.length;
      Array<?> vala = Arrays.factory(ArrayType.DOUBLE, new int[] {n}, values);
      b.setArrayValues(vala); // make private
    }
    return b.build();
  }

  /** Create an Attribute from a ucar.unidata.util.Parameter. */
  public static Parameter toParameter(Attribute att) {
    Preconditions.checkNotNull(att);

    if (att.isString()) {
      return new Parameter(att.getShortName(), att.getStringValue());
    } else {
      if (att.getLength() == 1) {
        return new Parameter(att.getShortName(), att.getNumericValue().doubleValue());
      }
      double[] values = new double[att.getLength()];
      for (int idx = 0; idx < att.getLength(); idx++) {
        values[idx] = att.getNumericValue(idx).doubleValue();
      }
      return new Parameter(att.getShortName(), values);
    }
  }

  /** Create an Attribute from an ucar.ma2.Array. */
  public static Attribute fromArray(String name, ucar.ma2.Array values) {
    return builder(name).setValues(values).build();
  }

  /** Create an Attribute from an Array. */
  public static Attribute fromArray(String name, Array<?> values) {
    return builder(name).setArrayValues(values).build();
  }

  /** Create an Attribute with a datatype but no value. */
  public static Attribute emptyValued(String name, DataType dtype) {
    return builder(name).setDataType(dtype).build();
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
    this.dataType = DataType.STRING;
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
   * @param val value of Attribute
   */
  public Attribute(String name, Number val) {
    Preconditions.checkNotNull(Strings.emptyToNull(name), "Attribute name cannot be empty or null");
    Preconditions.checkNotNull(val, "Attribute value cannot be null");
    this.name = name;
    this.dataType = DataType.getType(val.getClass(), false);
    this.nvalue = val;

    this.enumtype = null;
    this.svalue = null;
    this.values = null;
    this.nelems = 1;
  }

  /** @deprecated use getArrayType() */
  @Deprecated
  public DataType getDataType() {
    return dataType;
  }

  /** Get the data type of the Attribute value. */
  public ArrayType getArrayType() {
    return dataType.getArrayType();
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

    if (dataType == DataType.STRING) {
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
    if (dataType != DataType.STRING)
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
    if (dataType != DataType.STRING)
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

  /**
   * Get the value as an Object (String or Number).
   *
   * @param index index into value Array.
   * @return ith value as an Object.
   */
  @Nullable
  public Object getValue(int index) {
    if (isString()) {
      return getStringValue(index);
    }
    return getNumericValue(index);
  }

  /** Get the values as an ucar.ma2.Array. */
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
      ucar.ma2.Array values = ucar.ma2.Array.factory(this.dataType, new int[] {1});
      values.setObject(values.getIndex(), nvalue);
      return ArraysConvert.convertToArray(values);
    }
    return values;
  }

  /** True if value is an array (getLength() > 1) */
  public boolean isArray() {
    return (getLength() > 1);
  }

  /** True if value is of type String and not null. */
  public boolean isString() {
    return (dataType == DataType.STRING) && null != getStringValue();
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
          number = DataType.widenNumber(number);
        }
        f.format("%s", number);

        if (dataType.isUnsigned()) {
          f.format("U");
        }
        if (dataType == DataType.FLOAT)
          f.format("f");
        else if (dataType == DataType.SHORT || dataType == DataType.USHORT) {
          f.format("S");
        } else if (dataType == DataType.BYTE || dataType == DataType.UBYTE) {
          f.format("B");
        } else if (dataType == DataType.LONG || dataType == DataType.ULONG) {
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

    // TODO Array doesnt have equals() !
    if (values != null) {
      for (int i = 0; i < getLength(); i++) {
        int r1 = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
        int r2 = att.isString() ? att.getStringValue(i).hashCode() : att.getNumericValue(i).hashCode();
        if (r1 != r2)
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
        int h = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
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
  private final DataType dataType;
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
    Builder b = builder().setName(this.name).setDataType(this.dataType).setEnumType(this.enumtype);
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
    private DataType dataType = DataType.STRING;
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
      this.dataType = dataType;
      return this;
    }

    public Builder setArrayType(ArrayType dataType) {
      this.dataType = dataType.getDataType();
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
      this.dataType = DataType.getType(val.getClass(), isUnsigned);
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
      this.dataType = DataType.STRING;
      return this;
    }

    /**
     * Set the values from a list of String or one of the primitives
     * Integer, Float, Double, Short, Long, Integer, Byte.
     */
    public Builder setValues(List<Object> values, boolean unsigned) {
      if (values == null || values.isEmpty()) {
        throw new IllegalArgumentException("values may not be null or empty");
      }
      int n = values.size();
      Class<?> c = values.get(0).getClass();
      Object pa;
      if (c == String.class) {
        this.dataType = DataType.STRING;
        String[] va = new String[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = stripZeroes((String) values.get(i));
        }
      } else if (c == Integer.class) {
        this.dataType = unsigned ? DataType.UINT : DataType.INT;
        int[] va = new int[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Integer) values.get(i);
        }
      } else if (c == Double.class) {
        this.dataType = DataType.DOUBLE;
        double[] va = new double[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Double) values.get(i);
        }
      } else if (c == Float.class) {
        this.dataType = DataType.FLOAT;
        float[] va = new float[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Float) values.get(i);
        }
      } else if (c == Short.class) {
        this.dataType = unsigned ? DataType.USHORT : DataType.SHORT;
        short[] va = new short[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Short) values.get(i);
        }
      } else if (c == Byte.class) {
        this.dataType = unsigned ? DataType.UBYTE : DataType.BYTE;
        byte[] va = new byte[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Byte) values.get(i);
        }
      } else if (c == Long.class) {
        this.dataType = unsigned ? DataType.ULONG : DataType.LONG;
        long[] va = new long[n];
        pa = va;
        for (int i = 0; i < n; i++) {
          va[i] = (Long) values.get(i);
        }
      } else {
        throw new IllegalArgumentException("Unknown type for Attribute = " + c.getName());
      }

      return setArrayValues(Arrays.factory(this.dataType.getArrayType(), new int[] {n}, pa));
    }

    /** Set the values from an Array, and the DataType from values.getElementType(). */
    public Builder setValues(ucar.ma2.Array arr) {
      if (arr == null) {
        dataType = DataType.STRING;
        return this;
      }
      setArrayValues(ArraysConvert.convertToArray(arr));
      return this;
    }

    /** Set the values as an Array. */
    public Builder setArrayValues(Array<?> arr) {
      if (arr == null) {
        dataType = DataType.STRING;
        return this;
      }

      if (arr.getArrayType() == ArrayType.CHAR) { // turn CHAR into STRING
        ucar.array.ArrayChar carr = (ucar.array.ArrayChar) arr;
        if (carr.getRank() < 2) { // common case
          svalue = carr.makeStringFromChar();
          this.nelems = 1;
          this.dataType = DataType.STRING;
          return this;
        }
        // otherwise its an array of Strings
        arr = carr.makeStringsFromChar();
      }

      if (arr.length() == 1) {
        if (arr.getArrayType().isNumeric()) {
          this.nvalue = (Number) arr.getScalar();
          this.nelems = 1;
          this.dataType = arr.getArrayType().getDataType();
          return this;
        } else if (arr.getArrayType() == ArrayType.STRING) {
          this.svalue = (String) arr.getScalar();
          this.nelems = 1;
          this.dataType = arr.getArrayType().getDataType();
          return this;
        }
      }

      if (arr.getRank() > 1) {
        // flatten into 1D array
        arr = Arrays.reshape(arr, new int[] {(int) arr.length()});
      }

      this.values = arr;
      this.nelems = (int) arr.length();
      this.dataType = arr.getArrayType().getDataType();
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
