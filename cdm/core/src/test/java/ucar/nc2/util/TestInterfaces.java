/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Test {@link ucar.nc2.util.CancelTask}, {@link ucar.nc2.util.DebugFlags} */
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
}
