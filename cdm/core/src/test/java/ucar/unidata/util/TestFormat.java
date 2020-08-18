package ucar.unidata.util;

import static com.google.common.truth.Truth.assertThat;
import static ucar.unidata.util.Format.dfrac;
import org.junit.Test;

/** Test {@link ucar.unidata.util.Format} */
public class TestFormat {

  @Test
  public void testDfrac() {
    double num = 1.00003;
    assertThat(dfrac(num, 0)).isEqualTo("1");
    assertThat(dfrac(num, 1)).isEqualTo("1.0");
    assertThat(dfrac(num, 2)).isEqualTo("1.00");
    assertThat(dfrac(num, 3)).isEqualTo("1.000");
    assertThat(dfrac(num, 4)).isEqualTo("1.0000");
    assertThat(dfrac(num, 5)).isEqualTo("1.00003");
    assertThat(dfrac(num, 6)).isEqualTo("1.000030");
  }

  @Test
  public void testD() {
    double num = 1.00003;
    assertThat(Format.d(num, 0)).isEqualTo("1");
    assertThat(Format.d(num, 1)).isEqualTo("1");
    assertThat(Format.d(num, 2)).isEqualTo("1.0");
    assertThat(Format.d(num, 3)).isEqualTo("1.00");
    assertThat(Format.d(num, 4)).isEqualTo("1.000");
    assertThat(Format.d(num, 5)).isEqualTo("1.0000");
    assertThat(Format.d(num, 6)).isEqualTo("1.00003");
    assertThat(Format.d(num, 7)).isEqualTo("1.00003");
  }

  @Test
  public void testDwidth() {
    double num = 1.2345;
    assertThat(Format.d(num, 3, 2)).isEqualTo("1.23");
    assertThat(Format.d(num, 3, 3)).isEqualTo("1.23");
    assertThat(Format.d(num, 3, 4)).isEqualTo("1.23");
    assertThat(Format.d(num, 3, 5)).isEqualTo(" 1.23");
    assertThat(Format.d(num, 3, 6)).isEqualTo("  1.23");
  }

  @Test
  public void testL() {
    long num = 12345L;
    assertThat(Format.l(num, 2)).isEqualTo("12345");
    assertThat(Format.l(num, 3)).isEqualTo("12345");
    assertThat(Format.l(num, 4)).isEqualTo("12345");
    assertThat(Format.l(num, 5)).isEqualTo("12345");
    assertThat(Format.l(num, 6)).isEqualTo(" 12345");
    assertThat(Format.l(num, 7)).isEqualTo("  12345");
  }

  @Test
  public void testI() {
    int num = 12345;
    assertThat(Format.i(num, 2)).isEqualTo("12345");
    assertThat(Format.i(num, 3)).isEqualTo("12345");
    assertThat(Format.i(num, 4)).isEqualTo("12345");
    assertThat(Format.i(num, 5)).isEqualTo("12345");
    assertThat(Format.i(num, 6)).isEqualTo(" 12345");
    assertThat(Format.i(num, 7)).isEqualTo("  12345");
  }

  @Test
  public void testPad() {
    assertThat(Format.pad("123", 2, true)).isEqualTo("123");
    assertThat(Format.pad("123", 2, false)).isEqualTo("123");
    assertThat(Format.pad("123", 3, true)).isEqualTo("123");
    assertThat(Format.pad("123", 3, false)).isEqualTo("123");
    assertThat(Format.pad("123", 4, true)).isEqualTo(" 123");
    assertThat(Format.pad("123", 4, false)).isEqualTo("123 ");
  }

  @Test
  public void testTab() {
    StringBuilder sb = new StringBuilder("123");
    Format.tab(sb, 2, false);
    assertThat(sb.toString()).isEqualTo("123");

    sb = new StringBuilder("123");
    Format.tab(sb, 2, true);
    assertThat(sb.toString()).isEqualTo("123 ");

    sb = new StringBuilder("123");
    Format.tab(sb, 6, false);
    assertThat(sb.toString()).isEqualTo("123   ");

    sb = new StringBuilder("123");
    Format.tab(sb, 6, true);
    assertThat(sb.toString()).isEqualTo("123   ");
  }



}
