/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import ucar.ma2.DataType;

import javax.annotation.Nullable;
import java.math.BigInteger;

/**
 * Type-safe enumeration of Array data types.
 * <p>
 * OPAQUE: Byte blobs, where length of blobs may be different. Usually stored as a vlen of Array&lt;Byte&gt;.
 * If theres only one, may be just an Array&lt;Byte&gt;.
 * </p>
 */
public enum ArrayType {
  BOOLEAN("boolean", 1, Byte.class, false), //
  BYTE("byte", 1, Byte.class, false), //
  CHAR("char", 1, Byte.class, false), //
  SHORT("short", 2, Short.class, false), //
  INT("int", 4, Integer.class, false), //
  LONG("long", 8, Long.class, false), //
  FLOAT("float", 4, Float.class, false), //
  DOUBLE("double", 8, Double.class, false), //

  UBYTE("ubyte", 1, Byte.class, true), // unsigned byte
  USHORT("ushort", 2, Short.class, true), // unsigned short
  UINT("uint", 4, Integer.class, true), // unsigned int
  ULONG("ulong", 8, Long.class, true), // unsigned long

  ENUM1("enum1", 1, byte.class, false), // byte
  ENUM2("enum2", 2, short.class, false), // short
  ENUM4("enum4", 4, int.class, false), // int

  // object types are variable length, they have 32 bit indices onto a heap inside of a Structure
  STRING("String", 4, String.class, false), // Java String
  STRUCTURE("Structure", 0, StructureData.class, false), // compact storage of heterogeneous fields
  SEQUENCE("Sequence", 4, StructureData.class, false), // Iterator<StructureData>
  OPAQUE("opaque", 1, Array.class, false), // Array<Array<Byte>>, an array of variable length byte arrays

  /** @deprecated legacy, do not use. */
  @Deprecated
  OBJECT("object", 1, Object.class, false), // legacy: size unknown, use with ucar.ma2.Array
  ;

  /**
   * A property of {@link #isIntegral() integral} data types that determines whether they can represent both
   * positive and negative numbers (signed), or only non-negative numbers (unsigned).
   */
  public enum Signedness {
    /** The data type can represent both positive and negative numbers. */
    SIGNED,
    /** The data type can represent only non-negative numbers. */
    UNSIGNED
  }

  private final String niceName;
  private final int size;
  private final Class<?> primitiveClass;
  private final Signedness signedness;

  ArrayType(String s, int size, Class<?> primitiveClass, boolean isUnsigned) {
    this(s, size, primitiveClass, isUnsigned ? Signedness.UNSIGNED : Signedness.SIGNED);
  }

  ArrayType(String s, int size, Class<?> primitiveClass, Signedness signedness) {
    this.niceName = s;
    this.size = size;
    this.primitiveClass = primitiveClass;
    this.signedness = signedness;
  }

  // LOOK CDL name?
  /** The ArrayType name, eg "byte", "float", "String". */
  public String toString() {
    return niceName;
  }

  /**
   * Size in bytes of one element of this data type.
   * Strings dont know, so return 0.
   * Structures return 1.
   *
   * @return Size in bytes of one element of this data type.
   */
  public int getSize() {
    return size;
  }

  /** The primitive Java class type, inverse of forPrimitiveClass() */
  public Class<?> getPrimitiveClass() {
    return primitiveClass;
  }

  /**
   * Returns the {@link Signedness signedness} of this data type.
   * For non-{@link #isIntegral() integral} data types, it is guaranteed to be {@link Signedness#SIGNED}.
   *
   * @return the signedness of this data type.
   */
  public Signedness getSignedness() {
    return signedness;
  }

  /**
   * Returns {@code true} if the data type is {@link Signedness#UNSIGNED unsigned}.
   * For non-{@link #isIntegral() integral} data types, it is guaranteed to be {@code false}.
   *
   * @return {@code true} if the data type is unsigned.
   */
  public boolean isUnsigned() {
    return signedness == Signedness.UNSIGNED;
  }

  /**
   * Is String or Char
   *
   * @return true if String or Char
   */
  public boolean isString() {
    return (this == ArrayType.STRING) || (this == ArrayType.CHAR);
  }

  /**
   * Is Byte, Float, Double, Int, Short, or Long
   *
   * @return true if numeric
   */
  public boolean isNumeric() {
    return (this == ArrayType.FLOAT) || (this == ArrayType.DOUBLE) || isIntegral();
  }

  /**
   * Is Byte, Int, Short, or Long
   *
   * @return true if integral
   */
  public boolean isIntegral() {
    return (this == ArrayType.BYTE) || (this == ArrayType.INT) || (this == ArrayType.SHORT) || (this == ArrayType.LONG)
        || (this == ArrayType.UBYTE) || (this == ArrayType.UINT) || (this == ArrayType.USHORT)
        || (this == ArrayType.ULONG);
  }

