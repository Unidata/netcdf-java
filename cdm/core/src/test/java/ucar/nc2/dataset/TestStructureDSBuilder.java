package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

public class TestStructureDSBuilder {

  @Test
  public void testBuilder() {
    Variable.Builder var = Variable.builder().setName("member").setDataType(DataType.FLOAT)
        .setGroup(makeDummyGroup());
    StructureDS struct =
        StructureDS.builder().setName("name").setDataType(DataType.FLOAT).addMemberVariable(var)
            .setGroup(makeDummyGroup()).build();
    assertThat(struct.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("name");
    assertThat(struct.isScalar()).isTrue();
    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getDataType()).isEqualTo(DataType.FLOAT);
  }

  @Test
  public void testBuilder2() {
    StructureDS var = StructureDS.builder().setName("name").setUnits("units").setDesc("desc")
        .setGroup(makeDummyGroup()).build();
    assertThat(var.getUnitsString()).isEqualTo("units");
    assertThat(var.getDescription()).isEqualTo("desc");
    assertThat(var.findAttValueIgnoreCase(CDM.UNITS, "")).isEqualTo("units");
    assertThat(var.findAttValueIgnoreCase(CDM.LONG_NAME, "")).isEqualTo("desc");
  }

  @Test
  public void testBuilderChain() {
    StructureDS struct = StructureDS.builder().setName("struct").addMemberVariables(ImmutableList.of())
        .setGroup(makeDummyGroup()).build();
    assertThat(struct.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("struct");
    assertThat(struct.getVariableNames()).hasSize(0);
    assertThat(struct.getVariables()).hasSize(0);
  }

  @Test
  public void testToBuilderChain() {
    Variable.Builder var = Variable.builder().setName("member").setDataType(DataType.FLOAT)
        .setGroup(makeDummyGroup());
    StructureDS struct = StructureDS.builder().setName("name").setUnits("units").addMemberVariable(var)
        .setGroup(makeDummyGroup()).build();

    StructureDS struct2 = struct.toBuilder().setName("s2").build();
    assertThat(struct2.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct2.getShortName()).isEqualTo("s2");
    assertThat(struct2.getUnitsString()).isEqualTo("units");

    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getDataType()).isEqualTo(DataType.FLOAT);
  }

  @Test
  public void testBuilderOrgValues() {
    Structure orgVar = Structure.builder().setName("orgName").setDataType(DataType.INT)
        .setGroup(makeDummyGroup()).build();
    StructureDS var =
        StructureDS.builder().setName("name").setOriginalName("orgName").setOriginalVariable(orgVar)
            .setGroup(makeDummyGroup()).build();
    assertThat(var.getOriginalDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(var.getOriginalName()).isEqualTo("orgName");
    assertThat((Object) var.getOriginalVariable()).isEqualTo(orgVar);
  }

}

