/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.grib2.Grib2Pds.PdsAerosol;
import ucar.nc2.grib.grib2.Grib2Pds.PdsInterval;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test data from an RRFS 2dfld product, filtered to contain just aerosol records (templates 4.46 and 4.48), and used
 * to create a .gbx9 file for testing. Only the 4.48 sections are tested here.
 */
@RunWith(JUnit4.class)
public class TestPds46 {

  private List<Grib2Pds> sections;

  @Before
  public void openTestFile() {
    String testfile = "../grib/src/test/data/index/rrfs.t00z.2dfld.3km.f003.conus.grib2.gbx9";

    Grib2Index gi = new Grib2Index();
    boolean success = gi.readIndex(testfile, -1);
    assertThat(success).isTrue();
    sections = gi.getRecords().stream().map(Grib2Record::getPDS).filter(e -> e.getTemplateNumber() == 46)
        .collect(Collectors.toList());
    assertThat(sections).hasSize(2);
  }

  @Test
  public void testPdsBasic() {
    sections.forEach(pds -> {
      assertThat(pds.getRawLength()).isEqualTo(71);
      assertThat(pds.getTemplateNumber()).isEqualTo(46);
    });
  }

  @Test
  public void testTemplateLength() {
    sections.forEach(pds -> {
      assertThat(pds.templateLength()).isEqualTo(71);
    });
  }

  @Test
  public void testIsAerosol() {
    sections.forEach(pds -> {
      assertThat(pds.isAerosol()).isTrue();
    });
  }

  @Test
  public void testIsTimeInterval() {
    sections.forEach(pds -> {
      assertThat(pds.isTimeInterval()).isTrue();
    });
  }

  @Test
  public void testAerosolType() {
    sections.forEach(pds -> {
      // 62000: Total aerosol (https://codes.ecmwf.int/grib/format/grib2/ctables/4/230/)
      assertThat(((PdsAerosol) pds).getAerosolType()).isEqualTo(62000);
    });
  }

  @Test
  public void testAerosolIntervalSizeType() {
    sections.forEach(pds -> {
      assertThat(((PdsAerosol) pds).getAerosolIntervalSizeType()).isEqualTo(0);
    });
  }

  @Test
  public void testAerosolSize1() {
    PdsAerosol pds0 = (PdsAerosol) sections.get(0);
    PdsAerosol pds1 = (PdsAerosol) sections.get(1);

    assertThat(pds0.getAerosolSize1()).isWithin(1e-12).of(2.5e-6); // PM 2.5um
    assertThat(pds1.getAerosolSize1()).isWithin(1e-12).of(10e-6); // PM 10um
  }

  @Test
  public void testAerosolSize2() {
    sections.forEach(pds -> {
      assertThat(((PdsAerosol) pds).getAerosolSize2()).isEqualTo(0);
    });
  }

  @Test
  public void testAerosolWavelengthFieldsAreUndefined() {
    sections.forEach(pds -> {
      PdsAerosol aerosol = (PdsAerosol) pds;
      assertThat(aerosol.getAerosolIntervalWavelengthType()).isEqualTo(GribNumbers.UNDEFINED);
      assertThat(aerosol.getAerosolWavelength1()).isEqualTo(GribNumbers.UNDEFINED);
      assertThat(aerosol.getAerosolWavelength2()).isEqualTo(GribNumbers.UNDEFINED);
    });
  }

  @Test
  public void testGenProcessType() {
    sections.forEach(pds -> {
      assertThat(pds.getGenProcessType()).isEqualTo(2);
    });
  }

  @Test
  public void testBackProcessId() {
    sections.forEach(pds -> {
      assertThat(pds.getBackProcessId()).isEqualTo(0);
    });
  }

  @Test
  public void testGenProcessId() {
    sections.forEach(pds -> {
      assertThat(pds.getGenProcessId()).isEqualTo(134);
    });
  }

  @Test
  public void testTimeUnit() {
    sections.forEach(pds -> {
      assertThat(pds.getTimeUnit()).isEqualTo(1);
    });
  }

  @Test
  public void testForecastTime() {
    sections.forEach(pds -> {
      assertThat(pds.getForecastTime()).isEqualTo(2);
    });
  }

  @Test
  public void testLevelType1() {
    sections.forEach(pds -> {
      assertThat(pds.getLevelType1()).isEqualTo(103);
    });
  }

  @Test
  public void testLevelScale1() {
    sections.forEach(pds -> {
      assertThat(pds.getLevelScale1()).isEqualTo(0);
    });
  }

