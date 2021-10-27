/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static ucar.nc2.TestUtils.makeDummyGroup;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

/** Test {@link VariableDS.Builder} */
public class TestVariableDSBuilder {

  @Test
  public void testVarBuilder() {
    VariableDS var = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).build(makeDummyGroup());
    assertThat(var.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isTrue();
  }

  @Test
  public void testVarDSBuilder() {
    VariableDS var = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).setUnits("units")
        .setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll()).build(makeDummyGroup());
    assertThat(var.getUnitsString()).isEqualTo("units");
    assertThat(var.getDescription()).isEqualTo("desc");
    assertThat(var.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(var.findAttributeString(CDM.UNITS, "")).isEqualTo("units");
    assertThat(var.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");
  }

  @Test
  public void testVarDSBuilderOrgValues() {
    Variable orgVar = Variable.builder().setName("orgName").setArrayType(ArrayType.INT).build(makeDummyGroup());
    VariableDS var = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).setOriginalName("orgName")
        .setOriginalArrayType(ArrayType.INT).setOriginalVariable(orgVar).build(makeDummyGroup());
    assertThat(var.getOriginalArrayType()).isEqualTo(ArrayType.INT);
    assertThat(var.getOriginalName()).isEqualTo("orgName");
    assertThat((Object) var.getOriginalVariable()).isEqualTo(orgVar);
  }

  @Test
  public void testWithDims() {
    assertThrows(NullPointerException.class, () -> {
      VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).setDimensionsByName("dim1 dim2")
          .build(makeDummyGroup());
    });

    Group group =
        Group.builder().addDimension(Dimension.builder().setName("dim1").setLength(7).setIsUnlimited(true).build())
            .addDimension(Dimension.builder().setName("dim2").setLength(27).build()).build();
    List<Dimension> varDims = group.makeDimensionsList("dim1 dim2");

    VariableDS var =
        VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).addDimensions(varDims).build(group);
    assertThat(var.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isTrue();
    assertThat(var.getShape()).isEqualTo(new int[] {7, 27});
    assertThat(Dimensions.makeShapeAll(var)).isEqualTo(new int[] {7, 27});
    assertThat(var.getSection()).isEqualTo(new Section(new int[] {7, 27}));
  }

  @Test
  public void testWithAnonymousDims() {
    // No parent group needed
    int[] shape = new int[] {3, 6, -1};
    VariableDS var = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).setDimensionsAnonymous(shape)
        .build(makeDummyGroup());
    assertThat(var.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isFalse();
    assertThat(var.getShape()).isEqualTo(new int[] {3, 6, -1});
    assertThat(Dimensions.makeShapeAll(var)).isEqualTo(new int[] {3, 6, -1});
    assertThat(var.getSection()).isEqualTo(new Section(new int[] {3, 6, -1}));
  }

  @Test
  public void testCopyFrom() {
    Group.Builder gb =
        Group.builder().addDimension(Dimension.builder().setName("dim1").setLength(7).setIsUnlimited(true).build())
            .addDimension(Dimension.builder().setName("dim2").setLength(27).build());
    Variable.Builder<?> vb = Variable.builder().setName("name").setArrayType(ArrayType.FLOAT).setParentGroupBuilder(gb)
        .setDimensionsByName("dim1 dim2").addAttribute(new Attribute("units", "flower"));
    vb.getAttributeContainer().addAttribute(new Attribute("attName", "AttValue"));
    Group group = gb.build();
    Variable v = vb.build(group);

    VariableDS.Builder<?> builder = VariableDS.builder().copyFrom(v);
    assertThat(builder.getUnits()).isEqualTo("flower");

    VariableDS varDS = builder.build(group);
    assertThat(varDS.getShortName()).isEqualTo("name");
    assertThat(varDS.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(varDS.findAttributeString("attname", null)).isEqualTo("AttValue");
    assertThat(varDS.getUnitsString()).isEqualTo("flower");
  }

  @Test
  public void testMissingData() throws IOException {
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    VariableDS vds =
        VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).setUnits("units").setDesc("desc")
            .setEnhanceMode(NetcdfDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1").build(parent.build());

    Array<Float> data = (Array<Float>) vds.readArray();
    for (Float val : data) {
      assertThat(val).isNaN();
    }
  }

  @Test
  public void testFromVar() {
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    Variable v = Variable.builder().setName("name").setArrayType(ArrayType.FLOAT)
        .addAttribute(new Attribute("units", " Mg/fortnight ")).addAttribute(new Attribute("long_name", "desc"))
        .addAttribute(new Attribute("missing_value", 0.0f)).setParentGroupBuilder(parent).setDimensionsByName("dim1")
        .build(parent.build());

    VariableDS vds = VariableDS.fromVar(v.getParentGroup(), v, true);
    assertThat(vds.getUnitsString()).isEqualTo("Mg/fortnight");
    assertThat(vds.convertNeeded()).isTrue();
    assertThat(vds.getEnhanceMode()).isEqualTo(NetcdfDataset.getDefaultEnhanceMode());
    assertThat(vds.getDatasetLocation()).isEqualTo("N/A");

    VariableDS vds2 = VariableDS.fromVar(v.getParentGroup(), v, false);
    assertThat(vds2.getUnitsString()).isEqualTo("Mg/fortnight");
    assertThat(vds2.convertNeeded()).isFalse();
    assertThat(vds2.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceNone());

    VariableDS varOrg = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT).setUnits("units")
        .setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll()).build(makeDummyGroup());
    VariableDS vds3 = VariableDS.fromVar(v.getParentGroup(), varOrg, false);
    assertThat(vds3.convertNeeded()).isFalse();
    assertThat(vds3.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
  }

}