  /**
   * Is Float or Double
   *
   * @return true if floating point type
   */
  public boolean isFloatingPoint() {
    return (this == ArrayType.FLOAT) || (this == ArrayType.DOUBLE);
  }

  /**
   * Is this an enumeration types?
   *
   * @return true if ENUM1, 2, or 4
   */
  public boolean isEnum() {
    return (this == ArrayType.ENUM1) || (this == ArrayType.ENUM2) || (this == ArrayType.ENUM4);
  }

  /**
   * Returns an ArrayType that is related to {@code this}, but with the specified signedness.
   * This method is only meaningful for {@link #isIntegral() integral} data types; if it is called on a non-integral
   * type, then {@code this} is simply returned. Examples:
   * 
   * <pre>
   * assert ArrayType.INT.withSignedness(ArrayType.Signedness.UNSIGNED) == ArrayType.UINT; // INT to UINT
   * assert ArrayType.ULONG.withSignedness(ArrayType.Signedness.SIGNED) == ArrayType.LONG; // ULONG to LONG
   * assert ArrayType.SHORT.withSignedness(ArrayType.Signedness.SIGNED) == ArrayType.SHORT; // this: Same signs
   * assert ArrayType.STRING.withSignedness(ArrayType.Signedness.UNSIGNED) == ArrayType.STRING; // this: Non-integral
   * </pre>
   *
   * @param signedness the desired signedness of the returned ArrayType.
   * @return a ArrayType that is related to {@code this}, but with the specified signedness.
   */
  public ArrayType withSignedness(Signedness signedness) {
    switch (this) {
      case BYTE:
      case UBYTE:
        return signedness == Signedness.UNSIGNED ? UBYTE : BYTE;
      case SHORT:
      case USHORT:
        return signedness == Signedness.UNSIGNED ? USHORT : SHORT;
      case INT:
      case UINT:
        return signedness == Signedness.UNSIGNED ? UINT : INT;
      case LONG:
      case ULONG:
        return signedness == Signedness.UNSIGNED ? ULONG : LONG;
    }
    return this;
  }

  /** @deprecated do not use. */
  @Deprecated
  public DataType getDataType() {
    return DataType.valueOf(this.name());
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Find the ArrayType that matches this name.
   *
   * @param name find ArrayType with this name.
   * @return ArrayType or null if no match.
   */
  @Nullable
  public static ArrayType getTypeByName(String name) {
    if (name == null) {
      return null;
    }
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) { // lame!
      return null;
    }
  }

  /** Find the ArrayType used for this primitive class type. */
  public static ArrayType forPrimitiveClass(Class<?> c, boolean isUnsigned) {
    if ((c == float.class) || (c == Float.class))
      return ArrayType.FLOAT;
    if ((c == double.class) || (c == Double.class))
      return ArrayType.DOUBLE;
    if ((c == short.class) || (c == Short.class))
      return isUnsigned ? ArrayType.USHORT : ArrayType.SHORT;
    if ((c == int.class) || (c == Integer.class))
      return isUnsigned ? ArrayType.UINT : ArrayType.INT;
    if ((c == byte.class) || (c == Byte.class))
      return isUnsigned ? ArrayType.UBYTE : ArrayType.BYTE;
    if ((c == char.class) || (c == Character.class))
      return ArrayType.CHAR;
    if ((c == boolean.class) || (c == Boolean.class))
      return ArrayType.BOOLEAN;
    if ((c == long.class) || (c == Long.class))
      return isUnsigned ? ArrayType.ULONG : ArrayType.LONG;
    if (c == String.class)
      return ArrayType.STRING;
    if (c == ucar.array.StructureData.class)
      return ArrayType.STRUCTURE;
    throw new RuntimeException("Unsupported primitive class " + c.getName());
  }

