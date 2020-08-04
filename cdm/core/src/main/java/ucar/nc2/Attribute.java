/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.ForbiddenConversionException;
import ucar.ma2.Index;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;
import java.nio.ByteBuffer;
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
      Array vala = Array.factory(DataType.DOUBLE, new int[] {n}, values);
      b.setValues(vala); // make private
    }
    return b.build();
  }

  /** Create an Attribute from an Array. See {@link Builder#setValues(Array)} */
  public static Attribute fromArray(String name, Array values) {
    return builder(name).setValues(values).build();
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
    this.name = name;
    this.dataType = DataType.STRING;

    if (val != null) {
      // get rid of trailing nul characters
      int len = val.length();
      while ((len > 0) && (val.charAt(len - 1) == 0)) {
        len--;
      }
      if (len != val.length()) {
        val = val.substring(0, len);
      }
    }

    this.nelems = 1;
    this.svalue = val;
    this.enumtype = null;
    this.values = null;
  }

  /**
   * Create a scalar, signed, numeric-valued Attribute.
   *
   * @param name name of Attribute
   * @param val value of Attribute
   */
  public Attribute(String name, Number val) {
    Preconditions.checkNotNull(Strings.emptyToNull(name), "Attribute name cannot be empty or null");
    Preconditions.checkNotNull(val, "Attribute value cannot be null");
    this.name = name;
    DataType dt = DataType.getType(val.getClass(), false);
    this.dataType = dt;

    Array vala = Array.factory(dt, new int[] {1});
    vala.setObject(vala.getIndex().set0(0), val);

    this.enumtype = null;
    this.svalue = null;
    this.values = vala;
    this.nelems = 1;
  }

  /** Get the data type of the Attribute value. */
  public DataType getDataType() {
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

  /** Get the Attribute name. */
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
        return new Double(svalue);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    // LOOK can attributes be enum valued? for now, no
    Preconditions.checkNotNull(values);
    switch (dataType) {
      case BYTE:
      case UBYTE:
        return values.getByte(index);
      case SHORT:
      case USHORT:
        return values.getShort(index);
      case INT:
      case UINT:
        return values.getInt(index);
      case FLOAT:
        return values.getFloat(index);
      case DOUBLE:
        return values.getDouble(index);
      case LONG:
      case ULONG:
        return values.getLong(index);
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
    if ((index < 0) || (index >= nelems))
      return null;
    return (String) values.getObject(index);
  }

  /**
   * Get the value as an Object (String or Number).
   *
   * @param index index into value Array.
   * @return ith value as an Object.
   */
  @Nullable
  public Object getValue(int index) {
    if (isString())
      return getStringValue(index);
    return getNumericValue(index);
  }

  /** Get the value as an Array. */
  @Nullable
  public Array getValues() {
    if (values == null && svalue != null) {
      Array values = Array.factory(DataType.STRING, new int[] {1});
      values.setObject(values.getIndex(), svalue);
      return values;
    }
    return values == null ? null : values.copy();
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
        int value = getValues().getInt(i);
        String ecname = en.lookupEnumString(value);
        if (ecname == null) {
          // TODO What does the C library do ?
          ecname = Integer.toString(value);
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
  private static final String[] replace = {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\\'", "\\\""};

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
    if (svalue != null)
      result = 37 * result + svalue.hashCode();
    else if (values != null) {
      for (int i = 0; i < getLength(); i++) {
        int h = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
        result = 37 * result + h;
      }
    }
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////////

  private final String name;
  private final DataType dataType;
  private final @Nullable String svalue; // optimization for common case of single String valued attribute
  private final @Nullable EnumTypedef enumtype;
  private final @Nullable Array values; // can this be made immutable?? Otherwise return a copy.
  private final int nelems; // can be 0 or greater

  private Attribute(Builder builder) {
    this.name = builder.name;
    this.svalue = builder.svalue;
    this.dataType = builder.dataType;
    this.enumtype = builder.enumtype;
    this.values = builder.values;
    if (svalue != null) {
      nelems = 1;
    } else if (this.values != null) {
      nelems = (int) this.values.getSize();
    } else {
      nelems = builder.nelems;
    }
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    Builder b = builder().setName(this.name).setDataType(this.dataType).setEnumType(this.enumtype);
    if (this.svalue != null) {
      b.setStringValue(this.svalue);
    } else if (this.values != null) {
      b.setValues(this.values);
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

  /** A builder for Attributes */
  public static class Builder {
    private String name;
    private DataType dataType = DataType.STRING;
    private String svalue; // optimization for common case of single String valued attribute
    private Array values;
    private int nelems;
    private EnumTypedef enumtype;
    private boolean built;

    private Builder() {}

    public Builder setName(String name) {
      Preconditions.checkNotNull(Strings.emptyToNull(name), "Attribute name cannot be empty or null");
      this.name = NetcdfFiles.makeValidCdmObjectName(name);
      return this;
    }

    public Builder setDataType(DataType dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder setEnumType(EnumTypedef enumtype) {
      this.enumtype = enumtype;
      return this;
    }

    public Builder setNumericValue(Number val, boolean isUnsigned) {
      Preconditions.checkNotNull(val, "Attribute value cannot be null");
      int[] shape = {1};
      DataType dt = DataType.getType(val.getClass(), isUnsigned);
      setDataType(dt);
      Array vala = Array.factory(dt, shape);
      Index ima = vala.getIndex();
      vala.setObject(ima.set0(0), val);
      setValues(vala);
      return this;
    }

    /**
     * Set the value as a String, trimming trailing zeros
     * 
     * @param svalue value of Attribute, may not be null. If you want a null valued Attribute, dont set a value.
     */
    public Builder setStringValue(String svalue) {
      Preconditions.checkNotNull(svalue, "Attribute value cannot be null");

      // get rid of trailing nul characters
      int len = svalue.length();
      while ((len > 0) && (svalue.charAt(len - 1) == 0))
        len--;
      if (len != svalue.length())
        svalue = svalue.substring(0, len);

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
      if (values == null || values.isEmpty())
        throw new IllegalArgumentException("values may not be null or empty");
      int n = values.size();
      Class c = values.get(0).getClass();
      Object pa;

      if (c == String.class) {
        this.dataType = DataType.STRING;
        String[] va = new String[n];
        pa = va;
        for (int i = 0; i < n; i++)
          va[i] = (String) values.get(i);
      } else if (c == Integer.class) {
        this.dataType = unsigned ? DataType.UINT : DataType.INT;
        int[] va = new int[n];
        pa = va;
        for (int i = 0; i < n; i++)
          va[i] = (Integer) values.get(i);
      } else if (c == Double.class) {
        this.dataType = DataType.DOUBLE;
        double[] va = new double[n];
        pa = va;
        for (int i = 0; i < n; i++)
          va[i] = (Double) values.get(i);
      } else if (c == Float.class) {
        this.dataType = DataType.FLOAT;
        float[] va = new float[n];
        pa = va;
        for (int i = 0; i < n; i++)
          va[i] = (Float) values.get(i);
      } else if (c == Short.class) {
        this.dataType = unsigned ? DataType.USHORT : DataType.SHORT;
        short[] va = new short[n];
        pa = va;
        for (int i = 0; i < n; i++)
          va[i] = (Short) values.get(i);
      } else if (c == Byte.class) {
        this.dataType = unsigned ? DataType.UBYTE : DataType.BYTE;
        byte[] va = new byte[n];
        pa = va;
        for (int i = 0; i < n; i++)
          va[i] = (Byte) values.get(i);
      } else if (c == Long.class) {
        this.dataType = unsigned ? DataType.ULONG : DataType.LONG;
        long[] va = new long[n];
        pa = va;
        for (int i = 0; i < n; i++)
          va[i] = (Long) values.get(i);
      } else {
        throw new IllegalArgumentException("Unknown type for Attribute = " + c.getName());
      }

      return setValues(Array.factory(this.dataType, new int[] {n}, pa));
    }

    /**
     * Set the values from an Array, and the DataType from values.getElementType().
     */
    public Builder setValues(Array arr) {
      if (arr == null) {
        dataType = DataType.STRING;
        return this;
      }

      if (arr.getElementType() == char.class) { // turn CHAR into STRING
        ArrayChar carr = (ArrayChar) arr;
        if (carr.getRank() == 1) { // common case
          svalue = carr.getString();
          this.nelems = 1;
          this.dataType = DataType.STRING;
          return this;
        }
        // otherwise its an array of Strings
        arr = carr.make1DStringArray();
      }

      // this should be a utility somewhere
      if (arr.getElementType() == ByteBuffer.class) { // turn OPAQUE into BYTE
        int totalLen = 0;
        arr.resetLocalIterator();
        while (arr.hasNext()) {
          ByteBuffer bb = (ByteBuffer) arr.next();
          totalLen += bb.limit();
        }
        byte[] ba = new byte[totalLen];
        int pos = 0;
        arr.resetLocalIterator();
        while (arr.hasNext()) {
          ByteBuffer bb = (ByteBuffer) arr.next();
          System.arraycopy(bb.array(), 0, ba, pos, bb.limit());
          pos += bb.limit();
        }
        arr = Array.factory(DataType.BYTE, new int[] {totalLen}, ba);
      }

      if (DataType.getType(arr) == DataType.OBJECT)
        throw new IllegalArgumentException("Cant set Attribute with type " + arr.getElementType());

      if (arr.getRank() > 1)
        arr = arr.reshape(new int[] {(int) arr.getSize()}); // make sure 1D

      this.values = arr;
      this.nelems = (int) arr.getSize();
      this.dataType = DataType.getType(arr);
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
