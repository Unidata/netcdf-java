/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TestCoordInterval {

  @Test
  public void testBasics() {
    CoordInterval intv = CoordInterval.create(3, 4);
    assertThat(intv.start()).isEqualTo(3);
    assertThat(intv.end()).isEqualTo(4);
    assertThat(intv.midpoint()).isEqualTo(3.5);
    assertThat(intv.toString()).isEqualTo("3.0-4.0");
    assertThat(intv.toString(0)).isEqualTo("3-4");
    assertThat(intv).isEqualTo(CoordInterval.parse(intv.toString()));
    assertThat(intv.hashCode()).isEqualTo(CoordInterval.parse(intv.toString()).hashCode());
  }

  @Test
  public void testFuzzy() {
    CoordInterval intv = CoordInterval.create(3.001, 4.001);
    assertThat(intv.start()).isEqualTo(3.001);
    assertThat(intv.end()).isEqualTo(4.001);
    assertThat(intv.fuzzyEquals(intv, .00001)).isTrue();
    assertThat(intv.fuzzyEquals(CoordInterval.create(3, 4), .002)).isTrue();
  }

  @Test
  public void testParse() {
    CoordInterval intv = CoordInterval.parse("3-4.159");
    assertThat(intv).isNotNull();
    assertThat(intv.start()).isEqualTo(3);
    assertThat(intv.end()).isEqualTo(4.159);
    assertThat(intv.midpoint()).isEqualTo(3.5795);

    assertThat(CoordInterval.parse("3 4.159")).isNull();
    assertThat(CoordInterval.parse("3")).isNull();
    assertThat(CoordInterval.parse("bad")).isNull();
    assertThat(CoordInterval.parse("")).isNull();
    assertThat(CoordInterval.parse(null)).isNull();

    assertThat(CoordInterval.parse("3-4")).isNotNull();
    assertThat(CoordInterval.parse("3 - 4")).isNull();
    assertThat(CoordInterval.parse("3- 4")).isNull();
    assertThat(CoordInterval.parse(" 3- 4")).isNull();
    assertThat(CoordInterval.parse(" 3-4 ")).isEqualTo(CoordInterval.parse("3-4"));
  }

  @Test
  public void testParseMinusPlus() {
    CoordInterval intv = CoordInterval.parse("-3-4.159");
    assertThat(intv).isNotNull();
    assertThat(intv.start()).isEqualTo(-3);
    assertThat(intv.end()).isEqualTo(4.159);
  }

  @Test
  public void testParseMinusMinus() {
    CoordInterval intv = CoordInterval.parse("-3--4.159");
    assertThat(intv).isNotNull();
    assertThat(intv.start()).isEqualTo(-3);
    assertThat(intv.end()).isEqualTo(-4.159);
  }

  @Test
  public void testParsePlusMinus() {
    CoordInterval intv = CoordInterval.parse("3--4.159");
    assertThat(intv).isNotNull();
    assertThat(intv.start()).isEqualTo(3);
    assertThat(intv.end()).isEqualTo(-4.159);
  }

}
