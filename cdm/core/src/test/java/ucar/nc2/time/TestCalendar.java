/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Test {@link ucar.nc2.time.Calendar} */
public class TestCalendar {

  @Test
  public void testGet() {
    for (Calendar cal : Calendar.values()) {
      assertThat(Calendar.get(cal.name())).isEqualTo(cal);
      assertThat(Calendar.get(cal.toString())).isEqualTo(cal);
    }

    assertThat(Calendar.get("standard")).isEqualTo(Calendar.gregorian);
    assertThat(Calendar.get("gregOrian")).isEqualTo(Calendar.gregorian);
    assertThat(Calendar.get("ISO8601")).isEqualTo(Calendar.proleptic_gregorian);
    assertThat(Calendar.get("365_day")).isEqualTo(Calendar.noleap);
    assertThat(Calendar.get("366_day")).isEqualTo(Calendar.all_leap);
    assertThat(Calendar.get("360_day")).isEqualTo(Calendar.uniform30day);
    assertThat(Calendar.get("NONE")).isEqualTo(Calendar.none);

    assertThat(Calendar.get(null)).isNull();
    assertThat(Calendar.get("bad")).isNull();

  }

}
