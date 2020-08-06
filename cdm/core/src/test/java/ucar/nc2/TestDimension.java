package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Test;
import ucar.ma2.DataType;

/** Test {@link ucar.nc2.Dimension} */
public class TestDimension {

  @Test
  public void testBuilder() {
    Dimension dim = Dimension.builder().setName("name").setLength(99).setIsUnlimited(true).setIsShared(false).build();
    assertThat(dim.getShortName()).isEqualTo("name");
    assertThat(dim.getLength()).isEqualTo(99);
    assertThat(dim.isUnlimited()).isEqualTo(true);
    assertThat(dim.isShared()).isEqualTo(false);
  }

  @Test
  public void testVariableLengthBuilder() {
    Dimension dim = Dimension.builder().setName("name").setLength(99).setIsUnlimited(true).setIsShared(true)
        .setIsVariableLength(true) // overrides
        .build();
    assertThat(dim.getShortName()).isEqualTo("name");
    assertThat(dim.getLength()).isEqualTo(-1);
    assertThat(dim.isUnlimited()).isEqualTo(false);
    assertThat(dim.isShared()).isEqualTo(false);
    assertThat(dim.isVariableLength()).isEqualTo(true);
  }

  @Test
  public void testConstructor() {
    Dimension dim = Dimension.builder().setName("name").setLength(99).build();
    assertThat(dim).isEqualTo(new Dimension("name", 99));
    assertThat(dim.isUnlimited()).isEqualTo(false);
    assertThat(dim.isShared()).isEqualTo(true);
    assertThat(dim.isVariableLength()).isEqualTo(false);
  }

  @Test
  public void testSetLength() {
    Dimension dim = Dimension.builder().setName("name").setLength(99).build();
    assertThat(dim.getLength()).isEqualTo(99);

    Dimension dim0 = Dimension.builder().setName("name").setIsUnlimited(true).setLength(0).build();
    assertThat(dim0.getLength()).isEqualTo(0);

    Dimension dimv = Dimension.builder().setName("name").setIsVariableLength(true).setLength(-1).build();
    assertThat(dimv.getLength()).isEqualTo(-1);

    try {
      Dimension.builder().setName("name").setLength(0).build();
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      Dimension.builder().setName("name").setIsUnlimited(true).setLength(-1).build();
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      Dimension.builder().setName("name").setIsVariableLength(true).setLength(1).build();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testToBuilder() {
    Dimension dim = Dimension.builder().setName("name").setLength(7).setIsUnlimited(true).build();
    assertThat(dim.getShortName()).isEqualTo("name");
    assertThat(dim.isUnlimited()).isTrue();

    Dimension dim2 = dim.toBuilder().setName("name2").build();
    assertThat(dim2).isEqualTo(new Dimension("name2", 7, true, true, false));

    assertThat(dim.compareTo(dim2)).isEqualTo(-1);
    assertThat(dim2.compareTo(dim)).isEqualTo(1);
    assertThat(dim2.compareTo(dim2)).isEqualTo(0);
  }

  @Test
  public void testEquals() {
    Dimension dim1 = Dimension.builder().setName("name").setLength(7).build();
    Dimension dim2 = new Dimension("name", 7);
    assertThat(dim1).isEqualTo(dim2);;
    assertThat(dim1.hashCode()).isEqualTo(dim2.hashCode());;

    assertThat(dim1).isEqualTo(dim1.toBuilder().build());;
    assertThat(dim1).isNotEqualTo(Dimension.builder().setName("name").setLength(7).setIsUnlimited(true).build());;
    assertThat(dim1).isNotEqualTo(Dimension.builder().setName("name").setLength(7).setIsVariableLength(true).build());;
  }

  @Test
  public void testWriteCDL() {
    Dimension dim = Dimension.builder().setName("name").setLength(7).build();
    assertThat(dim.toString()).isEqualTo("name = 7;");;
  }

  @Test
  public void testWriteCDLunlimited() {
    Dimension dim = Dimension.builder().setName("name").setLength(7).setIsUnlimited(true).build();
    assertThat(dim.toString()).isEqualTo("name = UNLIMITED;   // (7 currently)");;
  }

  @Test
  public void testWriteCDLvariable() {
    Dimension dim = Dimension.builder().setName("name").setLength(7).setIsVariableLength(true).build();
    assertThat(dim.toString()).isEqualTo("name = UNKNOWN;");;
  }

  @Test
  public void testFullName() {
    Dimension dim = new Dimension("dimname", 5);
    assertThat(dim.getShortName()).isNotNull();
    Group parent = Group.builder().addDimension(dim).build();
    assertThat(dim.makeFullName(parent)).isEqualTo(dim.getShortName());
  }

  @Test
  public void testFullNameNested() {
    Dimension dim = new Dimension("dimname", 5);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(dim);
    Group.Builder grandparent = Group.builder().setName("gramps").addGroup(parentg);
    Group root = Group.builder().addGroup(grandparent).build();
    Optional<Group> parent = root.findGroupNested("parent");
    // TODO shouldnt that have a leading / ?
    parent.ifPresent(g -> assertThat(dim.makeFullName(g)).isEqualTo("gramps/parent/dimname"));
  }

  @Test
  public void testFullNameNotFound() {
    Dimension dim = new Dimension("dimname", 5);
    Group parent = Group.builder().build();
    try {
      assertThat(dim.makeFullName(parent)).isEqualTo(dim.getShortName());
      fail();
    } catch (Exception e) {
      //
    }
  }

  @Test
  public void testFullNameVariable() {
    Dimension dim = new Dimension("dimname", 5);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(dim);
    Group.Builder grandparent = Group.builder().setName("gramps").addGroup(parentg);
    Group root = Group.builder().addGroup(grandparent).build();
    Optional<Group> parent = root.findGroupNested("parent");
    Variable v = Variable.builder().setName("v").setDataType(DataType.STRING).setParentGroupBuilder(parentg)
        .setDimensionsByName("dimname").build(parent.get());

    // TODO shouldnt that have a leading / ?
    parent.ifPresent(g -> assertThat(dim.makeFullName(v)).isEqualTo("gramps/parent/dimname"));
  }

  @Test
  public void testFullNameAnonymous() {
    Dimension dim = new Dimension(null, 5, false, false, false); // Anonymous dimension.
    Group.Builder parentg = Group.builder().setName("parent");
    Group.Builder grandparent = Group.builder().setName("gramps").addGroup(parentg);
    Group root = Group.builder().addGroup(grandparent).build();
    Optional<Group> parent = root.findGroupNested("parent");
    Variable v = Variable.builder().setName("v").setDataType(DataType.CHAR).setParentGroupBuilder(parentg)
        .setDimensions(ImmutableList.of(dim)).build(parent.get());

    // TODO what should it be?
    assertThat(dim.makeFullName(v)).isEqualTo("5");
  }


  @Test
  public void testAnonDimFullName() throws Exception {
    Dimension dim = new Dimension(null, 5, false, false, false); // Anonymous dimension.
    assertThat(dim.getShortName()).isNull();
    // Assert.assertNull(dim.getFullName()); // getFullName() This used to cause a NullPointerException.
  }
}
