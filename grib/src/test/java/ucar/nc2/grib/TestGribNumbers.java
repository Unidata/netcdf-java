/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.grib.GribNumbers.convertSignedByte;
import static ucar.nc2.grib.GribNumbers.convertSignedByte2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.array.ArrayType;

@RunWith(JUnit4.class)
public class TestGribNumbers {

  @Test
  public void testConvertSignedByte() {
    System.out.printf("byte == convertSignedByte == convertSignedByte2 == hex%n");
    for (int i = 125; i < 256; i++) {
      byte b = (byte) i;
      System.out.printf("%d == %d == %d == %s%n", b, convertSignedByte(b), convertSignedByte2(b),
          Long.toHexString((long) i));
      assertThat(convertSignedByte(b)).isEqualTo(convertSignedByte2(b));
    }
  }

  @Test
  public void testConvertUnsigned() {
    int val = ArrayType.unsignedByteToShort((byte) -200);
    int val2 = ArrayType.unsignedShortToInt((short) -200);
    assertThat(val).isNotEqualTo(val2);
  }

}
