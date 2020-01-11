package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;

public class TestNetcdfDatasetBuilder {

  @Test
  public void testBuilder() {
    Attribute att = new Attribute("attName", "value");
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder nested = Group.builder(null).setName("child");
    VariableDS.Builder<?> vb = VariableDS.builder().setName("varName").setDataType(DataType.STRING);
    Group.Builder groupb =
        Group.builder(null).setName("name").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb);
    nested.setParentGroup(groupb);

    NetcdfDataset.Builder builder =
        NetcdfDataset.builder().setId("Hid").setLocation("location").setRootGroup(groupb).setTitle("title");

    NetcdfDataset ncfile = builder.build();
    assertThat(ncfile.getId()).isEqualTo("Hid");
    assertThat(ncfile.getLocation()).isEqualTo("location");
    assertThat(ncfile.getTitle()).isEqualTo("title");

    Group group = ncfile.getRootGroup();
    assertThat(group.getNetcdfFile()).isEqualTo(ncfile);
    assertThat(group.getShortName()).isEqualTo("name");
    assertThat(group.isRoot()).isTrue();
    assertThat(group.getAttributes()).isNotEmpty();
    assertThat(group.getAttributes()).hasSize(1);
    assertThat(group.findAttribute("attName")).isEqualTo(att);
    assertThat(group.findAttValueIgnoreCase("attName", null)).isEqualTo("value");

    assertThat(group.getDimensions()).isNotEmpty();
    assertThat(group.getDimensions()).hasSize(1);
    assertThat(group.findDimension("dimName")).isEqualTo(dim);

    assertThat(group.getGroups()).isNotEmpty();
    assertThat(group.getGroups()).hasSize(1);
    Group child = group.findGroup("child");
    assertThat(child.getParentGroup()).isEqualTo(group);

    assertThat(group.getVariables()).isNotEmpty();
    assertThat(group.getVariables()).hasSize(1);
    Variable v = group.findVariable("varName");
    assertThat(v.getParentGroup()).isEqualTo(group);
    assertThat(v.getNetcdfFile()).isEqualTo(ncfile);
  }

}
