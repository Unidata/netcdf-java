package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import ucar.ma2.DataType;

public class TestNetcdfFileBuilder {

  @Test
  public void testBuilder() {
    Attribute att = new Attribute("attName", "value");
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder nested = Group.builder().setName("child");
    Variable.Builder vb = Variable.builder().setName("varName").setDataType(DataType.STRING);
    Group.Builder groupb =
        Group.builder().setName("name").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb);

    NetcdfFile.Builder builder =
        NetcdfFile.builder().setId("Hid").setLocation("location").setRootGroup(groupb).setTitle("title");


    NetcdfFile ncfile = builder.build();
    assertThat(ncfile.getId()).isEqualTo("Hid");
    assertThat(ncfile.getLocation()).isEqualTo("location");
    assertThat(ncfile.getTitle()).isEqualTo("title");

    Group group = ncfile.getRootGroup();
    assertThat(group.getNetcdfFile()).isEqualTo(ncfile);
    assertThat(group.getShortName()).isEqualTo("name");
    assertThat(group.isRoot()).isTrue();
    assertThat(group.attributes()).isNotEmpty();
    assertThat(group.attributes()).hasSize(1);
    assertThat(group.findAttribute("attName")).isEqualTo(att);
    assertThat(group.findAttributeString("attName", null)).isEqualTo("value");

    assertThat(group.getDimensions()).isNotEmpty();
    assertThat(group.getDimensions()).hasSize(1);
    assertThat(group.findDimension("dimName")).isEqualTo(dim);

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

}
