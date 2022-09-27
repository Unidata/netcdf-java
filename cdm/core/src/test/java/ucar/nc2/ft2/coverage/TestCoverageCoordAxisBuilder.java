
package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ft2.coverage.CoverageCoordAxis.Spacing;

@RunWith(Parameterized.class)
public class TestCoverageCoordAxisBuilder {

  @Parameterized.Parameters
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(

        new Object[] {Spacing.regularPoint, 10.0, false, new double[] {0.0, 10.0, 20.0, 30.0}},
        new Object[] {Spacing.irregularPoint, 5.0, false, new double[] {0.0, 5.0, 20.0, 30.0}},
        new Object[] {Spacing.regularInterval, 10.0, true, new double[] {0.0, 10.0, 10.0, 20.0}},
        new Object[] {Spacing.contiguousInterval, 10.0, true, new double[] {0.0, 10.0, 10.0, 30.0}},
        new Object[] {Spacing.discontiguousInterval, 20.0, true, new double[] {0.0, 0.0, 20.0, 30.0, 40.0, 50.0}},

        // regular except for a missing point
        new Object[] {Spacing.irregularPoint, 1.0, false, new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 9.0}},
        new Object[] {Spacing.regularPoint, 1.0, false,
            new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0,
                18.0, 19.0, 20.0, 21.0, 22.0, 23.0}},

        // regular except for a repeated point
        new Object[] {Spacing.irregularPoint, 1.0, false,
            new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 5.0, 6.0, 8.0, 9.0}},

        // This is based on the HRRR data's reftime values.
        // There is a missing point and the last 18 values are the same
        new Object[] {Spacing.irregularPoint, 1.0, false,
            new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0,
                17.0, 18.0, 18.0, 18.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0, 31.0, 32.0, 33.0,
                34.0, 35.0, 36.0, 37.0, 38.0, 39.0, 40.0, 41.0, 42.0, 43.0, 44.0, 45.0, 46.0, 47.0, 48.0, 49.0, 50.0,
                51.0, 52.0, 53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0, 60.0, 61.0, 62.0, 63.0, 64.0, 65.0, 66.0, 67.0,
                68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0, 68.0,
                68.0}}

    );
  }

  private final Spacing expectedSpacing;
  private final double expectedResolution;
  private final boolean isInterval;
  private final double[] values;

  public TestCoverageCoordAxisBuilder(Spacing expectedSpacing, double expectedResolution, boolean isInterval,
      double[] values) {
    this.expectedSpacing = expectedSpacing;
    this.expectedResolution = expectedResolution;
    this.isInterval = isInterval;
    this.values = values;
  }

  @Test
  public void shouldSetSpacingFromValues() {
    final int nCoords = isInterval ? values.length / 2 : values.length;
    final CoverageCoordAxisBuilder coverageCoordAxisBuilder =
        new CoverageCoordAxisBuilder("name", "unit", "description", DataType.DOUBLE, AxisType.Time, null,
            CoverageCoordAxis.DependenceType.independent, null, null, nCoords, -1, -1, -1, values, null);
    coverageCoordAxisBuilder.setSpacingFromValues(isInterval);

    assertThat(coverageCoordAxisBuilder.spacing).isEqualTo(expectedSpacing);
    assertThat(coverageCoordAxisBuilder.startValue).isEqualTo(values[0]);
    assertThat(coverageCoordAxisBuilder.endValue).isEqualTo(values[values.length - 1]);
    assertThat(coverageCoordAxisBuilder.resolution).isEqualTo(expectedResolution);
  }
}
