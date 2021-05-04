/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import ucar.nc2.internal.util.AliasTranslator;

/** Test {@link AliasTranslator} */
public class TestAliasTranslator {

  @Test
  public void testAliasTranslator() {
    AliasTranslator.addAlias("alias", "really");
    assertThat(AliasTranslator.translateAlias("alias")).isEqualTo("really");
    assertThat(AliasTranslator.translateAlias("aliasNot")).isEqualTo("reallyNot");
    assertThat(AliasTranslator.translateAlias("alia")).isEqualTo("alia");
  }

}
