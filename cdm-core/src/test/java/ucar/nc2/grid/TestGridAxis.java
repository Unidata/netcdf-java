package ucar.nc2.grid;

import org.junit.Test;
import ucar.array.MinMax;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link GridAxis} */
public class TestGridAxis {

  @Test
  public void testRegularPoint() {
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setRegular(7, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.GeoX);
    assertThat(axis1D.getUnits()).isEqualTo("unit");
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(axis1D.getCoordDouble(0)).isEqualTo(0);
    assertThat(axis1D.getCoordDouble(axis1D.getNominalSize() - 1)).isEqualTo(60);
    assertThat(axis1D.getResolution()).isEqualTo(10);
    assertThat(axis1D.getNominalSize()).isEqualTo(7);
    assertThat(Grids.isAscending(axis1D)).isTrue();
    assertThat(axis1D.isRegular()).isTrue();
    assertThat(axis1D.isInterval()).isFalse();
    assertThat(axis1D.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
    assertThat(Grids.getCoordEdgeMinMax(axis1D)).isEqualTo(MinMax.create(-5.0, 65.0));

    for (int i = 0; i < axis1D.getNominalSize(); i++) {
      assertThat(axis1D.getCoordDouble(i)).isEqualTo(10.0 * i);
      assertThat(axis1D.getCoordInterval(i).start()).isEqualTo(10.0 * i - 5.0);
      assertThat(axis1D.getCoordInterval(i).end()).isEqualTo(10.0 * i + 5.0);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(10.0 * i - 5.0, 10.0 * i + 5.0));
    }

    int count = 0;
    for (Number val : axis1D) {
      assertThat(val).isEqualTo(count * 10.0);
      count++;
    }

    count = 0;
    for (Object coord : axis1D) {
      assertThat(coord).isInstanceOf(Double.class);
      double val = (Double) coord;
      assertThat(val).isEqualTo(count * 10.0);
      count++;
    }

    count = 0;
    for (int i = 0; i < axis1D.getNominalSize(); i++) {
      CoordInterval intv = axis1D.getCoordInterval(i);
      assertThat(intv.start()).isEqualTo(10.0 * count - 5.0);
      assertThat(intv.end()).isEqualTo(10.0 * count + 5.0);
      count++;
    }

    GridAxisPoint copy = axis1D.toBuilder().build();
    assertThat((Object) copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
    assertThat(copy.toString()).isEqualTo(axis1D.toString());

    testFailures(axis1D);
  }

  @Test
  public void testIrregularPoint() {
    int n = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values)
        .setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.GeoX);
    assertThat(axis1D.getUnits()).isEqualTo("unit");
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
    assertThat(axis1D.getNominalSize()).isEqualTo(n);
    assertThat(axis1D.getCoordDouble(0)).isEqualTo(0);
    assertThat(axis1D.getCoordDouble(axis1D.getNominalSize() - 1)).isEqualTo(100);
    assertThat(axis1D.getResolution()).isEqualTo(100.0 / (n - 1));
    assertThat(Grids.isAscending(axis1D)).isTrue();
    assertThat(axis1D.isRegular()).isFalse();
    assertThat(axis1D.isInterval()).isFalse();
    assertThat(Grids.getCoordEdgeMinMax(axis1D)).isEqualTo(MinMax.create(-2.5, 110.0));

    for (int i = 0; i < axis1D.getNominalSize(); i++) {
      assertThat(axis1D.getCoordDouble(i)).isEqualTo(values[i]);
      if (i == 0) {
        assertThat(axis1D.getCoordInterval(i).start()).isEqualTo(values[i] - (values[i + 1] - values[i]) / 2);
      } else {
        assertThat(axis1D.getCoordInterval(i).start()).isEqualTo((values[i] + values[i - 1]) / 2);
      }
      if (i == n - 1) {
        assertThat(axis1D.getCoordInterval(i).end()).isEqualTo(values[i] + (values[i] - values[i - 1]) / 2);
      } else {
        assertThat(axis1D.getCoordInterval(i).end()).isEqualTo((values[i] + values[i + 1]) / 2);
      }
    }

    int count = 0;
    for (Number val : axis1D) {
      assertThat(val).isEqualTo(values[count]);
      count++;
    }

    count = 0;
    for (Object coord : axis1D) {
      assertThat(coord).isInstanceOf(Double.class);
      double val = (Double) coord;
      assertThat(val).isEqualTo(values[count]);
      count++;
    }

    GridAxisPoint copy = axis1D.toBuilder().build();
    assertThat((Object) copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
    assertThat(copy.toString()).isEqualTo(axis1D.toString());

    testFailures(axis1D);
  }

