package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import ucar.array.ArrayType;

/** Test {@link ucar.nc2.Group} */
public class TestGroup {

  @Test
  public void testBuilder() {
    Attribute att = new Attribute("attName", "value");
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder nested = Group.builder().setName("child");
    Variable.Builder<?> vb = Variable.builder().setName("varName").setArrayType(ArrayType.STRING);
    Group group =
        Group.builder().setName("name").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb).build();

    assertThat(group.getShortName()).isEqualTo("name");
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
    assertThat(child).isNotNull();
    assertThat(child.getParentGroup()).isEqualTo(group);

    assertThat(group.getVariables()).isNotEmpty();
    assertThat(group.getVariables()).hasSize(1);
    Variable v = group.findVariableLocal("varName");
    assertThat(v).isNotNull();
    assertThat(v.getParentGroup()).isEqualTo(group);
  }

  @Test
  public void testReplaceDimension() {
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder builder = Group.builder().setName("name");

    assertThat(builder.replaceDimension(dim)).isFalse();
    assertThat(builder.findDimension("dimName")).isEqualTo(Optional.of(dim));

    Dimension dim2 = new Dimension("dimName", 99);
    assertThat(builder.replaceDimension(dim2)).isTrue();
    assertThat(builder.findDimension("dimName")).isEqualTo(Optional.of(dim2));
  }

  @Test
  public void testDuplicateDimension() {
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder builder = Group.builder().setName("name").addDimension(dim);

    try {
      builder.addDimension(dim);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Dimension 'dimName' already exists");
    }
  }

  @Test
  public void testRemoveGroup() {
    Group.Builder child = Group.builder().setName("child");
    Group.Builder child2 = Group.builder().setName("child2");
    Group.Builder builder = Group.builder().setName("name").addGroup(child).addGroup(child2);

    assertThat(builder.gbuilders).hasSize(2);
    assertThat(builder.removeGroup("child")).isTrue();
    assertThat(builder.gbuilders).hasSize(1);
    assertThat(builder.findGroupLocal("child").isPresent()).isFalse();
    assertThat(builder.findGroupLocal("child2").isPresent()).isTrue();
  }

  @Test
  public void testSetDimensionsByNameNeedsParentGroup() {
    try {
      Variable.builder().setName("varName").setDimensionsByName("dim");
      fail();
    } catch (Exception e) {
      assertThat(e instanceof NullPointerException);
    }
  }

  @Test
  public void testDimNotExist() {
    Group.Builder gb = Group.builder().setName("name");
    try {
      Variable.builder().setName("varName").setParentGroupBuilder(gb).setDimensionsByName("dim");
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Dimension dim does not exist");
    }
  }

  @Test
  public void testReplaceVariable() {
    Variable.Builder<?> vb = Variable.builder().setName("varName");
    Group.Builder gb = Group.builder().setName("name").addDimension(new Dimension("dim", 42));

    assertThat(gb.replaceVariable(vb)).isFalse();
    assertThat(gb.findVariableLocal("varName")).isEqualTo(Optional.of(vb));

    Variable.Builder<?> vb2 =
        Variable.builder().setName("varName").setParentGroupBuilder(gb).setDimensionsByName("dim");
    assertThat(gb.replaceVariable(vb2)).isTrue();
    assertThat(gb.findVariableLocal("varName")).isEqualTo(Optional.of(vb2));
  }

  @Test
  public void testDuplicateVariable() {
    Variable.Builder<?> vb = Variable.builder().setName("varName");
    Group.Builder builder = Group.builder().setName("name").addVariable(vb);

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
    Group.Builder builder = Group.builder().setName("name").addAttribute(att1).addAttribute(att2);

    AttributeContainer atts = builder.getAttributeContainer();
    assertThat(Iterables.isEmpty(atts)).isFalse();
    assertThat(Iterables.size(atts)).isEqualTo(2);
    assertThat(atts.findAttribute("attName")).isEqualTo(att1);
    assertThat(atts.findAttributeString("attName", null)).isEqualTo("value");
    assertThat(atts.findAttributeString("attName2", null)).isEqualTo("value2");
  }

  @Test
  public void testGroupParents() {
    Group.Builder parentg = Group.builder().setName("parent");
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg);
    Group.Builder uncleb = Group.builder().setName("uncle");
    Group root = Group.builder().addGroup(grampsb).addGroup(uncleb).build();
    Group parent = root.findGroupNested("parent").orElse(null);
    assertThat(parent).isNotNull();
    Group gramps = root.findGroupNested("gramps").orElse(null);
    assertThat(gramps).isNotNull();
    Group uncle = root.findGroupNested("uncle").orElse(null);
    assertThat(uncle).isNotNull();

    assertThat(parent.commonParent(gramps)).isEqualTo(gramps);
    assertThat(gramps.commonParent(parent)).isEqualTo(gramps);
    assertThat(root.commonParent(parent)).isEqualTo(root);
    assertThat(uncle.commonParent(parent)).isEqualTo(root);
    assertThat(parent.commonParent(uncle)).isEqualTo(root);
    assertThat(uncle.commonParent(gramps)).isEqualTo(root);
    assertThat(gramps.commonParent(uncle)).isEqualTo(root);

    assertThat(root.isParent(parent)).isTrue();
    assertThat(root.isParent(uncle)).isTrue();
    assertThat(root.isParent(gramps)).isTrue();

    assertThat(gramps.isParent(parent)).isTrue();
    assertThat(parent.isParent(gramps)).isFalse();
    assertThat(uncle.isParent(gramps)).isFalse();
    assertThat(gramps.isParent(uncle)).isFalse();
    assertThat(uncle.isParent(parent)).isFalse();
    assertThat(parent.isParent(uncle)).isFalse();
  }

  @Test
  public void testGroupParentMistake() {
    Group.Builder parentg = Group.builder().setName("parent");
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg);
    Group.Builder uncleb = Group.builder().setName("uncle").addGroup(parentg); // ooops added twice
    try {
      Group.builder().addGroup(grampsb).addGroup(uncleb).build();
      fail();
    } catch (Exception e) {
      // expected
    }
  }


  @Test
  public void testFindDimension() {
    Dimension low = new Dimension("low", 1);
    Dimension mid = new Dimension("mid", 1);
    Dimension high = new Dimension("high", 1);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(low);
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg).addDimension(mid);
    Group.Builder uncleb = Group.builder().setName("uncle");
    Group root = Group.builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build();
    Group parent = root.findGroupNested("parent").orElse(null);
    assertThat(parent).isNotNull();
    Group gramps = root.findGroupNested("gramps").orElse(null);
    assertThat(gramps).isNotNull();
    Group uncle = root.findGroupNested("uncle").orElse(null);
    assertThat(uncle).isNotNull();

    assertThat(parent.findDimension("low").isPresent()).isTrue();
    assertThat(parent.findDimension("mid").isPresent()).isTrue();
    assertThat(parent.findDimension("high").isPresent()).isTrue();

    assertThat(gramps.findDimension("low").isPresent()).isFalse();
    assertThat(gramps.findDimension("mid").isPresent()).isTrue();
    assertThat(gramps.findDimension("high").isPresent()).isTrue();

    assertThat(root.findDimension("low").isPresent()).isFalse();
    assertThat(root.findDimension("mid").isPresent()).isFalse();
    assertThat(root.findDimension("high").isPresent()).isTrue();

    assertThat(uncle.findDimension("low").isPresent()).isFalse();
    assertThat(uncle.findDimension("mid").isPresent()).isFalse();
    assertThat(uncle.findDimension("high").isPresent()).isTrue();

    assertThat(parent.findDimension(low) != null).isTrue();
    assertThat(parent.findDimension(mid) != null).isTrue();
    assertThat(parent.findDimension(high) != null).isTrue();

    assertThat(gramps.findDimension(low) != null).isFalse();
    assertThat(gramps.findDimension(mid) != null).isTrue();
    assertThat(gramps.findDimension(high) != null).isTrue();

    assertThat(root.findDimension(low) != null).isFalse();
    assertThat(root.findDimension(mid) != null).isFalse();
    assertThat(root.findDimension(high) != null).isTrue();

    assertThat(uncle.findDimension(low) != null).isFalse();
    assertThat(uncle.findDimension(mid) != null).isFalse();
    assertThat(uncle.findDimension(high) != null).isTrue();

    assertThat(uncle.findDimension((Dimension) null) == null).isTrue();
    assertThat(uncle.findDimension((String) null).isPresent()).isFalse();
    assertThat(parent.findDimensionLocal(null) != null).isFalse();
  }

  @Test
  public void testFindEnum() {
    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef1 = new EnumTypedef("low", map);
    EnumTypedef typedef2 = new EnumTypedef("high", map);

    Dimension low = new Dimension("low", 1);
    Dimension mid = new Dimension("mid", 1);
    Dimension high = new Dimension("high", 1);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(low).addEnumTypedef(typedef1);
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg).addDimension(mid);
    Group.Builder uncleb = Group.builder().setName("uncle");
    Group root = Group.builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).addEnumTypedef(typedef2).build();
    Group parent = root.findGroupNested("parent").orElse(null);
    assertThat(parent).isNotNull();
    Group gramps = root.findGroupNested("gramps").orElse(null);
    assertThat(gramps).isNotNull();
    Group uncle = root.findGroupNested("uncle").orElse(null);
    assertThat(uncle).isNotNull();

    assertThat(parent.findEnumeration("high") != null).isTrue();
    assertThat(gramps.findEnumeration("high") != null).isTrue();
    assertThat(uncle.findEnumeration("high") != null).isTrue();
    assertThat(root.findEnumeration("high") != null).isTrue();

    assertThat(parent.findEnumeration("low") != null).isTrue();
    assertThat(gramps.findEnumeration("low") != null).isFalse();
    assertThat(uncle.findEnumeration("low") != null).isFalse();
    assertThat(root.findEnumeration("low") != null).isFalse();

    assertThat(root.findEnumeration(null) != null).isFalse();
  }

  @Test
  public void testFindVariable() {
    Dimension low = new Dimension("low", 1);
    Dimension mid = new Dimension("mid", 1);
    Dimension high = new Dimension("high", 1);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(low);
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg).addDimension(mid);
    Group.Builder uncleb = Group.builder().setName("uncle");

    Variable.Builder<?> vb = Variable.builder().setName("v").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parentg).setDimensionsByName("low");
    Variable.Builder<?> vattb = Variable.builder().setName("vatt").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parentg).setDimensionsByName("mid").addAttribute(new Attribute("findme", "findmevalue"));
    parentg.addVariable(vb);
    grampsb.addVariable(vattb);

    Group root = Group.builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build();
    Group parent = root.findGroupNested("parent").orElse(null);
    assertThat(parent).isNotNull();
    Group gramps = root.findGroupNested("gramps").orElse(null);
    assertThat(gramps).isNotNull();
    Group uncle = root.findGroupNested("uncle").orElse(null);
    assertThat(uncle).isNotNull();

    assertThat(parent.findVariableByAttribute("findme", "findmevalue") != null).isFalse();
    assertThat(uncle.findVariableByAttribute("findme", "findmevalue") != null).isFalse();
    assertThat(gramps.findVariableByAttribute("findme", "findmevalue") != null).isTrue();
    assertThat(root.findVariableByAttribute("findme", "findmevalue") != null).isTrue();

    assertThat(parent.findVariableLocal("v") != null).isTrue();
    assertThat(parent.findVariableLocal("findme") != null).isFalse();
    assertThat(gramps.findVariableLocal("v") != null).isFalse();
    assertThat(gramps.findVariableLocal("vatt") != null).isTrue();
    assertThat(gramps.findVariableLocal(null) != null).isFalse();

    assertThat(parent.findVariableOrInParent("v") != null).isTrue();
    assertThat(parent.findVariableOrInParent("vatt") != null).isTrue();
    assertThat(uncle.findVariableOrInParent("v") != null).isFalse();
    assertThat(root.findVariableOrInParent("vatt") != null).isFalse();
    assertThat(root.findVariableOrInParent(null) != null).isFalse();
  }

  @Test
  public void testGetters() {
    Dimension low = new Dimension("low", 1);
    Dimension mid = new Dimension("mid", 1);
    Dimension high = new Dimension("high", 1);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(low);
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg).addDimension(mid);
    Group.Builder uncleb = Group.builder().setName("uncle");

    Variable.Builder<?> vb = Variable.builder().setName("v").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parentg).setDimensionsByName("low");
    Variable.Builder<?> vattb = Variable.builder().setName("vatt").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parentg).setDimensionsByName("mid").addAttribute(new Attribute("findme", "findmevalue"));
    parentg.addVariable(vb);
    grampsb.addVariable(vattb);

    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef1 = new EnumTypedef("low", map);
    grampsb.addEnumTypedef(typedef1);

    Group root = Group.builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build();
    Group parent = root.findGroupNested("parent").orElse(null);
    assertThat(parent).isNotNull();
    Group gramps = root.findGroupNested("gramps").orElse(null);
    assertThat(gramps).isNotNull();
    Group uncle = root.findGroupNested("uncle").orElse(null);
    assertThat(uncle).isNotNull();

    assertThat(gramps.getDimensions()).isNotEmpty();
    assertThat(uncle.getDimensions()).isEmpty();

    assertThat(gramps.getEnumTypedefs()).isNotEmpty();
    assertThat(root.getEnumTypedefs()).isEmpty();

    assertThat(uncle.getGroups()).isEmpty();
    assertThat(root.getGroups()).isNotEmpty();

    assertThat(gramps.getVariables()).isNotEmpty();
    assertThat(uncle.getVariables()).isEmpty();
    assertThat(root.getVariables()).isEmpty();

    assertThat(parent.getParentGroup()).isEqualTo(gramps);
    assertThat(uncle.getParentGroup()).isEqualTo(root);
    assertThat(gramps.getParentGroup()).isEqualTo(root);
    assertThat(root.getParentGroup()).isNull();

    assertThat(gramps.isRoot()).isFalse();
    assertThat(root.isRoot()).isTrue();
  }

  @Test
  public void testWriteCDL() {
    Dimension low = new Dimension("low", 1);
    Dimension mid = new Dimension("mid", 1);
    Dimension high = new Dimension("high", 1);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(low);
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg).addDimension(mid);
    Group.Builder uncleb = Group.builder().setName("uncle");

    Variable.Builder<?> vb = Variable.builder().setName("v").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parentg).setDimensionsByName("low");
    Variable.Builder<?> vattb = Variable.builder().setName("vatt").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parentg).setDimensionsByName("mid").addAttribute(new Attribute("findme", "findmevalue"));
    parentg.addVariable(vb).addAttribute(new Attribute("groupAtt", "groupVal"));
    grampsb.addVariable(vattb);

    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef1 = new EnumTypedef("low", map);
    parentg.addEnumTypedef(typedef1);

    Group root = Group.builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build();
    Group parent = root.findGroupNested("parent").orElse(null);
    assertThat(parent).isNotNull();
    Group gramps = root.findGroupNested("gramps").orElse(null);
    assertThat(gramps).isNotNull();
    Group uncle = root.findGroupNested("uncle").orElse(null);
    assertThat(uncle).isNotNull();

    assertThat(gramps.toString()).startsWith(String.format("dimensions:%n" + "  mid = 1;%n" + "variables:%n"
        + "  string vatt(mid=1);%n" + "    :findme = \"findmevalue\"; // string%n" + "%n" + "group: parent {%n"
        + "  types:%n" + "    enum low { 'name1' = 1, 'name2' = 2, 'name3' = 3};%n" + "%n" + "  dimensions:%n"
        + "    low = 1;%n" + "  variables:%n" + "    string v(low=1);%n" + "%n" + "  // group attributes:%n"
        + "  :groupAtt = \"groupVal\";%n" + "}"));
    assertThat(uncle.toString()).isEqualTo(""); // really ?
  }

  @Test
  public void testEquals() {
    Group good = Group.builder().setName("good").build();
    Group good2 = Group.builder().setName("good").build();
    Group bad = Group.builder().setName("bad").build();

    assertThat(good).isEqualTo(good2);
    assertThat(good).isNotEqualTo(bad);
    assertThat(good.hashCode()).isEqualTo(good2.hashCode());
    assertThat(good.hashCode()).isNotEqualTo(bad.hashCode());
  }

  @Test
  public void testEqualsWithParent() {
    Group.Builder goodb = Group.builder().setName("good");
    Group parent = Group.builder().setName("parent").addGroup(goodb).build();
    Group.Builder goodb2 = Group.builder().setName("good");
    Group parent2 = Group.builder().setName("parent").addGroup(goodb2).build();

    Group good = parent.findGroupNested("good").orElse(null);
    assertThat(good).isNotNull();
    Group good2 = parent2.findGroupNested("good").orElse(null);
    assertThat(good2).isNotNull();

    Group bad = Group.builder().setName("good").build();

    assertThat(good).isEqualTo(good2);
    assertThat(good).isNotEqualTo(bad);
    assertThat(good.hashCode()).isEqualTo(good2.hashCode());
    assertThat(good.hashCode()).isNotEqualTo(bad.hashCode());
  }

  @Test
  public void testToBuilder() {
    Group.Builder goodb = Group.builder().setName("good");
    Group parent = Group.builder().setName("parent").addGroup(goodb).build();
    Group good = parent.findGroupNested("good").orElse(null);
    assertThat(good).isNotNull();
    Group good2 = good.toBuilder().build(parent);

    assertThat(good).isEqualTo(good2);
    assertThat(good.hashCode()).isEqualTo(good2.hashCode());
  }

  @Test
  public void testAddAndRemoveDimension() {
    Dimension dim = new Dimension("dim1", 42);
    Group.Builder gb = Group.builder().setName("name");
    assertThat(gb.addDimensionIfNotExists(dim)).isTrue();
    assertThat(gb.addDimensionIfNotExists(dim)).isFalse();

    assertThat(gb.findDimension("dim1").isPresent()).isTrue();
    assertThat(gb.getDimensions()).isNotEmpty();

    assertThat(gb.removeDimension("dim1")).isTrue();
    assertThat(gb.findDimension("dim1").isPresent()).isFalse();
    assertThat(gb.getDimensions()).isEmpty();

    assertThat(gb.addDimensionIfNotExists(dim)).isTrue();
    assertThat(gb.findDimension("dim1").isPresent()).isTrue();
  }

  @Test
  public void testNestedGroupBuilders() {
    Variable.Builder<?> vroot = Variable.builder().setName("vroot").setArrayType(ArrayType.STRING);
    Variable.Builder<?> vleaf = Variable.builder().setName("vleaf").setArrayType(ArrayType.STRING);
    Variable.Builder<?> voff = Variable.builder().setName("voff").setArrayType(ArrayType.STRING);

    Group.Builder parent = Group.builder().setName("parent").addVariable(vleaf);
    Group.Builder gramps = Group.builder().setName("gramps").addGroup(parent);
    Group.Builder uncle = Group.builder().setName("uncle").addVariable(voff);
    Group.Builder root = Group.builder().addGroup(gramps).addGroup(uncle).addVariable(vroot);

    Group.Builder found = root.findGroupNested("/gramps/parent").orElse(null);
    assertThat(found).isNotNull();
    assertThat(found).isEqualTo(parent);

    found = root.findGroupNested("/uncle").orElse(null);
    assertThat(found).isNotNull();
    assertThat(found).isEqualTo(uncle);

    found = root.findGroupNested("/").orElse(null);
    assertThat(found).isNotNull();
    assertThat(found).isEqualTo(root);

    // TODO The leading "/" is optional.
    assertThat(root.findGroupNested("gramps").isPresent()).isTrue();
    assertThat(root.findGroupNested("gramps/parent").isPresent()).isTrue();

    assertThat(root.findGroupNested("/random").isPresent()).isFalse();
    assertThat(root.findGroupNested("random").isPresent()).isFalse();
    assertThat(root.findGroupNested("parent").isPresent()).isFalse();
    assertThat(root.findGroupNested(null).isPresent()).isTrue();
    assertThat(root.findGroupNested("").isPresent()).isTrue();
    assertThat(gramps.findGroupNested("").isPresent()).isFalse();

    assertThat(root.findVariableNested("gramps/parent/vleaf").isPresent()).isTrue();
    assertThat(root.findVariableNested("/gramps/parent/vleaf").isPresent()).isTrue();
    assertThat(root.findVariableNested("vroot").isPresent()).isTrue();
    assertThat(root.findVariableNested("/vroot").isPresent()).isTrue();
    assertThat(root.findVariableNested("/uncle/voff").isPresent()).isTrue();
    assertThat(root.findVariableNested("uncle/voff").isPresent()).isTrue();

    assertThat(root.findVariableNested("/random").isPresent()).isFalse();
    assertThat(root.findVariableNested("/gramps/voff").isPresent()).isFalse();
    assertThat(root.findVariableNested("/gramps/parent/voff.m").isPresent()).isFalse();

    assertThat(gramps.findVariableNested("parent/vleaf").isPresent()).isTrue();
    assertThat(uncle.findVariableNested("voff").isPresent()).isTrue();
    assertThat(uncle.findVariableNested("vleaf").isPresent()).isFalse();
    assertThat(uncle.findVariableNested("vroot").isPresent()).isFalse();
  }

  @Test
  public void testCommonParentBuilders() {
    Group.Builder parent = Group.builder().setName("parent");
    Group.Builder gramps = Group.builder().setName("gramps").addGroup(parent);
    Group.Builder uncle = Group.builder().setName("uncle");
    Group.Builder root = Group.builder().addGroup(gramps).addGroup(uncle);

    assertThat(parent.commonParent(gramps)).isEqualTo(gramps);
    assertThat(gramps.commonParent(parent)).isEqualTo(gramps);
    assertThat(root.commonParent(parent)).isEqualTo(root);
    assertThat(uncle.commonParent(parent)).isEqualTo(root);
    assertThat(parent.commonParent(uncle)).isEqualTo(root);
    assertThat(uncle.commonParent(gramps)).isEqualTo(root);
    assertThat(gramps.commonParent(uncle)).isEqualTo(root);

    assertThat(root.isParent(parent)).isTrue();
    assertThat(root.isParent(uncle)).isTrue();
    assertThat(root.isParent(gramps)).isTrue();

    assertThat(gramps.isParent(parent)).isTrue();
    assertThat(parent.isParent(gramps)).isFalse();
    assertThat(uncle.isParent(gramps)).isFalse();
    assertThat(gramps.isParent(uncle)).isFalse();
    assertThat(uncle.isParent(parent)).isFalse();
    assertThat(parent.isParent(uncle)).isFalse();

    // TODO leading, trailing "/" ??
    assertThat(parent.makeFullName()).isEqualTo("gramps/parent/");
    assertThat(gramps.makeFullName()).isEqualTo("gramps/");
    assertThat(root.makeFullName()).isEqualTo("");
    assertThat(uncle.makeFullName()).isEqualTo("uncle/");
  }

  @Test
  public void testFindEnumBuilders() {
    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef1 = new EnumTypedef("low", map);
    EnumTypedef typedef2 = new EnumTypedef("high", map);

    Group.Builder parent = Group.builder().setName("parent").addEnumTypedef(typedef1);
    Group.Builder gramps = Group.builder().setName("gramps").addGroup(parent);
    Group.Builder uncle = Group.builder().setName("uncle");
    Group.Builder root = Group.builder().addGroups(ImmutableList.of(gramps, uncle)).addEnumTypedef(typedef2);

    assertThat(parent.findEnumeration("high").isPresent()).isFalse();
    assertThat(gramps.findEnumeration("high").isPresent()).isFalse();
    assertThat(uncle.findEnumeration("high").isPresent()).isFalse();
    assertThat(root.findEnumeration("high").isPresent()).isTrue();

    assertThat(parent.findEnumeration("low").isPresent()).isTrue();
    assertThat(gramps.findEnumeration("low").isPresent()).isFalse();
    assertThat(uncle.findEnumeration("low").isPresent()).isFalse();
    assertThat(root.findEnumeration("low").isPresent()).isFalse();

    assertThat(root.findEnumeration(null).isPresent()).isFalse();

    assertThat(root.findOrAddEnumTypedef("high", null)).isEqualTo(typedef2);
    EnumTypedef another = new EnumTypedef("another", map);
    assertThat(root.findOrAddEnumTypedef("another", map)).isEqualTo(another);
  }

  @Test
  public void testFindVariableBuilders() {
    Dimension low = new Dimension("low", 1);
    Dimension mid = new Dimension("mid", 1);
    Dimension high = new Dimension("high", 1);
    Group.Builder parent = Group.builder().setName("parent").addDimensions(ImmutableList.of(low, high));
    Group.Builder gramps = Group.builder().setName("gramps").addGroup(parent).addDimension(mid);
    Group.Builder uncle = Group.builder().setName("uncle");

    Variable.Builder<?> vb = Variable.builder().setName("v").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parent).setDimensionsByName("low");
    Variable.Builder<?> vattb = Variable.builder().setName("vatt").setArrayType(ArrayType.STRING)
        .setParentGroupBuilder(parent).setDimensionsByName("mid").addAttribute(new Attribute("findme", "findmevalue"));
    parent.addVariables(ImmutableList.of(vb, vattb));
    parent.replaceVariable(vb);

    Group.Builder root = Group.builder().addGroup(gramps).addGroup(uncle);

    assertThat(parent.findVariableLocal("v").isPresent()).isTrue();
    assertThat(parent.findVariableOrInParent("v").isPresent()).isTrue();
    assertThat(root.findVariableLocal("v").isPresent()).isFalse();

    Variable.Builder<?> vhigh =
        Variable.builder().setName("vhigh").setArrayType(ArrayType.STRING).setParentGroupBuilder(gramps);
    gramps.replaceVariable(vhigh);
    assertThat(parent.findVariableOrInParent("vhigh").isPresent()).isTrue();

    assertThat(gramps.removeVariable("vhigh")).isTrue();
    assertThat(parent.findVariableOrInParent("vhigh").isPresent()).isFalse();
    assertThat(gramps.removeVariable("vhigh")).isFalse();
  }

}
