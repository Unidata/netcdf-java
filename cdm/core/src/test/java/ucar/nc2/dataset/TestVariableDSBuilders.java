/* Copyright Unidata */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

/** Test VariableDS builders */
public class TestVariableDSBuilders {

  @Test
  public void testVarBuilder() {
    VariableDS var = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isTrue();
  }

  @Test
  public void testVarDSBuilder() {
    VariableDS var = VariableDS.builder().setName("name").setDataType(DataType.FLOAT)
      .setUnits("units").setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll()).build();
    assertThat(var.getUnitsString()).isEqualTo("units");
    assertThat(var.getDescription()).isEqualTo("desc");
    assertThat(var.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(var.findAttValueIgnoreCase(CDM.UNITS, "")).isEqualTo("units");
    assertThat(var.findAttValueIgnoreCase(CDM.LONG_NAME, "")).isEqualTo("desc");
  }

  @Test
  public void testVarDSBuilderOrgValues() {
    Variable orgVar = Variable.builder().setName("orgName").setDataType(DataType.INT).build();
    VariableDS var = VariableDS.builder().setName("name").setDataType(DataType.FLOAT)
      .setOriginalName("orgName").setOriginalDataType(DataType.INT).setOriginalVariable(orgVar).build();
    assertThat(var.getOriginalDataType()).isEqualTo(DataType.INT);
    assertThat(var.getOriginalName()).isEqualTo("orgName");
    assertThat(var.getOriginalVariable()).isEqualTo(orgVar);
  }

  @Test
  public void testWithDims() {
    try {
      // Must set dimension first
      VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setDimensionsByName("dim1 dim2").build();
      fail();
    } catch (Exception e) {
      // ok
    }

    Group group = Group.builder()
        .addDimension(Dimension.builder().setName("dim1").setLength(7).setIsUnlimited(true).build())
        .addDimension(Dimension.builder().setName("dim2").setLength(27).build())
        .build(null);
    List<Dimension> varDims = group.makeDimensionsList("dim1 dim2");

    VariableDS var = VariableDS.builder().setName("name").setDataType(DataType.FLOAT)
        .setGroup(group)
        .addDimensions(varDims).build();
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
    VariableDS var =  VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setDimensionsAnonymous(shape).build();
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
    StructureDS struct =  StructureDS.builder().setName("struct").addMemberVariables(ImmutableList.of()).build();
    assertThat(struct.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct.getShortName()).isEqualTo("struct");
    assertThat(struct.getVariableNames()).hasSize(0);
    assertThat(struct.getVariables()).hasSize(0);
  }

  @Test
  public void testToBuilderChain() {
    StructureDS struct =  StructureDS.builder().setName("struct").addMemberVariables(ImmutableList.of()).build();
    StructureDS struct2 = struct.toBuilder().setName("s2").build();
    assertThat(struct2.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(struct2.getShortName()).isEqualTo("s2");
    assertThat(struct2.getVariableNames()).hasSize(0);
    assertThat(struct2.getVariables()).hasSize(0);
  }

  @Test
  public void testStructBuilder() {
    StructureDS var = StructureDS.builder().setName("name").setDataType(DataType.FLOAT).build();
    assertThat(var.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isTrue();
  }

  @Test
  public void testStructDSBuilder() {
    StructureDS var = StructureDS.builder().setName("name")
        .setUnits("units").setDesc("desc").build();
    assertThat(var.getUnitsString()).isEqualTo("units");
    assertThat(var.getDescription()).isEqualTo("desc");
    assertThat(var.findAttValueIgnoreCase(CDM.UNITS, "")).isEqualTo("units");
    assertThat(var.findAttValueIgnoreCase(CDM.LONG_NAME, "")).isEqualTo("desc");
  }

  @Test
  public void testStructDSBuilderOrgValues() {
    Structure orgVar = Structure.builder().setName("orgName").setDataType(DataType.INT).build();
    StructureDS var = StructureDS.builder().setName("name")
        .setOriginalName("orgName").setOriginalVariable(orgVar).build();
    assertThat(var.getOriginalDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(var.getOriginalName()).isEqualTo("orgName");
    assertThat(var.getOriginalVariable()).isEqualTo(orgVar);
  }



}

