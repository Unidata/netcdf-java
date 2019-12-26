package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import java.util.Optional;
import org.junit.Test;
import ucar.ma2.DataType;

public class TestGroupBuilder {

  @Test
  public void testBuilder() {
    Attribute att = new Attribute("attName", "value");
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder nested = Group.builder(null).setName("child");
    Variable.Builder vb = Variable.builder().setName("varName").setDataType(DataType.STRING);
    Group group = Group.builder(null).setName("name").addAttribute(att).addDimension(dim).addGroup(nested)
        .addVariable(vb).build(null);

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
  }

  @Test
  public void testReplaceDimension() {
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder builder = Group.builder(null).setName("name");

    assertThat(builder.replaceDimension(dim)).isFalse();
    assertThat(builder.findDimension("dimName")).isEqualTo(Optional.of(dim));

    Dimension dim2 = new Dimension("dimName", 99);
    assertThat(builder.replaceDimension(dim2)).isTrue();
    assertThat(builder.findDimension("dimName")).isEqualTo(Optional.of(dim2));
  }

  @Test
  public void testDuplicateDimension() {
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder builder = Group.builder(null).setName("name").addDimension(dim);

    try {
      builder.addDimension(dim);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Dimension 'dimName' already exists");
    }
  }

  @Test
  public void testRemoveGroup() {
    Group.Builder child = Group.builder(null).setName("child");
    Group.Builder child2 = Group.builder(null).setName("child2");
    Group.Builder builder = Group.builder(null).setName("name").addGroup(child).addGroup(child2);

    assertThat(builder.gbuilders).hasSize(2);
    assertThat(builder.removeGroup("child")).isTrue();
    assertThat(builder.gbuilders).hasSize(1);
    assertThat(builder.findGroup("child").isPresent()).isFalse();
    assertThat(builder.findGroup("child2").isPresent()).isTrue();
  }

  @Test
  public void testReplaceVariable() {
    Variable.Builder vb = Variable.builder().setName("varName");
    Group.Builder builder = Group.builder(null).setName("name");

    assertThat(builder.replaceVariable(vb)).isFalse();
    assertThat(builder.findVariable("varName")).isEqualTo(Optional.of(vb));

    Variable.Builder vb2 = Variable.builder().setName("varName").setDimensionsByName("dim");
    assertThat(builder.replaceVariable(vb2)).isTrue();
    assertThat(builder.findVariable("varName")).isEqualTo(Optional.of(vb2));
  }

  @Test
  public void testDuplicateVariable() {
    Variable.Builder vb = Variable.builder().setName("varName");
    Group.Builder builder = Group.builder(null).setName("name").addVariable(vb);

    try {
      builder.addVariable(vb);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Variable 'varName' already exists");
    }
  }

  @Test
  public void testAttributes() {
    Attribute att1 = new Attribute("attName", "value");
    Attribute att2 = new Attribute("attName2", "value2");
    Group.Builder builder = Group.builder(null).setName("name").addAttribute(att1).addAttribute(att2);

    AttributeContainer atts = builder.getAttributeContainer();
    assertThat(atts.getAttributes()).isNotEmpty();
    assertThat(atts.getAttributes()).hasSize(2);
    assertThat(atts.findAttribute("attName")).isEqualTo(att1);
    assertThat(atts.findAttValueIgnoreCase("attName", null)).isEqualTo("value");
    assertThat(atts.findAttValueIgnoreCase("attName2", null)).isEqualTo("value2");
  }

}
