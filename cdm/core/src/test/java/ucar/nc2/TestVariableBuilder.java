package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static ucar.nc2.TestUtils.makeDummyGroup;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.Section;

public class TestVariableBuilder {

  @Test
  public void testBuilder() {
    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setGroup(makeDummyGroup()).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isTrue();
  }

  @Test
  public void testWithDims() {
    try {
      // Must set dimension first
      Variable.builder().setName("name").setDataType(DataType.FLOAT).setDimensionsByName("dim1 dim2").build();
      fail();
    } catch (Exception e) {
      // ok
    }

    Group group = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27)).build();

    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setGroup(group)
        .setDimensionsByName("dim1 dim2").build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isTrue();
    assertThat(var.getShape()).isEqualTo(new int[] {7, 27});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {7, 27}));
  }

  @Test
  public void testWithAnonymousDims() {
    int[] shape = new int[] {3, 6, -1};
    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setGroup(makeDummyGroup())
        .setDimensionsAnonymous(shape).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isFalse();
    assertThat(var.getShape()).isEqualTo(new int[] {3, 6, -1});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {3, 6, -1}));
  }

  @Test
  public void testCopy() {
    Group group = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27)).build();

    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setGroup(group)
        .setDimensionsByName("dim1 dim2").build();

    Variable copy = var.toBuilder().build();

    assertThat(copy.getParentGroup()).isEqualTo(group);
    assertThat(copy.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(copy.getShortName()).isEqualTo("name");
    assertThat(copy.isScalar()).isFalse();
    assertThat(copy.isUnlimited()).isTrue();
    assertThat(copy.getShape()).isEqualTo(new int[] {7, 27});
    assertThat(copy.getShapeAsSection()).isEqualTo(new Section(new int[] {7, 27}));

    assertThat((Object) copy).isEqualTo(var);
  }

}