  @Test
  public void testNominalPoint() {
    int n = 6;
    double[] values = new double[] {2, 4, 8, 15, 50, 80};
    double[] edges = new double[] {0, 3, 5, 10, 20, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values).setEdges(edges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.GeoX);
    assertThat(axis1D.getUnits()).isEqualTo("unit");
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);
    assertThat(axis1D.getNominalSize()).isEqualTo(n);
    assertThat(axis1D.getCoordDouble(0)).isEqualTo(2);
    assertThat(axis1D.getCoordDouble(axis1D.getNominalSize() - 1)).isEqualTo(80);
    assertThat(axis1D.getResolution()).isEqualTo(78.0 / (n - 1));
    assertThat(Grids.isAscending(axis1D)).isTrue();
    assertThat(axis1D.isRegular()).isFalse();
    assertThat(axis1D.isInterval()).isFalse();
    assertThat(Grids.getCoordEdgeMinMax(axis1D)).isEqualTo(MinMax.create(0, 100.0));

    for (int i = 0; i < axis1D.getNominalSize(); i++) {
      assertThat(axis1D.getCoordDouble(i)).isEqualTo(values[i]);
      assertThat(axis1D.getCoordInterval(i).start()).isEqualTo(edges[i]);
      assertThat(axis1D.getCoordInterval(i).end()).isEqualTo(edges[i + 1]);
    }

    int count = 0;
    for (Number val : axis1D) {
      assertThat(val).isEqualTo(values[count]);
      count++;
    }

    count = 0;
    for (Object coord : axis1D) {
      assertThat(coord).isInstanceOf(Double.class);
      double val = (Double) coord;
      assertThat(val).isEqualTo(values[count]);
      count++;
    }

    GridAxisPoint copy = axis1D.toBuilder().build();
    assertThat((Object) copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
    assertThat(copy.toString()).isEqualTo(axis1D.toString());

    testFailures(axis1D);
  }

  @Test
  public void testNominalPointFail() {
    int n = 6;
    double[] values = new double[] {2, 4, 8, 15, 50, 80};
    double[] edges = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values).setEdges(edges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    try {
      builder.build();
      fail();
    } catch (Throwable e) {
      // expected;
    }
  }


