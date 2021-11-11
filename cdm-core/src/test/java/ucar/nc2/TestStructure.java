/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.StructureMembers;

/** Test {@link ucar.nc2.Structure} */
public class TestStructure {

  @Test
  public void testBuilder() {
    Variable.Builder<?> var = Variable.builder().setName("member").setArrayType(ArrayType.FLOAT);
    Structure struct = Structure.builder().setName("name").addMemberVariable(var)
        .addAttribute(new Attribute("att", "value")).build(makeDummyGroup());
    assertThat(struct.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("name");
    assertThat(struct.isScalar()).isTrue();
    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getArrayType()).isEqualTo(ArrayType.FLOAT);

    assertThat(struct.getElementSize()).isEqualTo(4);
    assertThat(struct.isSubset()).isFalse();

    assertThat(struct.getNameAndAttributes()).startsWith("Structure name");
    assertThat(struct.toString())
        .startsWith(String.format("%nStructure {%n" + "  float member;%n" + "} name;%n" + ":att = \"value\";"));
  }

  @Test
  public void testBuilderChain() {
    Structure struct =
        Structure.builder().setName("struct").addMemberVariables(ImmutableList.of()).build(makeDummyGroup());
    assertThat(struct.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("struct");
    assertThat(struct.getVariableNames()).hasSize(0);
    assertThat(struct.getVariables()).hasSize(0);
  }

  @Test
  public void testToBuilderChain() {
    Variable.Builder<?> var = Variable.builder().setName("member").setArrayType(ArrayType.FLOAT);
    Structure struct = Structure.builder().setName("name").addMemberVariable(var).build(makeDummyGroup());
    Structure struct2 = struct.toBuilder().setName("s2").build(makeDummyGroup());
    assertThat(struct2.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(struct2.getShortName()).isEqualTo("s2");

    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getArrayType()).isEqualTo(ArrayType.FLOAT);

    assertThat(struct2.getElementSize()).isEqualTo(4);
    assertThat(struct2.isSubset()).isFalse();
  }

  @Test
  public void testNestedStructure() {
    Variable.Builder<?> varb1 = Variable.builder().setName("var1").setArrayType(ArrayType.FLOAT);
    Structure.Builder<?> structb = Structure.builder().setName("top").addMemberVariable(varb1);
    Variable.Builder<?> varb2 = Variable.builder().setName("var2").setArrayType(ArrayType.FLOAT);
    Structure.Builder<?> structb2 = Structure.builder().setName("nested").addMemberVariable(varb2);

    Structure struct = structb.addMemberVariable(structb2).build(makeDummyGroup());

    assertThat(struct.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("top");
    assertThat(struct.getElementSize()).isEqualTo(8);

    Variable v = struct.findVariable("nested");
    assertThat(v).isNotNull();
    assertThat(v instanceof Structure).isTrue();
    Structure nested = (Structure) v;

    Variable nestedVar = nested.findVariable("var2");
    assertThat(nestedVar).isNotNull();
    assertThat(nestedVar.getArrayType()).isEqualTo(ArrayType.FLOAT);

    assertThat(struct.getNumberOfMemberVariables()).isEqualTo(2);
    assertThat(nested.getNumberOfMemberVariables()).isEqualTo(1);
    assertThat(struct.findVariable(null)).isNull();

    StructureMembers sm = struct.makeStructureMembersBuilder().setStandardOffsets().build();
    assertThat(sm.findMember("var1")).isNotNull();
    assertThat(sm.findMember("nested")).isNotNull();

    StructureMembers smNested = nested.makeStructureMembersBuilder().setStandardOffsets().build();
    assertThat(smNested.findMember("var1")).isNull();
    assertThat(smNested.findMember("var2")).isNotNull();
  }

  @Test
  public void testSelect() {
    Variable.Builder<?> varb1 = Variable.builder().setName("var1").setArrayType(ArrayType.FLOAT);
    Variable.Builder<?> varb2 = Variable.builder().setName("var2").setArrayType(ArrayType.INT);
    Structure.Builder<?> structb =
        Structure.builder().setName("top").addMemberVariables(ImmutableList.of(varb1, varb2));
    Structure struct = structb.build(makeDummyGroup());

    assertThat(struct.getNumberOfMemberVariables()).isEqualTo(2);
    assertThat(struct.findVariable("var1")).isNotNull();
    assertThat(struct.findVariable("var2")).isNotNull();

    Structure selected = struct.select("var1");
    assertThat(selected.getNumberOfMemberVariables()).isEqualTo(1);
    assertThat(selected.findVariable("var1")).isNotNull();
    assertThat(selected.findVariable("var2")).isNull();
  }

  @Test
  public void testAddMembers() {
    Group.Builder parent = Group.builder();
    Structure.Builder<?> structb = Structure.builder().setName("struct").setParentGroupBuilder(parent)
        .addMemberVariable("one", ArrayType.BYTE, "").addMemberVariable("two", ArrayType.CHAR, "");

    Variable.Builder<?> two = Variable.builder().setName("two").setArrayType(ArrayType.FLOAT);
    assertThat(structb.replaceMemberVariable(two)).isTrue();

    assertThat(structb.removeMemberVariable("one")).isTrue();
    assertThat(structb.removeMemberVariable("nope")).isFalse();

    assertThat(structb.findMemberVariable("nope").isPresent()).isFalse();
    assertThat(structb.findMemberVariable("one").isPresent()).isFalse();
    assertThat(structb.findMemberVariable("two").isPresent()).isTrue();

    Variable.Builder<?> vb = structb.findMemberVariable("two").orElse(null);
    assertThat(vb).isNotNull();
    assertThat(vb.dataType).isEqualTo(ArrayType.FLOAT);
  }
}
