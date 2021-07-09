package ucar.nc2.internal.grid2;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisDependenceType;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.unidata.geoloc.projection.FlatEarth;

import java.util.ArrayList;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

/** Test {@link GridNetcdfCS} */
public class TestGridNetcdfCS {

  @Test
  public void testBasics() {
    // NetcdfDataset
    NetcdfDataset ncd = NetcdfDataset.builder().build();
    ArrayList<CoordinateAxis> axes = new ArrayList<>();
    ArrayList<CoordinateTransform> transforms = new ArrayList<>();

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setArrayType(ArrayType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX).build(makeDummyGroup()));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setArrayType(ArrayType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY).build(makeDummyGroup()));

    ProjectionCT projct = new ProjectionCT("horiz", "auth", new FlatEarth());
    transforms.add(projct);

    CoordinateSystem.Builder<?> csb = CoordinateSystem.builder().setCoordAxesNames("xname yname")
        .addCoordinateTransformByName("horiz").addCoordinateTransformByName("vert");
    CoordinateSystem coordSys = csb.build(ncd, axes, transforms);

    // GridDataset
    GridNetcdfCS.Builder<?> builder = GridNetcdfCS.builder();
    builder.setName(coordSys.getName());
    builder.setProjection(coordSys.getProjection());

    for (CoordinateAxis axis : coordSys.getCoordinateAxes()) {
      CoordAxisToGridAxis subject = new CoordAxisToGridAxis(axis, GridAxisDependenceType.independent, true);
      GridAxis<?> gridAxis = subject.extractGridAxis();
      builder.addAxis(gridAxis);
    }

    GridNetcdfCS subject = builder.build();
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

    assertThat((Object) subject.findCoordAxis(AxisType.Ensemble)).isNull();
    assertThat((Object) subject.getEnsembleAxis()).isNull();
    assertThat((Object) subject.getVerticalAxis()).isNull();
    assertThat(subject.getTimeCoordinateSystem()).isNull();

    assertThat(subject.getNominalShape()).isEqualTo(ImmutableList.of(1, 1));

    GridNetcdfCS copy = subject.toBuilder().build();
    assertThat(copy).isEqualTo(subject);
    assertThat(copy.hashCode()).isEqualTo(subject.hashCode());

    assertThat(copy.toString()).contains("(yname xname)");
    assertThat(copy.showFnSummary()).isEqualTo("GRID(): Y,X");
    Formatter info = new Formatter();
    copy.show(info, true);
    assertThat(info.toString()).contains("yname (GridAxisPoint) NaN, yunits");
  }
}
