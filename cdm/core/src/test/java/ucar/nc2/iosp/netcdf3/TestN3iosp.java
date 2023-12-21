package ucar.nc2.iosp.netcdf3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class TestN3iosp {

  @Test
  public void shouldReturnInvalidForNullOrEmptyNames() {
    assertThat(N3iosp.isValidNetcdfObjectName(null)).isFalse();
    assertThrows(NullPointerException.class, () -> N3iosp.makeValidNetcdfObjectName(null));

    assertThat(N3iosp.isValidNetcdfObjectName("")).isFalse();
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> N3iosp.makeValidNetcdfObjectName(""));
    assertThat(e.getMessage()).isEqualTo("Illegal NetCDF object name: ''");
  }

  @Test
  public void shouldReturnInvalidForBadFirstCharacters() {
    // names with first chars not in ([a-zA-Z0-9_]|{UTF8}) are invalid
    assertThat(N3iosp.isValidNetcdfObjectName(" blah")).isFalse();
    assertThat(N3iosp.makeValidNetcdfObjectName(" blah")).isEqualTo("blah");

    assertThat(N3iosp.isValidNetcdfObjectName("\n/blah")).isFalse();
    assertThat(N3iosp.makeValidNetcdfObjectName("\n/blah")).isEqualTo("blah");

    // Unit separator and DEL
    assertThat(N3iosp.isValidNetcdfObjectName("\u001F\u007F blah")).isFalse();
    assertThat(N3iosp.makeValidNetcdfObjectName("\u001F\u007F blah")).isEqualTo("blah");
  }

  @Test
  public void shouldReturnInvalidForBadCharacters() {
    // names with remaining chars not in ([^\x00-\x1F\x7F/]|{UTF8})* are invalid
    assertThat(N3iosp.isValidNetcdfObjectName("1\u000F2\u007F3/4")).isFalse();
    assertThat(N3iosp.makeValidNetcdfObjectName("1\u000F2\u007F3/4")).isEqualTo("1234");

    // names may not have trailing spaces
    assertThat(N3iosp.isValidNetcdfObjectName("foo     ")).isFalse();
    assertThat(N3iosp.makeValidNetcdfObjectName("foo     ")).isEqualTo("foo");
  }

  @Test
  public void shouldAcceptValidNames() {
    // valid names have syntax: ([a-zA-Z0-9_]|{UTF8})([^\x00-\x1F\x7F/]|{UTF8})*
    assertThat(N3iosp.isValidNetcdfObjectName("_KfS9Jn_s9__")).isTrue();
    assertThat(N3iosp.makeValidNetcdfObjectName("_KfS9Jn_s9__")).isEqualTo("_KfS9Jn_s9__");

    // unicode characters greater than 0x7F can appear anywhere
    assertThat(N3iosp.isValidNetcdfObjectName("\u0123\u1234\u2345\u3456")).isTrue();
    assertThat(N3iosp.makeValidNetcdfObjectName("\u0123\u1234\u2345\u3456")).isEqualTo("\u0123\u1234\u2345\u3456");
  }
}
