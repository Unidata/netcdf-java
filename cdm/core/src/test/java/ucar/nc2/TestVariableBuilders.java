package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.Section;

public class TestVariableBuilders {

  @Test
  public void testBuilder() {
    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isTrue();
  }

  @Test
  public void testWithDims() {
    try {
      // Must set dimension first
      Variable.builder().setName("name").setDataType(DataType.FLOAT).setDimensions("dim1 dim2").build();
      fail();
    } catch (Exception e) {
      // ok
    }

    Group group = Group.builder()
        .addDimension(Dimension.builder().setName("dim1").setLength(7).setIsUnlimited(true).build())
        .addDimension(Dimension.builder().setName("dim2").setLength(27).build())
        .build();

    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT)
        .setParent(group)
        .setDimensions("dim1 dim2").build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isTrue();
    assertThat(var.getShape()).isEqualTo(new int[] {7,27});
    assertThat(var.getShapeAll()).isEqualTo(new int[] {7,27});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {7,27}));
  }

  @Test
  public void testWithAnonymousDims() {
    // No parent group needed
    int[] shape = new int[] {3,6,-1};
    Variable var =  Variable.builder().setName("name").setDataType(DataType.FLOAT).setDimensionsAnonymous(shape).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isFalse();
    assertThat(var.getShape()).isEqualTo(new int[] {3,6,-1});
    assertThat(var.getShapeAll()).isEqualTo(new int[] {3,6,-1});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {3,6,-1}));
  }

  @Test
  public void testBuilderChain() {
    Structure struct =  Structure.builder().setName("struct").addMemberVariables(ImmutableList.of()).build();
    assertThat(struct.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("struct");
    assertThat(struct.getVariableNames()).hasSize(0);
    assertThat(struct.getVariables()).hasSize(0);
  }

  @Test
  public void testToBuilderChain() {
    Structure struct =  Structure.builder().setName("struct").addMemberVariables(ImmutableList.of()).build();
    Structure struct2 = struct.toBuilder().setName("s2").build();
    assertThat(struct2.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct2.getShortName()).isEqualTo("s2");
    assertThat(struct2.getVariableNames()).hasSize(0);
    assertThat(struct2.getVariables()).hasSize(0);
  }

}
