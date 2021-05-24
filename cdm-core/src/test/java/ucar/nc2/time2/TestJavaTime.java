/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.time2;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class TestJavaTime {

  @Test
  public void testOffsetDateTime() {
    System.out.printf("allOnes = %s%n", OffsetDateTime.of(1, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC));
    System.out.printf("zeroYear = %s%n", OffsetDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    System.out.printf("minusYear = %s%n", OffsetDateTime.of(-1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
  }

}
