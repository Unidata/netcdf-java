package ucar.nc2.grid2;

import org.junit.Test;
import ucar.array.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/** Test {@link GridAxis} subsetting */
public class TestGridAxisSubset {

  @Test
  public void testRegularPoint() {
    GridAxisPoint.Builder<?> builder =
        GridAxisPoint.builder().setAxisType(AxisType.Ensemble).setName("name").setUnits("unit").setDescription("desc")
            .setRegular(7, 1, 1).setSpacing(GridAxisSpacing.regularPoint).addAttribute(new Attribute("me", "mine"));
    GridAxisPoint axis1D = builder.build();

    Formatter errlog = new Formatter();
    GridAxisPoint subset = axis1D.subset(GridSubset.create().setEnsCoord(3), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(2, 2));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(subset.getUnits()).isEqualTo("unit");
    assertThat(subset.getDescription()).isEqualTo("desc");
    assertThat(subset.getName()).isEqualTo("name");
    assertThat(subset.getAxisType()).isEqualTo(AxisType.Ensemble);
    assertThat(subset.getSubsetRange().name()).isEqualTo(AxisType.Ensemble.toString());
    assertThat(subset.attributes()).isEqualTo(axis1D.attributes());

    int count = 0;
    for (Number val : subset) {
      assertThat(val).isEqualTo(3);
      count++;
    }
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testTimeOffsetIrregular() {
    int n = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.TimeOffset).setName("name")
        .setNcoords(n).setValues(values).setSpacing(GridAxisSpacing.irregularPoint);
    GridAxisPoint axis1D = builder.build();

    // empty subsetting parameters
    Formatter errlog = new Formatter();
    GridAxisPoint subset = axis1D.subset(GridSubset.create(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(7);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(7));
    assertThat((Object) subset).isEqualTo(axis1D);

    int count = 0;
    for (Number val : subset) {
      assertThat(val).isEqualTo(values[count]);
      count++;
    }
    assertThat(count).isEqualTo(7);

    // select single value
    subset = axis1D.subset(GridSubset.create().setTimeOffsetCoord(40.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    count = 0;
    for (Number val : subset) {
      assertThat(val).isEqualTo(40);
      count++;
    }
    assertThat(count).isEqualTo(1);

    // select single value with an interval parameter
    subset =
        axis1D.subset(GridSubset.create().setTimeOffsetCoord(CoordInterval.create(40.0, 41.0)), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    count = 0;
    for (Number val : subset) {
      assertThat(val).isEqualTo(40.0);
      count++;
    }
    assertThat(count).isEqualTo(1);

    // select latest value
    subset = axis1D.subset(GridSubset.create().setTimeLatest(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(6, 6));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    count = 0;
    for (Number val : subset) {
      assertThat(val).isEqualTo(100);
      count++;
    }
    assertThat(count).isEqualTo(1);

    // timePresent - no effect
    subset = axis1D.subset(GridSubset.create().setTimePresent(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(7);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(7));
    assertThat((Object) subset).isEqualTo(axis1D);
  }

  @Test
  public void testVertPointIrregular() {
    int n = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.GeoZ).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values)
        .setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint axis1D = builder.build();

    Formatter errlog = new Formatter();
    GridAxisPoint subset = axis1D.subset(GridSubset.create(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(7);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(7));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);

    subset = axis1D.subset(GridSubset.create().setVertCoord(40.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    subset = axis1D.subset(GridSubset.create().setVertCoord(CoordInterval.create(40.0, 41.0)), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
  }

  @Test
  public void testTimeNominalPoint() {
    int n = 6;
    double[] values = new double[] {2, 4, 8, 15, 50, 80};
    double[] edges = new double[] {0, 3, 5, 10, 20, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.Time).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values).setEdges(edges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint axis1D = builder.build();

    Formatter errlog = new Formatter();
    GridAxisPoint subset = axis1D.subset(GridSubset.create(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(n);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(n));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    subset = axis1D.subset(GridSubset.create().setTimeOffsetCoord(20.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(3, 3));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    subset = axis1D.subset(GridSubset.create().setTimeOffsetCoord(40.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    GridSubset params = GridSubset.create().setTimeOffsetCoord(CoordInterval.create(21.0, 22.0));
    subset = axis1D.subset(params, errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    // if we were just using the midpoint, would expect = 3
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
  }

  @Test
  public void testDescendingVertInterval() {
    GridAxisInterval.Builder<?> builder =
        GridAxisInterval.builder().setAxisType(AxisType.Pressure).setName("name").setUnits("unit")
            .setDescription("desc").setRegular(7, 100.0, -10.0).setSpacing(GridAxisSpacing.regularInterval);
    GridAxisInterval axis1D = builder.build();

    Formatter errlog = new Formatter();
    GridAxisInterval subset = axis1D.subset(GridSubset.create(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(7);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(7));

    subset = axis1D.subset(GridSubset.create().setVertCoord(89.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(1, 1));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(90.0, 80.0));

    subset = axis1D.subset(GridSubset.create().setVertCoord(CoordInterval.create(40.0, 41.0)), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(5, 5));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(50.0, 40.0));
  }

  @Test
  public void testTimeInterval() {
    int n = 6;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisInterval.Builder<?> builder = GridAxisInterval.builder().setAxisType(AxisType.Time).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values)
        .setSpacing(GridAxisSpacing.contiguousInterval).addAttribute(new Attribute("aname", 99.0));
    GridAxisInterval axis1D = builder.build();

    Formatter errlog = new Formatter();
    GridAxisInterval subset = axis1D.subset(GridSubset.create(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(n);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(n));

    subset = axis1D.subset(GridSubset.create().setTimeOffsetCoord(40.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(3, 3));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(20.0, 40.0));

    subset = axis1D.subset(GridSubset.create().setTimeOffsetCoord(41.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(40.0, 80.0));

    subset =
        axis1D.subset(GridSubset.create().setTimeOffsetCoord(CoordInterval.create(40.0, 41.0)), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(40.0, 80.0));

    subset = axis1D.subset(GridSubset.create().setTimeLatest(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(n - 1, n - 1));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(80.0, 100.0));

    // timePresent - no effect
    subset = axis1D.subset(GridSubset.create().setTimePresent(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(n);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(n));
  }

  @Test
  public void testVertDiscontiguousInterval() {
    int n = 6;
    double[] values = new double[] {0, 3, 4, 5, 10, 15, 16, 20, 40, 80, 90, 100};
    GridAxisInterval.Builder<?> builder =
        GridAxisInterval.builder().setAxisType(AxisType.Height).setName("name").setUnits("unit").setDescription("desc")
            .setNcoords(n).setValues(values).setSpacing(GridAxisSpacing.discontiguousInterval);
    GridAxisInterval axis1D = builder.build();

    Formatter errlog = new Formatter();
    GridAxisInterval subset = axis1D.subset(GridSubset.create(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(n);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(n));

    subset = axis1D.subset(GridSubset.create().setVertCoord(40.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(40.0, 80.0));

    subset = axis1D.subset(GridSubset.create().setVertCoord(41.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(40.0, 80.0));

    subset = axis1D.subset(GridSubset.create().setVertCoord(CoordInterval.create(40.0, 41.0)), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(40.0, 80.0));

    // not in an interval
    assertThat(axis1D.subset(GridSubset.create().setVertCoord(39.0), errlog)).isEmpty();
  }

  @Test
  public void testTimeDiscontiguousInterval() {
    int n = 6;
    double[] values = new double[] {0, 3, 0, 6, 10, 15, 16, 20, 40, 80, 90, 100};
    GridAxisInterval.Builder<?> builder =
        GridAxisInterval.builder().setAxisType(AxisType.TimeOffset).setName("name").setUnits("unit")
            .setDescription("desc").setNcoords(n).setValues(values).setSpacing(GridAxisSpacing.discontiguousInterval);
    GridAxisInterval axis1D = builder.build();

    Formatter errlog = new Formatter();
    GridAxisInterval subset = axis1D.subset(GridSubset.create(), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(n);
    assertThat(subset.getSubsetRange()).isEqualTo(new Range(n));

    subset = axis1D.subset(GridSubset.create().setTimeOffsetCoord(40.0), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(40.0, 80.0));

    subset =
        axis1D.subset(GridSubset.create().setTimeOffsetCoord(CoordInterval.create(40.0, 41.0)), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(4, 4));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(40.0, 80.0));

    // outside of any interval
    assertThat(axis1D.subset(GridSubset.create().setTimeOffsetCoord(CoordInterval.create(18.0, 42.0)), errlog))
        .isEmpty();

    // 2 intervals overlap
    subset =
        axis1D.subset(GridSubset.create().setTimeOffsetCoord(CoordInterval.create(2.0, 2.2)), errlog).orElseThrow();
    assertThat(subset.getNominalSize()).isEqualTo(1);
    assertThat(subset.getSubsetRange()).isEqualTo(Range.make(0, 0));
    assertThat(subset.getCoordinate(0)).isEqualTo(CoordInterval.create(0, 3));
  }

}
