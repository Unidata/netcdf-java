/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.collection.GribDataValidator;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** internal class for debugging. */
public class GribCoverageValidator implements GribDataValidator {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void validate(GribTables cust, RandomAccessFile rafData, long dataPos, GridSubset coords) throws IOException {
    if (cust instanceof Grib1Customizer)
      validateGrib1((Grib1Customizer) cust, rafData, dataPos, coords);
    else
      validateGrib2((Grib2Tables) cust, rafData, dataPos, coords);
  }

  public void validateGrib1(Grib1Customizer cust, RandomAccessFile rafData, long dataPos, GridSubset coords)
      throws IOException {
    rafData.seek(dataPos);
    Grib1Record gr = new Grib1Record(rafData);
    Grib1SectionProductDefinition pds = gr.getPDSsection();

    // runtime
    CalendarDate wantRuntime = coords.getRunTime();
    CalendarDate refdate = gr.getReferenceDate();
    assertThat(refdate).isEqualTo(wantRuntime);

    // time offset
    Double timeOffset = coords.getTimeOffset();
    if (timeOffset == null) {
      logger.debug("no timeOffsetCoord ");
      return;
    }

    Grib1ParamTime ptime = gr.getParamTime(cust);
    if (ptime.isInterval()) {
      int tinv[] = ptime.getInterval();
      assertWithMessage("time coord lower").that(tinv[0] <= timeOffset); // lower <= time
      assertWithMessage("time coord lower").that(tinv[1] >= timeOffset); // upper >= time
    } else {
      assertThat(Misc.nearlyEquals(timeOffset, ptime.getForecastTime())).isTrue();
    }

    // vert
    Double wantVert = coords.getVertPoint();
    if (wantVert != null) {
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      float lev1 = plevel.getValue1();
      if (cust.isLayer(pds.getLevelType())) {
        float lev2 = plevel.getValue2();
        double lower = Math.min(lev1, lev2);
        double upper = Math.max(lev1, lev2);
        assertWithMessage("vert coord lower").that(lower <= wantVert); // lower <= vert
        assertWithMessage("vert coord upper").that(upper >= wantVert); // upper >= vert

      } else {
        assertThat(Misc.nearlyEquals(lev1, wantVert)).isTrue();
      }
    }

    // ens
    Number wantEns = coords.getEnsCoord();
    if (wantEns != null) {
      assertThat(Misc.nearlyEquals(pds.getPerturbationNumber(), wantEns.doubleValue())).isTrue();
    }

  }

  public void validateGrib2(Grib2Tables cust, RandomAccessFile rafData, long dataPos, GridSubset coords)
      throws IOException {
    Grib2Record gr = Grib2RecordScanner.findRecordByDrspos(rafData, dataPos);
    Grib2Pds pds = gr.getPDS();

    // runtime
    CalendarDate wantRuntime = coords.getRunTime();
    CalendarDate refdate = gr.getReferenceDate();
    assertThat(wantRuntime).isEqualTo(refdate);

    // time offset
    CalendarDate wantTimeOffset = coords.getTimeOffsetDate();
    if (gr.getPDS().isTimeInterval()) {
      TimeCoordIntvDateValue tinv = cust.getForecastTimeInterval(gr);
      CoordInterval wantTimeOffsetIntv = coords.getTimeOffsetIntv();
      if (wantTimeOffset != null) {
        assertWithMessage("time coord lower").that(tinv.getStart().isAfter(wantTimeOffset)).isFalse(); // lower <= time
        assertWithMessage("time coord upper").that(tinv.getEnd().isBefore(wantTimeOffset)).isFalse();// upper >= time

      } else if (wantTimeOffsetIntv != null) {
        int[] gribIntv = cust.getForecastTimeIntervalOffset(gr);

        assertWithMessage("time coord lower").that(wantTimeOffsetIntv.start()).isEqualTo(gribIntv[0]);
        assertWithMessage("time coord upper").that(wantTimeOffsetIntv.end()).isEqualTo(gribIntv[1]);
      }

    } else {
      CalendarDate fdate = cust.getForecastDate(gr);
      if (!fdate.equals(wantTimeOffset))
        logger.debug("forecast date");
      assertThat(wantTimeOffset).isEqualTo(fdate);
    }

    // vert
    Double vertCoord = coords.getVertPoint();
    CoordInterval vertCoordIntv = coords.getVertIntv();
    double level1val = pds.getLevelValue1();

    if (vertCoordIntv != null) {
      assertThat(Grib2Utils.isLayer(pds)).isTrue();
      double level2val = pds.getLevelValue2();
      // double lower = Math.min(level1val, level2val);
      // double upper = Math.max(level1val, level2val);
      // assertWithMessage("vert coord lower", lower <= wantVert); // lower <= vert
      // assertWithMessage("vert coord upper", upper >= wantVert); // upper >= vert
      assertThat(Misc.nearlyEquals(vertCoordIntv.start(), level1val, 1e-6)).isTrue();
      assertThat(Misc.nearlyEquals(vertCoordIntv.end(), level2val, 1e-6)).isTrue();

    } else if (vertCoord != null) {
      assertThat(Misc.nearlyEquals(vertCoord, level1val, 1e-6)).isTrue();
    }

    // ens
    Number wantEns = coords.getEnsCoord();
    if (wantEns != null) {
      Grib2Pds.PdsEnsemble pdse = (Grib2Pds.PdsEnsemble) pds;
      assertThat(Misc.nearlyEquals(wantEns.doubleValue(), pdse.getPerturbationNumber())).isTrue();
    }
  }
}
