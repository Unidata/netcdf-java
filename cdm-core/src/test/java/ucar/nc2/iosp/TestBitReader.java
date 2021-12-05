/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ucar.nc2.iosp.BitReader} */
public class TestBitReader {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testUcar() throws IOException {
    BitReader bu = new BitReader(new byte[] {-1, 2, 4, 8});
    assertThat(127).isEqualTo(bu.bits2UInt(7));
    assertThat(1).isEqualTo(bu.bits2UInt(1));
  }

  @Test
  public void testSignedPositive() throws IOException {
    BitReader bu = new BitReader(new byte[] {32, 0, 0, 0});
    assertThat(2).isEqualTo((int) bu.bits2SInt(4));
  }

  @Test
  public void testSignedNegative() throws IOException {
    BitReader bu = new BitReader(new byte[] {(byte) 160, 0, 0, 0});
    int binary = (int) bu.bits2SInt(4);
    assertThat(-2).isEqualTo(binary);
  }

  @Test
  public void testSignedNegative2() throws IOException {
    BitReader bu = new BitReader(new byte[] {(byte) 71, (byte) 200, (byte) 235, (byte) 216, (byte) 128, (byte) 0});
    assertThat(574).isEqualTo((int) bu.bits2UInt(11));
    assertThat(570).isEqualTo((int) bu.bits2UInt(11));
    assertThat(-945).isEqualTo((int) bu.bits2SInt(11));
  }

  // 1100011111110010 0 0 110 110
  // 012345678901234567890123456789012345678901234567890
  // 1 2 3 4 5
  @Test
  public void testUnsigned() throws IOException {
    byte[] bits = new byte[] {(byte) 199, (byte) 242, (byte) 0, (byte) 0, (byte) 6, (byte) 6};
    System.out.printf("%s", Misc.showBits(bits));
    BitReader bu = new BitReader(bits);
    assertThat(799).isEqualTo((int) bu.bits2UInt(10));
    assertThat(800).isEqualTo((int) bu.bits2UInt(10));
    assertThat(0).isEqualTo((int) bu.bits2UInt(10));
    assertThat(6).isEqualTo((int) bu.bits2UInt(10));
    assertThat(6).isEqualTo((int) bu.bits2UInt(8));
  }

}
