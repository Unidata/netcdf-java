package ucar.nc2.util;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.util.Misc.nearlyEquals;
import static ucar.nc2.util.Misc.nearlyEqualsAbs;

import org.junit.Test;

public class TestMisc {

  @Test
  public void shouldCompareWithRelativeDifference() {
    assertThat(nearlyEquals(100.0, 99.0, 1)).isTrue();
    assertThat(nearlyEquals(100.0, 99.0, 0.01)).isTrue();
    assertThat(nearlyEquals(100.0, 99.0, 0.001)).isFalse();

    assertThat(nearlyEquals(100.0f, 99.0f, 1)).isTrue();
    assertThat(nearlyEquals(100.0f, 99.0f, 0.01)).isTrue();
    assertThat(nearlyEquals(100.0f, 99.0f, 0.001)).isFalse();
  }

  @Test
  public void shouldCompareWithAbsoluteDifference() {
    assertThat(nearlyEqualsAbs(100.0, 99.0, 1)).isTrue();
    assertThat(nearlyEqualsAbs(100.0, 99.0, 0.01)).isFalse();
    assertThat(nearlyEqualsAbs(100.0, 99.0, 0.001)).isFalse();

    assertThat(nearlyEqualsAbs(100.0f, 99.0f, 1)).isTrue();
    assertThat(nearlyEqualsAbs(100.0f, 99.0f, 0.01)).isFalse();
    assertThat(nearlyEqualsAbs(100.0f, 99.0f, 0.001)).isFalse();
  }
}
