/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.ForbiddenConversionException;
import ucar.ma2.Index;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.StringUtil2;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An Attribute is a name and a value, used for associating arbitrary metadata with another object.
 * The value can be a one dimensional array of Strings or numeric values.
 * <p/>
 * TODO Attributes will be immutable in 6.
 * TODO Attribute will not extend CDMNode in 6.
 * TODO Attribute will not know who it belongs to in 6 (Group or Variable).
 * TODO Attribute.getFullName() will not exist in 6.
 *
 * @author caron
 */
public class Attribute extends CDMNode {
  /** @deprecated move to jni.Nc4Iosp */
  @Deprecated
  private static final String SPECIALPREFIX = "_";

  /** @deprecated move to jni.Nc4Iosp */
  @Deprecated
  static final String[] SPECIALS =
      {CDM.NCPROPERTIES, CDM.ISNETCDF4, CDM.SUPERBLOCKVERSION, CDM.DAP4_LITTLE_ENDIAN, CDM.EDU_UCAR_PREFIX};

  /**
   * Turn a list into a map
   *
   * @param atts list of attributes
   * @return map of attributes by name
   * @deprecated do not use
   */
  @Deprecated
  public static Map<String, Attribute> makeMap(List<Attribute> atts) {
    int size = (atts == null) ? 1 : atts.size();
    Map<String, Attribute> result = new HashMap<>(size);
    if (atts == null)
      return result;
    for (Attribute att : atts)
      result.put(att.getShortName(), att);
    return result;
  }

  /** @deprecated move to jni.Nc4Iosp */
  @Deprecated
  public static boolean isspecial(Attribute a) {
    String nm = a.getShortName();
    if (nm.startsWith(SPECIALPREFIX)) {
      /* Check for selected special attributes */
      for (String s : SPECIALS) {
        if (nm.startsWith(s))
          return true; /* is special */
      }
    }
    return false; /* is not special */
  }

  ///////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the Attribute name.
   * Not deprecated in version 5 for Attribute.
   */
  @SuppressWarnings("deprecated")
  public String getName() {
    return shortName;
  }

  /** Get the data type of the Attribute value. */
  public DataType getDataType() {
    return dataType;
  }

  /** @deprecated Use Attribute.builder() */
  @Deprecated
  public void setDataType(DataType dt) {
    this.dataType = dt;
  }

  /** Get the EnumTypedef of the Attribute value, if DataType is an ENUM. */
  @Nullable
  public EnumTypedef getEnumType() {
    return this.enumtype;
  }

  /** @deprecated Use Attribute.builder() */
  @Deprecated
  public void setEnumType(EnumTypedef en) {
    this.enumtype = en;
  }

  /**
   * True if value is an array (getLength() > 1)
   *
   * @return if its an array.
   */
  public boolean isArray() {
    return (getLength() > 1);
  }

  /**
   * Get the length of the array of values
   *
   * @return number of elements in the array.
   */
  public int getLength() {
    return nelems;
  }

  /**
   * Get the value as an Array.
   *
   * @return Array of values.
   */
  @Nullable
  public Array getValues() {
    if (values == null && svalue != null) {
      values = Array.factory(DataType.STRING, new int[] {1});
      values.setObject(values.getIndex(), svalue);
      return values;
    }

    return values == null ? null : values.copy();
  }

  /**
   * Get the value as an Object.
   *
   * @param index which index
   * @return ith value as an Object.
   */
  @Nullable
  public Object getValue(int index) {
    if (isString())
      return getStringValue(index);
    return getNumericValue(index);
  }

  /**
   * True if value is of type String and not null.
   *
   * @return if its a String and not null.
   */
  public boolean isString() {
    return (dataType == DataType.STRING) && null != getStringValue();
  }

  /**
   * Retrieve String value; only call if isString() is true.
   *
   * @return String if this is a String valued attribute, else null.
   * @see Attribute#isString
   */
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
   * @see Attribute#isString
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
   * Retrieve numeric value. Equivalent to <code>getNumericValue(0)</code>
   *
   * @return the first element of the value array, or null if its a String that cant be converted.
   */
  @Nullable
  public Number getNumericValue() {
    return getNumericValue(0);
  }

  /// these deal with array-valued attributes

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

