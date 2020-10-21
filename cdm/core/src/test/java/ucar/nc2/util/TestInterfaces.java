/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Test {@link ucar.nc2.util.CancelTask}, {@link ucar.nc2.util.DebugFlags}, {@link ucar.nc2.util.NamedObject} */
public class TestInterfaces {

  @Test
  public void testCancelTask() {
    CancelTask cancelTask = CancelTask.create();
    assertThat(cancelTask.isCancel()).isFalse();
    cancelTask.cancel();
    assertThat(cancelTask.isCancel()).isTrue();
  }

  @Test
  public void testDebugFlags() {
    DebugFlags flags = DebugFlags.create("");
    assertThat(flags.isSet("")).isFalse();

    flags = DebugFlags.create("one two");
    assertThat(flags.isSet("one")).isTrue();
    assertThat(flags.isSet("two")).isTrue();
    assertThat(flags.isSet("onetwo")).isFalse();

    flags = DebugFlags.create("one; two");
    assertThat(flags.isSet("one;")).isTrue();
    assertThat(flags.isSet("two")).isTrue();
    assertThat(flags.isSet("one")).isFalse();
  }

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
