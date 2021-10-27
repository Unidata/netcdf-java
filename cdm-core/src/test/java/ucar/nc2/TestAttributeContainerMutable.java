/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Test;

/** Test {@link ucar.nc2.AttributeContainerMutable} */
public class TestAttributeContainerMutable {

  @Test
  public void testBasics() {
    AttributeContainerMutable attc = new AttributeContainerMutable("container");
    assertThat(attc.isEmpty()).isTrue();
    attc.addAttribute(new Attribute("name", "value"));
    attc.addAttribute(new Attribute("name2", "value2"));
    attc.addAttribute("name3", 123);
    assertThat(Iterables.size(attc)).isEqualTo(3);

    assertThat(attc.getName()).isEqualTo("container");
    attc.setName("c");
    assertThat(attc.getName()).isEqualTo("c");

    assertThat(attc.findAttributeIgnoreCase("NAME")).isNotNull();
    assertThat(attc.findAttributeIgnoreCase("NAME").getStringValue()).isEqualTo("value");

    assertThat(attc.findAttributeString("NAME", null)).isEqualTo("value");
    assertThat(attc.findAttributeInteger("NAME3", -1)).isEqualTo(123);
    assertThat(attc.findAttributeDouble("NAME3", Double.NaN)).isEqualTo(123.0);
  }

  @Test
  public void testAddingDuplicates() {
    AttributeContainerMutable attc = new AttributeContainerMutable("name");
    attc.addAttribute(new Attribute("name", "value1"));
    attc.addAttribute(new Attribute("name", "value2"));
    attc.addAttribute("name", "value3");

    assertThat(Iterables.size(attc)).isEqualTo(1);
    assertThat(attc.findAttributeString("name", null)).isEqualTo("value3");

    attc.addAll(ImmutableList.of(new Attribute("name", 12345)));
    assertThat(Iterables.size(attc)).isEqualTo(1);
    assertThat(attc.findAttributeInteger("name", -1)).isEqualTo(12345);
  }

  @Test
  public void testRemove() {
    AttributeContainerMutable attc = new AttributeContainerMutable("container");
    attc.addAttribute(new Attribute("name", "value"));
    attc.addAttribute(new Attribute("name2", "value2"));
    attc.addAttribute("name3", 123);
    assertThat(Iterables.size(attc)).isEqualTo(3);

    assertThat(attc.removeAttribute("name2")).isTrue();
    assertThat(Iterables.size(attc)).isEqualTo(2);
    assertThat(attc.findAttribute("name2")).isNull();
    assertThat(attc.removeAttribute("name2")).isFalse();

    assertThat(attc.remove(new Attribute("name", "value"))).isTrue();
    assertThat(Iterables.size(attc)).isEqualTo(1);
    assertThat(attc.findAttribute("name")).isNull();
    assertThat(attc.remove(new Attribute("name", "value"))).isFalse();

    assertThat(attc.removeAttributeIgnoreCase("NAMe3")).isTrue();
    assertThat(Iterables.size(attc)).isEqualTo(0);
    assertThat(attc.findAttribute("name3")).isNull();
    assertThat(attc.removeAttributeIgnoreCase("name3")).isFalse();
  }

  @Test
  public void testReplace() {
    AttributeContainerMutable attc = new AttributeContainerMutable("container");
    Attribute att = new Attribute("name", "valuable");
    attc.addAttribute(att);
    attc.addAttribute("name3", 123);
    assertThat(Iterables.size(attc)).isEqualTo(2);
    assertThat(attc.findAttribute("name2")).isNull();

    assertThat(attc.replace(att, "name2")).isTrue();
    assertThat(Iterables.size(attc)).isEqualTo(2);
    assertThat(attc.findAttribute("name2")).isNotNull();
    assertThat(attc.findAttributeString("name2", null)).isEqualTo("valuable");
  }

  @Test
  public void testFindConvert() {
    AttributeContainerMutable attc = new AttributeContainerMutable("container");
    attc.addAttribute("int", 123);
    attc.addAttribute("ints", "1234");
    attc.addAttribute("double", 666.6);
    attc.addAttribute("doubles", "777.7");

    assertThat(attc.findAttributeInteger("int", -1)).isEqualTo(123);
    assertThat(attc.findAttributeInteger("ints", -1)).isEqualTo(1234);
    assertThat(attc.findAttributeDouble("int", -1)).isEqualTo(123.0);
    assertThat(attc.findAttributeDouble("ints", -1)).isEqualTo(1234.0);
    assertThat(attc.findAttributeString("int", null)).isEqualTo(null);
    assertThat(attc.findAttributeString("ints", null)).isEqualTo("1234");

    assertThat(attc.findAttributeInteger("double", -1)).isEqualTo(666);
    assertThat(attc.findAttributeDouble("double", -1)).isEqualTo(666.6);
    assertThat(attc.findAttributeDouble("doubles", -1)).isEqualTo(777.7);
    assertThat(attc.findAttributeString("double", null)).isEqualTo(null);
    assertThat(attc.findAttributeString("doubles", null)).isEqualTo("777.7");

    try {
      assertThat(attc.findAttributeInteger("doubles", -1)).isEqualTo(777);
      fail();
    } catch (NumberFormatException e) {
      // expected
    }
  }

  @Test
  public void testToImmutable() {
    AttributeContainerMutable attc = new AttributeContainerMutable("container");
    assertThat(attc.isEmpty()).isTrue();
    attc.addAttribute(new Attribute("name", "value"));
    attc.addAttribute(new Attribute("name2", "value2"));
    attc.addAttribute("name3", 123);
    assertThat(Iterables.size(attc)).isEqualTo(3);

    AttributeContainer imm = attc.toImmutable();
    assertThat(!(imm instanceof AttributeContainerMutable)).isTrue();
    assertThat(imm.isEmpty()).isFalse();
    assertThat(Iterables.size(imm)).isEqualTo(3);

    assertThat(imm.getName()).isEqualTo("container");

    assertThat(imm.findAttributeIgnoreCase("NAME")).isNotNull();
    assertThat(imm.findAttributeString("NAME", null)).isEqualTo("value");
    assertThat(imm.findAttributeInteger("NAME3", -1)).isEqualTo(123);
    assertThat(imm.findAttributeDouble("NAME3", Double.NaN)).isEqualTo(123.0);

    AttributeContainerMutable attcc = new AttributeContainerMutable("cc", imm);
    assertThat(Iterables.size(attcc)).isEqualTo(3);
    assertThat(attcc.getName()).isEqualTo("cc");
  }

  @Test
  public void testFilter() {
    AttributeContainerMutable attc = new AttributeContainerMutable("container");
    assertThat(attc.isEmpty()).isTrue();
    attc.addAttribute(new Attribute("name1", "value"));
    attc.addAttribute(new Attribute("name2", "value2"));
    attc.addAttribute("name3", 123);
    assertThat(Iterables.size(attc)).isEqualTo(3);

    AttributeContainer imm = AttributeContainer.filter(attc, "name1", "name2");
    assertThat(!(imm instanceof AttributeContainerMutable)).isTrue();
    assertThat(imm.isEmpty()).isFalse();
    assertThat(Iterables.size(imm)).isEqualTo(1);

    assertThat(imm.getName()).isEqualTo("container");

    assertThat(imm.findAttributeIgnoreCase("NAME3")).isNotNull();
    assertThat(imm.findAttributeInteger("NAME3", -1)).isEqualTo(123);
  }


}
