package ucar.nc2.internal.grid;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.MinMax;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisSpacing;
import ucar.nc2.grid.Grids;
import ucar.nc2.write.NcdumpArray;

import java.io.IOException;
import java.util.List;

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

    CoordAxisToGridAxis subject = CoordAxisToGridAxis.create(axis, GridAxisDependenceType.independent, true);
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
      assertThat(gridAxis.getCoordDouble(i)).isEqualTo(10.0 * i);
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

    CoordAxisToGridAxis subject = CoordAxisToGridAxis.create(axis, GridAxisDependenceType.independent, true);
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
      assertThat(gridAxis.getCoordDouble(i)).isEqualTo(timeOffsets[i]);
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

  @Test
  public void testStringValue() {
    int n = 11;
    String units = "days since 2020-02-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", n).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    int count = 1;
    String[] strings = new String[n];
    for (int i = 0; i < n; i++) {
      strings[i] = CalendarDate.of(2020, 2, count++, 0, 0, 0).toString();
    }

    Array<String> values = Arrays.factory(ArrayType.STRING, new int[] {n}, strings);

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setArrayType(ArrayType.STRING).setUnits(units).setDesc("desc")
            .setEnhanceMode(NetcdfDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1").setSourceData(values);
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());

    ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);
    assertThat(extract.cdates).hasSize(n);
    System.out.printf("extract.cdates = %s%n", extract.cdates);

    CoordAxisToGridAxis subject = CoordAxisToGridAxis.create(axis, GridAxisDependenceType.independent, true);
    GridAxis<?> gridAxis = subject.extractGridAxis();

    CalendarDateUnit orgUnits = CalendarDateUnit.fromUdunitString(null, units).orElseThrow();
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, gridAxis.getUnits()).orElseThrow();
    System.out.printf("orgUnits = %s%n", orgUnits);
    System.out.printf("dateUnit = %s%n", dateUnit);

    for (int i = 0; i < gridAxis.getNominalSize(); i++) {
      assertThat(dateUnit.makeFractionalCalendarDate(gridAxis.getCoordDouble(i)))
          .isEqualTo(orgUnits.makeCalendarDate(i));
    }
  }


  /*
   * https://github.com/Unidata/netcdf-java/issues/592
   * CoordinateAxis1D lonAxis = ...
   * lonAxis.toString();
   * double Lon(XDim=360);
   * :units = "degrees_east";
   * :_CoordinateAxisType = "Lon";
   * lonAxis.isRegular() returns true
   * lonAxis.getStart() returns -179.5
   * lonAxis.getIncrement() returns 1.0
   * lonAxis.getMinEdgeValue() returns invalid value -179.5 !
   * lonAxis.getCoordEdge(0) returns valid value -180.0
   * lonAxis.getMinEdgeValue() now returns valid value -180.0 !
   */

  @Test
  public void testIssue592() throws IOException {
    Group group = Group.builder().addDimension(Dimension.builder().setName("lon").setLength(360).build()).build();
    List<Dimension> varDims = group.makeDimensionsList("lon");

    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("LonCoord").setUnits("degrees_east")
        .setArrayType(ArrayType.FLOAT).addDimensions(varDims).setAutoGen(-179.5, 1);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Lon);
    CoordinateAxis axis = builder.build(group);

    assertThat(axis.getRank()).isEqualTo(1);
    assertThat(axis).isInstanceOf(CoordinateAxis1D.class);

    CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
    System.out.printf("axis1D values=%s%n", NcdumpArray.printArray(axis1D.readArray()));

    CoordAxisToGridAxis convert = CoordAxisToGridAxis.create(axis1D, GridAxisDependenceType.independent, true);
    GridAxisPoint extract = (GridAxisPoint) convert.extractGridAxis();

    System.out.printf("extracted GridAxis=%s%n", extract);

    assertThat(extract.getNominalSize()).isEqualTo(360);
    assertThat(extract.getResolution()).isEqualTo(1);
    assertThat(extract.getCoordInterval(0)).isEqualTo(CoordInterval.create(-180, -179));
    assertThat(extract.getCoordInterval(extract.getNominalSize() - 1)).isEqualTo(CoordInterval.create(179, 180));
  }

}
