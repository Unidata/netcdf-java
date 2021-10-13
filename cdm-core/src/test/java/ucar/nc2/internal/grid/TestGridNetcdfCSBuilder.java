package ucar.nc2.internal.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.dataset.transform.horiz.ProjectionCTV;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.FlatEarth;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

/** Test {@link GridNetcdfCSBuilder} */
public class TestGridNetcdfCSBuilder {

  @Test
  public void testBasics() {
    // NetcdfDataset
    NetcdfDataset ncd = NetcdfDataset.builder().build();
    ArrayList<CoordinateAxis> axes = new ArrayList<>();

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setArrayType(ArrayType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX).build(makeDummyGroup()));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setArrayType(ArrayType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY).build(makeDummyGroup()));

    ProjectionCTV projct = new ProjectionCTV("horiz", new FlatEarth());
    List<ProjectionCTV> allProjs = ImmutableList.of(projct);

    CoordinateSystem.Builder<?> csb =
        CoordinateSystem.builder().setCoordAxesNames("xname yname").setCoordinateTransformName("horiz");
    CoordinateSystem coordSys = csb.build(ncd, axes, allProjs);

    // GridDataset
    GridNetcdfCSBuilder builder = new GridNetcdfCSBuilder();
    builder.setName(coordSys.getName());
    builder.setProjection(coordSys.getProjection());

    for (CoordinateAxis axis : coordSys.getCoordinateAxes()) {
      CoordAxisToGridAxis subject = CoordAxisToGridAxis.create(axis, GridAxisDependenceType.independent, true);
      GridAxis<?> gridAxis = subject.extractGridAxis();
      builder.addAxis(gridAxis);
    }

    GridCoordinateSystem subject = builder.build();
    assertThat(subject.getName()).isEqualTo(coordSys.getName());
    assertThat(subject.getHorizCoordinateSystem().getProjection()).isEqualTo(coordSys.getProjection());

    GridAxis<?> gridAxisX = subject.findAxis("xname").orElseThrow();
    assertThat(gridAxisX.getName()).isEqualTo("xname");
    GridAxisPoint xaxis = subject.getXHorizAxis();
    assertThat(xaxis.getName()).isEqualTo("xname");

    GridAxis<?> gridAxisY = subject.findAxis("yname").orElseThrow();
    assertThat(gridAxisY.getName()).isEqualTo("yname");
    GridAxisPoint yaxis = subject.getYHorizAxis();
    assertThat(yaxis.getName()).isEqualTo("yname");

    assertThat((Object) subject.findCoordAxisByType(AxisType.Ensemble)).isNull();
    assertThat((Object) subject.getEnsembleAxis()).isNull();
    assertThat((Object) subject.getVerticalAxis()).isNull();
    assertThat(subject.getTimeCoordinateSystem()).isNull();

    assertThat(subject.getNominalShape()).isEqualTo(ImmutableList.of(1, 1));
  }
}
