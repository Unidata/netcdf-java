package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ft2.coverage.CoverageCoordAxis.Spacing;
import ucar.nc2.util.Optional;

@RunWith(Enclosed.class)
public class TestCoordAxisHelper {

  @RunWith(Parameterized.class)
  public static class TestCoordAxisHelperParametrized {

    @Parameterized.Parameters(name = "{0}, {1}")
    public static List<Object[]> getTestParameters() {
      return Arrays.asList(

          new Object[] {Spacing.regularPoint, new double[] {0.0, 10.0, 20.0, 30.0, 40.0}},
          new Object[] {Spacing.irregularPoint, new double[] {0.0, 5.0, 20.0, 30.0, 40.0}},
          new Object[] {Spacing.regularInterval, new double[] {0.0, 10.0, 20.0, 30.0, 40.0}},
          new Object[] {Spacing.contiguousInterval, new double[] {0.0, 5.0, 20.0, 30.0, 40.0}},
          new Object[] {Spacing.discontiguousInterval, new double[] {0.0, 0.0, 20.0, 30.0, 40.0, 40.0, 50.0, 50.0}}

      );
    }

    private final Spacing spacing;
    private final double[] values;

    public TestCoordAxisHelperParametrized(Spacing spacing, double[] values) {
      this.spacing = spacing;
      this.values = values;
    }

    @Test
    public void shouldSubsetAxisByRange() throws InvalidRangeException {
      final AxisType axisType = AxisType.Time;
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

      if (spacing == Spacing.regularPoint || spacing == Spacing.regularInterval) {
        assertThat(subsetBuilder.values).isNull();
      } else {
        assertThat(subsetBuilder.values).isNotNull();
      }
    }
  }

  public static class TestCoordAxisHelperNonParameterized {

    @Test
    public void shouldSubsetSingleValuedAxis() {
      final double[] values = new double[] {42.0};
      final double resolution = 0.0;

      final CoverageCoordAxisBuilder coverageCoordAxisBuilder = new CoverageCoordAxisBuilder("name", "unit",
          "description", DataType.DOUBLE, AxisType.Time, null, CoverageCoordAxis.DependenceType.independent, null,
          Spacing.regularPoint, values.length, values[0], values[values.length - 1], resolution, values, null);
      final CoverageCoordAxis1D coverageCoordAxis = new CoverageCoordAxis1D(coverageCoordAxisBuilder);
      final CoordAxisHelper coordAxisHelper = new CoordAxisHelper(coverageCoordAxis);

      final Optional<CoverageCoordAxisBuilder> subsetBuilder = coordAxisHelper.subset(0.0, 100.0, 1);
      assertThat(subsetBuilder.isPresent()).isTrue();
      assertThat(subsetBuilder.get().startValue).isEqualTo(values[0]);
      assertThat(subsetBuilder.get().endValue).isEqualTo(values[0]);
    }
  }
}