  @Test
  public void testLevelValue1() {
    sections.forEach(pds -> {
      assertThat(pds.getLevelValue1()).isEqualTo(8);
    });
  }

  @Test
  public void testLevelType2() {
    sections.forEach(pds -> {
      assertThat(pds.getLevelType2()).isEqualTo(255);
    });
  }

  @Test
  public void testLevelScale2() {
    sections.forEach(pds -> {
      assertThat(pds.getLevelScale2()).isEqualTo(0);
    });
  }

  @Test
  public void testLevelValue2() {
    sections.forEach(pds -> {
      assertThat(pds.getLevelValue2()).isEqualTo(0);
    });
  }

  @Test
  public void testIntervalTimeEnd() {
    sections.forEach(pds -> {
      assertThat(((PdsInterval) pds).getIntervalTimeEnd().toString()).isEqualTo("2026-09-08T03:00:00Z");
    });
  }

  @Test
  public void testNumberTimeRanges() {
    sections.forEach(pds -> {
      assertThat(((PdsInterval) pds).getNumberTimeRanges()).isEqualTo(1);
    });
  }

  @Test
  public void testNumberMissing() {
    sections.forEach(pds -> {
      assertThat(((PdsInterval) pds).getNumberMissing()).isEqualTo(0);
    });
  }

  @Test
  public void testStatisticalProcessType() {
    sections.forEach(pds -> {
      assertThat(pds.getStatisticalProcessType()).isEqualTo(0);
    });
  }

  @Test
  public void testTimeIntervals() {
    sections.forEach(pds -> {
      Grib2Pds.TimeInterval[] tis = ((PdsInterval) pds).getTimeIntervals();
      assertThat(tis).hasLength(1);
      Grib2Pds.TimeInterval ti = tis[0];
      assertThat(ti.statProcessType).isEqualTo(0);
      assertThat(ti.timeIncrementType).isEqualTo(2);
      assertThat(ti.timeRangeUnit).isEqualTo(1);
      assertThat(ti.timeRangeLength).isEqualTo(1);
      assertThat(ti.timeIncrementUnit).isEqualTo(255);
      assertThat(ti.timeIncrement).isEqualTo(0);
    });
  }
}

