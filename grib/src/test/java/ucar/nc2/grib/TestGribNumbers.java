package ucar.nc2.grib;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.ma2.DataType;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotEquals;
import static ucar.nc2.grib.GribNumbers.*;

@RunWith(JUnit4.class)
public class TestGribNumbers {

  @Test
  public void testConvertSignedByte() {
    assertThat(convertSignedByte((byte) 0x00)).isEqualTo(0);
    assertThat(convertSignedByte((byte) 0x01)).isEqualTo(1);
    assertThat(convertSignedByte((byte) 0x02)).isEqualTo(2);
    assertThat(convertSignedByte((byte) 0x7d)).isEqualTo(125);
    assertThat(convertSignedByte((byte) 0x7e)).isEqualTo(126);
    assertThat(convertSignedByte((byte) 0x7f)).isEqualTo(127);

    assertThat(convertSignedByte((byte) 0x80)).isEqualTo(-0);
    assertThat(convertSignedByte((byte) 0x81)).isEqualTo(-1);
    assertThat(convertSignedByte((byte) 0x82)).isEqualTo(-2);
    assertThat(convertSignedByte((byte) 0xfd)).isEqualTo(-125);
    assertThat(convertSignedByte((byte) 0xfe)).isEqualTo(-126);
    assertThat(convertSignedByte((byte) 0xff)).isEqualTo(-127);
  }

  @Test
  public void testConvertSignedInt() {
    assertThat(int4(0x00, 0x00, 0x00, 0x00)).isEqualTo(0);
    assertThat(int4(0x00, 0x00, 0x00, 0x01)).isEqualTo(1);
    assertThat(int4(0x00, 0x00, 0x00, 0x02)).isEqualTo(2);
    assertThat(int4(0x7f, 0xff, 0xff, 0xfd)).isEqualTo(2147483645);
    assertThat(int4(0x7f, 0xff, 0xff, 0xfe)).isEqualTo(2147483646);
    assertThat(int4(0x7f, 0xff, 0xff, 0xff)).isEqualTo(2147483647);

    assertThat(int4(0x80, 0x00, 0x00, 0x00)).isEqualTo(-0);
    assertThat(int4(0x80, 0x00, 0x00, 0x01)).isEqualTo(-1);
    assertThat(int4(0x80, 0x00, 0x00, 0x02)).isEqualTo(-2);
    assertThat(int4(0xff, 0xff, 0xff, 0xfd)).isEqualTo(-2147483645);
    assertThat(int4(0xff, 0xff, 0xff, 0xfe)).isEqualTo(-2147483646);
    assertThat(int4(0xff, 0xff, 0xff, 0xff)).isEqualTo(UNDEFINED);
  }

  @Test
  public void testConvertUnsigned() {
    int val = (int) DataType.unsignedByteToShort((byte) -200);
    int val2 = DataType.unsignedShortToInt((short) -200);
    assertNotEquals(val, val2);
  }

  static void assertDecodes(int scaledValue, int scaleFactor, double expected) {
    assertThat(decodeScaledValue(scaledValue, scaleFactor)).isWithin(Math.ulp(expected)).of(expected);
  }

  @Test
  public void testDecodeScaledValue() {

    assertDecodes(9, 1, 0.9);
    assertDecodes(9, 0, 9);
    assertDecodes(9, -1, 90);

    assertDecodes(9, 3, 9e-3);
    assertDecodes(13, 3, 13e-3);
    assertDecodes(18, 3, 18e-3);
    assertDecodes(25, 3, 25e-3);
    assertDecodes(26, 3, 26e-3);
    assertDecodes(36, 3, 36e-3);
    assertDecodes(3, 4, 3e-4);
    assertDecodes(6, 4, 6e-4);
    assertDecodes(9, 4, 9e-4);
    assertDecodes(1, 5, 1e-5);
    assertDecodes(2, 5, 2e-5);
    assertDecodes(10, 5, 10e-5);
    assertDecodes(1, -5, 1e5);
    assertDecodes(8, -5, 8e5);
    assertDecodes(25, -5, 25e5);
    assertDecodes(1, -6, 1e6);
    assertDecodes(8, -6, 8e6);
    assertDecodes(25, -6, 25e6);
    assertDecodes(1, -9, 1e9);
    assertDecodes(33, -9, 33e9);
    assertDecodes(66, -9, 66e9);

    assertDecodes(1, -100, 1e100);
    assertDecodes(1, 100, 1e-100);

    assertDecodes(0x7fffffff, 0, 0x7fffffff);
    assertDecodes(-1, 0, -1);

    assertThat(decodeScaledValue(15, -127)).isEqualTo(1.5e+128);
    assertThat(decodeScaledValue(UNDEFINED, 1)).isEqualTo(-999.9);
    assertThat(decodeScaledValue(UNDEFINED, -127)).isEqualTo(-9.999e+130);

  }
}
