package ucar.nc2.ft2.coverage;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Optional;
import static com.google.common.truth.Truth.assertThat;


public class TestCoverageCoordAxis1D {

  private static final CalendarDate testDate = CalendarDate.present();

  private final int timeDelta = 2;

  @Test
  public void TestSubsetPresentTime1D() {
    SubsetParams presentSubsetParams = new SubsetParams();
    presentSubsetParams.setTimePresent();

    CoverageCoordAxis1D timeAxis = create1DTimeAxis();
    Optional<CoverageCoordAxis> subsetTimeAxis = timeAxis.subset(presentSubsetParams);

    assertThat(subsetTimeAxis.isPresent()).isTrue();

    if (!subsetTimeAxis.isPresent()) {
      return;
    }

    assertThat(subsetTimeAxis.get().startValue).isEqualTo(timeDelta);
    assertThat( subsetTimeAxis.get().endValue).isEqualTo(timeDelta);
    assertThat(subsetTimeAxis.get().isSubset()).isTrue();
  }

  private CoverageCoordAxis1D create1DTimeAxis() {
    CalendarDate refTime = testDate.subtract(CalendarPeriod.of(timeDelta, CalendarPeriod.Field.Day));
    String timeUnit = "Day since " + refTime;

    int valuesLen = 10;
    double[] values = new double[valuesLen];
    for (int i = 0; i < valuesLen; i++) {
      values[i] = i + 1;
    }
    final CoverageCoordAxisBuilder coordAxisBuilder = new CoverageCoordAxisBuilder("time", timeUnit,
        "GRIB forecast or observation time", DataType.DOUBLE, AxisType.Time, null,
        CoverageCoordAxis.DependenceType.independent, null, CoverageCoordAxis.Spacing.regularPoint, values.length,
        values[0], values[values.length - 1], values[1] - values[0], values, null);
    return new CoverageCoordAxis1D(coordAxisBuilder);
  }
}
