package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.unidata.geoloc.projection.FlatEarth;

import java.util.ArrayList;

/** Test {@link NetcdfDataset.Builder} */
public class TestNetcdfDatasetBuilder {

  @Test
  public void testBasics() {
    Attribute att = new Attribute("attName", "value");
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder nested = Group.builder().setName("child");
    VariableDS.Builder<?> vb = VariableDS.builder().setName("varName").setDataType(DataType.STRING);
    Group.Builder groupb =
        Group.builder().setName("").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb);
    nested.setParentGroup(groupb);

    NetcdfDataset.Builder<?> builder =
        NetcdfDataset.builder().setId("Hid").setLocation("location").setRootGroup(groupb).setTitle("title");

    NetcdfDataset ncfile = builder.build();
    assertThat(ncfile.getId()).isEqualTo("Hid");
    assertThat(ncfile.getLocation()).isEqualTo("location");
    assertThat(ncfile.getTitle()).isEqualTo("title");

    Group group = ncfile.getRootGroup();
    assertThat(group.getNetcdfFile()).isEqualTo(ncfile);
    assertThat(group.getShortName()).isEqualTo("");
    assertThat(group.isRoot()).isTrue();
    assertThat(group.attributes()).isNotEmpty();
    assertThat(group.attributes()).hasSize(1);
    assertThat(group.findAttribute("attName")).isEqualTo(att);
    assertThat(group.findAttributeString("attName", null)).isEqualTo("value");

    assertThat(group.getDimensions()).isNotEmpty();
    assertThat(group.getDimensions()).hasSize(1);
    assertThat(group.findDimension("dimName").isPresent()).isTrue();
    assertThat(group.findDimension("dimName").get()).isEqualTo(dim);

    assertThat(group.getGroups()).isNotEmpty();
    assertThat(group.getGroups()).hasSize(1);
    Group child = group.findGroupLocal("child");
    assertThat(child.getParentGroup()).isEqualTo(group);

    assertThat(group.getVariables()).isNotEmpty();
    assertThat(group.getVariables()).hasSize(1);
    Variable v = group.findVariableLocal("varName");
    assertThat(v.getParentGroup()).isEqualTo(group);
    assertThat(v.getNetcdfFile()).isEqualTo(ncfile);
  }

  @Test
  public void testCoordinatesHelper() {
    NetcdfDataset.Builder<?> ncdb = NetcdfDataset.builder();
    CoordinatesHelper.Builder coords = ncdb.coords;

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setDataType(DataType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    ncdb.rootGroup.addVariable(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setDataType(DataType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    ncdb.rootGroup.addVariable(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY));

    CoordinateSystem.Builder<?> csb = CoordinateSystem.builder().setCoordAxesNames("xname yname");
    coords.addCoordinateSystem(csb);

    NetcdfDataset ncd = ncdb.build();
    CoordinateSystem coordSys = ncd.findCoordinateSystem("yname xname");
    assertThat(coordSys).isNotNull();

    CoordinateAxis xaxis = coordSys.findAxis(AxisType.GeoX);
    assertThat(xaxis).isNotNull();
    assertThat(xaxis.getShortName()).isEqualTo("xname");
    assertThat(xaxis.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(xaxis.getUnitsString()).isEqualTo("xunits");
    assertThat(xaxis.getDescription()).isEqualTo("xdesc");
    assertThat(xaxis.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(xaxis.findAttributeString(CDM.UNITS, "")).isEqualTo("xunits");
    assertThat(xaxis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("xdesc");

    CoordinateAxis xaxis2 = ncd.findCoordinateAxis("xname");
    assertThat(xaxis2).isNotNull();
    assertThat(xaxis2).isEqualTo(xaxis);

  }

}