  /**
   * Convert the argument to the next largest integral data type by an unsigned conversion. In the larger data type,
   * the upper-order bits will be zero, and the lower-order bits will be equivalent to the bits in {@code number}.
   * Thus, we are "widening" the argument by prepending a bunch of zero bits to it.
   * <p/>
   * This widening operation is intended to be used on unsigned integral values that are being stored within one of
   * Java's signed, integral data types. For example, if we have the bit pattern "11001010" and treat it as
   * an unsigned byte, it'll have the decimal value "202". However, if we store that bit pattern in a (signed)
   * byte, Java will interpret it as "-52". Widening the byte to a short will mean that the most-significant
   * set bit is no longer the sign bit, and thus Java will no longer consider the value to be negative.
   * <p/>
   * <table border="1">
   * <tr>
   * <th>Argument type</th>
   * <th>Result type</th>
   * </tr>
   * <tr>
   * <td>Byte</td>
   * <td>Short</td>
   * </tr>
   * <tr>
   * <td>Short</td>
   * <td>Integer</td>
   * </tr>
   * <tr>
   * <td>Integer</td>
   * <td>Long</td>
   * </tr>
   * <tr>
   * <td>Long</td>
   * <td>BigInteger</td>
   * </tr>
   * <tr>
   * <td>Any other Number subtype</td>
   * <td>Just return argument</td>
   * </tr>
   * </table>
   *
   * @param number an integral number to treat as unsigned.
   * @return an equivalent but wider value that Java will interpret as non-negative.
   */
  public static Number widenNumber(Number number) {
    if (number instanceof Byte) {
      return unsignedByteToShort(number.byteValue());
    } else if (number instanceof Short) {
      return unsignedShortToInt(number.shortValue());
    } else if (number instanceof Integer) {
      return unsignedIntToLong(number.intValue());
    } else if (number instanceof Long) {
      return unsignedLongToBigInt(number.longValue());
    } else {
      return number;
    }
  }

  /**
   * This method is similar to {@link #widenNumber}, but only integral types <i>that are negative</i> are widened.
   *
   * @param number an integral number to treat as unsigned.
   * @return an equivalent value that Java will interpret as non-negative.
   */
  public static Number widenNumberIfNegative(Number number) {
    if (number instanceof Byte && number.byteValue() < 0) {
      return unsignedByteToShort(number.byteValue());
    } else if (number instanceof Short && number.shortValue() < 0) {
      return unsignedShortToInt(number.shortValue());
    } else if (number instanceof Integer && number.intValue() < 0) {
      return unsignedIntToLong(number.intValue());
    } else if (number instanceof Long && number.longValue() < 0) {
      return unsignedLongToBigInt(number.longValue());
    }

    return number;
  }

  /**
   * Converts the argument to a {@link BigInteger} by an unsigned conversion. In an unsigned conversion to a
   * {@link BigInteger}, zero and positive {@code long} values are mapped to a numerically equal {@link BigInteger}
   * value and negative {@code long} values are mapped to a {@link BigInteger} value equal to the input plus
   * 2<sup>64</sup>.
   *
   * @param l a {@code long} to treat as unsigned.
   * @return the equivalent {@link BigInteger} value.
   */
  public static BigInteger unsignedLongToBigInt(long l) {
    // This is a copy of the implementation of Long.toUnsignedBigInteger(), which is private for some reason.

    if (l >= 0L)
      return BigInteger.valueOf(l);
    else {
      int upper = (int) (l >>> 32);
      int lower = (int) l;

      // return (upper << 32) + lower
      return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32)
          .add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
    }
  }

  /**
   * Converts the argument to a {@code long} by an unsigned conversion. In an unsigned conversion to a {@code long},
   * the high-order 32 bits of the {@code long} are zero and the low-order 32 bits are equal to the bits of the integer
   * argument.
   *
   * Consequently, zero and positive {@code int} values are mapped to a numerically equal {@code long} value and
   * negative {@code int} values are mapped to a {@code long} value equal to the input plus 2<sup>32</sup>.
   *
   * @param i an {@code int} to treat as unsigned.
   * @return the equivalent {@code long} value.
   */
  public static long unsignedIntToLong(int i) {
    return Integer.toUnsignedLong(i);
  }

  /**
   * Converts the argument to an {@code int} by an unsigned conversion. In an unsigned conversion to an {@code int},
   * the high-order 16 bits of the {@code int} are zero and the low-order 16 bits are equal to the bits of the {@code
   * short} argument.
   *
   * Consequently, zero and positive {@code short} values are mapped to a numerically equal {@code int} value and
   * negative {@code short} values are mapped to an {@code int} value equal to the input plus 2<sup>16</sup>.
   *
   * @param s a {@code short} to treat as unsigned.
   * @return the equivalent {@code int} value.
   */
  public static int unsignedShortToInt(short s) {
    return Short.toUnsignedInt(s);
  }

  /**
   * Converts the argument to a {@code short} by an unsigned conversion. In an unsigned conversion to a {@code
   * short}, the high-order 8 bits of the {@code short} are zero and the low-order 8 bits are equal to the bits of
   * the {@code byte} argument.
   *
   * Consequently, zero and positive {@code byte} values are mapped to a numerically equal {@code short} value and
   * negative {@code byte} values are mapped to a {@code short} value equal to the input plus 2<sup>8</sup>.
   *
   * @param b a {@code byte} to treat as unsigned.
   * @return the equivalent {@code short} value.
   */
  public static short unsignedByteToShort(byte b) {
    return (short) Byte.toUnsignedInt(b);
  }
}