    // LOOK can attributes be enum valued? for now, no
    switch (dataType) {
      case STRING:
        try {
          return new Double(getStringValue(index));
        } catch (NumberFormatException e) {
          return null;
        }
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

  @Override
  public String toString() {
    return toString(false);
  }

  /**
   * CDL representation, may be strict.
   * 
   * @param strict if true, create strict CDL, escaping names
   * @return CDL representation
   * @deprecated use CDLWriter
   */
  @Deprecated
  public String toString(boolean strict) {
    Formatter f = new Formatter();
    writeCDL(f, strict, null);
    return f.toString();
  }

  /**
   * Write CDL representation into a Formatter.
   *
   * @param f write into this
   * @param strict if true, create strict CDL, escaping names
   * @deprecated use CDLWriter
   */
  @Deprecated
  protected void writeCDL(Formatter f, boolean strict, String parentname) {
    if (strict && (isString() || this.getEnumType() != null))
      // Force type explicitly for string.
      f.format("string "); // note lower case and trailing blank
    if (strict && parentname != null)
      f.format(NetcdfFiles.makeValidCDLName(parentname));
    f.format(":");
    f.format("%s", strict ? NetcdfFiles.makeValidCDLName(getShortName()) : getShortName());
    if (isString()) {
      f.format(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0)
          f.format(", ");
        String val = getStringValue(i);
        if (val != null)
          f.format("\"%s\"", encodeString(val));
      }
    } else if (getEnumType() != null) {
      f.format(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0)
          f.format(", ");
        EnumTypedef en = getEnumType();
        String econst = getStringValue(i);
        Integer ecint = en.lookupEnumInt(econst);
        if (ecint == null)
          throw new ForbiddenConversionException("Illegal enum constant: " + econst);
        f.format("\"%s\"", encodeString(econst));
      }
    } else {
      f.format(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0)
          f.format(", ");

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

  private static char[] org = {'\b', '\f', '\n', '\r', '\t', '\\', '\'', '\"'};
  private static String[] replace = {"\\b", "\\f", "\\n", "\\r", "\\t", "\\\\", "\\\'", "\\\""};

  @Deprecated
  private static String encodeString(String s) {
    return StringUtil2.replace(s, org, replace);
  }

  /**
   * Copy constructor
   *
   * @param name name of new Attribute
   * @param from copy value from here.
   * @deprecated Use Attribute.toBuilder().build();
   */
  @Deprecated
  public Attribute(String name, Attribute from) {
    super(name);
    if (name == null)
      throw new IllegalArgumentException("Trying to set name to null on " + this);
    setDataType(from.dataType);
    setEnumType(from.enumtype);
    this.nelems = from.nelems;
    this.svalue = from.svalue;
    this.values = from.values;
    setImmutable();
  }

  /**
   * Create a String-valued Attribute.
   *
   * @param name name of Attribute
   * @param val value of Attribute
   */
  public Attribute(String name, String val) {
    super(name);
    if (name == null)
      throw new IllegalArgumentException("Trying to set name to null on " + this);
    this.dataType = DataType.STRING;
    setStringValue(val);
    setImmutable();
  }

  /**
   * Create a scalar, signed, numeric-valued Attribute.
   *
   * @param name name of Attribute
   * @param val value of Attribute
   */
  public Attribute(String name, Number val) {
    this(name, val, false);
  }

  /**
   * Create a scalar numeric-valued Attribute, possibly unsigned.
   *
   * @param name name of Attribute
   * @param val value of Attribute
   * @param isUnsigned if value is unsigned, used only for integer types.
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public Attribute(String name, Number val, boolean isUnsigned) {
    super(name);
    if (name == null)
      throw new IllegalArgumentException("Trying to set name to null on " + this);

    int[] shape = new int[1];
    shape[0] = 1;
    DataType dt = DataType.getType(val.getClass(), isUnsigned);
    this.dataType = dt;
    Array vala = Array.factory(dt, shape);
    Index ima = vala.getIndex();
    vala.setObject(ima.set0(0), val);
    setValues(vala); // make private
    setImmutable();
  }

  /**
   * Construct attribute with Array of values.
   *
   * @param name name of attribute
   * @param values array of values.
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public Attribute(String name, Array values) {
    this(name, values.getDataType());
    setValues(values); // make private
    setImmutable();
  }

  /**
   * Construct an empty attribute with no values
   * 
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public Attribute(String name, DataType dataType) {
    this(name);
    setDataType(dataType);
  }

  /** @deprecated Use Attribute.builder() */
  @Deprecated
  public Attribute(String name, List values) {
    this(name, values, false);
  }

  /**
   * Construct attribute with list of String or Number values.
   * The list determines the attribute type
   * 
   * @param name name of attribute
   * @param values list of values. must be String or Number, must all be the same type, and have at least 1 member
   * @param isUnsigned if the data type is unsigned.
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public Attribute(String name, List values, boolean isUnsigned) {
    this(name);
    if (values == null || values.isEmpty())
      throw new IllegalArgumentException("Cannot determine attribute's type");
    Class c = values.get(0).getClass();
    this.dataType = DataType.getType(c, isUnsigned);
    setValues(values); // make private
    setImmutable();
  }

  /**
   * A copy constructor using a ucar.unidata.util.Parameter.
   * Need to do this so ucar.unidata.geoloc package doesnt depend on ucar.nc2 library
   *
   * @param param copy info from here.
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public Attribute(Parameter param) {
    this(param.getName());

    if (param.isString()) {
      setStringValue(param.getStringValue());

    } else {
      double[] values = param.getNumericValues();
      int n = values.length;
      Array vala = Array.factory(DataType.DOUBLE, new int[] {n}, values);
      setValues(vala); // make private
    }
    setImmutable();
  }

  /**
   * Set the value as a String, trimming trailing zeros
   * 
   * @param val value of Attribute
   */
  private void setStringValue(String val) {
    if (val == null)
      throw new IllegalArgumentException("Attribute value cannot be null");

    // get rid of trailing nul characters
    int len = val.length();
    while ((len > 0) && (val.charAt(len - 1) == 0))
      len--;
    if (len != val.length())
      val = val.substring(0, len);

    this.svalue = val;
    this.nelems = 1;
    this.dataType = DataType.STRING;
  }

  //////////////////////////////////////////
  // the following make this mutable, but its restricted to subclasses, namely DODSAttribute


  /**
   * Constructor. Must also set value
   *
   * @param name name of Attribute
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  protected Attribute(String name) {
    super(name);
    if (name == null)
      throw new IllegalArgumentException("Trying to set name to null on " + this);
  }

  /**
   * Set the values from a list
   * 
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public void setValues(List values) {
    if (values == null || values.isEmpty())
      throw new IllegalArgumentException("Cannot determine attribute's type");
    int n = values.size();
    Class c = values.get(0).getClass();
    Object pa;

    if (c == String.class) {
      String[] va = new String[n];
      pa = va;
      for (int i = 0; i < n; i++)
        va[i] = (String) values.get(i);
    } else if (c == Integer.class) {
      int[] va = new int[n];
      pa = va;
      for (int i = 0; i < n; i++)
        va[i] = (Integer) values.get(i);
    } else if (c == Double.class) {
      double[] va = new double[n];
      pa = va;
      for (int i = 0; i < n; i++)
        va[i] = (Double) values.get(i);
    } else if (c == Float.class) {
      float[] va = new float[n];
      pa = va;
      for (int i = 0; i < n; i++)
        va[i] = (Float) values.get(i);
    } else if (c == Short.class) {
      short[] va = new short[n];
      pa = va;
      for (int i = 0; i < n; i++)
        va[i] = (Short) values.get(i);
    } else if (c == Byte.class) {
      byte[] va = new byte[n];
      pa = va;
      for (int i = 0; i < n; i++)
        va[i] = (Byte) values.get(i);
    } else if (c == Long.class) {
      long[] va = new long[n];
      pa = va;
      for (int i = 0; i < n; i++)
        va[i] = (Long) values.get(i);
    } else {
      throw new IllegalArgumentException("Unknown type for Attribute = " + c.getName());
    }
    setValues(Array.factory(this.dataType, new int[] {n}, pa));
  }


  /**
   * Set the values from an Array
   *
   * @param arr value of Attribute
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public void setValues(Array arr) {
    if (immutable)
      throw new IllegalStateException("Cant modify");

    if (arr == null) {
      dataType = DataType.STRING;
      return;
    }

    if (arr.getElementType() == char.class) { // turn CHAR into STRING
      ArrayChar carr = (ArrayChar) arr;
      if (carr.getRank() == 1) { // common case
        svalue = carr.getString();
        this.nelems = 1;
        this.dataType = DataType.STRING;
        return;
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
  }

  /**
   * Set the name of this Attribute.
   * Attribute names are unique within a NetcdfFile's global set, and within a Variable's set.
   *
   * @param name name of attribute
   * @deprecated Use Attribute.builder()
   */
  @Deprecated
  public synchronized void setName(String name) {
    if (immutable)
      throw new IllegalStateException("Cant modify");
    setShortName(name);
  }

  /**
   * Instances which have same content are equal.
   */
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

  /**
   * Override Object.hashCode() to implement equals.
   */
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

  // TODO make these final in 6.
  private String svalue; // optimization for common case of single String valued attribute
  private DataType dataType;
  private EnumTypedef enumtype;
  private int nelems; // can be 0 or greater
  private Array values; // can this be made immutable?? Otherwise return a copy.

  private Attribute(Builder builder) {
    super(builder.name);
    this.svalue = builder.svalue;
    this.dataType = builder.dataType;
    this.enumtype = builder.enumtype;
    this.nelems = builder.nelems;
    this.values = builder.values;
    this.nelems = (svalue != null) ? 1 : (this.values != null) ? (int) this.values.getSize() : 0;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    Builder b = builder().setName(this.shortName).setDataType(this.dataType).setEnumType(this.enumtype);
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
    private DataType dataType;
    private String svalue; // optimization for common case of single String valued attribute
    private Array values;
    private int nelems;
    private EnumTypedef enumtype;
    private boolean built;

    private Builder() {}

    public Builder setName(String name) {
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
     * @param svalue value of Attribute
     */
    public Builder setStringValue(String svalue) {
      if (svalue == null)
        throw new IllegalArgumentException("Attribute value cannot be null");

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
        throw new IllegalArgumentException("Cannot determine attribute's type");
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
