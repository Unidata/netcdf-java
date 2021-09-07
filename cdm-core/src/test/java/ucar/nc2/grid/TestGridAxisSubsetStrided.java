package ucar.nc2.grid;

import org.junit.Test;
import ucar.array.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridAxis} subsetting with strides. */
public class TestGridAxisSubsetStrided {

  @Test
  public void testRegularPoint() {
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setRegular(10, 1, 1).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint axis1D = builder.build();

    GridAxisPoint subset = axis1D.toBuilder().subsetWithStride(3).build();
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(0, 9, 3));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    int count = 0;
    double[] mid = new double[] {1, 4, 7, 10};
    for (Number val : subset) {
      assertThat(val).isEqualTo(mid[count]);
      count++;
    }

    double[] edge = new double[] {.5, 3.5, 6.5, 9.5, 10.5};
    for (int i = 0; i < subset.ncoords; i++) {
      assertThat(subset.getCoordInterval(i).start()).isEqualTo(edge[i]);
      assertThat(subset.getCoordInterval(i).end()).isEqualTo(edge[i + 1]);
    }
  }

  @Test
  public void testIrregularPoint() {
    int n = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.TimeOffset).setName("name")
        .setNcoords(n).setValues(values).setSpacing(GridAxisSpacing.irregularPoint);
    GridAxisPoint axis1D = builder.build();

    GridAxisPoint subset = axis1D.toBuilder().subsetWithStride(2).build();
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    double[] mid = new double[] {0, 10, 40, 100};
    int count = 0;
    for (Number val : subset) {
      assertThat(val).isEqualTo(mid[count]);
      count++;
    }

    double[] edge = new double[] {-2.5, 7.5, 30, 90, 110};
    for (int i = 0; i < subset.ncoords; i++) {
      assertThat(subset.getCoordInterval(i).start()).isEqualTo(edge[i]);
      assertThat(subset.getCoordInterval(i).end()).isEqualTo(edge[i + 1]);
    }
  }

  @Test
  public void testNominalPoint() {
    int n = 6;
    double[] values = new double[] {2, 4, 8, 15, 50, 80, 100, 111};
    double[] edges = new double[] {0, 3, 5, 10, 20, 80, 100, 101, 111};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.Time).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values).setEdges(edges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint axis1D = builder.build();

    GridAxisPoint subset = axis1D.toBuilder().subsetWithStride(3).build();
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    int count = 0;
    double[] mid = new double[] {2, 15, 100};
    for (Number val : subset) {
      assertThat(val).isEqualTo(mid[count]);
      count++;
    }

    double[] edge = new double[] {0, 10, 100, 111};
    for (int i = 0; i < subset.ncoords; i++) {
      assertThat(subset.getCoordInterval(i).start()).isEqualTo(edge[i]);
      assertThat(subset.getCoordInterval(i).end()).isEqualTo(edge[i + 1]);
    }
  }

  @Test
  public void testRegularDescending() {
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.Pressure).setName("name")
        .setUnits("unit").setDescription("desc").setRegular(7, 100.0, -10.0).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint axis1D = builder.build();

    GridAxisPoint subset = axis1D.toBuilder().subsetWithStride(2).build();
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    int count = 0;
    double[] mid = new double[] {100, 80, 60, 40};
    for (Number val : subset) {
      assertThat(val).isEqualTo(mid[count]);
      count++;
    }

    double[] edge = new double[] {105, 85, 65, 45, 35};
    for (int i = 0; i < subset.ncoords; i++) {
      assertThat(subset.getCoordInterval(i).start()).isEqualTo(edge[i]);
      assertThat(subset.getCoordInterval(i).end()).isEqualTo(edge[i + 1]);
    }
  }

  @Test
  public void testIrregularDescending() {
    int n = 7;
    double[] values = new double[] {0, -5, -10, -20, -40, -80, -100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.TimeOffset).setName("name")
        .setNcoords(n).setValues(values).setSpacing(GridAxisSpacing.irregularPoint);
    GridAxisPoint axis1D = builder.build();

    GridAxisPoint subset = axis1D.toBuilder().subsetWithStride(2).build();
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    double[] mid = new double[] {0, -10, -40, -100};
    int count = 0;
    for (Number val : subset) {
      assertThat(val).isEqualTo(mid[count]);
      count++;
    }

    double[] edge = new double[] {2.5, -7.5, -30, -90, -110};
    for (int i = 0; i < subset.ncoords; i++) {
      assertThat(subset.getCoordInterval(i).start()).isEqualTo(edge[i]);
      assertThat(subset.getCoordInterval(i).end()).isEqualTo(edge[i + 1]);
    }
  }

  @Test
  public void testNominalDescending() {
    int n = 6;
    double[] values = new double[] {-2, -4, -8, -15, -50, -80, -100, -111};
    double[] edges = new double[] {0, -3, -5, -10, -20, -80, -100, -101, -111};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.Time).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values).setEdges(edges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint axis1D = builder.build();

    GridAxisPoint subset = axis1D.toBuilder().subsetWithStride(3).build();
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    int count = 0;
    double[] mid = new double[] {-2, -15, -100};
    for (Number val : subset) {
      assertThat(val).isEqualTo(mid[count]);
      count++;
    }

    double[] edge = new double[] {0, -10, -100, -111};
    for (int i = 0; i < subset.ncoords; i++) {
      assertThat(subset.getCoordInterval(i).start()).isEqualTo(edge[i]);
      assertThat(subset.getCoordInterval(i).end()).isEqualTo(edge[i + 1]);
    }
  }
}
