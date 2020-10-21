/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import static com.google.common.truth.Truth.assertThat;

import java.util.Formatter;
import org.junit.Test;

/** Test {@link ucar.nc2.util.Misc} */
public class TestMisc {

  @Test
  public void testCompareBytes() {
    byte[] raw1 = new byte[] {1, 2, 3};
    byte[] raw2 = new byte[] {1, 2, 2};
    byte[] raw3 = new byte[] {1, 2};

    Formatter f;

    assertThat(Misc.compare(raw1, null, null)).isFalse();

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw1, f)).isTrue();
    assertThat(f.toString()).isEmpty();

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw2, f)).isFalse();
    assertThat(f.toString())
        .isEqualTo(String.format("   2 :   3 !=   2%n" + "Misc.compare 3 bytes, 1 are different%n"));

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw3, f)).isFalse();
    assertThat(f.toString()).isEqualTo(String.format("length 1=   3 != length 2=  2%n"));
  }

  @Test
  public void testCompareInt() {
    int[] raw1 = new int[] {1, 2, 3};
    int[] raw2 = new int[] {1, 2, 2};
    int[] raw3 = new int[] {1, 2};

    Formatter f;

    assertThat(Misc.compare(raw1, null, null)).isFalse();

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw1, f)).isTrue();
    assertThat(f.toString()).isEmpty();

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw2, f)).isFalse();
    assertThat(f.toString()).isEqualTo(String.format("   2 :   3 !=   2%n" + "Misc.compare 3 ints, 1 are different%n"));

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw3, f)).isFalse();
    assertThat(f.toString()).isEqualTo(String.format("compareInt: length 1=   3 != length 2=  2%n"));
  }

  @Test
  public void testCompareFloat() {
    float[] raw1 = new float[] {1, 2, 3};
    float[] raw2 = new float[] {1, 2, 2};
    float[] raw3 = new float[] {1, 2};

    Formatter f;

    assertThat(Misc.compare(raw1, null, null)).isFalse();

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw1, f)).isTrue();
    assertThat(f.toString()).isEmpty();

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw2, f)).isFalse();
    assertThat(f.toString())
        .isEqualTo(String.format("     2 : 3.000000 != 2.000000%n" + "Misc.compare 3 floats, 1 are different%n"));

    f = new Formatter();
    assertThat(Misc.compare(raw1, raw3, f)).isFalse();
    assertThat(f.toString()).isEqualTo(String.format("compareFloat: length 1=   3 != length 2=  2%n"));
  }

}
