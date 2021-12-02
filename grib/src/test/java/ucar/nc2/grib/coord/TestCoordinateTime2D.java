package ucar.nc2.grib.coord;

import com.google.common.truth.Truth;
import org.junit.Test;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.internal.util.Counters;
import ucar.nc2.util.Indent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test {@link CoordinateTime2D}
 */
public class TestCoordinateTime2D {
  static int code = 0;
  static CalendarPeriod timeUnit = CalendarPeriod.of("1 hour");
  static CalendarDate startDate = CalendarDate.fromUdunitIsoDate(null, "1970-01-01T00:00:00").orElseThrow();

  @Test
  public void testCoordinateTime2D() {
    // TODO: orthogonal, regular, and intervals
    CoordinateTime2D subject = makeTimeCoordinate2D(12, 6);
    testShow(subject);
    testCalcDistributions(subject);
  }

  private void testShow(CoordinateTime2D subject) {
    Formatter f = new Formatter();
    subject.showInfo(f, new Indent(2));
    assertThat(f.toString()).contains("runtime=reftime nruns=12 ntimes=6 isOrthogonal=false isRegular=false");
    assertThat(f.toString()).contains("All time values= 0, 1, 2, 3, 4, 5, (n=6)");

    Formatter f2 = new Formatter();
    subject.showCoords(f2);
    assertThat(f2.toString()).contains("runtime=reftime nruns=12 ntimes=6 isOrthogonal=false isRegular=false");
    assertThat(f2.toString()).contains("Time offsets: (1 hours) ref=1970-01-01T00:00Z");
  }

  private void testCalcDistributions(CoordinateTime2D subject) {
    Counters counters = subject.calcDistributions();
    assertThat(counters.toString()).contains("1: count = 5");
  }

  private CoordinateTime2D makeTimeCoordinate2D(int nruns, int ntimes) {
    CoordinateRuntime.Builder1 runBuilder = new CoordinateRuntime.Builder1(timeUnit);
    Map<Object, CoordinateBuilderImpl<Grib1Record>> timeBuilders = new HashMap<>();

    List<CoordinateTime2D.Time2D> vals = new ArrayList<>(nruns * ntimes);
    for (int j = 0; j < nruns; j++) {
      CalendarDate runDate = startDate.add(j, CalendarPeriod.Field.Hour);
      for (int i = 0; i < ntimes; i++) {
        CoordinateTime2D.Time2D time2D = new CoordinateTime2D.Time2D(runDate, (long) i, null);
        vals.add(time2D);

        runBuilder.add(time2D.refDate);
        CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(time2D.refDate);
        if (timeBuilder == null) {
          timeBuilder = new CoordinateTime.Builder1(null, code, timeUnit, time2D.getRefDate());
          timeBuilders.put(time2D.refDate, timeBuilder);
        }
        timeBuilder.add(time2D.time);
      }
    }
    CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();

    List<Coordinate> times = new ArrayList<>(runCoord.getSize());
    for (int idx = 0; idx < runCoord.getSize(); idx++) {
      long runtime = runCoord.getRuntime(idx);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(runtime);
      times.add(timeBuilder.finish());
    }
    Collections.sort(vals);

    return new CoordinateTime2D(code, timeUnit, vals, runCoord, times, null);
  }
}
