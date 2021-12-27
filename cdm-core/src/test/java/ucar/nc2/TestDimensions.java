/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.Section;

/** Test {@link ucar.nc2.Dimensions} */
public class TestDimensions {

  @Test
  public void testMakeDimensionString() {
    Dimension dim = new Dimension("dimname", 5);
    Dimension anon = Dimension.builder().setLength(5).setIsShared(false).build(); // Anonymous dimension.
    assertThat(Dimensions.makeDimensionsString(ImmutableList.of(dim, anon))).isEqualTo("dimname 5");

    assertThat(Dimensions.makeDimensionsString(null)).isEqualTo("");
  }

  @Test
  public void testMakeDimensionsList() {
    Dimension dim = new Dimension("dimname", 5);
    Group.Builder groupb = Group.builder().addDimension(dim);

    List<Dimension> list = Dimensions.makeDimensionsList(groupb::findDimension, "dimname 5");
    assertThat(list).hasSize(2);
    assertThat(list.get(0)).isEqualTo(dim);
    assertThat(list.get(1).isShared()).isFalse();
    assertThat(list.get(1).getLength()).isEqualTo(5);

    assertThat(Dimensions.makeDimensionsList(groupb::findDimension, null)).isEmpty();
    assertThat(Dimensions.makeDimensionsList(groupb::findDimension, "")).isEmpty();
  }

  @Test
  public void testSize() {
    Dimension dim1 = new Dimension("dim1", 5);
    Dimension dim2 = new Dimension("dim2", 6);
    Dimension dim3 = new Dimension("dim3", 7);
    assertThat(Dimensions.getSize(ImmutableList.of(dim1, dim2, dim3))).isEqualTo(210);
  }

  @Test
  public void testMakeShape() {
    Dimension dim1 = new Dimension("dim1", 5);
    Dimension dim2 = new Dimension("dim2", 6);
    Dimension dim3 = new Dimension("dim3", 7);
    assertThat(Dimensions.makeShape(ImmutableList.of(dim1, dim2, dim3))).isEqualTo(new int[] {5, 6, 7});
  }

  @Test
  public void testMakeDimensionsAnon() {
    List<Dimension> list = Dimensions.makeDimensionsAnon(new int[] {5, 6, 7});
    Dimension dim1o = list.get(0);
    assertThat(dim1o.getShortName()).isEqualTo("5");
    assertThat(dim1o.getLength()).isEqualTo(5);
    assertThat(dim1o.isShared()).isFalse();
    assertThat(dim1o.isUnlimited()).isFalse();
    assertThat(dim1o.isVariableLength()).isFalse();
    assertThat(dim1o.toString()).isEqualTo("5;");

    // unshared dimensions dont match
    Dimension dim1 = Dimension.builder("", 5).setIsShared(false).build();
    Dimension dim2 = Dimension.builder("", 6).setIsShared(false).build();
    Dimension dim3 = Dimension.builder("", 7).setIsShared(false).build();
    assertThat(ImmutableList.of(dim1, dim2, dim3)).isNotEqualTo(list);
    assertThat(list.get(0)).isNotEqualTo(dim1);
    // unshared dimensions compare by name
    assertThat(list.get(0).compareTo(dim1)).isEqualTo(0);

    assertThat(Dimensions.makeDimensionsAnon(new int[0])).isEmpty();
  }

  @Test
  public void testMakeSectionFromDimensions() {
    Dimension dim1 = new Dimension("dim1", 5);
    Dimension dim2 = new Dimension("dim2", 6);
    Dimension dim3 = new Dimension("dim3", 7);
    Section section = Dimensions.makeArraySectionFromDimensions(ImmutableList.of(dim1, dim2, dim3)).build();
    assertThat(section).isEqualTo(new Section(new int[] {5, 6, 7}));
  }

  @Test
  public void testMakeSectionVlen() {
    Dimension dim1 = new Dimension("dim1", 5);
    Dimension dim2 = new Dimension("dim2", 6);
    Dimension dim3 = Dimension.builder().setIsVariableLength(true).build();
    Section section = Dimensions.makeArraySectionFromDimensions(ImmutableList.of(dim1, dim2, dim3)).build();
    assertThat(section).isEqualTo(new Section(new int[] {5, 6, -1}));
  }

  @Test
  public void testMakeSectionUlim() {
    Dimension dim1 = new Dimension("dim1", 5);
    Dimension dim2 = new Dimension("dim2", 6);
    Dimension dim3 = Dimension.builder().setName("udim").setIsUnlimited(true).build();
    Section section = Dimensions.makeArraySectionFromDimensions(ImmutableList.of(dim1, dim2, dim3)).build();
    assertThat(section).isEqualTo(new Section(new int[] {5, 6, 0}));
  }

  @Test
  public void testMakeDimensionsAll() {
    Dimension dim1 = new Dimension("dim1", 5);
    Dimension dim2 = new Dimension("dim2", 58);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(dim1);
    Group.Builder grandparent = Group.builder().setName("gramps").addGroup(parentg).addDimension(dim2);
    Group root = Group.builder().addGroup(grandparent).build();
    Optional<Group> parento = root.findGroupNested("parent");
    assertThat(parento.isPresent()).isTrue();

    Variable v = Variable.builder().setName("v").setArrayType(ArrayType.STRING).setParentGroupBuilder(parentg)
        .setDimensionsByName("dim1 dim2").build(parento.get());

    assertThat(Dimensions.makeDimensionsAll(v)).isEqualTo(ImmutableList.of(dim1, dim2));
  }

}
