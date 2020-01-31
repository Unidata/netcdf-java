package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.ma2.DataType;

public class TestStructureBuilder {

  @Test
  public void testBuilder() {
    Variable.Builder var = Variable.builder().setName("member").setDataType(DataType.FLOAT).setGroup(makeDummyGroup());
    Structure struct = Structure.builder().setName("name").setDataType(DataType.FLOAT).setGroup(makeDummyGroup())
        .addMemberVariable(var).build();
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
    Structure struct =
        Structure.builder().setName("struct").addMemberVariables(ImmutableList.of()).setGroup(makeDummyGroup()).build();
    assertThat(struct.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("struct");
    assertThat(struct.getVariableNames()).hasSize(0);
    assertThat(struct.getVariables()).hasSize(0);
  }

  @Test
  public void testToBuilderChain() {
    Variable.Builder var = Variable.builder().setName("member").setGroup(makeDummyGroup()).setDataType(DataType.FLOAT);
    Structure struct = Structure.builder().setName("name").setDataType(DataType.FLOAT).setGroup(makeDummyGroup())
        .addMemberVariable(var).build();
    Structure struct2 = struct.toBuilder().setName("s2").build();
    assertThat(struct2.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct2.getShortName()).isEqualTo("s2");

    assertThat(struct.getVariableNames()).hasSize(1);
    assertThat(struct.getVariableNames().get(0)).isEqualTo("member");
    assertThat(struct.getVariables()).hasSize(1);
    assertThat(struct.getVariables().get(0).getShortName()).isEqualTo("member");
    assertThat(struct.getVariables().get(0).getDataType()).isEqualTo(DataType.FLOAT);
  }

}
