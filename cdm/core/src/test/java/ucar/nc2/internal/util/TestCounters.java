/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import ucar.nc2.internal.util.Counters.Counter;

/** Test {@link ucar.nc2.internal.util.Counters} */
public class TestCounters {

  @Test
  public void testCounters() {
    Counters counters = new Counters();
    assertThat(counters.get("one")).isNull();
    counters.add("one");
    assertThat(counters.get("one")).isNotNull();
    assertThat(counters.get("two")).isNull();

    Counter one = counters.get("one");
    assertThat(one).isNotNull();
    assertThat(one.getName()).isEqualTo("one");
    assertThat(one.getCount("value1")).isEqualTo(0);
    assertThat(one.count("value1")).isEqualTo(one);
    assertThat(counters.count("one", "value1")).isEqualTo(one);
    assertThat(one.getCount("value1")).isEqualTo(2);

    assertThat(one.getCount("value2")).isEqualTo(0);
    assertThat(one.count("value2")).isEqualTo(one);
    assertThat(one.getCount("value2")).isEqualTo(1);

    assertThat(one.getMode()).isEqualTo("value1");
    assertThat(one.getTotal()).isEqualTo(3);
  }

}
