package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

public class TestStructurePseudoDSBuilder {

  @Test
  public void testBuilder() {
    Variable.Builder var = Variable.builder().setName("member").setDataType(DataType.FLOAT);
    StructurePseudoDS struct =
        StructurePseudoDS.builder().setName("name").setDataType(DataType.FLOAT).addMemberVariable(var).build();
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
  public void testBuilderChain() {
    StructurePseudoDS struct =
        StructurePseudoDS.builder().setName("struct").addMemberVariables(ImmutableList.of()).build();
    assertThat(struct.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("struct");
    assertThat(struct.getVariableNames()).hasSize(0);
    assertThat(struct.getVariables()).hasSize(0);
  }

  @Test
  public void testToBuilderChain() {
    Variable.Builder var = Variable.builder().setName("member").setDataType(DataType.FLOAT);
    StructurePseudoDS struct =
        StructurePseudoDS.builder().setName("name").setUnits("units").addMemberVariable(var).build();

    StructurePseudoDS struct2 = struct.toBuilder().setName("s2").build();
    assertThat(struct2.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct2.getShortName()).isEqualTo("s2");
    assertThat(struct2.getUnitsString()).isEqualTo("units");

    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getDataType()).isEqualTo(DataType.FLOAT);
  }
}
