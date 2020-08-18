/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.nc2.internal.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Test {@link ucar.nc2.internal.util.EscapeStrings} */
public class TestEscapeStrings {

  @Test
  public void showOGC() {
    for (char c : (EscapeStrings.alphaNumeric + " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~").toCharArray()) {
      String encoded = EscapeStrings.escapeOGC("" + c);
      System.err.printf("|%c|=|%s|%n", c, encoded);
    }
  }

  @Test
  public void testOGC() {
    for (char c : (EscapeStrings.alphaNumeric + " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~").toCharArray()) {
      String encoded = EscapeStrings.escapeOGC("" + c);
      if (EscapeStrings._allowableInOGC.indexOf(c) >= 0) {
        assertThat(encoded).isEqualTo("" + c);
      } else {
        assertThat(encoded).isEqualTo("%"+Integer.toHexString(c));
      }

      EscapeStrings.unescapeString("" + c);
    }
  }

}
