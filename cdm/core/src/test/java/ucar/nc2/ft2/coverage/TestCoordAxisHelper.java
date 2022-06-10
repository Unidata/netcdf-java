package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ft2.coverage.CoverageCoordAxis.Spacing;

public class TestCoordAxisHelper {

  @Test
  public void shouldSubsetAxisByRange() throws InvalidRangeException {
    final AxisType axisType = AxisType.Time;
    final double[] values = new double[] {0.0, 10.0, 20.0, 30.0, 40.0};
    final Spacing spacing = Spacing.regularInterval;
    final double resolution = values[1] - values[0];

    final CoverageCoordAxisBuilder coverageCoordAxisBuilder = new CoverageCoordAxisBuilder("name", "unit",
        "description", DataType.DOUBLE, axisType, null, CoverageCoordAxis.DependenceType.independent, null, spacing,
        values.length, values[0], values[values.length - 1], resolution, values, null);
    final CoverageCoordAxis1D coverageCoordAxis = new CoverageCoordAxis1D(coverageCoordAxisBuilder);
    final CoordAxisHelper coordAxisHelper = new CoordAxisHelper(coverageCoordAxis);

    final Range subsetRange = new Range(1, 3);
    final CoverageCoordAxisBuilder subsetBuilder = coordAxisHelper.subsetByIndex(subsetRange);
    assertThat(subsetBuilder).isNotNull();
    assertThat(subsetBuilder.axisType).isEqualTo(axisType);
    assertThat(subsetBuilder.startValue).isEqualTo(values[subsetRange.first()]);
    assertThat(subsetBuilder.endValue).isEqualTo(values[subsetRange.last()]);
    assertThat(subsetBuilder.values).isNull(); // null for regular values
  }
}
