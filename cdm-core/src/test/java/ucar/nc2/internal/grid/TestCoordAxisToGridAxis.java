package ucar.nc2.internal.grid;

import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.MinMax;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisSpacing;
import ucar.nc2.grid.Grids;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CoordAxisToGridAxis} */
public class TestCoordAxisToGridAxis {

  @Test
  public void testFromVariableDS() {
    Group.Builder parentb = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).setUnits("unit").setAutoGen(0.0, 10.0)
            .addAttribute(new Attribute("aname", 99.0)).setParentGroupBuilder(parentb).setDimensionsByName("dim1");
    parentb.addVariable(vdsBuilder);
    Group parent = parentb.build();
    VariableDS vds = (VariableDS) parent.findVariableLocal("name");
    assertThat(vds).isNotNull();

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Ensemble);
    CoordinateAxis axis = builder.build(parent);

    CoordAxisToGridAxis subject = new CoordAxisToGridAxis(axis, GridAxisDependenceType.independent, true);
    GridAxis<?> gridAxis = subject.extractGridAxis();

    assertThat(gridAxis.getName()).isEqualTo("name");
    assertThat(gridAxis.getAxisType()).isEqualTo(AxisType.Ensemble);
    assertThat(gridAxis.getUnits()).isEqualTo("unit");
    assertThat(gridAxis.getDescription()).isEqualTo("");
    assertThat(gridAxis.attributes().findAttributeString(CDM.UNITS, "")).isEqualTo("unit");
    assertThat(gridAxis.attributes().findAttributeString(CDM.LONG_NAME, "")).isEqualTo("");
    assertThat(gridAxis.attributes().findAttributeDouble("aname", 0.0)).isEqualTo(99.0);

    assertThat(gridAxis.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(gridAxis.getNominalSize()).isEqualTo(7);
    assertThat(Grids.isAscending(gridAxis)).isTrue();
    assertThat(Grids.getCoordEdgeMinMax(gridAxis)).isEqualTo(MinMax.create(-5.0, 65));

    for (int i = 0; i < gridAxis.getNominalSize(); i++) {
      assertThat(gridAxis.getCoordMidpoint(i)).isEqualTo(10.0 * i);
      CoordInterval intv = gridAxis.getCoordInterval(i);
      assertThat(intv.start()).isEqualTo(10.0 * i - 5.0);
      assertThat(intv.end()).isEqualTo(10.0 * i + 5.0);
      assertThat(gridAxis.getCoordInterval(i)).isEqualTo(CoordInterval.create(10.0 * i - 5.0, 10.0 * i + 5.0));
    }

    int count = 0;
    for (Object val : gridAxis) {
      assertThat(val).isEqualTo(count * 10.0);
      count++;
    }

    assertThat((Object) gridAxis).isInstanceOf(GridAxisPoint.class);
    GridAxisPoint gridAxisPoint = (GridAxisPoint) gridAxis;

    GridAxis<?> copy = gridAxisPoint.toBuilder().build();
    assertThat((Object) copy).isEqualTo(gridAxisPoint);
    assertThat(copy.hashCode()).isEqualTo(gridAxisPoint.hashCode());
    assertThat(copy.toString()).isEqualTo(gridAxisPoint.toString());
  }

  @Test
  public void testIrregularCoordinate() {
    String unitString = "days since 2020-11-01 0:00 GMT";
    int[] timeOffsets = new int[] {0, 1, 2, 3, 5, 8, 13, 21};
    int n = timeOffsets.length;
    Group.Builder parentb = Group.builder().addDimension(Dimension.builder("dim1", n).build());

    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT)
        .setUnits(unitString).setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll())
        .addAttribute(new Attribute(CF.CALENDAR, "noleap")).setParentGroupBuilder(parentb).setDimensionsByName("dim1");
    vdsBuilder.setSourceData(Arrays.factory(ArrayType.INT, new int[] {n}, timeOffsets));
    parentb.addVariable(vdsBuilder);
    Group parent = parentb.build();
    VariableDS vds = (VariableDS) parent.findVariableLocal("name");
    assertThat(vds).isNotNull();

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent);

    CoordAxisToGridAxis subject = new CoordAxisToGridAxis(axis, GridAxisDependenceType.independent, true);
    GridAxis<?> gridAxis = subject.extractGridAxis();

    assertThat(gridAxis.getName()).isEqualTo("name");
    assertThat(gridAxis.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(gridAxis.getUnits()).isEqualTo(unitString);
    assertThat(gridAxis.getDescription()).isEqualTo("desc");
    assertThat(gridAxis.attributes().findAttributeString(CDM.UNITS, "")).isEqualTo(unitString);
    assertThat(gridAxis.attributes().findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");
    assertThat(gridAxis.attributes().findAttributeString(CF.CALENDAR, "")).isEqualTo("noleap");

    assertThat(gridAxis.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
    assertThat(gridAxis.getNominalSize()).isEqualTo(n);
    assertThat(Grids.isAscending(gridAxis)).isTrue();
    assertThat(Grids.getCoordEdgeMinMax(gridAxis)).isEqualTo(MinMax.create(-0.5, 25));

    for (int i = 0; i < gridAxis.getNominalSize(); i++) {
      assertThat(gridAxis.getCoordMidpoint(i)).isEqualTo(timeOffsets[i]);
    }

    int count = 0;
    for (Object val : gridAxis) {
      assertThat(val).isEqualTo(timeOffsets[count]);
      count++;
    }

    assertThat((Object) gridAxis).isInstanceOf(GridAxisPoint.class);
    GridAxisPoint gridAxisPoint = (GridAxisPoint) gridAxis;

    GridAxis<?> copy = gridAxisPoint.toBuilder().build();
    assertThat((Object) copy).isEqualTo(gridAxisPoint);
    assertThat(copy.hashCode()).isEqualTo(gridAxisPoint.hashCode());
    assertThat(copy.toString()).isEqualTo(gridAxisPoint.toString());
  }
}
