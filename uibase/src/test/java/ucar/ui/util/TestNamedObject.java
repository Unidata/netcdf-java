/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.util;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ucar.ui.util.NamedObject} */
public class TestNamedObject {

  @Test
  public void testNamedObject() {
    Object what = new Object();
    NamedObject named = NamedObject.create("name", "desc", what);
    assertThat(named.getName()).isEqualTo("name");
    assertThat(named.getDescription()).isEqualTo("desc");
    assertThat(named.getValue() == what).isTrue();

    String thing = "what";
    NamedObject namedThing = NamedObject.create(thing, "descr");
    assertThat(namedThing.getName()).isEqualTo("what");
    assertThat(namedThing.getDescription()).isEqualTo("descr");
    assertThat(namedThing.getValue() == thing).isTrue();

    assertThat(named instanceof NamedObject).isTrue();
    assertThat(named).isInstanceOf(NamedObject.class);
  }
}
