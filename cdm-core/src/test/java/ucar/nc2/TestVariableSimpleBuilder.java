/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.StructureMembers;

/** Test {@link ucar.nc2.VariableSimpleBuilder} */
public class TestVariableSimpleBuilder {

  @Test
  public void testVariableSimpleBuilder() {
    Dimension dim1 = new Dimension("dim1", 1);
    Dimension dim2 = new Dimension("dim2", 2);
    VariableSimpleBuilder builder =
        new VariableSimpleBuilder("name", "desc", "units", ArrayType.INT, ImmutableList.of(dim1, dim2));

    builder.addAttribute(new Attribute("name", 1.23));
    builder.addAttribute("name2", "value2");

    VariableSimpleIF vs = builder.build();
    assertThat(vs.getShortName()).isEqualTo("name");
    assertThat(vs.getFullName()).isEqualTo("name");
    assertThat(vs.getDescription()).isEqualTo("desc");
    assertThat(vs.getUnitsString()).isEqualTo("units");
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.INT);
    assertThat(vs.getDimensions()).hasSize(2);
    assertThat(vs.getDimensions()).isInstanceOf(ImmutableList.class);
    assertThat(vs.getShape()).isEqualTo(new int[] {1, 2});
    assertThat(vs.getRank()).isEqualTo(2);

    assertThat(vs.attributes().findAttributeString("name2", null)).isEqualTo("value2");
    assertThat(vs.attributes().findAttributeDouble("name", 0.0)).isEqualTo(1.23);
  }

  @Test
  public void testMakeScalar() {
    VariableSimpleBuilder builder = VariableSimpleBuilder.makeScalar("name", "desc", "units", ArrayType.CHAR)
        .addAttribute(new Attribute("name", 2.34));

    VariableSimpleIF vs = builder.build();
    assertThat(vs.getShortName()).isEqualTo("name");
    assertThat(vs.getFullName()).isEqualTo("name");
    assertThat(vs.getDescription()).isEqualTo("desc");
    assertThat(vs.getUnitsString()).isEqualTo("units");
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.CHAR);
    assertThat(vs.getDimensions()).hasSize(0);
    assertThat(vs.getShape()).isEqualTo(new int[] {});
    assertThat(vs.getRank()).isEqualTo(0);

    assertThat(vs.attributes().findAttributeDouble("name", 0.0)).isEqualTo(2.34);
  }

  @Test
  public void testMakeString() {
    VariableSimpleBuilder builder =
        VariableSimpleBuilder.makeString("name", "desc", "units", 11).addAttribute(new Attribute("name", 3.45));

    VariableSimpleIF vs = builder.build();
    assertThat(vs.getShortName()).isEqualTo("name");
    assertThat(vs.getFullName()).isEqualTo("name");
    assertThat(vs.getDescription()).isEqualTo("desc");
    assertThat(vs.getUnitsString()).isEqualTo("units");
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.CHAR);
    assertThat(vs.getDimensions()).hasSize(1);
    assertThat(vs.getShape()).isEqualTo(new int[] {11});
    assertThat(vs.getRank()).isEqualTo(1);

    assertThat(vs.attributes().findAttributeDouble("name", 0.0)).isEqualTo(3.45);
  }

  @Test
  public void testMakeMember() {
    StructureMembers.MemberBuilder mb = StructureMembers.memberBuilder().setName("name").setDesc("desc")
        .setUnits("units").setArrayType(ArrayType.INT).setShape(new int[] {22, 6}).setOffset(0);
    VariableSimpleBuilder builder = VariableSimpleBuilder.fromMember(mb.build(0, false));

    VariableSimpleIF vs = builder.build();
    assertThat(vs.getShortName()).isEqualTo("name");
    assertThat(vs.getFullName()).isEqualTo("name");
    assertThat(vs.getDescription()).isEqualTo("desc");
    assertThat(vs.getUnitsString()).isEqualTo("units");
    assertThat(vs.getArrayType()).isEqualTo(ArrayType.INT);
    assertThat(vs.getDimensions()).hasSize(2);
    assertThat(vs.getShape()).isEqualTo(new int[] {22, 6});
    assertThat(vs.getRank()).isEqualTo(2);
  }

}