  @Test
  public void testRegularInterval() {
    GridAxisInterval.Builder<?> builder = GridAxisInterval.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setRegular(7, -5.0, 10.0).setSpacing(GridAxisSpacing.regularInterval)
        .setDependenceType(GridAxisDependenceType.fmrcReg).setDependsOn("it depends")
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisInterval axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.GeoX);
    assertThat(axis1D.getUnits()).isEqualTo("unit");
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getNominalSize()).isEqualTo(7);
    assertThat(axis1D.getSpacing()).isEqualTo(GridAxisSpacing.regularInterval);
    assertThat(axis1D.getCoordInterval(0).start()).isEqualTo(-5);
    assertThat(axis1D.getCoordInterval(axis1D.getNominalSize() - 1).end()).isEqualTo(65);
    assertThat(axis1D.getResolution()).isEqualTo(10);
    assertThat(Grids.isAscending(axis1D)).isTrue();
    assertThat(axis1D.isRegular()).isTrue();
    assertThat(axis1D.isInterval()).isTrue();
    assertThat(axis1D.getDependenceType()).isEqualTo(GridAxisDependenceType.fmrcReg);
    assertThat(Grids.getCoordEdgeMinMax(axis1D)).isEqualTo(MinMax.create(-5.0, 65.0));

    for (int i = 0; i < axis1D.getNominalSize(); i++) {
      assertThat(axis1D.getCoordDouble(i)).isEqualTo(10.0 * i);
      assertThat(axis1D.getCoordInterval(i).start()).isEqualTo(10.0 * i - 5.0);
      assertThat(axis1D.getCoordInterval(i).end()).isEqualTo(10.0 * i + 5.0);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(10.0 * i - 5.0, 10.0 * i + 5.0));
    }

    int count = 0;
    for (CoordInterval coord : axis1D) {
      assertThat(coord).isEqualTo(CoordInterval.create(10.0 * count - 5.0, 10.0 * count + 5.0));
      count++;
    }

    GridAxisInterval copy = axis1D.toBuilder().build();
    assertThat((Object) copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
    assertThat(copy.toString()).isEqualTo(axis1D.toString());

    testFailures(axis1D);
  }

  @Test
  public void testContiguousInterval() {
    int n = 6;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisInterval.Builder<?> builder = GridAxisInterval.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values)
        .setSpacing(GridAxisSpacing.contiguousInterval).addAttribute(new Attribute("aname", 99.0));
    GridAxisInterval axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.GeoX);
    assertThat(axis1D.getUnits()).isEqualTo("unit");
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxisSpacing.contiguousInterval);
    assertThat(axis1D.getNominalSize()).isEqualTo(n);
    assertThat(axis1D.getCoordInterval(0).start()).isEqualTo(0);
    assertThat(axis1D.getCoordInterval(axis1D.getNominalSize() - 1).end()).isEqualTo(100);
    assertThat(axis1D.getResolution()).isEqualTo(100.0 / n);
    assertThat(Grids.isAscending(axis1D)).isTrue();
    assertThat(axis1D.isRegular()).isFalse();
    assertThat(axis1D.isInterval()).isTrue();
    assertThat(Grids.getCoordEdgeMinMax(axis1D)).isEqualTo(MinMax.create(0, 100.0));

    for (int i = 0; i < axis1D.getNominalSize(); i++) {
      assertThat(axis1D.getCoordDouble(i)).isEqualTo((values[i] + values[i + 1]) / 2);
      assertThat(axis1D.getCoordInterval(i).start()).isEqualTo(values[i]);
      assertThat(axis1D.getCoordInterval(i).end()).isEqualTo(values[i + 1]);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(values[i], values[i + 1]));
    }

    int count = 0;
    for (CoordInterval val : axis1D) {
      assertThat(val.start()).isEqualTo(values[count]);
      assertThat(val.end()).isEqualTo(values[count + 1]);
      count++;
    }

    GridAxisInterval copy = axis1D.toBuilder().build();
    assertThat((Object) copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
    assertThat(copy.toString()).isEqualTo(axis1D.toString());

    testFailures(axis1D);
  }

  @Test
  public void testDiscontiguousInterval() {
    int n = 6;
    double[] values = new double[] {0, 3, 4, 5, 10, 15, 16, 20, 40, 80, 90, 100};
    GridAxisInterval.Builder<?> builder = GridAxisInterval.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values)
        .setSpacing(GridAxisSpacing.discontiguousInterval).addAttribute(new Attribute("aname", 99.0));
    GridAxisInterval axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.GeoX);
    assertThat(axis1D.getUnits()).isEqualTo("unit");
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxisSpacing.discontiguousInterval);
    assertThat(axis1D.getNominalSize()).isEqualTo(n);
    assertThat(axis1D.getCoordInterval(0).start()).isEqualTo(0);
    assertThat(axis1D.getCoordInterval(axis1D.getNominalSize() - 1).end()).isEqualTo(100);
    assertThat(axis1D.getResolution()).isEqualTo(0);
    assertThat(Grids.isAscending(axis1D)).isTrue();
    assertThat(axis1D.isRegular()).isFalse();
    assertThat(axis1D.isInterval()).isTrue();
    assertThat(Grids.getCoordEdgeMinMax(axis1D)).isEqualTo(MinMax.create(0, 100));

    for (int i = 0; i < axis1D.getNominalSize(); i++) {
      assertThat(axis1D.getCoordDouble(i)).isEqualTo((values[2 * i] + values[2 * i + 1]) / 2);
      assertThat(axis1D.getCoordInterval(i).start()).isEqualTo(values[2 * i]);
      assertThat(axis1D.getCoordInterval(i).end()).isEqualTo(values[2 * i + 1]);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(values[2 * i], values[2 * i + 1]));
    }

    int count = 0;
    for (CoordInterval val : axis1D) {
      assertThat(val.start()).isEqualTo(values[count * 2]);
      assertThat(val.end()).isEqualTo(values[count * 2 + 1]);
      count++;
    }

    count = 0;
    for (Object coord : axis1D) {
      assertThat(coord).isInstanceOf(CoordInterval.class);
      CoordInterval val = (CoordInterval) coord;
      assertThat(val).isEqualTo(CoordInterval.create(values[count], values[count + 1]));
      count += 2;
    }

    GridAxisInterval copy = axis1D.toBuilder().build();
    assertThat((Object) copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
    assertThat(copy.toString()).isEqualTo(axis1D.toString());

    testFailures(axis1D);
  }

  @Test
  public void testPointSpacingFails() {
    try {
      int n = 7;
      double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
      GridAxisPoint.Builder<?> builder =
          GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name").setUnits("unit").setDescription("desc")
              .setNcoords(n).setValues(values).setSpacing(GridAxisSpacing.contiguousInterval).setResolution(13.0)
              .addAttribute(new Attribute("aname", 99.0));
      fail();
    } catch (Exception ok) {
      // ok
    }
  }

  @Test
  public void testIntervalSpacingFails() {
    try {
      int n = 6;
      double[] values = new double[] {0, 3, 4, 5, 10, 15, 16, 20, 40, 80, 90, 100};
      GridAxisInterval.Builder<?> builder = GridAxisInterval.builder().setAxisType(AxisType.GeoX).setName("name")
          .setUnits("unit").setDescription("desc").setNcoords(n).setValues(values)
          .setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
      fail();
    } catch (Exception ok) {
      // ok
    }
  }

  private void testFailures(GridAxis<?> subject) {
    try {
      subject.getCoordDouble(-1);
      fail();
    } catch (Exception ok) {
      // ok
    }
    try {
      subject.getCoordDouble(10000);
      fail();
    } catch (Exception ok) {
      // ok
    }
    try {
      subject.getCoordInterval(-1);
      fail();
    } catch (Exception ok) {
      // ok
    }
    try {
      subject.getCoordInterval(10000);
      fail();
    } catch (Exception ok) {
      // ok
    }
  }

}
