package ucar.nc2.grid;

import org.junit.Test;
import ucar.array.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.grid.TimeHelper;
import ucar.nc2.util.MinMax;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridAxis1DTime.Builder} */
public class TestGridAxis1DTimeBuilder {
  private static final String unitString = "days since 2020-11-01 0:00 GMT";

  @Test
  public void testFromVariableDS() {
    Group.Builder parentb = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setDataType(DataType.FLOAT)
        .setUnits(unitString).setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll()).setAutoGen(0.0, 10.0)
        .addAttribute(new Attribute(CF.CALENDAR, "noleap")).setParentGroupBuilder(parentb).setDimensionsByName("dim1");
    parentb.addVariable(vdsBuilder);
    Group parent = parentb.build();
    VariableDS vds = (VariableDS) parent.findVariableLocal("name");
    assertThat(vds).isNotNull();

    GridAxis1DTime.Builder<?> builder = GridAxis1DTime.builder(vds);
    try {
      Array<Double> array = ucar.array.Arrays.toDouble(vds.readArray());
      double[] values = new double[(int) array.length()];
      int count = 0;
      for (double val : array) {
        values[count++] = val;
      }
      builder.setValues(values).setNcoords(values.length).setSpacing(GridAxis.Spacing.irregularPoint);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    GridAxis1DTime axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(axis1D.getUnits()).isEqualTo(unitString);
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeString(CDM.UNITS, "")).isEqualTo(unitString);
    assertThat(axis1D.attributes().findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeString(CF.CALENDAR, "")).isEqualTo("noleap");

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxis.Spacing.irregularPoint);
    assertThat(axis1D.getNcoords()).isEqualTo(7);
    assertThat(axis1D.isAscending()).isTrue();
    assertThat(axis1D.getCoordEdgeMinMax()).isEqualTo(MinMax.create(-5.0, 65.0));

    for (int i = 0; i < axis1D.getNcoords(); i++) {
      assertThat(axis1D.getCoordMidpoint(i)).isEqualTo(10.0 * i);
      assertThat(axis1D.getCoordEdge1(i)).isEqualTo(10.0 * i - 5.0);
      assertThat(axis1D.getCoordEdge2(i)).isEqualTo(10.0 * i + 5.0);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(10.0 * i - 5.0, 10.0 * i + 5.0));
    }

    int count = 0;
    for (double val : axis1D.getCoordsAsArray()) {
      assertThat(val).isEqualTo(count * 10.0);
      count++;
    }

    count = 0;
    for (double val : axis1D.getCoordBoundsAsArray()) {
      if (count % 2 == 0) {
        assertThat(val).isEqualTo(10.0 * (count / 2) - 5.0);
      } else {
        assertThat(val).isEqualTo(10.0 * (count / 2) + 5.0);
      }
      count++;
    }

    GridAxis1DTime copy = axis1D.toBuilder().build();
    assertThat(copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());

    assertThat(axis1D.getTimeHelper()).isEqualTo(TimeHelper.factory(unitString, axis1D.attributes()));
  }

  @Test
  public void testRegularPoint() {
    GridAxis1DTime.Builder<?> builder = GridAxis1DTime.builder().setName("name").setUnits(unitString)
        .setDescription("desc").setRegular(7, 0.0, 60.0, 10.0).setSpacing(GridAxis.Spacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxis1DTime axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(axis1D.getUnits()).isEqualTo(unitString);
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxis.Spacing.regularPoint);
    assertThat(axis1D.getStartValue()).isEqualTo(0);
    assertThat(axis1D.getEndValue()).isEqualTo(60);
    assertThat(axis1D.getResolution()).isEqualTo(10);
    assertThat(axis1D.getNcoords()).isEqualTo(7);
    assertThat(axis1D.isAscending()).isTrue();
    assertThat(axis1D.isRegular()).isTrue();
    assertThat(axis1D.isInterval()).isFalse();
    assertThat(axis1D.getDependenceType()).isEqualTo(GridAxis.DependenceType.independent);
    assertThat(axis1D.getDependsOn()).isEmpty();
    assertThat(axis1D.getCoordEdgeMinMax()).isEqualTo(MinMax.create(-5.0, 65.0));

    for (int i = 0; i < axis1D.getNcoords(); i++) {
      assertThat(axis1D.getCoordMidpoint(i)).isEqualTo(10.0 * i);
      assertThat(axis1D.getCoordEdge1(i)).isEqualTo(10.0 * i - 5.0);
      assertThat(axis1D.getCoordEdge2(i)).isEqualTo(10.0 * i + 5.0);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(10.0 * i - 5.0, 10.0 * i + 5.0));
    }

    int count = 0;
    for (double val : axis1D.getCoordsAsArray()) {
      assertThat(val).isEqualTo(count * 10.0);
      count++;
    }

    count = 0;
    for (double val : axis1D.getCoordBoundsAsArray()) {
      if (count % 2 == 0) {
        assertThat(val).isEqualTo(10.0 * (count / 2) - 5.0);
      } else {
        assertThat(val).isEqualTo(10.0 * (count / 2) + 5.0);
      }
      count++;
    }

    GridAxis1DTime copy = axis1D.toBuilder().build();
    assertThat(copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());

    assertThat(copy.toString()).isEqualTo(axis1D.toString());
    assertThat(copy.getSummary()).isEqualTo(axis1D.getSummary());
    assertThat(copy.toString()).isEqualTo(axis1D.toString());
    assertThat(copy.getValues()).isEqualTo(axis1D.getValues());
  }

  @Test
  public void testRegularInterval() {
    GridAxis1DTime.Builder<?> builder = GridAxis1DTime.builder().setName("name").setUnits(unitString)
        .setDescription("desc").setRegular(7, -5.0, 65.0, 10.0).setSpacing(GridAxis.Spacing.regularInterval)
        .setDependenceType(GridAxis.DependenceType.fmrcReg).setDependsOn("it depends")
        .addAttribute(new Attribute("aname", 99.0));
    GridAxis1DTime axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(axis1D.getUnits()).isEqualTo(unitString);
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getNcoords()).isEqualTo(7);
    assertThat(axis1D.getSpacing()).isEqualTo(GridAxis.Spacing.regularInterval);
    assertThat(axis1D.getStartValue()).isEqualTo(-5);
    assertThat(axis1D.getEndValue()).isEqualTo(65);
    assertThat(axis1D.getResolution()).isEqualTo(10);
    assertThat(axis1D.isAscending()).isTrue();
    assertThat(axis1D.isRegular()).isTrue();
    assertThat(axis1D.isInterval()).isTrue();
    assertThat(axis1D.getDependenceType()).isEqualTo(GridAxis.DependenceType.fmrcReg);
    assertThat(axis1D.getDependsOn()).containsExactly("it", "depends");
    assertThat(axis1D.getCoordEdgeMinMax()).isEqualTo(MinMax.create(-5.0, 65.0));

    for (int i = 0; i < axis1D.getNcoords(); i++) {
      assertThat(axis1D.getCoordMidpoint(i)).isEqualTo(10.0 * i);
      assertThat(axis1D.getCoordEdge1(i)).isEqualTo(10.0 * i - 5.0);
      assertThat(axis1D.getCoordEdge2(i)).isEqualTo(10.0 * i + 5.0);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(10.0 * i - 5.0, 10.0 * i + 5.0));
    }

    int count = 0;
    for (double val : axis1D.getCoordsAsArray()) {
      assertThat(val).isEqualTo(count * 10.0);
      count++;
    }

    count = 0;
    for (double val : axis1D.getCoordBoundsAsArray()) {
      if (count % 2 == 0) {
        assertThat(val).isEqualTo(10.0 * (count / 2) - 5.0);
      } else {
        assertThat(val).isEqualTo(10.0 * (count / 2) + 5.0);
      }
      count++;
    }

    GridAxis1DTime copy = axis1D.toBuilder().build();
    assertThat(copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
  }

  @Test
  public void testIrregularPoint() {
    int n = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxis1DTime.Builder<?> builder = GridAxis1DTime.builder().setName("name").setUnits(unitString)
        .setDescription("desc").setNcoords(n).setValues(values).setSpacing(GridAxis.Spacing.irregularPoint)
        .setResolution(13.0).addAttribute(new Attribute("aname", 99.0));
    GridAxis1DTime axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(axis1D.getUnits()).isEqualTo(unitString);
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxis.Spacing.irregularPoint);
    assertThat(axis1D.getNcoords()).isEqualTo(n);
    assertThat(axis1D.getStartValue()).isEqualTo(0);
    assertThat(axis1D.getEndValue()).isEqualTo(0);
    assertThat(axis1D.getResolution()).isEqualTo(13.0);
    assertThat(axis1D.isAscending()).isTrue();
    assertThat(axis1D.isRegular()).isFalse();
    assertThat(axis1D.isInterval()).isFalse();
    assertThat(axis1D.getCoordEdgeMinMax()).isEqualTo(MinMax.create(-2.5, 110.0));

    for (int i = 0; i < axis1D.getNcoords(); i++) {
      assertThat(axis1D.getCoordMidpoint(i)).isEqualTo(values[i]);
      if (i == 0) {
        assertThat(axis1D.getCoordEdge1(i)).isEqualTo(values[i] - (values[i + 1] - values[i]) / 2);
      } else {
        assertThat(axis1D.getCoordEdge1(i)).isEqualTo((values[i] + values[i - 1]) / 2);
      }
      if (i == n - 1) {
        assertThat(axis1D.getCoordEdge2(i)).isEqualTo(values[i] + (values[i] - values[i - 1]) / 2);
      } else {
        assertThat(axis1D.getCoordEdge2(i)).isEqualTo((values[i] + values[i + 1]) / 2);
      }
    }

    int count = 0;
    for (double val : axis1D.getCoordsAsArray()) {
      assertThat(val).isEqualTo(values[count]);
      count++;
    }

    GridAxis1DTime copy = axis1D.toBuilder().build();
    assertThat(copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
  }

  @Test
  public void testContiguousInterval() {
    int n = 6;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxis1DTime.Builder<?> builder = GridAxis1DTime.builder().setName("name").setUnits(unitString)
        .setDescription("desc").setNcoords(n).setValues(values).setSpacing(GridAxis.Spacing.contiguousInterval)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxis1DTime axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(axis1D.getUnits()).isEqualTo(unitString);
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxis.Spacing.contiguousInterval);
    assertThat(axis1D.getNcoords()).isEqualTo(n);
    assertThat(axis1D.getStartValue()).isEqualTo(0);
    assertThat(axis1D.getEndValue()).isEqualTo(0);
    assertThat(axis1D.getResolution()).isEqualTo(0);
    assertThat(axis1D.isAscending()).isTrue();
    assertThat(axis1D.isRegular()).isFalse();
    assertThat(axis1D.isInterval()).isTrue();
    assertThat(axis1D.getCoordEdgeMinMax()).isEqualTo(MinMax.create(0, 100.0));

    for (int i = 0; i < axis1D.getNcoords(); i++) {
      assertThat(axis1D.getCoordMidpoint(i)).isEqualTo((values[i] + values[i + 1]) / 2);
      assertThat(axis1D.getCoordEdge1(i)).isEqualTo(values[i]);
      assertThat(axis1D.getCoordEdge2(i)).isEqualTo(values[i + 1]);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(values[i], values[i + 1]));
    }

    int count = 0;
    for (double val : axis1D.getCoordsAsArray()) {
      assertThat(val).isEqualTo((values[count] + values[count + 1]) / 2);
      count++;
    }

    count = 0;
    for (double val : axis1D.getCoordBoundsAsArray()) {
      if (count % 2 == 0) {
        assertThat(val).isEqualTo(values[count / 2]);
      } else {
        assertThat(val).isEqualTo(values[count / 2 + 1]);
      }
      count++;
    }

    GridAxis1DTime copy = axis1D.toBuilder().build();
    assertThat(copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
  }

  @Test
  public void testDiscontiguousInterval() {
    int n = 6;
    double[] values = new double[] {0, 3, 4, 5, 10, 15, 16, 20, 40, 80, 90, 100};
    GridAxis1DTime.Builder<?> builder = GridAxis1DTime.builder().setName("name").setUnits(unitString)
        .setDescription("desc").setNcoords(n).setValues(values).setSpacing(GridAxis.Spacing.discontiguousInterval)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxis1DTime axis1D = builder.build();

    assertThat(axis1D.getName()).isEqualTo("name");
    assertThat(axis1D.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(axis1D.getUnits()).isEqualTo(unitString);
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(axis1D.getSpacing()).isEqualTo(GridAxis.Spacing.discontiguousInterval);
    assertThat(axis1D.getNcoords()).isEqualTo(n);
    assertThat(axis1D.getStartValue()).isEqualTo(0);
    assertThat(axis1D.getEndValue()).isEqualTo(0);
    assertThat(axis1D.getResolution()).isEqualTo(0);
    assertThat(axis1D.isAscending()).isTrue();
    assertThat(axis1D.isRegular()).isFalse();
    assertThat(axis1D.isInterval()).isTrue();
    assertThat(axis1D.getCoordEdgeMinMax()).isEqualTo(MinMax.create(0, 100));

    for (int i = 0; i < axis1D.getNcoords(); i++) {
      assertThat(axis1D.getCoordMidpoint(i)).isEqualTo((values[2 * i] + values[2 * i + 1]) / 2);
      assertThat(axis1D.getCoordEdge1(i)).isEqualTo(values[2 * i]);
      assertThat(axis1D.getCoordEdge2(i)).isEqualTo(values[2 * i + 1]);
      assertThat(axis1D.getCoordInterval(i)).isEqualTo(CoordInterval.create(values[2 * i], values[2 * i + 1]));
    }

    int count = 0;
    for (double val : axis1D.getCoordsAsArray()) {
      assertThat(val).isEqualTo((values[count * 2] + values[count * 2 + 1]) / 2);
      count++;
    }

    count = 0;
    for (double val : axis1D.getCoordBoundsAsArray()) {
      assertThat(val).isEqualTo(values[count]);
      count++;
    }

    GridAxis1DTime copy = axis1D.toBuilder().build();
    assertThat(copy).isEqualTo(axis1D);
    assertThat(copy.hashCode()).isEqualTo(axis1D.hashCode());
  }

}
