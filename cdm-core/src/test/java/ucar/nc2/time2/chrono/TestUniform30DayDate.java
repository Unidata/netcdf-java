/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.time2.chrono;

import org.junit.Test;

import java.util.Random;

import static com.google.common.truth.Truth.assertThat;

public class TestUniform30DayDate {
  private static final Random random = new Random();
  private static final int COUNT = 1000;

  @Test
  public void testOfEpochDay() {
    for (int i = 0; i < COUNT; i++) {
      long epochDay = random.nextInt(100000) * (random.nextBoolean() ? 1 : -1);
      try {
        Uniform30DayDate date = Uniform30DayDate.ofEpochDay(epochDay);
        long roundtrip = date.toEpochDay();
        assertThat(roundtrip).isEqualTo(epochDay);
      } catch (Exception e) {
        System.out.printf("Failed on epochDay = %d%n", epochDay);
      }
    }
    System.out.printf("Tested %d values%n", COUNT);
  }
}
