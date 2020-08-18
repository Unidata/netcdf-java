/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
        assertThat(encoded).isEqualTo("%" + Integer.toHexString(c));
      }
    }
  }

  @Test
  public void testBackslashEscapeCDMString() {
    assertThat(EscapeStrings.backslashEscapeCDMString("", "stuff")).isEqualTo("");
    assertThat(EscapeStrings.backslashEscapeCDMString("any", "")).isEqualTo("any");
    assertThat(EscapeStrings.backslashEscapeCDMString("any", "n")).isEqualTo("a\\ny");
  }

  @Test
  public void testEscapeURLQuery() {
    assertThat(EscapeStrings.escapeURLQuery("any")).isEqualTo("any");
    assertThat(EscapeStrings.escapeURLQuery("any thing")).isEqualTo("any%20thing");
    assertThat(EscapeStrings.escapeURLQuery("any+thing")).isEqualTo("any+thing");
    assertThat(EscapeStrings.escapeURLQuery("any|thing")).isEqualTo("any%7cthing");
    assertThat(EscapeStrings.escapeURLQuery("any%7cthing")).isEqualTo("any%7cthing");

    assertThat(EscapeStrings.unescapeURLQuery("any%20thing")).isEqualTo("any thing");
    assertThat(EscapeStrings.unescapeURLQuery("any+thing")).isEqualTo("any+thing");
    assertThat(EscapeStrings.unescapeURLQuery("any%7cthing")).isEqualTo("any|thing");
  }

  @Test
  public void testUrlDecode() {
    assertThat(EscapeStrings.urlDecode("https::/stuff%")).isNull();
    assertThat(EscapeStrings.urlDecode("https://localhost:8000/search?q=text#hello"))
        .isEqualTo("https://localhost:8000/search?q=text#hello");
  }

}
