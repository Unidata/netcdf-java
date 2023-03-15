package ucar.nc2.ft2.coverage;

import org.joda.time.DateTimeUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Optional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TestCoverageCoordAxis1D {

  private static final Instant testInstant = Instant.parse("2023-02-17T00:00:00Z");
  private final int timeDelta = 2;

  @BeforeClass
  public static void SetupTests() {
    DateTimeUtils.setCurrentMillisFixed(testInstant.toEpochMilli());
  }

  @AfterClass
  public static void TeardownTests() {
    DateTimeUtils.setCurrentMillisSystem();
  }

  @Test
  public void TestSubsetPresentTime1D() {
    SubsetParams presentSubsetParams = new SubsetParams();
    presentSubsetParams.setTimePresent();

    CoverageCoordAxis1D timeAxis = create1DTimeAxis();
    Optional<CoverageCoordAxis> subsetTimeAxis = timeAxis.subset(presentSubsetParams);

    assert subsetTimeAxis.isPresent();

    if (!subsetTimeAxis.isPresent()) {
      return;
    }

    assert subsetTimeAxis.get().startValue == timeDelta;
    assert subsetTimeAxis.get().endValue == timeDelta;
    assert subsetTimeAxis.get().isSubset();
  }

  private CoverageCoordAxis1D create1DTimeAxis() {
    Instant refTime = testInstant.minus(timeDelta, ChronoUnit.DAYS);
    String timeUnit = "Day since " + refTime.toString();

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
