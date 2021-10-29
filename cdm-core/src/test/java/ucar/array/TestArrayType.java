/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import java.math.BigInteger;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

/** Test {@link ucar.array.ArrayType} */
public class TestArrayType {

  @Test
  public void testBasics() {
    assertThat(ArrayType.STRING.toString().toUpperCase()).isEqualTo(ArrayType.STRING.name());
    assertThat(ArrayType.STRING.toCdl()).isEqualTo(ArrayType.STRING.toNcml().toLowerCase());
    assertThat(ArrayType.STRING.getPrimitiveClass()).isEqualTo(String.class);

    assertThat(ArrayType.SHORT.getSignedness()).isEqualTo(ArrayType.Signedness.SIGNED);
    assertThat(ArrayType.USHORT.getSignedness()).isEqualTo(ArrayType.Signedness.UNSIGNED);

    assertThat(ArrayType.STRING.isString()).isTrue();
    assertThat(ArrayType.CHAR.isString()).isTrue();
    assertThat(ArrayType.INT.isString()).isFalse();

    assertThat(ArrayType.FLOAT.isNumeric()).isTrue();
    assertThat(ArrayType.UINT.isNumeric()).isTrue();
    assertThat(ArrayType.STRING.isNumeric()).isFalse();
    assertThat(ArrayType.STRUCTURE.isNumeric()).isFalse();

    assertThat(ArrayType.FLOAT.isIntegral()).isFalse();
    assertThat(ArrayType.UINT.isIntegral()).isTrue();
    assertThat(ArrayType.STRING.isIntegral()).isFalse();

    assertThat(ArrayType.FLOAT.isFloatingPoint()).isTrue();
    assertThat(ArrayType.UINT.isFloatingPoint()).isFalse();
    assertThat(ArrayType.STRING.isFloatingPoint()).isFalse();

    assertThat(ArrayType.FLOAT.isEnum()).isFalse();
    assertThat(ArrayType.UINT.isEnum()).isFalse();
    assertThat(ArrayType.STRING.isEnum()).isFalse();
    assertThat(ArrayType.ENUM1.isEnum()).isTrue();
    assertThat(ArrayType.ENUM2.isEnum()).isTrue();
    assertThat(ArrayType.ENUM4.isEnum()).isTrue();
  }

  @Test
  public void testWithSigndedness() {
    assertThat(ArrayType.UINT.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.UINT);
    assertThat(ArrayType.UINT.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.INT);

    assertThat(ArrayType.INT.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.UINT);
    assertThat(ArrayType.INT.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.INT);

    assertThat(ArrayType.USHORT.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.USHORT);
    assertThat(ArrayType.USHORT.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.SHORT);

    assertThat(ArrayType.SHORT.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.USHORT);
    assertThat(ArrayType.SHORT.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.SHORT);

    assertThat(ArrayType.BYTE.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.UBYTE);
    assertThat(ArrayType.BYTE.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.BYTE);

    assertThat(ArrayType.UBYTE.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.UBYTE);
    assertThat(ArrayType.UBYTE.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.BYTE);

    assertThat(ArrayType.LONG.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.ULONG);
    assertThat(ArrayType.LONG.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.LONG);

    assertThat(ArrayType.ULONG.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.ULONG);
    assertThat(ArrayType.ULONG.withSignedness(ArrayType.Signedness.SIGNED)).isEqualTo(ArrayType.LONG);

    assertThat(ArrayType.STRING.withSignedness(ArrayType.Signedness.UNSIGNED)).isEqualTo(ArrayType.STRING);
  }

  @Test
  public void testGetTypeByName() {
    for (ArrayType type : ArrayType.values()) {
      assertThat(ArrayType.getTypeByName(type.name())).isEqualTo(type);
    }
    assertThat(ArrayType.getTypeByName("bad")).isNull();
    assertThat(ArrayType.getTypeByName(null)).isNull();
  }

