package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.ArrayType;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.unidata.geoloc.projection.FlatEarth;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static ucar.nc2.TestUtils.makeDummyGroup;

/** Test {@link CoordinateSystem.Builder} */
public class TestCoordSystemBuilder {

  @Test
  public void testBasics() {
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

    CoordinateSystem.Builder<?> builder = CoordinateSystem.builder().setCoordAxesNames("xname yname")
        .addCoordinateTransformByName("horiz").addCoordinateTransformByName("vert");
    CoordinateSystem coordSys = builder.build(ncd, axes, transforms);

    CoordinateAxis xaxis = coordSys.findAxis(AxisType.GeoX);
    assertThat(xaxis).isNotNull();
    assertThat(xaxis.getShortName()).isEqualTo("xname");
    assertThat(xaxis.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(xaxis.getUnitsString()).isEqualTo("xunits");
    assertThat(xaxis.getDescription()).isEqualTo("xdesc");
    assertThat(xaxis.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(xaxis.findAttributeString(CDM.UNITS, "")).isEqualTo("xunits");
    assertThat(xaxis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("xdesc");

    assertThat(coordSys.getProjectionCT()).isEqualTo(projct);
    assertThat(coordSys.getProjection()).isEqualTo(projct.getProjection());

    assertThat(coordSys.isImplicit()).isFalse();
    assertThat(coordSys.isGeoReferencing()).isTrue();
    assertThat(coordSys.isGeoXY()).isTrue();
    assertThat(coordSys.isLatLon()).isFalse();
    assertThat(coordSys.isRadial()).isFalse();
    assertThat(coordSys.isRegular()).isTrue();
    assertThat(coordSys.isProductSet()).isFalse();

    CoordinateSystem copy = coordSys.toBuilder().build(ncd, axes, transforms);
    assertThat(copy.findAxis(AxisType.GeoX)).isEqualTo(coordSys.findAxis(AxisType.GeoX));
    assertThat(copy.findAxis(AxisType.GeoY)).isEqualTo(coordSys.findAxis(AxisType.GeoY));
    assertThat(copy).isEqualTo(coordSys);
    assertThat(copy.hashCode()).isEqualTo(coordSys.hashCode());
  }

  @Test
  public void testFindMethods() {
    NetcdfDataset ncd = NetcdfDataset.builder().build();
    ArrayList<CoordinateAxis> axes = new ArrayList<>();
    ArrayList<CoordinateTransform> transforms = new ArrayList<>();

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setArrayType(ArrayType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX).build(makeDummyGroup()));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setArrayType(ArrayType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY).build(makeDummyGroup()));

    CoordinateSystem.Builder<?> builder = CoordinateSystem.builder().setCoordAxesNames("xname yname")
        .addCoordinateTransformByName("horiz").addCoordinateTransformByName("vert");
    CoordinateSystem coordSys = builder.build(ncd, axes, transforms);

    CoordinateAxis xaxis = coordSys.findAxis(AxisType.GeoX);
    assertThat(xaxis).isNotNull();
    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.GeoX, AxisType.GeoY)).isEqualTo(xaxis);

    CoordinateAxis yaxis = coordSys.findAxis(AxisType.GeoY);
    assertThat(yaxis).isNotNull();
    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.GeoY, AxisType.GeoX)).isEqualTo(yaxis);
    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.Pressure, AxisType.Height)).isNull();

    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.Pressure, AxisType.Height)).isNull();

    assertThat(coordSys.getProjection()).isNull();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testSeparateGroups() throws IOException {
    // This has Best and TwoD, and the coordSys are mixing them up
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/noaaport/NDFD-CONUS_noaaport.ncx4";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      for (Variable v : ds.getVariables()) {
        System.out.printf(" Check variable %s%n", v.getFullName());
        VariableDS vds = (VariableDS) v;
        Group parent = v.getParentGroup();
        for (CoordinateSystem csys : vds.getCoordinateSystems()) {
          System.out.printf("  Check csys %s%n", csys.getName());
          assertThat(csys.isCoordinateSystemFor(v));
          for (Dimension dim : csys.getDomain()) { // TODO another possibility is to remove extra dimension from domain
            if (dim.isShared()) {
              assertWithMessage(dim.toString()).that(parent.findDimension(dim) == dim).isTrue();
            }
          }
          for (CoordinateAxis axis : csys.getCoordinateAxes()) {
            System.out.printf("   Check axis %s%n", axis.getFullName());
            for (Dimension dim : axis.getDimensions()) {
              if (dim.isShared()) {
                assertWithMessage(dim.toString()).that(parent.findDimension(dim) == dim).isTrue();
              }
            }
          }
        }
      }
    }
  }
}
