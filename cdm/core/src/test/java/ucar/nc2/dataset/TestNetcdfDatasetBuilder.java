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
    Group.Builder nested = Group.builder().setName("child");
    Variable.Builder<?> variableBuilder = Variable.builder().setName("varName").setDataType(DataType.STRING);
    VariableDS.Builder<?> variableDSBuilder = VariableDS.builder().setName("varDSName").setDataType(DataType.STRING);
    Group.Builder groupb = Group.builder().setName("name").addAttribute(att).addDimension(dim).addGroup(nested)
        .addVariable(variableBuilder).addVariable(variableDSBuilder);
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
    assertThat(group.getVariables()).hasSize(2);

    Variable variable = group.findVariableLocal("varName");
    // TODO is this correct behavior that a NetcdfDataset is allowed to have a Variable that is not a VariableDS?
    assertThat((Object) variable).isNotInstanceOf(VariableDS.class);
    assertThat(variable.getParentGroup()).isEqualTo(group);
    assertThat(variable.getNetcdfFile()).isEqualTo(ncfile);

    Variable variableDS = group.findVariableLocal("varDSName");
    assertThat((Object) variableDS).isInstanceOf(VariableDS.class);
    assertThat(variableDS.getParentGroup()).isEqualTo(group);
    assertThat(variableDS.getNetcdfFile()).isEqualTo(ncfile);
  }

}
