/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import org.junit.Test;
import java.math.BigInteger;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link ucar.array.ArrayType} */
public class TestArrayType {

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

  // Asserts tha numbers have the same type and value.
  private static void assertSameNumber(Number expected, Number actual) {
    assertThat(expected.getClass()).isEqualTo(actual.getClass());
    assertThat(expected).isEqualTo(actual);
  }
}
