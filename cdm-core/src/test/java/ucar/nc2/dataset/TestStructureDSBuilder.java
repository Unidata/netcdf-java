/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

/** Test {@link StructureDS.Builder} */
public class TestStructureDSBuilder {

  @Test
  public void testBuilder() {
    Variable.Builder var = Variable.builder().setName("member").setArrayType(ArrayType.FLOAT);
    StructureDS struct = StructureDS.builder().setName("name").setArrayType(ArrayType.FLOAT).addMemberVariable(var)
        .build(makeDummyGroup());
    assertThat(struct.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("name");
    assertThat(struct.isScalar()).isTrue();
    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getArrayType()).isEqualTo(ArrayType.FLOAT);
  }

  @Test
  public void testBuilder2() {
    StructureDS var = StructureDS.builder().setName("name").setUnits("units").setDesc("desc").build(makeDummyGroup());
    assertThat(var.getUnitsString()).isEqualTo("units");
    assertThat(var.getDescription()).isEqualTo("desc");
    assertThat(var.findAttributeString(CDM.UNITS, "")).isEqualTo("units");
    assertThat(var.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");
  }

  @Test
  public void testBuilderChain() {
    StructureDS struct =
        StructureDS.builder().setName("struct").addMemberVariables(ImmutableList.of()).build(makeDummyGroup());
    assertThat(struct.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("struct");
    assertThat(struct.getVariableNames()).hasSize(0);
    assertThat(struct.getVariables()).hasSize(0);
  }

  @Test
  public void testToBuilderChain() {
    Variable.Builder var = Variable.builder().setName("member").setArrayType(ArrayType.FLOAT);
    StructureDS struct =
        StructureDS.builder().setName("name").setUnits("units").addMemberVariable(var).build(makeDummyGroup());

    StructureDS struct2 = struct.toBuilder().setName("s2").build(makeDummyGroup());
    assertThat(struct2.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(struct2.getShortName()).isEqualTo("s2");
    assertThat(struct2.getUnitsString()).isEqualTo("units");

    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getArrayType()).isEqualTo(ArrayType.FLOAT);
  }

  @Test
  public void testBuilderOrgValues() {
    Structure orgVar = Structure.builder().setName("orgName").setArrayType(ArrayType.INT).build(makeDummyGroup());
    StructureDS var = StructureDS.builder().setName("name").setOriginalName("orgName").setOriginalVariable(orgVar)
        .build(makeDummyGroup());
    assertThat(var.getOriginalArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(var.getOriginalName()).isEqualTo("orgName");
    assertThat((Object) var.getOriginalVariable()).isEqualTo(orgVar);
  }

}

