/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test data from github issue https://github.com/Unidata/netcdf-java/pull/1568
 * Data was used to create a .gbx9 file for testing.
 * <p>
 * PDS (Secton 4) output from ecCodes grib_dump at the end of the file, uses as a basis for
 * testing PDS (Secton 4) parsing
 */
@RunWith(JUnit4.class)
public class TestPds40 {

  private Grib2Pds pds;

  @Before
  public void openTestFile() throws IOException {
    String testfile = "../grib/src/test/data/index/kenda-ch1-202606181100-0-poacsnc-ctrl.grib2.gbx9";

    Grib2Index gi = new Grib2Index();
    boolean success = gi.readIndex(testfile, -1);
    assertThat(success).isTrue();
    List<Grib2Record> records = gi.getRecords();
    Grib2Record record = records.get(0);
    pds = record.getPDS();
  }

  @Test
  public void testPdsBasic() {
    assertThat(pds.getRawLength()).isEqualTo(60);
    assertThat(pds.getTemplateNumber()).isEqualTo(40);
  }

  // check overrides
  @Test
  public void testGenProcessType() {
    assertThat(pds.getGenProcessType()).isEqualTo(2);;
  }

  @Test
  public void testBackProcessId() {
    assertThat(pds.getBackProcessId()).isEqualTo(0);;
  }

  @Test
  public void testGenProcessId() {
    assertThat(pds.getGenProcessId()).isEqualTo(151);;
  }

  @Test
  public void testTimeUnit() {
    assertThat(pds.getTimeUnit()).isEqualTo(0);
  }

  @Test
  public void testLevelType1() {
    assertThat(pds.getLevelType1()).isEqualTo(150);
  }

  @Test
  public void testLevelScale1() {
    assertThat(pds.getLevelScale1()).isEqualTo(0);
  }

  @Test
  public void testLevelValue1() {
    assertThat(pds.getLevelValue1()).isEqualTo(80);
  }

  @Test
  public void testLevelType2() {
    assertThat(pds.getLevelType2()).isEqualTo(150);
  }

  @Test
  public void testLevelScale2() {
    assertThat(pds.getLevelScale2()).isEqualTo(0);
  }

  @Test
  public void testLevelValue2() {
    assertThat(pds.getLevelValue2()).isEqualTo(81);
  }

  @Test
  public void testTemplateLength() {
    assertThat(pds.templateLength()).isEqualTo(36);
  }
}

// grib_dump -O kenda-ch1-202606181100-0-poacsnc-ctrl.grib2
// ***** FILE: kenda-ch1-202606181100-0-poacsnc-ctrl.grib2
// ...
// ====================== SECTION_4 ( length=60, padding=0 ) ======================
// 1-4 section4Length = 60
// 5 numberOfSection = 4
// 6-7 NV = 6
// 8-9 productDefinitionTemplateNumber = 40 [Analysis or forecast at a horizontal level or in a horizontal layer at a
// point in time for atmospheric chemical constituents (grib2/tables/15/4.0.table) ]
// 10 parameterCategory = 20 [Atmospheric chemical constituents (grib2/tables/15/4.1.0.table) ]
// 11 parameterNumber = 60 [Unknown code table entry (grib2/tables/15/4.2.0.20.table) ]
// 12-13 constituentType = 62300 [Unknown code table entry (grib2/tables/15/4.230.table) ]
// 14 typeOfGeneratingProcess = 2 [Forecast (grib2/tables/15/4.3.table) ]
// 15 backgroundProcess = 0
// 16 generatingProcessIdentifier = 151
// 17-18 hoursAfterDataCutoff = 0
// 19 minutesAfterDataCutoff = 0
// 20 indicatorOfUnitForForecastTime = 0 [Minute (grib2/tables/15/4.4.table) ]
// 21-24 forecastTime = 0
// 25 typeOfFirstFixedSurface = 150 [Generalized vertical height coordinate (grib2/tables/15/4.5.table) ]
// 26 scaleFactorOfFirstFixedSurface = 0
// 27-30 scaledValueOfFirstFixedSurface = 80
// 31 typeOfSecondFixedSurface = 150 [Generalized vertical height coordinate (grib2/tables/15/4.5.table) ]
// 32 scaleFactorOfSecondFixedSurface = 0
// 33-36 scaledValueOfSecondFixedSurface = 81
// 37-40 nlev = 81
// 41-44 numberOfVGridUsed = 4
// 45-60 uuidOfVGrid = 16 {
// 6f, a9, e9, b5, 8a, d4, 5b, 2a, 94, 7c, 0e, 86, aa, db, 2d, e0
// } # bytes uuidOfVGrid
