/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/** Test {@link Calendar} */
public class TestCalendar {

  @Test
  public void testGet() {
    for (Calendar cal : Calendar.values()) {
      assertThat(Calendar.get(cal.name()).orElseThrow()).isEqualTo(cal);
      assertThat(Calendar.get(cal.toString()).orElseThrow()).isEqualTo(cal);
    }

    assertThat(Calendar.get("standard").orElseThrow()).isEqualTo(Calendar.gregorian);
    assertThat(Calendar.get("gregOrian").orElseThrow()).isEqualTo(Calendar.gregorian);
    assertThat(Calendar.get("ISO8601").orElseThrow()).isEqualTo(Calendar.proleptic_gregorian);
    assertThat(Calendar.get("365_day").orElseThrow()).isEqualTo(Calendar.noleap);
    assertThat(Calendar.get("366_day").orElseThrow()).isEqualTo(Calendar.all_leap);
    assertThat(Calendar.get("360_day").orElseThrow()).isEqualTo(Calendar.uniform30day);
    assertThat(Calendar.get("NONE").orElseThrow()).isEqualTo(Calendar.none);

    assertThat(Calendar.get(null)).isEmpty();
    assertThat(Calendar.get("bad")).isEmpty();
  }

}