  @Test
  public void testGetPrimitiveClass() {
    List<ArrayType> list =
        ImmutableList.of(ArrayType.BYTE, ArrayType.DOUBLE, ArrayType.FLOAT, ArrayType.INT, ArrayType.SHORT,
            ArrayType.LONG, ArrayType.UINT, ArrayType.USHORT, ArrayType.ULONG, ArrayType.STRUCTURE, ArrayType.STRING);
    for (ArrayType type : list) {
      assertWithMessage(type.toString()).that(ArrayType.forPrimitiveClass(type.getPrimitiveClass(), type.isUnsigned()))
          .isEqualTo(type);
    }
    assertThrows(RuntimeException.class, () -> ArrayType.forPrimitiveClass(this.getClass(), false));
  }

  @Test
  public void testUnsignedLongToBigInt() {
    assertWithMessage("The long round-trips as expected.").that(Long.MAX_VALUE)
        .isEqualTo(ArrayType.unsignedLongToBigInt(Long.MAX_VALUE).longValueExact());

    BigInteger overflowAsBigInt = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    assertThat(new BigInteger("9223372036854775808")).isEqualTo(overflowAsBigInt);

    long overflowAsLong = overflowAsBigInt.longValue();
    assertThat(-9223372036854775808L).isEqualTo(overflowAsLong);

    assertWithMessage("Interpret signed long as unsigned long").that(overflowAsBigInt)
        .isEqualTo(ArrayType.unsignedLongToBigInt(overflowAsLong));
  }

  @Test
  public void convertUnsignedByte() {
    // Widen regardless
    assertSameNumber((short) 12, ArrayType.widenNumber((byte) 12));

    // Widen only if negative.
    assertSameNumber((byte) 12, ArrayType.widenNumberIfNegative((byte) 12));
    assertSameNumber((short) 155, ArrayType.widenNumberIfNegative((byte) 155));
    assertSameNumber((short) 224, ArrayType.widenNumberIfNegative((byte) -32));
  }

  @Test
  public void convertUnsignedShort() {
    // Widen regardless
    assertSameNumber((int) 3251, ArrayType.widenNumber((short) 3251));

    // Widen only if negative.
    assertSameNumber((short) 3251, ArrayType.widenNumberIfNegative((short) 3251));
    assertSameNumber((int) 40000, ArrayType.widenNumberIfNegative((short) 40000));
    assertSameNumber((int) 43314, ArrayType.widenNumberIfNegative((short) -22222));
  }

  @Test
  public void convertUnsignedInt() {
    // Widen regardless
    assertSameNumber((long) 123456, ArrayType.widenNumber((int) 123456));

    // Widen only if negative.
    assertSameNumber((int) 123456, ArrayType.widenNumberIfNegative((int) 123456));
    assertSameNumber((long) 3500000000L, ArrayType.widenNumberIfNegative((int) 3500000000L));
    assertSameNumber((long) 4289531025L, ArrayType.widenNumberIfNegative((int) -5436271));
  }

  @Test
  public void convertUnsignedLong() {
    // The maximum signed long is 9223372036854775807.
    // So, the number below fits in an unsigned long, but not a signed long.
    BigInteger tenQuintillion = new BigInteger("10000000000000000000"); // Nineteen zeros.

    // Widen regardless
    assertSameNumber(BigInteger.valueOf(3372036854775L), ArrayType.widenNumber((long) 3372036854775L));

    // Widen only if negative.
    assertSameNumber(3372036854775L, ArrayType.widenNumberIfNegative((long) 3372036854775L));
    assertSameNumber(tenQuintillion, ArrayType.widenNumberIfNegative(tenQuintillion.longValue()));
    assertSameNumber(new BigInteger("18446620616920539271"), ArrayType.widenNumberIfNegative((long) -123456789012345L));
  }

  // Asserts the numbers have the same type and value.
  private static void assertSameNumber(Number expected, Number actual) {
    assertThat(expected.getClass()).isEqualTo(actual.getClass());
    assertThat(expected).isEqualTo(actual);
  }
}
