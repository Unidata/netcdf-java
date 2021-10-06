package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
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
    Dimension anon = Dimension.builder().setLength(5).setIsShared(false).build(); // Anonymous dimension.
    Group.Builder groupb = Group.builder().addDimension(dim);

    ImmutableList<Dimension> list = Dimensions.makeDimensionsList(groupb::findDimension, "dimname 5");
    assertThat(ImmutableList.of(dim, anon)).isEqualTo(list);

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
    ImmutableList<Dimension> list = Dimensions.makeDimensionsAnon(new int[] {5, 6, 7});
    Dimension dim1 = Dimension.builder().setIsShared(false).setLength(5).build();
    Dimension dim2 = Dimension.builder().setIsShared(false).setLength(6).build();
    Dimension dim3 = Dimension.builder().setIsShared(false).setLength(7).build();
    assertThat(ImmutableList.of(dim1, dim2, dim3)).isEqualTo(list);

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
