/* Copyright Unidata */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static ucar.nc2.TestUtils.makeDummyGroup;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

/** Test VariableDS builders */
public class TestVariableDSBuilder {

  @Test
  public void testVarBuilder() {
    VariableDS var =
        VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setGroup(makeDummyGroup()).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isTrue();
  }

  @Test
  public void testVarDSBuilder() {
    VariableDS var = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setUnits("units").setDesc("desc")
        .setGroup(makeDummyGroup()).setEnhanceMode(NetcdfDataset.getEnhanceAll()).build();
    assertThat(var.getUnitsString()).isEqualTo("units");
    assertThat(var.getDescription()).isEqualTo("desc");
    assertThat(var.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(var.findAttValueIgnoreCase(CDM.UNITS, "")).isEqualTo("units");
    assertThat(var.findAttValueIgnoreCase(CDM.LONG_NAME, "")).isEqualTo("desc");
  }

  @Test
  public void testVarDSBuilderOrgValues() {
    Variable orgVar =
        Variable.builder().setName("orgName").setDataType(DataType.INT).setGroup(makeDummyGroup()).build();
    VariableDS var = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setOriginalName("orgName")
        .setOriginalDataType(DataType.INT).setOriginalVariable(orgVar).setGroup(makeDummyGroup()).build();
    assertThat(var.getOriginalDataType()).isEqualTo(DataType.INT);
    assertThat(var.getOriginalName()).isEqualTo("orgName");
    assertThat((Object) var.getOriginalVariable()).isEqualTo(orgVar);
  }

  @Test
  public void testWithDims() {
    try {
      // Must set dimension first
      VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setGroup(makeDummyGroup())
          .setDimensionsByName("dim1 dim2").build();
      fail();
    } catch (Exception e) {
      // ok
    }

    Group group =
        Group.builder().addDimension(Dimension.builder().setName("dim1").setLength(7).setIsUnlimited(true).build())
            .addDimension(Dimension.builder().setName("dim2").setLength(27).build()).build();
    List<Dimension> varDims = group.makeDimensionsList("dim1 dim2");

    VariableDS var =
        VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setGroup(group).addDimensions(varDims).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isTrue();
    assertThat(var.getShape()).isEqualTo(new int[] {7, 27});
    assertThat(var.getShapeAll()).isEqualTo(new int[] {7, 27});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {7, 27}));
  }

  @Test
  public void testWithAnonymousDims() {
    // No parent group needed
    int[] shape = new int[] {3, 6, -1};
    VariableDS var = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setGroup(makeDummyGroup())
        .setDimensionsAnonymous(shape).build();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isFalse();
    assertThat(var.getShape()).isEqualTo(new int[] {3, 6, -1});
    assertThat(var.getShapeAll()).isEqualTo(new int[] {3, 6, -1});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {3, 6, -1}));
  }

  @Test
  public void testCopyFrom() {
    Group group =
        Group.builder().addDimension(Dimension.builder().setName("dim1").setLength(7).setIsUnlimited(true).build())
            .addDimension(Dimension.builder().setName("dim2").setLength(27).build()).build();
    Variable.Builder vb = Variable.builder().setName("name").setDataType(DataType.FLOAT).setGroup(group)
        .setDimensionsByName("dim1 dim2").addAttribute(new Attribute("units", "flower"));
    vb.getAttributeContainer().addAttribute(new Attribute("attName", "AttValue"));
    Variable v = vb.build();

    VariableDS.Builder builder = VariableDS.builder().copyFrom(v);
    assertThat(builder.getUnits()).isEqualTo("flower");

    VariableDS varDS = builder.build();
    assertThat(varDS.getShortName()).isEqualTo("name");
    assertThat(varDS.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(varDS.findAttValueIgnoreCase("attname", null)).isEqualTo("AttValue");
    assertThat(varDS.getUnitsString()).isEqualTo("flower");
  }

  @Test
  public void testMissingData() throws IOException {
    Group parent = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27)).build();

    VariableDS vds = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setUnits("units").setDesc("desc")
        .setEnhanceMode(NetcdfDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
        .setDimensionsByName("dim1").setGroup(parent).build();

    Array data = vds.read();
    System.out.printf("data = %s%n", data);
    IndexIterator iter = data.getIndexIterator();
    while (iter.hasNext()) {
      assertThat(iter.getFloatNext()).isEqualTo(Float.NaN);
    }
  }

}

