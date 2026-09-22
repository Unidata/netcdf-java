/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class TestGdt33 {

  private Grib2Record record;

  @Before
  public void openTestFile() {
    String testfile = "../grib/src/test/data/index/grib-template-3-33.grib.gbx9";

    Grib2Index gi = new Grib2Index();
    boolean success = gi.readIndex(testfile, -1);
    assertThat(success).isTrue();
    assertThat(gi.getRecords()).hasSize(1);
    assertThat(gi.getGds()).hasSize(1);
    record = gi.getRecords().get(0);
  }

  @Test
  public void testGdsTemplate() {
    Grib2Gds gds = record.getGDS();
    assertThat(gds.template).isEqualTo(33);

    // check that the grid dimensions are parsed out as expected
    assertThat(gds.getNx()).isEqualTo(1909);
    assertThat(gds.getNy()).isEqualTo(1609);
  }

  @Test
  public void testReferenceDateSanity() {
    assertEquals("2026-08-01T00:00:00Z", record.getReferenceDate().toString());
  }

}