// grib_dump -O rrfs.t00z.2dfld.3km.f003.conus.grib2
// ***** FILE: rrfs.t00z.2dfld.3km.f003.conus.grib2
// ...
// ====================== SECTION_4 ( length=71, padding=0 ) ======================
// 1-4 section4Length = 71
// 5 numberOfSection = 4
// 6-7 NV = 0
// 8-9 productDefinitionTemplateNumber = 46 [Average, accumulation, extreme values or other statistically
// processed values at a horizontal level or in a horizontal layer in a continuous or non-continuous time
// interval for atmospheric aerosol (grib2/tables/2/4.0.table) ]
// 10 parameterCategory = 20 [Atmospheric chemical or physical constituents (grib2/tables/2/4.1.0.table) ]
// 11 parameterNumber = 0 [Mass density (concentration) (grib2/tables/2/4.2.0.20.table) ]
// 12-13 constituentType = 62000 [Unknown code table entry () ]
// 14 typeOfSizeInterval = 0 [Below lower limit (grib2/tables/2/4.91.table) ]
// 15 scaleFactorOfFirstSize = 7
// 16-19 scaledValueOfFirstSize = 25
// 20 scaleFactorOfSecondSize = 0
// 21-24 scaledValueOfSecondSize = 0
// 25 typeOfGeneratingProcess = 2 [Forecast (grib2/tables/2/4.3.table) ]
// 26 backgroundProcess = 0
// 27 generatingProcessIdentifier = 134
// 28-29 hoursAfterDataCutoff = 0
// 30 minutesAfterDataCutoff = 0
// 31 indicatorOfUnitForForecastTime = 1 [Hour (grib2/tables/2/4.4.table) ]
// 32-35 forecastTime = 2
// 36 typeOfFirstFixedSurface = 103 [Specified height level above ground (m) (grib2/tables/2/4.5.table ,
// grib2/tables/local/kwbc/1/4.5.table) ]
// 37 scaleFactorOfFirstFixedSurface = 0
// 38-41 scaledValueOfFirstFixedSurface = 8
// 42 typeOfSecondFixedSurface = 255 [Missing (grib2/tables/2/4.5.table , grib2/tables/local/kwbc/1/4.5.table) ]
// 43 scaleFactorOfSecondFixedSurface = 0
// 44-47 scaledValueOfSecondFixedSurface = 0
// 48-49 yearOfEndOfOverallTimeInterval = 2026
// 50 monthOfEndOfOverallTimeInterval = 9
// 51 dayOfEndOfOverallTimeInterval = 8
// 52 hourOfEndOfOverallTimeInterval = 3
// 53 minuteOfEndOfOverallTimeInterval = 0
// 54 secondOfEndOfOverallTimeInterval = 0
// 55 numberOfTimeRanges = 1
// 56-59 numberOfMissingInStatisticalProcess = 0
// 60 typeOfStatisticalProcessing = 0 [Average (grib2/tables/2/4.10.table) ]
// 61 typeOfTimeIncrement = 2 [Successive times processed have same start time of forecast, forecast time is
// incremented (grib2/tables/2/4.11.table) ]
// 62 indicatorOfUnitForTimeRange = 1 [Hour (grib2/tables/2/4.4.table) ]
// 63-66 lengthOfTimeRange = 1
// 67 indicatorOfUnitForTimeIncrement = 255 [Missing (grib2/tables/2/4.4.table) ]
// 68-71 timeIncrement = 0
// ...
// ====================== SECTION_4 ( length=71, padding=0 ) ======================
// 1-4 section4Length = 71
// 5 numberOfSection = 4
// 6-7 NV = 0
// 8-9 productDefinitionTemplateNumber = 46 [Average, accumulation, extreme values or other statistically
// processed values at a horizontal level or in a horizontal layer in a continuous or non-continuous time
// interval for atmospheric aerosol (grib2/tables/2/4.0.table) ]
// 10 parameterCategory = 20 [Atmospheric chemical or physical constituents (grib2/tables/2/4.1.0.table) ]
// 11 parameterNumber = 0 [Mass density (concentration) (grib2/tables/2/4.2.0.20.table) ]
// 12-13 constituentType = 62000 [Unknown code table entry () ]
// 14 typeOfSizeInterval = 0 [Below lower limit (grib2/tables/2/4.91.table) ]
// 15 scaleFactorOfFirstSize = 7
// 16-19 scaledValueOfFirstSize = 100
// 20 scaleFactorOfSecondSize = 0
// 21-24 scaledValueOfSecondSize = 0
// 25 typeOfGeneratingProcess = 2 [Forecast (grib2/tables/2/4.3.table) ]
// 26 backgroundProcess = 0
// 27 generatingProcessIdentifier = 134
// 28-29 hoursAfterDataCutoff = 0
// 30 minutesAfterDataCutoff = 0
// 31 indicatorOfUnitForForecastTime = 1 [Hour (grib2/tables/2/4.4.table) ]
// 32-35 forecastTime = 2
// 36 typeOfFirstFixedSurface = 103 [Specified height level above ground (m) (grib2/tables/2/4.5.table ,
// grib2/tables/local/kwbc/1/4.5.table) ]
// 37 scaleFactorOfFirstFixedSurface = 0
// 38-41 scaledValueOfFirstFixedSurface = 8
// 42 typeOfSecondFixedSurface = 255 [Missing (grib2/tables/2/4.5.table , grib2/tables/local/kwbc/1/4.5.table) ]
// 43 scaleFactorOfSecondFixedSurface = 0
// 44-47 scaledValueOfSecondFixedSurface = 0
// 48-49 yearOfEndOfOverallTimeInterval = 2026
// 50 monthOfEndOfOverallTimeInterval = 9
// 51 dayOfEndOfOverallTimeInterval = 8
// 52 hourOfEndOfOverallTimeInterval = 3
// 53 minuteOfEndOfOverallTimeInterval = 0
// 54 secondOfEndOfOverallTimeInterval = 0
// 55 numberOfTimeRanges = 1
// 56-59 numberOfMissingInStatisticalProcess = 0
// 60 typeOfStatisticalProcessing = 0 [Average (grib2/tables/2/4.10.table) ]
// 61 typeOfTimeIncrement = 2 [Successive times processed have same start time of forecast, forecast time is
// incremented (grib2/tables/2/4.11.table) ]
// 62 indicatorOfUnitForTimeRange = 1 [Hour (grib2/tables/2/4.4.table) ]
// 63-66 lengthOfTimeRange = 1
// 67 indicatorOfUnitForTimeIncrement = 255 [Missing (grib2/tables/2/4.4.table) ]
// 68-71 timeIncrement = 0
// ...
